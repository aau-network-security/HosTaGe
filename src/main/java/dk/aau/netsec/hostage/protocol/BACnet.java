package dk.aau.netsec.hostage.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import dk.aau.netsec.hostage.BACnetListener;
import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.protocol.utils.bacnetUtils.BACnetHandler;
import dk.aau.netsec.hostage.wrapper.Packet;

/**
 * BACnet protocol implementation
 */
public class BACnet implements Protocol {
    public static final int DEFAULT_PORT = 47808;
    private int port = DEFAULT_PORT;
    private DatagramSocket sock;
    private BACnetListener listener;

    @Override public int getPort() { return port; }
    @Override public void setPort(int port) { this.port = port; }
    @Override public boolean isClosed() { return sock == null || sock.isClosed(); }
    @Override public boolean isSecure() { return false; }
    @Override public TALK_FIRST whoTalksFirst() { return TALK_FIRST.CLIENT; }
    @Override public String toString() { return "BACnet"; }

    /**
     * Start UDP listener
     */
    public void startServer(long attackId) throws IOException {
        sock = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
        listener = new BACnetListener((Hostage) Hostage.getContext(), this, port, attackId);
        BACnetHandler.setListener(listener);  // UPDATED

        new Thread(() -> {
            byte[] buf = new byte[1500];
            while (!sock.isClosed()) {
                try {
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    sock.receive(dp);
                    BACnetHandler.handlePacket(dp.getAddress(), dp.getPort(), dp.getData());  // UPDATED
                } catch (IOException ignored) {}
            }
        }, "BACnet-UDP").start();
    }

    /**
     * Stop the UDP listener
     */
    public void stopServer() {
        if (sock != null && !sock.isClosed()) {
            sock.close();
        }
    }

    @Override
    public List<Packet> processMessage(Packet p) {
        throw new UnsupportedOperationException("BACnetListener handles all I/O");
    }
}
