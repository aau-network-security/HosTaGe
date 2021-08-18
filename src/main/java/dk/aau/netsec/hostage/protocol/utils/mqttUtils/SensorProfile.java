package dk.aau.netsec.hostage.protocol.utils.mqttUtils;

import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import dk.aau.netsec.hostage.protocol.MQTT;

public class SensorProfile {
    private final double temperature = ThreadLocalRandom.current().nextDouble(10, 30 + 1);
    private final int humidity= ThreadLocalRandom.current().nextInt(55, 80 + 1);
    private final static String clientId = UUID.randomUUID().toString();


    public SensorProfile() {

    }

    public void startSensor() {
        MQTT mqtt = new MQTT();
        Mqtt3BlockingClient client = mqtt.client(clientId);
        mqtt.publish(client, "/Temperature", String.valueOf(temperature) + "°C");
        mqtt.publish(client, "/Humidity", String.valueOf(humidity) + "%");
    }


    public static String getClientID(){
        return clientId;
    }


}
