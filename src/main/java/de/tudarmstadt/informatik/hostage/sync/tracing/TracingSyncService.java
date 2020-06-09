package de.tudarmstadt.informatik.hostage.sync.tracing;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.http.client.HttpClient;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;

import de.tudarmstadt.informatik.hostage.logging.Record;
import de.tudarmstadt.informatik.hostage.logging.SyncData;
import de.tudarmstadt.informatik.hostage.logging.SyncRecord;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBOpenHelper;
import de.tudarmstadt.informatik.hostage.sync.Synchronizer;
import de.tudarmstadt.informatik.hostage.sync.android.SyncUtils;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;


/**
 * Service that synchronizes with a specified remote server.
 * 
 * @author Lars Pandikow
 */
public class TracingSyncService extends IntentService {

	public static final String REMOTE_DEVICE = "de.tudarmstadt.informatik.hostage.REMOTE_DEVICE";

	public static final String ACTION_START_SYNC = "de.tudarmstadt.informatik.hostage.ACTION_START_SYNC";
	public static final String EXTRA_RECEIVER = "de.tudarmstadt.informatik.hostage.EXTRA_HANDLER";

	public static final String UPLOAD_SIZE = "de.tudarmstadt.informatik.hostage.UPLOAD_SIZE";
	public static final String UPLOAD_PROGRESS = "de.tudarmstadt.informatik.hostage.UPLOAD_PROGRESS";

	public static final int RECORD_UPLOADED = 0x00;
    public static final int RECORD_DOWNLOAD = 0x01;
	public static final int SYNC_COMPLETE = 0x02;
    public static final int SYNC_ERROR = 0x03;
    public static final int SYNC_UPLOAD_ERROR = 0x04;
    public static final int SYNC_DOWNLOAD_ERROR = 0x05;

    private HttpClient httpClient;
	private ResultReceiver receiver;

	HostageDBOpenHelper dbh;
    Synchronizer synchronizer;

	SharedPreferences pref;
	Editor editor;

	public TracingSyncService() {
		super(TracingSyncService.class.getName());

	}

	@Override
	public void onCreate() {
		super.onCreate();
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		editor = pref.edit();
		dbh = new HostageDBOpenHelper(this);
        synchronizer = new Synchronizer(dbh);
	}

	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns,
	 * IntentService stops the service, as appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();
			if (ACTION_START_SYNC.equals(action)) {
				receiver = intent.getParcelableExtra(EXTRA_RECEIVER);
				syncNewRecords();
			}

		}
	}

	/**
	 * Uploads all new Records to a server, specified in the settings.
	 */
	private void syncNewRecords() {
        long lastSyncTime = pref.getLong("LAST_SYNC_TIME", 0);


        // Then upload to tracing
        String serverAddress = pref.getString("pref_upload_server", "http://www.tracingmonitor.org"); //"https://192.168.1.118:9999"

        LogFilter filter = new LogFilter();
        filter.setAboveTimestamp(lastSyncTime);

        ArrayList<Record> records = dbh.getRecordsForFilter(filter);
        StringWriter writer = new StringWriter();

        int size = records.size();
        int offset = 1;
        int currOffset = 1;

        boolean error = false;
        for (Record record : records) {
            SyncUtils.appendRecordToStringWriter(record, writer);

            if(currOffset == 100 || offset == size){
                boolean success = SyncUtils.uploadRecordsToServer(writer.toString(), serverAddress);
                Log.i("Tracing upload", "Upload of record: " + offset + "/" + size + ((success) ? " successful." : " failed."));

                if(!success){
                    if(receiver != null){
                        receiver.send(SYNC_UPLOAD_ERROR, null);
                        error = true;
                    }
                    break;
                }

                if (receiver != null) {
                    Bundle data = new Bundle();
                    data.putInt(UPLOAD_SIZE, size);
                    data.putInt(UPLOAD_PROGRESS, offset);
                    receiver.send(RECORD_UPLOADED, data);
                }

                writer.getBuffer().setLength(0);
                currOffset = 0;
            }

            offset++;
            currOffset++;
        }

        // First download from tracing
        if(receiver != null) receiver.send(RECORD_DOWNLOAD, null);
        SyncData syncDataFromTracing = SyncUtils.getSyncDataFromTracing(this, synchronizer, lastSyncTime);

        if(syncDataFromTracing == null && error){
           if(receiver != null) receiver.send(SYNC_ERROR, null);
            return;
        } else if(syncDataFromTracing == null){
            if(receiver != null) receiver.send(SYNC_DOWNLOAD_ERROR, null);
            return;
        }

        HashSet<String> devices = new HashSet<String>();
        for(SyncRecord s: syncDataFromTracing.syncRecords){
            devices.add(s.getDevice());
        }

        synchronizer.updateNewDevices(new ArrayList<String>(devices));
        synchronizer.updateFromSyncData(syncDataFromTracing);

        if(!error) {
            pref.edit().putLong("LAST_SYNC_TIME", System.currentTimeMillis()).apply();
            if (receiver != null) {
                receiver.send(SYNC_COMPLETE, null);
            }
        }
    }
}
