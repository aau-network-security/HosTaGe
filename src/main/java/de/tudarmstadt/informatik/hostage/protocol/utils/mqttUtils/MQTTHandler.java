package de.tudarmstadt.informatik.hostage.protocol.utils.mqttUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.HostageApplication;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.commons.SubnetUtils;
import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.logging.SyncDevice;
import de.tudarmstadt.informatik.hostage.persistence.DAO.DAOHelper;
import de.tudarmstadt.informatik.hostage.protocol.MQTT;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;
import io.moquette.broker.ClientDescriptor;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;

public class MQTTHandler {
    private static ArrayList<InterceptPublishMessage> publishMessages = new ArrayList<>();
    private static ArrayList<InterceptPublishMessage> currentPublishMessages = new ArrayList<>();

    private static ArrayList<InterceptConnectMessage> interceptConnectMessages = new ArrayList<>();
    private static ArrayList<InterceptConnectMessage> currentConnectedMessages = new ArrayList<>();

    private static ArrayList<InterceptDisconnectMessage> interceptDisconnectMessages = new ArrayList<>();
    private static ArrayList<InterceptConnectionLostMessage> interceptConnectionLostMessages = new ArrayList<>();

    private static ArrayList<InterceptSubscribeMessage> interceptSubscribeMessages = new ArrayList<>();
    private static ArrayList<InterceptSubscribeMessage> currentSubscribeMessages = new ArrayList<>();

    private static ArrayList<InterceptUnsubscribeMessage> interceptUnsubscribeMessages = new ArrayList<>();
    private static ArrayList<InterceptAcknowledgedMessage> interceptAcknowledgedMessages = new ArrayList<>();

    private final static int brokerPort = 1883;
    private static String packet = "";


    /**
     * Intercepts all the captured packets from the broker and adds them to the appropriate list.
     * @return MQTT Handler
     */
    public InterceptHandler getHandler(){
        Class<?>[] ALL_MESSAGE_TYPES = {InterceptConnectMessage.class, InterceptDisconnectMessage.class,
                InterceptConnectionLostMessage.class, InterceptPublishMessage.class, InterceptSubscribeMessage.class,
                InterceptUnsubscribeMessage.class, InterceptAcknowledgedMessage.class};

        InterceptHandler handler = new InterceptHandler() {
            @Override
            public String getID() {
                return null;
            }

            @Override
            public Class<?>[] getInterceptedMessageTypes() {
                return ALL_MESSAGE_TYPES;
            }

            @Override
            public void onConnect(InterceptConnectMessage interceptConnectMessage) {
                interceptConnectMessages.add(interceptConnectMessage);
                currentConnectedMessages.add(interceptConnectMessage);
            }

            @Override
            public void onDisconnect(InterceptDisconnectMessage interceptDisconnectMessage) {
                interceptDisconnectMessages.add(interceptDisconnectMessage);
            }

            @Override
            public void onConnectionLost(InterceptConnectionLostMessage interceptConnectionLostMessage) {
                interceptConnectionLostMessages.add(interceptConnectionLostMessage);
            }

            @Override
            public void onPublish(InterceptPublishMessage interceptPublishMessage) {
                publishMessages.add(interceptPublishMessage);
                currentPublishMessages.add(interceptPublishMessage);
            }

            @Override
            public void onSubscribe(InterceptSubscribeMessage interceptSubscribeMessage) {
                currentSubscribeMessages.add(interceptSubscribeMessage);
            }

            @Override
            public void onUnsubscribe(InterceptUnsubscribeMessage interceptUnsubscribeMessage) {
                interceptUnsubscribeMessages.add(interceptUnsubscribeMessage);
            }

            @Override
            public void onMessageAcknowledged(InterceptAcknowledgedMessage interceptAcknowledgedMessage) {
                interceptAcknowledgedMessages.add(interceptAcknowledgedMessage);
            }
        };
        return handler;
    }

    public static ArrayList<InterceptPublishMessage> getPublishMessages() {
        return publishMessages;
    }

    public static ArrayList<InterceptConnectMessage> getInterceptConnectMessages() {
        return interceptConnectMessages;
    }

    public static ArrayList<InterceptDisconnectMessage> getInterceptDisconnectMessages() {
        return interceptDisconnectMessages;
    }

    public static ArrayList<InterceptConnectionLostMessage> getInterceptConnectionLostMessages() {
        return interceptConnectionLostMessages;
    }

    public static ArrayList<InterceptSubscribeMessage> getInterceptSubscribeMessages() {
        return interceptSubscribeMessages;
    }

    public static ArrayList<InterceptUnsubscribeMessage> getInterceptUnsubscribeMessages() {
        return interceptUnsubscribeMessages;
    }

    public static ArrayList<InterceptAcknowledgedMessage> getInterceptAcknowledgedMessages() {
        return interceptAcknowledgedMessages;
    }

    public synchronized static ArrayList<InterceptPublishMessage> getCurrentPublishMessages() {
        return currentPublishMessages;
    }

    public synchronized static ArrayList<InterceptConnectMessage> getCurrentConnectedMessages() {
        return currentConnectedMessages;
    }

    public static ArrayList<InterceptSubscribeMessage> getCurrentSubscribeMessages() {
        return currentSubscribeMessages;
    }

    /**
     * Checks if there is an ongoing attack, without confusing the SensorProfile client as an attacker.
     * @return
     */

    public synchronized static boolean isAnAttackOngoing(){
        return isAnAttackerConnected();
    }

    /**
     * Checks if a topic is published from an Attacker and updates the record.
     */
    public synchronized static void isTopicPublished(){
        boolean isMessagePublished = isMessagePublished();
        if(isMessagePublished){
            if(!currentPublishMessages.isEmpty()) {
                DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
                DAOHelper daoHelper = new DAOHelper(dbSession);
                MessageRecord record = daoHelper.getMessageRecordDAO().getLastedInsertedRecord();
                AttackRecord attackRecord = daoHelper.getAttackRecordDAO().getMatchingAttackRecord(record);
                if(attackRecord.getProtocol().equals("MQTT")) {//prevents change of packets content when two attacks occur simultaneously.
                        record.setPacket(getPublishedTopics());
                        daoHelper.getMessageRecordDAO().updateRecord(record);
                    }
                currentPublishMessages.clear();
            }
        }

    }

    private synchronized static boolean isMessagePublished(){
        return discoverPublishedMessages();
    }

    private synchronized static boolean isAnAttackerConnected(){
        return discoverOtherClients();
    }

    private synchronized static boolean discoverOtherClients(){
        CopyOnWriteArrayList<InterceptConnectMessage> clients = new CopyOnWriteArrayList<> (getCurrentConnectedMessages());
        if(!clients.isEmpty()) {
            for (Iterator<InterceptConnectMessage> iterator = clients.iterator(); iterator.hasNext();) {
                InterceptConnectMessage item = iterator.next();
                if (item != null) {
                    if (!item.getClientID().equals(SensorProfile.getClientID())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private synchronized static boolean discoverPublishedMessages(){
        CopyOnWriteArrayList<InterceptPublishMessage> clients = new CopyOnWriteArrayList<>(getCurrentPublishMessages());
        if(!clients.isEmpty()) {
            for (InterceptPublishMessage item : clients) {
                if (item != null ) {
                    if(item.getClientID() == null){
                        return true;
                    }
                    if (!item.getClientID().equals(SensorProfile.getClientID())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Gets the IP address of the current connected client
     * @return ip address
     */
    private synchronized static String getIPCurrentClient() {
        Collection<ClientDescriptor> clients = Collections.synchronizedCollection(MQTT.listConnectedClients());
        CopyOnWriteArrayList<InterceptConnectMessage> connectMessages = new CopyOnWriteArrayList<>(getInterceptConnectMessages());

        String  ipAddress = "localhost";
        if(!clients.isEmpty() && !connectMessages.isEmpty()) {
            for (ClientDescriptor item : clients) {
                if(item!=null && !item.getClientID().equals(SensorProfile.getClientID())){
                    if (connectMessages.stream().anyMatch(o -> o.getClientID().equals(item.getClientID()))) { //ArrayList preserves order of inserted items.
                        ipAddress = item.getAddress();
                    }
                }
            }
        }
        return ipAddress;
    }

    private synchronized static String getPublishedTopics(){
        CopyOnWriteArrayList<InterceptPublishMessage> publishMessages = new CopyOnWriteArrayList<>(getCurrentPublishMessages());
        CopyOnWriteArrayList<InterceptConnectMessage> connectMessages = new CopyOnWriteArrayList<>(getInterceptConnectMessages());

        if(!publishMessages.isEmpty()) {
            for (InterceptPublishMessage message : publishMessages) {
                if (message != null) {
                    if (message.getClientID() == null) {
                        return emptyClientIdPacket(message);
                    }
                    if (!message.getClientID().equals(SensorProfile.getClientID())) {
                       return fullPackets(connectMessages, message);
                    }
                }
            }
        }
        return packet;
    }

    private static String fullPackets(CopyOnWriteArrayList<InterceptConnectMessage> connectMessages, InterceptPublishMessage message){
        if (connectMessages.stream().anyMatch(o -> o.getClientID().equals(message.getClientID()))) {
            packet += "TopicName: " + message.getTopicName() + " " +
                    "Message Clientid: " + message.getClientID() +
                    "/n";
            return packet;
        }
        return packet;
    }
    /**
     * In case the client has an empty client Id
     * @param message the message that the client has
     * @return packet
     */
    private static String emptyClientIdPacket(InterceptPublishMessage message){
        return packet += "TopicName: " + message.getTopicName();
    }

    /**
     * Gets the port number that the client client connected.
     * @return port of the client
     */
    private synchronized static int getPortCurrentClient() {
        Collection<ClientDescriptor> clients = Collections.synchronizedCollection(MQTT.listConnectedClients());
        CopyOnWriteArrayList<InterceptConnectMessage> connectMessages = new CopyOnWriteArrayList<>(getInterceptConnectMessages());
        int  port = 0;
        if(!clients.isEmpty() && !connectMessages.isEmpty()) {
            for (ClientDescriptor item : clients) {
                if (item != null && !item.getClientID().equals(SensorProfile.getClientID())) {
                    if (connectMessages.stream().anyMatch(o -> o.getClientID().equals(item.getClientID()))) {
                        port = item.getPort();
                    }
                }
            }
        }
        return port;
    }

    /**
     * Removes the current client in order to monitor a new attack
     */
    public static void removeCurrentConnected(){
        if(!currentConnectedMessages.isEmpty())
            currentConnectedMessages.clear();
    }

    /**
     * Helper method for Handler, creates an attackRecord with the logs from the InterceptHandler.
     * @param attack_id attackId
     * @param externalIP externalIP
     * @param protocol protocol
     * @param subnetMask subnetMask
     * @param BSSID BSSID
     * @param internalIPAddress internalIPAddress
     * @return attack record.
     */
    public synchronized static AttackRecord createAttackRecord(Long attack_id, String externalIP, Protocol protocol,int subnetMask,String BSSID,int internalIPAddress){
        AttackRecord record = new AttackRecord();
        String internalIp = HelperUtils.intToStringIp(internalIPAddress);
        String remoteIp = getIPCurrentClient();

        record.setAttack_id(attack_id);
        record.setSync_id(attack_id);
        if(SyncDevice.currentDevice()!=null)
            record.setDevice(Objects.requireNonNull(SyncDevice.currentDevice()).getDeviceID());
        else
            record.setDevice(UUID.randomUUID().toString());
        record.setProtocol("MQTT");
        record.setExternalIP(externalIP);
        record.setLocalIP(internalIp);
        record.setLocalPort(brokerPort);
        record.setWasInternalAttack(checkIfIsInternalAttack(remoteIp,internalIp));
        record.setRemoteIP(remoteIp);
        record.setRemotePort(getPortCurrentClient());
        record.setBssid(BSSID);

        return record;
    }

    private synchronized static boolean checkIfIsInternalAttack(String remoteIPAddress,String internalIPAddress){
        if(remoteIPAddress.equals("127.0.0.1"))
            return true;
        int prefix = Hostage.prefix;
        SubnetUtils utils = new SubnetUtils(internalIPAddress+"/"+prefix);

        return utils.getInfo().isInRange(remoteIPAddress);
    }

    /**
     *Helper method for Handler, creates a messageRecord with the logs from the InterceptHandler.
     * @param type message type, SENT or RECEIVED
     * @param attack_id attack id
     * @return message record
     */
    public synchronized static MessageRecord createMessageRecord(MessageRecord.TYPE type, long attack_id) {
        MessageRecord record = new MessageRecord(true);
        record.setAttack_id(attack_id);
        record.setType(type);
        record.setStringMessageType(type.name());
        record.setTimestamp(System.currentTimeMillis());
        record.setPacket(getPublishedTopics());
        return record;
    }

}
