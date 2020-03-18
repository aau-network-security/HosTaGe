package de.tudarmstadt.informatik.hostage.protocol;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.informatik.hostage.wrapper.Packet;

/**
 * ECHO protocol. Implementation of RFC document 862.
 * 
 * @author Wulf Pfeiffer
 */
public class ECHO implements Protocol {

	private int port = 7;

	@Override
	public int getPort() { return port; }

	@Override
	public void setPort(int port){ this.port = port;}

	@Override
	public boolean isClosed() {
		return true;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public List<Packet> processMessage(Packet requestPacket) {
		List<Packet> responsePackets = new ArrayList<Packet>();
		responsePackets.add(requestPacket);
		return responsePackets;
	}

	@Override
	public String toString() {
		return "ECHO";
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.CLIENT;
	}

}
