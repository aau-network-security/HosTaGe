package de.tudarmstadt.informatik.hostage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.location.MyLocationManager;
import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.Logger;
import de.tudarmstadt.informatik.hostage.logging.NetworkRecord;
import de.tudarmstadt.informatik.hostage.net.MyServerSocketFactory;
import de.tudarmstadt.informatik.hostage.protocol.MQTT;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;
import de.tudarmstadt.informatik.hostage.protocol.SMB;
import de.tudarmstadt.informatik.hostage.protocol.SSLProtocol;
import de.tudarmstadt.informatik.hostage.protocol.mqttUtils.MQTTHandler;
import de.tudarmstadt.informatik.hostage.system.Device;


/**
 * Protocol listener class:<br>
 * Creates a Socket on the port of a given protocol and listens for incoming
 * connections.<br>
 * For each connection creates a Socket and instantiate an {@link Handler}.
 * 
 * @author Mihai Plasoianu
 * @author Wulf Pfeiffer
 */
public class Listener implements Runnable {

    public Listener getListener() {
        return this;
    }

    private ArrayList<Handler> handlers = new ArrayList<Handler>();

    private Protocol protocol;

    private ServerSocket server;
    private Thread thread;
    private Thread socketsThread;
    private int port;
    private Hostage service;
    private ConnectionRegister conReg;
    private boolean running = false;
    private int mqttport =1883;

    private static Semaphore mutex = new Semaphore(1); // to enable atomic section in portscan detection

    private static Map<String,Integer> realPorts = new HashMap<>();
    /**
     * Constructor for the class. Instantiate class variables.
     *
     * @param service  The Background service that started the listener.
     * @param protocol The Protocol on which the listener is running.
     */
    public Listener(Hostage service, Protocol protocol) {
        this.service = service;
        this.protocol = protocol;
        port = protocol.getPort();
        conReg = new ConnectionRegister(service);
    }

    public Listener(Hostage service, Protocol protocol, int port) {
        this.service = service;
        this.protocol = protocol;
        this.port = port;
        conReg = new ConnectionRegister(service);
    }

    /**
     * Determines the amount of active handlers.
     *
     * @return The number of active handlers.
     */
    public int getHandlerCount() {
        return handlers.size();
    }

    /**
     * Return the port number on which the listener listening.
     *
     * @return Used port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Determine the name of the protocol the listener is running on.
     *
     * @return Name of the protocol
     */
    public String getProtocolName() {
        return protocol.toString();
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public Hostage getService() {
        return service;
    }

    /**
     * Determines if the service is running.
     *
     * @return True if the service is running, else false.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Remove all terminated handlers from its internal ArrayList.
     */
    public void refreshHandlers() {
        for (Iterator<Handler> iterator = handlers.iterator(); iterator.hasNext(); ) {
            Handler handler = iterator.next();
            if(handler == null){
                conReg.closeConnection();
                socketsThread.interrupt();
                iterator.remove();
            }else {
                if (handler.isTerminated()) {
                    conReg.closeConnection();
                    socketsThread.interrupt();
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void run() {
        if (protocol.toString().equals("")) return;

        while (!thread.isInterrupted()) {
            try {
                fullHandler();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Handler handler : handlers) {
            handler.kill();
        }
    }

    /**
     * Starts the listener. Creates a server socket runs itself in a new Thread
     * and notifies the background service.
     */
    public boolean start() {
        if (protocol.toString().equals("")) {
            if (!Device.isPortRedirectionAvailable()) {
				/*
				We can only use SMB with iptables since we can't transfer UDP sockets using domain sockets (port binder).
				TODO: somehow communicate this limitation to the user. Right now SMB will simply just fail.
				 */
                return false;
            }
            if (Device.isPorthackInstalled()) {
				/*
				Currently the port binder is the preferred method for creating sockets.
				If it installed, we can't use iptables to create UDP sockets.
				@see MyServerSocketFactory
				 */
                return false;
            }
            ((SMB) protocol).initialize(this);
        }

        try {
            server = new MyServerSocketFactory().createServerSocket(port);
            if (server == null) {
                server = new MyServerSocketFactory().createServerSocket(0);
                addRealPorts(protocol.toString(), server.getLocalPort());
            }
                if (server == null)
                    return false;
            (this.thread = new Thread(this)).start();
            return notifyUI(true);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean notifyUI(boolean running){
        this.running = running;
        service.notifyUI(this.getClass().getName(),
                new String[]{service.getString(R.string.broadcast_started), protocol.toString(), Integer.toString(port)});
        return true;
    }

    /**
     * Stops the listener. Closes the server socket, interrupts the Thread its
     * running in and notifies the background service.
     */
    public void stop() {
        if(stopMqttBroker())
            return;
        try {
            server.close();
            thread.interrupt();
            notifyUI(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean stopMqttBroker(){
        if(port == mqttport) {
            MQTT.brokerStop();
            notifyUI(false);
            return true;
        }
        return false;
    }


    /**
     * Waits for an incoming connection, accepts it and starts a {@link Handler}
     */
    private void fullHandler() throws IOException {
        if (conReg.isConnectionFree()) {
            ExecutorService threadPool = Executors.newFixedThreadPool(1);
            Socket client = server.accept();
            Thread socketsThread = socketsThread(client);
            threadPool.submit(socketsThread);
            threadPool.shutdown();
        }
    }


    private Thread socketsThread(Socket client){
        socketsThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (checkPostScanInProgressNomutex(client))
                        return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String ip = client.getInetAddress().getHostAddress();

                    // the mutex should prevent multiple hostage.logging of a portscan
                    try {
                        mutex.acquire();
                        if (checkPostScanInProgress(client))
                            return;

                        if (checkRegisteredConnection(client, ip))
                            return;

                        mutex.release();
                        Thread.sleep(100); // wait to see if other listeners detected a portscan

                        if (checkPostScanInProgressNomutex(client))
                            return;

                        if (protocol.isSecure()) {
                            startSecureHandler(client);
                        } else {
                            startHandler(client);
                        }
                        conReg.newOpenConnection();


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

            }
        });

       return socketsThread;
    }

    private boolean checkPostScanInProgress(Socket client) throws IOException {
        if (ConnectionGuard.portscanInProgress()) {
            mutex.release();
            client.close();
            return true;
        }
        return false;
    }

    private boolean checkPostScanInProgressNomutex(Socket client) throws IOException {
        if (ConnectionGuard.portscanInProgress()) {
            client.close();
            return true;
        }
        return false;
    }

    private boolean checkRegisteredConnection(Socket client,String ip) throws IOException {
        if (ConnectionGuard.registerConnection(port, ip)) { // returns true when a port scan is detected
            logPortscan(client, System.currentTimeMillis());
            mutex.release();
            client.close();
            return true;
        }
        return false;
    }

    /**
     * Creates a new instance of an {@link Handler}.
     *
     * @param service  The background service
     * @param listener The listener that created the handler
     * @param protocol The Protocol the handler will run on
     * @param client   The Socket the handler uses
     * @return A Instance of a {@link Handler} with the specified parameter.
     */
    private Handler newInstance(Hostage service, Listener listener, Protocol protocol, Socket client) {
        return new Handler(service, listener, protocol, client);
    }

    /**
     * Starts a {@link Handler} with the given socket.
     *
     * @param client The socket with the accepted connection.
     * @throws Exception
     */
    private void startHandler(Socket client) throws Exception {
        handlers.add(newInstance(service, this, protocol.toString().equals("CIFS") ? protocol : protocol.getClass().newInstance(), client));
    }

    /**
     * Creates a SSLSocket out of the given socket and starts a {@link Handler}.
     *
     * @param client The socket with the accepted connection.
     * @throws Exception
     */
    private void startSecureHandler(Socket client) throws Exception {
        SSLContext sslContext = ((SSLProtocol) protocol).getSSLContext();
        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket sslClient = (SSLSocket) factory.createSocket(client, null, client.getPort(), false);
        sslClient.setUseClientMode(false);
        handlers.add(newInstance(service, this, protocol.toString().equals("CIFS") ? protocol : protocol.getClass().newInstance(), sslClient));
    }

    /**
     * Logs a port scan attack and notifies ui about the portscan
     *
     * @param client    The socket on which a port scan has been detected.
     * @param timestamp Timestamp when the portscan has been detected.
     */
    private void logPortscan(Socket client, long timestamp) {
        SharedPreferences connInfo = service.getSharedPreferences(service.getString(R.string.connection_info), Context.MODE_PRIVATE);

        AttackRecord attackRecord = logAttackRecord(connInfo, client);
        NetworkRecord networkRecord = logNetworkRecord(connInfo);

        Logger.logPortscan(Hostage.getContext(), attackRecord, networkRecord, timestamp);

        // now that the record exists we can inform the ui
        // only handler informs about attacks so its name is used here
        service.notifyUI(Handler.class.getName(),
                new String[]{service.getString(R.string.broadcast_started), "PORTSCAN",
                        Integer.toString(client.getPort())});
    }


    private AttackRecord logAttackRecord(SharedPreferences connInfo, Socket client){
        AttackRecord attackRecord = new AttackRecord(true);

        attackRecord.setProtocol("PORTSCAN");
        attackRecord.setExternalIP(connInfo.getString(service.getString(R.string.connection_info_external_ip), null));
        attackRecord.setLocalIP(client.getLocalAddress().getHostAddress());
        attackRecord.setLocalPort(0);
        attackRecord.setRemoteIP(client.getInetAddress().getHostAddress());
        attackRecord.setRemotePort(client.getPort());
        attackRecord.setBssid(connInfo.getString(service.getString(R.string.connection_info_bssid), null));

        return attackRecord;

    }

    private NetworkRecord logNetworkRecord(SharedPreferences connInfo){
        NetworkRecord networkRecord = new NetworkRecord();
        networkRecord.setBssid(connInfo.getString(service.getString(R.string.connection_info_bssid), null));
        networkRecord.setSsid(connInfo.getString(service.getString(R.string.connection_info_ssid), null));
        if (MyLocationManager.getNewestLocation() != null) {
            networkRecord.setLatitude(MyLocationManager.getNewestLocation().getLatitude());
            networkRecord.setLongitude(MyLocationManager.getNewestLocation().getLongitude());
            networkRecord.setAccuracy(MyLocationManager.getNewestLocation().getAccuracy());
            networkRecord.setTimestampLocation(MyLocationManager.getNewestLocation().getTime());
        } else {
            networkRecord.setLatitude(0.0);
            networkRecord.setLongitude(0.0);
            networkRecord.setAccuracy(Float.MAX_VALUE);
            networkRecord.setTimestampLocation(0);
        }

        return networkRecord;
    }

    private void addRealPorts(String protocol, Integer port){
        realPorts.put(protocol,port);
    }

    public static Map<String,Integer>  getRealPorts(){
        return realPorts;
    }


}




