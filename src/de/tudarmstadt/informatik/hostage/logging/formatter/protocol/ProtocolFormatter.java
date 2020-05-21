package de.tudarmstadt.informatik.hostage.logging.formatter.protocol;

/**
 * Protocol formatter. This class provides functionality to format the packet as
 * a human-readable string of a specific protocol. Custom formatters should
 * inherit from this class.
 * 
 * @author Wulf Pfeiffer
 * @author Mihai Plasoianu
 */
public class ProtocolFormatter {

	/**
	 * Loads a protocol formatter for a protocol. Load a class matching
	 * {protocol} located in hostage.logging.formatter.protocol, default formatter
	 * otherwise.
	 * 
	 * @param protocolName
	 *            String representation of a protocol.
	 * @return The protocol formatter.
	 */
	public static ProtocolFormatter getFormatter(String protocolName) {
		String packageName = ProtocolFormatter.class.getPackage().getName();
		String className = String.format("%s.%s", packageName, protocolName);
		try {
			return (ProtocolFormatter) Class.forName(className).newInstance();
		} catch (InstantiationException e) {
			return new ProtocolFormatter();
		} catch (IllegalAccessException e) {
			return new ProtocolFormatter();
		} catch (ClassNotFoundException e) {
			return new ProtocolFormatter();
		}

	}

	/**
	 * Formats the content of packet. The packet content is represented as
	 * string or hex, depending on the protocol.
	 * 
	 * @param packet
	 *            Packet to format.
	 * @return Formatted string.
	 */
	public String format(String packet) {
		return String.format("%s\n", packet);
	}

}
