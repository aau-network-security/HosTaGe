package dk.aau.netsec.hostage.sync.p2p;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

import androidx.core.app.ActivityCompat;

import dk.aau.netsec.hostage.location.CustomLocationManager;
import dk.aau.netsec.hostage.location.LocationException;
import dk.aau.netsec.hostage.ui.activity.MainActivity;


public class P2PBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private P2PSyncActivity mActivity;

    public P2PBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, P2PSyncActivity activity) {
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                mActivity.setWifiDirectAvailable();
            } else {
                mActivity.setWifiDirectNotAvailable();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            try{
                //Retrieving latest location will trigger location permission request, if needed.
                CustomLocationManager.getLocationManagerInstance(null).getLatestLocation();
            } catch (LocationException le){
                le.printStackTrace();
                return;
            }
            mManager.requestPeers(mChannel, mActivity);
        } else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if(networkInfo.isConnected()){
                mManager.requestConnectionInfo(mChannel, mActivity);
            } else {
                mActivity.closeConnection();
            }
        } else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            mActivity.updateThisDevice(device);
        }
    }
}
