package de.tudarmstadt.informatik.hostage.sync.wifi_direct;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by Julien on 07.01.2015.
 */
public class WiFiP2pEventHandler implements WifiP2pManager.ChannelListener {

    /**
     * The WiFiP2pEventListener informs about all wifi direct changes.
     */
    public interface WiFiP2pEventListener extends WiFiP2pBroadcastReceiver.WiFiP2pBroadcastListener{
         void onConnectionLost();
    }

    private WifiP2pManager manager;

    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private WiFiP2pBroadcastReceiver receiver = null;

    private WiFiP2pEventListener eventListener;

    private Activity activity;


    public WiFiP2pEventHandler(Activity activity, WiFiP2pEventListener listener){
        this.eventListener = listener;
        this.activity = activity;
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(activity, activity.getMainLooper(), this);
    }

    public void setOnRetryIfTheChannelIsLost(boolean shouldRetry){
        this.retryChannel = shouldRetry;
    }
    public boolean isRetryingIfTheChannelIsLost(){
        return this.retryChannel;
    }

    /**
     * Call this method if the app comes back to foreground. E.g. in the onResume from the activity.
     */
    public void startService() {
        if (receiver == null){
            receiver = new WiFiP2pBroadcastReceiver(manager, channel, this.eventListener);
            activity.registerReceiver(receiver, intentFilter);
        }
    }

    /**
     * Call this method to disabled wifi direct while the app is in the background.  E.g. in the onPause from the activity.
     */
    public void stopService() {
        if (receiver != null) {
            activity.unregisterReceiver(receiver);
            receiver = null;
        }
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            retryChannel = true;
            manager.initialize(this.activity, this.activity.getMainLooper(), this);
            receiver = new WiFiP2pBroadcastReceiver(manager, channel, this.eventListener);
            activity.registerReceiver(receiver, intentFilter);
        } else {
            this.eventListener.onConnectionLost();
        }
    }

    /**
     * Connect your device to the given device.
     * @param device
     */
    public void connect(WifiP2pDevice device){
        if (this.receiver != null && device != null) {
            this.receiver.connect(device);
        }
    }

    /**
     * Disconnect from the connected wifi group.
     */
    public void disconnect() {
        if (this.receiver != null)
            this.receiver.disconnect();
    }

    /**
     * Discover all available wifi direct device in the current environment.
     */
    public void discoverDevices(){
        if (this.receiver != null)
            this.receiver.discoverDevices();
    }
}
