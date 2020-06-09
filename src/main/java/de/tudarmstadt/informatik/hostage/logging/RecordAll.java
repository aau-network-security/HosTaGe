package de.tudarmstadt.informatik.hostage.logging;

public class RecordAll {
    private long id;
    private long attack_id;
    private long timestamp;
    private MessageRecord.TYPE type;
    private String packet;
    private String bssid;
    private String ssid;
    private long timestampLocation;
    private double latitude;
    private double longitude;
    private float accuracy;
    private long sync_id;
    private String device;
    private String protocol;
    private String localIP;
    private int localPort;
    private String remoteIP;
    private int remotePort;
    private String externalIP;
    private boolean wasInternalAttack= true;

    public RecordAll(int id, long attack_id, long timestamp,
                     MessageRecord.TYPE type, String packet, String bssid, String ssid, long timestampLocation, double latitude, double longitude, float accuracy, long sync_id, String device, String protocol, String localIP, int localPort, String remoteIP, int remotePort, String externalIP, boolean wasInternalAttack) {
        this.id = id;
        this.attack_id = attack_id;
        this.timestamp = timestamp;
        this.type = type;
        this.packet = packet;
        this.bssid = bssid;
        this.ssid = ssid;
        this.timestampLocation = timestampLocation;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.sync_id = sync_id;
        this.device = device;
        this.protocol = protocol;
        this.localIP = localIP;
        this.localPort = localPort;
        this.remoteIP = remoteIP;
        this.remotePort = remotePort;
        this.externalIP = externalIP;
        this.wasInternalAttack = wasInternalAttack;
    }


    public  RecordAll(){

    }

    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getAttack_id() {
        return attack_id;
    }

    public void setAttack_id(long attack_id) {
        this.attack_id = attack_id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public MessageRecord.TYPE getType() {
        return type;
    }

    public void setType(MessageRecord.TYPE type) {
        this.type = type;
    }

    public String getPacket() {
        return packet;
    }

    public void setPacket(String packet) {
        this.packet = packet;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public long getTimestampLocation() {
        return timestampLocation;
    }

    public void setTimestampLocation(long timestampLocation) {
        this.timestampLocation = timestampLocation;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public long getSync_id() {
        return sync_id;
    }

    public void setSync_id(long sync_id) {
        this.sync_id = sync_id;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getLocalIP() {
        return localIP;
    }

    public void setLocalIP(String localIP) {
        this.localIP = localIP;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public String getRemoteIP() {
        return remoteIP;
    }

    public void setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public String getExternalIP() {
        return externalIP;
    }

    public void setExternalIP(String externalIP) {
        this.externalIP = externalIP;
    }

    public boolean isWasInternalAttack() {
        return wasInternalAttack;
    }

    public void setWasInternalAttack(boolean wasInternalAttack) {
        this.wasInternalAttack = wasInternalAttack;
    }
}
