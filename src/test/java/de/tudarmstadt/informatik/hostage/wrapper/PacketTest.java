package de.tudarmstadt.informatik.hostage.wrapper;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PacketTest {
    @Test
    public void testConstructor(){
        String test = "androidTest";
        byte[] payload = test.getBytes();
        String protocol = "SMB";
        Packet packet = new Packet(payload,protocol);
        assertTrue(packet.getBytes().equals(payload));
        assertTrue(packet.getProtocol().equals(protocol));

    }

}
