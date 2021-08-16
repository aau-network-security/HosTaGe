package dk.aau.netsec.hostage.sync.wifi_direct.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.sync.wifi_direct.BackgroundTask;
import dk.aau.netsec.hostage.sync.wifi_direct.WiFiP2pEventHandler;
import dk.aau.netsec.hostage.sync.wifi_direct.sync_tasks.SyncClientTask;
import dk.aau.netsec.hostage.sync.wifi_direct.sync_tasks.SyncHostTask;
import dk.aau.netsec.hostage.ui.activity.MainActivity;


/**
 * Created by Julien on 14.01.2015.
 */
public class WiFiP2pSyncActivity extends Activity implements AdapterView.OnItemClickListener {

    private SyncClientTask clientTask;
    private SyncHostTask hostTask;
    private BackgroundTask executingTask;
    private boolean isHost;

    private WiFiP2pEventHandler _wifiEventHandler = null;

    private WiFiP2pEventHandler.WiFiP2pEventListener _p2pEventListener = null;
    private BackgroundTask.BackgroundTaskCompletionListener _syncCompletionListener = null;

    private TextView mTxtP2PDeviceName;
    private TextView mTxtP2PDeviceStatus;
    private ViewAnimator mViewAnimator;
    private RelativeLayout mDevicesContainer;
    private TextView mTxtP2PSearchProgress;
    private ListView mLstP2PDevices;
    private RelativeLayout mWelcomeContainer;
    private TextView mTxtP2PNotAvailable;
    private TextView mTxtP2PChangeDeviceName;

    private WifiP2pDevice ownDevice;
    private WifiP2pDevice mOtherDevice;

    private ArrayList<WifiP2pDevice> discoveredDevices = new ArrayList<WifiP2pDevice>();

    private ProgressDialog progressDialog;

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean isHost) {
        this.isHost = isHost;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.wifiEventHandler();

        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        this.progressDialog.setIndeterminate(true);
        this.progressDialog.setTitle(getString(R.string.PROGRESS_TITLE_SYNC));

        setContentView(R.layout.activity_p2_psync);

        assert getActionBar() != null;
        getActionBar().setTitle(getString(R.string.ACTIONBAR_TITLE));
        getActionBar().setDisplayHomeAsUpEnabled(true);

        this.extractFromView();
        this.registerListeners();

        this.wifiEventHandler().startService();

        int seconds = 25;

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        searchForDevices();
                    }
                });
            }
        }, 1000, seconds * 1000); // search for devices every few seconds
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.wifiEventHandler().startService();
    }

    @Override
    public void onPause() {
        if (clientTask != null) clientTask.interrupt(true);
        if (hostTask != null) hostTask.interrupt(true);

        wifiEventHandler().disconnect();
        wifiEventHandler().stopService();
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        wifiEventHandler().startService();
    }

    @Override
    protected void onStop() {
        if (this.clientTask != null) this.clientTask.interrupt(true);
        if (this.hostTask != null) this.hostTask.interrupt(true);
        wifiEventHandler().disconnect();
        wifiEventHandler().stopService();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (clientTask != null) clientTask.interrupt(true);
        if (hostTask != null) hostTask.interrupt(true);
        wifiEventHandler().stopService();
        super.onDestroy();
    }

    /**
     * Returns a instance of the wifi event handler listener object. If no private instance was initiated it creates a new one.
     * This object handles all gui changes.
     *
     * @return WiFiP2pEventHandler.WiFiP2pEventListener
     */
    private WiFiP2pEventHandler.WiFiP2pEventListener eventListener() {
        if (_p2pEventListener == null) {
            _p2pEventListener = new WiFiP2pEventHandler.WiFiP2pEventListener() {
                WiFiP2pSyncActivity activity = null;

                public WiFiP2pEventHandler.WiFiP2pEventListener init(WiFiP2pSyncActivity act) {
                    activity = act;
                    return this;
                }

                @Override
                public void discoveredDevices(List<WifiP2pDevice> peers) {
                    Log.d("DEBUG_WiFiP2p", "Activity - Actualise devices list");
                    activity.updateDeviceListView(peers);
                }

                @Override
                public void wifiP2pIsEnabled(boolean enabled) {
                    String tmp = enabled ? "enabled" : "disabled";
                    Log.d("DEBUG_WiFiP2p", "Activity - Peer to peer is " + tmp + ".");
                    activity.setWifiDirectAvailable(enabled);
                }

                @Override
                public void didConnect(boolean isHost, WifiP2pInfo connectionInfo) {
                    Log.d("DEBUG_WiFiP2p", "Activity - Did connect");

                    activity.progressDialog.setMessage(getString(R.string.PROGRESS_TITLE_LOADING));
                    if (!activity.progressDialog.isShowing()) {
                        activity.progressDialog.show();
                    }

                    activity.setHost(isHost);
                    if (isHost) {
                        Log.d("DEBUG_WiFiP2p", "Activity - Connected as HOST");
                        activity.startHost();
                    } else {
                        Log.d("DEBUG_WiFiP2p", "Activity - Connected as Client");
                        activity.startClient(connectionInfo);
                    }
                }

                @Override
                public void failedToConnect() {
                    Log.d("DEBUG_WiFiP2p", "Activity - Failed to connect");
                    Toast.makeText(activity, getString(R.string.COULD_NOT_CONNECT_MESSAGE), Toast.LENGTH_LONG).show();
                    if (activity.progressDialog != null) {
                        activity.progressDialog.dismiss();
                    }
                }

                @Override
                public void didDisconnect() {
                    Log.d("DEBUG_WiFiP2p", "Activity - Did disconnect");
                    if (activity.progressDialog != null) {
                        activity.progressDialog.dismiss();
                    }
                }

                @Override
                public void failedToDisconnect() {
                    Log.d("DEBUG_WiFiP2p", "Activity - Failed to disconnect");
                    //Toast.makeText(this.activity, "Could not disconnect with device. Retry.", Toast.LENGTH_LONG).show();
                    // Other device did disconnect a while before.
                    if (activity.progressDialog != null &&
                            ((activity.hostTask == null && activity.clientTask == null) ||
                                    (activity.clientTask != null && activity.clientTask.isInterrupted()) ||
                                    (activity.hostTask != null && activity.hostTask.isInterrupted()) ||
                                    (activity.ownDevice == null && activity.ownDevice.status != WifiP2pDevice.CONNECTED)
                            )) {
                        activity.progressDialog.dismiss();
                    }
                }

                @Override
                public void ownDeviceInformationIsUpdated(WifiP2pDevice device) {
                    Log.d("DEBUG_WiFiP2p", "Activity - Updated device " + device.deviceName + " " + device.deviceAddress + ".");
                    activity.updateOwnDeviceInformation(device);
                    activity.searchForDevices();
                }

                @Override
                public void onConnectionLost() {
                    Toast.makeText(activity, R.string.CONNECTION_LOST_MESSAGE, Toast.LENGTH_LONG).show();
                    if (activity.progressDialog != null && activity.progressDialog.isShowing()) {
                        activity.progressDialog.dismiss();
                    }
                }
            }.init(this);
        }

        return _p2pEventListener;
    }

    /**
     * Returns a instance of the wifi event handler. If no private instance was initiated it creates a new one.
     *
     * @return WiFiP2pEventHandler
     */
    private WiFiP2pEventHandler wifiEventHandler() {
        if (_wifiEventHandler == null) {
            _wifiEventHandler = new WiFiP2pEventHandler(this, eventListener());
        }
        return _wifiEventHandler;
    }


    /**
     * Returns a sync completion listener. If no listener was initiated it creates a new on.
     *
     * @return BackgroundTaskCompletionListener
     */
    private BackgroundTask.BackgroundTaskCompletionListener syncCompletionListener() {
        if (_syncCompletionListener == null) {
            _syncCompletionListener = new BackgroundTask.BackgroundTaskCompletionListener() {
                WiFiP2pSyncActivity activity = null;

                public BackgroundTask.BackgroundTaskCompletionListener init(WiFiP2pSyncActivity act) {
                    activity = act;
                    return this;
                }

                @Override
                public void didSucceed() {
                    Toast.makeText(activity, R.string.SYNCHRONIZATION_COMPLETE_MESSAGE, Toast.LENGTH_SHORT).show();
                    activity.wifiEventHandler().disconnect();
                    if (activity.hostTask != null) {
                        activity.hostTask.setInterrupted(true);
                        activity.hostTask = null;
                    }
                    //activity.clientTask = null;
                }

                @Override
                public void didFail(String e) {
                    boolean hasMessage = ((e != null) && (e.length() > 0));
                    String message = hasMessage ? e : getString(R.string.SYNCHRONIZATION_FAILED_MESSAGE);

                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                    activity.wifiEventHandler().disconnect();
                    if (activity.hostTask != null) {
                        activity.hostTask.setInterrupted(true);
                        activity.hostTask = null;
                    }
                }
            }.init(this);
        }
        return _syncCompletionListener;
    }


    /**
     * Updates the device list on the ui thread.
     *
     * @param peers
     */
    private void updateDeviceListView(List<WifiP2pDevice> peers) {
        mTxtP2PSearchProgress.setVisibility(View.GONE);

        discoveredDevices = new ArrayList<WifiP2pDevice>();
        discoveredDevices.addAll(peers);
        WiFiPeerListAdapter listAdapter = (WiFiPeerListAdapter) mLstP2PDevices.getAdapter();
        listAdapter.addItems(peers);

        // Run the update process on the gui thread, otherwise the list wont be updated.
        runOnUiThread(new Runnable() {
            private ListView listView;

            @Override
            public void run() {
                WiFiPeerListAdapter adapter = (WiFiPeerListAdapter) listView.getAdapter();
                listView.setAdapter(null);
                adapter.notifyDataSetChanged();
                listView.setAdapter(adapter);
            }

            public Runnable init(ListView listview) {
                listView = listview;
                return this;
            }
        }.init(mLstP2PDevices));
        Log.d("DEBUG_WiFiP2p", "Activity - Discovered " + peers.size() + " devices.");

        if (peers.size() == 0) {
            searchForDevices();
        }
    }

    /**
     * Starts the Host task. Informs the user by a little toast.
     */
    private void startHost() {

        //if (this.hostTask == null || this.hostTask.isInterrupted()){
        Log.d("DEBUG_WiFiP2p", "Activity - Starting HOST Task");
        //Toast.makeText(this, PERFORMING_TASK_AS_HOST , Toast.LENGTH_SHORT).show();
        hostTask = new SyncHostTask(ownDevice, syncCompletionListener(), getApplicationContext());
        executingTask = hostTask;
        hostTask.execute();
        //} else {
        //    Log.d("DEBUG_WiFiP2p", "Activity - Preventing third device for any syncing.");
        //}
    }

    /**
     * Starts the wifi direct client task. Informs the user by a little toast.
     *
     * @param info the WifiP2pInfo contains the groupOwnerAddress which is needed for the client task.
     */
    private void startClient(WifiP2pInfo info) {
        Log.d("DEBUG_WiFiP2p", "Activity - Starting CLIENT Task");
        clientTask = new SyncClientTask(info.groupOwnerAddress.getHostAddress(), ownDevice, syncCompletionListener(), getApplicationContext());
        executingTask = clientTask;
        clientTask.execute();
    }

    /**
     * Try to connect to the given device and shows a simple progress dialog.
     *
     * @param device
     */
    private void connectTo(WifiP2pDevice device) {
        if (device != null) {
            progressDialog.setMessage(getString(R.string.PROGRESS_TITLE_CONNECTING));
            progressDialog.show();

            mOtherDevice = device;
            wifiEventHandler().connect(device);

        }
    }

    /**
     * Returns a localized device status string id.
     *
     * @param deviceStatus the status to convert.
     * @return status string id
     */
    private static int getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return R.string.DEVICE_STATUS_AVAILABLE;
            case WifiP2pDevice.INVITED:
                return R.string.DEVICE_STATUS_INVITED;
            case WifiP2pDevice.CONNECTED:
                return R.string.DEVICE_STATUS_CONNECTED;
            case WifiP2pDevice.FAILED:
                return R.string.DEVICE_STATUS_FAILED;
            case WifiP2pDevice.UNAVAILABLE:
                return R.string.DEVICE_STATUS_UNAVAILABLE;
            default:
                return R.string.DEVICE_STATUS_UNKNOWN;
        }
    }

    /**
     * Updates / displays own device information.
     *
     * @param device
     */
    private void updateOwnDeviceInformation(WifiP2pDevice device) {
        mTxtP2PDeviceName.setText(device.deviceName);
        mTxtP2PDeviceStatus.setText(getDeviceStatus(device.status));
        ownDevice = device;
    }

    /**
     * Method to search for new devices.
     */
    private void searchForDevices() {
        mTxtP2PSearchProgress.setVisibility(View.VISIBLE);
        wifiEventHandler().discoverDevices();
    }

    /********************** UI ************************/

    /**
     * Informs the user about a changed wifi state.
     * enabled = true - mTxtP2PNotAvailable is gone
     * enabled = false - mTxtP2PNotAvailable stays and a alert box is displayed for a quick navigation to the wifi settings.
     *
     * @param enabled
     */
    public void setWifiDirectAvailable(boolean enabled) {
        if (enabled) {
            mTxtP2PNotAvailable.setVisibility(View.GONE);
        } else {
            mTxtP2PNotAvailable.setVisibility(View.VISIBLE);
            ((WiFiPeerListAdapter) mLstP2PDevices.getAdapter()).notifyDataSetChanged();
            ownDevice = null;
            updateDeviceListView(new ArrayList<WifiP2pDevice>());
            showWifiDisabledDialog();
            //Toast.makeText(this, "WiFi Direct P2P is disabled.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Displays a AlertDialog that informs the User about the disabled Wifi state and can navigate the user directly to the wifi settings.
     */
    private void showWifiDisabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.WIFI_STATUS_DISABLED_MESSAGE)
                .setCancelable(true)
                .setPositiveButton(R.string.WIFI_STATUS_ENABLE_BUTTON, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    }
                })
                .setNegativeButton(R.string.CANCEL_BUTTON_TITLE, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });

        AlertDialog info = builder.create();
        info.show();
    }

    /**
     * Extracts all subview initially from the view hierarchy.
     */
    private void extractFromView() {
        mTxtP2PDeviceName = findViewById(R.id.txt_p2p_device_name);
        mTxtP2PDeviceStatus = findViewById(R.id.txt_p2p_device_status);
        mTxtP2PChangeDeviceName = findViewById(R.id.txtP2PChangeDeviceName);

        mViewAnimator = findViewById(R.id.viewAnimator);
        mDevicesContainer = findViewById(R.id.devicesContainer);
        mWelcomeContainer = findViewById(R.id.welcomeContainer);
        mTxtP2PSearchProgress = findViewById(R.id.txtP2PSearchProgress);
        mLstP2PDevices = findViewById(R.id.lstP2PDevices);
        mTxtP2PNotAvailable = findViewById(R.id.txtP2PNotAvailable);
    }

    /**
     * Registers all the gui listeners.
     */
    public void registerListeners() {
        if (mLstP2PDevices.getOnItemClickListener() != this)
            mLstP2PDevices.setOnItemClickListener(this);

        if (mLstP2PDevices.getAdapter() == null) {
            discoveredDevices = new ArrayList();
            WiFiPeerListAdapter listAdapter = new WiFiPeerListAdapter(this, R.layout.row_devices, discoveredDevices);
            mLstP2PDevices.setAdapter(listAdapter);
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final WifiP2pDevice device = (WifiP2pDevice) mLstP2PDevices.getAdapter().getItem(position);
        connectTo(device);
    }


    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {
        private List<WifiP2pDevice> items;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                                   List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;
        }

        @Override
        public int getCount() {
            return items.size();
        }


        public void addItems(List<WifiP2pDevice> devicesToAdd) {
            items.clear();
            items.addAll(devicesToAdd);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView top = v.findViewById(R.id.device_name);
                TextView bottom = v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(device.deviceName);
                }
                if (bottom != null) {
                    bottom.setText(getDeviceStatus(device.status));
                }
            }
            return v;
        }
    }
}
