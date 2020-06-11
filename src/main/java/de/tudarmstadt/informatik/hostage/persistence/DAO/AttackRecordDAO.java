package de.tudarmstadt.informatik.hostage.persistence.DAO;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;


import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.AttackRecordDao;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.logging.NetworkRecord;
import de.tudarmstadt.informatik.hostage.logging.Record;
import de.tudarmstadt.informatik.hostage.logging.RecordAll;
import de.tudarmstadt.informatik.hostage.logging.SyncDevice;
import de.tudarmstadt.informatik.hostage.logging.SyncRecord;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;



public class AttackRecordDAO extends  DAO {
    private DaoSession daoSession;
    public static SyncDevice thisDevice = null;


    public AttackRecordDAO(DaoSession daoSession){
        this.daoSession= daoSession;

    }

    /**
     * Adds a given {@link AttackRecord} to the database.
     *
     * @param record
     *            The added {@link AttackRecord} .
     */

    public  void insert(AttackRecord record){
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        insertElement(recordDao,record);
    }

    /**
     * Adds a given {@link AttackRecord}s to the database.
     *
     * @param records {@link List}<AttackRecord>
     *            The added {@link AttackRecord}s .
     */
    synchronized public void insertAttackRecords(List<AttackRecord> records) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        insertElements(recordDao,records);

    }

    public ArrayList<AttackRecord> getAttackRecords(){
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        ArrayList<AttackRecord> attackRecords = (ArrayList<AttackRecord>) selectElements(recordDao);

        return  attackRecords;

    }


    /**
     * Adds a given {@link AttackRecord} to the database.
     *
     * @param record
     *            The added {@link AttackRecord} .
     */
    synchronized public void addAttackRecord(AttackRecord record) {
        this.insert(record);
        ArrayList<SyncDevice> devices = new ArrayList<SyncDevice>();
        devices.add(SyncDevice.currentDevice());
        SyncDeviceDAO deviceDAO = new SyncDeviceDAO(this.daoSession);
        deviceDAO.updateSyncDevices(devices);
    }

    /**
     * Adds a given {@link SyncRecord}s to the database.
     *
     * @param records {@link List}<AttackRecord>
     *            The added {@link SyncRecord}s .
     */
    synchronized public void insertSyncRecords(List<SyncRecord> records) {
        for (SyncRecord record : records) {
            AttackRecord attackRecord = record.getAttackRecord();
            this.insert(attackRecord);

            if (record.getMessageRecords() == null) {
                MessageRecord msg = new MessageRecord(true);
                msg.setAttack_id(attackRecord.getAttack_id());
                msg.setType(MessageRecord.TYPE.RECEIVE);
                msg.setPacket("");
                msg.setTimestamp(System.currentTimeMillis());
                MessageRecordDAO messageRecordDAO = new MessageRecordDAO(this.daoSession);
                messageRecordDAO.insert(msg);
            }
        }

        this.updateSyncDevicesMaxID();

    }

    public synchronized ArrayList<AttackRecord> getAttacksPerProtocol(String protocol) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder().where(AttackRecordDao.Properties.Protocol.eq(protocol));
        ArrayList<AttackRecord> records = (ArrayList<AttackRecord>) qb.list();

        return records;

    }


    /**
     * Determines the number of different attacks for a specific protocol in
     * the database.
     *
     * @param protocol
     *            The String representation of the
     *            {@link Protocol
     *            Protocol}
     * @return The number of different attacks in the database.
     */
    public synchronized int getAttackPerProtocolCount(String protocol) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder().where(AttackRecordDao.Properties.Protocol.eq(protocol));
        int result = (int) qb.buildCount().count();

        return result;

    }

    /**
     * Determines the number of  attacks for a specific protocol in
     * the database since the given attack_id.
     *
     * @param protocol
     *            The String representation of the
     *            {@link Protocol
     *            Protocol}
     * @param attack_id  The attack id to match the query against.
     * @return The number of different attacks in the database since the given attack_id.
     */
    public synchronized int getAttackPerProtocolCount(String protocol, int attack_id) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.and(AttackRecordDao.Properties.Protocol.eq(protocol),
                AttackRecordDao.Properties.Attack_id.eq(attack_id));

        return (int) qb.buildCount().count();

    }

    /**
     * Determines the number of recorded attacks for a specific protocol and accesss point since the given attack_id.
     *
     * @param protocol
     *            The String representation of the
     *            {@link Protocol
     *            Protocol}
     * @param attack_id  The attack id to match the query against.
     * @param bssid The BSSID of the access point.
     * @return The number of different attacks in the database since the given attack_id.
     */
    public synchronized int getAttackPerProtocolCount(String protocol, int attack_id, String bssid) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.and(AttackRecordDao.Properties.Protocol.eq(protocol),
                qb.and( AttackRecordDao.Properties.Attack_id.eq(attack_id), AttackRecordDao.Properties.Bssid.eq(bssid)));

        int result = (int) qb.buildCount().count();

        return result;

    }


    /**
     * Updates the own devices and connects attack records without a device to the own device
     */
    synchronized public void updateUntrackedAttacks(){
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        SyncDevice ownDevice = this.currentDevice();
        long highestID = ownDevice.getHighest_attack_id();

        List<AttackRecord> records = recordDao.queryBuilder()
                .where(AttackRecordDao.Properties.Device.isNull()).list();

        addUpdatedRecords(records,ownDevice,highestID);

        updateDevicesNoID( ownDevice, highestID);

    }

    synchronized public void updateSyncAttackCounter(AttackRecord record){
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        insert(record);
    }

        /**
         * Updates the sync devices max sync id.
         */
        //TODO:Ask about the logic
    synchronized public void updateSyncDevicesMaxID(){
       AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

      //  Query<AttackRecord> query = recordDao.queryBuilder().where(AttackRecordDao.Properties.Sync_id.isNotNull())
             //   .orderDesc(AttackRecordDao.Properties.Sync_id).limit(1).build();

        setMaxIDinDevices();

    }

    /**
     * Determines if an attack has been recorded on a specific protocol in a
     * network with a given BSSID.
     *
     * @param protocol
     *            The
     *            {@link Protocol
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
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.and(AttackRecordDao.Properties.Protocol.eq(protocol),AttackRecordDao.Properties.Bssid.eq(BSSID));
        List<AttackRecord> records = qb.list();

        if(!records.isEmpty())
            return  true;

        return  false;
    }

    public synchronized int getNumAttacksSeenByBSSID(String BSSID) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder().where(AttackRecordDao.Properties.Bssid.eq(BSSID));
        int result = (int) qb.buildCount().count();

        return  result;
    }


        private void setMaxIDinDevices( ){
        SyncDeviceDAO deviceDAO = new SyncDeviceDAO(this.daoSession);
        ArrayList<SyncDevice> allDevices = deviceDAO.getSyncDevices();
        long highestID = 0;
        for (SyncDevice device : allDevices){
            Long sync_id = Long.valueOf(device.getDeviceID()).longValue();

            highestID = device.getHighest_attack_id();
            if (sync_id != null && highestID < sync_id) highestID = sync_id;

        }
        for (SyncDevice device : allDevices){
            device.setHighest_attack_id(highestID);

            }

        deviceDAO.updateSyncDevices(allDevices);
    }

    private void addUpdatedRecords(List<AttackRecord> attackRecords, SyncDevice ownDevice,long highestID ){

        if(!attackRecords.isEmpty()){
            for (AttackRecord element : attackRecords) {
                AttackRecord record = this.createAttackRecord(element);
                record.setDevice(ownDevice.getDeviceID());
                highestID = (highestID > record.getAttack_id())? highestID : record.getAttack_id();
                this.insert(record);

            }
        }

        ownDevice.setHighest_attack_id(highestID);


    }

    private void updateDevicesNoID(SyncDevice ownDevice,long highestID){
        if (highestID != ownDevice.getHighest_attack_id()){
            // THERE WERE ATTACKS WITHOUT A DEVICE ID
            ArrayList<SyncDevice> devices = new ArrayList<SyncDevice>();
            devices.add(ownDevice);
            SyncDeviceDAO deviceDAO = new SyncDeviceDAO(this.daoSession);
            deviceDAO.updateSyncDevices(devices);
        }
    }

    /**
     * Creates a {@link AttackRecord}
     *
     * @param existingRecord
     * @return Returns the created {@link AttackRecord} .
     */
    private synchronized AttackRecord createAttackRecord(AttackRecord existingRecord) {
        AttackRecord record = new AttackRecord();
        record.setAttack_id(existingRecord.getAttack_id());
        record.setProtocol(existingRecord.getProtocol());
        record.setExternalIP(existingRecord.getExternalIP());
        record.setLocalIP(existingRecord.getLocalIP());
        record.setLocalPort(existingRecord.getLocalPort());
        record.setRemoteIP(existingRecord.getRemoteIP());
        record.setRemotePort(existingRecord.getRemotePort());
        record.setWasInternalAttack(existingRecord.getWasInternalAttack());
        record.setBssid(existingRecord.getBssid());
        record.setSync_id(existingRecord.getSync_id());
        record.setDevice(existingRecord.getDevice());
        return record;
    }


    public static SyncDevice currentDevice(){
        SharedPreferences pref = null;
        if (thisDevice != null){
            if(MainActivity.getContext() != null)
                pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext());
                if(pref == null)
                    thisDevice.setHighest_attack_id(1);
                else {
                    int attack_id = pref.getInt("ATTACK_ID_COUNTER", 0);
                    thisDevice.setHighest_attack_id(attack_id);
                }
        }
        return thisDevice;
    }

    /**
     * Deletes all records from the persistence.
     */
    public synchronized void clearData() {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        deleteAllFromTable(recordDao);

    }

    /**
     * Deletes all {@link AttackRecord} with a specific Attack ID.
     *
     * @param attackID
     *            The Attack ID to match against.
     */
    public synchronized void deleteByAttackID(long attackID) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        deleteElementsByCondition(recordDao, AttackRecordDao.Properties.Attack_id.eq(attackID));

    }

    /**
     * Deletes all attacks for the given filter object.
     * @param filter
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public synchronized void deleteAttacksByFilter(LogFilter filter){
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        ArrayList<AttackRecord>  records = selectionQueryFromFilter(filter);

        recordDao.deleteInTx(records);


    }

    /**
     * Gets all received {@link Record Records} for the specified information in
     * the LogFilter ordered by date.
     *
     * @return A ArrayList with all received {@link Record Records} for the
     *         LogFilter.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public synchronized ArrayList<RecordAll> getRecordsForFilter(LogFilter filter) {
        NetworkRecordDAO networkRecordDAO = new NetworkRecordDAO(this.daoSession);
        MessageRecordDAO messageRecordDAO = new MessageRecordDAO(this.daoSession);

        ArrayList<RecordAll> allRecords = new ArrayList<>();
        ArrayList<AttackRecord> attackRecords = this.selectionQueryFromFilter(filter);
        ArrayList<NetworkRecord> bssIdRecords = networkRecordDAO.selectionBSSIDFromFilter(filter);
        ArrayList<NetworkRecord> essIdRecords = networkRecordDAO.selectionESSIDFromFilter(filter);
        ArrayList<MessageRecord> records = messageRecordDAO.selectionQueryFromFilter(filter);

        allRecords.addAll(attackRecords);
        allRecords.addAll(bssIdRecords);
        allRecords.addAll(essIdRecords);
        allRecords.addAll(records);

        return  allRecords;

    }

    /**
     * Returns the query for the given filter.
     * @param filter (LogFilter)
     * @return QueryBuilder<AttackRecord> query
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public ArrayList<AttackRecord> selectionQueryFromFilter(LogFilter filter) {
        ArrayList<String> filterProtocols = filter.getProtocols();
        ArrayList<AttackRecord> attackRecords = this.getAttackRecords();
        ArrayList<AttackRecord> list = new ArrayList<>();


        if(filterProtocols != null && filterProtocols.size() > 0) {
            for (final String current : filterProtocols) {

                list.add(attackRecords.stream().filter(o -> o.getProtocol().equals(current)).findAny().orElse(null));

            }
        }
        list.removeAll(Collections.singleton(null));

        return list;

    }

    /**
     * Returns the Conversation of a specific attack id
     *
     * @param attack_id Tha attack id to match the query against.
     *
     * @return A arraylist with all {@link Record Records}s for an attack id.
     */
    //TODO check if this method is ok
    public synchronized ArrayList<RecordAll> getConversationForAttackID(long attack_id) {
        ArrayList<RecordAll> recordList = new ArrayList<RecordAll>();
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.where(AttackRecordDao.Properties.Attack_id.eq(attack_id));

        ArrayList<AttackRecord> attackRecords =  ( ArrayList<AttackRecord>) qb.list();

        recordList.addAll(attackRecords);

        return recordList;

    }
    /**
     * Gets all {@link RecordAll Records} saved in the database.
     *
     * @return A ArrayList of all the {@link RecordAll Records} in the Database.
     */
    public synchronized ArrayList<RecordAll> getAllRecords(){
        NetworkRecordDAO networkRecordDAO = new NetworkRecordDAO(this.daoSession);
        MessageRecordDAO messageRecordDAO = new MessageRecordDAO(this.daoSession);

        ArrayList<RecordAll> allRecords = new ArrayList<>();
        ArrayList<AttackRecord> attackRecords = this.getAttackRecords();
        ArrayList<NetworkRecord> networkRecords = networkRecordDAO.getNetworkInformation();
        ArrayList<MessageRecord> records = messageRecordDAO.getAllMessageRecords();

        allRecords.addAll(attackRecords);
        allRecords.addAll(networkRecords);
        allRecords.addAll(records);

        return allRecords;

    }



}
