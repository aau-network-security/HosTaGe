package de.tudarmstadt.informatik.hostage.protocol;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.informatik.hostage.wrapper.Packet;


/**
 * FTP protocol. Implementation of RFC document 959. It can handle the following
 * requests: USER, PASS, QUIT.
 * 
 * @author Wulf Pfeiffer
 */
public class FTP implements Protocol {

	/**
	 * Represents the states of the protocol
	 */
	private enum STATE {
		NONE, OPEN, CLOSED, USER, LOGGED_IN
	}

    /**
	 * Denotes in which state the protocol is right now
	 */
	private STATE state = STATE.NONE;

	// commands
	private static final String REPLY_CODE_220 = "220 Service ready for new user.";
	private static final String REPLY_CODE_221 = "221 Service closing control connection.";
	private static final String REPLY_CODE_230 = "230 User logged in.";
	private static final String REPLY_CODE_331 = "331 User name ok, need password.";
	private static final String REPLY_CODE_332 = "332 Need account for login.";
	private static final String REPLY_CODE_421 = "421 Service not available, closing control connection.";
	private static final String REPLY_CODE_500 = "500 Syntax error, command unrecognized.";
	private static final String REPLY_CODE_501 = "501 Syntax error in parameters or arguments";

	private int port = 21;

	@Override
	public int getPort() { return port; }

	@Override
	public void setPort(int port){ this.port = port;}

	@Override
	public boolean isClosed() {
		return state == STATE.CLOSED;
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
		List<Packet> responsePackets = new ArrayList<Packet>();
		switch (state) {
		case NONE:
			if (request == null) {
				state = STATE.OPEN;
				responsePackets.add(new Packet(REPLY_CODE_220 + "\r\n", toString()));
			} else {
				state = STATE.CLOSED;
				responsePackets.add(new Packet(REPLY_CODE_421 + "\r\n", toString()));
			}
			break;
		case OPEN:
			if (request.contains("QUIT")) {
				state = STATE.CLOSED;
				return null;
			} else if (request.equals("USER \r\n")) {
				responsePackets.add(new Packet(REPLY_CODE_501 + "\r\n", toString()));
			} else if (request.contains("USER")) {
				state = STATE.USER;
				responsePackets.add(new Packet(REPLY_CODE_331 + "\r\n", toString()));
			} else {
				responsePackets.add(new Packet(REPLY_CODE_332 + "\r\n", toString()));
			}
			break;
		case USER:
			if (request.equals("PASS \r\n")) {
				state = STATE.OPEN;
				responsePackets.add(new Packet(REPLY_CODE_501 + "\r\n", toString()));
			} else if (request.contains("PASS")) {
				state = STATE.LOGGED_IN;
				responsePackets.add(new Packet(REPLY_CODE_230 + "\r\n", toString()));
			} else {
				state = STATE.CLOSED;
				responsePackets.add(new Packet(REPLY_CODE_221 + "\r\n", toString()));
			}
			break;
		case LOGGED_IN:
			if (request != null && !request.contains("QUIT")) {
				responsePackets.add(new Packet(REPLY_CODE_500 + "\r\n", toString()));
			} else {
				state = STATE.CLOSED;
				responsePackets.add(new Packet(REPLY_CODE_221 + "\r\n", toString()));
			}
			break;
		default:
			state = STATE.CLOSED;
			responsePackets.add(new Packet(REPLY_CODE_421 + "\r\n", toString()));
		}
		return responsePackets;
	}

	@Override
	public String toString() {
		return "FTP";
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.SERVER;
	}

}
