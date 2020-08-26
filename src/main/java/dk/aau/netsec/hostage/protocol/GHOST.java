package dk.aau.netsec.hostage.protocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import dk.aau.netsec.hostage.wrapper.Packet;


/**
 * Ghost Protocol. This protocol mirrors an incoming connection back to the
 * attacker on the same port, that it is running on. It will send all incoming
 * requests back to the attacker on the mirrored connection and will relpy with
 * the responses it get's from this mirrored connection.
 * 
 * @author Wulf Pfeiffer
 */
@Deprecated
public class GHOST implements Protocol {

	private boolean isClosed = false;

	private Socket mirroredConnection;

	private BufferedInputStream mirrorInputStream;

	private BufferedOutputStream mirrorOutputStream;
	
	private int currentPort;
	
	private InetAddress attackerIP;
	

	public void setCurrentPort(int currentPort) {
		this.currentPort = currentPort;
	}

	public void setAttackerIP(InetAddress attackerIP) {
		this.attackerIP = attackerIP;
	}

	private int port = 1433;

	@Override
	public int getPort() { return port; }

	@Override
	public void setPort(int port){ this.port = port;}

	@Override
	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public List<Packet> processMessage(Packet requestPacket) {
		List<Packet> responsePackets = new ArrayList<Packet>();
		try {
			if (mirroredConnection == null) {
				mirroredConnection = new Socket(attackerIP, currentPort);
				mirrorInputStream = new BufferedInputStream(
						mirroredConnection.getInputStream());
				mirrorOutputStream = new BufferedOutputStream(
						mirroredConnection.getOutputStream());
			}
			if (mirroredConnection.isInputShutdown()
					|| mirroredConnection.isOutputShutdown()) {
				mirrorInputStream.close();
				mirrorOutputStream.close();
				mirroredConnection.close();
				isClosed = true;
			}

			mirrorOutputStream.write(requestPacket.getBytes());
			mirrorOutputStream.flush();

			int availableBytes;
			while ((availableBytes = mirrorInputStream.available()) <= 0) {
				Thread.yield();
			}
			byte[] mirrorResponse = new byte[availableBytes];
			mirrorInputStream.read(mirrorResponse);
			responsePackets.add(new Packet(mirrorResponse, toString()));
		} catch (IOException e) {
			e.printStackTrace();
			responsePackets.add(requestPacket);
		}
		return responsePackets;
	}

	@Override
	public String toString() {
		return "GHOST";
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.CLIENT;
	}
}
