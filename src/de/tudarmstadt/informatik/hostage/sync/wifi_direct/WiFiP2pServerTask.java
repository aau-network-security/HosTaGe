package de.tudarmstadt.informatik.hostage.sync.wifi_direct;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

/**
 * Created by Julien on 07.01.2015.
 *
 * The Server Task.
 *
 * The Server task is waiting as long as a client is connecting to it.
 * As a result of this the main part and process will start after the client had send the first data.
 *
 * You never know how will become the host / server.
 * So you need to implement a strategy if you want do send just from one specific device to the other.
 *
 *  * The server creates the object stream before the client can do it - to avoid a chicken and egg problem.
 */
public abstract class WiFiP2pServerTask extends BackgroundTask {

    public static String ERROR_MESSAGE_UNKNOWN      = MainActivity.getContext().getString(R.string.ERROR_MESSAGE_UNKNOWN);
    public static String ERROR_COMMUNICATION_FAILED = MainActivity.getContext().getString(R.string.ERROR_COMMUNICATION_FAILED);
    public static String ERROR_CONNECTION_FAILED    = MainActivity.getContext().getString(R.string.ERROR_CONNECTION_FAILED);
    public static String ERROR_CONNECTION_TIMEOUT   = MainActivity.getContext().getString(R.string.ERROR_CONNECTION_TIMEOUT);


    static int DEFAULT_TIMEOUT = 15;



    private ServerSocket serverSocket;

    private WifiP2pDevice ownDevice;

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Set the time out seconds to
     * -1 for infinity
     * 0 for defaut 15 seconds
     * 1 or more for other seconds
     * @param timeoutSeconds the seconds to wait for an request before time ist out.
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        if (timeoutSeconds == 0) {
            this.timeoutSeconds = DEFAULT_TIMEOUT;
        } else {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    private int timeoutSeconds;


    @Override
    public void interrupt(boolean b){
        super.interrupt(b);
        if (b && this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (IOException e) {
                String message = e.getLocalizedMessage() != null? e.getLocalizedMessage() : ERROR_MESSAGE_UNKNOWN;
                Log.e("DEBUG_WiFiP2p", "ServerTask - " + message);
            }
        }
    }

    public WiFiP2pServerTask( WifiP2pDevice ownDevice, BackgroundTaskCompletionListener l){
        super(l);
        this.timeoutSeconds = DEFAULT_TIMEOUT;
        this.ownDevice = ownDevice;
    }

    @Override
    public String performInBackground(){
        while (!this.isInterrupted()){
            try {
                this.serverSocket = new ServerSocket(WiFiP2pClientTask.port());
                //serverSocket.setReuseAddress(true);
                //serverSocket.bind(new InetSocketAddress(WiFiP2pClientTask.port()));
                Log.d("DEBUG_WiFiP2p", "ServerTask - Socket opened");
                this.serverSocket.setSoTimeout(this.getTimeoutSeconds() * 1000);
                Socket client = this.serverSocket.accept();
                Log.d("DEBUG_WiFiP2p", "ServerTask - connection done");

                this.handleConnection(client, this.serverSocket);

                client.close();
                serverSocket.close();

                return BACKGROUND_TASK_MESSAGE_SUCCESS;
            } catch (ClassNotFoundException e){
                e.printStackTrace();
                Log.e("DEBUG_WiFiP2p", "ServerTask - " + e.getMessage());
                String e_message =  null;
                e_message = e.getLocalizedMessage();
                if (e_message == null){
                    e_message = ERROR_COMMUNICATION_FAILED;// COMMUNICATION_ERROR
                }
                return e_message;
            } catch (IOException  e) {
                try {
                    if (!this.serverSocket.isClosed()) this.serverSocket.close();
                }catch (IOException ec){
                    Log.e("DEBUG_WiFiP2p", "ServerTask - Could not close server socket.");
                }
                e.printStackTrace();
                Log.e("DEBUG_WiFiP2p", "ServerTask - " + e.getMessage());
                String e_message = e.getLocalizedMessage();
                if (e_message == null){
                    e_message = ERROR_MESSAGE_UNKNOWN;// UNKNOWN_ERROR
                }
                return e_message;
            }
        }
        return BACKGROUND_TASK_MESSAGE_SUCCESS;
    }


    /**
     * The server creates the object stream before the client can do it to avoid a chicken and egg problem.
     * @param client the client socket.
     * @param server the server socket.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void handleConnection(Socket client, ServerSocket server) throws IOException, ClassNotFoundException {
        OutputStream os = client.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.flush();

        InputStream is = client.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(is);


        Object obj = ois.readObject();

        while (obj != null && obj instanceof WiFiP2pSerializableObject) {
            WiFiP2pSerializableObject receivedObj = (  WiFiP2pSerializableObject) obj;
            obj = null;
            WiFiP2pSerializableObject toSend = this.handleReceivedObject(receivedObj);
            if (toSend != null) {
                toSend.setActingDevice_IP_address(this.ownDevice.deviceAddress);
                oos.writeObject(toSend);
                oos.flush();
                oos.reset();
            }
            try {
                obj = ois.readObject();
            }catch (IOException e){
                // IF NULL WAS TRANSMITTED
                obj = null;
            }
        }

        oos.close();
        os.close();
        ois.close();
        is.close();
    }

    /**
     * This method will be called if the server receives data from the client.
     * Always return a WiFiP2pSerializableObject instance to give a simple response, otherwise the connection will be disconnected.
     * @param receivedObj WiFiP2pSerializableObject the clients request
     * @return WiFiP2pSerializableObject the response
     */
    abstract public WiFiP2pSerializableObject handleReceivedObject(WiFiP2pSerializableObject receivedObj);
}
