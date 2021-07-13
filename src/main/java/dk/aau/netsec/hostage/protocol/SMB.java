package dk.aau.netsec.hostage.protocol;


import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import androidx.preference.PreferenceManager;

import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.core.DeviceContextException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.Listener;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.location.CustomLocationManager;
import dk.aau.netsec.hostage.location.LocationException;
import dk.aau.netsec.hostage.logging.AttackRecord;
import dk.aau.netsec.hostage.logging.Logger;
import dk.aau.netsec.hostage.logging.MessageRecord;
import dk.aau.netsec.hostage.logging.NetworkRecord;
import dk.aau.netsec.hostage.logging.SyncDevice;
import dk.aau.netsec.hostage.protocol.utils.cifs.CifsServer;
import dk.aau.netsec.hostage.protocol.utils.cifs.FileInject;
import dk.aau.netsec.hostage.ui.activity.MainActivity;
import dk.aau.netsec.hostage.wrapper.Packet;


/**
 * HostageV3
 * ================
 *
 * @author Alexander Brakowski
 * @author Daniel Lazar
 */
public class SMB implements Protocol {
    private Listener mListener;
    private CifsServer mCifsServer;

    SharedPreferences pref;

    private long attack_id;
    private String externalIP;
    private String BSSID;
    private String SSID;

    private int subnetMask;
    private int internalIPAddress;

    private boolean logged;

    public Listener getListener() {
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


        try {
            mCifsServer = new CifsServer(this, fileInject);
            mCifsServer.run();
        } catch (InvalidConfigurationException | DeviceContextException | IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        mCifsServer.stop();
    }

    public int getLocalIp() {
        WifiManager wifi = (WifiManager) MainActivity.getContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        return dhcp.ipAddress;
    }

    private synchronized void getAndIncrementAttackID(SharedPreferences pref) {
        SharedPreferences.Editor editor = pref.edit();
        attack_id = pref.getLong("ATTACK_ID_COUNTER", 0);
        editor.putLong("ATTACK_ID_COUNTER", attack_id + 1);
        editor.apply();
    }

    public MessageRecord createMessageRecord(MessageRecord.TYPE type, String packet) {
        MessageRecord record = new MessageRecord(true);
        record.setAttack_id(attack_id);
        record.setType(type);
        record.setStringMessageType(type.name());
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

        try {
            Location latestLocation = CustomLocationManager.getLocationManagerInstance(null).getLatestLocation();

            record.setLatitude(latestLocation.getLatitude());
            record.setLongitude(latestLocation.getLongitude());
            record.setAccuracy(latestLocation.getAccuracy());
            record.setTimestampLocation(latestLocation.getTime());
        } catch (LocationException le) {
            record.setLatitude(0.0);
            record.setLongitude(0.0);
            record.setAccuracy(Float.MAX_VALUE);
            record.setTimestampLocation(0);
        }
        return record;
    }

    public void log(MessageRecord.TYPE type, String packet, int localPort, InetAddress remoteIP, int remotePort) {
        if (!logged) {
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
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

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

    public String toString() {
        return "SMB";
    }
}
