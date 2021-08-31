package dk.aau.netsec.hostage.logging;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.lang.ref.WeakReference;

import dk.aau.netsec.hostage.services.PcapLoggingService;
import dk.aau.netsec.hostage.ui.fragment.SettingsFragment;

/**
 * This class manages the operations of a PCAP Logging Service. It maintains the state of user's
 * settings (via Shared Preferences) and starts and stops the service.
 *
 * This class is a singleton and at most one instance should exist at all times.
 *
 * @author Filip Adamik
 * Created on 28/07/2021
 */
public class PcapLoggingManager {

    private Context context;
    private Uri mOutputLocation;
    private boolean mPcapLogEnabled;
    private int mLogRotationPeriod;

    private static WeakReference<PcapLoggingManager> pcapManagerInstance;

    private static final String PREF_PCAP_LOG_KEY = "pref_pcap_log_setting";
    private static final String PREF_PCAP_LOCATION_KEY = "pref_pcap_location_setting";
    private static final String PREF_PCAP_LOG_ROTATION_KEY = "pref_pcap_log_rotation_setting";

    public static final int[] PCAP_LOG_DURATION_OPTIONS = {10, 30, 60, 90, 180};

    public static final int ACTION_PICK_FOLDER_AND_ENABLE = 6666;
    public static final int ACTION_PICK_FOLDER = 6667;

    public static final String TAG = "PCAP Logging Manager";

    /**
     * Private mandatory constructor.
     * <p>
     * getPcapLoggingManagerInstance should be used to obtain an instance of logging manager, to
     * ensure there is only a sinle instance active at any given time.
     *
     * @param context
     */
    private PcapLoggingManager(Context context) {
        pcapManagerInstance = new WeakReference<>(this);
        this.context = context;

        retrieveOutputLocation();
        retrieveLogRotationPeriod();
        retrievePcapLogState();

        // Set this line if PCAP logs should not restart automatically
//        mPcapLogEnabled = false;

        if (mPcapLogEnabled && pcapLocationWritable(mOutputLocation)){
            startPcapLogging();
        }
    }

    /**
     * Return the instance of PcapLogging manger, if it exists or create and return a new one.
     * <p>
     * Always provide fresh context to the instance before returning it.
     *
     * @param context Context to provide to the instance.
     * @return Instance of PcapLoggingManager
     */
    @NonNull
    public static PcapLoggingManager getPcapLoggingManagerInstance(@NonNull Context context) {
        PcapLoggingManager instance = null;

        if (pcapManagerInstance != null) {
            instance = pcapManagerInstance.get();
            instance.context = context;
        }

        if (instance == null) {
            instance = new PcapLoggingManager(context);
        }

        return instance;
    }

    /**
     * Start PCAP capture by launching a service. The service must be provided with output location,
     * and log rotation period in seconds.
     */
    private void startPcapLogging() {
        Intent intent = new Intent(context, PcapLoggingService.class);
        // TODO implement various log types

        intent.putExtra(PcapLoggingService.PCAP_SERVICE_INTENT_TYPE, PcapLoggingService.LOG_TYPE_PCAP);
        intent.putExtra(PcapLoggingService.PCAP_SERVICE_INTENT_URI, mOutputLocation.toString());
        intent.putExtra(PcapLoggingService.PCAP_SERVICE_INTENT_SECONDS, mLogRotationPeriod);

        context.startService(intent);
    }

    /**
     * Stop the {@link PcapLoggingService} and stop capturing PCAP logs.
     */
    private void stopPcapLogging() {
        Intent intent = new Intent(context, PcapLoggingService.class);
        context.stopService(intent);
    }

    /**
     * Launch a folder picker activity. The user is prompted to select and output folder where the
     * PCAP logs will be written to.
     *
     * @param fragment         fragment to launch the picker from and return back to
     * @param enableAfterwards Speficies whether the PCAP logging should be enabled after the user
     *                         has selected the output folder.
     */
    private void launchFolderPicker(SettingsFragment fragment, boolean enableAfterwards) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        int request_code = enableAfterwards ? ACTION_PICK_FOLDER_AND_ENABLE : ACTION_PICK_FOLDER;

        fragment.startActivityForResult(intent, request_code);
    }

    /**
     * Verify that the specified location is writable.
     *
     * @param locationUri Output location Uri
     * @return True if location is writable
     */
    private boolean pcapLocationWritable(Uri locationUri) {
        DocumentFile dirFile = DocumentFile.fromTreeUri(context, locationUri);

        return dirFile.canWrite();
    }

    /**
     * Set whether PCAP logging is activated or not and save it to Shared Preferences
     *
     * @param pcapLogState
     */
    private void setPcapLogState(boolean pcapLogState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREF_PCAP_LOG_KEY, pcapLogState);
        editor.apply();

        this.mPcapLogEnabled = pcapLogState;
    }

    /**
     * Retrieve the
     */
    private void retrievePcapLogState() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        mPcapLogEnabled = sharedPref.getBoolean(PREF_PCAP_LOG_KEY, false);
    }

    /**
     * Set output location and save it to Shared Preferences.
     *
     * @param folderLocation Uri of the desired log output location
     */
    private void setOutputLocation(Uri folderLocation) {
        mOutputLocation = folderLocation;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_PCAP_LOCATION_KEY, folderLocation.toString());
        editor.apply();
    }

    /**
     * Retrieve log output location from Shared Preferences
     */
    private void retrieveOutputLocation() {
        if (mOutputLocation == null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String uriString = sharedPref.getString(PREF_PCAP_LOCATION_KEY, null);

            if (uriString != null) {
                mOutputLocation = Uri.parse(uriString);
            }
        }
    }

    /**
     * Set log rotation period and save it to Shared Preferences
     *
     * @param period log rotation period in seconds
     */
    private void setLogRotationPeriod(int period) {
        mLogRotationPeriod = period;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(PREF_PCAP_LOG_ROTATION_KEY, period);
        editor.apply();
    }

    /**
     * Retrieve log rotation period from Shared Preferences
     */
    private void retrieveLogRotationPeriod() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        mLogRotationPeriod = sharedPref.getInt(PREF_PCAP_LOG_ROTATION_KEY, 180);
    }

    /**
     * Activate PCAP logging. If output location is null or is not writable, prompt the user to
     * select an output location first.
     *
     * @param fragment Settings fragment instance where the user triggerred the action.
     */
    public void enablePcapLogging(SettingsFragment fragment) {
        if (mOutputLocation == null || !pcapLocationWritable(mOutputLocation)) {
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
     * <p>
     * If the folder picker was launched as a result of user activating PCAP logging without a writable
     * location available, set the location and start the PCAP logging.
     * <p>
     * If the folder picker was launched by user clicking on Location selection setting, then update
     * the location and restart the logging (if it is currently running).
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

            setOutputLocation(location);

            if (!mPcapLogEnabled) {
                if (enablePcap) {
                    setPcapLogState(true);
                    startPcapLogging();
                }
            } else {
                stopPcapLogging();
                startPcapLogging();
            }
        }
    }

    /**
     * Public method to provide Log Rotation period to PcapLoggingManager.
     *
     * @param period Log rotation period in seconds
     */
    public void logRotationPeriodSelected(int period) {
        setLogRotationPeriod(period);
    }

    /**
     * Return current status of Pcap logging
     *
     * @return Whether pcap log is currently enabled.
     */
    public boolean isPcapLogEnabled() {
        return mPcapLogEnabled;
    }

    /**
     * Return output path specified by the user. If the output path is not in memory, attempt to
     * retrieve it from shared preferences
     *
     * @return Output path in user-readable form
     */
    public String getOutputLocationPath() {
        retrieveOutputLocation();

        if (mOutputLocation != null) {
            String outputPath = DocumentsContract.getTreeDocumentId(mOutputLocation);
            int colonPos = outputPath.indexOf(":");
            outputPath = outputPath.substring(colonPos + 1);

            return outputPath;
        } else {
            return null;
        }
    }

    /**
     * Provide the currently set log rotation period.
     *
     * @return Log rotation period in seconds
     */
    public int getLogRotationPeriod() {
        retrieveLogRotationPeriod();

        return mLogRotationPeriod;
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
