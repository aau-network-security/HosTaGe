package dk.aau.netsec.hostage.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;

import dk.aau.netsec.hostage.logging.RecordAll;


public class LogFilter implements Parcelable {

	public final static String LOG_FILTER_INTENT_KEY = "dk.aau.netsec.hostage.logfilter";

	//private static final String TIMESTAMP_BELOW_KEY = "dk.aau.netsec.hostage.logfilter.timestampbelow";
	//private static final String TIMESTAMP_ABOVE_KEY = "dk.aau.netsec.hostage.logfilter.timestampabove";
	private static final String PROTOCOLS_KEY = "dk.aau.netsec.hostage.logfilter.protocols";
	private static final String ESSID_KEY = "dk.aau.netsec.hostage.logfilter.essid";
	private static final String BSSID_KEY = "dk.aau.netsec.hostage.logfilter.bssid";
	private static final String IP_KEY = "dk.aau.netsec.hostage.logfilter.ip";

	//private static final String SORTTYPE_KEY = "dk.aau.netsec.hostage.logfilter.sorttype";

    /**
     * The SortType
     */
	public enum SortType {
		packet_timestamp(0), protocol(1),ip(2), ssid(3), _bssid(4), _attack_id(7), _id(8);
		private final int id;

		SortType(int id) {
			this.id = id;
		}

		public int getValue() {
			return id;
		}
	}

	public ArrayList<String> BSSIDs;
	public ArrayList<String> ESSIDs;
	public ArrayList<String> IPs;
	public ArrayList<String> protocols;

	public boolean isNotEditable;

	public SortType sorttype;

	public long belowTimestamp;
	public long aboveTimestamp;

    /**
     * Constructur
     */
	public LogFilter() {
		this.clear();
	}

    /**
     * Clears / resets all attributes of the filter objects
     * The below timestamp will be maximal and the above timestamp will be minimal (long).
     * The sort type is set to the default: timestamp
     */
	public void clear() {
		this.belowTimestamp = Long.MAX_VALUE;
		this.aboveTimestamp = Long.MIN_VALUE;
		this.sorttype = SortType.packet_timestamp;
		this.BSSIDs = new ArrayList<>();
		this.ESSIDs = new ArrayList<>();
		this.IPs = new ArrayList<>();
		this.protocols = new ArrayList<>();
	}

    @Override
	public int describeContents() {
		return 0;
	}

	// write filter's data to the passed-in Parcel
    @Override
	public void writeToParcel(Parcel out, int flags) {
		HashMap<String, ArrayList<String>> values = new HashMap<>();
		if (this.BSSIDs != null && this.BSSIDs.size() > 0) {
			values.put(BSSID_KEY, this.getBSSIDs());
		}

		if (this.IPs != null && this.IPs.size() > 0) {
			values.put(IP_KEY, this.getIPs());
		}
		if (this.ESSIDs != null && this.ESSIDs.size() > 0) {
			values.put(ESSID_KEY, this.getESSIDs());
		}
		if (this.protocols != null && this.protocols.size() > 0) {
			values.put(PROTOCOLS_KEY, this.getProtocols());
		}
		long[] timeArray = new long[] { this.aboveTimestamp, this.belowTimestamp };
		out.writeMap(values);
		out.writeInt(this.sorttype.getValue());
		out.writeDouble(timeArray.length);
		out.writeLongArray(timeArray);
		out.writeString(this.isNotEditable ? "true" : "false");
	}

    // needed to create a parcel object
	public static final Parcelable.Creator<LogFilter> CREATOR = new Parcelable.Creator<LogFilter>() {
		public LogFilter createFromParcel(Parcel in) {
			return new LogFilter(in);
		}

		public LogFilter[] newArray(int size) {
			return new LogFilter[size];
		}
	};

	/** constructor
     * that takes a (filter) Parcel and gives you an LogFilter populated
	 * with it's values.
     * @param in {@link Parcel parcel}
     * */
	private LogFilter(Parcel in) {
		HashMap<String, ArrayList<String>> values = new HashMap<>();
		in.readMap(values, ArrayList.class.getClassLoader());

		this.BSSIDs = values.get(BSSID_KEY);
		this.ESSIDs = values.get(ESSID_KEY);
		this.IPs = values.get(IP_KEY);
		this.protocols = values.get(protocols);

		if (this.BSSIDs == null)
			this.BSSIDs = new ArrayList<>();
		if(this.IPs == null)
			this.IPs = new ArrayList<>();
		if (this.ESSIDs == null)
			this.ESSIDs = new ArrayList<>();
		if (this.protocols == null)
			this.protocols = new ArrayList<>();

		this.sorttype = SortType.values()[Math.min(in.readInt(), SortType.values().length)];

		int size = (int) in.readDouble();
		long[] timeArray = new long[size];
		in.readLongArray(timeArray);

		this.belowTimestamp = timeArray[1];
		this.aboveTimestamp = timeArray[0];

		String bool = in.readString();
		if (bool.equals("true"))
			this.isNotEditable = true;
	}

    /**
     * If the filter can be edited this method returns false.
     * @return boolean
     */
	public boolean isNotEditable() {
		return this.isNotEditable;
	}

    /**
     * Returns the filter's sorttype
     * @return {@link SortType, sorttype}
     */
	public SortType getSorttype() {
		return this.sorttype;
	}

    /**
     * Returns the filtered essid names.
     * @return ArrayList<String>
     */
	public ArrayList<String> getBSSIDs() {
		return this.BSSIDs;
	}

	/**
	 * Returns the filtered ips names.
	 * @return ArrayList<String>
	 */
	public ArrayList<String> getIPs() {
		return this.IPs;
	}

	/**
     * Returns the filtered bssid names.
     * @return ArrayList<String>
     */
	public ArrayList<String> getESSIDs() {
		return this.ESSIDs;
	}
    /**
     * Returns the filtered protocol names.
     * @return ArrayList<String>
     */
	public ArrayList<String> getProtocols() {
		return this.protocols;
	}

    /**
     * If you don't want a filter to be editable, call this method and insert true
     * @param b boolean
     */
	public void setIsNotEditable(boolean b) {
		this.isNotEditable = b;
	}

    /**
     * Returns the filtered maximal timestamp a entry could have.
     * The default is max long.
     * @return long timestamp
     */
	public long getBelowTimestamp() {
		return this.belowTimestamp;
	}
    /**
     * Returns the filtered minimal timestamp a entry could have.
     * The default is min long.
     * @return long timestamp
     */
	public long getAboveTimestamp() {
		return this.aboveTimestamp;
	}

    /**
     * Set the protocols which a {@link RecordAll Record} can have.
     * @param protocols ArrayList<String>
     */
	public void setProtocols(ArrayList<String> protocols) {
		this.protocols = protocols;
	}

    /**
     * Set the bssids which a {@link RecordAll Record} can have.
     * @param bssids ArrayList<String>
     */
	public void setBSSIDs(ArrayList<String> bssids) {
		this.BSSIDs = bssids;
	}

	/**
	 * Set the ips which a {@link RecordAll Record} can have.
	 * @param ips ArrayList<String>
	 */
	public void setIps(ArrayList<String> ips) {
		this.IPs = ips;
	}

    /**
     * Set the Essids which a {@link RecordAll Record} can have.
     * @param essids ArrayList<String>
     */
	public void setESSIDs(ArrayList<String> essids) {
		this.ESSIDs = essids;
	}

    /**
     * Set the minimal Timestamp a filter {@link RecordAll Record} can have.
     * @param timestamp long
     */
	public void setAboveTimestamp(long timestamp) {
		this.aboveTimestamp = timestamp;
	}

    /**
     * Set the maximal Timestamp a filtered {@link RecordAll Record} can have.
     * @param timestamp long
     */
	public void setBelowTimestamp(long timestamp) {
		this.belowTimestamp = timestamp;
	}

    /**
     * Set the {@link SortType SortType}.
     * @param type SortType
     */
	public void setSorttype(SortType type) {
		this.sorttype = type;
	}

    /**
     * Returns the query statement string for all filtered BSSIDs.
     * This method is used to perform a sql query.
     * @param tablename String, the table name.
     * @param key String, the table column name.
     * @return queryString String
     */
	public String getBSSIDQueryStatement(String tablename, String key) {
		return this.convertArrayListToQueryString(this.BSSIDs, tablename, key);
	}

    /**
     * Returns the query statement string for all filtered ESSIDs.
     * This method is used to perform a sql query.
     * @param tablename String, the table name.
     * @param key String, the table column name.
     * @return queryString String
     */
	public String getESSIDQueryStatement(String tablename, String key) {
		return this.convertArrayListToQueryString(this.ESSIDs, tablename, key);
	}

    /**
     * Returns the query statement string for all filtered protocols.
     * This method is used to perform a sql query.
     * @param tablename String, the table name.
     * @param key String, the table column name.
     * @return queryString String
     */
	public String getProtocolsQueryStatement(String tablename, String key) {
		return this.convertArrayListToQueryString(this.protocols, tablename, key);
	}

    /**
     * Returns true if the filter has any attributes set.
     * @return boolean
     */
	public boolean isSet() {
		boolean hasTime = this.hasATimestamp();
		boolean hasBSSIDs = this.hasBSSIDs();
		boolean hasESSIDs = this.hasESSIDs();
		boolean hasIps = this.hasIps();
		boolean hasProtocols = this.hasProtocols();

		return hasBSSIDs || hasESSIDs || hasIps || hasProtocols | hasTime;
	}

    /**
     * Returns true if the filter has more than one bssid.
     * @return boolean
     */
	public boolean hasBSSIDs() {
		return this.getBSSIDs().size() > 0;
	}

	/**
	 * Returns true if the filter has more than one ip.
	 * @return boolean
	 */
	public boolean hasIps() {
		return this.getIPs().size() > 0;
	}
    /**
     * Returns true if the filter has more than one essid.
     * @return boolean
     */
	public boolean hasESSIDs() {
		return this.getESSIDs().size() > 0;
	}
    /**
     * Returns true if the filter has more than one protocl.
     * @return boolean
     */
	public boolean hasProtocols() {
		return this.getProtocols().size() > 0;
	}

    /**
     * Returns true if the filter has a minimal timestamp.
     * @return boolean
     */
	public boolean hasAboveTimestamp() {
		return this.aboveTimestamp != Long.MIN_VALUE;
	}
    /**
     * Returns true if the filter has a maximal timestamp.
     * @return boolean
     */
	public boolean hasBelowTimestamp() {
		return this.belowTimestamp != Long.MAX_VALUE;
	}
    /**
     * Returns true if the filter has a any timestamp.
     * @return boolean
     */
	public boolean hasATimestamp() {
		return this.hasBelowTimestamp() || this.hasAboveTimestamp();
	}

    /**
     * Returns a query statement to perform a sql query. The given list will be concatenate by an OR statement.
     * @param list ArrayList<String> The entries which should be concatenate.
     * @param table The table name.
     * @param key The table column name.
     * @return queryString string
     */
	public String convertArrayListToQueryString(ArrayList<String> list, String table, String key) {
		String statement = "";
		if (list == null)
			return statement;
		statement = " ( ";

		int i = 0, max = list.size();
		for (String element : list) {
			i++;
			statement = statement + table + "." + key + " = " + "'" + element + "'";
			if (i == max)
				continue;
			statement = statement + " OR ";
		}
		statement = statement + " ) ";

		return statement;
	}

}
