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
import java.util.concurrent.CountDownLatch;

import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.DaoMaster;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.logging.MessageRecordDao;
import de.tudarmstadt.informatik.hostage.persistence.DAO.MessageRecordDAO;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class MessageRecordDAOTest {
    private DaoSession daoSession;
    private MessageRecordDao messageRecordDao; //greenDao

    private MessageRecordDAO messageRecordDAO; //persistence DAO
    private MessageRecord record;

    @Before
    public void setUp() {
        DaoMaster.DevOpenHelper openHelper = new DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null);
        Database db = openHelper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
        messageRecordDao = daoSession.getMessageRecordDao();

        messageRecordDAO = new MessageRecordDAO(this.daoSession);
        record= new MessageRecord();
    }

    @Test
    public void testBasics() {
        record.setBssid("1");
        daoSession.insert(record);
        assertNotNull(record.getId());
        assertNotNull(messageRecordDao.load(record.getId()));
        assertEquals(1, messageRecordDao.count());
        assertEquals(1, daoSession.loadAll(MessageRecord.class).size());

        daoSession.update(record);
        daoSession.delete(record);
        assertNull(messageRecordDao.load(record.getId()));
    }

    @Test
    public void testQueryForCurrentThread() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Query<MessageRecord>[] queryHolder = new Query[1];
        new Thread() {
            @Override
            public void run() {
                try {
                    queryHolder[0] = messageRecordDao.queryBuilder().build();
                    queryHolder[0].list();
                } finally {
                    latch.countDown();
                }
            }
        }.start();
        latch.await();
        Query<MessageRecord> query = queryHolder[0].forCurrentThread();
        query.list();
    }

    @Test
    public void testInsert(){
        record.setId(1);
        messageRecordDAO.insert(record);
        Assert.assertNotNull(record.getId());
        Assert.assertNotNull(messageRecordDao.load(record.getId()));
    }

    @Test
    public void testInsertMessageRecords(){
        ArrayList<MessageRecord> records = new ArrayList<>();
        MessageRecord second = new MessageRecord();
        second.setId(2);
        records.add(record);
        records.add(second);

        messageRecordDAO.insertMessageRecords(records);
        assertNotNull(records);
        assertEquals(2,records.size());

    }

    @Test
    public void testJoins(){
        String protocolFirst = "https";
        String protocolSecond = "telnet";
        AttackRecord attackRecord = new AttackRecord();
        AttackRecord attackRecordSecond = new AttackRecord();

        attackRecord.setAttack_id(2);
        attackRecord.setAttack_id(2);
        attackRecord.setProtocol(protocolFirst);
        attackRecordSecond.setAttack_id(3);
        attackRecordSecond.setProtocol(protocolSecond);

        record.setId(1);
        record.setAttack_id(2);
        record.setRecord(attackRecord);
        daoSession.insert(attackRecord);
        daoSession.insert(attackRecordSecond);
        daoSession.insert(record);

        ArrayList<AttackRecord> records = messageRecordDAO.joinAttacks(protocolFirst);
        assertEquals(1,records.size());
        assertEquals(protocolFirst,records.get(0).getProtocol());

    }

    @After
    public void breakdown(){
        daoSession.clear();
        record = null;
    }
}
