package de.tudarmstadt.informatik.hostage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import de.tudarmstadt.informatik.hostage.protocol.MQTT;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;
import de.tudarmstadt.informatik.hostage.protocol.mqttUtils.MQTTHandler;

public class MQTTListener extends Listener {
    private ArrayList<Handler> handlers = new ArrayList<Handler>();
    private Thread thread;
    private Thread brokerThread;
    private ConnectionRegister conReg;
    private boolean running = false;
    private int mqttport =1883;

    private static Semaphore mutex = new Semaphore(1);
    /**
     * Constructor for the class. Instantiate class variables.
     *
     * @param service  The Background service that started the listener.
     * @param protocol The Protocol on which the listener is running.
     */
    public MQTTListener(Hostage service, Protocol protocol) {
        super(service, protocol);
    }

    public MQTTListener(Hostage service, Protocol protocol, int port) {
        super(service, protocol, port);
        conReg = new ConnectionRegister(service);
    }

    /**
     * Determines if the service is running.
     *
     * @return True if the service is running, else false.
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Remove all terminated handlers from its internal ArrayList.
     */
    @Override
    public void refreshHandlers() {
        for (Iterator<Handler> iterator = handlers.iterator(); iterator.hasNext(); ) {
            Handler handler = iterator.next();
            if(handler == null){
                conReg.closeConnection();
                brokerThread.interrupt();
                iterator.remove();
            }else {
                if (handler.isTerminated()) {
                    conReg.closeConnection();
                    brokerThread.interrupt();
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void run() {
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
     * Starts the listener. Creates a new Thread
     * and notifies the background service.
     */
    @Override
    public boolean start() {
        (this.thread = new Thread(this)).start();
        return notifyUI(true);
    }

    private boolean notifyUI(boolean running){
        this.running = running;
        getService().notifyUI(this.getClass().getName(),
                new String[]{getService().getString(R.string.broadcast_started), super.getProtocolName(), Integer.toString(super.getPort())});
        return true;
    }

    @Override
    public void stop() { stopMqttBroker();}

    public boolean stopMqttBroker(){
        if(super.getPort() == mqttport) {
            MQTT.brokerStop();
            if(brokerThread!=null)
                brokerThread.interrupt();
            if(thread!=null)
                thread.interrupt();
            notifyUI(false);
            return true;
        }
        return false;
    }

    private void fullHandler() throws IOException {
        if (conReg.isConnectionFree()) {
            Thread brokerThread = brokerThread();
            brokerThread.start();

        }
    }

    private Thread brokerThread(){
        brokerThread =  new Thread(new Runnable() {
            @Override
            public void run() {
                if (ConnectionGuard.portscanInProgress())
                    return;

                try {
                    mutex.acquire();

                    if (checkPostScanInProgress())
                        return;
                    mutex.release();
                    Thread.sleep(100); // wait to see if other listeners detected a portscan

                    if (ConnectionGuard.portscanInProgress())
                        return;

                    if(MQTTHandler.isAnAttackOngoing()) {
                        startHandler();

                        conReg.newOpenConnection();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return brokerThread;

    }

    private boolean checkPostScanInProgress() throws IOException {
        if (ConnectionGuard.portscanInProgress()) {
            mutex.release();
            return true;
        }
        return false;
    }


    private Handler newInstance(Hostage service, Listener listener, Protocol protocol) {
        return new Handler(service, listener, protocol);
    }

    private void startHandler() throws Exception {
        if(handlers.isEmpty())
            handlers.add(newInstance(getService(), this, super.getProtocol().toString().equals("CIFS") ? super.getProtocol() : super.getProtocol().getClass().newInstance()));
    }

}
