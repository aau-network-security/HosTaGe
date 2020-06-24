package hostage.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;


import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.AttackRecordDao;
import de.tudarmstadt.informatik.hostage.logging.DaoMaster;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.logging.NetworkRecord;
import de.tudarmstadt.informatik.hostage.logging.NetworkRecordDao;
import de.tudarmstadt.informatik.hostage.logging.RecordAll;
import de.tudarmstadt.informatik.hostage.logging.SyncDevice;
import de.tudarmstadt.informatik.hostage.logging.SyncDeviceDao;
import de.tudarmstadt.informatik.hostage.logging.SyncInfoRecordDao;
import de.tudarmstadt.informatik.hostage.persistence.DAO.AttackRecordDAO;
import de.tudarmstadt.informatik.hostage.persistence.DAO.NetworkRecordDAO;
import de.tudarmstadt.informatik.hostage.persistence.DAO.SyncDeviceDAO;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AttackRecordDAOTest {
    private DaoSession daoSession;
    private AttackRecordDao attackRecordDao; //greenDao
    private SyncDeviceDao syncDeviceDao;

    private AttackRecordDAO attackRecordDAO; //persistence DAO
    private  AttackRecord record;


    @Before
    public void setUp() {
        DaoMaster.DevOpenHelper openHelper = new DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null);
        Database db = openHelper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
        attackRecordDao = daoSession.getAttackRecordDao();
        syncDeviceDao = daoSession.getSyncDeviceDao();

        attackRecordDAO = new AttackRecordDAO(this.daoSession);
        record= new AttackRecord();

    }

    @Test
    public void testBasics() {
        record.setBssid("1");
        daoSession.insert(record);
        assertNotNull(record.getAttack_id());
        assertNotNull(attackRecordDao.load(record.getAttack_id()));
        assertEquals(1, attackRecordDao.count());
        assertEquals(1, daoSession.loadAll(AttackRecord.class).size());

        daoSession.update(record);
        daoSession.delete(record);
        assertNull(attackRecordDao.load(record.getAttack_id()));
    }

    @Test
    public void testQueryForCurrentThread() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Query<AttackRecord>[] queryHolder = new Query[1];
        new Thread() {
            @Override
            public void run() {
                try {
                    queryHolder[0] = attackRecordDao.queryBuilder().build();
                    queryHolder[0].list();
                } finally {
                    latch.countDown();
                }
            }
        }.start();
        latch.await();
        Query<AttackRecord> query = queryHolder[0].forCurrentThread();
        query.list();
    }

    @Test
    public void testInsert(){
        attackRecordDAO.insert(record);
        assertNotNull(record.getAttack_id());
        assertNotNull(attackRecordDao.load(record.getAttack_id()));
    }

    @Test
    public void  testInsertAttackRecords(){
        ArrayList<AttackRecord> records = new ArrayList<>();
        AttackRecord second = new AttackRecord();
        second.setAttack_id(2);
        records.add(record);
        records.add(second);

        attackRecordDAO.insertAttackRecords(records);
        assertNotNull(records);
        assertEquals(2,records.size());

    }

    @Test
    public void testGetAttackPerProtocolCount(){
        String protocol = "Http";
        record.setProtocol(protocol);
        attackRecordDAO.insert(record);

       int result = attackRecordDAO.getAttackPerProtocolCount(protocol);

        assertEquals(1, result);

    }

    @Test
    public void testGetAttackPerProtocolCountTwoArguments(){
        String protocol = "Http";
        record.setProtocol(protocol);
        record.setAttack_id(1);
        attackRecordDAO.insert(record);

        int result = attackRecordDAO.getAttackPerProtocolCount(protocol,1);

        assertEquals(1, result);

    }

    @Test
    public void testGetAttackPerProtocolCountThreeArguments(){
        String protocol = "Http";
        String protocol2 = "smb";
        String bssid = "test";

        AttackRecord secondRecord = new AttackRecord();

        secondRecord.setAttack_id(2);
        secondRecord.setProtocol(protocol2);
        secondRecord.setBssid(bssid);

        record.setProtocol(protocol);
        record.setAttack_id(1);
        record.setBssid(bssid);

        attackRecordDAO.insert(record);
        attackRecordDAO.insert(secondRecord);

        int result = attackRecordDAO.getAttackPerProtocolCount(protocol,1,bssid);

        assertEquals(1, result);

    }
    //TODO change logic
    public void testupdateUntrackedAttacks(){
        AttackRecord second = new AttackRecord();
        AttackRecord third = new AttackRecord();
        SyncDevice  device = new SyncDevice();
        String deviceName = "new";
        second.setAttack_id(2);
        third.setAttack_id(3); //highest id
        device.setDeviceID("1");


        record.setDevice(deviceName);
        daoSession.insert(record);
        daoSession.insert(second);
        daoSession.insert(third);
        daoSession.insert(device);

       attackRecordDAO.updateUntrackedAttacks();

        assertEquals(3,attackRecordDao.count());
        assertEquals(3,syncDeviceDao.load(device.getDeviceID()).getHighest_attack_id());

    }
    //TODO change logic
    public void testUpdateSyncAttackCounter(){
        record.setAttack_id(1);
        attackRecordDAO.updateSyncAttackCounter(record);

        assertNotNull(record.getAttack_id());
        //assertNotNull(attackRecordDao.load(record.getAttack_id()));
        assertEquals(1,attackRecordDao.count());

    }

    @Test
    public void testUpdateSyncDevicesMaxID(){
        AttackRecord recordSecond = new AttackRecord();
        recordSecond.setAttack_id(2);
        SyncDevice  device = new SyncDevice();
        SyncDevice  deviceSecond = new SyncDevice();

        device.setDeviceID("1");
        deviceSecond.setDeviceID("2");
        record.setSync_id(1);
        recordSecond.setSync_id(2); //highest

        daoSession.insert(device);
        daoSession.insert(deviceSecond);
        daoSession.insert(record);
        daoSession.insert(recordSecond);

        attackRecordDAO.updateSyncDevicesMaxID();

        assertEquals(2,syncDeviceDao.load(deviceSecond.getDeviceID()).getHighest_attack_id());
        assertEquals(2,syncDeviceDao.load(device.getDeviceID()).getHighest_attack_id());

    }

    @Test
    public void testBssidSeen(){
        String protocol = "http";
        String bssid = "test";
        record.setProtocol(protocol);
        record.setBssid(bssid);

        daoSession.insert(record);

        boolean bssidSeen = attackRecordDAO.bssidSeen(protocol,bssid);

        assertTrue(bssidSeen);
    }

    @Test
    public void testGetNumAttacksSeenByBSSID(){
        String bssid = "test";
        record.setBssid(bssid);

        daoSession.insert(record);

        int counter = attackRecordDAO.getNumAttacksSeenByBSSID(bssid);

        assertEquals(1,counter);

    }


    @Test
    public void testGetNumAttacksSeenByBSSIDProtocol(){
        String bssid = "test";
        String protocol = "http";
        String protocol2 = "smb";
        AttackRecord secondRecord = new AttackRecord();
        record.setBssid(bssid);
        record.setProtocol(protocol);
        secondRecord.setAttack_id(2);
        secondRecord.setBssid(bssid);
        secondRecord.setProtocol(protocol2);

        daoSession.insert(record);
        daoSession.insert(secondRecord);

        int counter = attackRecordDAO.getNumAttacksSeenByBSSID(protocol,bssid);

        assertEquals(1,counter);

    }

    @Test
    public void testSelectionQueryFromFilter(){
        String http = "http";
        String smb = "smb";
        AttackRecord recordSecond = new AttackRecord();

        LogFilter filter =  attackRecordFilter(smb, http,recordSecond);
        ArrayList<AttackRecord> records = attackRecordDAO.selectionQueryFromFilter(filter);

        assertEquals(http,records.get(0).getProtocol());
        assertEquals(smb,records.get(1).getProtocol());

    }

    @Test
    public void testGetConversationForAttackID(){
        AttackRecord recordSecond = new AttackRecord();
        recordSecond.setAttack_id(2); //have to set the id.
        record.setAttack_id(1);
        MessageRecord messageRecord = new MessageRecord();
        messageRecord.setAttack_id(2);
        daoSession.insert(messageRecord);
        daoSession.insert(record);
        daoSession.insert(recordSecond);

        ArrayList<RecordAll> records = attackRecordDAO.getConversationForAttackID(2);

        assertEquals(2,records.get(0).getAttack_id());

    }

    @Test
    public void testGetAllRecords(){
        NetworkRecord networkRecord = new NetworkRecord();
        MessageRecord messageRecord = new MessageRecord();
        networkRecord.setBssid("1");
        messageRecord.setId(1);

        daoSession.insert(networkRecord);
        daoSession.insert(messageRecord);
        daoSession.insert(record);

        ArrayList<RecordAll> records = attackRecordDAO.getAllRecords();

        assertNotNull(records);
        assertEquals(3,records.size());


    }

    @Test
    public void testGetRecordsForFilter(){
        LogFilter filter = new LogFilter();
        ArrayList<String> filterProtocols = new ArrayList<>();
        ArrayList<String> bssids = new ArrayList<>();
        ArrayList<String> essids = new ArrayList<>();
        MessageRecord messageRecord = new MessageRecord();
        AttackRecord secondRecord = new AttackRecord();
        NetworkRecord networkRecord = new NetworkRecord();
        NetworkRecord networkRecordSecond = new NetworkRecord();

        String http = "http";
        String smb = "smb";

        String firstBssid = "test";
        String secondBssid = "test1";

        String firstEssid = "test";
        String secondEssid = "test1";


        filterProtocols.add(http);
        filterProtocols.add(smb);
        bssids.add(firstBssid);
        bssids.add(secondBssid);
        essids.add(firstEssid);
        essids.add(secondEssid);

        filter.setProtocols(filterProtocols);
        filter.setBSSIDs(bssids);
        filter.setESSIDs(essids);
        filter.setAboveTimestamp(100);
        filter.setBelowTimestamp(50);

        messageRecord.setTimestamp(51);
        messageRecord.setId(1);
        messageRecord.setAttack_id(1);
        record.setProtocol(http);
        record.setAttack_id(1);
        record.setBssid(firstBssid);
        secondRecord.setProtocol(smb);
        secondRecord.setAttack_id(2);
        networkRecord.setBssid(firstBssid);
        networkRecordSecond.setBssid(secondBssid);
        networkRecord.setSsid(firstEssid);
        networkRecordSecond.setSsid(secondEssid);

        daoSession.insert(messageRecord);
        daoSession.insert(record);
        daoSession.insert(secondRecord);
        daoSession.insert(networkRecord);
        daoSession.insert(networkRecordSecond);

        ArrayList<RecordAll> records = attackRecordDAO.getRecordsForFilter(filter,0);

        assertNotNull(records);
        assertEquals(1,records.size());
        assertEquals(http,records.get(0).getProtocol());
    }

    @Test
    public void testDeleteAttacksByFilter(){
        String http = "http";
        String smb = "smb";
        AttackRecord recordSecond = new AttackRecord();

        LogFilter filter =  attackRecordFilter(smb, http,recordSecond);

        ArrayList<AttackRecord> records = attackRecordDAO.selectionQueryFromFilter(filter,0);

        assertEquals(http,records.get(0).getProtocol());
        assertEquals(smb,records.get(1).getProtocol());

        attackRecordDAO.deleteAttacksByFilter(filter,0);

        assertNull(attackRecordDao.load(record.getAttack_id()));
        assertNull(attackRecordDao.load(recordSecond.getAttack_id()));


    }

    @Test
    public void testDeleteByAttackID(){
        daoSession.insert(record);
        assertNotNull(attackRecordDao.load(record.getAttack_id()));

        attackRecordDAO.deleteByAttackID(record.getAttack_id());

        assertNull(attackRecordDao.load(record.getAttack_id()));

    }


    private LogFilter attackRecordFilter(String smb, String http, AttackRecord recordSecond){
        LogFilter filter = new LogFilter();
        ArrayList<String> filterProtocols = new ArrayList<>();
        String notExistent= "random";
        filterProtocols.add(http);
        filterProtocols.add(smb);
        filterProtocols.add(notExistent);

        filter.setProtocols(filterProtocols);
        recordSecond.setAttack_id(2);
        recordSecond.setProtocol(smb);
        record.setProtocol(http);

        daoSession.insert(record);
        daoSession.insert(recordSecond);

        return filter;
    }


    public void createRecords(){
        LogFilter filter = new LogFilter();
        ArrayList<String> filterProtocols = new ArrayList<>();
        ArrayList<String> bssids = new ArrayList<>();
        ArrayList<String> essids = new ArrayList<>();
        MessageRecord messageRecord = new MessageRecord();
        AttackRecord secondRecord = new AttackRecord();
        NetworkRecord networkRecord = new NetworkRecord();
        NetworkRecord networkRecordSecond = new NetworkRecord();

        String http = "http";
        String smb = "smb";

        String firstBssid = "test";
        String secondBssid = "test1";

        String firstEssid = "test";
        String secondEssid = "test1";


        filterProtocols.add(http);
        filterProtocols.add(smb);
        bssids.add(firstBssid);
        bssids.add(secondBssid);
        essids.add(firstEssid);
        essids.add(secondEssid);

        filter.setProtocols(filterProtocols);
        filter.setBSSIDs(bssids);
        filter.setESSIDs(essids);
        filter.setAboveTimestamp(100);
        filter.setBelowTimestamp(50);

        messageRecord.setTimestamp(51);
        messageRecord.setId(1);
        record.setProtocol(http);
        secondRecord.setProtocol(smb);
        secondRecord.setAttack_id(2);
        networkRecord.setBssid(firstBssid);
        networkRecordSecond.setBssid(secondBssid);
        networkRecord.setSsid(firstEssid);
        networkRecordSecond.setSsid(secondEssid);

        ArrayList<NetworkRecord> networkRecords = new ArrayList<>();
        ArrayList<MessageRecord> messageRecords = new ArrayList<>();
        ArrayList<AttackRecord> attackRecords = new ArrayList<>();

        networkRecords.add(networkRecord);
        networkRecords.add(networkRecordSecond);
        messageRecords.add(messageRecord);
        attackRecords.add(record);
        attackRecords.add(secondRecord);

        //ArrayList<RecordAll> allrecords = attackRecordDAO.createRecords(attackRecords,networkRecords,messageRecords);

//        assertNotNull(allrecords);
//        assertEquals(1,allrecords.size());
//        assertEquals(http,allrecords.get(0).getProtocol());
    }



    @After
    public void breakdown(){
        daoSession.clear();
        record = null;

    }
}
