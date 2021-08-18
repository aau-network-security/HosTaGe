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
    private final String port ="port";
    private String portValue="1883";
    private final String host="host";
    private String hostValue="0.0.0.0";
    private final String websocket_port="websocket_port";
    private String websocket_port_value="8080";
    private final String password_file= "password_file";
    private String password_file_value= "password_file.conf";
    private final String ssl_port="ssl_port";
    private String ssl_port_value="8883";
    private final String jks_path="jks_path";
    private String jks_path_value="serverkeystore.jks";
    private final String key_store_password="key_store_password";
    private String key_store_passwordValue="passw0rdsrv";
    private final String key_manager_password="key_manager_password";
    private String key_manager_passwordValue="passw0rdsrv";
    private final String allow_anonymous="allow_anonymous";
    private String allow_anonymous_value="true";
    private final String reauthorize_subscriptions_on_connect="reauthorize_subscriptions_on_connect";
    private String reauthorize_subscriptions_on_connectValue="false";

    public MQTTConfig(String portValue, String hostValue, String websocket_port_value, String password_file_value, String ssl_port_value, String jks_path_value, String key_store_passwordValue, String key_manager_passwordValue, String allow_anonymous_value, String reauthorize_subscriptions_on_connectValue) {
        this.portValue = portValue;
        this.hostValue = hostValue;
        this.websocket_port_value = websocket_port_value;
        this.password_file_value = password_file_value;
        this.ssl_port_value = ssl_port_value;
        this.jks_path_value = jks_path_value;
        this.key_store_passwordValue = key_store_passwordValue;
        this.key_manager_passwordValue = key_manager_passwordValue;
        this.allow_anonymous_value = allow_anonymous_value;
        this.reauthorize_subscriptions_on_connectValue = reauthorize_subscriptions_on_connectValue;
    }

    public MQTTConfig(String portValue, String hostValue) {
        this.portValue = portValue;
        this.hostValue = hostValue;
    }

    public MemoryConfig configBroker(){
        MemoryConfig memoryConfig = new MemoryConfig(new Properties());
        memoryConfig.setProperty(this.port,this.portValue);
        memoryConfig.setProperty(this.host,this.hostValue);

        return memoryConfig;
    }
}
