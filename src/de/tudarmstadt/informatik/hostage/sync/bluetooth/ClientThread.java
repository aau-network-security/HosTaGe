package de.tudarmstadt.informatik.hostage.sync.bluetooth;

import java.io.IOException;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

/**
 * Thread used to connect to a remote bluetooth device.
 * @author Lars Pandikow
 */
public class ClientThread extends Thread {
	private final BluetoothSocket socket;
	private final Handler mHandler;

	public ClientThread(BluetoothDevice device, Handler handler) {
		mHandler = handler;
		BluetoothSocket tmp = null;
		try {
			tmp = device.createRfcommSocketToServiceRecord(BluetoothSyncActivity.serviceUUID);
		} catch (IOException e) {
		}
		socket = tmp;
	}

	/** Will cancel an in-progress connection, and close the socket */
	public void cancel() {
		try {
			socket.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void run() {

		try {
			socket.connect();
			mHandler.obtainMessage(BluetoothSyncActivity.CONNECTION_ESTABLISHED, socket).sendToTarget();
		} catch (IOException connectException) {
			mHandler.obtainMessage(BluetoothSyncActivity.CONNECTION_FAILED).sendToTarget();
			// Unable to connect; close the socket and get out
			try {
				socket.close();
			} catch (IOException closeException) {
			}
			return;
		}		
	}
}