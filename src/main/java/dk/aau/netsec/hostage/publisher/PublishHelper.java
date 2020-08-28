package dk.aau.netsec.hostage.publisher;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.commons.JSONHelper;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.logging.RecordAll;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.ui.activity.MainActivity;
import dk.aau.netsec.hostage.ui.model.LogFilter;

public class PublishHelper {

    private DaoSession dbSession;
    private DAOHelper daoHelper;
    private int offset=0;
    private int limit=20;
    private int attackRecordOffset=0;
    private int attackRecordLimit=999;
    LogFilter filter = null;
    JSONHelper jsonHelper = new JSONHelper();
    private String host = "";
    private int port = 20000;
    private String ident = "";
    private String secret = "";
    private String channel = "";


    public PublishHelper(){
        this.dbSession = HostageApplication.getInstances().getDaoSession();
        this.daoHelper = new DAOHelper(dbSession);
       //initializeHpFeedsCredentials(); //hpfeeds disabled
    }

    private void initializeHpFeedsCredentials(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext());
        this.host = preferences.getString("pref_host_hpfeeds", "130.226.249.235");
        this.port = Integer.parseInt(preferences.getString("pref_port_hpfeeds", "20000"));
        this.ident = preferences.getString("pref_ident_hpfeeds", "irinil");
        this.secret = preferences.getString("pref_secret_hpfeeds", "gsoc2020");
        this.channel = preferences.getString("pref_secret_channel", "hostage");
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
        jsonHelper.jsonWriter(getLastInsertedRecords());
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
        publisher.setCommand(host,port,ident,secret,channel,initialConfigurationUrl);
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
