package dk.aau.netsec.hostage.wrapper;
import dk.aau.netsec.hostage.commons.HelperUtils;

/**
 * Wrapper class for the payload of a network packet.
 * 
 * @author Mihai Plasoianu
 * @author Wulf Pfeiffer
 */
public class Packet {

	private final byte[] payload;
	private final String protocol;

	/**
	 * Constructs Packet from byte[]
	 * 
	 * @param payload
	 *            The byte[] payload
	 */
	public Packet(byte[] payload, String protocol) {
		this.payload = payload;
		this.protocol = protocol;
	}

	/**
	 * Constructs Packet from String
	 * 
	 * @param payload
	 *            The String payload
	 */
	public Packet(String payload, String protocol) {
		this.payload = payload.getBytes();
		this.protocol = protocol;
	}

	/**
	 * Returns a byte[] representation of the payload.
	 * 
	 * @return byte[] representation.
	 */
	public byte[] getBytes() {
		return payload;
	}


	/**
	 * Returns protocol name.
	 *
	 * @return protocol.
	 */
	public String getProtocol(){
		return protocol;
	}
	/**
	 * Returns a String representation of the payload.
	 * Depending on the protocol, the String will be represented 
	 * as a String of it's byte values.
	 * E.g.: the byte[] {0x01, 0x4A, 0x03} would look like
	 * the String "01, 4A, 03", or
	 * otherwise a normal String will be created with the payload.
	 * 
	 * @return String representation.
	 */
	@Override
	public String toString() {
		if (protocol.equals("FTP") 
				|| protocol.equals("HTTP") 
				|| protocol.equals("HTTPS")
				|| protocol.equals("SIP")
				|| protocol.equals("MODBUS")
				|| protocol.equals("SMTP")) {
			return new String(payload);
		} else {
			return HelperUtils.bytesToHexString(payload);
		}
	}

}