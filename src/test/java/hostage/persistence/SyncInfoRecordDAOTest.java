package hostage.persistence;

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
import java.util.concurrent.CountDownLatch;

import de.tudarmstadt.informatik.hostage.logging.DaoMaster;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.SyncInfoRecord;
import de.tudarmstadt.informatik.hostage.logging.SyncInfoRecordDao;
import de.tudarmstadt.informatik.hostage.persistence.DAO.SyncInfoRecordDAO;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class SyncInfoRecordDAOTest {
    private DaoSession daoSession;
    private SyncInfoRecordDao syncInfoRecordDao; //greenDao

    private SyncInfoRecordDAO syncInfoRecordDAO; //persistence DAO
    private SyncInfoRecord record;

    @Before
    public void setUp() {
        DaoMaster.DevOpenHelper openHelper = new DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null);
        Database db = openHelper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
        syncInfoRecordDao = daoSession.getSyncInfoRecordDao();

        syncInfoRecordDAO = new SyncInfoRecordDAO(this.daoSession);
        record= new SyncInfoRecord();
    }

    @Test
    public void testBasics() {
        record.setDeviceID("1");
        daoSession.insert(record);
        assertNotNull(record.getDeviceID());
        assertNotNull(syncInfoRecordDao.load(record.getDeviceID()));
        assertEquals(1, syncInfoRecordDao.count());
        assertEquals(1, daoSession.loadAll(SyncInfoRecord.class).size());

        daoSession.update(record);
        daoSession.delete(record);
        assertNull(syncInfoRecordDao.load(record.getDeviceID()));
    }

    @Test
    public void testQueryForCurrentThread() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Query<SyncInfoRecord>[] queryHolder = new Query[1];
        new Thread() {
            @Override
            public void run() {
                try {
                    queryHolder[0] = syncInfoRecordDao.queryBuilder().build();
                    queryHolder[0].list();
                } finally {
                    latch.countDown();
                }
            }
        }.start();
        latch.await();
        Query<SyncInfoRecord> query = queryHolder[0].forCurrentThread();
        query.list();
    }

    @Test
    public void testInsert(){
        record.setDeviceID("1");
        syncInfoRecordDAO.insert(record);
        Assert.assertNotNull(record.getDeviceID());
        Assert.assertNotNull(syncInfoRecordDao.load(record.getDeviceID()));
    }

    @Test
    public void testGetSyncInfoRecords(){
        record.setDeviceID("1");
        SyncInfoRecord syncInfoRecord = new SyncInfoRecord();
        syncInfoRecord.setDeviceID("2");

        daoSession.insert(record);
        daoSession.insert(syncInfoRecord);

        ArrayList<SyncInfoRecord> records = syncInfoRecordDAO.getSyncInfo();
        assertNotNull(records);
        assertEquals(2,records.size());

    }

    @Test
    public void testUpdateSyncInfo(){
        record.setDeviceID("1");
        SyncInfoRecord syncInfoRecord = new SyncInfoRecord();
        syncInfoRecord.setDeviceID("2");

        daoSession.insert(record);
        daoSession.insert(syncInfoRecord);

        assertEquals("1",syncInfoRecordDao.load(record.getDeviceID()).getDeviceID());
        assertEquals("2",syncInfoRecordDao.load(syncInfoRecord.getDeviceID()).getDeviceID());

        record.setDeviceID("5");
        syncInfoRecord.setDeviceID("6");

        ArrayList<SyncInfoRecord> records = new ArrayList<>();

        records.add(record);
        records.add(syncInfoRecord);

        syncInfoRecordDAO.updateSyncInfo(records);

        assertEquals("5",syncInfoRecordDao.load(record.getDeviceID()).getDeviceID());
        assertEquals("6",syncInfoRecordDao.load(syncInfoRecord.getDeviceID()).getDeviceID());


    }

    @After
    public void breakdown(){
        daoSession.clear();
        record = null;
    }
}
