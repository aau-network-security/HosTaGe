package dk.aau.netsec.hostage.sync.bluetooth;

import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.sync.android.SyncUtils;

/**
 * Activity that allows the user to choose a bluetooth device to 
 * synchronize with and informs about the status of the synchronization.
 *
 * @author Lars Pandikow
 */
public class BluetoothSyncActivity extends Activity{	
	
	public static final int CONNECTING = 0x0;
    public static final int CONNECTION_ESTABLISHED = 0x1;
    public static final int CONNECTION_FAILED = 0x2;
    public static final int SYNC_START = 0x3;
    public static final int SYNC_SUCCESSFUL = 0x4;
    public static final int SYNC_FAILED = 0x5;
    
    public static UUID serviceUUID;
	
	private BluetoothAdapter mBluetoothAdapter;
	private ArrayAdapter<String> arrayAdapter;

	private ServerThread serverThread;	
	private ClientThread clientThread;
	private CommunicationThread commThread;
	
	
	private TextView mInfoText;
	private ListView listView;
    private ProgressBar progressBar;


	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);

		serviceUUID = UUID.fromString(getResources().getString(R.string.UUID));
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();	
		arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_view_bluetooth_devices);	
		
		setLayoutElement();		
		registerBroadcastReceiver();
		
		if(savedInstanceState != null){
			CharSequence text = savedInstanceState.getCharSequence("mInfoText");
			mInfoText.setText(text); 		
		    String[] data = savedInstanceState.getStringArray("adapter");
		    if(data != null){
			    for(int i = 0; i < data.length; i++){
					arrayAdapter.add(data[i]);
				    arrayAdapter.notifyDataSetChanged();
			    }
		    }	
		    if(savedInstanceState.getBoolean("listView")){
		    	listView.setVisibility(View.VISIBLE);
		    }else{
		    	listView.setVisibility(View.GONE);
		    }
		}
							
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth
			mInfoText.setText("Bluetooth is not available on this device.");
		}
		else if (!mBluetoothAdapter.isEnabled()) {
			mInfoText.setText("Enable Bluetooth before synchronizing.");
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivity(enableBtIntent);
		} else if(savedInstanceState == null){
			startConnectionListener();
			chooseDevice();
		}
	}
		
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		if(mRecieverRegistered){
			unregisterBroadcastReceiver();
		}
		cancelThreads();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState){
		String[] data = new String[arrayAdapter.getCount()];
		for(int i = 0; i < arrayAdapter.getCount(); i++){
			data[i] = arrayAdapter.getItem(i);
		}
		outState.putStringArray("adapter", data);
		outState.putCharSequence("mInfoText", mInfoText.getText());
		outState.putBoolean("listView", listView.isShown());
		super.onSaveInstanceState(outState);
	}
	
	
	/**
	 * Starts discovery of bluetooth devices.
	 */
	private void chooseDevice(){
		if (!mBluetoothAdapter.startDiscovery())
			return;
		mInfoText.setText("Choose Device for synchronizing:\n");
		listView.setVisibility(View.VISIBLE);
	}

	/**
	 * Start a ServerThread to listen for incoming connections
	 * @see ServerThread
	 */
	private void startConnectionListener() {
		if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}

		serverThread = new ServerThread(mHandler, getResources().getString(R.string.app_name));
		serverThread.start();
	}
		
	/**
	 * Called when a connection has been established.
	 * Starts a {@link CommunicationThread} for communication.
	 * @param socket The socket of the connection.
	 */
	protected void manageConnectedSocket(BluetoothSocket socket) {
		mBluetoothAdapter.cancelDiscovery();
		unregisterBroadcastReceiver();
		
		listView.setVisibility(View.GONE);
		String deviceName = socket.getRemoteDevice().getName();
		mInfoText.setText("Synchronizing with " + deviceName + "...");	
		
		commThread = new CommunicationThread(this, socket, mHandler);
		commThread.start();
	}

	/**
	 * BroadcastReciever listens for state changes of bluetooth and discovery of new devices.
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// Add the name and address to an array adapter to show in a ListView
				arrayAdapter.add(device.getName() + "\n" + device.getAddress());
				arrayAdapter.notifyDataSetChanged();
			}else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
				if(state == BluetoothAdapter.STATE_ON){
					startConnectionListener();
					chooseDevice();
				}else if(state == BluetoothAdapter.STATE_OFF ||  state == BluetoothAdapter.STATE_TURNING_OFF){
					mInfoText.setText("Enable Bluetooth before synchronizing.");
					listView.setVisibility(View.GONE);
				}
			}
		}
	};
	
	private boolean mRecieverRegistered = false;


	/**
	 *  Register the BroadcastReceiver
	 */
	private void registerBroadcastReceiver() {
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter. ACTION_STATE_CHANGED);
		registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
		mRecieverRegistered = true;
	}
	
	/**
	 *  Unregister the BroadcastReceiver
	 */
	private void unregisterBroadcastReceiver(){
		unregisterReceiver(mReceiver);
		mRecieverRegistered = false;
	}
	
	/**
	 * Creates the list of bluetooth devices. 
	 * Starts a {@link ClientThread} to establish connection when a device is clicked.
	 */
	private void setLayoutElement(){
		mInfoText = findViewById(R.id.bluetoothInfoText);
        progressBar = findViewById(R.id.bluetoothProgressBar);

		listView = findViewById(R.id.bluetoothListView);
		listView.setAdapter(arrayAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String deviceInfo = arrayAdapter.getItem(position);
				String mac = deviceInfo.substring(deviceInfo.indexOf("\n") + 1);
				String name = deviceInfo.substring(0, deviceInfo.indexOf("\n"));
				mHandler.obtainMessage(CONNECTING, name).sendToTarget();
				clientThread = new ClientThread(mBluetoothAdapter.getRemoteDevice(mac), mHandler);
				clientThread.start();
			}
		});		
	}	
	
	private void cancelThreads(){
		if(commThread != null) {
			commThread.cancel();
		}
		if(clientThread != null){
			clientThread.cancel();
		}
		if(serverThread != null){
			serverThread.cancel();
		}
	}

    protected void setResultIntent(int result, boolean doFinish){
        Intent intent = new Intent();
        intent.putExtra("result", result);

        setResult(result, intent);

        if(doFinish) finish();
    }

	/**
	 * Handles message sent from the background threads and updates UI.
	 */
	private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
        	switch(msg.what){
        		case CONNECTING:       
        			listView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
        			mInfoText.setText("Connecting to " + msg.obj + "...");
        			break;
        		case CONNECTION_ESTABLISHED: 
        			BluetoothSocket socket = (BluetoothSocket) msg.obj;
        			manageConnectedSocket(socket);
        			break;
                case SYNC_START:
                    progressBar.setVisibility(View.VISIBLE);
                    mInfoText.setText("Synchronizing data...");
                    break;
        		case CONNECTION_FAILED: 
        			mInfoText.setText("Failed to connect to device!");
                    progressBar.setVisibility(View.GONE);

                    BluetoothSyncActivity.this.setResultIntent(SyncUtils.SYNC_SUCCESSFUL, false);

        			break;
        		case SYNC_SUCCESSFUL: 
        			mInfoText.setText("Synchronization successful!");
                    progressBar.setVisibility(View.GONE);

                    BluetoothSyncActivity.this.setResultIntent(SyncUtils.SYNC_SUCCESSFUL, true);

                    break;
        		case SYNC_FAILED: 
        			commThread.cancel();
        			mInfoText.setText("Synchronization failed!");
                    progressBar.setVisibility(View.GONE);

                    BluetoothSyncActivity.this.setResultIntent(SyncUtils.SYNC_SUCCESSFUL, false);
                    break;
        	}        		
        }
	};
	
	
}
