package de.tudarmstadt.informatik.hostage.logging;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Holds the Information a specific device gathered about a network identified by its BSSID.
 * @author Lars Pandikow
 */
@Entity
public class SyncInfoRecord implements Serializable{

	private static final long serialVersionUID = 7156818788190434192L;
	@Id
	private String deviceID;
	private String BSSID;
	private long number_of_attacks;
	private long number_of_portscans;

	@Generated(hash = 1640797190)
	public SyncInfoRecord(String deviceID, String BSSID, long number_of_attacks,
			long number_of_portscans) {
		this.deviceID = deviceID;
		this.BSSID = BSSID;
		this.number_of_attacks = number_of_attacks;
		this.number_of_portscans = number_of_portscans;
	}
	@Generated(hash = 1014952315)
	public SyncInfoRecord() {
	}

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
