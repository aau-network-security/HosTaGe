package dk.aau.netsec.hostage.persistence.DAO;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

import dk.aau.netsec.hostage.logging.AttackRecord;
import dk.aau.netsec.hostage.logging.AttackRecordDao;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.logging.MessageRecord;
import dk.aau.netsec.hostage.logging.MessageRecordDao;
import dk.aau.netsec.hostage.logging.Record;
import dk.aau.netsec.hostage.ui.model.LogFilter;

public class MessageRecordDAO extends DAO {

    private final DaoSession daoSession;

    public MessageRecordDAO(DaoSession daoSession) {
        this.daoSession = daoSession;

    }

    /**
     * Adds a given {@link MessageRecord} to the database.
     *
     * @param record The added {@link MessageRecord} .
     */
    public void insert(MessageRecord record) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        insertElement(recordDao, record);
    }

    /**
     * Adds a given {@link MessageRecord}s to the database.
     *
     * @param records {@link List}<MessageRecord>
     *                The added {@link MessageRecord}s .
     */
    public void insertMessageRecords(List<MessageRecord> records) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        insertElements(recordDao, records);

    }

    /**
     * Returns Last Inserted Record.
     *
     * @return
     */

    public MessageRecord getLastedInsertedRecord() {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        MessageRecord record = new MessageRecord();

        List<MessageRecord> messageRecords = recordDao.queryBuilder()
                .orderDesc(MessageRecordDao.Properties.Id)
                .limit(1)
                .list();
        if (!messageRecords.isEmpty()) {
            return messageRecords.get(0);
        }
        return record;
    }

    public void updateRecord(MessageRecord record) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();

        updateElement(recordDao, record);
    }

    public ArrayList<MessageRecord> getAllMessageRecords() {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();

        return (ArrayList<MessageRecord>) recordDao.queryBuilder()
                .orderDesc(MessageRecordDao.Properties.Id)
                .list();
    }


    public ArrayList<MessageRecord> getAllMessageRecordsLimit(int offset, int limit) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();
        qb.orderDesc(MessageRecordDao.Properties.Timestamp).build();
        qb.offset(offset).limit(limit).build();

        return (ArrayList<MessageRecord>) qb.list();

    }

    /**
     * Determines the number of {@link Record Records} in the database.
     *
     * @return The number of {@link Record Records} in the database.
     */
    public synchronized int getRecordCount() {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();

        return (int) countElements(recordDao);
    }

    /**
     * Deletes all records from the persistence.
     */
    public synchronized void clearData() {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();

        deleteAllFromTable(recordDao);

    }

    public ArrayList<MessageRecord> getMessageRecords(AttackRecord attackRecord) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();

        return (ArrayList<MessageRecord>) selectElementsByCondition
                (recordDao, MessageRecordDao.Properties.Attack_id.eq(attackRecord.getAttack_id()));

    }


    /**
     * Returns the query for the given filter.
     *
     * @param filter (LogFilter)
     * @return QueryBuilder<AttackRecord> query
     */
    public ArrayList<MessageRecord> selectionQueryFromFilter(LogFilter filter, int offset, int limit) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        ArrayList<MessageRecord> list = getAllMessageRecordsLimit(offset, limit);
        if (filter == null)
            return list;

        if (filter.getBelowTimestamp() == 0 || filter.getAboveTimestamp() == 0)
            return list;

        return filterTimestamp(recordDao, filter, offset, limit);
    }

    private ArrayList<MessageRecord> filterTimestamp(MessageRecordDao recordDao, LogFilter filter, int offset, int limit) {
        ArrayList<MessageRecord> list;

        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();
        qb.orderDesc(MessageRecordDao.Properties.Id);
        qb.and(MessageRecordDao.Properties.Timestamp.lt(filter.getBelowTimestamp()),
                MessageRecordDao.Properties.Timestamp.gt(filter.getAboveTimestamp()));
        qb.offset(offset).limit(limit);

        list = (ArrayList<MessageRecord>) qb.list();

        return list;
    }

    /**
     * Returns the query for the given filter.
     *
     * @param filter (LogFilter)
     * @return QueryBuilder<AttackRecord> query
     */
    public ArrayList<MessageRecord> selectionQueryFromFilter(LogFilter filter) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        ArrayList<MessageRecord> list = this.getAllMessageRecords();

        if (filter == null)
            return list;

        if (filter.getBelowTimestamp() == 0 || filter.getAboveTimestamp() == 0)
            return list;
        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();

        qb.and(MessageRecordDao.Properties.Timestamp.lt(filter.getBelowTimestamp()),
                MessageRecordDao.Properties.Timestamp.gt(filter.getAboveTimestamp()));
        qb.orderDesc(MessageRecordDao.Properties.Id);

        list = (ArrayList<MessageRecord>) qb.list();

        return list;
    }

    /**
     * Returns the query for the given filter.
     *
     * @param filter (LogFilter)
     * @return QueryBuilder<AttackRecord> query
     */
    public ArrayList<MessageRecord> selectionQueryFromFilterMultiStage(LogFilter filter) {
        return filterList(filter);
    }

    private ArrayList<MessageRecord> filterList(LogFilter filter) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        ArrayList<MessageRecord> list;
        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();

        qb.and(MessageRecordDao.Properties.Timestamp.lt(filter.getBelowTimestamp()),
                MessageRecordDao.Properties.Timestamp.gt(filter.getAboveTimestamp()));

        list = (ArrayList<MessageRecord>) qb.list();

        return list;

    }

    public ArrayList<AttackRecord> joinAttacks(String protocol) {
        AttackRecordDao messageRecordDao = this.daoSession.getAttackRecordDao();

        QueryBuilder<AttackRecord> qb = messageRecordDao.queryBuilder();
        qb.join(MessageRecord.class
                , MessageRecordDao.Properties.Attack_id);

        return (ArrayList<AttackRecord>) qb.list();

    }

    public void deleteFromFilter(LogFilter filter) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();


        deleteElementsByCondition(recordDao, MessageRecordDao.Properties.Timestamp.lt(filter.getBelowTimestamp()),
                MessageRecordDao.Properties.Timestamp.gt(filter.getAboveTimestamp()));

    }


}
