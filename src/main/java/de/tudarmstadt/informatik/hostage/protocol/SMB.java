package de.tudarmstadt.informatik.hostage.protocol;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import org.alfresco.jlan.app.XMLServerConfiguration;
import org.alfresco.jlan.netbios.server.NetBIOSNameServer;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.smb.server.SMBServer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.List;


import de.tudarmstadt.informatik.hostage.Handler;
import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.Listener;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.location.MyLocationManager;
import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.Logger;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.logging.NetworkRecord;
import de.tudarmstadt.informatik.hostage.logging.SyncDevice;
import de.tudarmstadt.informatik.hostage.protocol.cifs.CifsServer;
import de.tudarmstadt.informatik.hostage.protocol.cifs.FileInject;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.wrapper.Packet;


/**
 * HostageV3
 * ================
 * @author Alexander Brakowski
 * @author Daniel Lazar
 */
public class SMB implements Protocol {
    private Listener mListener;
    private Handler mHandler;
    private SMBServer mSmbServer;
    private NetBIOSNameServer mNbNameServer;
    private CifsServer mCifsServer;

    SharedPreferences pref;

    private long attack_id;
    private String externalIP;
    private String BSSID;
    private String SSID;

    private int subnetMask;
    private int internalIPAddress;

    private boolean logged;


    public boolean fileInjected = HelperUtils.isFileInjected;

    public Listener getListener(){
        return mListener;
    }

    public void initialize(Listener mListener) {
        this.mListener = mListener;
        FileInject fileInject = new FileInject();

        fileInject.startListner(mListener);



        Hostage service = mListener.getService();
        pref = PreferenceManager.getDefaultSharedPreferences(service);

        getAndIncrementAttackID(pref);
        SharedPreferences connInfo = service.getSharedPreferences(service.getString(R.string.connection_info), Context.MODE_PRIVATE);
        BSSID = connInfo.getString(service.getString(R.string.connection_info_bssid), null);
        SSID = connInfo.getString(service.getString(R.string.connection_info_ssid), null);
        externalIP = connInfo.getString(service.getString(R.string.connection_info_external_ip), null);

        // we need this info to find out whether the attack was internal
        subnetMask = connInfo.getInt(service.getString(R.string.connection_info_subnet_mask), 0);
        internalIPAddress = connInfo.getInt(service.getString(R.string.connection_info_internal_ip), 0);
        logged = false;

        XMLServerConfiguration smbConfig = new XMLServerConfiguration();

        try {
            smbConfig.loadConfiguration(new InputStreamReader(MainActivity.context.getResources().openRawResource(R.raw.jlan_config)));
            mCifsServer = new CifsServer(smbConfig, this, fileInject);
            mCifsServer.run();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        mCifsServer.stop();
    }

    public int getLocalIp(){
        WifiManager wifi = (WifiManager) MainActivity.context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        return dhcp.ipAddress;
    }

    private synchronized void getAndIncrementAttackID(SharedPreferences pref) {
        SharedPreferences.Editor editor = pref.edit();
        attack_id = pref.getLong("ATTACK_ID_COUNTER", 0);
        editor.putLong("ATTACK_ID_COUNTER", attack_id + 1);
        editor.commit();
    }

    public MessageRecord createMessageRecord(MessageRecord.TYPE type, String packet) {
        MessageRecord record = new MessageRecord(true);
        //record.setId(message_id++); // autoincrement
        record.setAttack_id(attack_id);
        record.setType(type);
        record.setTimestamp(System.currentTimeMillis());
        record.setPacket(packet);
        return record;
    }

    public AttackRecord createAttackRecord(int localPort, InetAddress remoteIP, int remotePort) {
        AttackRecord record = new AttackRecord();
        record.setAttack_id(attack_id);
        record.setSync_id(attack_id);
        record.setDevice(SyncDevice.currentDevice().getDeviceID());



        record.setProtocol(this.toString());
        record.setExternalIP(externalIP);
        record.setLocalIP(CifsServer.intToInetAddress(getLocalIp()).getHostAddress());
        record.setLocalPort(localPort);
        record.setWasInternalAttack((HelperUtils.packInetAddress(remoteIP.getAddress()) & subnetMask) == (internalIPAddress & subnetMask));
        record.setRemoteIP(remoteIP.getHostAddress());
        record.setRemotePort(remotePort);
        record.setBssid(BSSID);
        return record;
    }

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

    public void log(MessageRecord.TYPE type, String packet, int localPort, InetAddress remoteIP, int remotePort){
        if(!logged){
            Logger.log(Hostage.getContext(), createNetworkRecord());
            Logger.log(Hostage.getContext(), createAttackRecord(localPort, remoteIP, remotePort));
            logged = true;
        }
        if (packet != null && packet.length() > 0) { // prevent hostage.logging empty packets
            Logger.log(Hostage.getContext(), createMessageRecord(type, packet));
        }
    }
    private int port = 1025;

    @Override
    public int getPort() { return port; }

    @Override
    public void setPort(int port){ this.port = port;}

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public List<Packet> processMessage(Packet requestPacket) {
        return null;
    }

    @Override
    public TALK_FIRST whoTalksFirst() {
        return TALK_FIRST.CLIENT;
    }

    public String toString(){
        return "SMB";
    }
}
