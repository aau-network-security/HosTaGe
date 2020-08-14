package de.tudarmstadt.informatik.hostage.protocol.smbutils;

import java.nio.ByteBuffer;

import de.tudarmstadt.informatik.hostage.commons.HelperUtils;


/**
 * NetBIOS Datagram Service.
 * @author Wulf Pfeiffer
 */
public class NBDS {
	
	private String dst;
	private byte[] type;
	private byte[] flags;
	private byte[] transactID;
	private byte[] srcIP;
	private byte[] srcPort;
	private byte[] length;
	private byte[] offset;
	private byte[] srcName;
	private byte[] dstName;
	private int nbdstype;
	private SMBPacket smb;
	
	public NBDS(byte[] addr, String src, String dst) {
		this.dst = dst;
		type = new byte[]{0x11};
		flags = new byte[]{0x0a};
		srcIP = addr;
		srcPort = new byte[]{0x00, (byte) 0x8a};
		offset = new byte[]{0x00, 0x00};
		length = new byte[2];
		srcName = NMBStringCoder.wrapNBNSName(NMBStringCoder.encodeNBNSName(src.getBytes()), NBNSService.WORKSTATION);
		smb = new SMBPacket(null, src, dst);
	}
	
	/**
	 * Prepares the content for the next packet.
	 */
	private void preparePacket() {
		transactID = NMB.getAndIncTransactID();
		if (nbdstype == NBDSType.REQUEST_ANNOUNCEMENT || nbdstype == NBDSType.LOCAL_MASTER_ANNOUNCEMENT_ALL 
				|| nbdstype == NBDSType.LOCAL_MASTER_ANNOUNCEMENT) {
			dstName = NMBStringCoder.wrapNBNSName(NMBStringCoder.encodeNBNSName(dst.getBytes()), NBNSService.BROWSER_ELECTION);
		} else if (nbdstype == NBDSType.DOMAIN_ANNOUNCEMENT) {
			dstName = HelperUtils.concat(new byte[]{0x20, 0x41, 0x42, 0x41, 0x43}, NMBStringCoder.encodeNBNSName("__MSBROWSE__".getBytes()),
					new byte[]{0x41, 0x43, 0x41, 0x42, 0x00});
		} else {
			dstName = NMBStringCoder.wrapNBNSName(NMBStringCoder.encodeNBNSName(dst.getBytes()), NBNSService.LOCAL_MASTER_BROWSER);
		}
		smb.prepareNextResponse(nbdstype);
		byte[] buffer = HelperUtils.concat(srcName, dstName, smb.getTrans());
		byte[] lengthBuffer = ByteBuffer.allocate(4).putInt(buffer.length).array();
		length[0] = lengthBuffer[2];
		length[1] = lengthBuffer[3];
	}
	
	/**
	 * @return next Packet.
	 */
	public byte[] getNextPacket() {
		preparePacket();
		return getBytes();
	}
	
	/**
	 * @return content of the packet.
	 */
	private byte[] getBytes() {
		return HelperUtils.concat(type, flags, transactID, srcIP, srcPort, length,
				offset, srcName, dstName, smb.getTrans());
	}

	/**
	 * Set the NBDSType.
	 * @param nbdstype
	 */
	public void setNbdstype(int nbdstype) {
		this.nbdstype = nbdstype;
	}
	
}
