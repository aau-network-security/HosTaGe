package de.tudarmstadt.informatik.hostage.protocol;

import org.apache.qpid.server.SystemLauncher;
import org.apache.qpid.server.configuration.updater.TaskExecutor;
import org.apache.qpid.server.configuration.updater.TaskExecutorImpl;
import org.apache.qpid.server.logging.EventLogger;
import org.apache.qpid.server.logging.LoggingMessageLogger;
import org.apache.qpid.server.logging.MessageLogger;
import org.apache.qpid.server.model.Broker;
import org.apache.qpid.server.model.SystemConfig;
import org.apache.qpid.server.plugin.PluggableFactoryLoader;
import org.apache.qpid.server.plugin.SystemConfigFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;


import de.tudarmstadt.informatik.hostage.protocol.amqpUtils.LogBackWatcher;
import de.tudarmstadt.informatik.hostage.wrapper.Packet;

public class AMQP implements Protocol {
    private int port = 5672;
    private static final String INITIAL_CONFIGURATION = "qpid_embedded_inmemory_configuration.json";
    private static boolean brokerStarted = false; //prevents the server from starting multiple times from the threads
    private static final SystemLauncher systemLauncher = new SystemLauncher();
    private static final LogBackWatcher watcher = new LogBackWatcher();

    public AMQP(){
        if(!brokerStarted){
            watcher.register();//starts to watch the Logs of the broker.
            start();
        }
    }

    /**
     * Returns the port on which the protocol is running.
     *
     * @return The port used by the protocol (1-65535)
     */
    @Override
    public int getPort() {
        return port;
    }

    /**
     * Sets the port on  the protocol.
     *
     * @param port The new port
     */
    @Override
    public void setPort(int port) {
        this.port=port;
    }

    /**
     * Returns whether the communication is ended and the connection should be
     * closed or not.
     *
     * @return true if the connection should be closed, false otherwise.
     */
    @Override
    public boolean isClosed() {
        return false;
    }

    /**
     * Returns whether the protocol uses a secure connection or not.
     *
     * @return true if SSL/TLS is used, false otherwise.
     */
    @Override
    public boolean isSecure() {
        return false;
    }

    /**
     * Determines the next response.
     *
     * @param requestPacket Last message received from the client.
     * @return Message to be sent to the client.
     */
    @Override
    public List<Packet> processMessage(Packet requestPacket) {
        return null;
    }

    /**
     * Returns the name of the protocol.
     *
     * @return String representation of the protocol.
     */
    @Override
    public String toString() {
        return "AMQP";
    }

    /**
     * Specifies who starts the communication once the connection is
     * established.
     *
     * @return A value in TALK_FIRST.
     */
    @Override
    public TALK_FIRST whoTalksFirst() {
        return TALK_FIRST.SERVER;
    }

    /**
     * Starts the broker
     * This method has issues with com.sun.management.OperatingSystemMXBean that is currently not
     * supported for Android!
     * @param systemConfig default custom configuration.
     */
    private static void startAlternative(final SystemConfig<?> systemConfig){
        Broker<?> broker = systemConfig.getChildByName(Broker.class,"Broker");
        brokerStarted=true;
    }

    /**
     * Stops the broker.
     */
    public static void stopBroker(){
        watcher.unregister();
        systemLauncher.shutdown();
    }
    /**
     * Starts the broker
     */
    public void start(){
        try {
            systemLauncher.startup(getBrokerAttributes());
        }catch (Exception e){
            e.printStackTrace();
        }
        brokerStarted=true;
    }

    /**
     * This method loads the system config from the json file located in resources.
     * Loggers can be added or removed at runtime, without restarting the Broker.
     * However changes to a Logger's configuration such as filenames and rolling options don't take effect until the next restart.
     * Changes to a Logger's inclusion rules take effect immediately.
     * @return System configuration
     */
    private static SystemConfig<?> createSystemConfig() {
        TaskExecutor taskExecutor = new TaskExecutorImpl();
        MessageLogger messageLogger = new LoggingMessageLogger();
        EventLogger eventLogger = new EventLogger();
        eventLogger.setMessageLogger(messageLogger);

        PluggableFactoryLoader<SystemConfigFactory> configFactoryLoader = new PluggableFactoryLoader<>(SystemConfigFactory.class);
        SystemConfigFactory configFactory = configFactoryLoader.get("Memory");


        SystemConfig<?> systemConfig = configFactory.newInstance(taskExecutor, eventLogger, () -> "system", getBrokerAttributes());
        systemConfig.open();

        return systemConfig;
    }

    /**
     * Loads the config attributes.
     * @return attributes for the Broker.
     */
    private static Map<String,Object> getBrokerAttributes(){
        String initialConfigurationUrl =  AMQP.class.getClassLoader().getResource(INITIAL_CONFIGURATION).toExternalForm();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SystemConfig.INITIAL_CONFIGURATION_LOCATION, initialConfigurationUrl);

        attributes.put("type", "Memory");
        attributes.put("startupLoggedToSystemOut", "true");

        return attributes;
    }

}
