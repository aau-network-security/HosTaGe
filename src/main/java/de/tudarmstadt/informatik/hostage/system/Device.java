package de.tudarmstadt.informatik.hostage.system;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;

import de.tudarmstadt.informatik.hostage.system.iptablesUtils.Api;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;


public class Device {
	private static boolean initialized = false;
	private static boolean root = false; // device is rooted
	private static boolean iptables = false; // iptables redirection confirmed working

    public static void checkCapabilities() {
		// assume worst case
		initialized = false;
		root = false;
		iptables = false;

		final String ipTablesList = "iptables -L -n -t nat"; // list all rules in NAT table

		try {
			Process p = new ProcessBuilder("su", "-c", ipTablesList).start();
            switch (p.waitFor()) {
				case 0: // everything is fine
					root=true;
					iptables = true; // iptables available and working
					break;

				case 3: // no such table
				case 127: // command not found
				default: // unexpected return code
					// while testing code 3 has been returned when table NAT is not available
					iptables = false;
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		initialized = true;
	}


	public static boolean isRooted() {
		return root;
	}

	public static boolean isPortRedirectionAvailable() { // using iptables
		return iptables;
	}
	/**
	 * copies an asset to the local filesystem for later usage
	 * (used for port hack and shell scripts)
	 * @param assetFilePath asset FilePath
	 * @param destFilePath destination Filepath
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
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true; // SUCCESS!
	}

	public static void executePortRedirectionScript() {
		Api.remountSystem();
		String mode="0777";

		if (deployAsset("payload/redirect-ports.sh", "redirect-ports.sh")) {
			String scriptFilePath = new File(MainActivity.getInstance().getFilesDir(), "redirect-ports.sh").getAbsolutePath();
			Process p = null;
			try {
				Runtime.getRuntime().exec("chmod " + mode + " " + scriptFilePath).waitFor();

				p = new ProcessBuilder("su", "-c", "sh "+scriptFilePath).start();
				if (p.waitFor() == 0) {
					System.out.println("Test script "+ String.valueOf(p.waitFor()));
					System.out.println("Filepath of payload: "+scriptFilePath);
				} else {
					Api.executeCommands();
					//Api.addRediractionPorts();
				}
				// stall the main thread
			} catch (IOException | InterruptedException e) {
				System.out.println("InsidePortRedirection");
				e.printStackTrace();
			}
		}
	}

}
