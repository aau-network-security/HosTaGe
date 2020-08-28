package dk.aau.netsec.hostage.protocol.utils.coapUtils.smokeSensor;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SmokeSensorProfile {
    private final String temperature = ThreadLocalRandom.current().nextDouble(25, 35 + 1)+"°C";
    private final String[] values = {"High", "Low", "Medium", "Critical"};
    private final Random random = new Random();
    // randomly selects an index from the arr
    private final int select = random.nextInt(values.length);
    private final String abnormality = values[select];

    public String getTemperature() {
        return temperature;
    }

    public String getAbnormality() {
        return abnormality;
    }
}
