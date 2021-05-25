package dk.aau.netsec.hostage.deprecated;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import dk.aau.netsec.hostage.logging.MessageRecord;
import dk.aau.netsec.hostage.logging.Record;
import dk.aau.netsec.hostage.model.Profile;
import dk.aau.netsec.hostage.ui.model.LogFilter;


/**
 * This class creates SQL tables and handles all access to the database.<br>
 * It contains several methods with predefined queries to extract different
 * kinds of information from the database.<br>
 * The database contains two tables: {@link #TABLE_RECORDS} and
 * {@link #TABLE_BSSIDS}:<br>
 * {@link #TABLE_RECORDS} contains all hostage.logging information of a single message
 * record except the SSID.<br>
 * {@link #TABLE_BSSIDS} contains the BSSID of all recorded Networks and the
 * corresponding SSID.<br>
 * 
 * @author Lars Pandikow
 */
public class UglyDbHelper extends SQLiteOpenHelper {

	// All Static variables
	// Database Version
	private static final int DATABASE_VERSION = 1;

	// Database Name
	private static final String DATABASE_NAME = "hostage.db";

	// Contacts table names
	private static final String TABLE_ATTACK_INFO = "attack";
	private static final String TABLE_RECORDS = "packet";
	private static final String TABLE_BSSIDS = "network";
	private static final String TABLE_PROFILES = "profiles";

	// Contacts Table Columns names
	public static final String KEY_ID = "_id";
	public static final String KEY_ATTACK_ID = "_attack_id";
	public static final String KEY_TYPE = "type";
	public static final String KEY_TIME = "packet_timestamp";
	public static final String KEY_PACKET = "packet";
	public static final String KEY_PROTOCOL = "protocol";
	public static final String KEY_EXTERNAL_IP = "externalIP";
	public static final String KEY_LOCAL_IP = "localIP";
	public static final String KEY_LOCAL_PORT = "localPort";
	public static final String KEY_REMOTE_IP = "remoteIP";
	public static final String KEY_REMOTE_PORT = "remotePort";
	public static final String KEY_BSSID = "_bssid";
	public static final String KEY_SSID = "ssid";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_ACCURACY = "accuracy";
	public static final String KEY_GEO_TIMESTAMP = "geo_timestamp";

	public static final String KEY_PROFILE_ID = "_profile_id";
	public static final String KEY_PROFILE_NAME = "profile_name";
	public static final String KEY_PROFILE_DESCRIPTION = "profile_description";
	public static final String KEY_PROFILE_ICON = "profile_icon";
	public static final String KEY_PROFILE_EDITABLE = "profile_editable";
	public static final String KEY_PROFILE_ACTIVE = "profile_active";
	public static final String KEY_PROFILE_ICON_NAME = "profile_icon_name";

	// Database sql create statements
	private static final String CREATE_PROFILE_TABLE = "CREATE TABLE " + TABLE_PROFILES + "(" + KEY_PROFILE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ KEY_PROFILE_NAME + " TEXT," + KEY_PROFILE_DESCRIPTION + " TEXT," + KEY_PROFILE_ICON + " TEXT," + KEY_PROFILE_ICON_NAME + " TEXT,"
			+ KEY_PROFILE_EDITABLE + " INTEGER," + KEY_PROFILE_ACTIVE + " INTEGER" + ")";

	private static final String CREATE_RECORD_TABLE = "CREATE TABLE " + TABLE_RECORDS + "(" + KEY_ID + " INTEGER NOT NULL," + KEY_ATTACK_ID
			+ " INTEGER NOT NULL," + KEY_TYPE + " TEXT," + KEY_TIME + " INTEGER," + KEY_PACKET + " TEXT," + "FOREIGN KEY(" + KEY_ATTACK_ID + ") REFERENCES "
			+ TABLE_ATTACK_INFO + "(" + KEY_ATTACK_ID + ")," + "PRIMARY KEY(" + KEY_ID + ", " + KEY_ATTACK_ID + ")" + ")";

	private static final String CREATE_ATTACK_INFO_TABLE = "CREATE TABLE " + TABLE_ATTACK_INFO + "(" + KEY_ATTACK_ID + " INTEGER PRIMARY KEY," + KEY_PROTOCOL
			+ " TEXT," + KEY_EXTERNAL_IP + " TEXT," + KEY_LOCAL_IP + " BLOB," + KEY_LOCAL_PORT + " INTEGER," + KEY_REMOTE_IP + " BLOB," + KEY_REMOTE_PORT
			+ " INTEGER," + KEY_BSSID + " TEXT," + "FOREIGN KEY(" + KEY_BSSID + ") REFERENCES " + TABLE_BSSIDS + "(" + KEY_BSSID + ")" + ")";

	private static final String CREATE_BSSID_TABLE = "CREATE TABLE " + TABLE_BSSIDS + "(" + KEY_BSSID + " TEXT PRIMARY KEY," + KEY_SSID + " TEXT,"
			+ KEY_LATITUDE + " INTEGER," + KEY_LONGITUDE + " INTEGER," + KEY_ACCURACY + " INTEGER," + KEY_GEO_TIMESTAMP + " INTEGER" + ")";

	public UglyDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/*
	 * // Contacts Table Columns names private static final String KEY_ID =
	 * "_id"; private static final String KEY_ATTACK_ID = "_attack_id"; private
	 * static final String KEY_TYPE = "type"; private static final String
	 * KEY_TIME = "timestamp"; private static final String KEY_PACKET =
	 * "packet"; private static final String KEY_PROTOCOL = "protocol"; private
	 * static final String KEY_EXTERNAL_IP ="externalIP"; private static final
	 * String KEY_LOCAL_IP = "localIP"; private static final String
	 * KEY_LOCAL_HOSTNAME = "localHostName"; private static final String
	 * KEY_LOCAL_PORT = "localPort"; private static final String KEY_REMOTE_IP =
	 * "remoteIP"; private static final String KEY_REMOTE_HOSTNAME =
	 * "remoteHostName"; private static final String KEY_REMOTE_PORT =
	 * "remotePort"; private static final String KEY_BSSID = "_bssid"; private
	 * static final String KEY_SSID = "ssid"; private static final String
	 * KEY_LATITUDE = "latitude"; private static final String KEY_LONGITUDE =
	 * "longitude"; private static final String KEY_ACCURACY = "accuracy";
	 */

	/**
	 * Gets all received {@link Record Records} for the specified information in
	 * the LogFilter ordered by date.
	 * 
	 * @return A ArrayList with all received {@link Record Records} for the
	 *         LogFilter.
	 */
	public ArrayList<Record> getRecordsForFilter(LogFilter filter) {
		ArrayList<Record> recordList = new ArrayList<Record>();
		String selectQuery = "SELECT  * FROM " + TABLE_RECORDS + " NATURAL JOIN " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID
				+ ")";

		// TIMESTAMPS
		selectQuery = selectQuery + " WHERE " + TABLE_RECORDS + "." + KEY_TIME;
		selectQuery = selectQuery + " < " + filter.getBelowTimestamp();
		selectQuery = selectQuery + " AND " + TABLE_RECORDS + "." + KEY_TIME;
		selectQuery = selectQuery + " > " + filter.getAboveTimestamp();

		if (filter.getBSSIDs() != null && filter.getBSSIDs().size() > 0) {
			selectQuery = selectQuery + " AND ";
			selectQuery = selectQuery + filter.getBSSIDQueryStatement(TABLE_BSSIDS, KEY_BSSID);
		}
		if (filter.getESSIDs() != null && filter.getESSIDs().size() > 0) {
			selectQuery = selectQuery + " AND ";
			selectQuery = selectQuery + filter.getESSIDQueryStatement(TABLE_BSSIDS, KEY_SSID);
		}
		if (filter.getProtocols() != null && filter.getProtocols().size() > 0) {
			selectQuery = selectQuery + " AND ";
			selectQuery = selectQuery + filter.getProtocolsQueryStatement(TABLE_ATTACK_INFO, KEY_PROTOCOL);
		}

		selectQuery = selectQuery + " GROUP BY " + TABLE_RECORDS + "." + KEY_ATTACK_ID;

		if (filter.getSorttype() == LogFilter.SortType.packet_timestamp) {
			// DESC
			selectQuery = selectQuery + " ORDER BY " + filter.getSorttype() + " DESC";
		} else {
			selectQuery = selectQuery + " ORDER BY " + filter.getSorttype();
		}

		System.out.println(selectQuery);
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				Record record = createRecord(cursor);
				// Adding record to list
				recordList.add(record);
			} while (cursor.moveToNext());
		}
		cursor.close();

		// return record list
		db.close();
		return recordList;
	}

	/**
	 * Gets all non duplicate Records For the key BSSID.
	 * 
	 * @return A ArrayList with received Records.
	 */
	public ArrayList<String> getUniqueBSSIDRecords() {
		return this.getUniqueDataEntryForKeyType(KEY_BSSID, TABLE_BSSIDS);
	}

	/**
	 * Gets all non duplicate Records For the key ESSID.
	 * 
	 * @return A ArrayList with received Records.
	 */
	public ArrayList<String> getUniqueESSIDRecords() {
		return this.getUniqueDataEntryForKeyType(KEY_SSID, TABLE_BSSIDS);
	}

	public ArrayList<String> getUniqueESSIDRecordsForProtocol(String protocol) {
		return this.getUniqueIDForProtocol(KEY_SSID, protocol);
	}

	public ArrayList<String> getUniqueBSSIDRecordsForProtocol(String protocol) {
		return this.getUniqueIDForProtocol(KEY_BSSID, protocol);
	}

	private ArrayList<String> getUniqueIDForProtocol(String id, String protocol) {
		ArrayList<String> recordList = new ArrayList<String>();
		String selectQuery = "SELECT DISTINCT " + id + " FROM " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID + ") " + " WHERE "
				+ TABLE_ATTACK_INFO + "." + KEY_PROTOCOL + " = " + "'" + protocol + "'" + " ORDER BY " + id; // " NATURAL JOIN "
																												// +
																												// TABLE_ATTACK_INFO
																												// +
																												// " NATURAL JOIN "
																												// +
																												// TABLE_BSSIDS
																												// +
																												// " NATURAL JOIN "
																												// +
																												// TABLE_PORTS
																												// +

		// ORDERED BY TIME
		System.out.println(selectQuery);
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				String record = cursor.getString(0);
				recordList.add(record);
			} while (cursor.moveToNext());
		}
		cursor.close();

		// return record list
		db.close();
		return recordList;
	}

	/**
	 * Gets all non duplicate Data Entry For a specific KeyType ( e.g. BSSIDs).
	 * 
	 * @return A ArrayList with received Records.
	 */
	public ArrayList<String> getUniqueDataEntryForKeyType(String keyType, String table) {
		ArrayList<String> recordList = new ArrayList<String>();
		// String selectQuery = "SELECT  * FROM " + TABLE_RECORDS +
		// " NATURAL JOIN " + TABLE_ATTACK_INFO + " NATURAL JOIN " +
		// TABLE_BSSIDS + " NATURAL JOIN " + TABLE_PORTS;
		String selectQuery = "SELECT DISTINCT " + keyType + " FROM " + table + " ORDER BY " + keyType; // " NATURAL JOIN "
																										// +
																										// TABLE_ATTACK_INFO
																										// +
																										// " NATURAL JOIN "
																										// +
																										// TABLE_BSSIDS
																										// +
																										// " NATURAL JOIN "
																										// +
																										// TABLE_PORTS
																										// +

		// ORDERED BY TIME
		System.out.println(selectQuery);
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				String record = cursor.getString(0);
				recordList.add(record);
			} while (cursor.moveToNext());
		}
		cursor.close();

		// return record list
		db.close();
		return recordList;
	}

	/**
	 * Adds a given {@link Record} to the database.
	 * 
	 * @param record
	 *            The added {@link Record} .
	 */
	public void addRecord(Record record) {
		SQLiteDatabase db = this.getWritableDatabase();

		HashMap<String, Object> bssidValues = new HashMap<String, Object>();
		bssidValues.put(KEY_BSSID, record.getBssid());
		bssidValues.put(KEY_SSID, record.getSsid());
		bssidValues.put(KEY_LATITUDE, record.getLatitude());
		bssidValues.put(KEY_LONGITUDE, record.getLongitude());
		bssidValues.put(KEY_ACCURACY, record.getAccuracy());
		bssidValues.put(KEY_GEO_TIMESTAMP, record.getTimestampLocation());

		ContentValues attackValues = new ContentValues();
		attackValues.put(KEY_ATTACK_ID, record.getAttack_id()); // Log Attack ID
		attackValues.put(KEY_PROTOCOL, record.getProtocol());
		attackValues.put(KEY_EXTERNAL_IP, record.getExternalIP());
		attackValues.put(KEY_LOCAL_IP, record.getLocalIP()); // Log Local IP
		attackValues.put(KEY_LOCAL_PORT, record.getLocalPort());
		attackValues.put(KEY_REMOTE_IP, record.getRemoteIP()); // Log Remote IP
		attackValues.put(KEY_REMOTE_PORT, record.getRemotePort()); // Log Remote
																	// Port
		attackValues.put(KEY_BSSID, record.getBssid());

		ContentValues recordValues = new ContentValues();
		recordValues.put(KEY_ID, record.getId()); // Log Message Number
		recordValues.put(KEY_ATTACK_ID, record.getAttack_id()); // Log Attack ID
		recordValues.put(KEY_TYPE, record.getType().name()); // Log Type
		recordValues.put(KEY_TIME, record.getTimestamp()); // Log Timestamp
		recordValues.put(KEY_PACKET, record.getPacket()); // Log Packet

		// Inserting Rows
		db.insertWithOnConflict(TABLE_ATTACK_INFO, null, attackValues, SQLiteDatabase.CONFLICT_REPLACE);
		db.insert(TABLE_RECORDS, null, recordValues);
		db.close(); // Closing database connection
		// Update Network Information
		updateNetworkInformation(bssidValues);
	}

	/**
	 * Determines if a network with given BSSID has already been recorded as
	 * malicious.
	 * 
	 * @param BSSID
	 *            The BSSID of the network.
	 * @return True if an attack has been recorded in a network with the given
	 *         BSSID, else false.
	 */
	public boolean bssidSeen(String BSSID) {
		String countQuery = "SELECT  * FROM " + TABLE_BSSIDS + " WHERE " + KEY_BSSID + " = " + "'" + BSSID + "'";
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		int result = cursor.getCount();
		cursor.close();
		db.close();
		return result > 0;
	}

	public int numBssidSeen(String BSSID) {
		String countQuery = "SELECT  COUNT(*) FROM " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID + ")" + " WHERE "
				+ TABLE_BSSIDS + "." + KEY_BSSID + " = " + "'" + BSSID + "'";
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		cursor.moveToFirst();
		int result = cursor.getInt(0);
		cursor.close();
		db.close();
		return result;
	}

	public int numBssidSeen(String protocol, String BSSID) {
		String countQuery = "SELECT  COUNT(*) FROM " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID + ")" + " WHERE "
				+ TABLE_ATTACK_INFO + "." + KEY_PROTOCOL + " = " + "'" + protocol + "'" + " AND " + TABLE_BSSIDS + "." + KEY_BSSID + " = " + "'" + BSSID + "'";
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		cursor.moveToFirst();
		int result = cursor.getInt(0);
		cursor.close();
		db.close();
		return result;
	}

	/**
	 * Determines if an attack has been recorded on a specific protocol in a
	 * network with a given BSSID.
	 * 
	 * @param protocol
	 *            The
	 *            {@link
	 *            } to inspect.
	 * @param BSSID
	 *            The BSSID of the network.
	 * @return True if an attack on the given protocol has been recorded in a
	 *         network with the given BSSID, else false.
	 */
	public boolean bssidSeen(String protocol, String BSSID) {
		String countQuery = "SELECT  * FROM " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID + ")" + " WHERE " + TABLE_ATTACK_INFO
				+ "." + KEY_PROTOCOL + " = " + "'" + protocol + "'" + " AND " + TABLE_BSSIDS + "." + KEY_BSSID + " = " + "'" + BSSID + "'";
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		int result = cursor.getCount();
		cursor.close();
		db.close();
		return result > 0;
	}

	/**
	 * Deletes all records from {@link #TABLE_RECORDS}.
	 */
	public void clearData() {
		SQLiteDatabase db = this.getReadableDatabase();
		db.delete(TABLE_RECORDS, null, null);
		db.delete(TABLE_ATTACK_INFO, null, null);
		db.delete(TABLE_PROFILES, null, null);
		db.close();
	}

	/**
	 * Deletes all records from {@link #TABLE_RECORDS} with a specific BSSID.
	 * 
	 * @param bssid
	 *            The BSSID to match against.
	 */
	public void deleteByBSSID(String bssid) {
		SQLiteDatabase db = this.getReadableDatabase();
		db.delete(TABLE_RECORDS, KEY_BSSID + " = ?", new String[] { bssid });
		db.delete(TABLE_ATTACK_INFO, KEY_BSSID + " = ?", new String[] { bssid });
		db.close();
	}

	// TODO Delete statement �berarbeiten
	/**
	 * Deletes all records from {@link #TABLE_RECORDS} with a time stamp smaller
	 * then the given
	 * 
	 * @param date
	 *            A Date represented in milliseconds.
	 */
	public void deleteByDate(long date) {
		SQLiteDatabase db = this.getReadableDatabase();
		String deleteQuery = "DELETE  FROM " + TABLE_RECORDS + " WHERE " + KEY_TIME + " < " + date;
		// TODO Delete statement �berarbeiten
		// String deleteQuery2 = "DELETE "
		db.execSQL(deleteQuery);
		db.close();
	}

	/**
	 * Deletes all records from {@link #TABLE_RECORDS} with a specific Attack ID.
	 *
	 * @param attackID
	 *            The Attack ID to match against.
	 */
	public void deleteByAttackID(long attackID) {
		SQLiteDatabase db = this.getReadableDatabase();
		db.delete(TABLE_RECORDS, KEY_ATTACK_ID + " = ?", new String[] { String.valueOf(attackID) });
		db.delete(TABLE_ATTACK_INFO, KEY_ATTACK_ID + " = ?", new String[] { String.valueOf(attackID) });
		db.close();
	}

	/**
	 * Returns a String array with all BSSIDs stored in the database.
	 * 
	 * @return String[] of all recorded BSSIDs.
	 */
	public String[] getAllBSSIDS() {
		String selectQuery = "SELECT  * FROM " + TABLE_BSSIDS;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		String[] bssidList = new String[cursor.getCount()];
		int counter = 0;
		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				bssidList[counter] = cursor.getString(0);
				counter++;
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return bssidList;
	}

	/**
	 * Gets all received {@link Record Records} for every attack identified by
	 * its attack id and ordered by date.
	 * 
	 * @return A ArrayList with all received {@link Record Records} for each
	 *         attack id in the Database.
	 */
	public ArrayList<Record> getAllReceivedRecordsOfEachAttack() {
		ArrayList<Record> recordList = new ArrayList<Record>();
		String selectQuery = "SELECT  * FROM " + TABLE_RECORDS + " NATURAL JOIN " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID
				+ ")" + " WHERE " + KEY_TYPE + "='RECEIVE'" + " ORDER BY " + TABLE_RECORDS + "." + KEY_TIME;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				Record record = createRecord(cursor);
				// Adding record to list
				recordList.add(record);
			} while (cursor.moveToNext());
		}
		cursor.close();

		// return record list
		db.close();
		return recordList;
	}

	/**
	 * Gets all {@link Record Records} saved in the database.
	 * 
	 * @return A ArrayList of all the {@link Record Records} in the Database.
	 */
	public ArrayList<Record> getAllRecords() {
		ArrayList<Record> recordList = new ArrayList<Record>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_RECORDS + " NATURAL JOIN " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID
				+ ")";

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		Log.i("Database", "Start loop");
		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				Log.i("Database", "Add Record");
				Record record = createRecord(cursor);
				// Adding record to list
				recordList.add(record);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		// return record list
		return recordList;
	}

	/**
	 * Determines the number of different attack_ids in the database.
	 * 
	 * @return The number of different attack_ids in the database.
	 */
	public int getAttackCount() {
		String countQuery = "SELECT  * FROM " + TABLE_ATTACK_INFO;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);

        if (!cursor.moveToFirst()) return 0;

        int result = cursor.getCount();
		cursor.close();

		// return count
		db.close();
		return result;
	}

	/**
	 * Determines the number of different attack_ids for a specific protocol in
	 * the database.
	 * 
	 * @param protocol
	 *            The String representation of the
	 *            {@link
	 *            }
	 * @return The number of different attack_ids in the database.
	 */
	public int getAttackPerProtocolCount(String protocol) {
		String countQuery = "SELECT  * FROM " + TABLE_ATTACK_INFO + " WHERE " + KEY_PROTOCOL + " = " + "'" + protocol + "'";
		SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(countQuery, null);

        if (!cursor.moveToFirst()) return 0;
		int result = cursor.getCount();

        cursor.close();
		//db.close();

		return result;
	}

	/**
	 * Determines the highest attack id stored in the database.
	 * 
	 * @return The highest attack id stored in the database.
	 */
	public long getHighestAttackId() {
		String selectQuery = "SELECT MAX(" + KEY_ATTACK_ID + ") FROM " + TABLE_ATTACK_INFO;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		int result;

		if (cursor.moveToFirst()) {
			result = cursor.getInt(0);
		} else {
			result = -1;
		}
		cursor.close();
		db.close();
		return result;
	}

	public ArrayList<HashMap<String, Object>> getNetworkInformation() {
		String selectQuery = "SELECT  * FROM " + TABLE_BSSIDS;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		ArrayList<HashMap<String, Object>> networkInformation = new ArrayList<HashMap<String, Object>>();

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				HashMap<String, Object> values = new HashMap<String, Object>();
				values.put(KEY_BSSID, cursor.getString(0));
				values.put(KEY_SSID, cursor.getString(1));
				values.put(KEY_LATITUDE, Double.parseDouble(cursor.getString(2)));
				values.put(KEY_LONGITUDE, Double.parseDouble(cursor.getString(3)));
				values.put(KEY_ACCURACY, Float.parseFloat(cursor.getString(4)));
				values.put(KEY_GEO_TIMESTAMP, cursor.getLong(5));
				networkInformation.add(values);
			} while (cursor.moveToNext());
		}

		cursor.close();
		db.close();
		return networkInformation;
	}

	/**
	 * Gets a single {@link Record} with the given ID from the database.
	 * 
	 * @param id
	 *            The ID of the {@link Record};
	 * @return The {@link Record}.
	 */
	public Record getRecord(int id) {
		String selectQuery = "SELECT  * FROM " + TABLE_RECORDS + " NATURAL JOIN " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID
				+ ")" + " WHERE " + TABLE_RECORDS + "." + KEY_ID + " = " + id;
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.rawQuery(selectQuery, null);
		Record record = null;
		if (cursor.moveToFirst()) {
			record = createRecord(cursor);
		}

		cursor.close();
		db.close();
		// return contact
		return record;
	}

	/**
	 * Determines the number of {@link Record Records} in the database.
	 * 
	 * @return The number of {@link Record Records} in the database.
	 */
	public int getRecordCount() {
		String countQuery = "SELECT  * FROM " + TABLE_RECORDS;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		int result = cursor.getCount();
		cursor.close();

		// return count
		db.close();
		return result;
	}

	/**
	 * Gets a single {@link Record} with the given attack id from the database.
	 * 
	 * @param attack_id
	 *            The attack id of the {@link Record};
	 * @return The {@link Record}.
	 */
	public Record getRecordOfAttackId(long attack_id) {
		String selectQuery = "SELECT  * FROM " + TABLE_RECORDS + " NATURAL JOIN " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID
				+ ")" + " WHERE " + TABLE_RECORDS + "." + KEY_ATTACK_ID + " = " + attack_id + " GROUP BY " + TABLE_RECORDS + "." + KEY_ATTACK_ID;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		Record record = null;

		if (cursor.moveToFirst()) {
			record = createRecord(cursor);
		}
		cursor.close();

		// return record list
		db.close();
		return record;
	}

	/**
	 * Gets a representative {@link Record} for every attack identified by its
	 * attack id.
	 * 
	 * @return A ArrayList with one {@link Record Records} for each attack id in
	 *         the Database.
	 */
	public ArrayList<Record> getRecordOfEachAttack() {
		ArrayList<Record> recordList = new ArrayList<Record>();
		String selectQuery = "SELECT  * FROM " + TABLE_RECORDS + " NATURAL JOIN " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID
				+ ")" + " GROUP BY " + TABLE_RECORDS + "." + KEY_ATTACK_ID;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				Record record = createRecord(cursor);
				// Adding record to list
				recordList.add(record);
			} while (cursor.moveToNext());
		}
		cursor.close();

		// return record list
		db.close();
		return recordList;
	}

	/*
	 * Returns the Conversation of a specific attack id
	 * 
	 * @param attack_id Tha attack id to match the query against.
	 * 
	 * @return A arraylist with all {@link Record Records}s for an attack id.
	 */
	public ArrayList<Record> getConversationForAttackID(long attack_id) {
		ArrayList<Record> recordList = new ArrayList<Record>();
		String selectQuery = "SELECT  * FROM " + TABLE_RECORDS + " NATURAL JOIN " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID
				+ ")" + " WHERE " + TABLE_RECORDS + "." + KEY_ATTACK_ID + " = " + attack_id;

		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		if (cursor.moveToFirst()) {
			do {
				Record record = createRecord(cursor);
				recordList.add(record);
			} while (cursor.moveToNext());
		}
		cursor.close();

		db.close();
		return recordList;
	}

	/**
	 * Gets a representative {@link Record} for every attack with a higher
	 * attack id than the specified.
	 * 
	 * @param attack_id
	 *            The attack id to match the query against.
	 * @return A ArrayList with one {@link Record Records} for each attack id
	 *         higher than the given.
	 */
	public ArrayList<Record> getRecordOfEachAttack(long attack_id) {
		ArrayList<Record> recordList = new ArrayList<Record>();
		String selectQuery = "SELECT  * FROM " + TABLE_RECORDS + " NATURAL JOIN " + TABLE_ATTACK_INFO + " JOIN " + TABLE_BSSIDS + " USING " + "(" + KEY_BSSID
				+ ")" + " WHERE " + TABLE_RECORDS + "." + KEY_ATTACK_ID + " > " + attack_id + " GROUP BY " + TABLE_RECORDS + "." + KEY_ATTACK_ID;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				Record record = createRecord(cursor);
				// Adding record to list
				recordList.add(record);
			} while (cursor.moveToNext());
		}
		cursor.close();

		// return count
		db.close();
		return recordList;
	}

	/**
	 * Determines the smallest attack id stored in the database.
	 * 
	 * @return The smallest attack id stored in the database.
	 */
	public long getSmallestAttackId() {
		String selectQuery = "SELECT MIN(" + KEY_ATTACK_ID + ") FROM " + TABLE_ATTACK_INFO;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		int result;

		if (cursor.moveToFirst()) {
			result = cursor.getInt(0);
		} else {
			result = -1;
		}
		cursor.close();
		db.close();
		return result;
	}

	/**
	 * Gets the last recorded SSID to a given BSSID.
	 * 
	 * @param bssid
	 *            The BSSID to match against.
	 * @return A String of the last SSID or null if the BSSID is not in the
	 *         database.
	 */
	public String getSSID(String bssid) {
		String selectQuery = "SELECT " + KEY_SSID + " FROM " + TABLE_BSSIDS + " WHERE " + KEY_BSSID + " = " + "'" + bssid + "'";
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		String ssid = null;
		if (cursor.moveToFirst()) {
			ssid = cursor.getString(0);
		}
		cursor.close();
		db.close();
		return ssid;
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_BSSID_TABLE);
		db.execSQL(CREATE_ATTACK_INFO_TABLE);
		db.execSQL(CREATE_RECORD_TABLE);
		db.execSQL(CREATE_PROFILE_TABLE);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTACK_INFO);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_BSSIDS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILES);

		// Create tables again
		onCreate(db);
	}

	/**
	 * Retrieves all the profiles from the database
	 * 
	 * @return list of profiles
	 */
	public List<Profile> getAllProfiles() {
		List<Profile> profiles = new LinkedList<Profile>();

		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_PROFILES;

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				Profile profile = new Profile(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getInt(5) == 1);

				if (cursor.getInt(6) == 1) {
					profile.mActivated = true;
				}

				profile.mIconName = cursor.getString(4);

				// Adding record to list
				profiles.add(profile);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		// return record list
		return profiles;
	}

	/**
	 * Persists the given profile into the database
	 * 
	 * @param profile
	 *            the profile which should be persisted
	 * 
	 * @return
	 */
	public long persistProfile(Profile profile) {
		SQLiteDatabase db = this.getReadableDatabase();

		ContentValues values = new ContentValues();

		if (profile.mId != -1) {
			values.put(KEY_PROFILE_ID, profile.mId);
		}

		values.put(KEY_PROFILE_NAME, profile.mLabel);
		values.put(KEY_PROFILE_DESCRIPTION, profile.mText);
		values.put(KEY_PROFILE_ICON, profile.mIconPath);
		values.put(KEY_PROFILE_ICON_NAME, profile.mIconName);
		values.put(KEY_PROFILE_ACTIVE, profile.mActivated);
		values.put(KEY_PROFILE_EDITABLE, profile.mEditable);

		return db.replace(TABLE_PROFILES, null, values);
	}

	/**
	 * private static final String CREATE_PROFILE_TABLE = "CREATE TABLE " +
	 * TABLE_PROFILES + "(" + KEY_PROFILE_ID +
	 * " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_PROFILE_NAME + " TEXT," +
	 * KEY_PROFILE_DESCRIPTION + " TEXT," + KEY_PROFILE_ICON + " TEXT," +
	 * KEY_PROFILE_ICON_ID + " INTEGER," + KEY_PROFILE_EDITABLE + " INTEGER," +
	 * KEY_PROFILE_ACTIVE + " INTEGER" + ")";
	 */
	public Profile getProfile(int id) {
		String selectQuery = "SELECT  * FROM " + TABLE_PROFILES + " WHERE " + TABLE_PROFILES + "." + KEY_PROFILE_ID + " = " + id;
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.rawQuery(selectQuery, null);
		Profile profile = null;

		if (cursor.moveToFirst()) {
			profile = new Profile(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getInt(5) == 1);

			if (cursor.getInt(6) == 1) {
				profile.mActivated = true;
			}

			profile.mIconName = cursor.getString(5);
		}

		cursor.close();
		db.close();

		// return contact
		return profile;
	}

	public void deleteProfile(int id) {
		SQLiteDatabase db = this.getReadableDatabase();

		db.delete(TABLE_PROFILES, KEY_PROFILE_ID + "=?", new String[] { String.valueOf(id) });
	}

	public void updateNetworkInformation(ArrayList<HashMap<String, Object>> networkInformation) {
		Log.i("DatabaseHandler", "Starts updating");
		for (HashMap<String, Object> values : networkInformation) {
			updateNetworkInformation(values);
		}
	}

	public void updateNetworkInformation(HashMap<String, Object> networkInformation) {
		SQLiteDatabase db = this.getReadableDatabase();
		String bssid = (String) networkInformation.get(KEY_BSSID);
		String bssidQuery = "SELECT  * FROM " + TABLE_BSSIDS + " WHERE " + KEY_BSSID + " = " + "'" + bssid + "'";
		Cursor cursor = db.rawQuery(bssidQuery, null);
		int result = cursor.getCount();
		if (cursor != null && cursor.moveToFirst() && (result <= 0 || cursor.getLong(5) < (Long) networkInformation.get(KEY_GEO_TIMESTAMP)))
			;
		{
			ContentValues bssidValues = new ContentValues();
			bssidValues.put(KEY_BSSID, bssid);
			bssidValues.put(KEY_SSID, (String) networkInformation.get(KEY_SSID));
			bssidValues.put(KEY_LATITUDE, (Double) networkInformation.get(KEY_LATITUDE));
			bssidValues.put(KEY_LONGITUDE, (Double) networkInformation.get(KEY_LONGITUDE));
			bssidValues.put(KEY_ACCURACY, (Float) networkInformation.get(KEY_ACCURACY));
			bssidValues.put(KEY_GEO_TIMESTAMP, (Long) networkInformation.get(KEY_GEO_TIMESTAMP));
			db.insertWithOnConflict(TABLE_BSSIDS, null, bssidValues, SQLiteDatabase.CONFLICT_REPLACE);
		}
		cursor.close();
		db.close();
	}

	/**
	 * Creates a {@link Record} from a Cursor. If the cursor does not show to a
	 * valid data structure a runtime exception is thrown.
	 * 
	 * @param cursor
	 * @return Returns the created {@link Record} .
	 */
	private Record createRecord(Cursor cursor) {
		Record record = new Record();
		record.setId(Integer.parseInt(cursor.getString(0)));
		record.setAttack_id(cursor.getLong(1));
		record.setType(MessageRecord.TYPE.valueOf(cursor.getString(2)));
		record.setTimestamp(cursor.getLong(3));
		record.setPacket(cursor.getString(4));
		record.setProtocol(cursor.getString(5));
		record.setExternalIP(cursor.getString(6));

		record.setLocalIP(cursor.getString(7));
		record.setLocalPort(Integer.parseInt(cursor.getString(8)));

		record.setRemoteIP(cursor.getString(9));
		record.setRemotePort(Integer.parseInt(cursor.getString(10)));

		record.setBssid(cursor.getString(11));
		record.setSsid(cursor.getString(12));
		record.setLatitude(Double.parseDouble(cursor.getString(13)));
		record.setLongitude(Double.parseDouble(cursor.getString(14)));
		record.setAccuracy(Float.parseFloat(cursor.getString(15)));
		record.setTimestampLocation(cursor.getLong(16));

		return record;
	}
}
