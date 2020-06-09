package de.tudarmstadt.informatik.hostage.sync.bluetooth;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

/**
 * Thread listens for incomming connections.
 * @author Lars Pandikow
 */
public class ServerThread extends Thread {
	private final BluetoothServerSocket serverSocket;
	private final Handler mHandler;

	public ServerThread(Handler handler, String app_name) {
		BluetoothServerSocket tmp = null;
		mHandler = handler;
		try {
			tmp = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord(app_name, BluetoothSyncActivity.serviceUUID);
		} catch (IOException e) {
			mHandler.obtainMessage(BluetoothSyncActivity.CONNECTION_FAILED).sendToTarget();
		}
		serverSocket = tmp;
	}

	/** Will cancel the listening socket, and cause the thread to finish */
	public void cancel() {
		try {
			serverSocket.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void run() {
		BluetoothSocket socket = null;
		while (true) {
			try {
				socket = serverSocket.accept();
			} catch (IOException e) {
				e.printStackTrace();
				mHandler.obtainMessage(BluetoothSyncActivity.CONNECTION_FAILED).sendToTarget();
				break;
			}

			if (socket != null) {
				// Do work to manage the connection (in a separate thread)
				mHandler.obtainMessage(BluetoothSyncActivity.CONNECTION_ESTABLISHED, socket).sendToTarget();
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
		}
	}
}
