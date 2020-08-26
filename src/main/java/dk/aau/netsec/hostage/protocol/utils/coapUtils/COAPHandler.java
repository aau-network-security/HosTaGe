package dk.aau.netsec.hostage.protocol.utils.coapUtils;

import com.mbed.coap.exception.CoapCodeException;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.CoapExchange;
import com.mbed.coap.utils.CoapResource;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.commons.SubnetUtils;
import dk.aau.netsec.hostage.logging.AttackRecord;
import dk.aau.netsec.hostage.logging.MessageRecord;
import dk.aau.netsec.hostage.logging.SyncDevice;
import dk.aau.netsec.hostage.protocol.Protocol;

public class COAPHandler extends CoapResource {
    private static ArrayList<CoapPacket> requests = new ArrayList<>();
    private static final ArrayList<CoapPacket> fullRequests = new ArrayList<>();
    private String value="Response";


    public COAPHandler(){

    }

    public COAPHandler(String value){
        this.value=value;
    }


    @Override
    public void get(CoapExchange exchange) {
        requests.add(exchange.getRequest());
        fullRequests.add(exchange.getRequest());
        exchange.setResponseCode(Code.C205_CONTENT);
        exchange.setResponseBody(value);
        exchange.sendResponse();
    }

    @Override
    public void put(CoapExchange exchange) {
        requests.add(exchange.getRequest());
        fullRequests.add(exchange.getRequest());
        exchange.setResponseCode(Code.C204_CHANGED);
        exchange.setResponseBody(Integer.valueOf(exchange.getRequestBody().length).toString());
        exchange.sendResponse();
    }

    @Override
    public void post(CoapExchange exchange) throws CoapCodeException {
        requests.add(exchange.getRequest());
        fullRequests.add(exchange.getRequest());
        throw new CoapCodeException(Code.C400_BAD_REQUEST);
    }

    @Override
    public void delete(CoapExchange exchange){
        requests.add(exchange.getRequest());
        fullRequests.add(exchange.getRequest());
        exchange.setResponseCode(Code.C202_DELETED);
        exchange.sendResponse();
    }

    public static boolean isAnAttackOngoing(){
        return !requests.isEmpty();
    }

    private static CoapPacket getCurrentPacket(){
        InetSocketAddress socketAddress = new InetSocketAddress(5683);
        CoapPacket coapPacket = new CoapPacket(socketAddress);
        if(!requests.isEmpty())
            return requests.get(0);
        else
            return coapPacket;
    }

    public static void removeCurrentConnected(){
        if(!requests.isEmpty())
            requests.clear();
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
        String remoteIp = getCurrentPacket().getRemoteAddress().getAddress().toString();
        remoteIp = remoteIp.startsWith("/") ? remoteIp.substring(1) : remoteIp;

        record.setAttack_id(attack_id);
        record.setSync_id(attack_id);
        if(SyncDevice.currentDevice()!=null)
            record.setDevice(Objects.requireNonNull(SyncDevice.currentDevice()).getDeviceID());
        else
            record.setDevice(UUID.randomUUID().toString());
        record.setProtocol("COAP");
        record.setExternalIP(externalIP);
        record.setLocalIP(internalIp);
        record.setLocalPort(5683);
        record.setWasInternalAttack(checkIfIsInternalAttack(remoteIp,internalIp));
        record.setRemoteIP(remoteIp);
        record.setRemotePort(getCurrentPacket().getRemoteAddress().getPort());
        record.setBssid(BSSID);

        return record;
    }

    private synchronized static boolean checkIfIsInternalAttack(String remoteIPAddress,String internalIPAddress){
        if(remoteIPAddress.equals("127.0.0.1"))
            return true;
        int prefix = Hostage.prefix;
        try {
            SubnetUtils utils = new SubnetUtils(internalIPAddress + "/" + prefix);
            return utils.getInfo().isInRange(remoteIPAddress);
        }catch (IllegalArgumentException e){
            return true;
        }

    }

    /**
     *Helper method for Handler, creates a messageRecord with the logs from the COAPHandler.
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
        record.setPacket(getCurrentPacket().getPayloadString()+" "+"Message Id: "+getCurrentPacket().getMessageId());
        return record;
    }

}
