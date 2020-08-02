package de.tudarmstadt.informatik.hostage.protocols;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;


public class AMQPTest {
    private final static String QUEUE_NAME = "hello";


    @Before
    public void testBroker() throws IOException, TimeoutException {
//        EmbeddedBroker broker = new EmbeddedBroker();
//        try {
//            broker.start();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Test
    public void testClientSendConnection(){
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                String message = "Hello World!";
                channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));

                System.out.println(" [x] Sent '" + message + "' "+channel.getChannelNumber());

                assertEquals(channel.getChannelNumber(),1);
                assertEquals(connection.getAddress().toString(),"/127.0.0.1");
        } catch (TimeoutException | IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testClientReceiveConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
        };
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
        }
}



