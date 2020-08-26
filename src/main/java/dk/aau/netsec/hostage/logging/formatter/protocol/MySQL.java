package dk.aau.netsec.hostage.logging.formatter.protocol;


import dk.aau.netsec.hostage.commons.HelperUtils;

/**
 * MySQL log view formatter.
 * 
 * @author Wulf Pfeiffer
 */
public class MySQL extends ProtocolFormatter {

	@Override
	public String format(String packet) {
		byte[] bytes = HelperUtils.hexStringToBytes(packet);
		String command = "Command: " + getCommand(bytes) + "\n";
		String content = "Content: " + HelperUtils.byteToStr(bytes) + "\n";
		return command + content;
	}

	/**
	 * Checks a packet for its command code and returns the name of this
	 * command.
	 * 
	 * @param bytes
	 *            to check.
	 * @return name of the command.
	 */
	private String getCommand(byte[] bytes) {
		// if packet number is 1 server started conversation so it must be login
		if (bytes.length < 5)
			return "";
		if (bytes[3] == 0x01)
			return "Login request";
		// else check for command code
		switch (bytes[4]) {
		case 0x00:
			return "COM_SLEEP";
		case 0x01:
			return "COM_QUIT";
		case 0x02:
			return "COM_INIT_DB";
		case 0x03:
			return "COM_QUERY";
		case 0x04:
			return "COM_FIELD_LIST";
		case 0x05:
			return "COM_CREATE_DB";
		case 0x06:
			return "COM_DROP_DB";
		case 0x07:
			return "COM_REFRESH";
		case 0x08:
			return "COM_SHUTDOWN";
		case 0x09:
			return "COM_STATISTICS";
		case 0x0a:
			return "COM_PROCESS_INFO";
		case 0x0b:
			return "COM_CONNECT";
		case 0x0c:
			return "COM_PROCESS_KILL";
		case 0x0d:
			return "COM_DEBUG";
		case 0x0e:
			return "COM_PING";
		case 0x0f:
			return "COM_TIME";
		case 0x10:
			return "COM_DELAYED_INSERT";
		case 0x11:
			return "COM_CHANGE_USER";
		case 0x12:
			return "COM_BINLOG_DUMP";
		case 0x13:
			return "COM_TABLE_DUMP";
		case 0x14:
			return "COM_CONNECT_OUT";
		case 0x15:
			return "COM_REGISTER_SLAVE";
		case 0x16:
			return "COM_STMT_PREPARE";
		case 0x17:
			return "COM_STMT_EXECUTE";
		case 0x18:
			return "COM_STMT_SEND_LONG_DATA";
		case 0x19:
			return "COM_STMT_CLOSE";
		case 0x1a:
			return "COM_STMT_RESET";
		case 0x1b:
			return "COM_SET_OPTION";
		case 0x1c:
			return "COM_STMT_FETCH";
		case 0x1d:
			return "COM_DAEMON";
		case 0x1e:
			return "COM_BINLOG_DUMP_GTID";
		default:
			return "unknown command";
		}
	}

}
