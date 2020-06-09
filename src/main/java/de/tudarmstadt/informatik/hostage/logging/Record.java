package de.tudarmstadt.informatik.hostage.logging;

import java.io.Serializable;

import de.tudarmstadt.informatik.hostage.logging.formatter.Formatter;


/**
 * Record that holds all information of a message including full attack and network information.
 * This class should be avoided but is necessary due to inter group complications.
 */
public class Record implements Serializable {
    private static final long serialVersionUID = 1L;

	//
	private MessageRecord message;
	// attack
	private AttackRecord attack;
	// network
	private NetworkRecord network;
	
	public Record(){
		message = new MessageRecord();
		attack = new AttackRecord();
		network = new NetworkRecord();
	}

	public float getAccuracy() {
		return network.getAccuracy();
	}

	public long getAttack_id() {
		return attack.getAttack_id();
	}

	public String getBssid() {
		return network.getBssid();
	}

	public String getExternalIP() {
		return attack.getExternalIP();
	}

	public long getId() {
		return message.getId();
	}

	public double getLatitude() {
		return network.getLatitude();
	}

	public String getLocalIP() {
		return attack.getLocalIP();
	}

	public int getLocalPort() {
		return attack.getLocalPort();
	}

	public double getLongitude() {
		return network.getLongitude();
	}

	public String getPacket() {
		return message.getPacket();
	}

	public String getProtocol() {
		return attack.getProtocol();
	}

	public String getRemoteIP() {
		return attack.getRemoteIP();
	}

	public int getRemotePort() {
		return attack.getRemotePort();
	}

	public boolean getWasInternalAttack() {
		return attack.getWasInternalAttack();
	}

	public String getSsid() {
		return network.getSsid();
	}

	public long getTimestamp() {
		return message.getTimestamp();
	}

	public long getTimestampLocation() {
		return network.getTimestampLocation();
	}

	public MessageRecord.TYPE getType() {
		return message.getType();
	}

	public void setAccuracy(float accuracy) {
		network.setAccuracy(accuracy);
	}

	public void setAttack_id(long attack_id) {
		message.setAttack_id(attack_id);
		attack.setAttack_id(attack_id);
	}

	public void setBssid(String bssid) {
		attack.setBssid(bssid);
		network.setBssid(bssid);
	}

    public void setDevice(String deviceId){
        this.attack.setDevice(deviceId);
    }
    public String getDevice(){
        return this.attack.getDevice();
    }
    public void setSync_ID(long s){
        this.attack.setSync_id(s);

    }

    public long getSync_id(){
        return this.attack.getSync_id();
    }

	public void setExternalIP(String externalIP) {
		attack.setExternalIP(externalIP);
	}

	public void setId(int id) {
		message.setId(id);
	}

	public void setLatitude(double latitude) {
		network.setLatitude(latitude);
	}

	public void setLocalIP(String localIP) {
		attack.setLocalIP(localIP);
	}

	public void setLocalPort(int localPort) {
		attack.setLocalPort(localPort);
	}

	public void setLongitude(double longitude) {
		network.setLongitude(longitude);
	}

	public void setPacket(String packet) {
		message.setPacket(packet);
	}

	public void setProtocol(String protocol) {
		attack.setProtocol(protocol);
	}

	public void setRemoteIP(String remoteIP) {
		attack.setRemoteIP(remoteIP);
	}

	public void setRemotePort(int remotePort) {
		attack.setRemotePort(remotePort);
	}

	public void setWasInternalAttack(boolean internalAttack) {
		attack.setWasInternalAttack(internalAttack);
	}

	public void setSsid(String ssid) {
		network.setSsid(ssid);
	}

	public void setTimestamp(long timestamp) {
		message.setTimestamp(timestamp);
	}

	public void setTimestampLocation(long timestampLocation) {
		network.setTimestampLocation(timestampLocation);
	}

	public void setType(MessageRecord.TYPE type) {
		message.setType(type);
	}

	@Override
	public String toString() {
		return toString(null);
	}

	public String toString(Formatter formatter) {
		if (null == formatter) {
			return Formatter.getDefault().format(this);
		}
		return formatter.format(this);
	}
}