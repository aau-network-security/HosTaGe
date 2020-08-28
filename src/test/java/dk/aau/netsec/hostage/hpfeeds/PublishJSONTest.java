package dk.aau.netsec.hostage.hpfeeds;

import org.junit.Test;

import java.io.IOException;

import dk.aau.netsec.hostage.publisher.Hpfeeds;
import dk.aau.netsec.hostage.publisher.Publisher;

public class PublishJSONTest {
    private static final String INITIAL_CONFIGURATION = "qpid_embedded_inmemory_configuration.json";

    @Test
    public void testPublish() throws Hpfeeds.ReadTimeOutException, Hpfeeds.EOSException, Hpfeeds.InvalidStateException, Hpfeeds.LargeMessageException, IOException {
        Publisher publisher = new Publisher();
        String initialConfigurationUrl =  PublisherTest.class.getClassLoader().getResource(INITIAL_CONFIGURATION).getPath();
        System.out.println(initialConfigurationUrl);

        publisher.setCommand("192.168.1.3",20000,"testing","secretkey","chan2",initialConfigurationUrl);

        publisher.publishFile();

    }
}
