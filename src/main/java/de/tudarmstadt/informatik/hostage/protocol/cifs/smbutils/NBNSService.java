package de.tudarmstadt.informatik.hostage.protocol.cifs.smbutils;

/**
 * NetBios Name Service services.
 * @author Wulf Pfeiffer.
 */
public class NBNSService {

	public static final int SERVER = 0;
	public static final int MESSENGER = 1;
	public static final int WORKSTATION = 2;
	public static final int BROWSER_ELECTION = 3;
	public static final int LOCAL_MASTER_BROWSER = 4;
	public static final int BROWSER = 5;

	/**
	 * Returns the proper bytes that are used in the wrapping of netbios names for the NBNSService.
	 * @param service NBNSService.
	 * @return bytes.
	 */
	public static byte[] getServiceBytes(int service) {
		switch (service) {
		case NBNSService.SERVER:
			return new byte[]{0x43, 0x41};
		case NBNSService.MESSENGER:
			return new byte[]{0x41, 0x44};
		case NBNSService.WORKSTATION:
			return new byte[]{0x41, 0x41};
		case NBNSService.BROWSER_ELECTION:
			return new byte[]{0x42, 0x4f};
		case NBNSService.LOCAL_MASTER_BROWSER:
			return new byte[]{0x42, 0x4e};
		default:
			return new byte[]{0x43, 0x41};
		}
	}
}
