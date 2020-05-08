package de.tudarmstadt.informatik.hostage.system;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.SharedPreferences;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.preference.PreferenceManager;
import android.util.Log;

import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;

public class PrivilegedPort implements Runnable {

	public enum TYPE {
		TCP, UDP
	}

	private final static String UNIX_PATH = "hostage";

	private final String LOG_TAG;

	private TYPE type;
	private int port;

	private FileDescriptor fd;

	public PrivilegedPort(TYPE type, int port) {
		LOG_TAG = String.format("HosTaGe (PortBinder %s %d)", type.toString(), port);
		this.type = type;
		this.port = port;
		try {
			new Thread(this).start();
			LocalServerSocket localServer = new LocalServerSocket(UNIX_PATH);
			LocalSocket localSocket = localServer.accept();
			while (localSocket.getInputStream().read() != -1)
				;
			FileDescriptor[] fdArray;
			fdArray = localSocket.getAncillaryFileDescriptors();
			if (fdArray != null) {
				this.fd = fdArray[0];
			}
			localSocket.close();
			localServer.close();
		} catch (IOException e) {
		}
	}

	public FileDescriptor getFD() {
		return fd;
	}

	@Override
	public void run() {
		String porthack = Device.getPorthackFilepath();
		Log.i("privileged port", porthack);
        String command = String.format(porthack+" %s %d", type.toString(), port);

		try {
			Process p = new ProcessBuilder("su", "-c", command).start();
			if (p.waitFor() != 0) {
				logError(p.getErrorStream());
			}
			logOutput(p.getInputStream());
		} catch (IOException e) {
		} catch (InterruptedException e) {
		}
	}

	private void logOutput(InputStream stdout) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
		String line;
		while ((line = reader.readLine()) != null) {
			Log.d(LOG_TAG, line);
		}
	}

	private void logError(InputStream stderr) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stderr));
		Log.e(LOG_TAG, reader.readLine());
	}

}