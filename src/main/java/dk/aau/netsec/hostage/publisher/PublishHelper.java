package dk.aau.netsec.hostage.publisher;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.io.File;
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

    private final DAOHelper daoHelper;
    final LogFilter filter = null;
    final JSONHelper jsonHelper = new JSONHelper();
    private String host = "130.225.57.113";
    private int port = 20000;
    private String ident = "hostage";
    private String secret = "xbI6272cUN6-cVzpUPElVKHXJlTG3aZ!";
    private String channel = "hostage";

    private static final String PERSIST_FILENAME = "publish.json";
    final File hpfeedsFile = new File("/data/data/" + MainActivity.getContext().getPackageName() + "/" + PERSIST_FILENAME);

    public PublishHelper(){
        DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
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
        jsonHelper.jsonWriter(getLastInsertedRecords(),hpfeedsFile);
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
        String initialConfigurationUrl = jsonHelper.getFilePath(hpfeedsFile);
        publisher.setCommand(host,port,ident,secret,channel,initialConfigurationUrl);
        publisher.publishFile();
    }

    /**
     * Returns an ArrayList of the last inserted records.
     * @return a list of the last inserted records from a current attack
     */
    private ArrayList<RecordAll> getLastInsertedRecords(){
        int attackRecordLimit = 999;
        int attackRecordOffset = 0;
        int limit = 20;
        int offset = 0;
        return  daoHelper.getAttackRecordDAO().getRecordsForFilter(filter, offset, limit, attackRecordOffset, attackRecordLimit);
    }

}
