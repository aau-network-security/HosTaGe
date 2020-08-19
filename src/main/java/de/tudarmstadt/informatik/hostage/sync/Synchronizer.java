package de.tudarmstadt.informatik.hostage.sync;

/**
 * Created by Julien on 08.12.2014.
 */

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import de.tudarmstadt.informatik.hostage.HostageApplication;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.NetworkRecord;
import de.tudarmstadt.informatik.hostage.logging.SyncData;
import de.tudarmstadt.informatik.hostage.logging.SyncDevice;
import de.tudarmstadt.informatik.hostage.logging.SyncInfo;
import de.tudarmstadt.informatik.hostage.logging.SyncRecord;
import de.tudarmstadt.informatik.hostage.persistence.DAO.DAOHelper;


public class Synchronizer {

    //private HostageDBOpenHelper dbh;
    private DaoSession dbSession;
    private DAOHelper daoHelper;

    public Synchronizer(DaoSession daoSession, Context context){
        super();
        this.dbSession = daoSession;
        this.daoHelper= new DAOHelper(daoSession,context);
    }


    /**
     * Returns own state of all registered devices.
     * @return  ArrayList<SyncDevice>
     */
    public SyncInfo getSyncInfo(){
        return this.daoHelper.getSyncDeviceDAO().getOwnState();
    }


    /**
     * Updates from the given sync data.
     * @param syncData {@link SyncData}
     */
    public void updateFromSyncData(SyncData syncData){
        this.updateNewNetworks(syncData.networkRecords);
        this.updateNewAttacks(syncData.syncRecords);
    }

    /**
     * Updates the devices list by the given {@link SyncInfo}.
     * @param sInfo  {@link SyncInfo}
     * @return  {@link SyncData}
     */
    public SyncData getSyncData(SyncInfo sInfo){
        SyncData sData = new SyncData();
        ArrayList<String> bssids = sInfo.bssids;
        HashMap<String, Long> deviceMap = sInfo.deviceMap;
        ArrayList<String> deviceIds = null;

        if (deviceMap != null && deviceMap.keySet() != null){
            deviceIds = this.stringSetToArrayList(deviceMap.keySet());

            this.updateNewDevices(deviceIds);
        }

        ArrayList<NetworkRecord> nets= new ArrayList<NetworkRecord>();
        if (bssids != null){
            nets = this.getMissingNetworkInformation(bssids);
        }

        ArrayList<SyncRecord> sRec = this.getUnsyncedRecords(sInfo);

        sData.syncRecords = sRec;
        sData.networkRecords = nets;

        return sData;
    }

    /**
     * Converts a string set in an array list
     * @param s the set to convert
     * @return array list
     */
    private ArrayList<String> stringSetToArrayList(Set<String> s){
        ArrayList<String> list = new ArrayList<String>();

        for (String string : s){
            list.add(string);
        }
        return list;
    }

    /***
     *
     *  PULL METHODS
     *
     *
     */

    /**
     * Inserts a list of networks
     * @param others all missing networks
     */
    private void updateNewNetworks(ArrayList<NetworkRecord> others){
        if (others != null && others.size() > 0){
            Log.i("DEBUG_Sync", "Updating Network Data Objects: " + others.size());
            this.daoHelper.getNetworkRecordDAO().updateNetworkInformation(others);
        }
    }

    /**
     * Updates new inserted devices.
     * @param otherDeviceIds ArrayList<String> other device ids
     */
    public void updateNewDevices(ArrayList<String> otherDeviceIds){

        if (otherDeviceIds != null){
            ArrayList<SyncDevice> otherDevices = new ArrayList<SyncDevice>();
            ArrayList<String> ownDevicesds = this.daoHelper.getSyncDeviceDAO().getAllDevicesIds();

            if (otherDeviceIds.size() > 0)
                Log.i("DEBUG_Sync", "Updating Devices: " + otherDevices.size());

            ArrayList<String> n = this.diffArray(otherDeviceIds, ownDevicesds);
            for (String deviceId : n){
                SyncDevice device = new SyncDevice();
                device.setDeviceID(deviceId);
                device.setHighest_attack_id(-1);
                device.setLast_sync_timestamp(0);
                otherDevices.add(device);
            }

            if (otherDevices.size() > 0)
                this.daoHelper.getSyncDeviceDAO().updateSyncDevices(otherDevices);
        }
    }

    /**
     * Diffs to array by their content.
     * @param origin the original array
     * @param toRemove all strings to remove
     * @return diffed array list
     */
    private ArrayList<String> diffArray(ArrayList<String> origin, ArrayList<String> toRemove){
        ArrayList<String> n = new ArrayList<String>();

        for (String s : origin){
            if (!toRemove.contains(s)){
                n.add(s);
            }
        }
        return n;
    }

    /**
     * Get all missing sync records from the other device.
     * @param updates list of new attack information
     */
    private void updateNewAttacks(ArrayList<SyncRecord> updates){
        if (updates != null && updates.size() > 0) {
            Log.i("DEBUG_Sync", "Updating Attack Objects: " + updates.size());
            this.daoHelper.getSyncDeviceDAO().insertSyncRecords(updates);
        }
    }



    /**
    *
    *  PUSH METHODS
    *
    *
    * */

    /**
     * Returns list of unsynced records.
     * @param si other states {@link SyncInfo}
     * @return unsynced sync records
     */
    private ArrayList<SyncRecord> getUnsyncedRecords(SyncInfo si){

        if (si.deviceMap != null){
            ArrayList<SyncRecord> records = daoHelper.getSyncDeviceDAO().getUnsyncedAttacksFor(si.deviceMap, true);
            Log.i("DEBUG_Sync", "Sending Attack Objects: " + records.size());
            return records;
        }
        return new ArrayList<SyncRecord>();
    }

    /**
     * Returns list of missing network records.
     * @param otherBSSIDs list of other bssids
     * @return array list of network records to push.
     */
private ArrayList<NetworkRecord> getMissingNetworkInformation(ArrayList<String> otherBSSIDs){

        if (otherBSSIDs != null){
            ArrayList<NetworkRecord> records = this.daoHelper.getNetworkRecordDAO().getMissingNetworkRecords(otherBSSIDs);
            Log.i("DEBUG_Sync", "Sending Network Objects: " + records.size());
            return records;

        }
        return new ArrayList<NetworkRecord>();
    }

}
