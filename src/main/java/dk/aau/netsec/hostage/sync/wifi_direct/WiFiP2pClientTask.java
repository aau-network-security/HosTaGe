package dk.aau.netsec.hostage.sync.wifi_direct;

import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Julien on 07.01.2015.
 *
 * The client task.
 *
 * The client is the initial actor and will try to connect to the host as long as the task is not interrupted or successfully connected to the host.
 *
 *
 */
public abstract class WiFiP2pClientTask extends BackgroundTask {


    // Set the port here for the client and host task.
    public static int DEFAULT_WIFI_PORT = 1029;

    private String hostIP;
    private Socket socket;

    private WifiP2pDevice ownDevice;

    public String getHostIP() {
        return hostIP;
    }

    public void setHostIP(String hostIP) {
        this.hostIP = hostIP;
    }


    public static int port(){
        return DEFAULT_WIFI_PORT;
    }
    public static int time_out(){
        return 1000;
    }


    public WiFiP2pClientTask(String hostIP, WifiP2pDevice ownDevice, BackgroundTaskCompletionListener l){
        super(l);
        this.ownDevice = ownDevice;
        this.hostIP = hostIP;
    }


    @Override
    public void interrupt(boolean b){
        super.interrupt(b);
        if (b && this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                Log.e("DEBUG_WiFiP2p", e.getMessage());
            }
        }
    }

    @Override
    public String performInBackground(){

        String e_message = null;

        int tryNum = 1;
        int max_tries = 10;
        while (!this.isInterrupted() && tryNum <= max_tries){
            this.socket = new Socket();

            try {
                Log.d("DEBUG_WiFiP2p", "ClientTask - Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(hostIP, port())), time_out());

                Log.d("DEBUG_WiFiP2p", "ClientTask socket - " + socket.isConnected());
                this.handleConnection(socket);

                Log.d("DEBUG_WiFiP2p", "ClientTask - Data written");
            } catch (ClassNotFoundException e){
                Log.e("DEBUG_WiFiP2p", e.getMessage());
                e_message =  e.getLocalizedMessage();
                if (e_message == null){
                    e_message = WiFiP2pServerTask.ERROR_COMMUNICATION_FAILED;// COMMUNICATION_ERROR
                }
                return e_message;            } catch (IOException e) {
                Log.e("DEBUG_WiFiP2p", e.getMessage());

                if(this.isInterrupted()) {
                    this.interrupt(true);
                    break;
                }

                long seconds_to_wait = (long) Math.min(60, Math.pow(2, 1));
                tryNum++;
                Log.i("DEBUG_WiFiP2p", "ClientTaskError - could not connect to server. Will try again in " + 1 + "s");
                try {
                    Thread.sleep(seconds_to_wait * time_out());
                } catch (InterruptedException ie){
                    ie.printStackTrace();
                }
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                            Log.d("DEBUG_WiFiP2p","ClientTaskError - Failed to close socket - "+ e.getLocalizedMessage());
                            e_message =  e.getLocalizedMessage();
                            if (e_message == null){
                                e_message = WiFiP2pServerTask.ERROR_CONNECTION_FAILED;// FAILED TO CONNECT
                            }
                            return e_message;
                        }
                    }
                }
            }
        }

        if (tryNum > max_tries){
            e_message = WiFiP2pServerTask.ERROR_CONNECTION_TIMEOUT;// CONNECTION_TIMEOUT
        }


        return e_message;
    }


    /**
     * Initiates the client task and sends the first response to the host.
     * @param client the client socket
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void handleConnection(Socket client) throws IOException, ClassNotFoundException {

        InputStream is = client.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        OutputStream os = client.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);

        Object obj = null;

        do {
            WiFiP2pSerializableObject receivedObj = (  WiFiP2pSerializableObject) obj;
            obj = null;
            WiFiP2pSerializableObject toSend = this.handleReceivedObject(receivedObj);
            if (toSend != null) {
                toSend.setActingDevice_IP_address(this.ownDevice.deviceAddress);
                oos.writeObject(toSend);
                oos.flush();
                oos.reset();
                obj = ois.readObject();
            }
        } while (obj != null && obj instanceof WiFiP2pSerializableObject);

        oos.close();
        os.close();
        ois.close();
        is.close();

        this.interrupt(true);
    }

    /**
     * This method is initially called with a null parameter to inform about a initial state.
     * Return null to disable the client task.
     * @param receivedObj the response for the last request, null if it is the first call on the host.
     * @return WiFiP2pSerializableObject request object
     */
    public abstract WiFiP2pSerializableObject handleReceivedObject(WiFiP2pSerializableObject receivedObj);
}
