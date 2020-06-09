package de.tudarmstadt.informatik.hostage.sync.wifi_direct.sync_tasks;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import de.tudarmstadt.informatik.hostage.logging.SyncData;
import de.tudarmstadt.informatik.hostage.logging.SyncInfo;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBOpenHelper;
import de.tudarmstadt.informatik.hostage.sync.Synchronizer;
import de.tudarmstadt.informatik.hostage.sync.wifi_direct.WiFiP2pSerializableObject;
import de.tudarmstadt.informatik.hostage.sync.wifi_direct.WiFiP2pServerTask;

/**
 * Created by Julien on 14.01.2015.
 */
public class SyncHostTask extends WiFiP2pServerTask {


    public static final String SYNC_INFO_REQUEST = "sync_info_request";
    public static final String SYNC_INFO_RESPONSE = "sync_info_response";

    public static final String SYNC_DATA_REQUEST = "sync_data_request";
    public static final String SYNC_DATA_RESPONSE = "sync_data_response";

    private HostageDBOpenHelper mdbh;
    private Synchronizer synchronizer;

    private Map<String, SyncInfo> receivedInfoPerDevice;

    public SyncHostTask(  WifiP2pDevice ownDevice, BackgroundTaskCompletionListener l , Context context) {
        super(ownDevice , l);
        mdbh = new HostageDBOpenHelper(context);
        synchronizer = new Synchronizer(mdbh);

        this.receivedInfoPerDevice = new HashMap<String, SyncInfo>();
    }

    @Override
    public WiFiP2pSerializableObject handleReceivedObject(WiFiP2pSerializableObject receivedObj){
        if (receivedObj != null) {
            Log.i("DEBUG_WiFiP2p_Host", "Received: " + receivedObj.getRequestIdentifier());

            if (receivedObj.getRequestIdentifier().equals(SYNC_INFO_REQUEST)){
                Log.i("DEBUG_WiFiP2p_Host", "Sending Sync Info: " + SYNC_INFO_RESPONSE);

                SyncInfo syncInfo = (SyncInfo) receivedObj.getObjectToSend();
                this.receivedInfoPerDevice.put(receivedObj.getActingDevice_IP_address(), syncInfo);

                SyncInfo response = synchronizer.getSyncInfo();
                WiFiP2pSerializableObject syncObj = new WiFiP2pSerializableObject();

                syncObj.setObjectToSend(response);
                syncObj.setRequestIdentifier(SYNC_INFO_RESPONSE);

                return syncObj;
            }

            if (receivedObj.getRequestIdentifier().equals(SYNC_DATA_REQUEST)){
                Log.i("DEBUG_WiFiP2p_Host", "Sending Sync Data: " + SYNC_DATA_RESPONSE);

                SyncData sdata = (SyncData) receivedObj.getObjectToSend();

                if (sdata != null && sdata instanceof SyncData){
                    synchronizer.updateFromSyncData(sdata);
                }

                SyncInfo syncInfo = this.receivedInfoPerDevice.get(receivedObj.getActingDevice_IP_address());
                if (syncInfo != null){
                    SyncData response = synchronizer.getSyncData(syncInfo);

                    WiFiP2pSerializableObject syncObj = new WiFiP2pSerializableObject();
                    syncObj.setObjectToSend(response);
                    syncObj.setRequestIdentifier(SYNC_DATA_RESPONSE);

                    return syncObj;
                }
            }
        }

        Log.i("DEBUG_WiFiP2p_Host", "Stop Sync Process - Performing a disconnect.");
        // DISCONNECT
        this.interrupt(true);
        return null;
    }
}
