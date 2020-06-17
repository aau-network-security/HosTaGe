package de.tudarmstadt.informatik.hostage.sync.wifi_direct.sync_tasks;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import de.tudarmstadt.informatik.hostage.HostageApplication;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.SyncData;
import de.tudarmstadt.informatik.hostage.logging.SyncInfo;
import de.tudarmstadt.informatik.hostage.persistence.DAO.DAOHelper;
import de.tudarmstadt.informatik.hostage.sync.Synchronizer;
import de.tudarmstadt.informatik.hostage.sync.wifi_direct.WiFiP2pClientTask;
import de.tudarmstadt.informatik.hostage.sync.wifi_direct.WiFiP2pSerializableObject;


/**
 * Created by Julien on 14.01.2015.
 */
public class SyncClientTask extends WiFiP2pClientTask {

    //private HostageDBOpenHelper mdbh;
    private DaoSession dbSession;
    private DAOHelper daoHelper;
    private Synchronizer synchronizer;

    public SyncClientTask(String hostIP,  WifiP2pDevice ownDevice,BackgroundTaskCompletionListener l, Context context) {
        super(hostIP, ownDevice, l);
        //mdbh = new HostageDBOpenHelper(context);
        dbSession = HostageApplication.getInstances().getDaoSession();
        daoHelper = new DAOHelper(dbSession,context);
        synchronizer = new Synchronizer(dbSession,context);
    }

@Override
    public WiFiP2pSerializableObject handleReceivedObject(WiFiP2pSerializableObject receivedObj) {

        if (receivedObj == null) {
            Log.i("DEBUG_WiFiP2p_Client", "Starting sync process: " + SyncHostTask.SYNC_INFO_REQUEST);
            SyncInfo thisSyncInfo = synchronizer.getSyncInfo();
            WiFiP2pSerializableObject syncObj = new WiFiP2pSerializableObject();

            syncObj.setObjectToSend(thisSyncInfo);
            syncObj.setRequestIdentifier(SyncHostTask.SYNC_INFO_REQUEST);

            return syncObj;

        } else {
            Log.i("DEBUG_WiFiP2p_Client", "Received: " + receivedObj.getRequestIdentifier());
            if (receivedObj.getRequestIdentifier().equals(SyncHostTask.SYNC_INFO_RESPONSE)) {
                SyncInfo sinfo = (SyncInfo) receivedObj.getObjectToSend();

                if (sinfo != null && (sinfo instanceof SyncInfo)) {
                    Log.i("DEBUG_WiFiP2p_Client", "Sending Data: " + SyncHostTask.SYNC_DATA_REQUEST);
                    SyncData syncData = synchronizer.getSyncData(sinfo);
                    WiFiP2pSerializableObject syncObj = new WiFiP2pSerializableObject();

                    syncObj.setObjectToSend(syncData);
                    syncObj.setRequestIdentifier(SyncHostTask.SYNC_DATA_REQUEST);

                    return syncObj;
                }

            } else if (receivedObj.getRequestIdentifier().equals(SyncHostTask.SYNC_DATA_RESPONSE)) {
                Log.i("DEBUG_WiFiP2p_Client", "Received Sync Data - ending sync process successfully." );
                SyncData sdata = (SyncData) receivedObj.getObjectToSend();

                if (sdata != null && (sdata instanceof SyncData)) {
                    synchronizer.updateFromSyncData(sdata);
                }
            }
        }

        // DISCONNECT
        this.interrupt(true);

        return null;
    }
}
