package de.tudarmstadt.informatik.hostage.protocol;

import java.util.List;

import de.tudarmstadt.informatik.hostage.wrapper.Packet;

public class AMQP implements Protocol {
    private int port = 5672;
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
     * Sets the port on  the protocol.
     *
     * @param port The new port
     */
    @Override
    public void setPort(int port) {
        this.port=port;
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
        return "AMQP";
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
}
