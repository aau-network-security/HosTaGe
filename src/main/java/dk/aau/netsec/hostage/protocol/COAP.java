package dk.aau.netsec.hostage.protocol;

import com.mbed.coap.client.CoapClient;
import com.mbed.coap.client.CoapClientBuilder;
import com.mbed.coap.server.CoapServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

import dk.aau.netsec.hostage.persistence.ProfileManager;
import dk.aau.netsec.hostage.protocol.utils.coapUtils.COAPHandler;
import dk.aau.netsec.hostage.protocol.utils.coapUtils.smokeSensor.SmokeSensorProfile;
import dk.aau.netsec.hostage.wrapper.Packet;

public class COAP implements Protocol {
    private final static String defaultAddress = "0.0.0.0";//change to your IP.
    private final static int defaultPort = 5683;
    private int port = 5683;
    private static boolean serverStarted = false; //prevents the server from starting multiple times from the threads
    private static InetAddress address = null;

    static {
        try {
            address = InetAddress.getByName(defaultAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private final static CoapServer server = CoapServer.builder().transport(address, defaultPort).build();

    public COAP() throws IOException {
        if (!serverStarted)
            startServerMode();
    }

    private boolean enabledProfile() {
        return ProfileManager.getInstance().getCurrentActivatedProfile().mId == 15;
    }

    private void startServerMode() throws IOException {
        SmokeSensorProfile profile = new SmokeSensorProfile();
        if (enabledProfile())
            startServerProfile(profile.getTemperature(), profile.getAbnormality());
        else
            startServer();
    }

    /**
     * Returns the port on which the protocol is running.
     *
     * @return The port used by the protocol (1-65535)
     */
    @Override
    public int getPort() {
        return port;
    }

    /**
     * Sets the port on the protocol.
     *
     * @param port The new port
     */
    @Override
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns whether the communication is ended and the connection should be
     * closed or not.
     *
     * @return true if the connection should be closed, false otherwise.
     */
    @Override
    public boolean isClosed() {
        return false;
    }

    /**
     * Returns whether the protocol uses a secure connection or not.
     *
     * @return true if SSL/TLS is used, false otherwise.
     */
    @Override
    public boolean isSecure() {
        return false;
    }

    /**
     * Determines the next response.
     *
     * @param requestPacket Last message received from the client.
     * @return Message to be sent to the client.
     */
    @Override
    public List<Packet> processMessage(Packet requestPacket) {
        return null;
    }

    /**
     * Returns the name of the protocol.
     *
     * @return String representation of the protocol.
     */
    @Override
    public String toString() {
        return "COAP";
    }

    /**
     * Specifies who starts the communication once the connection is
     * established.
     *
     * @return A value in TALK_FIRST.
     */
    @Override
    public TALK_FIRST whoTalksFirst() {
        return TALK_FIRST.SERVER;
    }

    /**
     * Starts the CoAP server.
     *
     * @throws IOException
     */
    public CoapServer startServer() throws IOException {
        server.addRequestHandler("/*", new COAPHandler());
        server.start();
        serverStarted = true;

        return server;
    }

    /**
     * Starts the CoAP server for a Profile.
     *
     * @throws IOException
     */
    public CoapServer startServerProfile(String temperature, String abnormality) throws IOException {
        server.addRequestHandler("/temp", new COAPHandler(temperature));
        server.addRequestHandler("/abnormality", new COAPHandler(abnormality));
        server.start();
        serverStarted = true;

        return server;
    }

    /**
     * Returns a simple CoAP client.
     *
     * @param serverAddress the server address with the port.
     * @param server        the server instance.
     */
    public CoapClient getSimpleClient(InetSocketAddress serverAddress, CoapServer server) {

        return CoapClientBuilder.clientFor(serverAddress, server);
    }

    /**
     * Stops the server and closes the port
     */
    public static void serverStop() {
        if (serverStarted) {
            server.stop();
            serverStarted = false;
        }
    }

}
