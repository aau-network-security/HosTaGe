package dk.aau.netsec.hostage.protocols.HTTP;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import dk.aau.netsec.hostage.protocol.HTTP;
import dk.aau.netsec.hostage.wrapper.Packet;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HTTP.class)
public class HTTPTest {

    @Test
    public void testPackets() {
        HTTP protocol = mock(HTTP.class);
        Packet requestPacket = new Packet("GET me","HTTP");
        List<Packet> packets = new ArrayList<>();
        packets.add(requestPacket);
        when(protocol.processMessage(requestPacket)).thenReturn(packets);
        assertEquals(protocol.processMessage(requestPacket).get(0),requestPacket);
    }
}
