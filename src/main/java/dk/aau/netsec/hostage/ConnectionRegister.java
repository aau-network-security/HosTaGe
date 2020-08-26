package dk.aau.netsec.hostage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Saves the amount of active connections and limits them to a specific number.
 * 
 * @author Wulf Pfeiffer
 * @author Lars Pandikow
 */
public class ConnectionRegister {

	/** Active connections . **/
	private static int openConnections = 0;
	/** Context in which ConnectionRegister is created. **/
	private Context context;

	/**
	 * Constructor sets context.
	 * 
	 * @param context
	 *            Context in which ConnectionRegister is created.
	 */
	public ConnectionRegister(Context context) {
		this.context = context;
	}

	/**
	 * Deregisters a active connection if at least one active connection is
	 * registered.
	 * 
	 * @return true if the connection has been successfully unregistered, else
	 *         false.
	 */
	public boolean closeConnection() {
		if (openConnections > 0) {
			openConnections--;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns the maximum number of active connections.
	 * 
	 * @return maximum number of active connections.
	 */
	public int getMaxConnections() {
		SharedPreferences defaultPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return Integer.parseInt(defaultPref.getString("pref_max_connections", "1"));
	}

	/**
	 * Returns the number of active connections.
	 * 
	 * @return number of active connections.
	 */
	public int getOpenConnections() {
		return openConnections;
	}

	/**
	 * Returns if there are new connections allowed or not.
	 * 
	 * @return true if a new connection is allowed, else false.
	 */
	public boolean isConnectionFree() {
		return getMaxConnections() == 0
				|| openConnections < getMaxConnections();
	}

	/**
	 * Registers a new active connection if there are connections allowed.
	 * 
	 * @return true if a new connection has been successfully registered, else
	 *         false.
	 */
	public boolean newOpenConnection() {
		if (isConnectionFree()) {
			openConnections++;
			return true;
		} else {
			return false;
		}
	}

}
