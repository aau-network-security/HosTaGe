package de.tudarmstadt.informatik.hostage.logging;

import java.io.Serializable;

/**
 * Holds the Information a specific device gathered about a network identified by its BSSID.
 * @author Lars Pandikow
 */
public class SyncInfoRecord implements Serializable{

	private static final long serialVersionUID = 7156818788190434192L;

	private String deviceID;
	private String BSSID;
	private long number_of_attacks;
	private long number_of_portscans;

	/**
	 * @return the deviceID
	 */
	public String getDeviceID() {
		return deviceID;
	}
	/**
	 * @param deviceID the deviceID to set
	 */
	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}
	/**
	 * @return the bSSID
	 */
	public String getBSSID() {
		return BSSID;
	}
	/**
	 * @param bSSID the bSSID to set
	 */
	public void setBSSID(String bSSID) {
		BSSID = bSSID;
	}
	/**
	 * @return the number_of_attacks
	 */
	public long getNumber_of_attacks() {
		return number_of_attacks;
	}
	/**
	 * @param number_of_attacks the number_of_attacks to set
	 */
	public void setNumber_of_attacks(long number_of_attacks) {
		this.number_of_attacks = number_of_attacks;
	}
	/**
	 * @return the number_of_portscans
	 */
	public long getNumber_of_portscans() {
		return number_of_portscans;
	}
	/**
	 * @param number_of_portscans the number_of_portscans to set
	 */
	public void setNumber_of_portscans(long number_of_portscans) {
		this.number_of_portscans = number_of_portscans;
	}

}
