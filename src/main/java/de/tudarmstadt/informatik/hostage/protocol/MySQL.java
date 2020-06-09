package de.tudarmstadt.informatik.hostage.protocol;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.wrapper.Packet;


/**
 * MySQL protocol.
 * Implementation of https://dev.mysql.com/doc/internals/en/client-server-protocol.html.
 * @author Wulf Pfeiffer
 */
public class MySQL implements Protocol {
	/**
	 * Represents the states of the protocol
	 */
	private enum STATE {
		NONE, CONNECTED, LOGIN_INFO, CLOSED
	}

	/**
	 * Denotes in which state the protocol is right now
	 */
	private STATE state = STATE.NONE;

	/** last request from client */
	private byte[] lastReceivedMessage;

	// version stuff
	private String[][][] possibleMysqlVersions = {
			{ { "5.7." }, { "1", "2" } },
			{
					{ "5.6." },
					{ "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
							"13", "14" } },
			{ { "5.5." }, { "27", "28", "29", "30", "31", "32", "33", "34" } } };

	private String serverVersion = initMysqlVersion();

	private String initMysqlVersion() {
		SecureRandom rndm = new SecureRandom();
		int majorVersion = rndm.nextInt(possibleMysqlVersions.length);
		return possibleMysqlVersions[majorVersion][0][0]
				+ possibleMysqlVersions[majorVersion][1][rndm
						.nextInt(possibleMysqlVersions[majorVersion][1].length)];
	}

	private int port = 3306;

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
		byte[] request = null;
		if (requestPacket != null) {
			request = requestPacket.getBytes();
		}
		List<Packet> responsePackets = new ArrayList<Packet>();
		if (request != null)
			lastReceivedMessage = request;

		switch (state) {
		case NONE:
			responsePackets.add(greeting());
			state = STATE.CONNECTED;
			break;
		case CONNECTED:
			responsePackets.add(responseOK());
			state = STATE.LOGIN_INFO;
			break;
		case LOGIN_INFO:
			if (this.lastReceivedMessage[4] == 0x01) {
				state = STATE.CLOSED;
			} else {
				responsePackets.add(responseError());
			}
			break;
		default:
			state = STATE.CLOSED;
			break;
		}

		return responsePackets;
	}

	@Override
	public String toString() {
		return "MySQL";
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.SERVER;
	}

	/**
	 * Builds the greeting packet that the server sends as first packet
	 * 
	 * @return greeting packet
	 */
	private Packet greeting() {
		byte[] protocol = { 0x0a };
		byte[] version = serverVersion.getBytes();
		byte[] versionFin = { 0x00 };
		byte[] thread = { 0x2a, 0x00, 0x00, 0x00 };
		byte[] salt = { 0x44, 0x64, 0x49, 0x7e, 0x60, 0x48, 0x25, 0x7e, 0x00 };
		byte[] capabilities = { (byte) 0xff, (byte) 0xf7 };
		byte[] language = { 0x08 };
		byte[] status = { 0x02, 0x00 };
		byte[] unused = { 0x0f, (byte) 0x80, 0x15, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] salt2 = { 0x6c, 0x26, 0x71, 0x2c, 0x25, 0x72, 0x31, 0x3d, 0x7d,
				0x21, 0x26, 0x3b, 0x00 };
		String payload = "mysql_native_password";
		byte[] fin = { 0x00 };

		byte[] response = HelperUtils.concat(protocol, version, versionFin,
				thread, salt, capabilities, language, status, unused, salt2,
				payload.getBytes(), fin);
		return wrapPacket(response);
	}

	/**
	 * Builds the error-response packet
	 * 
	 * @return error-response packet
	 */
	private Packet responseError() {
		byte[] fill1 = { (byte) 0xff };
		byte[] code = { 0x17, 0x04 };
		byte[] fill2 = { 0x23 };
		String state = "08S01";
		String msg = "Unknown command";

		byte[] response = HelperUtils.concat(fill1, code, fill2,
				state.getBytes(), msg.getBytes());
		return wrapPacket(response);
	}

	/**
	 * Builds the ok-response packet
	 * 
	 * @return ok-response packet
	 */
	private Packet responseOK() {
		byte[] affectedRows = { 0x00, 0x00, 0x00 };
		byte[] status = { 0x02, 0x00 };
		byte[] warnings = { 0x00, 0x00 };

		byte[] response = HelperUtils.concat(affectedRows, status, warnings);
		return wrapPacket(response);
	}

	/**
	 * Wraps the response packet with the packet length and number
	 * 
	 * @param response
	 *            that is wrapped
	 * @return wrapped packet
	 */
	private Packet wrapPacket(byte[] response) {
		if (response.length == 0){

		}
		byte[] buffer = ByteBuffer.allocate(4).putInt(response.length).array();
		byte[] packetLength = { buffer[3], buffer[2], buffer[1] };
		byte[] packetNumber = new byte[1];
		if (lastReceivedMessage != null)
			packetNumber[0] = (byte) (lastReceivedMessage[3] + 1);
		else
			packetNumber[0] = 0x00;

		byte[] wrappedResponse = HelperUtils.concat(packetLength, packetNumber,
				response);
		return new Packet(wrappedResponse, toString());
	}
}
