package de.tudarmstadt.informatik.hostage.protocol.cifs.smbutils;

import java.nio.ByteBuffer;

import de.tudarmstadt.informatik.hostage.commons.HelperUtils;


/**
 * NetBIOS Name Service.
 * @author Wulf Pfeiffer
 */
public class NBNS {
	
	private byte[] transactID;
	private byte[] flags;
	private byte[] questions;
	private byte[] answerRRs;
	private byte[] authorityRRs;
	private byte[] additionalRRs;
	private byte[] payload;
	private byte[] additional;
	private byte[] addr;
	private byte[] name;
	private int type;
	private int service;
	
	public NBNS(byte[] addr) {
		this.addr = addr;
	}
	
	/**
	 * Prepares the content for the next packet regarding of the set type.
	 */
	private void preparePacket() {
		transactID = NMB.getAndIncTransactID();
		switch (type) {
		case NBNSType.REGISTRATION_UNIQUE:
			prepareRegistrationPacket();
			break;
		case NBNSType.REGISTRATION_GROUP:
			prepareRegistrationPacket();
			break;
		case NBNSType.NAME_QUERY:
			prepareNameQueryPacket();
			break;
		case NBNSType.REGISTRATION_MSBROWSE:
			prepareRegistrationMsBrowse();
			break;
		default:
		}
	}
	
	/**
	 * Prepares the content for the next registration packet.
	 */
	private void prepareRegistrationPacket() {
		flags = new byte[]{0x29, 0x10};
		questions = new byte[]{0x00, 0x01};
		answerRRs = new byte[]{0x00, 0x00};
		authorityRRs = new byte[]{0x00, 0x00};
		additionalRRs = new byte[]{0x00, 0x01};
		payload = getPayload();
		additional = getAdditionalRecords();
	}
	
	/**
	 * Prepares the content for the next name query packet.
	 */
	private void prepareNameQueryPacket() {
		flags = new byte[] {0x01, 0x10};
		questions = new byte[]{0x00, 0x01};
		answerRRs = new byte[]{0x00, 0x00};
		authorityRRs = new byte[]{0x00, 0x00};
		additionalRRs = new byte[]{0x00, 0x00};
		payload = getPayload();
	}
	
	/**
	 * Prepares the content for the next MSBROWSE registration packet.
	 */
	private void prepareRegistrationMsBrowse() {
		flags = new byte[]{0x29, 0x10};
		questions = new byte[]{0x00, 0x01};
		answerRRs = new byte[]{0x00, 0x00};
		authorityRRs = new byte[]{0x00, 0x00};
		additionalRRs = new byte[]{0x00, 0x01};
		payload = HelperUtils.concat(new byte[]{0x20, 0x41, 0x42, 0x41, 0x43}, name,
				new byte[]{0x41, 0x43, 0x41, 0x42, 0x00, 0x00, 0x20, 0x00, 0x01});
		additional = getAdditionalRecords();
	}
	
	/**
	 * Prepares the content for the next response packet.
	 */
	public void prepareResponsePacket(byte[] packet, byte[] myAddr) {
		this.transactID = new byte[]{packet[0], packet[1]};
		flags = new byte[]{(byte) 0x85, (byte) 0x80};
		questions = new byte[]{0x00, 0x00};
		answerRRs = new byte[]{0x00, 0x01};
		authorityRRs = new byte[]{0x00, 0x00};
		additionalRRs = new byte[]{0x00, 0x00};
		byte[] nameBytes = new byte[32]; 
		System.arraycopy(packet, 13, nameBytes, 0, 32);
		String name = NMBStringCoder.decodeNBNSName(nameBytes);
		byte[] query = new byte[50];
		System.arraycopy(packet, 12, query, 0, 38);
		// time to live
		query[38] = 0x00;
		query[39] = 0x00;
		query[40] = 0x02;
		query[41] = 0x58;
		// data length
		query[42] = 0x00;
		query[43] = 0x06;
		// name flags
		query[44] = (byte) 0x80;
		query[45] = 0x00;
		// addr
		query[46] = myAddr[0];
		query[47] = myAddr[1];
		query[48] = myAddr[2];
		query[49] = myAddr[3];
		payload = query;
	}
	
	/**
	 * Builds the payload for the packet.
	 * @return payload.
	 */
	private byte[] getPayload() {
		byte[] payload = NMBStringCoder.wrapNBNSName(this.name, service);
		byte[] type = {0x00, 0x20};
		byte[] nbnsclass = {0x00, 0x01};
		return HelperUtils.concat(payload, type, nbnsclass);
	}

	/**
	 * Builds the additional records field.
	 * @return additional records.
	 */
	private byte[] getAdditionalRecords() {
		byte[] name = {(byte) 0xc0, 0x0c};
		byte[] type = {0x00, 0x20};
		byte[] nbnsclass = {0x00, 0x01};
		byte[] timeToLive = {0x00, 0x00, 0x00, 0x00};
		byte[] nameFlags = ((this.type == NBNSType.REGISTRATION_UNIQUE) ? new byte[]{0x00, 0x00} : new byte[]{(byte) 0x80, 0x00});
		byte[] buffer = ByteBuffer.allocate(4).putInt(nameFlags.length + addr.length).array();
		byte[] length = {buffer[2], buffer[3]};
		return HelperUtils.concat(name, type, nbnsclass, timeToLive, length, nameFlags, addr);
	}
	
	/**
	 * Returns the next packet.
	 * Use only after the name, type and service were set.
	 * @return next packet.
	 */
	public byte[] getNextPacket() {
		preparePacket();
		return getBytes();
	}
	
	/**
	 * Returns the next response packet.
	 * @param packet request packet.
	 * @param myAddr your current ip in bytes.
	 * @return next response packet.
	 */
	public byte[] getNextResponse(byte[] packet, byte[] myAddr) {
		prepareResponsePacket(packet, myAddr);
		return getBytes();
	}

	/**
	 * Returns the content of the packet in bytes.
	 * @return packet content.
	 */
	private byte[] getBytes() {
		return HelperUtils.concat(transactID, flags, questions, answerRRs, authorityRRs, additionalRRs, payload, additional);
	}

	/**
	 * Set the name for the payload.
	 * @param name
	 */
	public void setName(String name) {
		this.name = name.getBytes();
		this.name = NMBStringCoder.encodeNBNSName(this.name);
	}

	/**
	 * Set the NBNSType.
	 * @param type NBNSType.
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Set the NBNSService.
	 * @param service NBNSService.
	 */
	public void setService(int service) {
		this.service = service;
	}

}
