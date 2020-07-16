package de.tudarmstadt.informatik.hostage.persistence.DAO;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * Returns Last Inserted Record.
     * @return
     */

    public MessageRecord getLastedInsertedRecord(){
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        MessageRecord record = new MessageRecord();

        List<MessageRecord> messageRecords = recordDao.queryBuilder()
                .orderDesc(MessageRecordDao.Properties.Id)
                .limit(1)
                .list();
        if(!messageRecords.isEmpty()){
            return messageRecords.get(0);
        }
        return  record;
    }

    public void updateRecord(MessageRecord record){
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();

        updateElement(recordDao,record);
    }

    public ArrayList<MessageRecord> getAllMessageRecords(){
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        ArrayList<MessageRecord> messageRecords = (ArrayList<MessageRecord>) recordDao.queryBuilder()
                .orderDesc(MessageRecordDao.Properties.Id)
                .list();

        return  messageRecords;
    }


    public ArrayList<MessageRecord> getAllMessageRecordsLimit(int offset,int limit){
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();
        qb.orderDesc(MessageRecordDao.Properties.Timestamp).build();
        qb.offset(offset).limit(limit).build();

        ArrayList<MessageRecord> messageRecords = (ArrayList<MessageRecord>) qb.list();
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
    public ArrayList<MessageRecord> selectionQueryFromFilter(LogFilter filter,int offset,int limit) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        ArrayList<MessageRecord> list = getAllMessageRecordsLimit(offset,limit);
            if(filter == null)
                return list;

            if(filter.getBelowTimestamp() == 0 || filter.getAboveTimestamp() == 0)
                return list;

        return filterTimestamp (recordDao,filter,offset,limit);
    }

    private ArrayList<MessageRecord> filterTimestamp(MessageRecordDao recordDao,LogFilter filter,int offset,int limit){
        ArrayList<MessageRecord> list = new ArrayList<>();

        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();
        qb.orderDesc(MessageRecordDao.Properties.Id);
        qb.and(MessageRecordDao.Properties.Timestamp.lt(filter.getBelowTimestamp()),
                MessageRecordDao.Properties.Timestamp.gt(filter.getAboveTimestamp()));
        qb.offset(offset).limit(limit);

        try {
            list = (ArrayList<MessageRecord>) qb.list();
        } catch (Exception e) {
            list = getAllMessageRecordsLimit(offset,limit);
            return list;
        }

        return list;
    }

    /**
     * Returns the query for the given filter.
     * @param filter (LogFilter)
     * @return QueryBuilder<AttackRecord> query
     */
    public ArrayList<MessageRecord> selectionQueryFromFilter(LogFilter filter) {
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        ArrayList<MessageRecord> list = this.getAllMessageRecords();

        if(filter == null)
            return list;

        if(filter.getBelowTimestamp() == 0 || filter.getAboveTimestamp() == 0)
            return list;
        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();

        qb.and(MessageRecordDao.Properties.Timestamp.lt(filter.getBelowTimestamp()),
                MessageRecordDao.Properties.Timestamp.gt(filter.getAboveTimestamp()));
        qb.orderDesc(MessageRecordDao.Properties.Id);
        try {
            list = (ArrayList<MessageRecord>) qb.list();
        } catch (Exception e) {
            QueryBuilder<MessageRecord> queryBuilder = recordDao.queryBuilder();
            queryBuilder.orderDesc(MessageRecordDao.Properties.Id);
            list =(ArrayList<MessageRecord>) queryBuilder.list();

            return list;
        }

        return list;
    }

    /**
     * Returns the query for the given filter.
     * @param filter (LogFilter)
     * @return QueryBuilder<AttackRecord> query
     */
    public ArrayList<MessageRecord> selectionQueryFromFilterMultiStage(LogFilter filter) {
        return filterList(filter);
    }

    private ArrayList<MessageRecord> filterList(LogFilter filter){
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        ArrayList<MessageRecord> list = new ArrayList<>();
        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();

        qb.and(MessageRecordDao.Properties.Timestamp.lt(filter.getBelowTimestamp()),
                MessageRecordDao.Properties.Timestamp.gt(filter.getAboveTimestamp()));
        try {
            list = (ArrayList<MessageRecord>) qb.list();
        } catch (Exception e) {
            return list;
        }

        return list;

    }

    public void deleteFromFilter(LogFilter filter){
        MessageRecordDao recordDao = this.daoSession.getMessageRecordDao();
        QueryBuilder<MessageRecord> qb = recordDao.queryBuilder();


        deleteElementsByCondition(recordDao,MessageRecordDao.Properties.Timestamp.lt(filter.getBelowTimestamp()),
                MessageRecordDao.Properties.Timestamp.gt(filter.getAboveTimestamp()));

    }


    }
