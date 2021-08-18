package dk.aau.netsec.hostage.persistence.DAO;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import dk.aau.netsec.hostage.logging.AttackRecord;
import dk.aau.netsec.hostage.logging.AttackRecordDao;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.logging.MessageRecord;
import dk.aau.netsec.hostage.logging.MessageRecordDao;
import dk.aau.netsec.hostage.logging.NetworkRecord;
import dk.aau.netsec.hostage.logging.RecordAll;
import dk.aau.netsec.hostage.logging.SyncDevice;
import dk.aau.netsec.hostage.logging.SyncRecord;
import dk.aau.netsec.hostage.protocol.Protocol;
import dk.aau.netsec.hostage.ui.activity.MainActivity;
import dk.aau.netsec.hostage.ui.model.LogFilter;

import static dk.aau.netsec.hostage.persistence.DAO.SyncDeviceDAO.thisDevice;

public class AttackRecordDAO extends DAO {
    private final DaoSession daoSession;
    private Context context;
    private final int limit = 20; //limit for the details Conversation


    public AttackRecordDAO(DaoSession daoSession) {
        this.daoSession = daoSession;

    }

    public AttackRecordDAO(DaoSession daoSession, Context context) {
        this.daoSession = daoSession;
        this.context = context;

    }

    /**
     * Adds a given {@link AttackRecord} to the database.
     *
     * @param record The added {@link AttackRecord} .
     */

    public void insert(AttackRecord record) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        insertElement(recordDao, record);
    }

    /**
     * Adds a given {@link AttackRecord}s to the database.
     *
     * @param records {@link List}<AttackRecord>
     *                The added {@link AttackRecord}s .
     */
    synchronized public void insertAttackRecords(List<AttackRecord> records) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        insertElements(recordDao, records);

    }

    public ArrayList<AttackRecord> getAttackRecords() {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.orderDesc(AttackRecordDao.Properties.Attack_id).build();
        ArrayList<AttackRecord> attackRecords = (ArrayList<AttackRecord>) qb.list();
        ;

        return attackRecords;
    }

    public ArrayList<AttackRecord> getAttackRecordsLimit(int offset, int limit) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.orderDesc(AttackRecordDao.Properties.Attack_id).build();
        qb.offset(offset).limit(limit).build();

        return (ArrayList<AttackRecord>) qb.list();
    }

    public long getRecordsCount() {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        return countElements(recordDao);
    }

    /**
     * Adds a given {@link AttackRecord} to the database.
     *
     * @param record The added {@link AttackRecord} .
     */
    synchronized public void addAttackRecord(AttackRecord record) {
        this.insert(record);
        ArrayList<SyncDevice> devices = new ArrayList<>();
        devices.add(SyncDevice.currentDevice());
        SyncDeviceDAO deviceDAO = new SyncDeviceDAO(this.daoSession, context);
        if (thisDevice != null)
            deviceDAO.updateSyncDevices(devices);
    }

    /**
     * Adds a given {@link SyncRecord}s to the database.
     *
     * @param records {@link List}<AttackRecord>
     *                The added {@link SyncRecord}s .
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

        return (ArrayList<AttackRecord>) qb.list();

    }

    /**
     * Determines the number of different attacks for a specific protocol in
     * the database.
     *
     * @param protocol The String representation of the
     *                 {@link Protocol
     *                 Protocol}
     * @return The number of different attacks in the database.
     */
    public synchronized int getAttackPerProtocolCount(String protocol) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder().where(AttackRecordDao.Properties.Protocol.eq(protocol));

        return (int) qb.buildCount().count();

    }

    /**
     * Determines the number of  attacks for a specific protocol in
     * the database since the given attack_id.
     *
     * @param protocol  The String representation of the
     *                  {@link Protocol
     *                  Protocol}
     * @param attack_id The attack id to match the query against.
     * @return The number of different attacks in the database since the given attack_id.
     */
    public synchronized int getAttackPerProtocolCount(String protocol, long attack_id) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.where(AttackRecordDao.Properties.Protocol.eq(protocol),
                AttackRecordDao.Properties.Attack_id.eq(attack_id));

        return (int) qb.buildCount().count();

    }

    /**
     * Determines the number of recorded attacks for a specific protocol and access point since the given attack_id.
     *
     * @param protocol  The String representation of the
     *                  {@link Protocol
     *                  Protocol}
     * @param attack_id The attack id to match the query against.
     * @param bssid     The BSSID of the access point.
     * @return The number of different attacks in the database since the given attack_id.
     */
    public synchronized int getAttackPerProtocolCount(String protocol, long attack_id, String bssid) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.where(AttackRecordDao.Properties.Protocol.eq(protocol),
                qb.and(AttackRecordDao.Properties.Attack_id.eq(attack_id), AttackRecordDao.Properties.Bssid.eq(bssid)));

        return (int) qb.buildCount().count();

    }


    /**
     * Updates the own devices and connects attack records without a device to the own device
     */
    synchronized public void updateUntrackedAttacks() {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        SyncDevice ownDevice = currentDevice();
        long highestID = ownDevice.getHighest_attack_id();

        List<AttackRecord> records = recordDao.queryBuilder()
                .where(AttackRecordDao.Properties.Device.isNull()).list();

        addUpdatedRecords(records, ownDevice, highestID);

        updateDevicesNoID(ownDevice, highestID);

    }

    synchronized public void updateSyncAttackCounter(AttackRecord record) {
        SyncDeviceDAO recordDao = new SyncDeviceDAO(this.daoSession, context);
        SyncDevice device = new SyncDevice();
        device.setLast_sync_timestamp(System.currentTimeMillis());
        device.setHighest_attack_id(record.getAttack_id());
        recordDao.insert(device);
    }

    /**
     * Updates the sync devices max sync id.
     */
    //TODO:Ask about the logic
    synchronized public void updateSyncDevicesMaxID() {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        //  Query<AttackRecord> query = recordDao.queryBuilder().where(AttackRecordDao.Properties.Sync_id.isNotNull())
        //   .orderDesc(AttackRecordDao.Properties.Sync_id).limit(1).build();

        setMaxIDinDevices();

    }

    /**
     * Determines if an attack has been recorded on a specific protocol in a
     * network with a given BSSID.
     *
     * @param protocol The
     *                 {@link Protocol
     *                 Protocol} to inspect.
     * @param BSSID    The BSSID of the network.
     * @return True if an attack on the given protocol has been recorded in a
     * network with the given BSSID, else false.
     */
    public synchronized boolean bssidSeen(String protocol, String BSSID) {
        if (BSSID == null || protocol == null) {
            return false;
        }
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.where(AttackRecordDao.Properties.Protocol.eq(protocol), AttackRecordDao.Properties.Bssid.eq(BSSID));
        List<AttackRecord> records = qb.list();

        return !records.isEmpty();
    }

    public synchronized int getNumAttacksSeenByBSSID(String BSSID) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        int result;
        if (BSSID != null) {
            QueryBuilder<AttackRecord> qb = recordDao.queryBuilder().where(AttackRecordDao.Properties.Bssid.eq(BSSID));

            result = qb.list().size();
            return result;
        }

        return 0;
    }

    synchronized public int getNumAttacksSeenByBSSID(String protocol, String BSSID) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.where(AttackRecordDao.Properties.Bssid.eq(BSSID), AttackRecordDao.Properties.Protocol.eq(protocol));

        return (int) qb.count();
    }

    private void setMaxIDinDevices() {
        SyncDeviceDAO deviceDAO = new SyncDeviceDAO(this.daoSession, context);
        ArrayList<SyncDevice> allDevices = deviceDAO.getSyncDevices();
        long highestID = 0;
        for (SyncDevice device : allDevices) {
            long sync_id = Long.parseLong(device.getDeviceID());

            highestID = device.getHighest_attack_id();
            if (sync_id != 0 && highestID < sync_id) highestID = sync_id;

        }
        for (SyncDevice device : allDevices) {
            device.setHighest_attack_id(highestID);

        }

        deviceDAO.updateSyncDevices(allDevices);
    }

    private void addUpdatedRecords(List<AttackRecord> attackRecords, SyncDevice ownDevice, long highestID) {
        if (!attackRecords.isEmpty()) {
            for (AttackRecord element : attackRecords) {
                AttackRecord record = this.createAttackRecord(element);
                record.setDevice(ownDevice.getDeviceID());
                highestID = Math.max(highestID, record.getAttack_id());
                this.insert(record);

            }
        }

        ownDevice.setHighest_attack_id(highestID);
    }

    private void updateDevicesNoID(SyncDevice ownDevice, long highestID) {
        if (highestID != ownDevice.getHighest_attack_id()) {
            // THERE WERE ATTACKS WITHOUT A DEVICE ID
            ArrayList<SyncDevice> devices = new ArrayList<>();
            devices.add(ownDevice);
            SyncDeviceDAO deviceDAO = new SyncDeviceDAO(this.daoSession, this.context);
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


    public SyncDevice currentDevice() {
        SharedPreferences pref = null;
        if (thisDevice != null) {
            if (MainActivity.getContext() != null && context != null)
                pref = PreferenceManager.getDefaultSharedPreferences(context);
            if (pref == null)
                thisDevice.setHighest_attack_id(1);
            else {
                long attack_id = pref.getLong("ATTACK_ID_COUNTER", 0);
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
     * @param attackID The Attack ID to match against.
     */
    public synchronized void deleteByAttackID(long attackID) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        deleteElementsByCondition(recordDao, AttackRecordDao.Properties.Attack_id.eq(attackID));
    }

    /**
     * Deletes all attacks for the given filter object.
     *
     * @param filter
     */
    public synchronized void deleteAttacksByFilter(LogFilter filter, int offset, int limit) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        ArrayList<AttackRecord> records = selectionQueryFromFilter(filter, offset, limit);
        recordDao.deleteInTx(records);
    }

    /**
     * Deletes all attacks for the given filter object.
     *
     * @param filter
     */
    public synchronized void deleteAttacksByFilter(LogFilter filter) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        ArrayList<AttackRecord> records = selectionQueryFromFilter(filter);
        recordDao.deleteInTx(records);
    }


    public synchronized ArrayList<RecordAll> getRecordsForFilter(LogFilter filter) {
        MessageRecordDAO messageRecordDAO = new MessageRecordDAO(this.daoSession);
        ArrayList<AttackRecord> attackRecords = this.selectionQueryFromFilter(filter);
        ArrayList<MessageRecord> records = messageRecordDAO.selectionQueryFromFilter(filter);
        ArrayList<MessageRecord> messageRecords;

        ArrayList<NetworkRecord> distinctNetworkRecords = distinctNetworkRecords(filter);
        messageRecords = updatedMessageRecordsFields(attackRecords, distinctNetworkRecords, records);

        return sortLists(filter, messageRecords);
    }

    public synchronized ArrayList<RecordAll> getRecordsForFilterMutliStage(LogFilter filter) {
        MessageRecordDAO messageRecordDAO = new MessageRecordDAO(this.daoSession);

        return new ArrayList<>(messageRecordDAO.selectionQueryFromFilterMultiStage(filter));
    }

    public synchronized ArrayList<RecordAll> getRecordsForFilter(LogFilter filter, int offset, int limit, int attackRecordOffset, int attackRecordLimit) {
        MessageRecordDAO messageRecordDAO = new MessageRecordDAO(this.daoSession);
        ArrayList<AttackRecord> attackRecords = this.selectionQueryFromFilter(filter, attackRecordOffset, attackRecordLimit);
        ArrayList<MessageRecord> records = messageRecordDAO.selectionQueryFromFilter(filter, 0, limit);
        ArrayList<MessageRecord> messageRecords;
        ArrayList<MessageRecord> filterProtocolRecords;

        ArrayList<NetworkRecord> distinctNetworkRecords = distinctNetworkRecords(filter);

        messageRecords = updatedMessageRecordsFields(attackRecords, distinctNetworkRecords, records);
        filterProtocolRecords = getProtocolsFilter(filter, messageRecords, offset);

        return sortLists(filter, filterProtocolRecords);
    }

    private ArrayList<MessageRecord> getProtocolsFilter(LogFilter filter, ArrayList<MessageRecord> messageRecords, int limit) {
        ArrayList<MessageRecord> records = new ArrayList<>();
        if (filter != null && !filter.protocols.isEmpty()) {
            ArrayList<String> filterProtocols = filter.protocols;
            for (final String current : filterProtocols) {
                records.addAll(messageRecords.stream().filter(o -> o.getProtocol().equals(current)).limit(limit).collect(Collectors.toList()));
            }
        } else {
            return messageRecords;
        }
        return records;
    }


    private ArrayList<MessageRecord> updatedMessageRecordsFields(ArrayList<AttackRecord> attackRecords, ArrayList<NetworkRecord> distinctNetworkRecords, ArrayList<MessageRecord> records) {
        ArrayList<AttackRecord> updatedAttackRecords;
        updatedAttackRecords = addNetworkFields(attackRecords, distinctNetworkRecords);

        return addMessageRecordFields(updatedAttackRecords, records);
    }

    private ArrayList<NetworkRecord> distinctNetworkRecords(LogFilter filter, int offset, int limit) {
        NetworkRecordDAO networkRecordDAO = new NetworkRecordDAO(this.daoSession);
        ArrayList<NetworkRecord> bssIdRecords = networkRecordDAO.selectionBSSIDFromFilter(filter, offset, limit);
        ArrayList<NetworkRecord> essIdRecords = networkRecordDAO.selectionESSIDFromFilter(filter, offset, limit);

        ArrayList<NetworkRecord> distinctNetworkRecords = new ArrayList<>();
        distinctNetworkRecords.addAll(bssIdRecords);
        distinctNetworkRecords.addAll(essIdRecords);

        distinctNetworkRecords = (ArrayList<NetworkRecord>) distinctNetworkRecords.stream().distinct().collect(Collectors.toList());

        return distinctNetworkRecords;
    }

    private ArrayList<NetworkRecord> distinctNetworkRecords(LogFilter filter) {
        NetworkRecordDAO networkRecordDAO = new NetworkRecordDAO(this.daoSession);
        ArrayList<NetworkRecord> bssIdRecords = networkRecordDAO.selectionBSSIDFromFilter(filter);
        ArrayList<NetworkRecord> essIdRecords = networkRecordDAO.selectionESSIDFromFilter(filter);

        ArrayList<NetworkRecord> distinctNetworkRecords = new ArrayList<>();
        distinctNetworkRecords.addAll(bssIdRecords);
        distinctNetworkRecords.addAll(essIdRecords);

        distinctNetworkRecords = (ArrayList<NetworkRecord>) distinctNetworkRecords.stream().distinct().collect(Collectors.toList());

        return distinctNetworkRecords;
    }

    private ArrayList<RecordAll> sortLists(LogFilter filter, ArrayList<MessageRecord> messageRecords) {
        ArrayList<MessageRecord> sorted;
        ArrayList<RecordAll> allRecords = new ArrayList<>();
        if (filter == null) {
            allRecords.addAll(messageRecords);
            return allRecords;
        }
        if (filter.getSorttype() != null) {
            sorted = (ArrayList<MessageRecord>) sortFilter(filter, messageRecords).stream().distinct().collect(Collectors.toList());
            allRecords.addAll(sorted);
            return allRecords;
        }
        allRecords.addAll(messageRecords);
        return allRecords;
    }

    public ArrayList<MessageRecord> addMessageRecordFields(ArrayList<AttackRecord> attackRecords, ArrayList<MessageRecord> messageRecords) {
        ArrayList<MessageRecord> updatedAttackRecords = new ArrayList<>();
        AttackRecord current = new AttackRecord();
        for (MessageRecord record : messageRecords) {
            current = attackRecords.stream().filter(o -> o.getAttack_id() == record.getAttack_id()).findAny().orElse(null);
            if (current != null) {
                record.setProtocol(current.getProtocol());
                record.setBssid(current.getBssid());
                record.setSsid(current.getSsid());
                record.setRemoteIP(current.getRemoteIP());
                record.setLocalIP(current.getLocalIP());
                record.setLocalPort(current.getLocalPort());
                record.setRemotePort(current.getRemotePort());
                record.setExternalIP(current.getExternalIP());
                record.setWasInternalAttack(current.getWasInternalAttack());
                record.setTimestampLocation(current.getTimestampLocation());
                record.setLatitude(current.getLatitude());
                record.setLongitude(current.getLongitude());
                record.setAccuracy(current.getAccuracy());
                updatedAttackRecords.add(record);
            }
        }

        return updatedAttackRecords;
    }

    public ArrayList<AttackRecord> addNetworkFields(ArrayList<AttackRecord> attackRecords, ArrayList<NetworkRecord> networkRecords) {
        ArrayList<AttackRecord> updatedAttackRecords = new ArrayList<>();
        NetworkRecord current = new NetworkRecord();

        for (AttackRecord record : attackRecords) {
            current = networkRecords.stream().filter(o -> o.getBssid().equals(record.getBssid())).findFirst().orElse(null);
            if (current != null) {
                record.setSsid(current.getSsid());
                record.setTimestampLocation(current.getTimestampLocation());
                record.setLatitude(current.getLatitude());
                record.setLongitude(current.getLongitude());
                record.setAccuracy(current.getAccuracy());
                updatedAttackRecords.add(record);
            }
        }
        return updatedAttackRecords;
    }


    /**
     * Returns the query for the given filter.
     *
     * @param filter (LogFilter)
     * @return QueryBuilder<AttackRecord> query
     */
    public ArrayList<AttackRecord> selectionQueryFromFilter(LogFilter filter, int offset, int limit) {
        ArrayList<String> filterProtocols = new ArrayList<>();
        ArrayList<AttackRecord> attackRecords = this.getAttackRecordsLimit(offset, limit);
        ArrayList<AttackRecord> list = new ArrayList<>();

        if (filter != null)
            filterProtocols = filter.getProtocols();

        if (filterProtocols.isEmpty())
            return attackRecords;

        for (final String current : filterProtocols) {
            list.addAll(attackRecords.stream().filter(o -> o.getProtocol().equals(current)).collect(Collectors.toList()));
        }

        list.removeAll(Collections.singleton(null));

        return list;

    }

    /**
     * Returns the query for the given filter.
     *
     * @param filter (LogFilter)
     * @return QueryBuilder<AttackRecord> query
     */
    public ArrayList<AttackRecord> selectionQueryFromFilter(LogFilter filter) {
        ArrayList<String> filterProtocols = new ArrayList<>();
        ArrayList<AttackRecord> attackRecords = this.getAttackRecords();
        ArrayList<AttackRecord> list = new ArrayList<>();

        if (filter != null)
            filterProtocols = filter.getProtocols();

        if (filterProtocols.isEmpty())
            return attackRecords;

        for (final String current : filterProtocols) {
            list.addAll(attackRecords.stream().filter(o -> o.getProtocol().equals(current)).collect(Collectors.toList()));
        }

        list.removeAll(Collections.singleton(null));

        return list;

    }


    /**
     * Returns the Conversation of a specific attack id with a limit of 10 in case of too many packets.
     *
     * @param attack_id Tha attack id to match the query against.
     * @return A arraylist with all {@link RecordAll Records}s for an attack id.
     */
    public synchronized ArrayList<RecordAll> getConversationForAttackID(long attack_id) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();

        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();
        qb.limit(limit);
        qb.where(MessageRecordDao.Properties.Attack_id.eq(attack_id));
        ArrayList<MessageRecord> attackRecords = (ArrayList<MessageRecord>) qb.list();

        return new ArrayList<>(attackRecords);
    }

    /**
     * Gets all {@link RecordAll Records} saved in the database.
     *
     * @return A ArrayList of all the {@link RecordAll Records} in the Database.
     */
    public synchronized ArrayList<RecordAll> getAllRecords() {
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

    public AttackRecord getMatchingAttackRecord(MessageRecord record) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.where(AttackRecordDao.Properties.Attack_id.eq(record.getAttack_id()));
        return qb.list().get(0);

    }

    /**
     * Gets a single {@link RecordAll} with the given attack id from the database.
     *
     * @param attack_id The attack id of the {@link RecordAll};
     * @return The {@link RecordAll}.
     */
    public synchronized RecordAll getRecordOfAttackId(long attack_id) {
        AttackRecordDao recordDao = this.daoSession.getAttackRecordDao();
        NetworkRecordDAO networkRecordDAO = new NetworkRecordDAO(this.daoSession);
        ArrayList<NetworkRecord> networkRecords = networkRecordDAO.getNetworkInformation();
        ArrayList<AttackRecord> updatedAttackRecords;

        QueryBuilder<AttackRecord> qb = recordDao.queryBuilder();
        qb.orderDesc(AttackRecordDao.Properties.Attack_id);
        qb.where(AttackRecordDao.Properties.Attack_id.eq(attack_id));
        qb.limit(1);

        ArrayList<AttackRecord> attackRecords = (ArrayList<AttackRecord>) qb.list();

        if (attackRecords.isEmpty())
            return null;

        updatedAttackRecords = this.addNetworkFields(attackRecords, networkRecords);

        return updatedAttackRecords.get(0);
    }

    /**
     * Gets all non duplicate Records For the key IP.
     *
     * @return A ArrayList with received Records.
     */
    public synchronized ArrayList<String> getUniqueIPRecords() {
        ArrayList<AttackRecord> attackRecords = this.getAttackRecords();

        ArrayList<String> ips = new ArrayList<>();
        attackRecords.stream().filter(o -> ips.add(o.getRemoteIP())).collect(Collectors.toList());

        return (ArrayList<String>) ips.stream().distinct().collect(Collectors.toList());

    }

    public ArrayList<MessageRecord> sortFilter(LogFilter filter, ArrayList<MessageRecord> list) {
        switch (filter.getSorttype().getValue()) {
            case 0:
                list.sort(new DateRecordComparator());
                return list;
            case 1:
                list.sort(Comparator.comparing(RecordAll::getProtocol));
                return list;
            case 2:
                list.sort(Comparator.comparing(RecordAll::getSsid));
                return list;
            case 3:
                list.sort(Comparator.comparing(RecordAll::getBssid));
                return list;
            case 7:
                list.sort(Comparator.comparingLong(MessageRecord::getAttack_id));
                return list;
            case 8:
                list.sort(Comparator.comparingLong(MessageRecord::getId));
                return list;
            default:
                return list;
        }
    }

    static class DateRecordComparator implements Comparator<RecordAll> {

        @Override
        public int compare(RecordAll recordAll, RecordAll t1) {
            return -1 * Long.compare(recordAll.getTimestamp(), t1.getTimestamp());
        }

        @Override
        public Comparator<RecordAll> reversed() {
            return null;
        }
    }

}
