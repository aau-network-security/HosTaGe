package dk.aau.netsec.hostage.protocol.utils.mqttUtils;

import java.util.Properties;

import io.moquette.broker.config.MemoryConfig;

/**
 * This how the Moquette Conf file is structured
 *
 * ##############################################
 * #  Moquette configuration file.
 * #
 * #  The synthax is equals to mosquitto.conf
 * #
 * ##############################################
 *
 * port 1883
 *
 * websocket_port 8080
 *
 * host 0.0.0.0
 *
 * #Password file
 * password_file password_file.conf
 *
 * ssl_port 8883
 * jks_path serverkeystore.jks
 * key_store_password passw0rdsrv
 * key_manager_password passw0rdsrv
 *
 * allow_anonymous true
 *
 * reauthorize_subscriptions_on_connect false
 */

/**
 * This class initializes Moquette config file.
 */

public class MQTTConfig {
    private String portValue="1883";
    private String hostValue="0.0.0.0";
    private final String websocket_port="websocket_port";
    private final String password_file= "password_file";
    private final String ssl_port="ssl_port";
    private final String jks_path="jks_path";
    private final String key_store_password="key_store_password";
    private final String key_manager_password="key_manager_password";
    private final String allow_anonymous="allow_anonymous";
    private final String reauthorize_subscriptions_on_connect="reauthorize_subscriptions_on_connect";

    public MQTTConfig(String portValue, String hostValue, String websocket_port_value, String password_file_value, String ssl_port_value, String jks_path_value, String key_store_passwordValue, String key_manager_passwordValue, String allow_anonymous_value, String reauthorize_subscriptions_on_connectValue) {
        this.portValue = portValue;
        this.hostValue = hostValue;
    }

    public MQTTConfig(String portValue, String hostValue) {
        this.portValue = portValue;
        this.hostValue = hostValue;
    }

    public MemoryConfig configBroker(){
        MemoryConfig memoryConfig = new MemoryConfig(new Properties());
        String port = "port";
        memoryConfig.setProperty(port,this.portValue);
        String host = "host";
        memoryConfig.setProperty(host,this.hostValue);

        return memoryConfig;
    }
}
