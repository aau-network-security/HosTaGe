package dk.aau.netsec.hostage.protocol.utils.bacnetUtils;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.commons.MyLinkedMap;
import dk.aau.netsec.hostage.commons.SubnetUtils;
import dk.aau.netsec.hostage.logging.AttackRecord;
import dk.aau.netsec.hostage.logging.MessageRecord;
import dk.aau.netsec.hostage.logging.SyncDevice;
import dk.aau.netsec.hostage.protocol.Protocol;
import dk.aau.netsec.hostage.protocol.commons.logWatchers.LogBackWatcher;
import dk.aau.netsec.hostage.protocol.commons.patterns.IpPattern;
import dk.aau.netsec.hostage.wrapper.Packet;

public class BACnetHandler {
    private static ArrayList<String> packets = LogBackWatcher.getList();

    public static boolean isAnAttackOngoing(){
        return !packets.isEmpty();
    }


    public static void removeCurrentConnected(){
        if(!packets.isEmpty())
            packets.clear();
    }

    /**
     * The first matched packet usually contains less information than the following one.
     * @return packet
     */
    private static String findFullInfoPacket(){
        if(packets.size()>1)
            return packets.get(1);
        else
            return packets.get(0);
    }

    /**
     * First inserted ip is the remote one.
     * @return Remote ip of the attacker.
     */
    private static String getRemoteIp(){
        return IpPattern.getsAllIpsPorts(findFullInfoPacket()).getValue(0);
    }

    /**
     * First inserted port is the remote one.
     * @return Remote port of the attacker.
     */
    private static int getRemotePort(){
        MyLinkedMap<Integer,String> remotePorts = IpPattern.getsAllIpsPorts(findFullInfoPacket());
        if(!remotePorts.isEmpty() )
            return remotePorts.getEntry(0).getKey();
        return 0;
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
        record.setProtocol("BACnet");
        record.setExternalIP(externalIP);
        record.setLocalIP(internalIp);
        record.setLocalPort(47808);
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
     *Helper method for Handler, creates a messageRecord with the logs from the BACnetHandler.
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
