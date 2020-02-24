package de.tudarmstadt.informatik.hostage.logging;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.UUID;

import de.tudarmstadt.informatik.hostage.persistence.HostageDBContract;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBOpenHelper;

import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

/**
 * Created by Julien on 04.12.2014.
 */
public class SyncDevice {

    private long highest_attack_id;

    private String deviceID;
    private long last_sync_timestamp;


    public long getHighest_attack_id(){return this.highest_attack_id;}
    public void setHighest_attack_id(long i){this.highest_attack_id = i;}

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

    public void setLast_sync_timestamp(long t){this.last_sync_timestamp = t;}
    public long getLast_sync_timestamp(){return this.last_sync_timestamp;}


    /**
     * Returns a SyncDevice Object representing the current device.
     * @return {@link de.tudarmstadt.informatik.hostage.logging.SyncDevice}
     */
    public static SyncDevice currentDevice()
    {
        return HostageDBOpenHelper.currentDevice();
    }
}
