package dk.aau.netsec.hostage.protocol.utils.cifs.smbutils;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import dk.aau.netsec.hostage.commons.HelperUtils;


/**
 * SMBPacket
 * 
 * @author Wulf Pfeiffer
 */
public class SMBPacket {
	
	private String[] serverVersion;
	private static byte[] serverName;
	private byte[] message = null;
	private static final byte[] serverGUID = HelperUtils.randomBytes(16);
	private boolean authenticateNext = false;

	// components of a SMB packet
	private byte[] serverComp = new byte[4];
	private byte[] smbCommand = new byte[1];
	private byte[] ntStat = new byte[4];
	private byte[] smbFlags = new byte[1];
	private byte[] smbFlags2 = new byte[2];
	private byte[] processIDHigh = new byte[2];
	private byte[] signature = new byte[8];
	private byte[] reserved = new byte[2];
	private byte[] treeID = new byte[2];
	private byte[] processID = new byte[2];
	private byte[] userID = new byte[2];
	private byte[] multiplexID = new byte[2];
	
	//special nbds stuff
	private static byte[] workgroup;
	private int type;
	
	public SMBPacket(String[] serverVersion, String serverName, String workgroup) {
		this.serverVersion = serverVersion;
		SMBPacket.serverName = serverName.getBytes();
		SMBPacket.workgroup = workgroup.getBytes();
	}
	
	public void prepareNextResponse(int type) {
		serverComp = new byte[] { (byte) 0xff, 0x53, 0x4d, 0x42 };
		smbCommand = new byte[] { 0x25 };
		ntStat = new byte[] { 0x00, 0x00, 0x00, 0x00 };
		// | 0x80 for mark response bit
		smbFlags = new byte[] { 0x00 }; 
		smbFlags2 = new byte[] { 0x00, 0x00 };
		processIDHigh = new byte[] { 0x00, 0x00 };
		signature = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		reserved = new byte[] { 0x00, 0x00 };
		treeID = new byte[] { 0x00, 0x00 };
		processID = new byte[] { 0x00, 0x00 };
		userID = new byte[] { 0x00, 0x00 };
		multiplexID = new byte[] { 0x00, 0x00 };
		this.type = type;
	}
	
	public void prepareNextResponse(byte[] message) {
		this.message = message;
		serverComp = new byte[] { message[4], message[5], message[6],message[7] };
		smbCommand = new byte[] { message[8] };
		ntStat = new byte[] { message[9], message[10], message[11], message[12] };
		// | 0x80 for mark response bit
		smbFlags = new byte[] { (byte) (message[13] | 0x80) }; 
		smbFlags2 = new byte[] { message[14], message[15] };
		processIDHigh = new byte[] { message[16], message[17] };
		signature = new byte[] { message[18], message[19], message[20],
				message[21], message[22], message[23], message[24], message[25] };
		reserved = new byte[] { message[26], message[27] };
		treeID = new byte[] { message[28], message[29] };
		processID = new byte[] { message[30], message[31] };
		userID = new byte[] { message[32], message[33] };
		multiplexID = new byte[] { message[34], message[35] };
	}
	
	/**
	 * Wraps the header around a response
	 * 
	 * @param response
	 *            that is wrapped
	 * @return wrapped response
	 */
	private byte[] wrapHeader(byte[] response) {
		byte[] header = new byte[0];
		return HelperUtils.concat(header, serverComp, smbCommand, ntStat,
				smbFlags, smbFlags2, processIDHigh, signature, reserved,
				treeID, processID, userID, multiplexID, response);
	}

	/**
	 * Wraps the Netbios header around a response
	 * 
	 * @param response
	 *            that is wrapped
	 * @return wrapped response
	 */
	private byte[] wrapNetbios(byte[] response) {
		byte[] netbios = { 0x00 };
		// allocate(4) because int is 4 bytes long
		byte[] buffer = ByteBuffer.allocate(4).putInt(response.length).array();
		// only bytes 1-3 needed, byte 0 is not  needed
		byte[] netbiosLength = { buffer[1], buffer[2], buffer[3] }; 
		return HelperUtils.concat(netbios, netbiosLength, response);
	}

	/**
	 * Evaluates what Dialects are offered by the client and which position the
	 * used NT LM 0.12 dialect is at
	 * 
	 * @return position of the NT LM 0.12 dialect
	 */
	private byte[] evaluateDialect() {
		byte[] dialectMsg = new byte[message.length - 39];
		System.arraycopy(message, 39, dialectMsg, 0, message.length - 39);
		short dialectNumber = 0;
		for (int i = 0, start = 0; i < dialectMsg.length; i++) {
			if (dialectMsg[i] == 0x00) {
				byte[] dialect = new byte[i - start];
				System.arraycopy(dialectMsg, start, dialect, 0, i - start);
				if (HelperUtils.byteToStr(dialect).contains("NT LM 0.12")) {
					return new byte[] { (byte) dialectNumber,
							(byte) (dialectNumber >> 8) };
				}
				start = i + 1;
				dialectNumber++;
			}
		}
		return new byte[] { 0x00, 0x00 };
	}

	/**
	 * Builds the close packet
	 * 
	 * @return close packet
	 */
	public byte[] getClose() {
		byte[] wordCount = { 0x00 };
		byte[] byteCount = { 0x00, 0x00 };

		smbCommand = new byte[] { 0x04 };

		byte[] response = HelperUtils.concat(wordCount, byteCount);

		return wrapNetbios(wrapHeader(response));
	}

	/**
	 * Builds the DCERPC packet
	 * 
	 * @return DCERPC packet
	 */
	public byte[] getDceRpc(byte[] transSub, int length) {
		byte[] majorVersion = { 0x05 };
		byte[] minorVersion = { 0x00 };
		byte[] packetType = null;
		byte[] packetFlags = { 0x03 };
		byte[] dataRepres = { 0x10, 0x00, 0x00, 0x00 };
		byte[] fragLength = null;
		byte[] authLength = { 0x00, 0x00 };
		byte[] callID = null;
		byte[] response = null;

		if (transSub[0] == 0x00 && transSub[1] == 0x0b) {
			packetType = new byte[] { 0x0c };
			fragLength = new byte[] { 0x44, 0x00 };
			callID = new byte[] { 0x01, 0x00, 0x00, 0x00 };
			byte[] maxXmitFrag = { (byte) 0xb8, 0x10 };
			byte[] maxRecvFrag = { (byte) 0xb8, 0x10 };
			byte[] assocGroup = { 0x4a, 0x41, 0x00, 0x00 };
			byte[] scndryAddrLen = { 0x0d, 0x00 };
			byte[] scndryAddr = { 0x5c, 0x50, 0x49, 0x50, 0x45, 0x5c, 0x73,
					0x72, 0x76, 0x73, 0x76, 0x63, 0x00, 0x00 };
			byte[] numResults = { 0x01, 0x00, 0x00, 0x00 };
			byte[] ctxItem = { 0x00, 0x00, 0x00, 0x00, 0x04, 0x5d, (byte) 0x88,
					(byte) 0x8a, (byte) 0xeb, 0x1c, (byte) 0xc9, 0x11,
					(byte) 0x9f, (byte) 0xe8, 0x08, 0x00, 0x2b, 0x10, 0x48,
					0x60, 0x02, 0x00, 0x00, 0x00 };

			response = HelperUtils.concat(majorVersion, minorVersion,
					packetType, packetFlags, dataRepres, fragLength,
					authLength, callID, maxXmitFrag, maxRecvFrag, assocGroup,
					scndryAddrLen, scndryAddr, numResults, ctxItem);
		} else if (transSub[0] == 0x00 && transSub[1] == 0x00) {
			packetType = new byte[] { 0x02 };
			byte[] tmp = ByteBuffer.allocate(4).putInt(length).array();
			fragLength = new byte[] { tmp[3], tmp[2] };
			callID = new byte[] { 0x02, 0x00, 0x00, 0x00 };
			tmp = ByteBuffer.allocate(4).putInt(length - 24).array();
			byte[] allocHint = new byte[] { tmp[3], tmp[2], tmp[1], tmp[0] };
			byte[] contextID = { 0x00, 0x00 };
			byte[] cancelCount = { 0x00, 0x00 };

			response = HelperUtils.concat(majorVersion, minorVersion,
					packetType, packetFlags, dataRepres, fragLength,
					authLength, callID, allocHint, contextID, cancelCount);
		}
		return response;
	}

	/**
	 * Builds the echo packet
	 * 
	 * @return echo packet
	 */
	public byte[] getEcho() {
		byte[] wordCount = { 0x01 };
		byte[] echoSeq = { 0x01, 0x00 };
		byte[] byteCount = { 0x10, 0x00 };
		byte[] echoData = { (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0,
				(byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0,
				(byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0,
				(byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0 };
		byte[] response = HelperUtils.concat(wordCount, echoSeq, byteCount,
				echoData);
		return wrapNetbios(wrapHeader(response));
	}

	/**
	 * Builds the negotiate packet
	 * 
	 * @return negotiate packet
	 */
	public byte[] getNego() {
		byte[] wordCount = { 0x11 };
		byte[] dialect = evaluateDialect();
		byte[] secMode = { 0x03 };
		byte[] maxMpxC = { 0x32, 0x00 };
		byte[] maxVcs = { 0x01, 0x00 };
		byte[] maxBufSize = { 0x04, 0x11, 0x00, 0x00 };
		byte[] maxRawBuf = { 0x00, 0x00, 0x01, 0x00 };
		byte[] sessionKey = { 0x00, 0x00, 0x00, 0x00 };
		byte[] capabilities = { (byte) 0xfc, (byte) 0xe3, 0x01, (byte) 0x80 };
		byte[] sysTime = getTimeInBytes();
		byte[] timeZone = getTimeZoneInBytes();
		byte[] keyLength = { 0x00 };
		byte[] byteCount = { 0x3a, 0x00 };
		byte[] guid = serverGUID;
		byte[] secBlob = { 0x60, 0x28, 0x06, 0x06 };
		byte[] oid = { 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02 };
		byte[] protectNeg = { (byte) 0xa0, 0x1e };
		byte[] negToken = { 0x30, 0x1c, (byte) 0xa0, 0x1a, 0x30, 0x18 };
		byte[] mechType = { 0x06, 0x0a, 0x2b, 0x06, 0x01, 0x04, 0x01,
				(byte) 0x82, 0x37, 0x02, 0x02, 0x1e };
		byte[] mechType2 = { 0x06, 0x0a, 0x2b, 0x06, 0x01, 0x04, 0x01,
				(byte) 0x82, 0x37, 0x02, 0x02, 0x0a };

		byte[] response = HelperUtils.concat(wordCount, dialect, secMode,
				maxMpxC, maxVcs, maxBufSize, maxRawBuf, sessionKey,
				capabilities, sysTime, timeZone, keyLength, byteCount, guid,
				secBlob, oid, protectNeg, negToken, mechType, mechType2);
		return wrapNetbios(wrapHeader(response));
	}

	/**
	 * Builds the nt create packet
	 * 
	 * @return nt create packet
	 */
	public byte[] getNTCreate() {
		byte[] wordCount = { 0x22 };
		byte[] andXCommand = { (byte) 0xff };
		byte[] reserved = { 0x00 };
		byte[] andXOffset = { 0x67, 0x00 };
		byte[] oplockLevel = { 0x00 };
		byte[] fid = { (byte) 0x00, 0x40 };
		byte[] createAction = { 0x01, 0x00, 0x00, 0x00 };
		byte[] created = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] lastAccess = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] lastWrite = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] change = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] fileAttributes = { (byte) 0x80, 0x00, 0x00, 0x00 };
		byte[] allocationSize = { 0x00, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00 };
		byte[] endOfFile = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] fileType = { 0x02, 0x00 };
		byte[] ipcState = { (byte) 0xff, 0x05 };
		byte[] isDirectory = { 0x00 };
		byte[] byteCount = { 0x00, 0x00 };

		byte[] response = HelperUtils.concat(wordCount, andXCommand, reserved,
				andXOffset, oplockLevel, fid, createAction, created,
				lastAccess, lastWrite, change, fileAttributes, allocationSize,
				endOfFile, fileType, ipcState, isDirectory, byteCount);
		return wrapNetbios(wrapHeader(response));
	}

	/**
	 * Builds the session setup packet
	 * 
	 * @ret urn session setup packet
	 */
	public byte[] getSessSetup() {
		if (authenticateNext) {
			return getSetupAuth();
		} else {
			authenticateNext = true;
			return getSetupChal();
		}
	}

	/**
	 * Builds the session setup packet for authentication required
	 * 
	 * @return session setup authentication packet
	 */
	private byte[] getSetupAuth() {
		byte[] wordCount = { 0x04 };
		byte[] andXCommand = { (byte) 0xff };
		byte[] reserved = { 0x00 };
		byte[] andXOffset = { (byte) 0xa2, 0x00 };
		byte[] action = { 0x01, 0x00 };
		byte[] secBlobLength;
		byte[] byteCount;
		byte[] secBlob = { (byte) 0xa1, 0x07, 0x30, 0x05, (byte) 0xa0, 0x03,
				0x0a, 0x01, 0x00 };
		byte[] nativOS = HelperUtils.fillWithZeroExtended(serverVersion[0]
				.getBytes());
		byte[] nativLanMngr = HelperUtils.fillWithZeroExtended(serverVersion[1]
				.getBytes());

		byte[] buffer = ByteBuffer.allocate(4).putInt(secBlob.length).array();
		secBlobLength = new byte[] { buffer[3], buffer[2] };
		buffer = ByteBuffer.allocate(4)
				.putInt(secBlob.length + nativOS.length + nativLanMngr.length)
				.array();
		byteCount = new byte[] { buffer[3], buffer[2] };

		byte[] response = HelperUtils.concat(wordCount, andXCommand, reserved,
				andXOffset, action, secBlobLength, byteCount, secBlob, nativOS,
				nativLanMngr);
		return wrapNetbios(wrapHeader(response));
	}

	/**
	 * Builds the session setup challange packet
	 * 
	 * @return session setup challange packet
	 */
	private byte[] getSetupChal() {
		byte[] wordCount = { 0x04 };
		byte[] andXCommand = { (byte) 0xff };
		byte[] reserved = { 0x00 };
		byte[] andXOffset = { 0x60, 0x01 };
		byte[] action = { 0x00, 0x00 };
		byte[] secBlobLength;
		byte[] byteCount;
		byte[] secBlob = { (byte) 0xa1, (byte) 0x81, (byte) 0xc4 };
		byte[] negToken = { 0x30, (byte) 0x81, (byte) 0xc1, (byte) 0xa0, 0x03,
				0x0a, 0x01 };
		byte[] negResult = { 0x01 };
		byte[] negToken2 = { (byte) 0xa1, 0x0c, 0x06, 0x0a };
		byte[] supportedMech = { 0x2b, 0x06, 0x01, 0x04, 0x01, (byte) 0x82,
				0x37, 0x02, 0x02, 0x0a };
		byte[] negToken3 = { (byte) 0xa2, (byte) 0x81, (byte) 0xab, 0x04,
				(byte) 0x81, (byte) 0xa8 };
		byte[] ntlmsspId = { 0x4e, 0x54, 0x4c, 0x4d, 0x53, 0x53, 0x50, 0x00 };
		byte[] nlmMsgType = { 0x02, 0x00, 0x00, 0x00 };
		byte[] buffer = ByteBuffer.allocate(4).putInt(serverName.length)
				.array();
		byte[] targetNameLength = new byte[] { buffer[3], buffer[2] };
		byte[] targetNameMaxLength = new byte[] { buffer[3], buffer[2] };
		byte[] targetNameOffset = { 0x38, 0x00, 0x00, 0x00 };
		byte[] flags = { 0x15, (byte) 0x82, (byte) 0x8a, 0x60 };
		if (!serverVersion[0].contains("Unix")) {
			flags[3] = (byte) (flags[3] | 0x02);
		}
		byte[] challenge = HelperUtils.randomBytes(8);
		byte[] reserved2 = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] targetInfoLength = { 0x60, 0x00 };
		byte[] targetInfoMaxLength = { 0x60, 0x00 };
		byte[] targetInfoOffset = { 0x48, 0x00, 0x00, 0x00 };
		byte[] version = null;
		if (serverVersion[0].contains("Windows 7")
				|| serverVersion[0].contains("Windows Server 2008")) {
			version = new byte[] { 0x06, 0x01, (byte) 0xb0, 0x1d, 0x00, 0x00,
					0x00, 0x0f };
		} else if (serverVersion[0].contains("Windows 8")
				|| serverVersion[0].contains("Windows Server 2012")) {
			version = new byte[] { 0x06, 0x02, (byte) 0xf0, 0x23, 0x00, 0x00,
					0x00, 0x0f };
		}
		// serverName
		byte[] attributeNBDomain = { 0x02, 0x00, 0x10, 0x00 };
		// serverName
		byte[] attributeNBcomputer = { 0x01, 0x00, 0x10, 0x00 };
		// serverName
		byte[] attributeDNSDomain = { 0x04, 0x00, 0x10, 0x00 };
		// serverName
		byte[] attributeDNScomputer = { 0x03, 0x00, 0x10, 0x00 };
		// serverName
		byte[] attributeTimeStamp = { 0x07, 0x00, 0x08, 0x00 };
		byte[] timeStamp = getTimeInBytes();
		byte[] attributeEnd = { 0x00, 0x00, 0x00, 0x00 };
		secBlob = HelperUtils.concat(secBlob, negToken, negResult, negToken2,
				supportedMech, negToken3, ntlmsspId, nlmMsgType,
				targetNameLength, targetNameMaxLength, targetNameOffset, flags,
				challenge, reserved2, targetInfoLength, targetInfoMaxLength,
				targetInfoOffset, version, serverName, attributeNBDomain,
				serverName, attributeNBcomputer, serverName,
				attributeDNSDomain, serverName, attributeDNScomputer,
				serverName, attributeTimeStamp, timeStamp, attributeEnd);
		byte[] nativOS = HelperUtils.fillWithZeroExtended(serverVersion[0]
				.getBytes());
		byte[] nativLanMngr = HelperUtils.fillWithZeroExtended(serverVersion[1]
				.getBytes());
		buffer = ByteBuffer.allocate(4).putInt(secBlob.length).array();
		secBlobLength = new byte[] { buffer[3], buffer[2] };
		buffer = ByteBuffer.allocate(4)
				.putInt(secBlob.length + nativOS.length + nativLanMngr.length)
				.array();
		byteCount = new byte[] { buffer[3], buffer[2] };

		ntStat = new byte[] { 0x16, 0x00, 0x00, (byte) 0xc0 };
		userID = new byte[] { 0x00, 0x08 };

		byte[] response = HelperUtils.concat(wordCount, andXCommand, reserved,
				andXOffset, action, secBlobLength, byteCount, secBlob, nativOS,
				nativLanMngr);
		return wrapNetbios(wrapHeader(response));
	}

	/**
	 * Returns the command number from the current message
	 * 
	 * @return command number
	 */
	public byte getSmbCommand() {
		return smbCommand[0];
	}

	/**
	 * Builds the trans packet
	 * 
	 * @return trans packet
	 */
	public byte[] getTrans() {
		byte[] transSub = getTransSub();
		byte[] response = null;
		
		if (transSub[0] == (byte) 0xff) { // for NMB in host announcement, NOT smb protocol
			byte[] wordCount = { 0x11 };
			byte[] totalParamCount = { 0x00, 0x00 };
			byte[] totalDataCount = new byte[2];
			byte[] maxParamCount = { 0x00, 0x00 };
			byte[] maxDataCount = { 0x00, 0x00 };
			byte[] maxSetupCount = { 0x00 };
			byte[] reserved = { 0x00 };
			byte[] flags = { 0x00, 0x00 };
			byte[] timeout = { 0x00, 0x00, 0x00, 0x00 };
			byte[] reserved2 = { 0x00, 0x00 };
			byte[] paramCount = { 0x00, 0x00 };
			byte[] paramOffset = { 0x00, 0x00 };
			byte[] dataCount = new byte[2];
			byte[] dataOffset = { 0x56, 0x00 };
			byte[] setupCount = { 0x03 };
			byte[] reserved3 = { 0x00 };
			
			//SMB MailSlot				
			byte[] opcode = new byte[]{0x01, 0x00};
			byte[] priority = new byte[]{0x01, 0x00};
			byte[] smbclass = new byte[]{0x02, 0x00};
			byte[] size = new byte[2];
			byte[] name = HelperUtils.concat("\\MAILSLOT\\BROWSE".getBytes(), new byte[]{0x00}); 
			
			byte[] windowsBrowser = null;
			if (type == NBDSType.BROWSER) {
				windowsBrowser = getBrowser();
			} else if (type == NBDSType.REQUEST_ANNOUNCEMENT) {
				byte[] command = {0x02};
				byte[] unusedFlags = {0x01, 0x00};
				byte[] responseCompName = serverName;
				windowsBrowser = HelperUtils.concat(command, unusedFlags, responseCompName);
			} else {
				windowsBrowser = getAnnouncement();
			}

			byte[] buffer = ByteBuffer.allocate(4).putInt(windowsBrowser.length).array();
			totalDataCount[0] = buffer[3];
			totalDataCount[1] = buffer[2];
			dataCount = totalDataCount;
			
			buffer = ByteBuffer.allocate(4).putInt(name.length + windowsBrowser.length).array();
			size[0] = buffer[3];
			size[1] = buffer[2];
			byte[] smbMailSlot = HelperUtils.concat(opcode, priority, smbclass, size, name);					

			// no netbios header required for NMB!!
			return wrapHeader(HelperUtils.concat(wordCount, totalParamCount, totalDataCount,
					maxParamCount, maxDataCount, maxSetupCount, reserved, flags, timeout, reserved2,
					paramCount, paramOffset, dataCount, dataOffset, setupCount, reserved3, smbMailSlot,
					windowsBrowser));
		} else if (transSub[0] == 0x00 && transSub[1] == 0x0b) { // bind_ack
			byte[] wordCount = { 0x0a };
			byte[] totalParamCount = { 0x00, 0x00 };
			byte[] totalDataCount = { 0x44, 0x00 };
			byte[] reserved = { 0x00, 0x00 };
			byte[] paramCount = { 0x00, 0x00 };
			byte[] paramOffset = { 0x38, 0x00 };
			byte[] paramDisplace = { 0x00, 0x00 };
			byte[] dataCount = { 0x44, 0x00 };
			byte[] dataOffset = { 0x38, 0x00 };
			byte[] dataDisplace = { 0x00, 0x00 };
			byte[] setupCount = { 0x00 };
			byte[] reserved2 = { 0x00 };
			byte[] byteCount = { 0x45, 0x00 };
			byte[] padding = { 0x00 };

			byte[] dcerpc = getDceRpc(transSub, 0);

			response = HelperUtils.concat(wordCount, totalParamCount,
					totalDataCount, reserved, paramCount, paramOffset,
					paramDisplace, dataCount, dataOffset, dataDisplace,
					setupCount, reserved2, byteCount, padding, dcerpc);

		} else if (transSub[0] == 0x00 && transSub[1] == 0x00) { // netShareEnumAll
			byte[] wordCount = { 0x0a };
			byte[] totalParamCount = { 0x00, 0x00 };
			byte[] totalDataCount = { 0x20, 0x01 };
			byte[] reserved = { 0x00, 0x00 };
			byte[] paramCount = { 0x00, 0x00 };
			byte[] paramOffset = { 0x38, 0x00 };
			byte[] paramDisplace = { 0x00, 0x00 };
			byte[] dataCount = { 0x20, 0x01 };
			byte[] dataOffset = { 0x38, 0x00 };
			byte[] dataDisplace = { 0x00, 0x00 };
			byte[] setupCount = { 0x00 };
			byte[] reserved2 = { 0x00 };
			byte[] byteCount = new byte[2]/* = {0x21, 0x01} */;
			byte[] padding = { 0x00 };

			byte[] dcerpc = new byte[24];

			byte[] levelPointer = { 0x01, 0x00, 0x00, 0x00 };
			byte[] ctr = { 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00 };
			byte[] ctr1 = { 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x02, 0x00,
					0x03, 0x00, 0x00, 0x00 };
			byte[] array1Pointer = { 0x08, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00,
					(byte) 0x80, 0x0c, 0x00, 0x02, 0x00 };
			byte[] array2Pointer = { 0x10, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00,
					(byte) 0x80, 0x14, 0x00, 0x02, 0x00 };
			byte[] array3Pointer = { 0x18, 0x00, 0x02, 0x00, 0x03, 0x00, 0x00,
					(byte) 0x80, 0x1c, 0x00, 0x02, 0x00 };
			byte[] array1 = { 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x07, 0x00, 0x00, 0x00, 0x41, 0x00, 0x44, 0x00, 0x4d, 0x00,
					0x49, 0x00, 0x4e, 0x00, 0x24, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x0d, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0d, 0x00,
					0x00, 0x00, 0x52, 0x00, 0x65, 0x00, 0x6d, 0x00, 0x6f, 0x00,
					0x74, 0x00, 0x65, 0x00, 0x20, 0x00, 0x41, 0x00, 0x64, 0x00,
					0x6d, 0x00, 0x69, 0x00, 0x6e, 0x00, 0x00, 0x00 };
			byte[] array2 = { 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x43, 0x00, 0x24, 0x00,
					0x00, 0x00, 0x00, 0x00, 0x0e, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x00, 0x00, 0x0e, 0x00, 0x00, 0x00, 0x44, 0x00, 0x65, 0x00,
					0x66, 0x00, 0x61, 0x00, 0x75, 0x00, 0x6c, 0x00, 0x74, 0x00,
					0x20, 0x00, 0x73, 0x00, 0x68, 0x00, 0x61, 0x00, 0x72, 0x00,
					0x65, 0x00 };
			byte[] array3 = { 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x49, 0x00, 0x50, 0x00,
					0x43, 0x00, 0x24, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0b, 0x00,
					0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0b, 0x00, 0x00, 0x00,
					0x52, 0x00, 0x65, 0x00, 0x6d, 0x00, 0x6f, 0x00, 0x74, 0x00,
					0x65, 0x00, 0x20, 0x00, 0x49, 0x00, 0x50, 0x00, 0x43, 0x00,
					0x00, 0x00 };
			byte[] totalEntries = { 0x00, 0x00, 0x03, 0x00, 0x00, 0x00 };
			byte[] referentID = { 0x20, 0x00, 0x02, 0x00 };
			byte[] resumeHandle = { 0x00, 0x00, 0x00, 0x00 };
			byte[] windowsError = { 0x00, 0x00, 0x00, 0x00 };
			int tmp = padding.length + dcerpc.length + levelPointer.length
					+ ctr.length + ctr1.length + array1Pointer.length
					+ array2Pointer.length + array3Pointer.length
					+ array1.length + array2.length + array3.length
					+ totalEntries.length + referentID.length
					+ resumeHandle.length + windowsError.length;
			byte[] tmp2 = ByteBuffer.allocate(4).putInt(tmp).array();
			byteCount = new byte[] { tmp2[3], tmp2[2] };
			dcerpc = getDceRpc(transSub, tmp - 1);

			response = HelperUtils.concat(wordCount, totalParamCount,
					totalDataCount, reserved, paramCount, paramOffset,
					paramDisplace, dataCount, dataOffset, dataDisplace,
					setupCount, reserved2, byteCount, padding, dcerpc,
					levelPointer, ctr, ctr1, array1Pointer, array2Pointer,
					array3Pointer, array1, array2, array3, totalEntries,
					referentID, resumeHandle, windowsError);
		}

		return wrapNetbios(wrapHeader(response));
	}

	/**
	 * Builds the trans2 packet
	 * 
	 * @return trans2 packet
	 */
	public byte[] getTrans2() {
		byte[] response = null;
		byte[] wordCount = { 0x00 };
		byte[] andXCommand = { 0x00, 0x00 };
		ntStat = new byte[] { 0x22, 0x00, 0x00, (byte) 0xc0 };
		response = HelperUtils.concat(wordCount, andXCommand);
		return wrapNetbios(wrapHeader(response));
	}

	/**
	 * Extracts the trans sub packet from message
	 * 
	 * @return trans sub packet
	 */
	private byte[] getTransSub() {
		byte[] transSub = new byte[2];
		if (smbCommand[0] == 0x32)
			transSub = new byte[] { message[66], message[65] };
		else if (smbCommand[0] == 0x25 && message != null)
			transSub = new byte[] { 0x00, message[90] };
		else if (smbCommand[0] == 0x25 && message == null)
			transSub = new byte[] { (byte) 0xff };
		else
			transSub = new byte[] { 0x00, 0x00 };
		return transSub;
	}
	
	/**
	 * For NBNS only.
	 * @return announcement packet.
	 */
	private byte[] getAnnouncement() {
		//Microsoft Windows Browser
		byte[] command = null;
		if(type == NBDSType.LOCAL_MASTER_ANNOUNCEMENT_ALL || type == NBDSType.LOCAL_MASTER_ANNOUNCEMENT) {
			command = new byte[]{0x0f};
		} else if (type == NBDSType.DOMAIN_ANNOUNCEMENT) {
			command = new byte[]{0x0c};
		} else {
			command = new byte[]{0x01};
		}
		byte[] updateCount = new byte[]{0x00};
		byte[] updatePeriodicity = null;
		if (type == NBDSType.HOST_ANNOUNCEMENT_WITH_SERVICES) {
			updatePeriodicity = new byte[]{0x60, (byte) 0xea, 0x00, 0x00};
		} else if (type == NBDSType.HOST_ANNOUNCEMENT) {
			updatePeriodicity = new byte[]{0x00, 0x00, 0x00, 0x00};
		} else if (type == NBDSType.LOCAL_MASTER_ANNOUNCEMENT_ALL || type == NBDSType.DOMAIN_ANNOUNCEMENT) {
			updatePeriodicity = new byte[]{(byte) 0xc0, (byte) 0xd4, 0x01, 0x00};
		} else if (type == NBDSType.LOCAL_MASTER_ANNOUNCEMENT) {
			updatePeriodicity = new byte[]{0x00, 0x00, 0x00, 0x00};
		}
		byte[] hostName = null;
		if (type == NBDSType.DOMAIN_ANNOUNCEMENT) {
			hostName = workgroup;
		} else {
			hostName = serverName;
		}
		for (int i = hostName.length; i < 16; i++) {
			hostName = HelperUtils.concat(hostName, new byte[]{0x00});
		}
		byte[] osMajorVersion = new byte[]{0x04};
		byte[] osMinorVersion = new byte[]{0x09};
		byte[] serverType = null;
		if (type == NBDSType.HOST_ANNOUNCEMENT_WITH_SERVICES || type == NBDSType.LOCAL_MASTER_ANNOUNCEMENT_ALL) {
			serverType = new byte[]{0x03, (byte) 0x9a, (byte) 0x81, 0x00};
		} else if (type == NBDSType.HOST_ANNOUNCEMENT || type == NBDSType.LOCAL_MASTER_ANNOUNCEMENT) {
			serverType = new byte[]{0x00, 0x00, 0x00, 0x00};
		} else if (type == NBDSType.DOMAIN_ANNOUNCEMENT) {
			serverType = new byte[]{0x00, 0x10, 0x00, (byte) 0x80};
		}
		byte[] browserProtocolMajorVer = new byte[]{0x0f};
		byte[] browserProtocolMinorVer = new byte[]{0x01};
		byte[] signature = new byte[]{0x55, (byte) 0xaa};
		byte[] hostComment = null;
		if (type == NBDSType.DOMAIN_ANNOUNCEMENT) {
			hostComment = HelperUtils.concat(serverName, new byte[]{0x00});
		} else {
			hostComment = HelperUtils.concat("".getBytes(), new byte[]{0x00});
		}
		
		return HelperUtils.concat(command, updateCount, updatePeriodicity, hostName,
				osMajorVersion, osMinorVersion, serverType, browserProtocolMajorVer, browserProtocolMinorVer,
				signature, hostComment);
	}
	
	/**
	 * For NBNS only.
	 * @return MicrosoftBrowser part in NetBIOS packets.
	 */
	private byte[] getBrowser() {
		byte[] command = {0x08};
		byte[] electionVersion = {0x01};
		byte[] electionCriteria = {0x02, 0x0f, 0x01, 0x14};
		byte[] uptime = {(byte) 0xb0, 0x36, 0x00, 0x00, 0x00, 0x00,0x00, 0x00};
		return HelperUtils.concat(command, electionVersion, electionCriteria, uptime, serverName, new byte[]{0x00});
	}

	/**
	 * Builds the tree connect packet
	 * 
	 * @return tree connect packet
	 */
	public byte[] getTreeCon() {
		String str = HelperUtils.byteToStr(message);
		byte[] wordCount = { 0x00 };
		byte[] andXCommand = { 0x00, 0x00 };
		byte[] response = null;
		if (str.contains("IPC$") || str.contains("C$")) {
			wordCount = new byte[] { 0x07 };
			andXCommand = new byte[] { (byte) 0xff };
			byte[] reserved = { 0x00 };
			byte[] andXOffset = { 0x38, 0x00 };
			byte[] optionalSupport = { 0x01, 0x00 };
			byte[] maxShareAccess = { (byte) 0xff, (byte) 0xff, 0x1f, 0x00 };
			byte[] guestMaxShareAccess = { (byte) 0xff, (byte) 0xff, 0x1f, 0x00 };
			byte[] byteCount = { 0x07, 0x00 };
			byte[] service = { 0x49, 0x50, 0x43, 0x00 };
			byte[] extraParameters = { 0x00, 0x00, 0x00 };

			treeID = new byte[] { 0x00, 0x08 };

			response = HelperUtils.concat(wordCount, andXCommand, reserved,
					andXOffset, optionalSupport, maxShareAccess,
					guestMaxShareAccess, byteCount, service, extraParameters);
		} else if (str.contains("ADMIN$")) {
			ntStat = new byte[] { 0x22, 0x00, 0x00, (byte) 0xc0 };
			response = HelperUtils.concat(wordCount, andXCommand);
		} else {
			ntStat = new byte[] { (byte) 0xcc, 0x00, 0x00, (byte) 0xc0 };
			response = HelperUtils.concat(wordCount, andXCommand);
		}

		return wrapNetbios(wrapHeader(response));
	}

	/**
	 * Builds the tree disconnect packet
	 * 
	 * @return tree disconnect packet
	 */
	public byte[] getTreeDisc() {
		byte[] wordCount = { 0x00 };
		byte[] byteCount = { 0x00, 0x00 };

		smbCommand[0] = 0x71;

		byte[] response = HelperUtils.concat(wordCount, byteCount);

		return wrapNetbios(wrapHeader(response));
	}
	

	/**
	 * Converts the current system time into a byte[] with windows specific time
	 * 
	 * @return current system time in windows format as byte[]
	 */
	private static byte[] getTimeInBytes() {
		long time = System.currentTimeMillis();
		Calendar calend = Calendar.getInstance();
		calend.setTimeZone(TimeZone.getTimeZone("UTC"));
		calend.set(1601, 0, 01, 00, 00, 00);
		time -= calend.getTimeInMillis();
		time *= 10000;

		byte[] timeInWindowsBytes = new byte[8];
		byte[] timeInBytes = ByteBuffer.allocate(8).putLong(time).array();

		for (int i = 0, j = 7; i < 8 && j > -1; i++, j--) {
			timeInWindowsBytes[i] = (byte) (timeInBytes[j] & 0xff);
		}

		return timeInWindowsBytes;
	}

	/**
	 * Converts the current timezone into a byte[] with windows specific format
	 * 
	 * @return current timezone in windows format as byte[]
	 */
	private static byte[] getTimeZoneInBytes() {
		// get current timezone offset in minutes
		Integer offset = new GregorianCalendar().getTimeZone().getRawOffset() / 1000 / 60; 
		char[] offsetChars = Integer.toBinaryString(offset).toCharArray();
		boolean invert = false;
		for (int i = offsetChars.length - 1; i > -1; i--) {
			if (!invert && offsetChars[i] == '1') {
				invert = true;
			} else if (invert) {
				offsetChars[i] = (offsetChars[i] == '0') ? '1' : '0';
			}
		}
		char[] extendedChars = new char[31];
		for (int i = 0; i < extendedChars.length - offsetChars.length; i++) {
			extendedChars[i] = '1';
		}
		for (int i = 0; i < offsetChars.length; i++) {
			extendedChars[i + extendedChars.length - offsetChars.length] = offsetChars[i];
		}
		int timezone = Integer.parseInt(new String(extendedChars), 2);
		byte[] timezoneBytes = new byte[2];
		timezoneBytes[1] = (byte) (timezone >> 8);
		timezoneBytes[0] = (byte) (timezone);
		return timezoneBytes;
	}

}