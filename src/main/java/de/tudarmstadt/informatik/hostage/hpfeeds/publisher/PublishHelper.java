package de.tudarmstadt.informatik.hostage.hpfeeds.publisher;

import java.io.IOException;
import java.util.ArrayList;

import de.tudarmstadt.informatik.hostage.HostageApplication;
import de.tudarmstadt.informatik.hostage.commons.JSONHelper;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.RecordAll;
import de.tudarmstadt.informatik.hostage.persistence.DAO.DAOHelper;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;

public class PublishHelper {
    private DaoSession dbSession;
    private DAOHelper daoHelper;
    private int offset=0;
    private int limit=20;
    private int attackRecordOffset=0;
    private int attackRecordLimit=999;
    LogFilter filter = null;
    JSONHelper jsonHelper = new JSONHelper();


    public PublishHelper(){
        this.dbSession = HostageApplication.getInstances().getDaoSession();
        this.daoHelper = new DAOHelper(dbSession);
    }

    /**
     * Uploads the records in the Hpfeeds broker.
     */
    public void uploadRecordHpfeeds(){
        persistRecord();
        try {
            publisher();
        } catch (Hpfeeds.ReadTimeOutException | Hpfeeds.EOSException | Hpfeeds.InvalidStateException | Hpfeeds.LargeMessageException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Persists the record in a JSON file.
     */
    private void persistRecord(){
        jsonHelper.persistData(getLastInsertedRecords());
    }

    /**
     * Publish the JSON file in the broker
     * @throws Hpfeeds.ReadTimeOutException ReadTimeOutException
     * @throws Hpfeeds.EOSException EOSException
     * @throws Hpfeeds.InvalidStateException InvalidStateException
     * @throws Hpfeeds.LargeMessageException LargeMessageException
     * @throws IOException IOException
     */
    private void publisher() throws Hpfeeds.ReadTimeOutException, Hpfeeds.EOSException, Hpfeeds.InvalidStateException, Hpfeeds.LargeMessageException, IOException {
        Publisher publisher = new Publisher();
        String initialConfigurationUrl = jsonHelper.getFilePath();

        publisher.setCommand("192.168.1.3",20000,"testing","secretkey","chan2",initialConfigurationUrl);

        publisher.publishFile();

    }

    /**
     * Returns an ArrayList of the last inserted records.
     * @return a list of the last inserted records from a current attack
     */
    private ArrayList<RecordAll> getLastInsertedRecords(){
        return  daoHelper.getAttackRecordDAO().getRecordsForFilter(filter,offset,limit,attackRecordOffset,attackRecordLimit);
    }
}
