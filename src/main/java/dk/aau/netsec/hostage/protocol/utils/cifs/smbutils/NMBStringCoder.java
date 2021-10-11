package dk.aau.netsec.hostage.protocol.utils.cifs.smbutils;


import dk.aau.netsec.hostage.commons.HelperUtils;

/**
 * Used to encode and decode a String into a byte array so that they can be used in NetBIOS.
 * @author Wulf Pfeiffer
 */
public class NMBStringCoder {

	/**
	 * Decodes a netbios name as byte array to its proper String value.
	 * @param name bytes.
	 * @return String.
	 */
	public static String decodeNBNSName(byte[] name) {
		byte a_ascii = (byte) 'A';
		byte[] reducedA = new byte[name.length];
		for (int i = 0; i < name.length; i++) {
			reducedA[i] = (byte) (name[i] - a_ascii);
		}
		byte[] converted = new byte[reducedA.length / 2];
		for (int i = 0, j = 0; i < name.length && j < converted.length; i += 2, j++) {
			byte b1 = (byte) (reducedA[i] << 4);
			byte b2 = reducedA[i + 1];
			converted[j] = (byte) (b1 | b2);
		}
		return new String(converted);
	}

	/**
	 * Encodes a String into its netbios name as byte array.
	 * @param name
	 * @return netbios name.
	 */
	public static byte[] encodeNBNSName(byte[] name) {
		byte a_ascii = (byte) 'A';
        byte[] converted = new byte[name.length * 2];
		for (int i = 0, j = 0; i < name.length && j < converted.length; i++, j += 2) {
			converted[j] = (byte) (name[i] >> 4);
			converted[j + 1] = (byte) (name[i] & 0x0F);
		}
		byte[] addedA = new byte[converted.length];
		for (int i = 0; i < converted.length; i++) {
			addedA[i] = (byte) (converted[i] + a_ascii);
		}
		return addedA;
	}

	/**
	 * Wraps a NetBIOS name with the required start and end bytes and some more informations.
	 * @param name netbios name.
	 * @param service NBNDSService.
	 * @return wrapped content.
	 */
	public static byte[] wrapNBNSName(byte[] name, int service) {
		byte[] nameStart = { 0x20 };
		byte[] namePadding = null;
		if (name.length < 32) {
			int paddingLen = 32 - name.length;
			namePadding = new byte[paddingLen];
			for (int i = 0; i < namePadding.length; i += 2) {
				if (i == namePadding.length - 2) {
					byte[] serviceBytes = NBNSService.getServiceBytes(service);
					namePadding[i] = serviceBytes[0];
					namePadding[i + 1] = serviceBytes[1];
				} else {
					namePadding[i] = 0x43;
					namePadding[i + 1] = 0x041;
				}
			}
		}
		byte[] nameEnd = { 0x00 };
		return HelperUtils.concat(nameStart, name, namePadding, nameEnd);
	}

}
