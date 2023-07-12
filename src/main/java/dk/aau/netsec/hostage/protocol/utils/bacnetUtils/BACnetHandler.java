package dk.aau.netsec.hostage.protocol.utils.bacnetUtils;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.event.DeviceEventListener;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.service.Service;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyAck;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyRequest;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.service.unconfirmed.IAmRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Sequence;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.MessagePriority;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class BACnetHandler extends LocalDevice {

    LocalDevice localDevice;

    Logger logger = LoggerFactory.getLogger(BACnetHandler.class);
    static ArrayList<UnconfirmedRequestService> unconfirmedRequest = new ArrayList<>();

    ArrayList<String> packets = LogBackWatcher.getList();
    public BACnetHandler(int deviceId, Transport transport) throws Exception {
        super(deviceId, transport);

        localDevice = new LocalDevice(deviceId, transport);
        localDevice.initialize();

        localDevice.getEventHandler().addListener(new DeviceEventListener() {
            @Override
            public void listenerException(Throwable throwable) {

            }

            @Override
            public void iAmReceived(RemoteDevice remoteDevice) {
                System.out.println("Received IAm from BACnet device " + remoteDevice.getInstanceNumber());
                //localDevice.sendGlobalBroadcast(localDevice.getIAm());
                System.out.println("Discovered device " + remoteDevice);
                System.out.println("device Address" + remoteDevice.getAddress().getMacAddress().getDescription());
                localDevice.addRemoteDevice(remoteDevice);

            }

            @Override
            public boolean allowPropertyWrite(Address address, BACnetObject baCnetObject, PropertyValue propertyValue) {
                return true;
            }

            @Override
            public void propertyWritten(Address address, BACnetObject baCnetObject, PropertyValue propertyValue) {

            }

            @Override
            public void iHaveReceived(RemoteDevice remoteDevice, RemoteObject remoteObject) {
                System.out.println("Value reported " + remoteDevice + " " + remoteObject);

            }

            @Override
            public void covNotificationReceived(UnsignedInteger unsignedInteger, RemoteDevice remoteDevice, ObjectIdentifier objectIdentifier, UnsignedInteger unsignedInteger1, SequenceOf<PropertyValue> sequenceOf) {

            }

            @Override
            public void eventNotificationReceived(UnsignedInteger unsignedInteger, RemoteDevice remoteDevice, ObjectIdentifier objectIdentifier, TimeStamp timeStamp, UnsignedInteger unsignedInteger1, UnsignedInteger unsignedInteger2, EventType eventType, CharacterString characterString, NotifyType notifyType, Boolean aBoolean, EventState eventState, EventState eventState1, NotificationParameters notificationParameters) {

            }

            @Override
            public void textMessageReceived(RemoteDevice remoteDevice, Choice choice, MessagePriority messagePriority, CharacterString characterString) {

            }

            @Override
            public void privateTransferReceived(Address address, UnsignedInteger unsignedInteger, UnsignedInteger unsignedInteger1, Sequence sequence) {

            }

            @Override
            public void reinitializeDevice(Address address, ReinitializeDeviceRequest.ReinitializedStateOfDevice reinitializedStateOfDevice) {

            }

            @Override
            public void synchronizeTime(Address address, DateTime dateTime, boolean b) {

            }

            //@Override
            public void requestReceived(Address address, Service service) {

            }

            public AcknowledgementService handleRequest(ConfirmedRequestService request) {
                if (request instanceof ReadPropertyRequest) {
                    ReadPropertyRequest readRequest = (ReadPropertyRequest) request;
                } else if (request instanceof WritePropertyRequest) {
                    WritePropertyRequest writeRequest = (WritePropertyRequest) request;
                    // ...
                } else {
                    // Handle other types of requests
                    // ...
                }
                return null;
            }

            public AcknowledgementService handleRequest(UnconfirmedRequestService request) {
                localDevice.sendGlobalBroadcast(new WhoIsRequest());

                if (request instanceof WhoIsRequest) {
                    localDevice.sendGlobalBroadcast(localDevice.getIAm());

                } else if (request instanceof IAmRequest) {
                    System.out.println("Received IAm from a BACnet device");
                    // ...
                } else {
                    // Handle other types of requests
                    // ...
                }
                return null;
            }
        });
    }

    public static boolean isAnAttackOngoing () {
        return !unconfirmedRequest.isEmpty();
    }

    public static void removeCurrentConnected () {
        if (!unconfirmedRequest.isEmpty())
            unconfirmedRequest.clear();
    }

    /**
     * The first matched packet usually contains less information than the following one.
     * @return packet
     */
    private static String findFullInfoPacket () {
        if (unconfirmedRequest.size() > 1)
            return String.valueOf(unconfirmedRequest.get(1));
        else
            return String.valueOf(unconfirmedRequest.get(0));
    }


    /**
     * First inserted ip is the remote one.
     * @return Remote ip of the attacker.
     */
    private static String getRemoteIp () {
        return IpPattern.getsAllIpsPorts(findFullInfoPacket()).getValue(0);
    }

    /**
     * First inserted port is the remote one.
     * @return Remote port of the attacker.
     */
    private static int getRemotePort () {
        MyLinkedMap<Integer, String> remotePorts = IpPattern.getsAllIpsPorts(findFullInfoPacket());
        if (!remotePorts.isEmpty())
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
    public synchronized static AttackRecord createAttackRecord (Long attack_id, String
            externalIP, Protocol protocol,int subnetMask, String BSSID,int internalIPAddress){
        AttackRecord record = new AttackRecord();
        String internalIp = HelperUtils.intToStringIp(internalIPAddress);
        String remoteIp = getRemoteIp();
        record.setAttack_id(attack_id);
        record.setSync_id(attack_id);
        if (SyncDevice.currentDevice() != null)
            record.setDevice(Objects.requireNonNull(SyncDevice.currentDevice()).getDeviceID());
        else
            record.setDevice(UUID.randomUUID().toString());
        record.setProtocol("BACnet");
        record.setExternalIP(externalIP);
        record.setLocalIP(internalIp);
        record.setLocalPort(0xBAC0);
        record.setWasInternalAttack(checkIfIsInternalAttack(remoteIp, internalIp));
        record.setRemoteIP(remoteIp);
        record.setRemotePort(getRemotePort());
        record.setBssid(BSSID);

        return record;
    }

    private synchronized static boolean checkIfIsInternalAttack (String remoteIPAddress, String internalIPAddress){
        if (remoteIPAddress == null)
            return true;
        if (remoteIPAddress.equals("127.0.0.1"))
            return true;
        int prefix = Hostage.prefix;
        SubnetUtils utils = new SubnetUtils(internalIPAddress + "/" + prefix);

        return utils.getInfo().isInRange(remoteIPAddress);
    }

    /**
     *Helper method for Handler, creates a messageRecord with the logs from the BACnetHandler.
     * @param type
     * @param attack_id
     * @return
     */
    public synchronized static MessageRecord createMessageRecord (MessageRecord.TYPE type, long attack_id){
        MessageRecord record = new MessageRecord(true);
        record.setAttack_id(attack_id);
        record.setType(type);
        record.setStringMessageType(type.name());
        record.setTimestamp(System.currentTimeMillis());
        record.setPacket(String.valueOf(unconfirmedRequest));
        return record;
    }

}