package de.tudarmstadt.informatik.hostage.protocols.SIP;

import org.junit.Test;


import de.tudarmstadt.informatik.hostage.protocol.SIP;
import de.tudarmstadt.informatik.hostage.wrapper.Packet;

import static org.junit.Assert.assertEquals;

public class SIPTest {
    @Test
    public void testRegister(){
        SIP protocol = new SIP();
        String payload = "SIP/2.0"+"\n"+"REGISTER";
        Packet packet = new Packet(payload,"SIP");
        assertEquals(protocol.processMessage(packet).size(),1);
        assertEquals(protocol.processMessage(packet).get(0).toString()," SIP/2.0 200 OK\n" +
                " Content-Length:   0\n");
    }
}
