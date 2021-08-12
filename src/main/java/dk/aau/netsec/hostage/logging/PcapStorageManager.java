package dk.aau.netsec.hostage.logging;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.lang.ref.WeakReference;

import dk.aau.netsec.hostage.services.PcapLoggingService;
import dk.aau.netsec.hostage.ui.fragment.SettingsFragment;

/**
 * TODO write javadoc
 *
 * @author Filip Adamik
 * Created on 28/07/2021
 */
public class PcapStorageManager {

    private Uri mStorageLocation;
    private Context context;
    private boolean pcapLogEnabled;
    private int logRotationPeriod;

    private static WeakReference<PcapStorageManager> pcapStorageManagerInstance;

    private static final String PREF_PCAP_LOG_KEY = "pref_pcap_log_setting";
    private static final String PREF_PCAP_LOCATION_KEY = "pref_pcap_location_setting";
    private static final String PREF_PCAP_LOG_ROTATION_KEY = "pref_pcap_log_rotation_setting";

    public static final int ACTION_PICK_FOLDER_AND_ENABLE = 6666;
    public static final int ACTION_PICK_FOLDER = 6667;

    public static final String TAG = "PCAP Storage Manager";

    /**
     * TODO write javadoc
     *
     * @param context
     */
    private PcapStorageManager(Context context) {
        this.context = context;

        pcapStorageManagerInstance = new WeakReference<>(this);

        retrievePcapLogState();
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

        return dirFile.canWrite();
    }

    /**
     * TODO write javadoc
     */
    private void startPcapLogging() {
        Intent intent = new Intent(context, PcapLoggingService.class);
//        TODO implement various log types

        intent.putExtra(PcapLoggingService.PCAP_SERVICE_INTENT_TYPE, PcapLoggingService.LOG_TYPE_PCAP);
        intent.putExtra(PcapLoggingService.PCAP_SERVICE_INTENT_URI, mStorageLocation.toString());
        intent.putExtra(PcapLoggingService.PCAP_SERVICE_INTENT_SECONDS, logRotationPeriod);

        context.startService(intent);
    }

    /**
     * TODO write javadoc
     */
    private void stopPcapLogging() {
        Intent intent = new Intent(context, PcapLoggingService.class);
        context.stopService(intent);
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
    private void retrievePcapLogState() {
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

                Log.d(TAG, "Location retrieved: " + uriString);

                mStorageLocation = Uri.parse(uriString);
            }
        }
    }

    /**
     * TODO write javadoc
     * @param period
     */
    private void setLogRotationPeriod(int period){
        logRotationPeriod = period;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(PREF_PCAP_LOG_ROTATION_KEY, period);
        editor.apply();
    }

    /**
     * TODO write javadoc
     */
    private void retrieveLogRotationPeriod(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        logRotationPeriod = sharedPref.getInt(PREF_PCAP_LOG_ROTATION_KEY, 10);
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
     * Deactivate PCAP logging
     */
    public void disablePcapLogging() {
        setPcapLogState(false);

        stopPcapLogging();
    }

    /**
     * Change output location, but do not change logging state afterwards.
     *
     * @param fragment context for folder picker action.
     */
    public void selectLocation(SettingsFragment fragment) {
        launchFolderPicker(fragment, false);
    }

    /**
     * Callback to indicate that the user has selected a folder and returned to the settings.
     *
     * @param location   Location of the folder selected by the user
     * @param enablePcap Whether <i>Enable logging</i> checkbox is ticked and logging should be
     *                   started immediately.
     */
    public void locationSelected(Uri location, boolean enablePcap) {
        if (pcapLocationWritable(location)) {

            //Persist location permission
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
     * @param period
     */
    public void logRotationPeriodSelected(int period){
        setLogRotationPeriod(period);
    }

    /**
     * Return current status of Pcap logging
     *
     * @return Whether pcap log is currently enabled.
     */
    public boolean isPcapLogEnabled() {
        return pcapLogEnabled;
    }

    /**
     * Return storage path specified by the user.
     *
     * @return Storage path in user-readable form
     */
    public String getStorageLocationPath() {
        retrieveStorageLocation();

        if (mStorageLocation != null) {
            String storagePath = DocumentsContract.getTreeDocumentId(mStorageLocation);
            int colonPos = storagePath.indexOf(":");
            storagePath = storagePath.substring(colonPos + 1);

            return storagePath;
        } else {
            return null;
        }
    }

    /**
     * TODO write javadoc
     * @return
     */
    public int getLogRotationPeriod(){
        retrieveLogRotationPeriod();

        return logRotationPeriod;
    }

//    //    ONLY FOR TESTING! (delete for release)
//    public void writeTestFile(Uri locationUri) {
//
//        DocumentFile dirFile = DocumentFile.fromTreeUri(context, locationUri);
//
//        DocumentFile file = dirFile.createFile("text/plain", "filipsFile");
//        Uri uri = file.getUri();
//
//
//        try {
//            ParcelFileDescriptor pfd = context.getContentResolver().
//                    openFileDescriptor(uri, "w");
//            FileOutputStream fileOutputStream =
//                    new FileOutputStream(pfd.getFileDescriptor());
//            fileOutputStream.write(("Overwritten at " + System.currentTimeMillis() +
//                    "\n").getBytes());
//            fileOutputStream.close();
//            pfd.close();
//
//        } catch (IOException fnfe) {
//            fnfe.printStackTrace();
//        }
//    }
}
