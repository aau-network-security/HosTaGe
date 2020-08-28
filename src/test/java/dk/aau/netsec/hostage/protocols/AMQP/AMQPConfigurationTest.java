package dk.aau.netsec.hostage.protocols.AMQP;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.apache.qpid.server.configuration.updater.TaskExecutor;
import org.apache.qpid.server.configuration.updater.TaskExecutorImpl;
import org.apache.qpid.server.logging.EventLogger;
import org.apache.qpid.server.logging.LoggingMessageLogger;
import org.apache.qpid.server.logging.MessageLogger;
import org.apache.qpid.server.model.Broker;

import org.apache.qpid.server.model.SystemConfig;
import org.apache.qpid.server.plugin.PluggableFactoryLoader;
import org.apache.qpid.server.plugin.SystemConfigFactory;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AMQPConfigurationTest {
    private static final String INITIAL_CONFIGURATION = "qpid_embedded_inmemory_configuration.json";
    private final static String QUEUE_NAME = "hello";


    @Test
    public void testBroker()  {
        LogbackSpy logSpy = new LogbackSpy();
        logSpy.register();
        SystemConfig<?> systemConfig = createSystemConfig();

        createAndBindQueue(systemConfig);

        testClientSendConnection();
        System.out.println("Test "+logSpy.getList().get(0));
        assertNotNull(logSpy.getList());
        logSpy.unregister();
    }

    @Test
    public void testClientSendConnection(){
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
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

    private static void createAndBindQueue(final SystemConfig<?> systemConfig){
        Broker<?> broker = systemConfig.getChildByName(Broker.class,"Broker");
    }

    private static SystemConfig<?> createSystemConfig() {
        TaskExecutor taskExecutor = new TaskExecutorImpl();
        MessageLogger messageLogger = new LoggingMessageLogger();
        EventLogger eventLogger = new EventLogger();
        eventLogger.setMessageLogger(messageLogger);

        PluggableFactoryLoader<SystemConfigFactory>
                configFactoryLoader = new PluggableFactoryLoader<>(SystemConfigFactory.class);
        SystemConfigFactory configFactory = configFactoryLoader.get("Memory");
        String initialConfigurationUrl =  EmbeddedBroker.class.getClassLoader().getResource(INITIAL_CONFIGURATION).toExternalForm();
       // Map<String, String> context = new HashMap<>();
       // context.put("qpid.work_dir", workDir.getAbsolutePath());
       // context.put("qpid.amqp_port", "5999");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("initialConfigurationLocation", initialConfigurationUrl);
        //attributes.put("context", context);
        attributes.put("type", "Memory");
        attributes.put("startupLoggedToSystemOut", "true");


        SystemConfig<?> systemConfig = configFactory.newInstance(taskExecutor, eventLogger, new Principal(){
            @Override
            public String getName() {
                return "system";
            }
        }, attributes);

        systemConfig.open();

        return systemConfig;
    }
}
