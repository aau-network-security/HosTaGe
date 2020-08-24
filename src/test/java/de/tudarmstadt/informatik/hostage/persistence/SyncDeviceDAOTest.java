package de.tudarmstadt.informatik.hostage.persistence;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.Query;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import de.tudarmstadt.informatik.hostage.logging.DaoMaster;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.SyncDevice;
import de.tudarmstadt.informatik.hostage.logging.SyncDeviceDao;
import de.tudarmstadt.informatik.hostage.persistence.DAO.SyncDeviceDAO;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class SyncDeviceDAOTest {
    private DaoSession daoSession;
    private SyncDeviceDao syncDeviceDao; //greenDao

    private SyncDeviceDAO syncDeviceDAO; //persistence DAO
    private SyncDevice record;

    @Before
    public void setUp() {
        DaoMaster.DevOpenHelper openHelper = new DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null);
        Database db = openHelper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
        syncDeviceDao = daoSession.getSyncDeviceDao();

        syncDeviceDAO = new SyncDeviceDAO(this.daoSession);
        record= new SyncDevice();
    }

    @Test
    public void testBasics() {
        record.setDeviceID("1");
        daoSession.insert(record);
        assertNotNull(record.getDeviceID());
        assertNotNull(syncDeviceDao.load(record.getDeviceID()));
        assertEquals(1, syncDeviceDao.count());
        assertEquals(1, daoSession.loadAll(SyncDevice.class).size());

        daoSession.update(record);
        daoSession.delete(record);
        assertNull(syncDeviceDao.load(record.getDeviceID()));
    }

    @Test
    public void testQueryForCurrentThread() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Query<SyncDevice>[] queryHolder = new Query[1];
        new Thread() {
            @Override
            public void run() {
                try {
                    queryHolder[0] = syncDeviceDao.queryBuilder().build();
                    queryHolder[0].list();
                } finally {
                    latch.countDown();
                }
            }
        }.start();
        latch.await();
        Query<SyncDevice> query = queryHolder[0].forCurrentThread();
        query.list();
    }

    @Test
    public void testInsert(){
        record.setDeviceID("1");
        syncDeviceDAO.insert(record);
        Assert.assertNotNull(record.getDeviceID());
        Assert.assertNotNull(syncDeviceDao.load(record.getDeviceID()));
    }

    @Test
    public void  testInsertMessageRecords(){
        ArrayList<SyncDevice> records = new ArrayList<>();
        SyncDevice second = new SyncDevice();
        record.setDeviceID("1");
        second.setDeviceID("2");
        records.add(record);
        records.add(second);

        syncDeviceDAO.insertSyncDevices(records);
        assertNotNull(records);
        assertEquals(2,records.size());
        assertEquals("1",syncDeviceDao.load(record.getDeviceID()).getDeviceID());
        assertEquals("2",syncDeviceDao.load(second.getDeviceID()).getDeviceID());

    }

    @Test
    public void  testUpdateSyncDevices(){
        String deviceId = "de/tudarmstadt/informatik/hostage/fragment";
        HashMap<String, Long> devices =  new HashMap<>();
        devices.put(deviceId,1L);

        record.setDeviceID(deviceId);
        record.setLast_sync_timestamp(2L);

        daoSession.insert(record);
        assertEquals(2L,syncDeviceDao.load(record.getDeviceID()).getLast_sync_timestamp());

        syncDeviceDAO.updateSyncDevices(devices);

        assertEquals(1L,syncDeviceDao.load(record.getDeviceID()).getLast_sync_timestamp());

    }

//    @Test
//    public void testGetOwnState(){
//        AttackRecord attackRecord = new AttackRecord();
//        NetworkRecord networkRecord = new NetworkRecord();
//        networkRecord.setBssid("1");
//        attackRecord.setAttack_id(1);
//        attackRecord.setRecord(networkRecord);
//        record.setDeviceID("2");
//        record.setHighest_attack_id(1);
//        attackRecord.setSyncDevice(record);
//
//        daoSession.insert(attackRecord);
//        daoSession.insert(networkRecord);
//        daoSession.insert(record);
//
//        SyncInfo info = syncDeviceDAO.getOwnState();
//        assertEquals(1,info.bssids.size());
//        assertEquals("1",info.bssids.get(0));
//        assertEquals(1,info.deviceMap.size());
//
//    }

    @After
    public void breakdown(){
        daoSession.clear();
        record = null;
    }
}
