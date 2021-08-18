package dk.aau.netsec.hostage.protocol;

import java.util.ArrayList;
import java.util.List;

import dk.aau.netsec.hostage.wrapper.Packet;


/**
 * SIP protocol. Implementation of RFC document 3261 It can handle the
 * following requests: REGISTER, INVITE, ACK, BYE. For all other requests
 * '400 Bad Request' will be replied.
 *
 * @author Wulf Pfeiffer
 */
public class SIP implements Protocol {

    private enum STATE {
        NONE, CLOSED
    }

    private STATE state = STATE.NONE;

    private static final String VERSION = "SIP/2.0";
    private static final String REGISTER = "REGISTER";
    private static final String INVITE = "INVITE";
    private static final String ACK = "ACK";
    private static final String BYE = "BYE";
    private static final String STATUS_CODE_200 = "200 OK";
    private static final String STATUS_CODE_400 = "400 Bad Request";
    private static final String STATUS_CODE_505 = "505 Version Not Supported";

    private String header;
    private String sdpPayload;

    private int port = 5060;

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean isClosed() {
        return (state == STATE.CLOSED);
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public List<Packet> processMessage(Packet requestPacket) {
        String request = null;
        if (requestPacket != null) {
            request = requestPacket.toString();
        }
        List<Packet> responsePackets = new ArrayList<>();
        String[] lines = request.split("\r\n");
        extractLines(lines);

        if (!lines[0].contains(VERSION)) {
            responsePackets.add(getVersionNotSupportedResponse());
            return responsePackets;
        } else if (lines[0].contains(REGISTER)) {
            responsePackets.add(getOkResponse());
        } else if (lines[0].contains(INVITE)) {
            responsePackets.add(getOkResponseWithSDP());
        } else if (lines[0].contains(BYE)) {
            responsePackets.add(getOkResponse());
            state = STATE.CLOSED;
        } else {
            responsePackets.add(getBadRequestResponse());
        }

        return responsePackets;
    }

    @Override
    public TALK_FIRST whoTalksFirst() {
        return TALK_FIRST.CLIENT;
    }

    @Override
    public String toString() {
        return "SIP";
    }

    private void extractLines(String[] lines) {
        header = "";
        sdpPayload = "";
        StringBuilder sbHeader = new StringBuilder();
        StringBuilder sbSdp = new StringBuilder();
        boolean recordHeader = false;
        boolean recordSdp = false;
        for (String line : lines) {
            if (line.startsWith("Via:")) {
                recordHeader = true;
            } else if (line.startsWith("Max-Forwards")) {
                recordHeader = false;
                header = sbHeader.toString();
            } else if (line.startsWith("v=")) {
                recordSdp = true;
            } else if (line.startsWith("a=")) {
                sbSdp.append(line).append("\r\n");
                sdpPayload = sbSdp.toString();
                break;
            }
            if (recordHeader) {
                sbHeader.append(line).append("\r\n");
            } else if (recordSdp) {
                sbSdp.append(line).append("\r\n");
            }
        }
    }

    private Packet getOkResponseWithSDP() {

        String sb = VERSION + " " + STATUS_CODE_200 + "\r\n" +
                header +
                "Content-Type: application/sdp\r\n" +
                "Content-Length:   " + sdpPayload.length() + "\r\n" +
                "\r\n" +
                sdpPayload;
        return new Packet(sb, toString());
    }

    private Packet getOkResponse() {

        String sb = VERSION + " " + STATUS_CODE_200 + "\r\n" +
                header +
                "Content-Length:   0\r\n";
        return new Packet(sb, toString());
    }

    private Packet getBadRequestResponse() {

        String sb = VERSION + " " + STATUS_CODE_400 + "\r\n" +
                header +
                "Content-Length:   0\r\n";
        return new Packet(sb, toString());
    }

    private Packet getVersionNotSupportedResponse() {

        String sb = VERSION + " " + STATUS_CODE_505 + "\r\n" +
                header +
                "Content-Length:   0\r\n";
        return new Packet(sb, toString());
    }

}
