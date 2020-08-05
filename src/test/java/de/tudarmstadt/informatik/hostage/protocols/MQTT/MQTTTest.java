package de.tudarmstadt.informatik.hostage.protocols.MQTT;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;
import io.moquette.broker.config.MemoryConfig;

import static com.ibm.icu.impl.Assert.fail;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class MQTTTest {
    final String host = "broker.mqttdashboard.com";
    final int port = 1883;
    final String clientIdSubscribe = "clientIdSub";
    final String clientIdPublish = "clientIdPub";
    final String topic = "subjectdirectory.changed";
    final String message = "de.tudarmstadt.informatik.hostage.test payload";
    final int publishCount = 5000;
    String ipaddress="localhost";
    static ArrayList<InterceptPublishMessage> messages = new ArrayList<>();
    MemoryConfig memoryConfig = new MemoryConfig(new Properties());

    io.moquette.broker.Server server = new io.moquette.broker.Server();
    Mqtt3BlockingClient client;

    @Before
    public void setUp() throws IOException {
        memoryConfig.setProperty("host",ipaddress);
        memoryConfig.setProperty("port","1883");
        server.startServer(memoryConfig);

        server.addInterceptHandler(handler());

        client= Mqtt3Client.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost(ipaddress)
                .serverPort(1883)
                .buildBlocking();

    }

    @Test
    public void testManyPublishes() {
        final Mqtt3AsyncClient publisher = createAndConnect(this.clientIdPublish);
        final Mqtt3AsyncClient subscriber = createAndConnect(this.clientIdSubscribe);

        AtomicInteger count2 =new AtomicInteger(0);
        final TestHandler subscriptionHandler = new TestHandler();

        subscribeBlocking(subscriber, subscriptionHandler);
        publishMessages(publisher, count2);
        waitSomeSeconds(1000);

        assertEquals(this.publishCount, count2.get());
    }


    @Test
    public void testconnect() throws Exception {

        assertEquals("SUCCESS",client.connect().getReturnCode().name());

    }

    @Test
    public void testBroker() throws Exception {

        assertEquals("SUCCESS",client.connect().getReturnCode().name());

    }


    @Test
    public void testInitialConnection(){
        Mqtt3BlockingClient client = Mqtt3Client.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost(ipaddress)
                .serverPort(1883)
                .buildBlocking();

        //client.connectWith().keepAlive(20).send();

        assertEquals("SUCCESS",client.connect().getReturnCode().name());

    }

    @Test
    public void testMoquette() throws Exception{

        assertEquals("SUCCESS",client.connect().getReturnCode().name());
        client.publishWith().topic(topic).qos(MqttQos.AT_LEAST_ONCE).payload("de/tudarmstadt/informatik/hostage/fragment".getBytes()).send();


        assertEquals(1,messages.size());

        assertEquals(topic,messages.get(0).getTopicName());

    }


    private Mqtt3AsyncClient createAndConnect(final String clientId) {
        // we have create the connection in the sync thread
        final Mqtt3BlockingClient blockingClient =
                MqttClient.builder().useMqttVersion3().identifier(clientId).automaticReconnectWithDefaultConfig().serverHost(this.host).serverPort(this.port).buildBlocking();
        // and now we will switch to async client.
        return this.connect(blockingClient);
    }

    private Mqtt3AsyncClient connect(final Mqtt3BlockingClient blockingClient) {
        blockingClient.connectWith().simpleAuth().username("user").password("password".getBytes()).applySimpleAuth().cleanSession(false).send();
        return blockingClient.toAsync();
    }

    private void subscribe(final Mqtt3AsyncClient client, final TestHandler handler) {
        client.subscribeWith()
                .topicFilter(this.topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(publish -> {
                    handler.onMessage(publish.getPayloadAsBytes()); })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        System.out.println("failSub");
                        //assertThrows(throwable);
                    }
                });

    }

    private void subscribeBlocking(final Mqtt3AsyncClient client, final TestHandler handler) {
        client.subscribeWith()
                .topicFilter(this.topic)
                .qos(MqttQos.AT_MOST_ONCE)
                .callback(publish -> {
                    handler.onMessage(publish.getPayloadAsBytes()); })
                .send().join();
    }


    private void publishMessages(final Mqtt3AsyncClient client, AtomicInteger count2) {

        for (int i = 0; i < this.publishCount; i++) {
            final String payload = this.message.concat(String.valueOf(i));
            client.publishWith().topic(this.topic).payload(payload.getBytes())
                    .qos(MqttQos.AT_LEAST_ONCE).send()
                    .whenComplete((mqtt3Publish, throwable) -> {
                        if (throwable != null) {
                            System.out.println("fail");
                            //fail(throwable);
                        } else {
                            System.out.println("message arrived: " + new String(payload) );
                            count2.incrementAndGet();
                        }

                    });
        }
    }

    private void waitSomeSeconds(final int secs) {
        try {
            Thread.sleep(secs * 10);
        } catch (final InterruptedException e) {
            fail(e);
        }
    }

    static class TestHandler {

        private AtomicInteger count=new AtomicInteger(0);

        public TestHandler() {
        }

        public void onMessage(final byte[] payload) {
            System.out.println("message arrived: " + new String(payload) );
            count.incrementAndGet();
        }

        public int getCallCount() {
            return this.count.get();
        }
    }

    private InterceptHandler handler(){
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

                //messages.add(interceptConnectMessage);

            }

            @Override
            public void onDisconnect(InterceptDisconnectMessage interceptDisconnectMessage) {

            }

            @Override
            public void onConnectionLost(InterceptConnectionLostMessage interceptConnectionLostMessage) {

            }

            @Override
            public void onPublish(InterceptPublishMessage interceptPublishMessage) {
                messages.add(interceptPublishMessage);

            }

            @Override
            public void onSubscribe(InterceptSubscribeMessage interceptSubscribeMessage) {

            }

            @Override
            public void onUnsubscribe(InterceptUnsubscribeMessage interceptUnsubscribeMessage) {

            }

            @Override
            public void onMessageAcknowledged(InterceptAcknowledgedMessage interceptAcknowledgedMessage) {

            }
        };

        return handler;
    }

    @After
    public void tearDown(){
        server.stopServer();
    }
}
