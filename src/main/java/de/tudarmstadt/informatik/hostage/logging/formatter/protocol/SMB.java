package de.tudarmstadt.informatik.hostage.logging.formatter.protocol;


import de.tudarmstadt.informatik.hostage.commons.HelperUtils;

/**
 * SMB log view formatter.
 * 
 * @author Wulf Pfeiffer
 */
public class SMB extends ProtocolFormatter {

	@Override
	public String format(String packet) {
		byte[] bytes = HelperUtils.hexStringToBytes(packet);
		byte cmd = bytes[8]; // command code located at 8
		StringBuffer buffer = new StringBuffer();
		buffer.append("Command: ");
		buffer.append(getCommandString(cmd));
		buffer.append("\n");
		buffer.append("Content: ");
		buffer.append(getContent(cmd, bytes));

		return buffer.toString();
	}

	/**
	 * Returns the content of a packet with command code 0x72.
	 * 
	 * @param packet
	 *            content as byte array.
	 * @return content as String.
	 */
	private String get0x72content(byte[] packet) {
		byte[] content = new byte[packet.length - 39];
		System.arraycopy(packet, 39, content, 0, content.length);
		return HelperUtils.byteToStr(content);
	}

	/**
	 * Checks command code for its command code and returns the name of this
	 * command.
	 * 
	 * @param command
	 *            as byte.
	 * @return command name as String.
	 */
	private String getCommandString(byte command) {
		switch (command) {
		case 0x00:
			return "SMB_COM_CREATE_DIRECTORY";
		case 0x01:
			return "SMB_COM_DELETE_DIRECTORY";
		case 0x02:
			return "SMB_COM_OPEN";
		case 0x03:
			return "SMB_COM_CREATE";
		case 0x04:
			return "SMB_COM_CLOSE";
		case 0x05:
			return "SMB_COM_FLUSH";
		case 0x06:
			return "SMB_COM_DELETE";
		case 0x07:
			return "SMB_COM_RENAME";
		case 0x08:
			return "SMB_COM_QUERY_INFORMATION";
		case 0x09:
			return "SMB_COM_SET_INFORMATION";
		case 0x0A:
			return "SMB_COM_READ";
		case 0x0B:
			return "SMB_COM_WRITE";
		case 0x0C:
			return "SMB_COM_LOCK_BYTE_RANGE";
		case 0x0D:
			return "SMB_COM_UNLOCK_BYTE_RANGE";
		case 0x0E:
			return "SMB_COM_CREATE_TEMPORARY";
		case 0x0F:
			return "SMB_COM_CREATE_NEW";
		case 0x10:
			return "SMB_COM_CHECK_DIRECTORY";
		case 0x11:
			return "SMB_COM_PROCESS_EXIT";
		case 0x12:
			return "SMB_COM_SEEK";
		case 0x13:
			return "SMB_COM_LOCK_AND_READ";
		case 0x14:
			return "SMB_COM_WRITE_AND_UNLOCK";
		case 0x1A:
			return "SMB_COM_READ_RAW";
		case 0x1B:
			return "SMB_COM_READ_MPX";
		case 0x1C:
			return "SMB_COM_READ_MPX_SECONDARY";
		case 0x1D:
			return "SMB_COM_WRITE_RAW";
		case 0x1E:
			return "SMB_COM_WRITE_MPX";
		case 0x1F:
			return "SMB_COM_WRITE_MPX_SECONDARY";
		case 0x20:
			return "SMB_COM_WRITE_COMPLETE";
		case 0x21:
			return "SMB_COM_QUERY_SERVER";
		case 0x22:
			return "SMB_COM_SET_INFORMATION2";
		case 0x23:
			return "SMB_COM_QUERY_INFORMATION2";
		case 0x24:
			return "SMB_COM_LOCKING_ANDX";
		case 0x25:
			return "SMB_COM_TRANSACTION";
		case 0x26:
			return "SMB_COM_TRANSACTION_SECONDARY";
		case 0x27:
			return "SMB_COM_IOCTL";
		case 0x28:
			return "SMB_COM_IOCTL_SECONDARY";
		case 0x29:
			return "SMB_COM_COPY";
		case 0x2A:
			return "SMB_COM_MOVE";
		case 0x2B:
			return "SMB_COM_ECHO";
		case 0x2C:
			return "SMB_COM_WRITE_AND_CLOSE";
		case 0x2D:
			return "SMB_COM_OPEN_ANDX";
		case 0x2E:
			return "SMB_COM_READ_ANDX";
		case 0x2F:
			return "SMB_COM_WRITE_ANDX";
		case 0x30:
			return "SMB_COM_NEW_FILE_SIZE";
		case 0x31:
			return "SMB_COM_CLOSE_AND_TREE_DISC";
		case 0x32:
			return "SMB_COM_TRANSACTION2";
		case 0x33:
			return "SMB_COM_TRANSACTION2_SECONDARY";
		case 0x34:
			return "SMB_COM_FIND_CLOSE2";
		case 0x35:
			return "SMB_COM_FIND_NOTIFY_CLOSE";
		case 0x70:
			return "SMB_COM_TREE_CONNECT";
		case 0x71:
			return "SMB_COM_TREE_DISCONNECT";
		case 0x72:
			return "SMB_COM_NEGOTIATE";
		case 0x73:
			return "SMB_COM_SESSION_SETUP_ANDX";
		case 0x74:
			return "SMB_COM_LOGOFF_ANDX";
		case 0x75:
			return "SMB_COM_TREE_CONNECT_ANDX";
		case (byte) 0x80:
			return "SMB_COM_QUERY_INFORMATION_DISK";
		case (byte) 0x81:
			return "SMB_COM_SEARCH";
		case (byte) 0x82:
			return "SMB_COM_FIND";
		case (byte) 0x83:
			return "SMB_COM_FIND_UNIQUE";
		case (byte) 0x84:
			return "SMB_COM_FIND_CLOSE";
		case (byte) 0xA0:
			return "SMB_COM_NT_TRANSACT";
		case (byte) 0xA1:
			return "SMB_COM_NT_TRANSACT_SECONDARY";
		case (byte) 0xA2:
			return "SMB_COM_NT_CREATE_ANDX";
		case (byte) 0xA4:
			return "SMB_COM_NT_CANCEL";
		case (byte) 0xA5:
			return "SMB_COM_NT_RENAME";
		case (byte) 0xC0:
			return "SMB_COM_OPEN_PRINT_FILE";
		case (byte) 0xC1:
			return "SMB_COM_WRITE_PRINT_FILE";
		case (byte) 0xC2:
			return "SMB_COM_CLOSE_PRINT_FILE";
		case (byte) 0xC3:
			return "SMB_COM_GET_PRINT_QUEUE";
		case (byte) 0xD8:
			return "SMB_COM_READ_BULK";
		case (byte) 0xD9:
			return "SMB_COM_WRITE_BULK";
		case (byte) 0xDA:
			return "SMB_COM_WRITE_BULK_DATA";
		case (byte) 0xFF:
			return "SMB_COM_NONE";
		default:
			return "Unknown Command";
		}
	}

	/**
	 * Returns the content of a packet as a String value, depending on its
	 * command code
	 * 
	 * @param command
	 *            of the packet.
	 * @param packet
	 *            content as byte array.
	 * @return content as a String.
	 */
	private String getContent(byte command, byte[] packet) {
		switch (command) {
		case 0x72:
			return get0x72content(packet);
		case 0x73:
			return HelperUtils.byteToStr(packet);
		case (byte) 0xa2:
			return HelperUtils.byteToStr(packet);
		case 0x25:
			return HelperUtils.byteToStr(packet);
		default:
			return "";
		}
	}

}
