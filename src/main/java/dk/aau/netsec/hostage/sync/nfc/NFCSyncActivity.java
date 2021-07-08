/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.aau.netsec.hostage.sync.nfc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.nfc.tech.NfcF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.logging.NetworkRecord;
import dk.aau.netsec.hostage.logging.SyncInfoRecord;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.sync.tracing.TracingSyncService;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NFCSyncActivity extends Activity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback {
	NfcAdapter mNfcAdapter;
	TextView mInfoText;
	private static final int MESSAGE_SENT = 0x1;
	private static final int MESSAGE_RECEIVED = 0x3;
	

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_SENT:				
				runOnUiThread(new Runnable() {
					public void run() {
		            	Toast.makeText(NFCSyncActivity.this, "Data sent!", Toast.LENGTH_LONG).show();
					}
		        });
				break;
			case MESSAGE_RECEIVED:
				runOnUiThread(new Runnable() {
					public void run() {
		            	Toast.makeText(NFCSyncActivity.this, "Data received!", Toast.LENGTH_LONG).show();
					}
		        });				
				break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nfc);

		mInfoText = findViewById(R.id.nfc_text_view);
		// Check for available NFC Adapter
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter == null) {
			mInfoText.setText(R.string.nfc_not_available);
		} else if(!mNfcAdapter.isEnabled()){
			mInfoText.setText(R.string.nfc_enable_beam);
		} else {
			mInfoText.setText(R.string.nfc_hold_phones_together);
			// Register callback to set NDEF message
			mNfcAdapter.setNdefPushMessageCallback(this, this);
			// Register callback to listen for message-sent success
			mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
		}
	}

	/**
	 * Implementation for the CreateNdefMessageCallback interface
	 */
	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		// Get Networkdata
		//HostageDBOpenHelper dbh = new HostageDBOpenHelper(this);
		DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
		DAOHelper daoHelper = new DAOHelper(dbSession,this);
		ArrayList<NetworkRecord> localNetworkInformation = daoHelper.getNetworkRecordDAO().getNetworkInformation();
		HashMap<String, Long> devices_local = daoHelper.getSyncDeviceDAO().getSyncDeviceHashMap();
		ArrayList<SyncInfoRecord> syncInfo = daoHelper.getSyncInfoRecordDAO().getSyncInfo();
		
		NdefMessage msg = null;
		try {
			NdefRecord netData = NdefRecord.createMime("application/dk.aau.netsec.hostage.", serialize(localNetworkInformation));
			NdefRecord deviceData = NdefRecord.createMime("application/dk.aau.netsec.hostage.", serialize(devices_local));
			NdefRecord syncData = NdefRecord.createMime("application/dk.aau.netsec.hostage.", serialize(syncInfo));
			msg = new NdefMessage(netData, deviceData, syncData);
					
		} catch (IOException e) {
			e.printStackTrace();
		}
		return msg;
	}

	/**
	 * Implementation for the OnNdefPushCompleteCallback interface
	 */
	@Override
	public void onNdefPushComplete(NfcEvent arg0) {
		// A handler is needed to send messages to the activity when this
		// callback occurs, because it happens from a binder thread
		mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
	}

	@Override
	public void onNewIntent(Intent intent) {
		// onResume gets called after this to handle the intent
		setIntent(intent);
	}

	// HELPER

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter[] mIntentFilters = null;
			
	    PendingIntent mPendingIntent = PendingIntent.getActivity(this, 0, 
	    		new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
 
        // set an intent filter for all MIME data
        IntentFilter ndefIntent = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefIntent.addDataType("*/*");
            mIntentFilters = new IntentFilter[] { ndefIntent };
        } catch (Exception e) {
            Log.e("TagDispatch", e.toString());
        }
 
        String[][] mNFCTechLists = new String[][] { new String[] { NfcF.class.getName() } };
        
        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mIntentFilters, mNFCTechLists);
	
		// Check to see that the Activity started due to an Android Beam
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			processIntent(getIntent());
		}		
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (mNfcAdapter != null)
			mNfcAdapter.disableForegroundDispatch(this);
	}

	/**
	 * Parses the NDEF Message from the intent and prints to the TextView
	 */
	void processIntent(Intent intent) {
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		// only one message sent during the beam
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		try {
			DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
			DAOHelper daoHelper = new DAOHelper(dbSession,this);
			//HostageDBOpenHelper dbh = new HostageDBOpenHelper(this);

			ArrayList<NetworkRecord> remoteNetworkInformation = (ArrayList<NetworkRecord>) deserialize(msg.getRecords()[0].getPayload());
			HashMap<String, Long> devices_remote = (HashMap<String, Long>) deserialize(msg.getRecords()[1].getPayload());
			HashMap<String, Long> devices_local = daoHelper.getSyncDeviceDAO().getSyncDeviceHashMap();
			ArrayList<SyncInfoRecord> syncInfo = (ArrayList<SyncInfoRecord>) deserialize(msg.getRecords()[2].getPayload());
			
			long tracing_timestamp = 0;
			if(devices_local.containsKey(TracingSyncService.REMOTE_DEVICE))
				tracing_timestamp = devices_local.get(TracingSyncService.REMOTE_DEVICE);
				
			for(Iterator<String> i = devices_remote.keySet().iterator(); i.hasNext(); ){
				String key = i.next();
				if((devices_local.containsKey(key) && devices_local.get(key) >= devices_remote.get(key)) 
				    || (tracing_timestamp > devices_remote.get(key))){
					i.remove();
				}
			}
			
			for ( Iterator<SyncInfoRecord> i = syncInfo.iterator(); i.hasNext(); ){
				SyncInfoRecord info = i.next();
				if(!devices_remote.containsKey(info.getDeviceID())){
					i.remove();
				}				    
			}	
			
			daoHelper.getSyncDeviceDAO().updateSyncDevices(devices_remote);
			daoHelper.getSyncInfoRecordDAO().updateSyncInfo(syncInfo);
			daoHelper.getNetworkRecordDAO().updateNetworkInformation(remoteNetworkInformation);
			mHandler.obtainMessage(MESSAGE_RECEIVED).sendToTarget();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}

	private static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}

}
