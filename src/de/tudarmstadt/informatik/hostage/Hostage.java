package de.tudarmstadt.informatik.hostage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;

import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.location.MyLocationManager;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBOpenHelper;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;
import de.tudarmstadt.informatik.hostage.services.MultiStageAlarm;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

import static de.tudarmstadt.informatik.hostage.commons.HelperUtils.*;

/**
 * Background service running as long as at least one protocol is active.
 * Service controls start and stop of protocol listener. Notifies GUI about
 * events happening in the background. Creates Notifications to inform the user
 * what it happening.
 * 
 * @author Mihai Plasoianu
 * @author Lars Pandikow
 * @author Wulf Pfeiffer
 */
public class Hostage extends Service {

	private HashMap<String, Boolean> mProtocolActiveAttacks;
	private Boolean multistage_service;

	MultiStageAlarm alarm = new MultiStageAlarm();

	public class LocalBinder extends Binder {
		public Hostage getService() {
			return Hostage.this;
		}
	}

	/**
	 * Task to find out the external IP.
	 * 
	 */
	private class SetExternalIPTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... url) {
			String ipAddress = null;
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet(url[0]);
				HttpResponse response;

				response = httpclient.execute(httpget);

				HttpEntity entity = response.getEntity();
				entity.getContentLength();
				String str = EntityUtils.toString(entity);
				JSONObject json_data = new JSONObject(str);
				ipAddress = json_data.getString("ip");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return ipAddress;
		}

		@Override
		protected void onPostExecute(String result) {
			connectionInfoEditor.putString(getString(R.string.connection_info_external_ip), result);
			connectionInfoEditor.commit();
			notifyUI(this.getClass().getName(), new String[] { getString(R.string.broadcast_connectivity) });
		}
	}

	private static Context context;
    Listener listener;

	/**
	 * Returns the application context.
	 * 
	 * @return context.
	 */
	public static Context getContext() {
		return Hostage.context;
	}

	private LinkedList<Protocol> implementedProtocols;
	private ArrayList<Listener> listeners = new ArrayList<Listener>();

	private NotificationCompat.Builder builder;

	private SharedPreferences connectionInfo;

	private Editor connectionInfoEditor;

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Receiver for connectivity change broadcast.
	 * 
	 * @see MainActivity #BROADCAST
	 */
	//TODO change bssid to 4g....
	private BroadcastReceiver netReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String bssid_old = connectionInfo.getString(getString(R.string.connection_info_bssid), "");
			String bssid_new;
			if (HelperUtils.isCellurarConnected(context)){
				bssid_new = "not Available";
			}
			else {
				 bssid_new = getBSSID(context);
			}
			if (bssid_new == null || !bssid_new.equals(bssid_old)) {
				deleteConnectionData();
				updateConnectionInfo();
				getLocationData();
				notifyUI(this.getClass().getName(), new String[] { getString(R.string.broadcast_connectivity) });
			}
		}
	};

	public List<Listener> getListeners() {
		return listeners;
	}

	/**
	 * Determines the number of active connections for a protocol running on its
	 * default port.
	 * 
	 * @param protocolName
	 *            The protocol name
	 * @return Number of active connections
	 */
	public int getNumberOfActiveConnections(String protocolName) {
		int port = getDefaultPort(protocolName);
		return getNumberOfActiveConnections(protocolName, port);
	}

	/**
	 * Determines the number of active connections for a protocol running on the
	 * given port.
	 * 
	 * @param protocolName
	 *            The protocol name
	 * @param port
	 *            Specific port
	 * @return Number of active connections
	 */
	public int getNumberOfActiveConnections(String protocolName, int port) {
		for (Listener listener : listeners) {
			if (listener.getProtocolName().equals(protocolName) && listener.getPort() == port) {
				return listener.getHandlerCount();
			}
		}
		return 0;
	}

	/**
	 * Determines if there any listener is currently running.
	 * 
	 * @return True if there is a running listener, else false.
	 */
	public boolean hasRunningListeners() {
		for (Listener listener : listeners) {
			if (listener.isRunning())
				return true;
		}
		return false;
	}

	/**
	 * Determines if a protocol with the given name is running on its default
	 * port.
	 * 
	 * @param protocolName
	 *            The protocol name
	 * @return True if protocol is running, else false.
	 */
	public boolean isRunning(String protocolName) {
		int port = getDefaultPort(protocolName);
		return isRunning(protocolName, port);
	}

	public boolean isRunningAnyPort(String protocolName){
		for(Listener listener: listeners){
			if(listener.getProtocolName().equals(protocolName)){
				if(listener.isRunning()) return true;
			}
		}

		return false;
	}

	/**
	 * Determines if a protocol with the given name is running on the given
	 * port.
	 * 
	 * @param protocolName
	 *            The protocol name
	 * @param port
	 *            Specific port
	 * @return True if protocol is running, else false.
	 */
	public boolean isRunning(String protocolName, int port) {
		for (Listener listener : listeners) {
			if (listener.getProtocolName().equals(protocolName) && listener.getPort() == port) {
				return listener.isRunning();
			}
		}
		return false;
	}

	/**
	 * Notifies the GUI about a event.
	 * 
	 * @param sender
	 *            Source where the event took place.
	 * @param values
	 *            Detailed information about the event.
	 */
	public void notifyUI(String sender, String[] values) {
		createNotification();
		// Send Notification
		if (sender.equals(Handler.class.getName()) && values[0].equals(getString(R.string.broadcast_started))) {
			this.mProtocolActiveAttacks.put(values[1], true);
			attackNotification();
		}
		// Inform UI of Preference Change
		Intent intent = new Intent(getString(R.string.broadcast));
		intent.putExtra("SENDER", sender);
		intent.putExtra("VALUES", values);
		Log.i("Sender", sender);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Hostage.context = getApplicationContext();
		implementedProtocols = getImplementedProtocols();
		connectionInfo = getSharedPreferences(getString(R.string.connection_info), Context.MODE_PRIVATE);
		connectionInfoEditor = connectionInfo.edit();
		connectionInfoEditor.commit();
		
		mProtocolActiveAttacks = new HashMap<String, Boolean>();

		createNotification();
		registerNetReceiver();
		updateConnectionInfo();
		getLocationData();
	}

	@Override
	public void onDestroy() {
		cancelNotification();
		unregisterNetReceiver();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.


		/*SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		multistage_service=sp.getBoolean("pref_multistage",true);*/

		startMultiStage();

		return START_STICKY;

	}

	private void stopMultiStage() {
		Context context =this;
		alarm.CancelAlarm(context);
	}

	private void startMultiStage() {
        Context context = this;
        if (alarm != null) {
            alarm.SetAlarm(context);
        } else {
            Toast.makeText(context, "Alarm is null", Toast.LENGTH_SHORT).show();
        }
    }

	/**
	 * Starts the listener for the specified protocol. Creates a new
	 * HoneyService if no matching HoneyListener is found.
	 * 
	 * @param protocolName
	 *            Name of the protocol that should be started.
	 */
	public boolean startListener(String protocolName) {
		return startListener(protocolName, getDefaultPort(protocolName));
	}

	/**
	 * Starts the listener for the specified protocol and port. Creates a new
	 * HoneyService if no matching HoneyListener is found.
	 * 
	 * @param protocolName
	 *            Name of the protocol that should be started.
	 * @param port
	 *            The port number in which the listener should run.
	 */
	public boolean startListener(String protocolName, int port) {
		for (Listener listener : listeners) {
			if (listener.getProtocolName().equals(protocolName) && listener.getPort() == port) {
				if (!listener.isRunning()) {
					if (listener.start()) {
						// Toast.makeText(getApplicationContext(), protocolName
						// + " SERVICE STARTED!", Toast.LENGTH_SHORT).show();
						return true;
					}
					Toast.makeText(getApplicationContext(), protocolName + " SERVICE COULD NOT BE STARTED!", Toast.LENGTH_SHORT).show();
					return false;
				}

			}
		}
		Listener listener = createListener(protocolName, port);
		if (listener != null) {
			if (listener.start()) {
				// Toast.makeText(getApplicationContext(), protocolName +
				// " SERVICE STARTED!", Toast.LENGTH_SHORT).show();
				return true;
			}
		}
		Toast.makeText(getApplicationContext(), protocolName + " SERVICE COULD NOT BE STARTED!", Toast.LENGTH_SHORT).show();
		return false;
	}

	/**
	 * Starts all listeners which are not already running.
	 */
	public void startListeners() {
		for (Listener listener : listeners) {
			if (!listener.isRunning()) {
				listener.start();
			}
		}
		// Toast.makeText(getApplicationContext(), "SERVICES STARTED!",
		// Toast.LENGTH_SHORT).show();
	}

	/**
	 * Stops the listener for the specified protocol.
	 * 
	 * @param protocolName
	 *            Name of the protocol that should be stopped.
	 */
	public void stopListener(String protocolName) {
		stopListener(protocolName, getDefaultPort(protocolName));
	}

	/**
	 * Stops the listener for the specified protocol.
	 * 
	 * @param protocolName
	 *            Name of the protocol that should be stopped.
	 * @param port
	 *            The port number in which the listener is running.
	 */
	public void stopListener(String protocolName, int port) {
		for (Listener listener : listeners) {
			if (listener.getProtocolName().equals(protocolName) && listener.getPort() == port) {
				if (listener.isRunning()) {
					listener.stop();
					mProtocolActiveAttacks.remove(protocolName);
				}
			}
		}
		// Toast.makeText(getApplicationContext(), protocolName +
		// " SERVICE STOPPED!", Toast.LENGTH_SHORT).show();
	}
	

	public void stopListenerAllPorts(String protocolName){
		for(Listener listener: listeners){
			if(listener.getProtocolName().equals(protocolName)){
				if(listener.isRunning()){
					listener.stop();
					mProtocolActiveAttacks.remove(protocolName);
				}
			}
		}
	}
	/**
	 * Stops all running listeners.
	 */
	public void stopListeners() {
		for (Listener listener : listeners) {
			if (listener.isRunning()) {
				listener.stop();
				mProtocolActiveAttacks.remove(listener.getProtocolName());
			}
		}
		// Toast.makeText(getApplicationContext(), "SERVICES STOPPED!",
		// Toast.LENGTH_SHORT).show();
	}

	/**
	 * Updates the notification when a attack is registered.
	 */
	private void attackNotification() {
		SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences(this);
		String strRingtonePreference = defaultPref.getString("pref_notification_sound", "content://settings/system/notification_sound");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = "Under Attack";
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			NotificationChannel channel = new NotificationChannel("32",name,importance);
			channel.setDescription("this");
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

			mNotificationManager.createNotificationChannel(channel);
			Notification.Builder notificationBuilder = new Notification.Builder(this,"32");
			notificationBuilder.setContentTitle(getString(R.string.app_name)).setTicker(getString(R.string.honeypot_live_threat))
					.setContentText(getString(R.string.honeypot_live_threat)).setSmallIcon(R.drawable.ic_service_red).setAutoCancel(true).setWhen(System.currentTimeMillis())
					.setSound(Uri.parse(strRingtonePreference));

			if (defaultPref.getBoolean("pref_vibration", false)) {
				notificationBuilder.setVibrate(new long[] { 100, 200, 100, 200 });
			}
			Notification notification = notificationBuilder.setOngoing(true)
					.setPriority(Notification.PRIORITY_DEFAULT)

					.build();
			startForeground(2, notification);

		}
	}

	/**
	 * Cancels the Notification
	 */
	private void cancelNotification() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(1);
	}

	/**
	 * Creates a HoneyListener for a given protocol on a specific port. After
	 * creation the HoneyListener is not started. Checks if the protocol is
	 * implemented first.
	 * 
	 * @param protocolName
	 *            Name of the protocol
	 * @param port
	 *            Port on which to start the HoneyListener
	 * @return Returns the created HoneyListener, if creation failed returns
	 *         null.
	 */
	private Listener createListener(String protocolName, int port) {
		for (Protocol protocol : implementedProtocols) {
			if (protocolName.equals(protocol.toString())) {
				Listener listener = new Listener(this, protocol, port);
				listeners.add(listener);
				return listener;
			}
		}
		return null;
	}

	/**
	 * Creates a Notification in the notification bar.
	 */
	private void createNotification() {
		if (MainActivity.getInstance() == null) {
			return; // prevent NullPointerException
		}

		HostageDBOpenHelper dbh = new HostageDBOpenHelper(this);
		boolean activeHandlers = false;
		boolean bssidSeen = false;
		boolean listening = false;

		for (Listener listener : listeners) {
			if (listener.isRunning())
				listening = true;
			if (listener.getHandlerCount() > 0) {
				activeHandlers = true;
			}
			if (dbh.bssidSeen(listener.getProtocolName(), getBSSID(getApplicationContext()))) {
				bssidSeen = true;
			}
		}

		PendingIntent resultPendingIntent = intentNotificationGenerator();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = "Under Attack";
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			NotificationChannel channel = new NotificationChannel("42",name,importance);
			channel.setDescription("this");

			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

			mNotificationManager.createNotificationChannel(channel);
			Notification.Builder notificationBuilder = new Notification.Builder(this,"42");

			notificationBuilder.setContentTitle(getString(R.string.app_name)).setWhen(System.currentTimeMillis());

			notficationIconBuilder( listening, activeHandlers, bssidSeen, notificationBuilder);

			notificationBuilder.setContentIntent(resultPendingIntent);
			notificationBuilder.setOngoing(true);

			Notification notification = notificationBuilder.setOngoing(true)
					.setPriority(Notification.PRIORITY_DEFAULT)
					.build();
			startForeground(3, notification);

		}
	}

	/**
	 * Selects the appropriate icon for the notification.
	 * @param listening listener for protocols
	 * @param activeHandlers active Handlers
	 * @param bssidSeen checks if this bssid is already seen
	 * @param notificationBuilder builds the notification
	 */

	private void notficationIconBuilder(boolean listening,boolean activeHandlers,boolean bssidSeen,Notification.Builder notificationBuilder){
		if (!listening) {
			notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
			notificationBuilder.setContentText(getString(R.string.hostage_not_monitoring));
		} else if (activeHandlers) {
			notificationBuilder.setSmallIcon(R.drawable.ic_service_red);
			notificationBuilder.setContentText(getString(R.string.hostage_live_threat));
		} else if (bssidSeen) {
			notificationBuilder.setSmallIcon(R.drawable.ic_service_yellow);
			notificationBuilder.setContentText(getString(R.string.hostage_past_threat));
		} else {
			notificationBuilder.setSmallIcon(R.drawable.ic_service_green);
			notificationBuilder.setContentText(getString(R.string.hostage_no_threat));
		}
	}

	/**
	 * Generates the intent for the notification.
	 * @return the pending Intent
	 */

	private PendingIntent intentNotificationGenerator(){

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(MainActivity.class);

		Intent intent = MainActivity.getInstance().getIntent();
		intent.addCategory(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.setAction("SHOW_HOME");
		stackBuilder.addNextIntent(intent);

		PendingIntent resultPendingIntent = PendingIntent.getActivity(MainActivity.context, 0, intent, 0); //stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		return resultPendingIntent;

	}

	/**
	 * Deletes all session related data.
	 */
	private void deleteConnectionData() {
		connectionInfoEditor.clear();
		connectionInfoEditor.commit();
	}

	/**
	 * Returns the default port number, if the protocol is implemented.
	 * 
	 * @param protocolName
	 *            The name of the protocol
	 * @return Returns the default port number, if the protocol is implemented.
	 *         Else returns -1.
	 */
	private int getDefaultPort(String protocolName) {
		for (Protocol protocol : implementedProtocols) {
			if (protocolName.equals(protocol.toString())) {
				return protocol.getPort();
			}
		}
		return -1;
	}

	/**
	 * Returns an LinkedList<String> with the names of all implemented
	 * protocols.
	 * 
	 * @return ArrayList of
	 *         {@link de.tudarmstadt.informatik.hostage.protocol.Protocol
	 *         Protocol}
	 */
	private LinkedList<Protocol> getImplementedProtocols() {
		String[] protocols = getResources().getStringArray(R.array.protocols);
		String packageName = Protocol.class.getPackage().getName();
		LinkedList<Protocol> implementedProtocols = new LinkedList<Protocol>();

		for (String protocol : protocols) {
			try {
				implementedProtocols.add((Protocol) Class.forName(String.format("%s.%s", packageName, protocol)).newInstance());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return implementedProtocols;
	}

	/**
	 * Starts an Instance of MyLocationManager to set the hostage.location within this
	 * class.
	 */
	private void getLocationData() {
		MyLocationManager locationManager = new MyLocationManager(this);
		locationManager.getUpdates(60 * 1000, 3,context);
	}

	// Notifications

	/**
	 * Register broadcast receiver for connectivity changes
	 */
	private void registerNetReceiver() {
		// register BroadcastReceiver on network state changes
		IntentFilter intent = new IntentFilter();
		intent.addAction(ConnectivityManager.CONNECTIVITY_ACTION); // "android.net.conn.CONNECTIVITY_CHANGE"
		registerReceiver(netReceiver, intent);
	}

	/**
	 * Unregister broadcast receiver for connectivity changes
	 */
	private void unregisterNetReceiver() {
		unregisterReceiver(netReceiver);
	}
	

	public boolean hasProtocolActiveAttacks(String protocol){
		if(!mProtocolActiveAttacks.containsKey(protocol)) return false;
		return mProtocolActiveAttacks.get(protocol);
	}

	public boolean hasActiveAttacks(){
		for(boolean b: mProtocolActiveAttacks.values()){
			if(b) return true;
		}

		return false;
	}

	/**
	 * Updates the connection info and saves them in the the SharedPreferences
	 * for session data. Works for both types of connection (4g and wifi).
	 * 
	 * @see MainActivity #CONNECTION_INFO
	 */
	private void updateConnectionInfo() {

		if (!HelperUtils.isNetworkAvailable(context) ){
			return; // no connection
		}

		if (HelperUtils.isCellurarConnected(context)) {

			int ipAddress = HelperUtils.getCellularIP();
			String ssid= "not Available";
			String bssid= "not Available";
			int netmask= 0;

			updateEditor( ssid,bssid, ipAddress, netmask);


		}else {
			final WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();

			if (connectionInfo == null) {
				return; // no wifi connection
			}
			final DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
			if (dhcpInfo == null) {
				return;
			}

			String ssid = connectionInfo.getSSID();
			int ipAddress = dhcpInfo.ipAddress;
			String bssid= connectionInfo.getBSSID();
			int netmask= dhcpInfo.netmask;

			if (ssid.startsWith("\"") && ssid.endsWith("\"")) { // trim those quotes
				ssid = ssid.substring(1, ssid.length() - 1);
			}

			updateEditor( ssid,bssid, ipAddress, netmask);

		}


		SetExternalIPTask async = new SetExternalIPTask();
		async.execute("http://ip2country.sourceforge.net/ip2c.php?format=JSON");

		this.mProtocolActiveAttacks.clear();
	}

	/**
	 * Updates the editor for the connection
	 *
	 * @see MainActivity #CONNECTION_INFO
	 */

	public void updateEditor(String ssid, String bssid, int ipAddress, int netmask){
		SharedPreferences pref = context.getSharedPreferences(getString(R.string.connection_info), Context.MODE_PRIVATE);

		Editor editor = pref.edit();

		editor.putString(getString(R.string.connection_info_ssid), ssid);
		editor.putString(getString(R.string.connection_info_bssid), bssid);
		editor.putInt(getString(R.string.connection_info_internal_ip), ipAddress);
		editor.putInt(getString(R.string.connection_info_subnet_mask), netmask);


		editor.commit();


	}


}
