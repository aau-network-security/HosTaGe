package dk.aau.netsec.hostage.protocol;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5MessageException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import dk.aau.netsec.hostage.protocol.utils.mqttUtils.MQTTConfig;
import dk.aau.netsec.hostage.protocol.utils.mqttUtils.MQTTHandler;
import dk.aau.netsec.hostage.wrapper.Packet;
import io.moquette.broker.ClientDescriptor;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;

import static com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance;

public class MQTT implements Protocol {

    private static final String TAG = "MQTT";
    private int port = 1883;
    private String defaultPort = "1883";
    private String defaultAddress = "0.0.0.0";
    private static final int brokerPort = 1883;
    private static boolean brokerStarted = false; //prevents the server from starting multiple times from the threads
    // private static final String MQTT_URI = "broker.mqttdashboard.com";
    private static final String MQTT_URI = "localhost";
    private static final io.moquette.broker.Server broker = new io.moquette.broker.Server();
    private MQTTHandler handler = new MQTTHandler();

    public MQTT() {
        if (!brokerStarted)
            broker();
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public List<Packet> processMessage(Packet requestPacket) {
        return null;
    }

    @Override
    public TALK_FIRST whoTalksFirst() {
        return TALK_FIRST.SERVER;
    }

    /**
     * Builds a clients connected to the MQTT_URI address and brokerPort.
     *
     * @return a MQTT5 client
     */
    public Mqtt5BlockingClient clientMQtt5() {
        Mqtt5BlockingClient client = Mqtt5Client.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost(MQTT_URI)
                .serverPort(brokerPort)
                .buildBlocking();

        return client;

    }

    /**
     * Builds a clients connected to the MQTT_URI address and brokerPort.
     *
     * @return a MQTT3 client
     */
    public Mqtt3BlockingClient client(String clientId) {
        Mqtt3BlockingClient client = Mqtt3Client.builder()
                .identifier(clientId)
                .serverHost(MQTT_URI)
                .serverPort(brokerPort)
                .buildBlocking();

        return client;

    }

    /**
     * Moquette broker starts on the localhost on port 1883
     * An intercept handler added to log connections and messages
     */
    private void broker() {
        try {
            broker.startServer(getConfig());
            broker.addInterceptHandler(handler.getHandler());

//            TODO quick hack to resolve dependency errors. Take better look at logging.
            getInstance().log("Server Started");
//            Log.d(TAG,"Server Started");
            brokerStarted = true;
        } catch (IOException e) {
            e.printStackTrace();
            brokerStarted = false;
        }
    }

    /**
     * Initializes ipAddress and port for the broker.
     *
     * @return
     */
    private MemoryConfig getConfig() {
        MQTTConfig config = new MQTTConfig(defaultPort, defaultAddress);
        return config.configBroker();
    }


    public Server getBroker() {
        return broker;
    }

    /**
     * List of connectedClients
     *
     * @return the List
     */
    public static Collection<ClientDescriptor> listConnectedClients() {

        return broker.listConnectedClients();
    }

    /**
     * Stops the broker and closes the port
     */
    public static void brokerStop() {
        if (brokerStarted) {
            broker.stopServer();
            brokerStarted = false;
        }
    }

    /**
     * Connect for the MQTT5 protocol version
     *
     * @param client
     */
    public void connectMqtt5(Mqtt5BlockingClient client) {
        client.connect();
    }

    /**
     * Connect for the MQTT3 protocol version
     *
     * @param client
     */
    public void connect(Mqtt3BlockingClient client) {
        client.connect();
    }

    /**
     * Publish for the MQTT5 protocol after connecting with the broker
     *
     * @param client
     * @param topic
     * @param payload
     */
    public void publishMQTT5(Mqtt5BlockingClient client, String topic, String payload) {
        this.connectMqtt5(client);
        client.publishWith().topic(topic).qos(MqttQos.AT_LEAST_ONCE).payload(payload.getBytes()).send();
        client.disconnect();
    }

    /**
     * Publish for the MQTT3 protocol after connecting with the broker
     *
     * @param client
     * @param topic
     * @param payload
     */
    public void publish(Mqtt3BlockingClient client, String topic, String payload) {
        this.connect(client);
        client.publishWith().topic(topic).qos(MqttQos.AT_LEAST_ONCE).payload(payload.getBytes()).send();
        client.disconnect();
    }

    /**
     * Subscribe for the MQTT5 protocol
     *
     * @param client
     * @param topic
     */
    public void subscribeMQTT5(Mqtt5BlockingClient client, String topic) {
        client.subscribeWith().topicFilter(topic).qos(MqttQos.EXACTLY_ONCE).send();
    }

    /**
     * Subscribe for the MQTT3 protocol
     *
     * @param client
     * @param topic
     */
    public void subscribe(Mqtt3BlockingClient client, String topic) {
        client.subscribeWith().topicFilter(topic).qos(MqttQos.EXACTLY_ONCE).send();
    }

    /**
     * Unsubscribe for the MQTT5 protocol
     *
     * @param client
     * @param topic
     */
    public void unSubscribeMQTT5(Mqtt5BlockingClient client, String topic) {
        client.unsubscribeWith().topicFilter(topic).send();
    }

    /**
     * Unsubscribe for the MQTT3 protocol
     *
     * @param client
     * @param topic
     */
    public void unSubscribe(Mqtt3BlockingClient client, String topic) {
        client.unsubscribeWith().topicFilter(topic).send();
    }

    /**
     * Sends an MQTT5 message
     *
     * @param client
     * @param topic
     * @param payload
     */
    public void sendMessageMQTT5(Mqtt5BlockingClient client, String topic, String payload) {
        try {
            publishMQTT5(client, topic, payload);
        } catch (Mqtt5MessageException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends an MQTT3 message
     *
     * @param client
     * @param topic
     * @param payload
     */
    public void sendMessage(Mqtt3BlockingClient client, String topic, String payload) {
        try {
            publish(client, topic, payload);
        } catch (Mqtt5MessageException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String toString() {
        return "MQTT";
    }

}
