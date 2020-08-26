package dk.aau.netsec.hostage.logging;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Holds all necessary information about a single network.
 */
@Entity
public class NetworkRecord extends RecordAll implements Parcelable, Serializable {

	private static final long serialVersionUID = -1586629159904177836L;
	private String bssid;
	private String ssid;
	@Id
	private long timestampLocation;
	private double latitude;
	private double longitude;
	private float accuracy;

	public static final Parcelable.Creator<NetworkRecord> CREATOR = new Parcelable.Creator<NetworkRecord>() {
		@Override
		public NetworkRecord createFromParcel(Parcel source) {
			return new NetworkRecord(source);
		}

		@Override
		public NetworkRecord[] newArray(int size) {
			return new NetworkRecord[size];
		}
	};

	public NetworkRecord() {

	}

	public NetworkRecord(Parcel source) {
        super();
		this.bssid = source.readString();
		this.ssid = source.readString();
		this.timestampLocation = source.readLong();
		this.latitude = source.readDouble();
		this.longitude = source.readDouble();
		this.accuracy = source.readFloat();
	}

	@Generated(hash = 1853639091)
	public NetworkRecord(String bssid, String ssid, long timestampLocation, double latitude, double longitude, float accuracy) {
		this.bssid = bssid;
		this.ssid = ssid;
		this.timestampLocation = timestampLocation;
		this.latitude = latitude;
		this.longitude = longitude;
		this.accuracy = accuracy;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(bssid);
		dest.writeString(ssid);
		dest.writeLong(timestampLocation);
		dest.writeDouble(latitude);
		dest.writeDouble(longitude);
		dest.writeFloat(accuracy);

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
	 * @return the ssid
	 */
	public String getSsid() {
		return ssid;
	}

	/**
	 * @param ssid
	 *            the ssid to set
	 */
	public void setSsid(String ssid) {
		this.ssid = ssid;
	}

	/**
	 * @return the timestampLocation
	 */
	public long getTimestampLocation() {
		return timestampLocation;
	}

	/**
	 * @param timestampLocation
	 *            the timestampLocation to set
	 */
	public void setTimestampLocation(long timestampLocation) {
		this.timestampLocation = timestampLocation;
	}

	/**
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude
	 *            the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude
	 *            the longitude to set
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * @return the accuracy
	 */
	public float getAccuracy() {
		return accuracy;
	}

	/**
	 * @param accuracy
	 *            the accuracy to set
	 */
	public void setAccuracy(float accuracy) {
		this.accuracy = accuracy;
	}

	public String toJSONString() {
		return String.format("{\"bssid\":\"%s\",\"ssid\":%s,\"latitude\":%s,\"longitude\":%s,\"timestamp\":%s,\"attacks\":%d,\"portscans\":%d}", bssid, ssid,
				latitude, longitude, timestampLocation, -1, -1);
	}

    @Override
    public String toString() {
        return "NetworkRecord{" +
                "bssid='" + bssid + '\'' +
                ", ssid='" + ssid + '\'' +
                ", timestampLocation=" + timestampLocation +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                '}';
    }
}
