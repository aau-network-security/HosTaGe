package de.tudarmstadt.informatik.hostage.persistence.DAO;

import java.util.ArrayList;

import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.SyncInfoRecord;
import de.tudarmstadt.informatik.hostage.logging.SyncInfoRecordDao;


public class SyncInfoRecordDAO extends DAO {
    private DaoSession daoSession;

    public SyncInfoRecordDAO(DaoSession daoSession){
        this.daoSession= daoSession;

    }

    /**
     * Adds a given {@link SyncInfoRecord} to the database.
     *
     * @param record
     *            The added {@link SyncInfoRecord} .
     */
    public  void insert(SyncInfoRecord record){
        SyncInfoRecordDao recordDao = this.daoSession.getSyncInfoRecordDao();
        insertElement(recordDao,record);
    }

    /**
     * Returns an ArrayList of all SyncInfo.
     * @return ArrayList containing all ths SynchInfo
     */
    public synchronized ArrayList<SyncInfoRecord> getSyncInfoRecords(){
        SyncInfoRecordDao recordDao = this.daoSession.getSyncInfoRecordDao();
        ArrayList<SyncInfoRecord> records = (ArrayList<SyncInfoRecord>) selectElements(recordDao);

        return records;

    }

    /**
     * Returns a ArrayList containing all information stored in the SyncInfo table.
     * @return ArrayList<SyncInfo>
     */
    public synchronized ArrayList<SyncInfoRecord> getSyncInfo(){
        ArrayList<SyncInfoRecord> records = this.getSyncInfoRecords();

        return records;

    }

    /**
     * Updates the sync_info table with the information contained in the parameter.
     * @param syncInfos ArrayList of {@link SyncInfoRecord SyncInfoRecords}
     */
    public synchronized void updateSyncInfo(ArrayList<SyncInfoRecord> syncInfos){
        SyncInfoRecordDao recordDao = this.daoSession.getSyncInfoRecordDao();

        updateElements(recordDao,syncInfos);

    }

}
