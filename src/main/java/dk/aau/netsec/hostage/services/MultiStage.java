package dk.aau.netsec.hostage.services;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.location.CustomLocationManager;
import dk.aau.netsec.hostage.location.LocationException;
import dk.aau.netsec.hostage.logging.AttackRecord;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.logging.Logger;
import dk.aau.netsec.hostage.logging.MessageRecord;
import dk.aau.netsec.hostage.logging.NetworkRecord;
import dk.aau.netsec.hostage.logging.RecordAll;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.ui.model.LogFilter;


/**
 * Multistage attack detection service
 */
public class MultiStage extends Service {
    private String bssid = "";
    private String ssid = "";
    private String externalIP;
    String stackRemoteIP;
    String stackLocalIp;
    String stackProtocol;
    int stackRport;
    int stackLport;
    String stackssid;
    String stackbssid;
    private DAOHelper daoHelper;
    Notification notification;
    NotificationManager manager;
    private static final int offset = 0;
    private final int limit = 50;
    private int size;
    List<RecordAll> recordArray = new ArrayList<>();

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    /**
     * Creates a Background Service before onStartCommand.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        DaoSession dbSession;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dbSession = HostageApplication.getInstances().getDaoSession();
            daoHelper = new DAOHelper(dbSession, this);
            startCustomForeground();
        } else {
            dbSession = HostageApplication.getInstances().getDaoSession();
            daoHelper = new DAOHelper(dbSession, this);
            startForeground(1, new Notification());
        }
        fetchData();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startCustomForeground();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        manager.cancel(1);
        stopForeground(true);
        stopSelf();
    }

    //fetch data of records of last 10 mins
    public void fetchData() {
        long currentTime = System.currentTimeMillis();

        int fetchInterval = 1000 * 60 * 30; // setInterval in millis  Millisec * Second * Minute

        long filterTime = (currentTime - fetchInterval);

        LogFilter filter = new LogFilter();

        filter.setAboveTimestamp(filterTime);
        recordArray = daoHelper.getAttackRecordDAO().getRecordsForFilterMutliStage(filter);
        sortListIPs();
        ArrayList<Stackbean> records = new ArrayList<>(addRecordsToStackBean());
        createMultistageRecord(records);
    }

    /**
     * Packing the attack record
     *
     * @param type
     * @param message
     * @param remoteip the remote IP
     * @param localip  the local IP
     * @param protocol is always MULTISTAGE
     * @param rport    the remote port
     * @param lport    the local port
     * @param bssid    Basic service set identifier
     * @param ssid     Service set identifier-Name of the Wifi Network
     */
    public void log(MessageRecord.TYPE type, String message, String externalIP, String remoteip, String localip, String protocol, int rport, int lport, String bssid, String ssid) {
        AttackRecord attackRecord = new AttackRecord(true);

        attackRecord.setProtocol("MULTISTAGE");
        attackRecord.setExternalIP(externalIP);
        attackRecord.setLocalIP(localip);
        attackRecord.setLocalPort(lport);
        attackRecord.setRemoteIP(remoteip);
        attackRecord.setRemotePort(rport);
        attackRecord.setBssid(bssid);

        NetworkRecord networkRecord = new NetworkRecord();
        networkRecord.setBssid(bssid);
        networkRecord.setSsid(ssid);
        try {
            Location latestLocation = CustomLocationManager.getLocationManagerInstance(null).getLatestLocation();
            networkRecord.setLatitude(latestLocation.getLatitude());
            networkRecord.setLongitude(latestLocation.getLongitude());
            networkRecord.setAccuracy(latestLocation.getAccuracy());
            networkRecord.setTimestampLocation(latestLocation.getTime());
        } catch (LocationException le) {
            networkRecord.setLatitude(0.0);
            networkRecord.setLongitude(0.0);
            networkRecord.setAccuracy(Float.MAX_VALUE);
            networkRecord.setTimestampLocation(0);
        }

        MessageRecord messageRecord = new MessageRecord(true);
        messageRecord.setAttack_id(attackRecord.getAttack_id());
        messageRecord.setType(type);
        messageRecord.setTimestamp(System.currentTimeMillis());
        messageRecord.setPacket(message);

        Logger.logMultiStageAttack(Hostage.getContext(), attackRecord, networkRecord, messageRecord, System.currentTimeMillis());

    }

    private void sortListIPs() {
        if (!recordArray.isEmpty())
            recordArray.sort(Comparator.comparing(RecordAll::getRemoteIP));

    }

    private ArrayList<Stackbean> addRecordsToStackBean() {
        ArrayList<Stackbean> b = new ArrayList<>();
        String prevRemoteIP = "";
        String prevProt = "";
        int prevlport = 0;
        int prevrport = 0;
        String prevLocalIP = "";
        if (recordArray.size() != 0) {
            for (RecordAll tmp : recordArray) {

                if ((prevRemoteIP.equals(tmp.getRemoteIP()) && !prevProt.equals(tmp.getProtocol()) && !prevProt.contentEquals("MULTISTAGE"))) {

                    b.add(new Stackbean(prevRemoteIP, prevLocalIP, prevProt, prevrport, prevlport, bssid, ssid));
                    b.add(new Stackbean(tmp.getRemoteIP(), tmp.getLocalIP(), tmp.getProtocol(), tmp.getRemotePort(), tmp.getLocalPort(), tmp.getBssid(), tmp.getSsid()));         //,tmp.getLocalPort(),tmp.getRemotePort()));
                }
                prevRemoteIP = tmp.getRemoteIP();
                prevProt = tmp.getProtocol();
                prevrport = tmp.getRemotePort();
                prevlport = tmp.getLocalPort();
                externalIP = tmp.getExternalIP();
                bssid = tmp.getBssid();
                ssid = tmp.getSsid();
                prevLocalIP = tmp.getLocalIP();

            }
        }

        return b;
    }

    public void createMultistageRecord(ArrayList<Stackbean> b) {
        if (b.size() != 0) {
            StringBuilder message = new StringBuilder();
            for (Stackbean tmp : b) {

                message.append("\nMulti Stage Attack Detected!\n" + "IP:").append(tmp.getRemoteIp()).append("\nProtocol:").append(tmp.getProtocol());

                stackRemoteIP = tmp.getRemoteIp();
                stackLocalIp = tmp.getLocalip();
                stackProtocol = tmp.getProtocol();
                stackRport = tmp.getRemotePort();
                stackLport = tmp.getLocalPort();
                stackbssid = tmp.getBSSID();
                stackssid = tmp.getSSID();
            }
            log(MessageRecord.TYPE.RECEIVE, message.toString(), externalIP,
                    stackRemoteIP, stackLocalIp, stackProtocol, stackRport, stackLport, stackbssid, stackssid);
            b.clear();
            message.setLength(0);

        }

    }

    /**
     * Custom foreground for background service
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startCustomForeground() {
        String NOTIFICATION_CHANNEL_ID = "Try";
        String channelName = "BackgroundService";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        Notification.Builder notificationBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setContentTitle("MultiStage").setContentText("MultiStage running...").setSmallIcon(R.drawable.ic_launcher);

        notification = notificationBuilder.setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        startForeground(1, notification);
    }
}