package de.tudarmstadt.informatik.hostage.persistence;

import java.lang.reflect.Array;
import java.sql.SQLInput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;
import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.logging.NetworkRecord;
import de.tudarmstadt.informatik.hostage.logging.Record;
import de.tudarmstadt.informatik.hostage.logging.SyncDevice;
import de.tudarmstadt.informatik.hostage.logging.SyncInfo;
import de.tudarmstadt.informatik.hostage.logging.SyncInfoRecord;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord.TYPE;
import de.tudarmstadt.informatik.hostage.logging.SyncRecord;
import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBContract.AttackEntry;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBContract.NetworkEntry;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBContract.PacketEntry;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBContract.ProfileEntry;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBContract.SyncDeviceEntry;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBContract.SyncInfoEntry;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.helper.ColorSequenceGenerator;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;
import de.tudarmstadt.informatik.hostage.ui.model.PlotComparisonItem;

/**
 * Database Helper class to create, read and write the database.
 * @author Mihai Plasoianu
 * @author Lars Pandikow
 *
 */
public class HostageDBOpenHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "hostage.db";
	private static final int DATABASE_VERSION = 3;
	private Context context;

	static {
        // NETWORK
		StringBuilder networkSQLBuilder = new StringBuilder("CREATE TABLE ").append(NetworkEntry.TABLE_NAME).append("(");
		networkSQLBuilder.append(NetworkEntry.COLUMN_NAME_BSSID).append(" TEXT PRIMARY KEY,");
		networkSQLBuilder.append(NetworkEntry.COLUMN_NAME_SSID).append(" TEXT,");
		networkSQLBuilder.append(NetworkEntry.COLUMN_NAME_LATITUDE).append(" INTEGER,");
		networkSQLBuilder.append(NetworkEntry.COLUMN_NAME_LONGITUDE).append(" INTEGER,");
		networkSQLBuilder.append(NetworkEntry.COLUMN_NAME_ACCURACY).append(" INTEGER,");
		networkSQLBuilder.append(NetworkEntry.COLUMN_NAME_GEO_TIMESTAMP).append(" INTEGER");
		networkSQLBuilder.append(")");
		SQL_CREATE_NETWORK_ENTRIES = networkSQLBuilder.toString();

        // ATTACK
		StringBuilder attackSQLBuilder = new StringBuilder("CREATE TABLE ").append(AttackEntry.TABLE_NAME).append("(");
		attackSQLBuilder.append(AttackEntry.COLUMN_NAME_ATTACK_ID).append(" INTEGER PRIMARY KEY,");
		attackSQLBuilder.append(AttackEntry.COLUMN_NAME_PROTOCOL).append(" TEXT,");
		attackSQLBuilder.append(AttackEntry.COLUMN_NAME_EXTERNAL_IP).append(" TEXT,");
		attackSQLBuilder.append(AttackEntry.COLUMN_NAME_LOCAL_IP).append(" BLOB,");
		attackSQLBuilder.append(AttackEntry.COLUMN_NAME_LOCAL_PORT).append(" INTEGER,");
		attackSQLBuilder.append(AttackEntry.COLUMN_NAME_REMOTE_IP).append(" BLOB,");
		attackSQLBuilder.append(AttackEntry.COLUMN_NAME_REMOTE_PORT).append(" INTEGER,");
		attackSQLBuilder.append(AttackEntry.COLUMN_NAME_INTERNAL_ATTACK).append(" INTEGER,");
		attackSQLBuilder.append(AttackEntry.COLUMN_NAME_BSSID).append(" TEXT,");
        attackSQLBuilder.append(AttackEntry.COLUMN_NAME_SYNC_ID).append(" INTEGER,");
        attackSQLBuilder.append(AttackEntry.COLUMN_NAME_DEVICE).append(" TEXT,");
		attackSQLBuilder.append(String.format("FOREIGN KEY(%s) REFERENCES %s(%s) ON DELETE CASCADE ON UPDATE CASCADE,", AttackEntry.COLUMN_NAME_BSSID, NetworkEntry.TABLE_NAME,
				NetworkEntry.COLUMN_NAME_BSSID));
        attackSQLBuilder.append(String.format("FOREIGN KEY(%s) REFERENCES %s(%s) ON DELETE CASCADE ON UPDATE CASCADE", AttackEntry.COLUMN_NAME_DEVICE, SyncDeviceEntry.TABLE_NAME,
                SyncDeviceEntry.COLUMN_NAME_DEVICE_ID));
		attackSQLBuilder.append(")");
		SQL_CREATE_ATTACK_ENTRIES = attackSQLBuilder.toString();

        // PACKET
		StringBuilder packetSQLBuilder = new StringBuilder("CREATE TABLE ").append(PacketEntry.TABLE_NAME).append("(");
		packetSQLBuilder.append(PacketEntry.COLUMN_NAME_ID).append(" INTEGER NOT NULL,");
		packetSQLBuilder.append(PacketEntry.COLUMN_NAME_ATTACK_ID).append(" INTEGER NOT NULL,");
		packetSQLBuilder.append(PacketEntry.COLUMN_NAME_TYPE).append(" TEXT,");
		packetSQLBuilder.append(PacketEntry.COLUMN_NAME_PACKET_TIMESTAMP).append(" INTEGER,");
		packetSQLBuilder.append(PacketEntry.COLUMN_NAME_PACKET).append(" TEXT,");
		packetSQLBuilder.append(String.format("PRIMARY KEY(%s,%s)", PacketEntry.COLUMN_NAME_ID, PacketEntry.COLUMN_NAME_ATTACK_ID));
		packetSQLBuilder.append(String.format("FOREIGN KEY(%s) REFERENCES %s(%s)", PacketEntry.COLUMN_NAME_ATTACK_ID, AttackEntry.TABLE_NAME,
				AttackEntry.COLUMN_NAME_ATTACK_ID));
		packetSQLBuilder.append(")");
		SQL_CREATE_PACKET_ENTRIES = packetSQLBuilder.toString();

        // SyncDeviceEntry
		StringBuilder syncDevicesSQLBuilder = new StringBuilder("CREATE TABLE ").append(SyncDeviceEntry.TABLE_NAME).append("(");
		syncDevicesSQLBuilder.append(SyncDeviceEntry.COLUMN_NAME_DEVICE_ID).append(" TEXT PRIMARY KEY,");
		syncDevicesSQLBuilder.append(SyncDeviceEntry.COLUMN_NAME_DEVICE_TIMESTAMP).append(" INTEGER,");
        syncDevicesSQLBuilder.append(SyncDeviceEntry.COLUMN_NAME_HIGHEST_ATTACK_ID).append(" INTEGER");
        syncDevicesSQLBuilder.append(")");
		SQL_CREATE_SYNC_DEVICES_ENTRIES = syncDevicesSQLBuilder.toString();

        // SyncInfoEntry
		StringBuilder syncInfoSQLBuilder = new StringBuilder("CREATE TABLE ").append(SyncInfoEntry.TABLE_NAME).append("(");
		syncInfoSQLBuilder.append(SyncInfoEntry.COLUMN_NAME_DEVICE_ID).append(" TEXT,");
		syncInfoSQLBuilder.append(SyncInfoEntry.COLUMN_NAME_BSSID).append(" TEXT,");
		syncInfoSQLBuilder.append(SyncInfoEntry.COLUMN_NAME_NUMBER_ATTACKS).append(" INTEGER,");
		syncInfoSQLBuilder.append(SyncInfoEntry.COLUMN_NAME_NUMBER_PORTSCANS).append(" INTEGER,");
		syncInfoSQLBuilder.append(String.format("PRIMARY KEY(%s,%s)", SyncInfoEntry.COLUMN_NAME_DEVICE_ID, SyncInfoEntry.COLUMN_NAME_BSSID));
		syncInfoSQLBuilder.append(String.format("FOREIGN KEY(%s) REFERENCES %s(%s)", SyncInfoEntry.COLUMN_NAME_BSSID, NetworkEntry.TABLE_NAME,
				NetworkEntry.COLUMN_NAME_BSSID));
		syncInfoSQLBuilder.append(")");
		SQL_CREATE_SYNC_INFO_ENTRIES = syncInfoSQLBuilder.toString();

        // ProfileEntry
		StringBuilder profilSQLBuilder = new StringBuilder("CREATE TABLE ").append(ProfileEntry.TABLE_NAME).append("(");
		profilSQLBuilder.append(ProfileEntry.COLUMN_NAME_PROFILE_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT,");
		profilSQLBuilder.append(ProfileEntry.COLUMN_NAME_PROFILE_NAME).append(" TEXT,");
		profilSQLBuilder.append(ProfileEntry.COLUMN_NAME_PROFILE_DESCRIPTION ).append(" TEXT,");
		profilSQLBuilder.append(ProfileEntry.COLUMN_NAME_PROFILE_ICON).append(" TEXT,");
		profilSQLBuilder.append(ProfileEntry.COLUMN_NAME_PROFILE_ICON_NAME).append(" TEXT,");
		profilSQLBuilder.append(ProfileEntry.COLUMN_NAME_PROFILE_EDITABLE).append(" INTEGER,");
		profilSQLBuilder.append(ProfileEntry.COLUMN_NAME_PROFILE_ACTIVE).append(" INTEGER");
		profilSQLBuilder.append(")");
		SQL_CREATE_PROFILE_ENTRIES = profilSQLBuilder.toString();
	}

	private static final String SQL_CREATE_NETWORK_ENTRIES;
	private static final String SQL_CREATE_ATTACK_ENTRIES;
	private static final String SQL_CREATE_PACKET_ENTRIES;
	private static final String SQL_CREATE_PROFILE_ENTRIES;
	private static final String SQL_CREATE_SYNC_DEVICES_ENTRIES;
	private static final String SQL_CREATE_SYNC_INFO_ENTRIES;

	private static final String SQL_DELETE_PACKET_ENTRIES = "DROP TABLE IF EXISTS " + PacketEntry.TABLE_NAME;
	private static final String SQL_DELETE_ATTACK_ENTRIES = "DROP TABLE IF EXISTS " + AttackEntry.TABLE_NAME;
	private static final String SQL_DELETE_NETWORK_ENTRIES = "DROP TABLE IF EXISTS " + NetworkEntry.TABLE_NAME;
	private static final String SQL_DELETE_PROFILE_ENTRIES = "DROP TABLE IF EXISTS " + ProfileEntry.TABLE_NAME;
	private static final String SQL_DELETE_SYNC_DEVICES_ENTRIES = "DROP TABLE IF EXISTS " + SyncDeviceEntry.TABLE_NAME;
	private static final String SQL_DELETE_SYNC_INFO_ENTRIES = "DROP TABLE IF EXISTS " + SyncInfoEntry.TABLE_NAME;

	public HostageDBOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;

        this.generateCurrentDevice();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SYNC_DEVICES_ENTRIES);
        db.execSQL(SQL_CREATE_NETWORK_ENTRIES);
		db.execSQL(SQL_CREATE_ATTACK_ENTRIES);
		db.execSQL(SQL_CREATE_PACKET_ENTRIES);
		db.execSQL(SQL_CREATE_PROFILE_ENTRIES);
		db.execSQL(SQL_CREATE_SYNC_INFO_ENTRIES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_SYNC_DEVICES_ENTRIES);
        db.execSQL(SQL_DELETE_SYNC_INFO_ENTRIES);
		db.execSQL(SQL_DELETE_PACKET_ENTRIES);
		db.execSQL(SQL_DELETE_ATTACK_ENTRIES);
		db.execSQL(SQL_DELETE_PROFILE_ENTRIES);
		db.execSQL(SQL_DELETE_NETWORK_ENTRIES);
		onCreate(db);
	}

    private static  SyncDevice thisDevice = null;
    public static SyncDevice currentDevice(){
        if (thisDevice != null){
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext());
            int attack_id = pref.getInt("ATTACK_ID_COUNTER", 0);
            thisDevice.setHighest_attack_id(attack_id);
        }
        return thisDevice;
    }
    /**
     * Returns a SyncDevice Object representing the current device.
     * @return {@link de.tudarmstadt.informatik.hostage.logging.SyncDevice}
     */
    public SyncDevice generateCurrentDevice()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.context);
        int attack_id = pref.getInt("ATTACK_ID_COUNTER", 0);

        // IF THE SHARED INSTANCE IS NOT AVAILABLE GET IT
        if (thisDevice == null){
            String deviceUUID = pref.getString("CURRENT_DEVICE_IDENTIFIER", UUID.randomUUID().toString());

            String selectQuery = "SELECT * FROM " + HostageDBContract.SyncDeviceEntry.TABLE_NAME + " D "
                    + " WHERE " + " D." + HostageDBContract.SyncDeviceEntry.COLUMN_NAME_DEVICE_ID + " = " + "'"+deviceUUID+"'";
            //HostageDBOpenHelper dbh = new HostageDBOpenHelper(MainActivity.context);

            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(selectQuery, null);

            // IF WE ALREADY HAVE A SYNC DEVICE FOR THE GIVEN DEVICE UUID
            if (cursor.moveToFirst()) {
                SyncDevice record = new SyncDevice();
                record.setDeviceID(cursor.getString(0));
                record.setLast_sync_timestamp(cursor.getLong(1));
                record.setHighest_attack_id(cursor.getLong(2));
                thisDevice = record;
                thisDevice.setHighest_attack_id(attack_id-1);
                cursor.close();
                db.close();
                // return record list
            } else {
                cursor.close();
                db.close();

                // CREATE A NEW SYNC DEVICE
                thisDevice = new SyncDevice();
                // ITS IMPORTANT TO CREATE A COMPLETE NEW DEVICE UUID
                deviceUUID = UUID.randomUUID().toString();
                thisDevice.setDeviceID(deviceUUID);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("CURRENT_DEVICE_IDENTIFIER", thisDevice.getDeviceID());
                editor.commit();
                thisDevice.setLast_sync_timestamp(0);
                thisDevice.setHighest_attack_id(attack_id-1);
                ArrayList<SyncDevice> devices = new ArrayList<SyncDevice>();
                devices.add(thisDevice);
                this.insertSyncDevices(devices);
            }



        }

        thisDevice.setHighest_attack_id(attack_id - 1);
        return thisDevice;
    }

	/**
	 * Adds a given {@link MessageRecord} to the database.
	 * 
	 * @param record
	 *            The added {@link MessageRecord} .
	 */
	synchronized public void addMessageRecord(MessageRecord record) {
		SQLiteDatabase db = this.getWritableDatabase();

		this.insertMessageRecordWithOnConflict(record, db);
		db.close(); // Closing database connection
	}

    /**
     * Adds a given {@link de.tudarmstadt.informatik.hostage.logging.MessageRecord}s to the database.
     *
     * @param records {@link List}<MessageRecord>
     *            The added {@link de.tudarmstadt.informatik.hostage.logging.MessageRecord}s .
     */
    synchronized public void insertMessageRecords(List<MessageRecord> records){
        if(records == null) return;

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            for (MessageRecord record : records){
                this.insertMessageRecordWithOnConflict(record,db);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }
    public void insertMessageRecords(List<MessageRecord> records, SQLiteDatabase db){
        if(records == null) return;

        for (MessageRecord record : records){
            this.insertMessageRecordWithOnConflict(record,db);
        }
    }

    synchronized private void insertMessageRecordWithOnConflict(MessageRecord record, SQLiteDatabase db){
        ContentValues recordValues = new ContentValues();
        recordValues.put(PacketEntry.COLUMN_NAME_ID, record.getId()); // Log Message Number
        recordValues.put(PacketEntry.COLUMN_NAME_ATTACK_ID, record.getAttack_id()); // Log Attack ID
        recordValues.put(PacketEntry.COLUMN_NAME_TYPE, record.getType().name()); // Log Type
        recordValues.put(PacketEntry.COLUMN_NAME_PACKET_TIMESTAMP, record.getTimestamp()); // Log Timestamp
        recordValues.put(PacketEntry.COLUMN_NAME_PACKET, record.getPacket()); // Log Packet

        // Inserting Rows
        db.insertWithOnConflict(PacketEntry.TABLE_NAME, null, recordValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

	/**
	 * Adds a given {@link AttackRecord} to the database.
	 *
	 * @param record
	 *            The added {@link AttackRecord} .
	 */
    synchronized public void addAttackRecord(AttackRecord record) {
		//Log.i("DBHelper", "Add Attack Record with id: " + record.getAttack_id());
		SQLiteDatabase db = this.getWritableDatabase();
		this.insertAttackRecordWithOnConflict(record,db);
		db.close(); // Closing database connection
        ArrayList<SyncDevice> devices = new ArrayList<SyncDevice>();
        devices.add(SyncDevice.currentDevice());
        this.updateSyncDevices(devices);
	}

    /**
     * Updates the own devices and connects attack records without a device to the own device
     */
    synchronized public void updateUntrackedAttacks(){
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT * FROM " + AttackEntry.TABLE_NAME + " A WHERE " + AttackEntry.COLUMN_NAME_DEVICE + " IS NULL ORDER BY " + AttackEntry.COLUMN_NAME_ATTACK_ID + " DESC";
        Cursor cursor = db.rawQuery(selectQuery, null);

        SyncDevice ownDevice = currentDevice();

        long highestID = ownDevice.getHighest_attack_id();

        ArrayList<AttackRecord> records = new ArrayList<AttackRecord>();

        if (cursor.moveToFirst()) {
            do {
                AttackRecord record = this.createAttackRecord(cursor);
                record.setDevice(ownDevice.getDeviceID());
                highestID = (highestID > record.getAttack_id())? highestID : record.getAttack_id();
                records.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        ownDevice.setHighest_attack_id(highestID);


        // UPDATE RECORDS
        if (records.size() > 0){
            db.beginTransaction();
            try {
                for (AttackRecord record : records){
                    this.insertAttackRecordWithOnConflict(record,db);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (highestID != ownDevice.getHighest_attack_id()){
            // THERE WERE ATTACKS WITHOUT A DEVICE ID
            ArrayList<SyncDevice> devices = new ArrayList<SyncDevice>();
            devices.add(ownDevice);
            updateSyncDevices(devices);
        }


        db.close();
    }

    /**
     * Adds a given {@link AttackRecord}s to the database.
     *
     * @param records {@link List}<AttackRecord>
     *            The added {@link AttackRecord}s .
     */
    synchronized public void insertAttackRecords(List<AttackRecord> records) {


        //Log.i("DBHelper", "Add Attack Record with id: " + record.getAttack_id());
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            for (AttackRecord record : records){
                this.insertAttackRecordWithOnConflict(record,db);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close(); // Closing database connection
        ArrayList<SyncDevice> devices = new ArrayList<SyncDevice>();
        devices.add(SyncDevice.currentDevice());
        this.updateSyncDevices(devices);
    }

    synchronized private void insertAttackRecordWithOnConflict(AttackRecord record, SQLiteDatabase db){
        ContentValues attackValues = new ContentValues();
        attackValues.put(AttackEntry.COLUMN_NAME_ATTACK_ID, record.getAttack_id()); // Log Attack ID
        attackValues.put(AttackEntry.COLUMN_NAME_PROTOCOL, record.getProtocol());
        attackValues.put(AttackEntry.COLUMN_NAME_EXTERNAL_IP, record.getExternalIP());
        attackValues.put(AttackEntry.COLUMN_NAME_LOCAL_IP, record.getLocalIP()); // Log Local IP
        attackValues.put(AttackEntry.COLUMN_NAME_LOCAL_PORT, record.getLocalPort());
        attackValues.put(AttackEntry.COLUMN_NAME_REMOTE_IP, record.getRemoteIP()); // Log Remote IP
        attackValues.put(AttackEntry.COLUMN_NAME_REMOTE_PORT, record.getRemotePort()); // Log Remote Port
        attackValues.put(AttackEntry.COLUMN_NAME_INTERNAL_ATTACK, record.getWasInternalAttack());
        attackValues.put(AttackEntry.COLUMN_NAME_BSSID, record.getBssid());
        attackValues.put(AttackEntry.COLUMN_NAME_DEVICE, record.getDevice());
        attackValues.put(AttackEntry.COLUMN_NAME_SYNC_ID, record.getSync_id());

        // Inserting Rows
        db.insertWithOnConflict(AttackEntry.TABLE_NAME, null, attackValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Adds a given {@link SyncRecord}s to the database.
     *
     * @param records {@link List}<AttackRecord>
     *            The added {@link SyncRecord}s .
     */
    synchronized public void insertSyncRecords(List<SyncRecord> records) {
        //Log.i("DBHelper", "Add Attack Record with id: " + record.getAttack_id());
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            for (SyncRecord record : records){
                AttackRecord attackRecord = record.getAttackRecord();
                this.insertAttackRecordWithOnConflict(attackRecord, db);

                if(record.getMessageRecords() == null){
                    MessageRecord msg = new MessageRecord(true);
                    msg.setAttack_id(attackRecord.getAttack_id());
                    msg.setType(MessageRecord.TYPE.RECEIVE);
                    msg.setPacket("");
                    msg.setTimestamp(System.currentTimeMillis());

                    this.insertMessageRecordWithOnConflict(msg, db);
                } else {
                    this.insertMessageRecords(record.getMessageRecords(), db);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        this.updateSyncDevicesMaxID(db);

        db.close(); // Closing database connection

    }

    /**
     * Updates the sync devices max sync id.
     */
    synchronized public void updateSyncDevicesMaxID(SQLiteDatabase db){
        HashMap<String, Long> deviceIDmap = new HashMap<String, Long>();

        String selectQuery = "SELECT "+AttackEntry.COLUMN_NAME_DEVICE+ ","+ AttackEntry.COLUMN_NAME_SYNC_ID+" FROM " + AttackEntry.TABLE_NAME + " A " + " WHERE " + AttackEntry.COLUMN_NAME_SYNC_ID + " NOT NULL "  + " GROUP BY " + AttackEntry.COLUMN_NAME_DEVICE + " HAVING " + AttackEntry.COLUMN_NAME_SYNC_ID + " = MAX( " + AttackEntry.COLUMN_NAME_SYNC_ID + " )";

        {
            Cursor cursor = db.rawQuery(selectQuery, null);
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    String device_id = cursor.getString(0);
                    long sync_id = cursor.getLong(1);

                    deviceIDmap.put(device_id, sync_id);

                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        {
            ArrayList<SyncDevice> allDevices = this.getSyncDevices(db);
            for (SyncDevice device : allDevices){
                Long sync_id = deviceIDmap.get(device.getDeviceID());
                long highestID = device.getHighest_attack_id();
                if (sync_id != null && highestID < sync_id) highestID = sync_id.longValue();
                device.setHighest_attack_id(highestID);
            }
            this.updateSyncDevices(allDevices, db);
        }

    }

    synchronized public void updateSyncAttackCounter(AttackRecord record){
		
		SQLiteDatabase db = this.getWritableDatabase();

		//String mac = HelperUtils.getMacAdress(context);
        SyncDevice currentDevice = SyncDevice.currentDevice();
        ContentValues syncDeviceValues = new ContentValues();
		syncDeviceValues.put(SyncDeviceEntry.COLUMN_NAME_DEVICE_ID, currentDevice.getDeviceID());
		syncDeviceValues.put(SyncDeviceEntry.COLUMN_NAME_DEVICE_TIMESTAMP, System.currentTimeMillis());
		syncDeviceValues.put(SyncDeviceEntry.COLUMN_NAME_HIGHEST_ATTACK_ID, record.getAttack_id());

		db.insertWithOnConflict(SyncDeviceEntry.TABLE_NAME, null, syncDeviceValues, SQLiteDatabase.CONFLICT_REPLACE);
		db.close(); // Closing database connection
	}
	

	/**
	 * Determines if a network with given BSSID has already been recorded as malicious.
	 * 
	 * @param BSSID
	 *            The BSSID of the network.
	 * @return True if an attack has been recorded in a network with the given
	 *         BSSID, else false.
	 */
	public synchronized boolean bssidSeen(String BSSID) {
		String countQuery = "SELECT  * FROM " + NetworkEntry.TABLE_NAME + " WHERE " + NetworkEntry.COLUMN_NAME_BSSID + " = ?";
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, new String[] {BSSID});
		int result = cursor.getCount();
		cursor.close();
		db.close();
		return result > 0;
	}

	/**
	 * Determines if an attack has been recorded on a specific protocol in a
	 * network with a given BSSID.
	 * 
	 * @param protocol
	 *            The
	 *            {@link de.tudarmstadt.informatik.hostage.protocol.Protocol
	 *            Protocol} to inspect.
	 * @param BSSID
	 *            The BSSID of the network.
	 * @return True if an attack on the given protocol has been recorded in a
	 *         network with the given BSSID, else false.
	 */
	public synchronized boolean bssidSeen(String protocol, String BSSID) {
		if(BSSID == null || protocol == null){
			return false;
		}
		String countQuery = "SELECT  * FROM " + AttackEntry.TABLE_NAME + " NATURAL JOIN " + NetworkEntry.TABLE_NAME + " WHERE "
				+ AttackEntry.COLUMN_NAME_PROTOCOL + " = ? AND " + NetworkEntry.COLUMN_NAME_BSSID + " = ?";
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, new String[]{protocol, BSSID});
		int result = cursor.getCount();
		cursor.close();
		db.close();
		return result > 0;
	}
	
	public synchronized int getNumAttacksSeenByBSSID(String BSSID) {
		String countQuery = "SELECT  COUNT(*) FROM " + AttackEntry.TABLE_NAME + " WHERE "
				+ AttackEntry.TABLE_NAME + "." + AttackEntry.COLUMN_NAME_BSSID + " = " + "'" + BSSID + "'";
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		cursor.moveToFirst();
		int result = cursor.getInt(0);
		cursor.close();
		db.close();
		return result;
	}

    synchronized public int getNumAttacksSeenByBSSID(String protocol, String BSSID) {
		String countQuery = "SELECT  COUNT(*) FROM " + AttackEntry.TABLE_NAME 
						+  " WHERE " + AttackEntry.TABLE_NAME + "." + AttackEntry.COLUMN_NAME_PROTOCOL + " = " + "'" + protocol + "'" 
						+  " AND " + AttackEntry.TABLE_NAME + "." + AttackEntry.COLUMN_NAME_BSSID + " = " + "'" + BSSID + "'";
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		cursor.moveToFirst();
		int result = cursor.getInt(0);
		cursor.close();
		db.close();
		return result;
	}


	/**
	 * Returns a String array with all BSSIDs stored in the database.
	 * 
	 * @return ArrayList<String> of all recorded BSSIDs.
	 */
	public synchronized ArrayList<String> getAllBSSIDS() {
		String selectQuery = "SELECT "+NetworkEntry.COLUMN_NAME_BSSID+" FROM " + NetworkEntry.TABLE_NAME;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<String> bssidList = new ArrayList<String>();
		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				String s = cursor.getString(0);
                bssidList.add(s);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return bssidList;
	}

    /**
     * Returns all missing network records.
     *
     * @return a list of missing network records.
     */
    public synchronized ArrayList<NetworkRecord> getMissingNetworkRecords(ArrayList<String> otherBSSIDs) {
        String prefix = " WHERE " + "N."+NetworkEntry.COLUMN_NAME_BSSID + " NOT IN ";
        String selectQuery = "SELECT * FROM " + NetworkEntry.TABLE_NAME + " N " + this.arrayToSQLString(otherBSSIDs, prefix);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<NetworkRecord> networkInformation = new ArrayList<NetworkRecord>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                NetworkRecord record = this.createNetworkRecord(cursor);
                networkInformation.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return networkInformation;
    }

    private String arrayToSQLString(ArrayList<String> a, String prefix)
    {
        String sql = "";

        if (a.size() !=0){
            sql = sql + prefix;
            sql = sql + " ( ";

            int i = 0;
            for (String s : a){
                i++;
                sql = sql + "'" +s + "'";
                if (i != a.size()){
                   sql = sql + ",";
                }
            }
            sql = sql + " ) ";
        }
        return sql;
    }

	/**
	 * Determines the number of different attacks in the database.
	 * 
	 * @return The number of different attacks in the database.
	 */
	public synchronized int getAttackCount() {
		SQLiteDatabase db = this.getReadableDatabase();
		String countQuery = "SELECT  * FROM " + AttackEntry.TABLE_NAME + 
				           " WHERE " + AttackEntry.COLUMN_NAME_PROTOCOL + " <> ?";		
		Cursor cursor = db.rawQuery(countQuery, new String[]{"PORTSCAN"});
		int result = cursor.getCount();
		cursor.close();

		// return count
		db.close();
		return result;
	}
	
	/**
	 * Determines the number of different recorded attacks in a specific access point since the given attack_id.
	 * The given attack_id is not included.
	 * @param attack_id  The attack id to match the query against.
	 * @param bssid  The BSSID of the access point. 
	 * @return The number of different attacks in the database since the given attack_id.
	 */
	public synchronized int getAttackCount(int attack_id, String bssid) {
		SQLiteDatabase db = this.getReadableDatabase();
		String countQuery = "SELECT * FROM " + AttackEntry.TABLE_NAME + 
				           " WHERE "+ AttackEntry.COLUMN_NAME_PROTOCOL + " <> ? " +
				           	"AND " + AttackEntry.COLUMN_NAME_ATTACK_ID + " > ? " +
				           	"AND " + AttackEntry.COLUMN_NAME_BSSID + " = ?";			
		String[] selectArgs = new String[]{"PORTSCAN", attack_id + "", bssid};
		Cursor cursor = db.rawQuery(countQuery, selectArgs);
		int result = cursor.getCount();
		cursor.close();

		// return count
		db.close();
		return result;
	}
	

	/**
	 * Determines the number of different attacks for a specific protocol in
	 * the database.
	 * 
	 * @param protocol
	 *            The String representation of the
	 *            {@link de.tudarmstadt.informatik.hostage.protocol.Protocol
	 *            Protocol}
	 * @return The number of different attacks in the database.
	 */
	public synchronized int getAttackPerProtocolCount(String protocol) {
		SQLiteDatabase db = this.getReadableDatabase();
		String countQuery = "SELECT * FROM " + AttackEntry.TABLE_NAME + 
		                   " WHERE "+ AttackEntry.COLUMN_NAME_PROTOCOL + " = ? ";	
		Cursor cursor = db.rawQuery(countQuery, new String[]{protocol});
		int result = cursor.getCount();
		cursor.close();

		// return count
		db.close();
		return result;
	}
	
	/**
	 * Determines the number of  attacks for a specific protocol in
	 * the database since the given attack_id.
	 * 
	 * @param protocol
	 *            The String representation of the
	 *            {@link de.tudarmstadt.informatik.hostage.protocol.Protocol
	 *            Protocol}
	 * @param attack_id  The attack id to match the query against.
	 * @return The number of different attacks in the database since the given attack_id.
	 */
	public synchronized int getAttackPerProtocolCount(String protocol, int attack_id) {
		SQLiteDatabase db = this.getReadableDatabase();
		String countQuery = "SELECT * FROM " + AttackEntry.TABLE_NAME + 
                		   " WHERE "+ AttackEntry.COLUMN_NAME_PROTOCOL + " = ? " +
                		    "AND " + AttackEntry.COLUMN_NAME_ATTACK_ID + " > ? ";	
		Cursor cursor = db.rawQuery(countQuery, new String[]{protocol, attack_id + ""});
		int result = cursor.getCount();
		cursor.close();

		// return count
		db.close();
		return result;
	}
	
	/**
	 * Determines the number of recorded attacks for a specific protocol and accesss point since the given attack_id.
	 * 
	 * @param protocol
	 *            The String representation of the
	 *            {@link de.tudarmstadt.informatik.hostage.protocol.Protocol
	 *            Protocol}
	 * @param attack_id  The attack id to match the query against.
	 * @param bssid The BSSID of the access point. 
	 * @return The number of different attacks in the database since the given attack_id.
	 */
	public synchronized int getAttackPerProtocolCount(String protocol, int attack_id, String bssid) {
		SQLiteDatabase db = this.getReadableDatabase();
		String countQuery = "SELECT * FROM " + AttackEntry.TABLE_NAME + 
     		   			   " WHERE "+ AttackEntry.COLUMN_NAME_PROTOCOL + " = ? " +
     		   			    "AND " + AttackEntry.COLUMN_NAME_ATTACK_ID + " > ? " +	
						 	"AND " + AttackEntry.COLUMN_NAME_BSSID + " = ?";
		Cursor cursor = db.rawQuery(countQuery, new String[]{protocol, attack_id + "", bssid});
		int result = cursor.getCount();
		cursor.close();

		// return count
		db.close();
		return result;
	}
	
	/**
	 * Determines the number of portscans stored in the database.
	 * 
	 * @return The number of portscans stored in the database.
	 */
	public synchronized int getPortscanCount() {
		return getAttackPerProtocolCount("PORTSCAN");
	}	
	
	/**
	 * Determines the number of recorded portscans since the given attack_id.
	 * @param attack_id  The attack id to match the query against.
	 * @return The number of portscans stored in the database since the given attack_id.
	 */
	public synchronized int getPortscanCount(int attack_id) {
		return getAttackPerProtocolCount("PORTSCAN", attack_id);
	}	
	
	/**
	 * Determines the number of recorded portscans in a specific access point since the given attack_id.
	 * @param attack_id  The attack id to match the query against.
	 * @param bssid The BSSID of the access point. 
	 * @return The number of portscans stored in the database since the given attack_id.
	 */
	public synchronized int getPortscanCount(int attack_id, String bssid) {
		return getAttackPerProtocolCount("PORTSCAN", attack_id, bssid);
	}	


	/**
	 * Determines the number of {@link Record Records} in the database.
	 * 
	 * @return The number of {@link Record Records} in the database.
	 */
	public synchronized int getRecordCount() {
		String countQuery = "SELECT  * FROM " + PacketEntry.TABLE_NAME;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		int result = cursor.getCount();
		cursor.close();

		// return count
		db.close();
		return result;
	}

	//TODO ADD AGAIN ?
	/**
	 * Returns the {@link AttackRecord} with the given attack id from the database.
	 * 
	 * @param attack_id
	 *            The attack id of the {@link Record};
	 * @return The {@link Record}.
	 */
	/*
	public AttackRecord getRecordOfAttackId(long attack_id) {
		String selectQuery = "SELECT  * FROM " + AttackEntry.TABLE_NAME + " WHERE " + AttackEntry.COLUMN_NAME_ATTACK_ID + " = " + attack_id;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		AttackRecord record = null;

		if (cursor.moveToFirst()) {
			record = createAttackRecord(cursor);
		}
		cursor.close();

		// return record list
		db.close();
		return record;
	} */

	/**
	 * Gets a {@link AttackRecord} for every attack identified by its attack id.
	 * 
	 * @return A ArrayList with a {@link AttackRecord} for each attack id in the Database.
	 */
	public synchronized ArrayList<AttackRecord> getRecordOfEachAttack() {
		ArrayList<AttackRecord> recordList = new ArrayList<AttackRecord>();
		String selectQuery = "SELECT  * FROM " + AttackEntry.TABLE_NAME;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				AttackRecord record = createAttackRecord(cursor);
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
	 * Gets a AttackRecord for every attack with a higher attack id than the specified.
	 * 
	 * @param attack_id
	 *            The attack id to match the query against.
	 * @return A ArrayList with one {@link AttackRecord} for each attack id
	 *         higher than the given.
	 */
	public synchronized ArrayList<AttackRecord> getRecordOfEachAttack(long attack_id) {
		ArrayList<AttackRecord> recordList = new ArrayList<AttackRecord>();
		String selectQuery = "SELECT  * FROM " + AttackEntry.TABLE_NAME + " WHERE " + AttackEntry.COLUMN_NAME_ATTACK_ID + " > " + attack_id;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				AttackRecord record = createAttackRecord(cursor);
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
	 * Determines the highest attack id stored in the database.
	 * 
	 * @return The highest attack id stored in the database.
	 */
	public synchronized long getHighestAttackId() {
		String selectQuery = "SELECT MAX(" + AttackEntry.COLUMN_NAME_ATTACK_ID + ") FROM " + AttackEntry.TABLE_NAME;
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
	 * Determines the smallest attack id stored in the database.
	 * 
	 * @return The smallest attack id stored in the database.
	 */
	public synchronized long getSmallestAttackId() {
		String selectQuery = "SELECT MIN(" + AttackEntry.COLUMN_NAME_ATTACK_ID + ") FROM " + AttackEntry.TABLE_NAME;
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
	public synchronized String getSSID(String bssid) {
		String selectQuery = "SELECT " + NetworkEntry.COLUMN_NAME_SSID + " FROM " + NetworkEntry.TABLE_NAME + " WHERE " + NetworkEntry.COLUMN_NAME_BSSID
				+ " = " + "'" + bssid + "'";
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
	
	/**
	 * Gets all network related data stored in the database
	 * @return An ArrayList with an Network for all Entry in the network table.
	 */
	public synchronized ArrayList<NetworkRecord> getNetworkInformation() {
		String selectQuery = "SELECT  * FROM " + NetworkEntry.TABLE_NAME;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		ArrayList<NetworkRecord> networkInformation = new ArrayList<NetworkRecord>();

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				NetworkRecord record = this.createNetworkRecord(cursor);
				networkInformation.add(record);
			} while (cursor.moveToNext());
		}

		cursor.close();
		db.close();
		return networkInformation;
	}

    private synchronized NetworkRecord createNetworkRecord(Cursor cursor){
        NetworkRecord record = new NetworkRecord();
        record.setBssid(cursor.getString(0));
        record.setSsid(cursor.getString(1));
        record.setLatitude(Double.parseDouble(cursor.getString(2)));
        record.setLongitude(Double.parseDouble(cursor.getString(3)));
        record.setAccuracy(Float.parseFloat(cursor.getString(4)));
        record.setTimestampLocation(cursor.getLong(5));
        return record;
    }

	/**
	 * Updates the network table with the information contained in the parameter.
	 * @param networkInformation ArrayList of {@link NetworkRecord NetworkRecords}
	 * @see  {@link HostageDBOpenHelper#updateNetworkInformation(NetworkRecord record)}
	 */
	public synchronized void updateNetworkInformation(ArrayList<NetworkRecord> networkInformation) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.beginTransaction();
        try {
            for (NetworkRecord record : networkInformation) {
                String bssid = record.getBssid();
                String bssidQuery = "SELECT  * FROM " +  NetworkEntry.TABLE_NAME + " WHERE " +  NetworkEntry.COLUMN_NAME_BSSID + " = ?";
                Cursor cursor = db.rawQuery(bssidQuery, new String[] {bssid});
                if (!cursor.moveToFirst() || cursor.getLong(5) < record.getTimestampLocation()){
                    ContentValues bssidValues = new ContentValues();
                    bssidValues.put(NetworkEntry.COLUMN_NAME_BSSID, bssid);
                    bssidValues.put(NetworkEntry.COLUMN_NAME_SSID, record.getSsid());
                    bssidValues.put(NetworkEntry.COLUMN_NAME_LATITUDE, record.getLatitude());
                    bssidValues.put(NetworkEntry.COLUMN_NAME_LONGITUDE, record.getLongitude());
                    bssidValues.put(NetworkEntry.COLUMN_NAME_ACCURACY, record.getAccuracy());
                    bssidValues.put(NetworkEntry.COLUMN_NAME_GEO_TIMESTAMP, record.getTimestampLocation());
                    db.insertWithOnConflict(NetworkEntry.TABLE_NAME, null, bssidValues, SQLiteDatabase.CONFLICT_REPLACE);
                }
                cursor.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        db.close();
	}

	/**
	 * Updated the network table with a new {@link NetworkRecord}.
	 * If information about this BSSID are already in the database, 
	 * the table will only be updated if the new {@link NetworkRecord } 
	 * has a newer hostage.location time stamp.
	 * @param record The new {@link NetworkRecord}.
	 */
	public synchronized void updateNetworkInformation(NetworkRecord record) {
		SQLiteDatabase db = this.getWritableDatabase();
		String bssid = record.getBssid();
		String bssidQuery = "SELECT  * FROM " +  NetworkEntry.TABLE_NAME + " WHERE " +  NetworkEntry.COLUMN_NAME_BSSID + " = ?";
		Cursor cursor = db.rawQuery(bssidQuery, new String[] {bssid});
		if (!cursor.moveToFirst() || cursor.getLong(5) < record.getTimestampLocation()){
			ContentValues bssidValues = new ContentValues();
			bssidValues.put(NetworkEntry.COLUMN_NAME_BSSID, bssid);
			bssidValues.put(NetworkEntry.COLUMN_NAME_SSID, record.getSsid());
			bssidValues.put(NetworkEntry.COLUMN_NAME_LATITUDE, record.getLatitude());
			bssidValues.put(NetworkEntry.COLUMN_NAME_LONGITUDE, record.getLongitude());
			bssidValues.put(NetworkEntry.COLUMN_NAME_ACCURACY, record.getAccuracy());
			bssidValues.put(NetworkEntry.COLUMN_NAME_GEO_TIMESTAMP, record.getTimestampLocation());
			db.insertWithOnConflict(NetworkEntry.TABLE_NAME, null, bssidValues, SQLiteDatabase.CONFLICT_REPLACE);
		}
		cursor.close();
		db.close();
	}
	
	
	/**
	 * Updates the the timestamp of a single device id
	 * @param deviceID The Device id
	 * @param timestamp The synchronization timestamp
	 */
	public synchronized void updateTimestampOfSyncDevice(String deviceID, long timestamp){
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues deviceValues = new ContentValues();
		deviceValues.put(SyncDeviceEntry.COLUMN_NAME_DEVICE_ID, deviceID);
		deviceValues.put(SyncDeviceEntry.COLUMN_NAME_DEVICE_TIMESTAMP, timestamp);
		db.insertWithOnConflict(SyncDeviceEntry.TABLE_NAME, null, deviceValues, SQLiteDatabase.CONFLICT_REPLACE);
		db.close();
	}
	
	/**
	 * Updates the Timestamps of synchronization devices from a HashMap.
	 * @param devices HashMap of device ids and their synchronization timestamps.
	 */
	public synchronized void updateSyncDevices(HashMap<String, Long> devices){
		SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            for(String key : devices.keySet()){
                ContentValues deviceValues = new ContentValues();
                deviceValues.put(SyncDeviceEntry.COLUMN_NAME_DEVICE_ID, key);
                deviceValues.put(SyncDeviceEntry.COLUMN_NAME_DEVICE_TIMESTAMP, devices.get(key));
                db.insertWithOnConflict(SyncDeviceEntry.TABLE_NAME, null, deviceValues, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

		db.close();
	}

    /**
     * Updates sync devices
     * @param devices array list of sync devices
     */
    public synchronized void updateSyncDevices(ArrayList<SyncDevice> devices){
        SQLiteDatabase db = this.getWritableDatabase();
        this.updateSyncDevices(devices, db);
        db.close();
    }

    /**
     * Updates sync devices.
     * @param devices array list of sync devices
     * @param db sqlite database
     */
    public synchronized void updateSyncDevices(ArrayList<SyncDevice> devices, SQLiteDatabase db){
        db.beginTransaction();

        try {
            for(SyncDevice device : devices){
                ContentValues deviceValues = new ContentValues();
                deviceValues.put(SyncDeviceEntry.COLUMN_NAME_DEVICE_ID, device.getDeviceID());
                deviceValues.put(SyncDeviceEntry.COLUMN_NAME_DEVICE_TIMESTAMP, device.getLast_sync_timestamp());
                deviceValues.put(SyncDeviceEntry.COLUMN_NAME_HIGHEST_ATTACK_ID, device.getHighest_attack_id());
                db.insertWithOnConflict(SyncDeviceEntry.TABLE_NAME, null, deviceValues, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Returns the own state containing all registered devices ids and their max sync_id
     * @return {@link de.tudarmstadt.informatik.hostage.logging.SyncInfo}
     */
    public synchronized SyncInfo getOwnState(){
        updateUntrackedAttacks();

        ArrayList<SyncDevice> devices = this.getSyncDevices();

        HashMap<String, Long> deviceMap = new HashMap<String, Long>();
        for (SyncDevice device : devices){
            deviceMap.put(device.getDeviceID(), device.getHighest_attack_id());
        }
        deviceMap.put(SyncDevice.currentDevice().getDeviceID(), SyncDevice.currentDevice().getHighest_attack_id());
        SyncInfo syncInfo = new SyncInfo();
        syncInfo.deviceMap  = deviceMap;
        syncInfo.bssids = this.getAllBSSIDS();
        return syncInfo;
    }
	
	/**
	 * Returns a HashMap of all devices that were previously synchronized with.
	 * @return HashMap containing device id's and the last synchronization timestamp.
	 */
	public synchronized HashMap<String, Long> getSyncDeviceHashMap(){
		SQLiteDatabase db = this.getReadableDatabase();
		HashMap<String, Long> devices = new HashMap<String, Long>();
		
		String query = "SELECT  * FROM " + SyncDeviceEntry.TABLE_NAME;
		Cursor cursor = db.rawQuery(query, null);
		
		if (cursor.moveToFirst()) {
			do {
				devices.put(cursor.getString(0), cursor.getLong(1));
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return devices;
	}

    /**
     * Returns a HashMap of all devices that were previously synchronized with.
     * @return HashMap containing device id's and the last synchronization timestamp.
     */
    public synchronized ArrayList<SyncDevice> getSyncDevices(){
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<SyncDevice> devices = this.getSyncDevices(db);
        db.close();
        return devices;
    }
    public synchronized ArrayList<SyncDevice> getSyncDevices(SQLiteDatabase db){
        ArrayList<SyncDevice> devices = new ArrayList<SyncDevice>();

        String query = "SELECT  * FROM " + SyncDeviceEntry.TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                SyncDevice device = this.createSyncDevice(cursor);
                devices.add(device);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return devices;
    }
	
	/**
	 * Returns a ArrayList containing all information stored in the SyncInfo table.
	 * @return ArrayList<SyncInfo>
	 */
	public synchronized ArrayList<SyncInfoRecord> getSyncInfo(){
		SQLiteDatabase db = this.getReadableDatabase();
		ArrayList<SyncInfoRecord> syncInfo = new ArrayList<SyncInfoRecord>();
		
		String query = "SELECT  * FROM " + SyncInfoEntry.TABLE_NAME;
		Cursor cursor = db.rawQuery(query, null);
		
		if (cursor.moveToFirst()) {
			do {
				SyncInfoRecord info = new SyncInfoRecord();
				info.setDeviceID(cursor.getString(0));
				info.setBSSID(cursor.getString(1));
				info.setNumber_of_attacks(cursor.getLong(2));
				info.setNumber_of_portscans(cursor.getLong(3));
				syncInfo.add(info);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return syncInfo;
	}	
	
	
	
	/**
	 * Updates the sync_info table with the information contained in the parameter.
	 * @param syncInfos ArrayList of {@link SyncInfoRecord SyncInfoRecords}
	 * @see  {@link HostageDBOpenHelper#updateSyncInfo(SyncInfoRecord syncInfo)}
	 */
	public synchronized void updateSyncInfo(ArrayList<SyncInfoRecord> syncInfos){
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            for(SyncInfoRecord syncInfo : syncInfos){
                ContentValues syncValues = new ContentValues();
                syncValues.put(SyncInfoEntry.COLUMN_NAME_BSSID, syncInfo.getBSSID());
                syncValues.put(SyncInfoEntry.COLUMN_NAME_DEVICE_ID, syncInfo.getDeviceID());
                syncValues.put(SyncInfoEntry.COLUMN_NAME_NUMBER_ATTACKS, syncInfo.getNumber_of_attacks());
                syncValues.put(SyncInfoEntry.COLUMN_NAME_NUMBER_PORTSCANS, syncInfo.getNumber_of_portscans());
                db.insertWithOnConflict(SyncInfoEntry.TABLE_NAME, null, syncValues, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        db.close();

	}
	
	/**
	 * Updated the network table with a new {@link SyncInfoRecord}.
	 * Conflicting rows will be replaced.
	 * @param syncInfo The new {@link NetworkRecord}.
	 */
	public synchronized void updateSyncInfo(SyncInfoRecord syncInfo){
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues syncValues = new ContentValues();
		syncValues.put(SyncInfoEntry.COLUMN_NAME_BSSID, syncInfo.getBSSID());
		syncValues.put(SyncInfoEntry.COLUMN_NAME_DEVICE_ID, syncInfo.getDeviceID());
		syncValues.put(SyncInfoEntry.COLUMN_NAME_NUMBER_ATTACKS, syncInfo.getNumber_of_attacks());
		syncValues.put(SyncInfoEntry.COLUMN_NAME_NUMBER_PORTSCANS, syncInfo.getNumber_of_portscans());
		db.insertWithOnConflict(SyncInfoEntry.TABLE_NAME, null, syncValues, SQLiteDatabase.CONFLICT_REPLACE);
		db.close();
	}
	
	/**
	 * Deletes a device with given id from the device {@link de.tudarmstadt.informatik.hostage.persistence.HostageDBContract.SyncDeviceEntry} and also all data captured by this device in  {@link SyncInfoEntry}
	 */
	public synchronized void clearSyncInfos(){
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(SyncDeviceEntry.TABLE_NAME, null, null);
		db.delete(SyncInfoEntry.TABLE_NAME, null, null);
		db.close();
	}
	
	
	/**
	 * Deletes all records from {@link PacketEntry}s and {@link de.tudarmstadt.informatik.hostage.logging.AttackRecord}.
	 */
	public synchronized void clearData() {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(PacketEntry.TABLE_NAME, null, null);
		db.delete(AttackEntry.TABLE_NAME, null, null);
		db.close();
	}

	/**
	 * Deletes all records from {@link PacketEntry}s with a specific BSSID.
	 * 
	 * @param bssid
	 *            The BSSID to match against.
	 */
	public synchronized void deleteByBSSID(String bssid) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(NetworkEntry.TABLE_NAME, NetworkEntry.COLUMN_NAME_BSSID + " = ?", new String[] { bssid });
		db.delete(AttackEntry.TABLE_NAME, AttackEntry.COLUMN_NAME_BSSID + " = ?", new String[]{bssid});
		db.close();
	}
	
	/**
	 * Deletes all records from {@link de.tudarmstadt.informatik.hostage.persistence.HostageDBContract.PacketEntry}s with a time stamp smaller
	 * then the given
	 * 
	 * @param date
	 *            A Date represented in milliseconds.
	 */
	public synchronized void deleteByDate(long date) {
		SQLiteDatabase db = this.getWritableDatabase();
		String deleteQuery = "DELETE  FROM " + PacketEntry.TABLE_NAME + " WHERE " + PacketEntry.COLUMN_NAME_PACKET_TIMESTAMP + " < " + date;
		db.execSQL(deleteQuery);
		db.close();
	}
	
	/**
	 * Deletes all {@link de.tudarmstadt.informatik.hostage.logging.AttackRecord} with a specific Attack ID.
	 *
	 * @param attackID
	 *            The Attack ID to match against.
	 */
	public synchronized void deleteByAttackID(long attackID) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(AttackEntry.TABLE_NAME, AttackEntry.COLUMN_NAME_ATTACK_ID + " = ?", new String[]{String.valueOf(attackID)});
		db.delete(PacketEntry.TABLE_NAME, PacketEntry.COLUMN_NAME_ATTACK_ID + " = ?", new String[]{String.valueOf(attackID)});
		db.close();
	}

    /**
     * Deletes all attacks for the given filter object.
     * @param filter
     */
    public synchronized void deleteAttacksByFilter(LogFilter filter){
        String selectQuery = this.selectionQueryFromFilter(filter, "" + AttackEntry.COLUMN_NAME_ATTACK_ID);
        String deletePacketQuery = "DELETE  FROM " + PacketEntry.TABLE_NAME + " WHERE "+ PacketEntry.TABLE_NAME + "."+ PacketEntry.COLUMN_NAME_ATTACK_ID+" in ( ";
        deletePacketQuery = deletePacketQuery + selectQuery + " )";
        String deleteAttacksQuery = "DELETE  FROM " + AttackEntry.TABLE_NAME + " WHERE "+ AttackEntry.TABLE_NAME + "."+ AttackEntry.COLUMN_NAME_ATTACK_ID+" in ( ";
        deleteAttacksQuery =deleteAttacksQuery + selectQuery + " )";

        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL(deleteAttacksQuery);
        db.execSQL(deletePacketQuery);
        db.close();
    }

    public List<Long> getAllAttackIdsForFilter(LogFilter filter){
        List<Long> results = new ArrayList<Long>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Select All Query
        String selectQuery = this.selectionQueryFromFilter(filter, "" + AttackEntry.COLUMN_NAME_ATTACK_ID);

        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                results.add(cursor.getLong(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return results;
    }

    public ArrayList<MessageRecord> getMessageRecords(AttackRecord attackRecord , SQLiteDatabase db){
        ArrayList<MessageRecord> mr = new ArrayList<MessageRecord>();

        String selectQuery = "SELECT  * FROM " + PacketEntry.TABLE_NAME + " WHERE " + PacketEntry.COLUMN_NAME_ATTACK_ID + " = " + attackRecord.getAttack_id();

        boolean createdDB = false;
        if (db == null){
            db = this.getReadableDatabase();
            createdDB = true;
        }

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                MessageRecord record = createMessageRecord(cursor);
                mr.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (createdDB) db.close();
        return mr;
    }

	/**
	 * Creates a {@link MessageRecord} from a Cursor. If the cursor does not show to a
	 * valid data structure a runtime exception is thrown.
	 * 
	 * @param cursor
	 * @return Returns the created {@link MessageRecord} .
	 */
	private synchronized MessageRecord createMessageRecord(Cursor cursor) {
		MessageRecord record = new MessageRecord();
		record.setId(Integer.parseInt(cursor.getString(0)));
		record.setAttack_id(cursor.getLong(1));
		record.setType(MessageRecord.TYPE.valueOf(cursor.getString(2)));
		record.setTimestamp(cursor.getLong(3));
		record.setPacket(cursor.getString(4));

		return record;
	}
	
	/**
	 * Creates a {@link AttackRecord} from a Cursor. If the cursor does not show to a
	 * valid data structure a runtime exception is thrown.
	 * 
	 * @param cursor
	 * @return Returns the created {@link AttackRecord} .
	 */
	private synchronized AttackRecord createAttackRecord(Cursor cursor) {
		AttackRecord record = new AttackRecord();
		record.setAttack_id(cursor.getLong(0));
		record.setProtocol(cursor.getString(1));
		record.setExternalIP(cursor.getString(2));
		record.setLocalIP(cursor.getString(3));
		record.setLocalPort(Integer.parseInt(cursor.getString(4)));
		record.setRemoteIP(cursor.getString(5));
		record.setRemotePort(Integer.parseInt(cursor.getString(6)));
		record.setWasInternalAttack(cursor.getInt(7) == 1);
		record.setBssid(cursor.getString(8));
        record.setSync_id(cursor.getLong(9));
        record.setDevice(cursor.getString(10));
		return record;
	}

    /**
     * Creates a {@link SyncRecord} from a Cursor. If the cursor does not show to a
     * valid data structure a runtime exception is thrown.
     *
     * @param cursor
     * @return Returns the created {@link SyncRecord} .
     */
    private synchronized SyncRecord createSyncRecord(Cursor cursor, SQLiteDatabase db){
        AttackRecord attackRecord = this.createAttackRecord(cursor);
        SyncRecord record = new SyncRecord(attackRecord);

        ArrayList<MessageRecord> mr = this.getMessageRecords(attackRecord,db);
        record.setMessageRecords(mr);

        return record;
    }
	
	/**
	 * Creates a {@link Record} from a Cursor. If the cursor does not show to a
	 * valid data structure a runtime exception is thrown.
	 * 
	 * @param cursor
	 * @return Returns the created {@link Record} .
	 */
	private synchronized Record createRecord(Cursor cursor) {
		Record record = new Record();
		record.setId(Integer.parseInt(cursor.getString(0)));
		record.setAttack_id(cursor.getLong(1));
		record.setType(TYPE.valueOf(cursor.getString(2)));
		record.setTimestamp(cursor.getLong(3));
		record.setPacket(cursor.getString(4));
		record.setProtocol(cursor.getString(5));
		record.setExternalIP(cursor.getString(6));

		record.setLocalIP(cursor.getString(7));
		record.setLocalPort(Integer.parseInt(cursor.getString(8)));

		record.setRemoteIP(cursor.getString(9));
		record.setRemotePort(Integer.parseInt(cursor.getString(10)));

		record.setWasInternalAttack(Integer.parseInt(cursor.getString(11)) == 1);

		record.setBssid(cursor.getString(12));
        record.setDevice(cursor.getString(cursor.getColumnIndex(AttackEntry.COLUMN_NAME_DEVICE)));
		record.setSync_ID(cursor.getLong(cursor.getColumnIndex(AttackEntry.COLUMN_NAME_SYNC_ID)));

        record.setSsid(cursor.getString(15));
		record.setLatitude(Double.parseDouble(cursor.getString(16)));
		record.setLongitude(Double.parseDouble(cursor.getString(17)));
		record.setAccuracy(Float.parseFloat(cursor.getString(18)));
		record.setTimestampLocation(cursor.getLong(19));

		return record;
	}
		
	/**
	 * Gets all received {@link Record Records} for the specified information in
	 * the LogFilter ordered by date.
	 * 
	 * @return A ArrayList with all received {@link Record Records} for the
	 *         LogFilter.
	 */
	public synchronized ArrayList<Record> getRecordsForFilter(LogFilter filter) {
		ArrayList<Record> recordList = new ArrayList<Record>();
		String selectQuery = this.selectionQueryFromFilter(filter, "*");

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
     * Returns the query for the given filter.
     * @param filter (LogFilter)
     * @param selectionString (String) for everything: "*"
     * @return (String) query string
     */
    public String selectionQueryFromFilter(LogFilter filter, String selectionString)
    {

        String selectQuery = "SELECT " + selectionString + " FROM " + PacketEntry.TABLE_NAME + " NATURAL JOIN " + AttackEntry.TABLE_NAME + " JOIN " + NetworkEntry.TABLE_NAME + " USING " + "(" + NetworkEntry.COLUMN_NAME_BSSID
                + ")";
        if (filter == null) return selectQuery;

        // TIMESTAMPS
        selectQuery = selectQuery + " WHERE " + PacketEntry.TABLE_NAME + "." + PacketEntry.COLUMN_NAME_PACKET_TIMESTAMP;
        selectQuery = selectQuery + " < " + filter.getBelowTimestamp();
        selectQuery = selectQuery + " AND " + PacketEntry.TABLE_NAME + "." + PacketEntry.COLUMN_NAME_PACKET_TIMESTAMP;
        selectQuery = selectQuery + " > " + filter.getAboveTimestamp();

        if (filter.getBSSIDs() != null && filter.getBSSIDs().size() > 0) {
            selectQuery = selectQuery + " AND ";
            selectQuery = selectQuery + filter.getBSSIDQueryStatement(NetworkEntry.TABLE_NAME, NetworkEntry.COLUMN_NAME_BSSID);
        }
        if (filter.getESSIDs() != null && filter.getESSIDs().size() > 0) {
            selectQuery = selectQuery + " AND ";
            selectQuery = selectQuery + filter.getESSIDQueryStatement(NetworkEntry.TABLE_NAME, NetworkEntry.COLUMN_NAME_SSID);
        }
        if (filter.getProtocols() != null && filter.getProtocols().size() > 0) {
            selectQuery = selectQuery + " AND ";
            selectQuery = selectQuery + filter.getProtocolsQueryStatement(AttackEntry.TABLE_NAME, AttackEntry.COLUMN_NAME_PROTOCOL);
        }

        selectQuery = selectQuery + " GROUP BY " + PacketEntry.TABLE_NAME + "." + PacketEntry.COLUMN_NAME_ATTACK_ID;

        if (filter.getSorttype() == LogFilter.SortType.packet_timestamp) {
            // DESC
            selectQuery = selectQuery + " ORDER BY " + filter.getSorttype() + " DESC";
        } else {
            selectQuery = selectQuery + " ORDER BY " + filter.getSorttype();
        }

        System.out.println(selectQuery);
        return selectQuery;
    }

	/**
	 * Returns the Conversation of a specific attack id
	 * 
	 * @param attack_id Tha attack id to match the query against.
	 * 
	 * @return A arraylist with all {@link Record Records}s for an attack id.
	 */
	public synchronized ArrayList<Record> getConversationForAttackID(long attack_id) {
		ArrayList<Record> recordList = new ArrayList<Record>();
		String selectQuery = "SELECT  * FROM " + PacketEntry.TABLE_NAME + " NATURAL JOIN " + AttackEntry.TABLE_NAME + " JOIN " + NetworkEntry.TABLE_NAME + " USING " + "(" + NetworkEntry.COLUMN_NAME_BSSID
				+ ")" + " WHERE " + PacketEntry.TABLE_NAME + "." + PacketEntry.COLUMN_NAME_ATTACK_ID + " = " + attack_id;

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
	 * Gets a single {@link Record} with the given attack id from the database.
	 * 
	 * @param attack_id
	 *            The attack id of the {@link Record};
	 * @return The {@link Record}.
	 */
	public synchronized Record getRecordOfAttackId(long attack_id) {
		String selectQuery = "SELECT  * FROM " + PacketEntry.TABLE_NAME + " NATURAL JOIN " + AttackEntry.TABLE_NAME + " JOIN " + NetworkEntry.TABLE_NAME + " USING " + "(" + NetworkEntry.COLUMN_NAME_BSSID
				+ ")" + " WHERE " + PacketEntry.TABLE_NAME + "." + PacketEntry.COLUMN_NAME_ATTACK_ID + " = " + attack_id + " GROUP BY " + PacketEntry.TABLE_NAME + "." + PacketEntry.COLUMN_NAME_ID;
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
	 * Gets a single {@link Record} with the given ID from the database.
	 * 
	 * @param id
	 *            The ID of the {@link Record};
	 * @return The {@link Record}.
	 */
	public synchronized Record getRecord(int id) {
		String selectQuery = "SELECT  * FROM " + PacketEntry.TABLE_NAME + " NATURAL JOIN " + AttackEntry.TABLE_NAME + " JOIN " + NetworkEntry.TABLE_NAME + " USING " + "(" + PacketEntry.COLUMN_NAME_ATTACK_ID
				+ ")" + " WHERE " + PacketEntry.TABLE_NAME + "." + PacketEntry.COLUMN_NAME_ID + " = " + id;
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
	 * Gets all {@link Record Records} saved in the database.
	 * 
	 * @return A ArrayList of all the {@link Record Records} in the Database.
	 */
	public synchronized ArrayList<Record> getAllRecords() {
		ArrayList<Record> recordList = new ArrayList<Record>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + PacketEntry.TABLE_NAME + " NATURAL JOIN " + AttackEntry.TABLE_NAME + " JOIN " + NetworkEntry.TABLE_NAME + " USING " + "(" + NetworkEntry.COLUMN_NAME_BSSID
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
	 * Gets all non duplicate Records For the key BSSID.
	 * 
	 * @return A ArrayList with received Records.
	 */
	public synchronized ArrayList<String> getUniqueBSSIDRecords() {
		return this.getUniqueDataEntryForKeyType(NetworkEntry.COLUMN_NAME_BSSID, NetworkEntry.TABLE_NAME);
	}

	/**
	 * Gets all non duplicate Records For the key ESSID.
	 * 
	 * @return A ArrayList with received Records.
	 */
	public synchronized ArrayList<String> getUniqueESSIDRecords() {
		return this.getUniqueDataEntryForKeyType(NetworkEntry.COLUMN_NAME_SSID, NetworkEntry.TABLE_NAME);
	}

	public synchronized ArrayList<String> getUniqueESSIDRecordsForProtocol(String protocol) {
		return this.getUniqueIDForProtocol(NetworkEntry.COLUMN_NAME_SSID, protocol);
	}

	public synchronized ArrayList<String> getUniqueBSSIDRecordsForProtocol(String protocol) {
		return this.getUniqueIDForProtocol(NetworkEntry.COLUMN_NAME_BSSID, protocol);
	}

	private synchronized ArrayList<String> getUniqueIDForProtocol(String id, String protocol) {
		ArrayList<String> recordList = new ArrayList<String>();
		String selectQuery = "SELECT DISTINCT " + id + " FROM " + AttackEntry.TABLE_NAME + " JOIN " + NetworkEntry.TABLE_NAME + " USING " + "(" + NetworkEntry.COLUMN_NAME_BSSID + ") " + " WHERE "
				+ AttackEntry.TABLE_NAME + "." + AttackEntry.COLUMN_NAME_PROTOCOL + " = " + "'" + protocol + "'" + " ORDER BY " + "'"+id+"'"; // " NATURAL JOIN "
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
	public synchronized ArrayList<String> getUniqueDataEntryForKeyType(String keyType, String table) {
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
	
	
	//TODO PROFILE DATABASE QUERIES - STILL NEEDED?
	
	/**
	 * Retrieves all the profiles from the database
	 * 
	 * @return list of profiles
	 */
	public synchronized List<Profile> getAllProfiles() {
		List<Profile> profiles = new LinkedList<Profile>();

		// Select All Query
		String selectQuery = "SELECT  * FROM " + ProfileEntry.TABLE_NAME;

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
	public synchronized long persistProfile(Profile profile) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();

		if (profile.mId != -1) {
			values.put(ProfileEntry.COLUMN_NAME_PROFILE_ID, profile.mId);
		}

		values.put(ProfileEntry.COLUMN_NAME_PROFILE_NAME, profile.mLabel);
		values.put(ProfileEntry.COLUMN_NAME_PROFILE_DESCRIPTION, profile.mText);
		values.put(ProfileEntry.COLUMN_NAME_PROFILE_ICON, profile.mIconPath);
		values.put(ProfileEntry.COLUMN_NAME_PROFILE_ICON_NAME, profile.mIconName);
		values.put(ProfileEntry.COLUMN_NAME_PROFILE_ACTIVE, profile.mActivated);
		values.put(ProfileEntry.COLUMN_NAME_PROFILE_EDITABLE, profile.mEditable);

		return db.replace(ProfileEntry.TABLE_NAME, null, values);
	}
	
	/**
	 * private static final String CREATE_PROFILE_TABLE = "CREATE TABLE " +
	 * TABLE_PROFILES + "(" + KEY_PROFILE_ID +
	 * " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_PROFILE_NAME + " TEXT," +
	 * KEY_PROFILE_DESCRIPTION + " TEXT," + KEY_PROFILE_ICON + " TEXT," +
	 * KEY_PROFILE_ICON_ID + " INTEGER," + KEY_PROFILE_EDITABLE + " INTEGER," +
	 * KEY_PROFILE_ACTIVE + " INTEGER" + ")";
	 */
	public synchronized Profile getProfile(int id) {
		String selectQuery = "SELECT  * FROM " + ProfileEntry.TABLE_NAME + " WHERE " + ProfileEntry.TABLE_NAME + "." + ProfileEntry.COLUMN_NAME_PROFILE_ID + " = " + id;
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
	

	public synchronized void deleteProfile(int id) {
		SQLiteDatabase db = this.getWritableDatabase();

		db.delete(ProfileEntry.TABLE_NAME, ProfileEntry.COLUMN_NAME_PROFILE_ID + "=?", new String[] { String.valueOf(id) });
        db.close();
	}
	
	
	/**
	 * Gets all received {@link Record Records} for every attack identified by
	 * its attack id and ordered by date.
	 * 
	 * @return A ArrayList with all received {@link Record Records} for each
	 *         attack id in the Database.
	 */
	public synchronized ArrayList<Record> getAllReceivedRecordsOfEachAttack() {
		ArrayList<Record> recordList = new ArrayList<Record>();
		String selectQuery = "SELECT  * FROM " + PacketEntry.TABLE_NAME + " NATURAL JOIN " + AttackEntry.TABLE_NAME + " JOIN " + NetworkEntry.TABLE_NAME + " USING " + "(" + NetworkEntry.COLUMN_NAME_BSSID
				+ ")" + " WHERE " + PacketEntry.COLUMN_NAME_TYPE + "='RECEIVE'" + " ORDER BY " + PacketEntry.TABLE_NAME + "." + PacketEntry.COLUMN_NAME_PACKET_TIMESTAMP;
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
     * Returns PlotComparisionItems for attacks per essid.
     * @param filter (LogFilter) filter object
     * @return ArrayList<PlotComparisonItem>
     */
    public synchronized ArrayList<PlotComparisonItem> attacksPerESSID(LogFilter filter) {

        String filterQuery = this.selectionQueryFromFilter(filter, AttackEntry.COLUMN_NAME_ATTACK_ID);

        filterQuery = filterQuery.split("GROUP BY")[0];

        String attackPerESSID_Query = "SELECT " + NetworkEntry.COLUMN_NAME_SSID + " , " + "COUNT( " + " * " + "  ) " + " "
                                    + " FROM "  +  AttackEntry.TABLE_NAME + " NATURAL JOIN " + NetworkEntry.TABLE_NAME //AttackEntry.TABLE_NAME + " a " + " , " + NetworkEntry.TABLE_NAME
                                    + " WHERE " + AttackEntry.COLUMN_NAME_ATTACK_ID + " IN " + " ( " + filterQuery + " ) "
                                    + " GROUP BY " + NetworkEntry.TABLE_NAME+"."+NetworkEntry.COLUMN_NAME_SSID;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(attackPerESSID_Query, null);
        ArrayList<PlotComparisonItem> plots = new ArrayList<PlotComparisonItem>();

        int counter = 0;

        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(0); // COLUMN_NAME_SSID
                double value = cursor.getDouble(1); // COUNT
                if (value == 0.) continue;
                PlotComparisonItem plotItem = new PlotComparisonItem(title, this.getColor(counter), 0. , value);
                plots.add(plotItem);

                counter++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return plots;

    }


    /**
     * Creates a {@link de.tudarmstadt.informatik.hostage.logging.SyncDevice} from a Cursor. If the cursor does not show to a
     * valid data structure a runtime exception is thrown.
     *
     * @param cursor the cursor
     * @return Returns the created {@link de.tudarmstadt.informatik.hostage.logging.SyncDevice} .
     */
    private synchronized SyncDevice createSyncDevice(Cursor cursor) {
        SyncDevice record = new SyncDevice();

        record.setDeviceID(cursor.getString(0));
        record.setLast_sync_timestamp(cursor.getLong(1));
        record.setHighest_attack_id(cursor.getLong(2));

        return record;
    }

    /**
     * Returns all missing / newly inserted and updated {@link de.tudarmstadt.informatik.hostage.logging.SyncDevice}s.
     * @param oldDevices array of {@link de.tudarmstadt.informatik.hostage.logging.SyncDevice}s
     * @param includeMissing boolean
     * @return array of {@link de.tudarmstadt.informatik.hostage.logging.SyncDevice}s
     */
    /*
    public ArrayList<SyncDevice> getUpdatedDevicesFor(List<SyncDevice> oldDevices, boolean includeMissing){

        HashMap<String, Long> oldDeviceMap = new HashMap<String, Long>();
        for (SyncDevice d : oldDevices){
            oldDeviceMap.put(d.getDeviceID(),d.getHighest_attack_id());
        }


        ArrayList<SyncDevice> recordList = new ArrayList<SyncDevice>();
        String selectQuery = "SELECT * FROM " + SyncDeviceEntry.TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                SyncDevice record = createSyncDevice(cursor);
                // Adding record to list
                if (oldDeviceMap.containsKey(record.getDeviceID())){
                    Long oldSyncId = oldDeviceMap.get(record.getDeviceID());
                    if (oldSyncId < record.getHighest_attack_id()){
                        recordList.add(record);
                    }
                } else {
                    if (includeMissing)
                        recordList.add(record);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        // return record list
        db.close();
        return recordList;
    }*/


    /**
     * Returns all missing / newly inserted and updated {@link de.tudarmstadt.informatik.hostage.logging.SyncDevice}s.
     * @param oldDeviceMap map with device id and max sync_id for the device
     * @param includeMissing boolean
     * @return array of {@link de.tudarmstadt.informatik.hostage.logging.SyncDevice}s
     */
    public synchronized  ArrayList<SyncDevice> getUpdatedDevicesFor(HashMap<String, Long> oldDeviceMap, boolean includeMissing){

        ArrayList<SyncDevice> recordList = new ArrayList<SyncDevice>();
        String selectQuery = "SELECT * FROM " + SyncDeviceEntry.TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        boolean actualiseOwnDevice = false;
        if (oldDeviceMap.containsKey(currentDevice().getDeviceID()) || includeMissing){
            actualiseOwnDevice = true;
        }

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                SyncDevice record = createSyncDevice(cursor);
                // Adding record to list

                if (currentDevice().getDeviceID().equals(record.getDeviceID()) && actualiseOwnDevice)
                    record.setHighest_attack_id(currentDevice().getHighest_attack_id());

                if (oldDeviceMap.containsKey(record.getDeviceID())){
                    Long oldSyncId = oldDeviceMap.get(record.getDeviceID());
                    if (oldSyncId < record.getHighest_attack_id()){
                        recordList.add(record);
                    }
                } else {
                    if (includeMissing)
                        recordList.add(record);
                }

            } while (cursor.moveToNext());
        }
        cursor.close();

        // return record list
        db.close();
        return recordList;
    }

    /**
     * Returns all device ids.
     * @return list of all device ids.
     */
    public synchronized  ArrayList<String> getAllDevicesIds(){

        String selectQuery = "SELECT "+ SyncDeviceEntry.COLUMN_NAME_DEVICE_ID+" FROM " + SyncDeviceEntry.TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        ArrayList<String> ids = new ArrayList<String>();
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                String s = cursor.getString(0);
                ids.add(s);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // return record list
        db.close();
        return ids;
    }

    /***
     * Returns all missing devices ids
     * @param devices owned device ids
     * @return list of missing devices ids
     */
    public synchronized  ArrayList<String> getMissingDeviceIds(ArrayList<String> devices){
        ArrayList<String> ids = new ArrayList<String>();
        String prefix = " D WHERE " + SyncDeviceEntry.COLUMN_NAME_DEVICE_ID + " NOT IN ";
        String selectQuery = "SELECT "+ SyncDeviceEntry.COLUMN_NAME_DEVICE_ID +" FROM " + SyncDeviceEntry.TABLE_NAME + this.arrayToSQLString(devices,prefix);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String deviceID = cursor.getString(0);
                ids.add(deviceID);
            } while (cursor.moveToNext());
        }
        cursor.close();

        db.close();
        return ids;
    }


    /**
     * Returns all new {@link de.tudarmstadt.informatik.hostage.logging.AttackRecord}s for the given devices (including all missing devices).
     * @param deviceMap map of device id and max sync_id of it
     * @param includeMissingDevices boolean
     * @return list of {@link de.tudarmstadt.informatik.hostage.logging.AttackRecord}s
     */
    public synchronized  ArrayList<SyncRecord> getUnsyncedAttacksFor(HashMap<String,Long> deviceMap, boolean includeMissingDevices){

        ArrayList<SyncDevice> updatedDevices = this.getUpdatedDevicesFor(deviceMap, includeMissingDevices);

        ArrayList<SyncRecord> recordList = new ArrayList<SyncRecord>();

        SQLiteDatabase db = this.getReadableDatabase();

        SyncDevice currentDevice = currentDevice();
        String own_device_id = currentDevice.getDeviceID();

        for (SyncDevice sDevice : updatedDevices){
            String deviceID = sDevice.getDeviceID();
            Long maxID = deviceMap.get(deviceID);
            long checkId = -1;
            if (maxID != null) checkId = maxID.longValue();
            String selectQuery = "SELECT * FROM " + AttackEntry.TABLE_NAME + " A "
                    + " WHERE "
                    +" ( "
                    + " A." + AttackEntry.COLUMN_NAME_DEVICE + " = " + "'" + deviceID + "'"
                    + " AND " + " A." + AttackEntry.COLUMN_NAME_SYNC_ID + " > " + checkId
                    + " ) "
                    //+ " GROUP BY " + AttackEntry.TABLE_NAME + "." + AttackEntry.COLUMN_NAME_DEVICE
                    + " ORDER BY " + " A" + "." + AttackEntry.COLUMN_NAME_SYNC_ID + " DESC ";
            Cursor cursor = db.rawQuery(selectQuery, null);

            // looping through all rows and adding to list
            if (cursor != null){
                if (cursor.moveToFirst()) {
                    do {
                        SyncRecord record = createSyncRecord(cursor , db);
                        recordList.add(record);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        }

        // return record list
        db.close();
        return recordList;
    }

    /**
     * Attacks per BSSID
     * @param filter (LogFilter) query filter
     * @return ArrayList<PlotComparisonItem>
     */
    public synchronized ArrayList<PlotComparisonItem> attacksPerBSSID(LogFilter filter) {

        String filterQuery = this.selectionQueryFromFilter(filter, AttackEntry.COLUMN_NAME_ATTACK_ID);

        filterQuery = filterQuery.split("GROUP BY")[0];

        String attackPerBSSID_Query = "SELECT " + AttackEntry.COLUMN_NAME_BSSID + " , " + "COUNT( " + "*" + "  ) " + " "
                + " FROM " + AttackEntry.TABLE_NAME + " a "
                + " WHERE " + " a." + AttackEntry.COLUMN_NAME_ATTACK_ID + " IN " + " ( " + filterQuery + " ) "
                + " GROUP BY " + AttackEntry.COLUMN_NAME_BSSID;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(attackPerBSSID_Query, null);
        ArrayList<PlotComparisonItem> plots = new ArrayList<PlotComparisonItem>();
        int counter = 0;

        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(0); // COLUMN_NAME_BSSID
                double value = cursor.getDouble(1); // COUNT

                if (value == 0.) continue;
                PlotComparisonItem plotItem = new PlotComparisonItem(title, this.getColor(counter), 0. , value);
                plots.add(plotItem);

                counter++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return plots;

    }

    /**
     * Inserts the given devices in the database with save.
     * @param devices list of {@link de.tudarmstadt.informatik.hostage.logging.SyncDevice}s
     */
    public synchronized void insertSyncDevices(List<SyncDevice> devices){
        SQLiteDatabase db = this.getWritableDatabase();

        db.beginTransaction();

        try {
            for (SyncDevice device : devices){
                insertSyncDevice(device, db);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        db.close(); // Closing database connection
    }

    /**
     * Inserts the given dives in the given SQLite Database without save.
     * @param device {@link de.tudarmstadt.informatik.hostage.logging.SyncDevice}
     * @param db  {@link android.database.sqlite.SQLiteDatabase}
     */
    private void insertSyncDevice(SyncDevice device, SQLiteDatabase db){
        ContentValues recordValues = new ContentValues();
        recordValues.put(SyncDeviceEntry.COLUMN_NAME_DEVICE_ID, device.getDeviceID());
        recordValues.put(SyncDeviceEntry.COLUMN_NAME_DEVICE_TIMESTAMP, device.getLast_sync_timestamp());
        recordValues.put(SyncDeviceEntry.COLUMN_NAME_HIGHEST_ATTACK_ID, device.getHighest_attack_id());

        // Inserting Rows
        db.insertWithOnConflict(SyncDeviceEntry.TABLE_NAME, null, recordValues, SQLiteDatabase.CONFLICT_REPLACE);
    }


    /** Returns the color for the given index
     * @return int color*/
    public Integer getColor(int index) {
        return ColorSequenceGenerator.getColorForIndex(index);
    }

}
