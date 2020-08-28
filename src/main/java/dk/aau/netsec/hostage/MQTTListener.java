package dk.aau.netsec.hostage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import dk.aau.netsec.hostage.persistence.ProfileManager;
import dk.aau.netsec.hostage.protocol.MQTT;
import dk.aau.netsec.hostage.protocol.Protocol;
import dk.aau.netsec.hostage.protocol.utils.mqttUtils.MQTTHandler;
import dk.aau.netsec.hostage.protocol.utils.mqttUtils.SensorProfile;

import static dk.aau.netsec.hostage.protocol.utils.mqttUtils.MQTTHandler.isTopicPublished;

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

    public void stopMqttBroker(){
        if(super.getPort() == mqttport) {
            MQTT.brokerStop();
            if(brokerThread!=null)
                brokerThread.interrupt();
            if(thread!=null)
                thread.interrupt();
            notifyUI(false);
        }
    }

    private void fullHandler() throws Exception {
        if (conReg.isConnectionFree()) {
            ExecutorService threadPool = Executors.newCachedThreadPool();

            Thread brokerThread = brokerThread();
            threadPool.submit(brokerThread);
            startsMonitoringProfile();
            threadPool.shutdown();
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }
    }

    private Thread brokerThread(){
        brokerThread =  new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (ConnectionGuard.portscanInProgress())
                        return;

                    mutex.acquire();

                    if (checkPostScanInProgress())
                        return;
                    mutex.release();
                    Thread.sleep(100); // wait to see if other listeners detected a portscan

                    if (ConnectionGuard.portscanInProgress())
                        return;

                    isTopicPublished();
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

    private void startsMonitoringProfile() throws Exception {
        if(ProfileManager.getInstance().getCurrentActivatedProfile().mId == 14){
            Timer timer = scheduleMonitorSensorProfile();
            TimeUnit.SECONDS.sleep(5); //this method is on the loop, so it is necessary to wait until it stops.
            timer.cancel();
        }
    }

    private Timer scheduleMonitorSensorProfile(){
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    monitorSensorProfile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            }, 3000, 4000 );//milliseconds

        return timer;
    }

    private void monitorSensorProfile() throws Exception {
        SensorProfile sensorProfile = new SensorProfile();
        sensorProfile.startSensor();
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
