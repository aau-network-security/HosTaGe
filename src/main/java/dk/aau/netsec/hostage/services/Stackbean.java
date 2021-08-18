package dk.aau.netsec.hostage.services;

/**
 * Created by Shreyas Srinivasa on 21.08.15.
 *
 * Bean class to store objects of multistage attack
 */
public class Stackbean {
    private final String remoteip;
    private final String localip;
    private String protocol;
    private final int remoteport;
    private int localport;
    private String BSSID;
    private String SSID;

    public int getRemotePort() {
        return remoteport;
    }

    public int getLocalPort() {
        return localport;
    }

    public void setLocalPort(int port) {
        this.localport = port;
    }


    public Stackbean(String remoteip, String localip, String protocol, int remoteport, int localport, String BSSID, String SSID) {
        this.remoteip = remoteip;
        this.localip = localip;
        this.protocol = protocol;
        this.remoteport = remoteport;
        this.localport = localport;
        this.BSSID = BSSID;
        this.SSID = SSID;
    }

    public String getLocalip() {
        return localip;
    }

    public String getBSSID() {
        return BSSID;
    }

    public void setBSSID(String BSSID) {
        this.BSSID = BSSID;
    }

    public String getSSID() {
        return SSID;
    }

    public void setSSID(String SSID) {
        this.SSID = SSID;}


    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRemoteIp() {
        return remoteip;
    }

}
