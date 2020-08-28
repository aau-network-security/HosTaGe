package dk.aau.netsec.hostage.protocol;

import org.junit.Before;
import org.junit.Test;

import dk.aau.netsec.hostage.logging.formatter.protocol.ProtocolFormatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



public class ProtocolFormatterTest {

    @Before
    public void setUp() throws Exception {

    }
    @Test
    public  void  testFormat(){
        ProtocolFormatter formatter = new ProtocolFormatter();
        String samplePacket = "\\x45\\x00\\x00\\x28\\xab\\xcd\\x00\\x00\\x40" +
                "\\x06\\xa6\\xec\\x0a\\x0a\\x0a\\x02\\x0a\\x0a\\x0a\\x01\\x30\\x39" +
                "\\x00\\x50\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x50\\x02\\x71\\x1d";
        String format = formatter.format(samplePacket);

        System.out.println(format);
    }

    @Test
    public  void testGetFormatter(){
        ProtocolFormatter formatter = new ProtocolFormatter();
       // assertEquals(ProtocolFormatter.getFormatter("MySQL"),formatter);
    }

}
