package dk.aau.netsec.hostage.protocol;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.wrapper.Packet;

/**
 * TELNET protocol. Implementation of RFC document 854.
 * 
 * @author Wulf Pfeiffer
 */
public class TELNET implements Protocol {
	
	/**
	 * Represents the states of the protocol
	 */
	private enum STATE {
		NONE, OPEN, CLOSED, LOGIN, AUTHENTICATE, LOGGED_IN
	}

	/**
	 * Denotes in which state the protocol is right now
	 */
	private STATE state = STATE.NONE;

	/** user entered by the client */
	private byte[] user;

	/** last command sent by the client */
	private byte[] command;

	/** name of the server */
	private String serverName = HelperUtils.getRandomString(16, false);

	private String serverVersion = initServerVersion();
	
	private String login = initLogin();
	
	private String serverBanner = initServerBanner();
	
	/** command line prefix */
	private static byte[] sessionToken = null;

	/** options requested by the server */
	private static final byte[] optionRequest = {
			(byte) 0xff, (byte) 0xfb,
			0x03, // will suppress go ahead
			(byte) 0xff, (byte) 0xfb, 0x01 // will echo
	};

	// session token prefix, mid and suffix
	private static final byte[] sessionPrefix = { 0x1b, 0x5d, 0x30, 0x3b };

	private static final byte[] sessionMiddle = { 0x3a, 0x20, 0x7e, 0x07, 0x1b,
			0x5b, 0x30, 0x31, 0x3b, 0x33, 0x32, 0x6d };

	private static final byte[] sessionSuffix = { 0x1b, 0x5b, 0x30, 0x30, 0x6d,
			0x20, 0x1b, 0x5b, 0x30, 0x31, 0x3b, 0x33, 0x34, 0x6d, 0x7e, 0x20,
			0x24, 0x1b, 0x5b, 0x30, 0x30, 0x6d, 0x20 };

	private int port = 23;

	@Override
	public int getPort() { return port; }

	@Override
	public void setPort(int port){ this.port = port;}

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
		byte[] request = null;
		if (requestPacket != null && requestPacket.getBytes().length > 0) { // ignore empty packets
			request = requestPacket.getBytes();
		}
		List<Packet> responsePackets = new ArrayList<Packet>();

		switch (state) {
		case NONE:
			responsePackets.add(new Packet(optionRequest, toString()));
			state = STATE.OPEN;
			break;
		case OPEN:
			if (request != null) {
				responsePackets.add(new Packet(getOptionResponse(request), toString()));
				responsePackets.add(new Packet(login + "login: ", toString()));
				state = STATE.LOGIN;
			}
			break;
		case LOGIN:
			if (request == null)
				break;
			else if (checkForByte(request, (byte) 0x0d)) {
				if (request.length > 2) {
					byte[] buffer = new byte[request.length - 2];
					System.arraycopy(request, 0, buffer, 0, request.length - 2);
					user = HelperUtils.concat(user, buffer);
					responsePackets.add(new Packet(buffer, toString()));
				}
				responsePackets.add(new Packet("\r\n", toString()));
				responsePackets.add(new Packet("password: ", toString()));
				state = STATE.AUTHENTICATE;
				if (serverVersion.contains("Windows")) {
					sessionToken = HelperUtils.concat("C:\\Users\\".getBytes(), user, ">".getBytes());
				} else {
					sessionToken = HelperUtils.concat(sessionPrefix, user,
							"@".getBytes(), serverName.getBytes(), sessionMiddle,
							user, "@".getBytes(), serverName.getBytes(),
							sessionSuffix);
				}
				break;
			} else if (checkForByte(request, (byte) 0x7f) && user != null
					&& user.length != 0) {
				byte[] tmp = new byte[user.length - 1];
				System.arraycopy(user, 0, tmp, 0, user.length - 1);
				user = tmp;
				responsePackets.add(new Packet("\b \b", toString()));
				break;
			} else if (!checkForByte(request, (byte) 0xff)) {
				if (user == null)
					user = request;
				else
					user = HelperUtils.concat(user, request);
				responsePackets.add(requestPacket);
			}
			break;
		case AUTHENTICATE:
			if (request == null)
				break;
			else if (checkForByte(request, (byte) 0x0d)) {
				responsePackets.add(new Packet("\r\n"+serverBanner, toString()));
				responsePackets.add(new Packet(sessionToken, toString()));
				state = STATE.LOGGED_IN;
			} else if (checkForByte(request, (byte) 0x7f)) {
				responsePackets.add(new Packet("\b \b", toString()));
			}
			break;
		case LOGGED_IN:
			if (request == null)
				break;
			else if (checkForByte(request, (byte) 0x0d)) {
				if (request.length > 2) {
					byte[] buffer = new byte[request.length - 2];
					System.arraycopy(request, 0, buffer, 0, request.length - 2);
					command = HelperUtils.concat(command, buffer);
					responsePackets.add(new Packet(buffer, toString()));
				}
				if (command == null) {
					responsePackets.add(new Packet("\r\n", toString()));
					responsePackets.add(new Packet(sessionToken, toString()));
				} else if (new String(command).contains("exit")) {
					responsePackets.add(new Packet("\r\nlogout\r\n", toString()));
					state = STATE.CLOSED;
				} else {
					String bash = "\r\n-bash: " + new String(command)
							+ ": command not found";
					responsePackets.add(new Packet(bash, toString()));
					responsePackets.add(new Packet("\r\n", toString()));
					responsePackets.add(new Packet(sessionToken, toString()));
					command = null;
				}
			} else if (checkForByte(request, (byte) 0x7f) && command != null
					&& command.length != 0) {
				byte[] tmp = new byte[command.length - 1];
				System.arraycopy(command, 0, tmp, 0, command.length - 1);
				command = tmp;
				responsePackets.add(new Packet("\b \b", toString()));
				break;
			} else if (!checkForByte(request, (byte) 0xff)) {
				if (command == null)
					command = request;
				else
					command = HelperUtils.concat(command, request);
				responsePackets.add(requestPacket);
			}
			break;
		default:
			responsePackets.add(new Packet("\r\nlogout\r\n", toString()));
			state = STATE.CLOSED;
			break;
		}
		return responsePackets;
	}

	@Override
	public String toString() {
		return "TELNET";
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.SERVER;
	}
	
	private static String initServerVersion() {
		String sharedPreferencePath = Hostage.getContext().getString(
				R.string.shared_preference_path);
		String profile = Hostage
				.getContext()
				.getSharedPreferences(sharedPreferencePath,
						Context.MODE_PRIVATE).getString("os", "");
		return profile;
	}
	
	private String initServerBanner() {
		if (serverVersion.contains("Windows")) {
			return "\r\n*===============================================================\r\n"
					+ "Microsoft Telnet Server.\r\n"
					+ "*===============================================================\r\n";
		}
		return "";
	}
	
	private String initLogin() {
		if (serverVersion.contains("Windows")) {
			return "Welcome to Microsoft Telnet Service \r\n\r\n";
		}
		return "Debian GNU/Linux 7.0\r\n";
	}

	/**
	 * Checks a byte array for occurrence of one byte.
	 * 
	 * @param bytes
	 *            byte array that is checked.
	 * @param b
	 *            searched byte.
	 * @return true if the byte was found, else false.
	 */
	private boolean checkForByte(byte[] bytes, byte b) {
		for (byte oneByte : bytes) {
			if (oneByte == b)
				return true;
		}
		return false;
	}

	/**
	 * Determines which options that are requested by the client will be done
	 * and which not
	 * 
	 * @param request
	 *            requested options
	 * @return accepted and unaccepted options
	 */
	private byte[] getOptionResponse(byte[] request) {
		List<byte[]> responseList = new ArrayList<byte[]>();
		byte[] requestInverse;
		for (int i = 0; i < request.length - 2; i += 3) {
			if (request[i] == (byte) 0xff && request[i + 2] != 0x03
					&& request[i + 2] != 0x01) {
				requestInverse = new byte[3];
				requestInverse[0] = request[i];
				requestInverse[1] = request[i + 1] == (byte) 0xfd ? (byte) 0xfc
						: (byte) 0xfe;
				requestInverse[2] = request[i + 2];
				responseList.add(requestInverse);
			}
		}
		byte[] optionResponse = new byte[0];
		for (byte[] response : responseList) {
			optionResponse = HelperUtils.concat(optionResponse, response);
		}
		return optionResponse;
	}
}