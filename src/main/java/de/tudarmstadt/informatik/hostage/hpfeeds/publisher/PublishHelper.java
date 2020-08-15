package de.tudarmstadt.informatik.hostage.hpfeeds.publisher;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;

import de.tudarmstadt.informatik.hostage.HostageApplication;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.JSONHelper;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.RecordAll;
import de.tudarmstadt.informatik.hostage.persistence.DAO.DAOHelper;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
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
    private String host = "";
    private int port = 0;
    private String ident = "";
    private String secret = "";
    private String channel = "";


    public PublishHelper(){
        this.dbSession = HostageApplication.getInstances().getDaoSession();
        this.daoHelper = new DAOHelper(dbSession);
        initializeHpFeedsCredentials();
    }

    private void initializeHpFeedsCredentials(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext());
        this.host = preferences.getString("pref_host_hpfeeds", String.valueOf(R.string.hpfeeds_host));
        this.port = Integer.parseInt(preferences.getString("pref_port_hpfeeds", String.valueOf(R.integer.hpfeeds_port)));
        this.ident = preferences.getString("pref_ident_hpfeeds", String.valueOf(R.string.hpfeeds_ident));
        this.secret = preferences.getString("pref_secret_hpfeeds", String.valueOf(R.string.hpfeeds_secret));
        this.channel = preferences.getString("pref_secret_channel", String.valueOf(R.string.hpfeeds_channel));
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
