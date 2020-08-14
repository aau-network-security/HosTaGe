package hostage.persistence;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;

import de.tudarmstadt.informatik.hostage.logging.DaoMaster;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.model.ProfileDao;
import de.tudarmstadt.informatik.hostage.persistence.DAO.ProfileDAO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class ProfileDAOTest {
    private DaoSession daoSession;
    private ProfileDao profileDao; //greenDao

    private ProfileDAO profileDAO; //persistence DAO
    private Profile record;

    @Before
    public void setUp() {
        DaoMaster.DevOpenHelper openHelper = new DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null);
        Database db = openHelper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
        profileDao = daoSession.getProfileDao();

        profileDAO = new ProfileDAO(this.daoSession);
        record= new Profile();

    }

    @Test
    public void testBasics() {
        record.setId(1);
        daoSession.insert(record);
        assertNotNull(record.getId());
        assertNotNull(profileDao.load(record.getId()));
        assertEquals(1, profileDao.count());
        assertEquals(1, daoSession.loadAll(Profile.class).size());

        daoSession.update(record);
        daoSession.delete(record);
        assertNull(profileDao.load(record.getId()));
    }

    @Test
    public void testQueryForCurrentThread() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Query<Profile>[] queryHolder = new Query[1];
        new Thread() {
            @Override
            public void run() {
                try {
                    queryHolder[0] = profileDao.queryBuilder().build();
                    queryHolder[0].list();
                } finally {
                    latch.countDown();
                }
            }
        }.start();
        latch.await();
        Query<Profile> query = queryHolder[0].forCurrentThread();
        query.list();
    }

    @Test
    public void testInsert(){
        record.setId(1);
        profileDAO.insert(record);
        assertNotNull(record.getId());
        assertNotNull(profileDao.load(record.getId()));
    }

    @After
    public void breakdown(){
        daoSession.clear();
        record = null;

    }

}
