package de.tudarmstadt.informatik.hostage.sync.tracing;

import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.sync.android.SyncUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

/**
 * Starts a synchronization service and shows the progress of the synchronization.
 * @author Lars Pandikow
 */
@Deprecated
public class TracingSyncActivity extends Activity implements TracingSyncResultReciever.Receiver{
	
	TextView mInfoText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nfc);
		mInfoText = findViewById(R.id.nfc_text_view);
		mInfoText.setText("Synchronizing...");
		
		TracingSyncResultReciever mReceiver = new TracingSyncResultReciever(new Handler());
        mReceiver.setReceiver(this);
        
		Intent intent = new Intent(this, TracingSyncService.class);
		intent.setAction(TracingSyncService.ACTION_START_SYNC);
		intent.putExtra(TracingSyncService.EXTRA_RECEIVER, mReceiver);
		startService(intent);
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
        case TracingSyncService.SYNC_COMPLETE:
        	mInfoText.setText("Records were synchronized successfully!");
            setResult(SyncUtils.SYNC_SUCCESSFUL, null);
            break;
        case TracingSyncService.RECORD_UPLOADED:      
        	mInfoText.setText("Uploading Records... (" + resultData.getInt(TracingSyncService.UPLOAD_PROGRESS) + "/"+ resultData.getInt(TracingSyncService.UPLOAD_SIZE) + ")");
            break;
        case TracingSyncService.RECORD_DOWNLOAD:
            mInfoText.setText("Downloading Records...");
            break;
        case TracingSyncService.SYNC_DOWNLOAD_ERROR:
            mInfoText.setText("There was an error while trying to download records to TraCiNG");
            break;
        case TracingSyncService.SYNC_UPLOAD_ERROR:
            mInfoText.setText("There was an error while trying to upload records to TraCiNG");
            break;
        case TracingSyncService.SYNC_ERROR:
            mInfoText.setText("There was an error while trying to synchronize with TraCiNG.");
            break;
        }
	}	
	
}
