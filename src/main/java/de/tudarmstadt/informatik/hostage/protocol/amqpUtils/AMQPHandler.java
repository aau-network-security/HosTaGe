package de.tudarmstadt.informatik.hostage.protocol.amqpUtils;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.commons.MyLinkedMap;
import de.tudarmstadt.informatik.hostage.commons.SubnetUtils;
import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.logging.SyncDevice;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;

public class AMQPHandler {
    private static ArrayList<String> packets = LogBackWatcher.getList();

    public static boolean isAnAttackOngoing(){
        return !packets.isEmpty();
    }

    public static void removeCurrentConnected(){
        if(!packets.isEmpty())
            packets.clear();
    }

    /**
     * Regex pattern matching Groups 1 and 2 will hold IP and port, respectively.
     * "(" capturing group,
     * "\d" Matches any digit character (0-9). Equivalent to [0-9].
     * "{1,3} or {1,5} Matches the specified quantity of the previous token.
     * "\." Matches a "." character.
     * @return receiver and sender ip and port.
     */
    public static MyLinkedMap<Integer,String> getsAllIpsPorts() {
        MyLinkedMap<Integer,String> allIpsPorts = new MyLinkedMap<>(); //keeps the insertion order.
        final Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})");
        String caturePacket = packets.get(0);
        Matcher matcher = pattern.matcher(caturePacket);
        while (matcher.find()) {
            allIpsPorts.put(Integer.valueOf(matcher.group(2)),matcher.group(1));//group 2 port, group 1 IP.
        }
        return allIpsPorts;
    }

    /**
     * First inserted ip is the remote one.
     * @return Remote ip of the attacker.
     */
    private static String getRemoteIp(){
        return getsAllIpsPorts().getValue(0);
    }
    /**
     * Second inserted ip is the local one.
     * @return Local ip of the honeypot.
     */
    private String getLocalIp(){
        return getsAllIpsPorts().getValue(1);
    }

    /**
     * First inserted port is the remote one.
     * @return Remote port of the attacker.
     */
    private static int getRemotePort(){
        if(!getsAllIpsPorts().isEmpty() )
            return getsAllIpsPorts().getEntry(0).getKey();
        return 0;
    }
    /**
     * Second inserted port is the local one.
     * @return Local port of the honeypot.
     */
    private static int getLocalPort(){
        if(!getsAllIpsPorts().isEmpty() && getsAllIpsPorts().size()>1)
            return getsAllIpsPorts().getEntry(1).getKey();
        return 5672;
    }
    /**
     * Helper method for Handler, creates an attackRecord with the logs from the InterceptHandler.
     * @param attack_id the attack_id
     * @param externalIP the externalIp
     * @param protocol the protocol
     * @param subnetMask the subnet mask
     * @param BSSID the BSSID
     * @param internalIPAddress the internal IpAddress
     * @return
     * @throws UnknownHostException
     */
    public synchronized static AttackRecord createAttackRecord(Long attack_id, String externalIP, Protocol protocol, int subnetMask, String BSSID, int internalIPAddress){
        AttackRecord record = new AttackRecord();
        String internalIp = HelperUtils.intToStringIp(internalIPAddress);
        String remoteIp = getRemoteIp();
        record.setAttack_id(attack_id);
        record.setSync_id(attack_id);
        if(SyncDevice.currentDevice()!=null)
            record.setDevice(Objects.requireNonNull(SyncDevice.currentDevice()).getDeviceID());
        else
            record.setDevice(UUID.randomUUID().toString());
        record.setProtocol("AMQP");
        record.setExternalIP(externalIP);
        record.setLocalIP(internalIp);
        record.setLocalPort(getLocalPort());
        record.setWasInternalAttack(checkIfIsInternalAttack(remoteIp,internalIp));
        record.setRemoteIP(remoteIp);
        record.setRemotePort(getRemotePort());
        record.setBssid(BSSID);

        return record;
    }

    private synchronized static boolean checkIfIsInternalAttack(String remoteIPAddress,String internalIPAddress){
        if(remoteIPAddress == null)
            return true;
        if(remoteIPAddress.equals("127.0.0.1"))
            return true;
        int prefix = Hostage.prefix;
        SubnetUtils utils = new SubnetUtils(internalIPAddress+"/"+prefix);

        return utils.getInfo().isInRange(remoteIPAddress);
    }

    /**
     *Helper method for Handler, creates a messageRecord with the logs from the AMQPHandler.
     * @param type
     * @param attack_id
     * @return
     */
    public synchronized static MessageRecord createMessageRecord(MessageRecord.TYPE type, long attack_id) {
        MessageRecord record = new MessageRecord(true);
        record.setAttack_id(attack_id);
        record.setType(type);
        record.setStringMessageType(type.name());
        record.setTimestamp(System.currentTimeMillis());
        record.setPacket(packets.get(0));
        return record;
    }
}
