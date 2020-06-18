package de.tudarmstadt.informatik.hostage.persistence.DAO;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;


import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.AttackRecordDao;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.logging.SyncDevice;
import de.tudarmstadt.informatik.hostage.logging.SyncDeviceDao;
import de.tudarmstadt.informatik.hostage.logging.SyncInfo;
import de.tudarmstadt.informatik.hostage.logging.SyncRecord;

import static de.tudarmstadt.informatik.hostage.persistence.DAO.AttackRecordDAO.currentDevice;


public class SyncDeviceDAO extends DAO {
    public static SyncDevice thisDevice = null;
    private Context context;
    private DaoSession daoSession;

    public SyncDeviceDAO(DaoSession daoSession){
        this.daoSession= daoSession;

    }

    public SyncDeviceDAO(DaoSession daoSession,Context context){
        this.daoSession= daoSession;
        this.context = context;
        //this.generateCurrentDevice();
    }


    /**
     * Adds a given {@link SyncDevice} to the database.
     *
     * @param record
     *            The added {@link SyncDevice} .
     */
    public void insert( SyncDevice record){
        SyncDeviceDao recordDao = this.daoSession.getSyncDeviceDao();
        record.setDeviceID(UUID.randomUUID().toString());
        insertElement(recordDao,record);
    }

    /**
     * Adds a given {@link SyncDevice}s to the database.
     *
     * @param records {@link List}<MessageRecord>
     *            The added {@link SyncDevice}s .
     */
    public synchronized void insertSyncDevices(List<SyncDevice> records){
        SyncDeviceDao recordDao = this.daoSession.getSyncDeviceDao();
        insertElements(recordDao,records);

    }

    /**
     * Returns an ArrayList of all devices that were previously synchronized with.
     * @return ArrayList containing device id's and the last synchronization timestamp.
     */
    public synchronized ArrayList<SyncDevice> getSyncDevices(){
        SyncDeviceDao recordDao = this.daoSession.getSyncDeviceDao();
        ArrayList<SyncDevice> devices = (ArrayList<SyncDevice>) selectElements(recordDao);

        return devices;

    }


    /**
     * Updates sync devices
     * @param devices array list of sync devices
     */
    public synchronized void updateSyncDevices(ArrayList<SyncDevice> devices){
        SyncDeviceDao recordDao = this.daoSession.getSyncDeviceDao();
        updateElements(recordDao,devices);
    }

    /**
     * Updates the Timestamps of synchronization devices from a HashMap.
     * @param devices HashMap of device ids and their synchronization timestamps.
     */
public synchronized void updateSyncDevices(HashMap<String, Long> devices){
        SyncDeviceDao recordDao = this.daoSession.getSyncDeviceDao();
        ArrayList<SyncDevice> allDevices = this.getSyncDevices();

        for(String key : devices.keySet()) {
           SyncDevice device = allDevices.stream().filter(o -> o.getDeviceID().equals(key)).findFirst().orElse(null);
           if(device != null) {
               device.setLast_sync_timestamp(devices.get(key));
               recordDao.update(device);
           }
        }


    }

    /**
     * Returns the own state containing all registered devices ids and their max sync_id
     * @return {@link SyncInfo}
     */
    public synchronized SyncInfo getOwnState(){
        AttackRecordDAO attackRecordDAO = new AttackRecordDAO(this.daoSession);
        NetworkRecordDAO networkRecordDAO = new NetworkRecordDAO(this.daoSession);
        attackRecordDAO.updateUntrackedAttacks();

        ArrayList<SyncDevice> devices = this.getSyncDevices();

        HashMap<String, Long> deviceMap = new HashMap<String, Long>();
        for (SyncDevice device : devices){
            deviceMap.put(device.getDeviceID(), device.getHighest_attack_id());
        }

        if(SyncDevice.currentDevice()!=null)
            deviceMap.put(SyncDevice.currentDevice().getDeviceID(), SyncDevice.currentDevice().getHighest_attack_id());
        SyncInfo syncInfo = new SyncInfo();
        syncInfo.deviceMap  = deviceMap;
        syncInfo.bssids = networkRecordDAO.getAllBSSIDS();
        return syncInfo;
    }

    /**
     * Returns all device ids.
     * @return list of all device ids.
     */
    public synchronized  ArrayList<String> getAllDevicesIds(){
        ArrayList<String> idsList = new ArrayList<String>();
        ArrayList<SyncDevice> syncDevices = this.getSyncDevices();

        for(SyncDevice record:syncDevices){
            String s = record.getDeviceID();
            idsList.add(s);
        }

        return idsList;

    }

    /**
     * Adds a given {@link SyncRecord}s to the database.
     *
     * @param records {@link List}<AttackRecord>
     *            The added {@link SyncRecord}s .
     */
    synchronized public void insertSyncRecords(List<SyncRecord> records) {
        AttackRecordDAO attackRecordDAO = new AttackRecordDAO(this.daoSession);
        MessageRecordDAO messageRecordDAO = new MessageRecordDAO(this.daoSession);

        for (SyncRecord record : records){
            AttackRecord attackRecord = record.getAttackRecord();
            attackRecordDAO.insert(attackRecord);

            if(record.getMessageRecords() == null){
                MessageRecord msg = new MessageRecord(true);
                msg.setAttack_id(attackRecord.getAttack_id());
                msg.setType(MessageRecord.TYPE.RECEIVE);
                msg.setPacket("");
                msg.setTimestamp(System.currentTimeMillis());

                messageRecordDAO.insert(msg);
            } else {
                messageRecordDAO.insertMessageRecords(record.getMessageRecords());
            }
        }
        attackRecordDAO.updateSyncDevicesMaxID();


    }


    /**
     * Returns a HashMap of all devices that were previously synchronized with.
     * @return HashMap containing device id's and the last synchronization timestamp.
     */
    public synchronized HashMap<String, Long> getSyncDeviceHashMap(){
        HashMap<String, Long> devices = new HashMap<String, Long>();

        ArrayList<SyncDevice> allDevices = this.getSyncDevices();

        for(SyncDevice device:allDevices){
            devices.put(device.getDeviceID(), device.getLast_sync_timestamp());
        }

        return devices;

    }


    /**
     * Returns a SyncDevice Object representing the current device.
     * @return {@link SyncDevice}
     */
    public SyncDevice generateCurrentDevice() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.context);
        long attack_id = pref.getInt("ATTACK_ID_COUNTER", 0);

        if (thisDevice == null) {
            String deviceUUID = pref.getString("CURRENT_DEVICE_IDENTIFIER", UUID.randomUUID().toString());
            SyncDeviceDao deviceDao = this.daoSession.getSyncDeviceDao();

            SyncDevice device = selectElementByCondition(deviceDao, SyncDeviceDao.Properties.DeviceID.eq(deviceUUID));

            if(device != null)
                deviceExist(device,attack_id);
            else
                createNewDevice(deviceUUID , attack_id, pref);

        }
        thisDevice.setHighest_attack_id(attack_id - 1);
        return thisDevice;
    }

    private void deviceExist(SyncDevice device, long attack_id){
        SyncDevice record = new SyncDevice();
        record.setDeviceID(device.getDeviceID());
        record.setLast_sync_timestamp(device.getLast_sync_timestamp());
        record.setHighest_attack_id(device.getHighest_attack_id());
        thisDevice = record;
        thisDevice.setHighest_attack_id(attack_id-1);

    }

    private void createNewDevice(String deviceUUID ,long attack_id, SharedPreferences pref){
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

    /**
     * Returns all missing / newly inserted and updated {@link SyncDevice}s.
     * @param oldDeviceMap map with device id and max sync_id for the device
     * @param includeMissing boolean
     * @return array of {@link SyncDevice}s
     */
    public synchronized  ArrayList<SyncDevice> getUpdatedDevicesFor(HashMap<String, Long> oldDeviceMap, boolean includeMissing){
        ArrayList<SyncDevice> devices = this.getSyncDevices();
        ArrayList<SyncDevice> recordList = new ArrayList<SyncDevice>();

        boolean actualiseOwnDevice = false;
        if (oldDeviceMap.containsKey(currentDevice().getDeviceID()) || includeMissing){
            actualiseOwnDevice = true;
        }
        for(SyncDevice record:devices){
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

        }
        return recordList;
    }

    /**
     * Returns all new {@link AttackRecord}s for the given devices (including all missing devices).
     * @param deviceMap map of device id and max sync_id of it
     * @param includeMissingDevices boolean
     * @return list of {@link AttackRecord}s
     */
    public synchronized  ArrayList<SyncRecord> getUnsyncedAttacksFor(HashMap<String,Long> deviceMap, boolean includeMissingDevices) {
        ArrayList<SyncDevice> updatedDevices = this.getUpdatedDevicesFor(deviceMap, includeMissingDevices);
        ArrayList<SyncRecord> recordList = new ArrayList<SyncRecord>();
        AttackRecordDao attackRecordDao = this.daoSession.getAttackRecordDao();
        SyncRecord syncRecord= new SyncRecord();


        SyncDevice currentDevice = currentDevice();
        for (SyncDevice sDevice : updatedDevices) {
            String deviceID = sDevice.getDeviceID();
            Long maxID = deviceMap.get(deviceID);
            long checkId = -1;
            if (maxID != null) checkId = maxID.longValue();

            QueryBuilder<AttackRecord> qb = attackRecordDao.queryBuilder();
            qb.where(AttackRecordDao.Properties.Device.eq(deviceID),AttackRecordDao.Properties.Sync_id.gt(maxID));
           // qb.join(SyncDevice.class
             //       ,AttackRecordDao.Properties.Device).and(AttackRecordDao.Properties.Device.eq(deviceID)
            //,AttackRecordDao.Properties.Sync_id.gt(maxID)); //with join
            ArrayList<AttackRecord> devices = (ArrayList<AttackRecord>) qb.list();

            if(devices!= null)
                syncRecord = new SyncRecord(devices.get(0));
                recordList.add(syncRecord);
        }
        return recordList;

    }

    }