package dk.aau.netsec.hostage.sync.wifi_direct;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;


import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

import dk.aau.netsec.hostage.location.CustomLocationManager;
import dk.aau.netsec.hostage.location.LocationException;
import dk.aau.netsec.hostage.ui.activity.MainActivity;

/**
 * Created by Julien on 07.01.2015.
 */
public class WiFiP2pBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {


    /**
     * This listener will inform about any wifi direct change.
     */
    public interface WiFiP2pBroadcastListener {
        void discoveredDevices(List<WifiP2pDevice> peers);

        void wifiP2pIsEnabled(boolean enabled);

        void didConnect(boolean isHost, WifiP2pInfo connectionInfo);

        void failedToConnect();

        void didDisconnect();

        void failedToDisconnect();

        void ownDeviceInformationIsUpdated(WifiP2pDevice device);
    }

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    private boolean isConnecting;

    public WifiP2pDevice getOwnDevice() {
        return ownDevice;
    }

    private WifiP2pDevice ownDevice;

    private android.net.NetworkInfo.DetailedState networkState = null;

    static boolean setIsWifiP2pEnabled;

    static boolean isConnected = false;

    private WiFiP2pBroadcastListener eventListener;

    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param listener WiFiP2pBroadcastListener
     */
    public WiFiP2pBroadcastReceiver(WifiP2pManager manager,
                                    WifiP2pManager.Channel channel,
                                    WiFiP2pBroadcastListener listener) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.eventListener = listener;
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            // Wifi Direct mode is enabled
            setIsWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED;
            this.eventListener.wifiP2pIsEnabled(setIsWifiP2pEnabled);

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // THE DEVICE LIST CHANGED
            // REQUEST THE LIST OF DEVICES
            Log.d("DEBUG_WiFiP2p", "BroadcastReceiver - P2P peers changed.");
            if (manager != null) {
                try{
                    //Retrieving latest location will trigger location permission request, if needed.
                    CustomLocationManager.getLocationManagerInstance(null).getLatestLocation();
                } catch (LocationException le){
                    le.printStackTrace();
                    return;
                }
                manager.requestPeers(channel, this);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                isConnected = true;
                // we are connected with the other device, request connection
                // info to find group owner IP
                manager.requestConnectionInfo(channel, this);
                if (this.ownDevice == null) {
                    this.disconnect();
                }
            } else {
                if (networkInfo.getDetailedState() == android.net.NetworkInfo.DetailedState.DISCONNECTED) {
                    isConnected = false;
                }
                if (this.networkState != null && !this.networkState.equals(networkInfo.getDetailedState()) && networkInfo.getDetailedState() == android.net.NetworkInfo.DetailedState.DISCONNECTED) {
                    // It's a disconnect
                    this.eventListener.didDisconnect();
                }
            }
            if (this.networkState != networkInfo.getDetailedState()) {
                Log.d("DEBUG_WiFiP2p", "BroadcastReceiver - P2P device network state changed to " + this.getDeviceNetworkStatus(networkInfo.getDetailedState()) + ".");
            }
            this.networkState = networkInfo.getDetailedState();
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            this.ownDevice = device;
            this.eventListener.ownDeviceInformationIsUpdated(device);
        }
    }


    // CONNECTION TO A DEVICE
    // ConnectionInfoListener
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        //
        // The owner IP is now known.
        //boolean thisDeviceIsHost = info.isGroupOwner;
        // InetAddress from WifiP2pInfo struct.
        //String ownerIP = info.groupOwnerAddress.getHostAddress();

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.

        if (info.groupFormed) {
            isConnecting = false;
            this.eventListener.didConnect(info.isGroupOwner, info);
        }
    }

    // AVAILABLE DEVICES
    // PEERLISTLISTENER
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {

        List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
        peers.addAll(peerList.getDeviceList());

        if (peers.size() == 0) {
            Log.d("DEBUG_WiFiP2p", "BroadcastReceiver - No devices found");
        }

        this.eventListener.discoveredDevices(peers);
        // DISMISS PROGRESS IF NEEDED
    }

    /**
     * Connects to the given device.
     * @param device
     */
    @SuppressLint("MissingPermission")
    public void connect(WifiP2pDevice device) {
        if (device != null) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            isConnecting = true;

            // Request latest location. This may trigger a location permission request.
            try{
                CustomLocationManager.getLocationManagerInstance(null).getLatestLocation();
            } catch (LocationException le){
                le.printStackTrace();
                return;
            }
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                private WiFiP2pBroadcastListener eventListener;

                @Override
                public void onSuccess() {
                    isConnected = true;
                    // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                }

                @Override
                public void onFailure(int reason) {
                    isConnecting = false;
                    this.eventListener.failedToConnect();
                }

                public WifiP2pManager.ActionListener init(WiFiP2pBroadcastListener eventListener) {
                    this.eventListener = eventListener;
                    return this;
                }
            }.init(this.eventListener));

            TimeoutTask abort_task = new TimeoutTask(20, new BackgroundTask.BackgroundTaskCompletionListener() {
                private WiFiP2pBroadcastReceiver receiver;

                @Override
                public void didSucceed() {
                    if (this.receiver.getOwnDevice().status != WifiP2pDevice.CONNECTED && isConnecting) {
                        this.receiver.disconnect();
                        Log.d("DEBUG_WiFiP2p", "BroadcastReceiver - Cancel connection process.");
                        isConnecting = false;
                    }
                }

                @Override
                public void didFail(String e) {
                    // do nothing here
                }

                public BackgroundTask.BackgroundTaskCompletionListener init(WiFiP2pBroadcastReceiver receiver) {
                    this.receiver = receiver;
                    return this;
                }
            }.init(this));
            abort_task.execute();
        }
    }

    /**
     * Disconnects from the connected wifi direct group if the own device is still connected.
     */
    public void disconnect() {
        if (this.getOwnDevice() == null) return; // no wifi connect

        if (isConnected || this.getOwnDevice().status == WifiP2pDevice.CONNECTED) {
            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                private WiFiP2pBroadcastListener eventListener;

                @Override
                public void onFailure(int reasonCode) {
                    isConnecting = false;
                    this.eventListener.failedToDisconnect();
                }

                @Override
                public void onSuccess() {
                    isConnected = false;
                    isConnecting = false;
                    this.eventListener.didDisconnect();
                }

                public WifiP2pManager.ActionListener init(WiFiP2pBroadcastListener eventListener) {
                    this.eventListener = eventListener;
                    return this;
                }
            }.init(this.eventListener));
        } else {
            if (isConnecting) {
                manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
                    private WiFiP2pBroadcastListener eventListener;

                    @Override
                    public void onSuccess() {
                        this.eventListener.failedToConnect();
                        isConnecting = false;
                    }

                    @Override
                    public void onFailure(int i) {
                        this.eventListener.failedToConnect();
                        isConnecting = false;
                    }

                    public WifiP2pManager.ActionListener init(WiFiP2pBroadcastListener listener) {
                        this.eventListener = listener;
                        return this;
                    }
                }.init(this.eventListener));
            }
        }
    }

    /**
     * Discover other devices.
     * The WiFiP2pBroadcastListener will inform about any change.
     *
     * Retrieve latest location from {@link CustomLocationManager}. If location has not been granted,
     * this will trigger a location permission request, which is needed for the discoverPeers()
     * method call.
     */
    @SuppressLint("MissingPermission")
    public void discoverDevices() {
        try {
            CustomLocationManager.getLocationManagerInstance(null).getLatestLocation();

        } catch (LocationException le){
            le.printStackTrace();
            return;
        }
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("DEBUG_WiFiP2p", "BroadcastReceiver - Discovering Peers initiated.");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d("DEBUG_WiFiP2p", "BroadcastReceiver - Discovering Peers failed. c=" + reasonCode);
            }
        });
    }

    /**
     * Returns a string representation of the given network status.
     * @param networkStatus network status
     * @return localised network status string
     */
    private String getDeviceNetworkStatus(android.net.NetworkInfo.DetailedState networkStatus) {
        switch (networkStatus) {
            case DISCONNECTED: {
                return "Disconnected";
            }
            case AUTHENTICATING:
            {
                return "Authenticating";
            }
            case BLOCKED:
            {
                return "Blocked";
            }
            case CONNECTED: {
                return "CONNECTED";
            }
            case CONNECTING:
            {
                return "Connecting";
            }
            case DISCONNECTING:
            {
                return "Disconnecting";
            }
            case FAILED:
            {
                return "Failed";
            }
            case IDLE:
            {
                return "IDLE";
            }
            case OBTAINING_IPADDR:
            {
                return "Obtaining IPADDR";
            }
            case SCANNING:
            {
                return "Scanning";
            }
            case SUSPENDED:
            {
                return "Suspended";
            }
            default: {
                return "Unknown";
            }
        }
    }
}
