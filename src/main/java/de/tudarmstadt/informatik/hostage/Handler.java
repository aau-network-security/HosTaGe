package de.tudarmstadt.informatik.hostage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.commons.SubnetUtils;
import de.tudarmstadt.informatik.hostage.location.MyLocationManager;
import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.Logger;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.logging.NetworkRecord;
import de.tudarmstadt.informatik.hostage.logging.SyncDevice;
import de.tudarmstadt.informatik.hostage.nio.Reader;
import de.tudarmstadt.informatik.hostage.nio.Writer;
import de.tudarmstadt.informatik.hostage.protocol.GHOST;
import de.tudarmstadt.informatik.hostage.protocol.MQTT;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;
import de.tudarmstadt.informatik.hostage.protocol.mqttUtils.MQTTHandler;
import de.tudarmstadt.informatik.hostage.sync.tracing.TracingSyncService;
import de.tudarmstadt.informatik.hostage.wrapper.Packet;


/**
 * Abstract class for a connection handler using a given protocol.
 *
 * @author Mihai Plasoianu
 * @author Wulf Pfeiffer
 * @author Lars Pandikow
 */
public class Handler implements Runnable {

	/** Time until the socket throws a time out. The time is in milliseconds. */
	private int TIMEOUT;

	private Hostage service;
	protected Protocol protocol;
	private Socket client;
	protected Thread thread;

	private SharedPreferences pref;

	private long attack_id;
	private String externalIP;
	private String BSSID;
	private String SSID;

	private int subnetMask;
	private int internalIPAddress;

	private boolean logged;

	private Listener listener;

	/**
	 * Constructor of the class. Initializes class variables for communication
	 * and hostage.logging. Then starts itself in a new Thread.
	 *
	 * @param service
	 *            The background service.
	 * @param listener
	 *            The Listener that called the service.
	 * @param protocol
	 *            The protocol on which the handler is running.
	 * @param client
	 *            A Socket for the communication with a remote client.
	 */
	public Handler(Hostage service, Listener listener, Protocol protocol, Socket client) {
		this.service = service;
		this.listener = listener;
		this.protocol = protocol;
		if (protocol.toString().equals("GHOST")) {
			((GHOST) protocol).setAttackerIP(client.getInetAddress());
			((GHOST) protocol).setCurrentPort(listener.getPort());
		}

		this.client = client;
		this.thread = new Thread(this);
		pref = PreferenceManager.getDefaultSharedPreferences(service);
		TIMEOUT = pref.getInt("timeout", 30) * 1000;
		getAndIncrementAttackID(pref);
		SharedPreferences connInfo = service.getSharedPreferences(service.getString(R.string.connection_info), Context.MODE_PRIVATE);
		BSSID = connInfo.getString(service.getString(R.string.connection_info_bssid), null);
		SSID = connInfo.getString(service.getString(R.string.connection_info_ssid), null);
		externalIP = connInfo.getString(service.getString(R.string.connection_info_external_ip), null);

		// we need this info to find out whether the attack was internal
		subnetMask = connInfo.getInt(service.getString(R.string.connection_info_subnet_mask), 0);
		internalIPAddress = connInfo.getInt(service.getString(R.string.connection_info_internal_ip), 0);

		setSoTimeout(client);
		logged = false;
		thread.start();
	}

    public Handler(Hostage service, Listener listener, Protocol protocol){
		this.service = service;
        this.listener = listener;
        this.protocol = protocol;

        this.thread = new Thread(this);
        pref = PreferenceManager.getDefaultSharedPreferences(service);
        TIMEOUT = pref.getInt("timeout", 30) * 1000;
        getAndIncrementAttackID(pref);
        SharedPreferences connInfo = service.getSharedPreferences(service.getString(R.string.connection_info), Context.MODE_PRIVATE);
        BSSID = connInfo.getString(service.getString(R.string.connection_info_bssid), null);
        SSID = connInfo.getString(service.getString(R.string.connection_info_ssid), null);
        externalIP = connInfo.getString(service.getString(R.string.connection_info_external_ip), null);

        // we need this info to find out whether the attack was internal
        subnetMask = connInfo.getInt(service.getString(R.string.connection_info_subnet_mask), 0);
        internalIPAddress = connInfo.getInt(service.getString(R.string.connection_info_internal_ip), 0);

        logged = false;
		thread.start();

	}

	/**
	 * Determines if the interrupt flag of the thread is set.
	 *
	 * @return True when the flag is set, else false.
	 */
	public boolean isTerminated() {
		return thread.isInterrupted();
	}

	/**
	 * Sets the interrupt flag of the thread and tries to close the socket.
	 */
	public void kill() {
		service.notifyUI(this.getClass().getName(),
				new String[] { service.getString(R.string.broadcast_started), protocol.toString(), Integer.toString(listener.getPort()) });
		thread.interrupt();

		try {
			if(client != null)
				client.close();
		} catch (Exception e) {

		}
		//uploadTracing();
		listener.refreshHandlers();
	}

	@Deprecated
	//TODO Temporary removed TracingMonitor
	private void uploadTracing(){
		boolean upload = pref.getBoolean("pref_auto_synchronize", false);
		if(upload){
			Intent intent = new Intent(service, TracingSyncService.class);
			intent.setAction(TracingSyncService.ACTION_START_SYNC);
			service.startService(intent);
		}

	}

	/**
	 * Creates InputStream and OutputStream for the socket. Starts communication
	 * with client. When the client closes the connection or a time out occurs
	 * the handler is finished.
	 */
	@Override
	public void run() {
		service.notifyUI(this.getClass().getName(),
				new String[] { service.getString(R.string.broadcast_started), protocol.toString(), Integer.toString(listener.getPort()) });
		if(!MQTTHandler.getCurrentConnectedMessages().isEmpty()){
			try {
				handleMQTTPackets();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			kill();
			return;
		}

		InputStream in;
		OutputStream out;
		try {
			if(client!=null) {
				in = client.getInputStream();
				out = client.getOutputStream();
				talkToClient(in, out);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		kill();
	}

	private void handleMQTTPackets() throws UnknownHostException {
		logMQTTPackets();
	}

	/**
	 * Gets attack ID for the attack. Also increases the attack ID counter by
	 * one. Method is synchronized for thread safety.
	 *
	 * @param pref
	 *            The default SharedPreference of the application
	 * @return Unique integer attack ID
	 */
	private synchronized void getAndIncrementAttackID(SharedPreferences pref) {
		Editor editor = pref.edit();
		attack_id = pref.getLong("ATTACK_ID_COUNTER", 0);
		editor.putLong("ATTACK_ID_COUNTER", attack_id + 1);
		editor.commit();
	}

	/**
	 * Set the timeout of the socket to the hard coded time out variable.
	 *
	 * @param client
	 *            The socket
	 * @see #TIMEOUT
	 */
	private void setSoTimeout(Socket client) {
		try {
			client.setSoTimeout(TIMEOUT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a MessageRecord for a message exchanged with a client.
	 *
	 * @param type
	 *            The type of the message.
	 * @param packet
	 *            The content of the message.
	 * @return The Record representing the communication message.
	 */
	public MessageRecord createMessageRecord(MessageRecord.TYPE type, String packet) {
		MessageRecord record = new MessageRecord(true);
		record.setAttack_id(attack_id);
		record.setType(type);
		record.setTimestamp(System.currentTimeMillis());
		if(packet != null && !packet.isEmpty())
			record.setPacket(packet);
		return record;
	}

	/**
	 * Creates a AttackRecord for a specific attack from a client.
	 *
	 * @return The AttackRecord representing the attack.
	 */
    public AttackRecord createAttackRecord() {
		AttackRecord record = new AttackRecord();
		record.setAttack_id(attack_id);
        record.setSync_id(attack_id);
        if(SyncDevice.currentDevice()!=null)
        	record.setDevice(SyncDevice.currentDevice().getDeviceID());
        else
        	record.setDevice(UUID.randomUUID().toString());
		record.setProtocol(protocol.toString());
		record.setExternalIP(externalIP);
		record.setLocalIP(client.getLocalAddress().getHostAddress());
		record.setLocalPort(client.getLocalPort());

		record.setWasInternalAttack(checkIfIsInternalAttack());
		record.setRemoteIP(client.getInetAddress().getHostAddress());
		record.setRemotePort(client.getPort());
		record.setBssid(BSSID);
		return record;
	}

	private boolean checkIfIsInternalAttack(){
		int prefix = Hostage.prefix;

		String internalIp = HelperUtils.intToStringIp(internalIPAddress);

		int remoteIPAddress = HelperUtils.packInetAddress(client.getInetAddress().getAddress());
		SubnetUtils utils = new SubnetUtils(internalIp+"/"+prefix);
		String remoteIP = HelperUtils.intToStringIp(remoteIPAddress);

		boolean isInRange = utils.getInfo().isInRange(remoteIP);

		return isInRange;
	}

	/**
	 * Creates a NetworkRecord containing information about the current network.
	 *
	 * @return The NetworkRecord representing the current network.
	 */
    public NetworkRecord createNetworkRecord() {
		NetworkRecord record = new NetworkRecord();
		record.setBssid(BSSID);
		record.setSsid(SSID);
		if (MyLocationManager.getNewestLocation() != null) {
			record.setLatitude(MyLocationManager.getNewestLocation().getLatitude());
			record.setLongitude(MyLocationManager.getNewestLocation().getLongitude());
			record.setAccuracy(MyLocationManager.getNewestLocation().getAccuracy());
			record.setTimestampLocation(MyLocationManager.getNewestLocation().getTime());
		} else {
			record.setLatitude(0.0);
			record.setLongitude(0.0);
			record.setAccuracy(Float.MAX_VALUE);
			record.setTimestampLocation(0);
		}
		return record;
	}

    public void log(MessageRecord.TYPE type, String packet){
		if(!logged){
			Logger.log(Hostage.getContext(), createNetworkRecord());
			Logger.log(Hostage.getContext(), createAttackRecord());
			logged = true;
		}
		if (packet != null && packet.length() > 0) { // prevent hostage.logging empty packets
			Logger.log(Hostage.getContext(), createMessageRecord(type, packet));
		}
	}

	public void log(MessageRecord.TYPE type) throws UnknownHostException {
    	if(!logged){
			Logger.log(Hostage.getContext(), createNetworkRecord());
			Logger.log(Hostage.getContext(), MQTTHandler.createAttackRecord(attack_id,externalIP,protocol,subnetMask,BSSID,internalIPAddress));
			Logger.log(Hostage.getContext(),MQTTHandler.createMessageRecord(type,attack_id));
			logged = true;
		}
	}

    //just for debugging purpose
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

	/**
	 * Communicates with a client using the corresponding protocol
	 * implementation.
	 *
	 * @param in
	 *            InputStream of the socket.
	 * @param out
	 *            OutputStream of the socket.
	 * @throws IOException
	 */

	protected void talkToClient(InputStream in, OutputStream out) throws IOException {
		Reader reader = new Reader(in, protocol.toString());
		Writer writer = new Writer(out);
		Packet inputLine;
		List<Packet> outputLine;
		if (protocol.whoTalksFirst() == Protocol.TALK_FIRST.SERVER) {
			outputLine = protocol.processMessage(null);
			writer.write(outputLine);
			for (Packet o : outputLine) {
				log(MessageRecord.TYPE.SEND, o.toString());
			}
		}
		while (!thread.isInterrupted() && (inputLine = reader.read()) != null) {
			outputLine = protocol.processMessage(inputLine);
			log(MessageRecord.TYPE.RECEIVE, inputLine.toString());
			if (outputLine != null) {
				writer.write(outputLine);
				for (Packet o : outputLine) {
					log(MessageRecord.TYPE.SEND, o.toString());
				}
			}
			if (protocol.isClosed()) {
				break;
			}
		}
	}

	protected void logMQTTPackets() throws UnknownHostException {
		log(MessageRecord.TYPE.RECEIVE);
	}
}
