package de.tudarmstadt.informatik.hostage.protocols.SSH;

import org.junit.Test;

import de.tudarmstadt.informatik.hostage.protocol.SSH;
import de.tudarmstadt.informatik.hostage.wrapper.Packet;

import static org.junit.Assert.assertEquals;

public class SSHTest {
    @Test
    public void testPackets(){
        SSH protocol = new SSH();
        byte[] payload = new byte [] {(byte)0x0d};

        Packet packet = new Packet(payload,"SSH");
        Packet expectedResponse = new Packet("53, 53, 48, 2D, 32, 2E, 30, 2D, 4F, 70, 65, 6E, 53, 53, 48, 5F, 36, 2E, 31, 0D, 0A","SSH");
        assertEquals(protocol.processMessage(packet).size(),1);
    }
}
