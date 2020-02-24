package de.tudarmstadt.informatik.hostage.logging.formatter.protocol;

import de.tudarmstadt.informatik.hostage.commons.HelperUtils;

/**
 * Telnet log view formatter.
 * 
 * @author Wulf Pfeiffer
 */
public class TELNET extends ProtocolFormatter {

	@Override
	public String format(String packet) {
		byte[] bytes = HelperUtils.hexStringToBytes(packet);
		String options = "Options:\n" + checkForOptions(bytes) + "\n";
		String content = "Content: " + HelperUtils.byteToStr(bytes);
		return options + content;
	}

	/**
	 * Checks a byte for its command value.
	 * 
	 * @param b
	 *            byte that is checked.
	 * @return name of the command.
	 */
	private String checkCommand(byte b) {
		switch (b) {
		case 0x00:
			return "Binary Transmission\n";
		case 0x01:
			return "Echo\n";
		case 0x02:
			return "Reconnection\n";
		case 0x03:
			return "Suppress Go Ahead\n";
		case 0x04:
			return "Approx Message Size Negotiation\n";
		case 0x05:
			return "Status\n";
		case 0x06:
			return "Timing Mark\n";
		case 0x07:
			return "Remote Controlled Trans and Echo\n";
		case 0x08:
			return "Output Line Width\n";
		case 0x09:
			return "Output Page Size\n";
		case 0x0a:
			return "Output Carriage-Return Disposition\n";
		case 0x0b:
			return "Output Horizontal Tab Stops\n";
		case 0x0c:
			return "Output Horizontal Tab Disposition\n";
		case 0x0d:
			return "Output Formfeed Disposition\n";
		case 0x0e:
			return "Output Vertical Tabstops\n";
		case 0x0f:
			return "Output Vertical Tab Disposition\n";
		case 0x10:
			return "Output Linefeed Disposition\n";
		case 0x11:
			return "Extended ASCII\n";
		case 0x12:
			return "Logout\n";
		case 0x13:
			return "Byte Macro\n";
		case 0x14:
			return "Data Entry Terminal\n";
		case 0x15:
			return "SUPDUP\n";
		case 0x16:
			return "SUPDUP Output\n";
		case 0x17:
			return "Send Location\n";
		case 0x18:
			return "Terminal Type\n";
		case 0x19:
			return "End of Record\n";
		case 0x1a:
			return "TACACS User Identification\n";
		case 0x1b:
			return "Output Marking\n";
		case 0x1c:
			return "Terminal Location Number\n";
		case 0x1d:
			return "Telnet 3270 Regime\n";
		case 0x1e:
			return "X.3 PAD\n";
		case 0x1f:
			return "Negotiate About Window Size\n";
		case 0x20:
			return "Terminal Speed\n";
		case 0x21:
			return "Remote Flow Control\n";
		case 0x22:
			return "Linemode\n";
		case 0x23:
			return "X Display Location\n";
		case (byte) 0xff:
			return "Extended-Options-List\n";
		default:
			return "unknown option\n";
		}
	}

	/**
	 * Checks a packet for option commands and returns their names as Strings.
	 * 
	 * @param bytes
	 *            that are checked.
	 * @return names of the option commands as String.
	 */
	private String checkForOptions(byte[] bytes) {
		StringBuffer options = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] == (byte) 0xff && i + 2 < bytes.length) {
				options.append(checkMode(bytes[i + 1]));
				// option name
				options.append(checkCommand(bytes[i + 2]));
			}
		}
		return options.toString();
	}

	/**
	 * Checks a byte for its mode value.
	 * 
	 * @param b
	 *            byte that is checked.
	 * @return name of the mode.
	 */
	private String checkMode(byte b) {
		switch (b) {
		case (byte) 0xfb:
			return " WILL ";
		case (byte) 0xfc:
			return " WON'T ";
		case (byte) 0xfd:
			return " DO ";
		case (byte) 0xfe:
			return " DON'T ";
		default:
			return " unkown command ";
		}
	}

}
