package dk.aau.netsec.hostage;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Class used to detect port scans.
 * We assume a port scan if at least 2 different ports get a connection in a small amount of time.
 */
public class ConnectionGuard {

    private ConnectionGuard() {
    }

    private static long lastConnectionTimestamp = 0;
    private static long lastPortscanTimestamp = 0;
    private static String lastIP = "";
    private static int lastPort = 0;

    private static long getPortscanTimeout() {
        // this might be called in a time critical context so maybe
        // we don't want to do this each time and instead just once
        SharedPreferences defaultPref = PreferenceManager
                .getDefaultSharedPreferences(Hostage.getContext());
        // the pref value is in seconds
        int portscanTimeoutS = Integer.parseInt(defaultPref.getString("pref_timeout", "60"));

        return 1000 * portscanTimeoutS;
    }

    /**
     * Register a connection for port scan detection. Stores information about the last connection.
     *
     * @param port The local port used for communication.
     * @param ip   The IP address of the remote device.
     * @return True if a port scan has been detected.
     */
    public synchronized static boolean registerConnection(int port, String ip) {
        long timestamp = System.currentTimeMillis();
        boolean result = detectedPortscan(port, ip, timestamp);

        lastConnectionTimestamp = timestamp;
        if (result) {
            lastPortscanTimestamp = timestamp;
        }
        lastIP = ip;
        lastPort = port;
        return result;
    }

    public synchronized static boolean portscanInProgress() {
        return (System.currentTimeMillis() - lastPortscanTimestamp) < getPortscanTimeout();
    }

    /**
     * Check if the new connection is part of a port scan attack.
     *
     * @param port      The local port used for communication.
     * @param ip        The IP address of the remote device.
     * @param timestamp Time stamp of connection
     * @return True if a port scan has been detected.
     */
    private synchronized static boolean detectedPortscan(int port, String ip, long timestamp) {
        Log.i("Alte Werte:", "LastTime: " + lastConnectionTimestamp + " ,LastIP: " + lastIP + ", lastPort:" + port);
        Log.i("Alte Werte:", "Time: " + timestamp + " ,IP: " + ip + ", Port:" + port);
        boolean result = false;
        boolean belowThreshold = ((timestamp - lastConnectionTimestamp) < getPortscanTimeout());
        boolean sameIP = (lastIP.equals(ip));
        boolean samePort = (lastPort == port);
        if (sameIP && belowThreshold && !samePort) {
            result = true;
        }

        return result;
    }
}
