package de.tudarmstadt.informatik.hostage.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.app.Activity;
import android.os.Build;
import android.util.Log;

import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;


public class Device {
	private static String porthackFilepath = "/data/local/bind";
	private static boolean initialized = false;
	private static boolean root = false; // device is rooted
	private static boolean porthack = false; // porthack installed
	private static boolean iptables = false; // iptables redirection confirmed working

	// TODO: do asynchronous
    public static void checkCapabilities() {
		// assume worst case
		initialized = false;
		root = false;
		porthack = false;
		iptables = false;

		porthackFilepath = getPorthackFilepath();
		String porthackExists = "[ -e "+porthackFilepath+" ]"; // checks existence of porthack

		try {
			Process p = new ProcessBuilder("su", "-c", porthackExists).start();
			switch (p.waitFor()) {
			case 0: porthack = true;
			// fall through and don't break
			case 1: root = true; // 0 and 1 are valid return values of the porthack
				break;

			case 127: // command not found or executable
				root = false;
				porthack = false;
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// TODO: test with various devices, cannot run programm su permission denied
		if (Build.VERSION.SDK_INT >= 18) { // iptables isn't fully implemented on older versions
			final String ipTablesList = "iptables -L -n -t nat"; // list all rules in NAT table
			try {
				Process p = new ProcessBuilder("su", "-c", ipTablesList).start();
				switch (p.waitFor()) {
					case 0: // everything is fine
						iptables = true; // iptables available and working
						break;

					case 3: // no such table
					case 127: // command not found
					default: // unexpected return code
						// while testing code 3 has been returned when table NAT is not available
						iptables = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		initialized = true;
		Log.i("TESTEST", "initialized");
	}

	public static boolean isRooted() {
		assert(initialized);
		return root;
	}

	public static boolean isPorthackInstalled() {
		assert(initialized);
		return porthack;
	}

	public static boolean isPortRedirectionAvailable() { // using iptables
		assert(initialized);
		return iptables;
	}
	/**
	 * copies an asset to the local filesystem for later usage
	 * (used for port hack and shell scripts)
	 * @param assetFilePath
	 * @param destFilePath
	 * @return true on success
	 */
	private static boolean deployAsset(String assetFilePath, String destFilePath) {
		Activity activity = MainActivity.getInstance();
		File file = new File(activity.getFilesDir(), destFilePath);
		try {
			OutputStream os = new FileOutputStream(file);
			try {
				InputStream is = activity.getAssets().open(assetFilePath);
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = is.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			os.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		//Log.i("FILEPATH", file.getAbsolutePath());
		return true; // SUCCESS!
	}

	public static void executePortRedirectionScript() {
		assert(iptables); // we need iptables for our next trick
		if (deployAsset("payload/redirect-ports.sh", "redirect-ports.sh")) {
			String scriptFilePath = new File(MainActivity.getInstance().getFilesDir(), "redirect-ports.sh").getAbsolutePath();
			Process p = null;
			try {
				p = new ProcessBuilder("su", "-c", "sh "+scriptFilePath).start();
				p.waitFor(); // stall the main thread
				// TODO: check return value?
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return name of porthack binary for device architecture
	 */
	private static String getPorthackName() {
		String porthack = "bind";

		// determine system architecture to return the correct binary
		String arch = System.getProperty("os.arch");
		// TODO: handle more architectures
		if (arch.equals("i686")) { // this is what genymotion reports
			porthack += ".x86";
		} else if (arch.startsWith("arm")) {
			/*
			possible values:
			armv4
			armv4t
			armv5t
			armv5te
			armv5tej
			armv6
			armv7
			 */
			porthack += ".arm";
		} else if (arch.equals("mips")) {
			porthack += ".mips";
		}

		return porthack;
	}

	/**
	 * @return filepath to deployed porthack binary
	 */
	public static String getPorthackFilepath() {
		File file = new File(MainActivity.getInstance().getFilesDir(), getPorthackName());
		return file.getAbsolutePath();
	}

	public static boolean deployPorthack() {
		String porthack = getPorthackName();
		if (!deployAsset("payload/"+porthack, porthack)) {
			return false; // :(
		}

		// make port hack executable
		try {
			Process p = new ProcessBuilder("su", "-c", "chmod 700 "+getPorthackFilepath()).start();
			if (p.waitFor() != 0) {
				logError(p.getErrorStream());
				return false;
			}
			logOutput(p.getInputStream());
		} catch (IOException e) {
			return false;
		} catch (InterruptedException e) {
			return false;
		}

		return true; // SUCCESS!
	}

	public static void uninstallPorthack() {try {
		Process p = new ProcessBuilder("su", "-c", "rm "+getPorthackFilepath()).start();
		if (p.waitFor() != 0) {
			logError(p.getErrorStream());
		}
		logOutput(p.getInputStream());
	} catch (IOException e) {
	} catch (InterruptedException e) {
	}
	}

	// copied from PrivilegedPort.java
	private static void logOutput(InputStream stdout) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
		String line;
		while ((line = reader.readLine()) != null) {
			Log.i("HelperUtils", line);
		}
	}

	private static void logError(InputStream stderr) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stderr));
		Log.e("HelperUtils", reader.readLine());
	}
}
