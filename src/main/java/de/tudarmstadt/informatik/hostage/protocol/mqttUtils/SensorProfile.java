package de.tudarmstadt.informatik.hostage.protocol.mqttUtils;

import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import de.tudarmstadt.informatik.hostage.protocol.MQTT;

public class SensorProfile {
    private double temperature = ThreadLocalRandom.current().nextDouble(10, 30 + 1);
    private int humidity= ThreadLocalRandom.current().nextInt(55, 80 + 1);
    private final static String clientId = UUID.randomUUID().toString();


    public SensorProfile() {

    }

    public void startSensor() throws Exception {
        MQTT mqtt = new MQTT();
        Mqtt3BlockingClient client = mqtt.client(clientId);
        mqtt.publish(client, "/Temperature", String.valueOf(temperature) + "°C");
        mqtt.publish(client, "/Humidity", String.valueOf(humidity) + "%");
    }


    public static String getClientID(){
        return clientId;
    }


}
