package de.tudarmstadt.informatik.hostage.persistence.DAO;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.logging.MessageRecordDao;
import de.tudarmstadt.informatik.hostage.logging.Record;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;



public class MessageRecordDAO extends DAO {

    private DaoSession daoSession;

    public MessageRecordDAO(DaoSession daoSession){
        this.daoSession= daoSession;

    }

    /**
     * Adds a given {@link MessageRecord} to the database.
     *
     * @param record
     *            The added {@link MessageRecord} .
     */
    public void insert(MessageRecord record){
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        insertElement(recordDao,record);
    }

    /**
     * Adds a given {@link MessageRecord}s to the database.
     *
     * @param records {@link List}<MessageRecord>
     *            The added {@link MessageRecord}s .
     */
    public void insertMessageRecords(List<MessageRecord> records){
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        insertElements(recordDao,records);

    }


    public ArrayList<MessageRecord> getAllMessageRecords(){
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        ArrayList<MessageRecord> messageRecords = (ArrayList<MessageRecord>) selectElements(recordDao);

        return  messageRecords;

    }

    /**
     * Determines the number of {@link Record Records} in the database.
     *
     * @return The number of {@link Record Records} in the database.
     */
    public synchronized int getRecordCount() {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();

        int result = (int) countElements(recordDao);

        return  result;
    }

    /**
     * Deletes all records from the persistence.
     */
    public synchronized void clearData() {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();

        deleteAllFromTable(recordDao);

    }

    public ArrayList<MessageRecord> getMessageRecords(AttackRecord attackRecord ){
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();

        ArrayList<MessageRecord> records =
                (ArrayList<MessageRecord> )selectElementsByCondition
                        (recordDao, MessageRecordDao.Properties.Attack_id.eq(attackRecord.getAttack_id()));
        return records;

    }

    /**
     * Returns the query for the given filter.
     * @param filter (LogFilter)
     * @return QueryBuilder<AttackRecord> query
     */
    public ArrayList<MessageRecord> selectionQueryFromFilter(LogFilter filter) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();

            qb.and(MessageRecordDao.Properties.Timestamp.lt(filter.getBelowTimestamp()),
                    MessageRecordDao.Properties.Timestamp.gt(filter.getAboveTimestamp()));

        ArrayList<MessageRecord> list = (ArrayList<MessageRecord>) qb.list();
        return list;

    }

    public void deleteFromFilter(LogFilter filter){
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();


        deleteElementsByCondition(recordDao,qb.and(MessageRecordDao.Properties.Timestamp.lt(filter.getBelowTimestamp()),
                MessageRecordDao.Properties.Timestamp.gt(filter.getAboveTimestamp())));

    }


    }
