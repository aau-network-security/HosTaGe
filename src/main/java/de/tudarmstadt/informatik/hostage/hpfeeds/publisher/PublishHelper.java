package de.tudarmstadt.informatik.hostage.hpfeeds.publisher;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;

import java.io.File;
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
    private static final String PERSIST_FILENAME = "publish.json";
    File file = new File("/data/data/" + MainActivity.getContext().getPackageName() + "/" + PERSIST_FILENAME);
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
        jsonHelper.jsonWriter(this.persistData(getLastInsertedRecords()),file);
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
        String initialConfigurationUrl = jsonHelper.getFilePath(file);
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

    /**
     * Creates a JSON array.
     * @param records of the attacks
     * @return JSON array.
     */
    public JSONArray persistData(ArrayList<RecordAll> records){
        JSONArray arr = new JSONArray();
        for(RecordAll record: records) {
            arr.put(record.toJSON());
        }
        return arr;
    }

}
