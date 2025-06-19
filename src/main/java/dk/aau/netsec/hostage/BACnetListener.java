package dk.aau.netsec.hostage;

import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.logging.AttackRecord;
import dk.aau.netsec.hostage.logging.Logger;
import dk.aau.netsec.hostage.logging.MessageRecord;
import dk.aau.netsec.hostage.protocol.BACnet;
import dk.aau.netsec.hostage.protocol.Protocol;
import dk.aau.netsec.hostage.protocol.utils.bacnetUtils.BACnetHandler;

public class BACnetListener extends Listener implements Runnable, BACnetHandler.BACnetEventListener {

    private ArrayList<Handler> handlers = new ArrayList<>();
    private Thread thread;
    private Thread serverThread;
    private boolean running = false;
    private int defaultPort = 47808;
    private static Semaphore mutex = new Semaphore(1);
    private ConnectionRegister conReg;

    private final long attackId;

    public BACnetListener(Hostage service, Protocol protocol, int port, long attackId) {
        super(service, protocol, port);
        conReg = new ConnectionRegister(service);
        this.attackId = attackId;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void refreshHandlers() {
        for (Iterator<Handler> iterator = handlers.iterator(); iterator.hasNext(); ) {
            Handler handler = iterator.next();
            if (handler == null || handler.isTerminated()) {
                conReg.closeConnection();
                if (serverThread != null) serverThread.interrupt();
                iterator.remove();
            }
        }
    }

    @Override
    public boolean start() {
        this.thread = new Thread(this);
        this.thread.start();
        return notifyUI(true);
    }

    private boolean notifyUI(boolean running) {
        this.running = running;
        getService().notifyUI(this.getClass().getName(),
                new String[]{getService().getString(R.string.broadcast_started), super.getProtocolName(), Integer.toString(super.getPort())});
        return true;
    }

    @Override
    public void stop() {
        stopServer();
    }

    private void stopServer() {
        if (super.getPort() == defaultPort) {
            ((BACnet) super.getProtocol()).stopServer();
            if (serverThread != null) serverThread.interrupt();
            if (thread != null) thread.interrupt();
            notifyUI(false);
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

    private void fullHandler() throws InterruptedException {
        if (conReg.isConnectionFree()) {
            ExecutorService threadPool = Executors.newCachedThreadPool();
            serverThread = createServerThread();
            threadPool.submit(serverThread);
            threadPool.shutdown();
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }
    }

    private Thread createServerThread() {
        return new Thread(() -> {
            try {
                if (ConnectionGuard.portscanInProgress()) return;
                mutex.acquire();
                if (checkPostScanInProgress()) return;
                mutex.release();

                Thread.sleep(100);

                if (ConnectionGuard.portscanInProgress()) return;

                // Start listening for BACnet
                BACnetHandler.setListener(this);
                ((BACnet) super.getProtocol()).startServer(attackId);


            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean checkPostScanInProgress() {
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
        if (handlers.isEmpty()) {
            handlers.add(newInstance(getService(), this, super.getProtocol().getClass().newInstance()));
        }
    }

    @Override
    public void onBACnetRequestReceived(InetAddress sourceAddress, int sourcePort, byte[] payload) {
        try {
            conReg.newOpenConnection();

            String hexPayload = HelperUtils.bytesToHex(payload);
            Log.d("BACnetListener", "Request from " + sourceAddress + ":" + sourcePort + " → " + hexPayload);

            // Log Attack
            AttackRecord ar = new AttackRecord();
            ar.setAttack_id(System.currentTimeMillis());
            ar.setTimestamp(System.currentTimeMillis());
            ar.setProtocol(getProtocolName());
            ar.setRemoteIP(sourceAddress.getHostAddress());
            ar.setRemotePort(sourcePort);
            ar.setWasInternalAttack(false);
            Logger.log(getService(), ar);

            // Log Message IN
            MessageRecord mrIn = new MessageRecord(true);
            mrIn.setAttack_id(ar.getAttack_id());
            mrIn.setTimestamp(System.currentTimeMillis());
            mrIn.setType(MessageRecord.TYPE.RECEIVE);
            mrIn.setStringMessageType(MessageRecord.TYPE.RECEIVE.name());
            mrIn.setPacket(hexPayload);
            Logger.log(getService(), mrIn);

            // Optional Response
            byte[] response = buildResponse(payload);
            if (response != null) {
                MessageRecord mrOut = new MessageRecord(true);
                mrOut.setAttack_id(ar.getAttack_id());
                mrOut.setTimestamp(System.currentTimeMillis());
                mrOut.setType(MessageRecord.TYPE.SEND);
                mrOut.setStringMessageType(MessageRecord.TYPE.SEND.name());
                mrOut.setPacket(HelperUtils.bytesToHex(response));
                Logger.log(getService(), mrOut);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] buildResponse(byte[] request) {
        if (request == null || request.length < 9) return null;
        int serviceChoice = request[8] & 0xFF;
        String hex;
        switch (serviceChoice) {
            case 0x08: hex = "810A001A01010400000000000009000A000E0000000100"; break; // I-AM
            case 0x07: hex = "810A001D01010400000000000009000100140200000001"; break; // I-HAVE
            case 0x0C: hex = "810A00250101040000000000000C0C0000000100750C010200"; break; // ReadPropertyAck
            default: return null;
        }
        return hexToBytes(hex);
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length() / 2;
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            data[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return data;
    }
}
