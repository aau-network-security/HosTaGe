package dk.aau.netsec.hostage.sync.p2p;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.logging.SyncData;
import dk.aau.netsec.hostage.logging.SyncInfo;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.sync.Synchronizer;


public class P2PSyncActivity extends Activity implements WifiP2pManager.GroupInfoListener, WifiP2pManager.PeerListListener, WifiP2pManager.ChannelListener, AdapterView.OnItemClickListener, WifiP2pManager.ConnectionInfoListener {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    private TextView mTxtP2PDeviceName;
    private TextView mTxtP2PDeviceStatus;
    private ViewAnimator mViewAnimator;
    private RelativeLayout mDevicesContainer;
    private TextView mTxtP2PSearchProgress;
    private ListView mLstP2PDevices;
    private RelativeLayout mWelcomeContainer;
    private TextView mTxtP2PNotAvailable;
    private TextView mTxtP2PChangeDeviceName;

    private WifiP2pDevice mDevice;
    private WifiP2pDevice mOtherDevice;
    private WifiP2pInfo mInfo;

    private final int OWNER_SERVER_PORT = 8988;
    private final int CLIENT_SERVER_PORT = 8989;

    private ClientAsyncTask clientAsync;
    private ServerAsyncTask serverAsync;

    // private HostageDBOpenHelper mDbHelper;
    private DaoSession dbSession;
    private DAOHelper daoHelper;
    private Synchronizer synchronizer;

    private void extractFromView() {
        mTxtP2PDeviceName = findViewById(R.id.txt_p2p_device_name);
        mTxtP2PDeviceStatus = findViewById(R.id.txt_p2p_device_status);
        mTxtP2PChangeDeviceName = findViewById(R.id.txtP2PChangeDeviceName);

        //mTxtP2PHeader = (TextView) findViewById(R.id.txtP2PHeader);
        //mTxtP2PSubHeader = (TextView) findViewById(R.id.txtP2PSubheader);
        //mTxtP2PHelpBack = (TextView) findViewById(R.id.txtP2PHelpBack);
        //mViewAnimator = (ViewAnimator) findViewById(R.id.viewAnimator);
        mDevicesContainer = findViewById(R.id.devicesContainer);
        //mWelcomeContainer = (RelativeLayout) findViewById(R.id.welcomeContainer);
        mTxtP2PSearchProgress = findViewById(R.id.txtP2PSearchProgress);
        mLstP2PDevices = findViewById(R.id.lstP2PDevices);
        //mBtnP2PSearch = (Button) findViewById(R.id.btnP2PSearch);
        mTxtP2PNotAvailable = findViewById(R.id.txtP2PNotAvailable);
    }

    public void discoverPeers() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mViewAnimator.showNext();
                mTxtP2PSearchProgress.setVisibility(View.VISIBLE);

                setWifiDirectAvailable();
            }

            @Override
            public void onFailure(int reason) {
                if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                    setWifiDirectNotAvailable();
                }
            }
        });
    }

    public void setWifiDirectNotAvailable() {
        //mBtnP2PSearch.setVisibility(View.GONE);
        mTxtP2PNotAvailable.setVisibility(View.VISIBLE);
        peers.clear();
        ((WiFiPeerListAdapter) mLstP2PDevices.getAdapter()).notifyDataSetChanged();
        mDevice = null;
    }

    public void setWifiDirectAvailable() {
        //mBtnP2PSearch.setVisibility(View.VISIBLE);
        mTxtP2PNotAvailable.setVisibility(View.GONE);
    }

    public void closeConnection() {
        if (this.serverAsync != null && !this.serverAsync.isCancelled()) {
            this.serverAsync.cancel(true);
        }

        if (this.clientAsync != null && !this.clientAsync.isCancelled()) {
            this.clientAsync.cancel(true);
        }
    }

    public void registerListeners() {
        mTxtP2PChangeDeviceName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Method method1 = null;
                try {
                    method1 = mManager.getClass().getDeclaredMethod("setDeviceName", WifiP2pManager.Channel.class);
                    method1.invoke(mManager, mChannel, "Android_fc546");
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mDbHelper = new HostageDBOpenHelper(getApplicationContext());
        dbSession = HostageApplication.getInstances().getDaoSession();
        daoHelper = new DAOHelper(dbSession, this);
        synchronizer = new Synchronizer(dbSession, this);

        setContentView(R.layout.activity_p2_psync);

        assert getActionBar() != null;
        getActionBar().setTitle("WifiDirect Synchronization");

        this.extractFromView();
        this.registerListeners();

        this.mLstP2PDevices.setAdapter(new WiFiPeerListAdapter(this, R.layout.row_devices, peers));
        this.mLstP2PDevices.setOnItemClickListener(this);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), new WifiP2pManager.ChannelListener() {
            @Override
            public void onChannelDisconnected() {
                System.out.println("---------------------> CHannel disconnect!!!");
            }
        });

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        this.discoverPeers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new P2PBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        mTxtP2PSearchProgress.setVisibility(View.GONE);
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        ((WiFiPeerListAdapter) mLstP2PDevices.getAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            return;
        }
    }

    private WifiP2pDevice getConnectedPeer() {
        for (WifiP2pDevice device : peers) {
            if (device.status == WifiP2pDevice.CONNECTED) {
                return device;
            }
        }

        return null;
    }

    private static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    public void updateThisDevice(WifiP2pDevice device) {
        this.mDevice = device;

        mTxtP2PDeviceName.setText(device.deviceName);
        mTxtP2PDeviceStatus.setText(getDeviceStatus(device.status));
    }

    @Override
    public void onChannelDisconnected() {
        closeConnection();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final WifiP2pDevice device = (WifiP2pDevice) this.mLstP2PDevices.getAdapter().getItem(position);
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mOtherDevice = device;
            }

            @Override
            public void onFailure(int reason) {
                //mOtherDevice = null;

                Toast.makeText(P2PSyncActivity.this, "Could not connect to device. Retry.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        mInfo = info;
        //if(mOtherDevice == null) return;

        if(info.groupFormed && info.isGroupOwner){
            Log.i("server", "I am the group owner!");
            WifiP2pDevice connectedPeer = getConnectedPeer();
            if(connectedPeer != null && mInfo != null){
                Log.i("peers", mInfo.groupFormed + " - " + mInfo.isGroupOwner);
                Log.i("server", "Starting async server!");
                serverAsync = new ServerAsyncTask(this, OWNER_SERVER_PORT, synchronizer);
                serverAsync.execute();
            }
            //new ClientAsyncTask(this, mOtherDevice.deviceAddress, CLIENT_SERVER_PORT).execute();
        } else if(info.groupFormed){
            //new ServerAsyncTask(this, CLIENT_SERVER_PORT).execute();
            Log.i("client", "Starting async client!");
            clientAsync = new ClientAsyncTask(this, mInfo.groupOwnerAddress.getHostAddress(), OWNER_SERVER_PORT, synchronizer);
            clientAsync.execute();
        }
    }

    public void sendDatabase(boolean isOwner){

    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {

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

    public static class ServerAsyncTask extends AsyncTask<String, Integer, Integer> {
        private Activity context;
        private int port;
        private final ProgressDialog progressDialog;
        private final Synchronizer synchronizer;

        /**
         * @param context
         */
        public ServerAsyncTask(Activity context, int port, Synchronizer synchronizer) {
            this.context = context;
            this.port = port;
            this.progressDialog = new ProgressDialog(context);
            this.synchronizer = synchronizer;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            this.progressDialog.setMessage("Synchronizing data with other device...");
            this.progressDialog.setIndeterminate(false);
            this.progressDialog.setMax(100);
            this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.progressDialog.setCancelable(true);
            this.progressDialog.show();
        }

        @Override
        protected Integer doInBackground(String... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(this.port);
                Log.i("server", "Waiting for clients!");

                Socket client = serverSocket.accept();
                Log.i("server", "Client connected!");

                publishProgress(1);

                ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());

                // -- client speaks first, receive syncinfo
                SyncInfo otherSyncInfo = null;

                try {
                    otherSyncInfo = (SyncInfo) ois.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                publishProgress(2);

                // --- 1. write first message - syncinfo
                Log.i("server", "writing to client");

                SyncInfo thisSyncInfo = new SyncInfo();
                thisSyncInfo.deviceMap.put("dev1", 12L);
                thisSyncInfo.deviceMap.put("dev2", 13L);

                oos.writeObject(thisSyncInfo);
                oos.flush();
                oos.reset();

                publishProgress(3);

                // --- 2. read sync data
                SyncData syncData = null;

                try {
                    syncData = (SyncData) ois.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                publishProgress(4);

                // --- 3. send sync data
                SyncData mySyncData = new SyncData();
                //mySyncData.records.add(new Record());

                oos.writeObject(mySyncData);
                oos.flush();
                oos.reset();

                publishProgress(5);

                // --- 4. We are done!
                ois.close();
                oos.close();

                //copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();

                return 0;
            } catch (IOException e) {
                return -1;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            this.progressDialog.setProgress(progress[0] * (100 / 5));
        }


        @Override
        protected void onPostExecute(Integer unused) {
            this.progressDialog.dismiss();
        }
    }

    public static class ClientAsyncTask extends AsyncTask<String, Integer, Integer> {
        private Context context;
        private int port;
        private String host;
        private static final int SOCKET_TIMEOUT = 10000;
        private int tryNum = 0;
        private final ProgressDialog progressDialog;
        private final Synchronizer synchronizer;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            this.progressDialog.setMessage("Synchronizing data with other device...");
            this.progressDialog.setIndeterminate(false);
            this.progressDialog.setMax(100);
            this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.progressDialog.setCancelable(true);
            this.progressDialog.show();
        }

        /**
         * @param context
         */
        public ClientAsyncTask(Context context, String host, int port, Synchronizer synchronizer) {
            this.context = context;
            this.host = host;
            this.port = port;
            this.progressDialog = new ProgressDialog(context);
            this.synchronizer = synchronizer;
        }

        @Override
        protected Integer doInBackground(String... params) {
            Socket socket = new Socket();
            tryNum++;

            try {
                Log.i("client", "trying to connect to " + host + ":" + port);
                socket.bind(null);
                socket.connect(new InetSocketAddress(host, port), SOCKET_TIMEOUT);

                Log.i("client", "connected to server");
                publishProgress(1);

                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                // Client sends first!
                SyncInfo thisSyncInfo = new SyncInfo();
                thisSyncInfo.deviceMap.put("dev4", 12L);
                thisSyncInfo.deviceMap.put("dev5", 13L);

                oos.writeObject(thisSyncInfo);
                oos.flush();
                oos.reset();

                publishProgress(2);

                // --- 1. Receive sync info
                SyncInfo otherSyncInfo = null;

                try {
                    otherSyncInfo = (SyncInfo) ois.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                publishProgress(3);

                // --- 2. Send sync data
                SyncData thisSyncData = new SyncData();
                //thisSyncData.records.add(new Record());
                oos.writeObject(thisSyncData);
                oos.flush();
                oos.reset();



                publishProgress(4);

                // --- 3. Receive sync data
                SyncData otherSyncData = null;

                try {
                    otherSyncData = (SyncData) ois.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                publishProgress(5);

                // --

                ois.close();
                oos.close();
                socket.close();
            } catch (IOException e) {
                if(tryNum >= 5) {
                    return -1;
                }

                long seconds_to_wait = (long) Math.min(60, Math.pow(2, tryNum));
                Log.i("client", "could not connect to server. Will try again in " + seconds_to_wait + "s");

                try {
                    Thread.sleep(seconds_to_wait * 1000);
                    doInBackground(params);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }

            return 0;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            this.progressDialog.setProgress(progress[0] * (100 / 5));
        }


        @Override
        protected void onPostExecute(Integer unused) {
            this.progressDialog.dismiss();
        }
    }


    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

}
