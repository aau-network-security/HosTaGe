package de.tudarmstadt.informatik.hostage.protocol;

import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import de.tudarmstadt.informatik.hostage.protocol.smptUtils.SmtpActionType;
import de.tudarmstadt.informatik.hostage.protocol.smptUtils.SmtpMessage;
import de.tudarmstadt.informatik.hostage.protocol.smptUtils.SmtpRequest;
import de.tudarmstadt.informatik.hostage.protocol.smptUtils.SmtpResponse;
import de.tudarmstadt.informatik.hostage.protocol.smptUtils.SmtpState;
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
        List<Packet> packets = new ArrayList<>();

        SmtpState smtpState = SmtpState.CONNECT;
        SmtpRequest smtpRequest = new SmtpRequest(SmtpActionType.CONNECT, "", smtpState);
        // Execute the connection request
        SmtpResponse smtpResponse = smtpRequest.execute();
        packets.add(sendResponse(smtpResponse));
        smtpState = smtpResponse.getNextState();
        Packet packet = prepareResponse(smtpState,requestPacket);
        // Move to next internal state
        if(packet!=null)
            packets.add(packet);

        return packets;
    }

    public TALK_FIRST whoTalksFirst() {
        return TALK_FIRST.CLIENT;
    }

    @NotNull
    @Override
    public String toString() {
        return "SMTP";
    }

    /**
     * Prepare response from the server.
     *
     * @param smtpState the smtp state
     * @param requestPacket the packet from the client.
     * @return the Packet.
     */
    private Packet prepareResponse(SmtpState smtpState,Packet requestPacket){
        if (smtpState != SmtpState.CONNECT) {
            // Create request from client input and current state
            if(requestPacket!=null) {
                SmtpRequest request = SmtpRequest.createRequest(requestPacket.toString(), smtpState);
                // Execute request and create response object
                SmtpResponse response = request.execute();
                // Move to next internal state
                return sendResponse(response);
            }
        }
        return null;
    }

    /**
     * Send response to client.
     *
     * @param smtpResponse response object
     */
    private Packet sendResponse(SmtpResponse smtpResponse) {
        String message = smtpResponse.getMessage();

        return new Packet(message,toString());
    }


}
