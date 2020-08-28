package dk.aau.netsec.hostage.logging;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;


import org.greenrobot.greendao.annotation.Generated;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.ui.activity.MainActivity;

/**
 * Created by Julien on 04.12.2014.
 */
@Entity
public class SyncDevice {

    private long highest_attack_id;
    @Id
    private String deviceID;
    private long last_sync_timestamp;


    @Generated(hash = 1030675105)
    public SyncDevice(long highest_attack_id, String deviceID,
            long last_sync_timestamp) {
        this.highest_attack_id = highest_attack_id;
        this.deviceID = deviceID;
        this.last_sync_timestamp = last_sync_timestamp;
    }
    @Generated(hash = 540305911)
    public SyncDevice() {
    }


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
     * @return {@link SyncDevice}
     */
    public static SyncDevice currentDevice() {
        SyncDevice device = null;
        if(MainActivity.getContext()!=null) {

            DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
            DAOHelper daoHelper = new DAOHelper(dbSession, (MainActivity.getContext()));
            return daoHelper.getAttackRecordDAO().currentDevice();

        }
        return device;
    }
}
