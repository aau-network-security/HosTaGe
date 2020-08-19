package de.tudarmstadt.informatik.hostage.protocol;

import java.util.List;

import de.tudarmstadt.informatik.hostage.wrapper.Packet;


/**
 * simple mail transfer protocol
 */
public class SMTP implements  Protocol {
    private int port = 25;

    @Override
    public int getPort() { return port; }

    @Override
    public void setPort(int port){ this.port = port;}

    public boolean isClosed() {
        return false;
    }

    public boolean isSecure() {
        return false;
    }

    public List<Packet> processMessage(Packet requestPacket) {
        return null;
    }

    public TALK_FIRST whoTalksFirst() {
        return null;
    }


    @Override
    public String toString() {
        return "SMTP";
    }
}
