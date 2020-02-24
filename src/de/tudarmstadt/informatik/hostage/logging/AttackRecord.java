package de.tudarmstadt.informatik.hostage.logging;

import java.io.Serializable;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import de.tudarmstadt.informatik.hostage.persistence.HostageDBOpenHelper;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

/**
 * Holds all necessary information about a single attack.
 */
public class AttackRecord implements Parcelable, Serializable {

	private static final long serialVersionUID = 6111024905373724227L;

	private long attack_id;
    private long sync_id;
	private String bssid;
    private String device;
	private String protocol;
	private String localIP;
	private int localPort;
	private String remoteIP;
	private int remotePort;
	private String externalIP;
	private int wasInternalAttack; // 1 if attacker ip and local ip were in same subnet, else 0

	public static final Parcelable.Creator<AttackRecord> CREATOR = new Parcelable.Creator<AttackRecord>() {
		@Override
		public AttackRecord createFromParcel(Parcel source) {
			return new AttackRecord(source);
		}

		@Override
		public AttackRecord[] newArray(int size) {
			return new AttackRecord[size];
		}
	};

	public AttackRecord() {

	}

	public AttackRecord(Parcel source) {
		this.attack_id = source.readLong();
		this.protocol = source.readString();
		this.localIP = source.readString();
		this.localPort = source.readInt();
		this.remoteIP = source.readString();
		this.remotePort = source.readInt();
		this.externalIP = source.readString();
		this.wasInternalAttack = source.readInt();
		this.bssid = source.readString();
        this.device = source.readString();
        this.sync_id = source.readLong();
	}

    public AttackRecord(boolean autoincrement){
        if (autoincrement){
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext());

            SharedPreferences.Editor editor = pref.edit();
            int attack_id = pref.getInt("ATTACK_ID_COUNTER", 0);
            editor.putInt("ATTACK_ID_COUNTER", attack_id + 1);
            editor.commit();
            this.attack_id = attack_id;
            this.sync_id = attack_id;

            SyncDevice currentDevice = SyncDevice.currentDevice();
            currentDevice.setHighest_attack_id(attack_id);
            this.setDevice(currentDevice.getDeviceID());
        }

    }

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(attack_id);
		dest.writeString(protocol);
		dest.writeString(localIP);
		dest.writeInt(localPort);
		dest.writeString(remoteIP);
		dest.writeInt(remotePort);
		dest.writeString(externalIP);
		dest.writeInt(wasInternalAttack);
		dest.writeString(bssid);
        dest.writeString(device);
        dest.writeLong(sync_id);
	}

	/**
	 * @return the attack_id
	 */
	public long getAttack_id() {
		return attack_id;
	}

	/**
	 * @param attack_id
	 *            the attack_id to set
	 */
	public void setAttack_id(long attack_id) {
		this.attack_id = attack_id;
	}

	/**
	 * @return the bssid
	 */
	public String getBssid() {
		return bssid;
	}

	/**
	 * @param bssid
	 *            the bssid to set
	 */
	public void setBssid(String bssid) {
		this.bssid = bssid;
	}

	/**
	 * @return the protocol
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * @param protocol
	 *            the protocol to set
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * @return the localIP
	 */
	public String getLocalIP() {
		return localIP;
	}

	/**
	 * @param localIP
	 *            the localIP to set
	 */
	public void setLocalIP(String localIP) {
		this.localIP = localIP;
	}

	/**
	 * @return the localPort
	 */
	public int getLocalPort() {
		return localPort;
	}

	/**
	 * @param localPort
	 *            the localPort to set
	 */
	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	/**
	 * @return the remoteIP
	 */
	public String getRemoteIP() {
		return remoteIP;
	}

	/**
	 * @param remoteIP
	 *            the remoteIP to set
	 */
	public void setRemoteIP(String remoteIP) {
		this.remoteIP = remoteIP;
	}

	/**
	 * @return the remotePort
	 */
	public int getRemotePort() {
		return remotePort;
	}

    public long getSync_id(){return sync_id;}
    public String getDevice(){return device;}
    public void setDevice(String d){this.device = d;}
    public void setSync_id(long i){this.sync_id = i;}
	/**
	 * @param remotePort
	 *            the remotePort to set
	 */
	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	/**
	 * @return the externalIP
	 */
	public String getExternalIP() {
		return externalIP;
	}

	/**
	 * @param externalIP
	 *            the externalIP to set
	 */
	public void setExternalIP(String externalIP) {
		this.externalIP = externalIP;
	}

	public boolean getWasInternalAttack() {return wasInternalAttack == 1;}
	public void setWasInternalAttack(boolean b) {wasInternalAttack = b ? 1 : 0;}
}
