package dk.aau.netsec.hostage.protocols.TELNET;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import dk.aau.netsec.hostage.protocol.TELNET;
import dk.aau.netsec.hostage.wrapper.Packet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TELNET.class)
public class TELNETTest {

    @Test
    public void testPackets(){
        TELNET protocol = mock(TELNET.class);
        byte[] payload = new byte [] {(byte)0x0d};
        Packet packet = new Packet(payload,"TELNET");
        List<Packet> packets = new ArrayList<>();
        packets.add(packet);
        when(protocol.processMessage(packet)).thenReturn(packets);
        assertEquals(protocol.processMessage(packet).get(0),packet);
    }
}
