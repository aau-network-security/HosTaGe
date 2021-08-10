package dk.aau.netsec.hostage.logging;

import static dk.aau.netsec.hostage.system.iptablesUtils.Api.runCommand;
import static dk.aau.netsec.hostage.system.iptablesUtils.Api.runCommandWithHandle;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.loader.content.AsyncTaskLoader;
import androidx.preference.PreferenceManager;

import org.apache.http.client.fluent.Async;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import dk.aau.netsec.hostage.system.PcapCapture;
import dk.aau.netsec.hostage.ui.fragment.SettingsFragment;

/**
 * TODO write javadoc
 *
 * @author Filip Adamik
 * Created on 28/07/2021
 */
public class PcapStorageManager {

    public static int ACTION_PICK_FOLDER_AND_ENABLE = 6666;
    public static int ACTION_PICK_FOLDER = 6667;

    private static final String PREF_PCAP_LOG_KEY = "pref_pcap_log_setting";
    private static final String PREF_PCAP_LOCATION_KEY = "pref_pcap_location_setting";

    private Uri mStorageLocation;
    private PcapCapture pcapCapture;

    private static WeakReference<PcapStorageManager> pcapStorageManagerInstance;

    Context context;
    boolean pcapLogEnabled;

    /**
     * TODO write javadoc
     *
     * @param context
     */
    private PcapStorageManager(Context context) {
        this.context = context;

        pcapStorageManagerInstance = new WeakReference<>(this);

        retrievePcapLogEnabled();
        retrieveStorageLocation();
    }

    /**
     * TODO write javadoc
     *
     * @param context
     * @return
     */
    @NonNull
    public static PcapStorageManager getPcapStorageManagerInstance(@NonNull Context context) {
        if (pcapStorageManagerInstance == null || pcapStorageManagerInstance.get() == null) {
            return new PcapStorageManager(context);
        } else {
            return pcapStorageManagerInstance.get();
        }
    }


    /**
     * TODO write javadoc
     *
     * @param fragment
     * @param enableAfterwards
     */
    private void launchFolderPicker(SettingsFragment fragment, boolean enableAfterwards) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        int request_code = enableAfterwards ? ACTION_PICK_FOLDER_AND_ENABLE : ACTION_PICK_FOLDER;

        fragment.startActivityForResult(intent, request_code);
    }

    /**
     * TODO write javadoc
     *
     * @param locationUri
     * @return
     */
    private boolean pcapLocationWritable(Uri locationUri) {

        DocumentFile dirFile = DocumentFile.fromTreeUri(context, locationUri);
        Log.d("filipko", "Can write is: " + dirFile.canWrite());

        return dirFile.canWrite();
    }

    /**
     * TODO write javadoc
     */
    private void startPcapLogging() {
        pcapCapture = new PcapCapture(context, mStorageLocation);
        pcapCapture.execute();
    }

    /**
     * TODO write javadoc
     */
    private void stopPcapLogging() {
        if (pcapCapture != null) {
            pcapCapture.stopTcpdumpPcap();
        }
    }

    /**
     * TODO write javadoc
     *
     * @param pcapLogState
     */
    private void setPcapLogState(boolean pcapLogState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREF_PCAP_LOG_KEY, pcapLogState);
        editor.apply();

        this.pcapLogEnabled = pcapLogState;
    }

    /**
     * TODO write javadoc
     */
    private void retrievePcapLogEnabled() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        pcapLogEnabled = sharedPref.getBoolean(PREF_PCAP_LOG_KEY, false);
    }

    /**
     * TODO write javadoc
     *
     * @param folderLocation
     */
    private void setStorageLocation(Uri folderLocation) {
        mStorageLocation = folderLocation;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_PCAP_LOCATION_KEY, folderLocation.toString());
        editor.apply();
    }

    /**
     * TODO write javadoc
     */
    private void retrieveStorageLocation() {
        if (mStorageLocation == null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String uriString = sharedPref.getString(PREF_PCAP_LOCATION_KEY, null);

            if (uriString != null) {

                Log.d("filipko", "Location retrieved: " + uriString);

                mStorageLocation = Uri.parse(uriString);
            }
        }
    }

    /**
     * TODO write javadoc
     *
     * @param fragment
     */
    public void enablePcapLogging(SettingsFragment fragment) {

        if (mStorageLocation == null || !pcapLocationWritable(mStorageLocation)) {
            launchFolderPicker(fragment, true);

        } else {
            setPcapLogState(true);
            startPcapLogging();

            fragment.setPcapChecked();
        }
    }

    /**
     * TODO write javadoc
     *
     * @param fragment
     */
    public void selectLocation(SettingsFragment fragment) {
        launchFolderPicker(fragment, false);
    }

    /**
     * TODO write javadoc
     *
     * @param location
     * @param enablePcap
     */
    public void locationSelected(Uri location, boolean enablePcap) {
        if (pcapLocationWritable(location)) {
            context.getContentResolver().takePersistableUriPermission(location,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            setStorageLocation(location);

            if (enablePcap) {
                setPcapLogState(true);

                startPcapLogging();
            }
        }
    }

    /**
     * TODO write javadoc
     */
    public void disablePcapLogging() {
        setPcapLogState(false);

        stopPcapLogging();
    }

    /**
     * TODO write javadoc
     *
     * @return
     */
    public boolean isPcapLogEnabled() {
        return pcapLogEnabled;
    }

    /**
     * TODO write javadoc
     *
     * @return
     */
    public String getStorageLocationPath() {
        retrieveStorageLocation();

        if (mStorageLocation != null) {
            String filipsId = DocumentsContract.getTreeDocumentId(mStorageLocation);
            int colonPos = filipsId.indexOf(":");
            filipsId = filipsId.substring(colonPos + 1);

            return filipsId;
        }

        return null;
    }

    //    ONLY FOR TESTING! (delete for release)
    public void writeTestFile(Uri locationUri) {

        DocumentFile dirFile = DocumentFile.fromTreeUri(context, locationUri);

        DocumentFile file = dirFile.createFile("text/plain", "filipsFile");
        Uri uri = file.getUri();


        try {
            ParcelFileDescriptor pfd = context.getContentResolver().
                    openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());
            fileOutputStream.write(("Overwritten at " + System.currentTimeMillis() +
                    "\n").getBytes());
            fileOutputStream.close();
            pfd.close();

        } catch (IOException fnfe) {
            fnfe.printStackTrace();
        }


//        String otherId = DocumentsContract.getTreeDocumentId(locationUri);
//
//        String filipsId = DocumentsContract.getDocumentId(uri);
//        int colonPos = filipsId.indexOf(":");
//        filipsId = filipsId.substring(colonPos + 1);
//
//        Log.d("filipko", otherId);


    }
}
