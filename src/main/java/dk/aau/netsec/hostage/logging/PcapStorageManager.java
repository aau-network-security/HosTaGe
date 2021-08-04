package dk.aau.netsec.hostage.logging;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import dk.aau.netsec.hostage.system.PcapWriter;

/**
 * @author Filip Adamik
 * Created on 28/07/2021
 */
public class PcapStorageManager {

    private static final String PREF_PCAP_LOG_KEY = "pref_pcap_log_setting";
    private static final String PREF_PCAP_LOCATION_KEY = "pref_pcap_location_setting";

    public static int ACTION_PICK_FOLDER_AND_ENABLE = 6666;
    public static int ACTION_PICK_FOLDER = 6667;

    Uri mStorageLocation;

    static WeakReference<PcapStorageManager> pcapStorageManagerInstance;

    Context context;
    boolean pcapLogEnabled;

    private PcapStorageManager(Context context) {
        this.context = context;

        pcapStorageManagerInstance = new WeakReference<>(this);

    }

    @NonNull
    public static PcapStorageManager getPcapStorageManagerInstance(@NonNull Context context) {
        if (pcapStorageManagerInstance == null || pcapStorageManagerInstance.get() == null) {
            return new PcapStorageManager(context);
        } else {
            return pcapStorageManagerInstance.get();
        }
    }

    public boolean retrievePcapLogSetting() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        pcapLogEnabled = sharedPref.getBoolean(PREF_PCAP_LOG_KEY, false);

        return pcapLogEnabled;
    }

    void setPcapLogSetting(boolean pcapLogEnabled) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREF_PCAP_LOG_KEY, pcapLogEnabled);
        editor.apply();

        this.pcapLogEnabled = pcapLogEnabled;
    }

    public void enablePcapLogging(Fragment fragment) {

        retrieveStorageLocation();

        if (mStorageLocation == null || !pcapLocationWritable(mStorageLocation)) {

            // Choose a directory using the system's file picker.
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker when it loads.
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);

            fragment.startActivityForResult(intent, ACTION_PICK_FOLDER_AND_ENABLE);

        } else {

            setPcapLogSetting(true);
            startPcapLoggingForRealNow();
        }
    }

    public void selectLocation(Fragment fragment){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when it loads.
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);

        fragment.startActivityForResult(intent, ACTION_PICK_FOLDER);
    }

    public void locationSelected(Uri location, boolean enableLog) {
        if (pcapLocationWritable(location)) {
            context.getContentResolver().takePersistableUriPermission(location,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            setStorageLocation(location);

            if (enableLog) {
                setPcapLogSetting(true);

                startPcapLoggingForRealNow();
            }
        }

    }

    void startPcapLoggingForRealNow(){
        PcapWriter.PcapCapture pcapCapture = new PcapWriter.PcapCapture(mStorageLocation);
        pcapCapture.execute();
    }


    public void disablePcapLogging() {

        setPcapLogSetting(false);

        PcapWriter.stopTcpdumpPcap();
//        Toast.makeText(context, "BAAAAARRR", Toast.LENGTH_SHORT).show();
    }


    public boolean pcapLocationWritable(Uri locationUri) {

        DocumentFile dirFile = DocumentFile.fromTreeUri(context, locationUri);
        Log.d("filipko", "Can write is: " + dirFile.canWrite());

        return dirFile.canWrite();
    }

    public void writeTestFile(Uri locationUri){

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

    void retrieveStorageLocation() {
        if (mStorageLocation == null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String uriString = sharedPref.getString(PREF_PCAP_LOCATION_KEY, null);

            if (uriString != null) {

                Log.d("filipko", "Location retrieved: " + uriString);

                mStorageLocation = Uri.parse(uriString);
            }
        }

    }

    void setStorageLocation(Uri folderLocation) {
        mStorageLocation = folderLocation;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_PCAP_LOCATION_KEY, folderLocation.toString());
        editor.apply();
    }

    public String getStorageLocationPath(){
        retrieveStorageLocation();

        if (mStorageLocation != null){
            String filipsId = DocumentsContract.getTreeDocumentId(mStorageLocation);
            int colonPos = filipsId.indexOf(":");
            filipsId = filipsId.substring(colonPos + 1);

            return filipsId;
        }

        return null;
    }
}
