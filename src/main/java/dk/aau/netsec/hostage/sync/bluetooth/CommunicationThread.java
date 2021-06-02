package dk.aau.netsec.hostage.sync.bluetooth;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;



import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.logging.SyncData;
import dk.aau.netsec.hostage.logging.SyncInfo;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.sync.SyncMessage;
import dk.aau.netsec.hostage.sync.Synchronizer;

/**
 * CommunicationThread is responsible for the exchange of synchronization messages between devices.
 * @author Lars Pandikow
 */
public class CommunicationThread extends Thread {
	private final Context context;
	private final BluetoothSocket mmSocket;
	private final ObjectInputStream objectInput;
	private final ObjectOutputStream objectOuput;
	private final Handler mHandler;
	private final DaoSession dbSession;
	private final DAOHelper daoHelper;
	//private final HostageDBOpenHelper mdbh;
    private Synchronizer synchronizer;

	public CommunicationThread(Context con, BluetoothSocket socket, Handler handler) {
		mmSocket = socket;
		mHandler = handler;
		context = con;
		//mdbh = new HostageDBOpenHelper(context);
		dbSession = HostageApplication.getInstances().getDaoSession();
		daoHelper = new DAOHelper(dbSession,context);
        synchronizer = new Synchronizer(dbSession,context);

		ObjectInputStream tmpIn = null;
		ObjectOutputStream tmpOut = null;

		// Get the input and output streams, using temp objects because
		// member streams are final
		try {
			tmpOut = new ObjectOutputStream(socket.getOutputStream());
			tmpIn = new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			mHandler.obtainMessage(BluetoothSyncActivity.CONNECTION_FAILED).sendToTarget();
			e.printStackTrace();
		}

		objectInput = tmpIn;
		objectOuput = tmpOut;
	}

	/* Call this from the main activity to shutdown the connection */
	public void cancel() {
		try {
			objectInput.close();
			objectOuput.close();
			mmSocket.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void run() {	
		HashMap<String, Long> devices = this.daoHelper.getSyncDeviceDAO().getSyncDeviceHashMap();
		write(new SyncMessage(SyncMessage.SYNC_REQUEST, devices));
		// Keep listening to the InputStream until an exception occurs
		while (true) {
		try {
			Object inputObject = objectInput.readObject();
			if(inputObject instanceof SyncMessage){
				handleMessage((SyncMessage) inputObject);				
			}		
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			cancel();
			e.printStackTrace();
			break;
		}
		}
	}
	
	/**
	 * Handles a received synchronization message.
	 * @param message The received message.
	 */
	private void handleMessage(SyncMessage message){
		Log.i("CommunicationThread", "Recieved: " + message.getMessage_code());
		switch(message.getMessage_code()){
			case SyncMessage.SYNC_REQUEST:
                mHandler.obtainMessage(BluetoothSyncActivity.SYNC_START).sendToTarget();
                SyncInfo thisSyncInfo = synchronizer.getSyncInfo();

                write(new SyncMessage(SyncMessage.SYNC_RESPONSE_INFO, thisSyncInfo));
				break;			
			case SyncMessage.SYNC_RESPONSE_INFO:
                SyncInfo otherSyncInfo = (SyncInfo) message.getPayload();

                SyncData thisSyncData = synchronizer.getSyncData(otherSyncInfo);
                write(new SyncMessage(SyncMessage.SYNC_RESPONSE_DATA, thisSyncData));

				break;
            case SyncMessage.SYNC_RESPONSE_DATA:
                SyncData otherData = (SyncData) message.getPayload();
                synchronizer.updateFromSyncData(otherData);

                mHandler.obtainMessage(BluetoothSyncActivity.SYNC_SUCCESSFUL).sendToTarget();
                break;
		}		
	}
	
	/**
	 * Send a message to the remote device.
	 * @param message The message to send.
	 */
	public void write(SyncMessage message) {
		try {
			objectOuput.writeObject(message);
		} catch (IOException e) {
			mHandler.obtainMessage(BluetoothSyncActivity.CONNECTION_FAILED).sendToTarget();
			e.printStackTrace();
		}
	}
}	
