package de.tudarmstadt.informatik.hostage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;



import de.tudarmstadt.informatik.hostage.protocol.AMQP;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;
import de.tudarmstadt.informatik.hostage.protocol.amqpUtils.AMQPHandler;

public class AMQPListener extends Listener {
    private ArrayList<Handler> handlers = new ArrayList<>();
    private Thread thread;
    private Thread serverThread;
    private ConnectionRegister conReg;
    private boolean running = false;
    private int defaultPort =5672;

    private static Semaphore mutex = new Semaphore(1);
    /**
     * Constructor for the class. Instantiate class variables.
     *
     * @param service  The Background service that started the listener.
     * @param protocol The Protocol on which the listener is running.
     */
    public AMQPListener(Hostage service, Protocol protocol) {
        super(service, protocol);
    }

    public AMQPListener(Hostage service, Protocol protocol, int port) {
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
                serverThread.interrupt();
                iterator.remove();
            }else {
                if (handler.isTerminated()) {
                    conReg.closeConnection();
                    serverThread.interrupt();
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
    public void stop() { stopServer();}

    public void stopServer(){
        if(super.getPort() == defaultPort) {
            AMQP.stopBroker();
            if(serverThread!=null)
                serverThread.interrupt();
            if(thread!=null)
                thread.interrupt();
            notifyUI(false);
        }
    }

    private void fullHandler() throws InterruptedException {
        if (conReg.isConnectionFree()) {
            ExecutorService threadPool = Executors.newCachedThreadPool();

            Thread serverThread = serverThread();
            threadPool.submit(serverThread);
            threadPool.shutdown();
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }
    }

    private Thread serverThread(){
        serverThread =  new Thread(() -> {
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

                if(AMQPHandler.isAnAttackOngoing()) {
                    startHandler();
                    conReg.newOpenConnection();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return serverThread;
    }

    private boolean checkPostScanInProgress(){
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
