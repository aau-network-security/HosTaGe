package dk.aau.netsec.hostage.logging;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import dk.aau.netsec.hostage.logging.formatter.Formatter;
import dk.aau.netsec.hostage.model.JSONSerializable;
import dk.aau.netsec.hostage.persistence.ProfileManager;

public class RecordAll implements JSONSerializable<RecordAll> {
    private long id;
    private long attack_id;
    private long timestamp;
    private MessageRecord.TYPE type;
    private String stringMessageType;
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
    private boolean wasInternalAttack;

    public RecordAll(long id, long attack_id, long timestamp,
                     MessageRecord.TYPE type, String stringMessageType, String packet, String bssid, String ssid, long timestampLocation, double latitude, double longitude, float accuracy, long sync_id, String device, String protocol, String localIP, int localPort, String remoteIP, int remotePort, String externalIP, boolean wasInternalAttack) {
        this.id = id;
        this.attack_id = attack_id;
        this.timestamp = timestamp;
        this.type = type;
        this.stringMessageType = stringMessageType;
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

    public void setId(long id) {
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

    public boolean getWasInternalAttack() {return wasInternalAttack;}

    public void setWasInternalAttack(boolean wasInternalAttack) {
        this.wasInternalAttack = wasInternalAttack;
    }


    public String getStringMessageType() {
        return stringMessageType;
    }

    public void setStringMessageType(String stringMessageType) {
        this.stringMessageType = stringMessageType;
    }

    public String convertPacketFromHex(String packet){
        String clean = packet.replaceAll("[, ;]", "");
        if(validateHex(clean)) {
            byte[] bytes;
            try {
                bytes = Hex.decodeHex(clean.toCharArray());
            } catch (DecoderException e) {
                return packet;
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return packet;
    }

    public String convertPacketFromText(String packet){
        String clean = packet.replaceAll("[, ;]", "");
        if(!validateHex(clean)) {
            return String.format("%x", new BigInteger(1, clean.getBytes(StandardCharsets.UTF_8)));
        }
        return packet;
    }

    private boolean validateHex(String packet){
        Pattern pattern = Pattern.compile("^[0-9a-fA-F]+$");

        return pattern.matcher(packet).matches();
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(Formatter formatter) {
        if (null == formatter) {
            return Formatter.getDefault().format(this);
        }
        return formatter.format(this);
    }

    @Override
    public RecordAll fromJSON(JSONObject json) {
        return null;
    }

    @Override
    public JSONObject toJSON() {
        return convertSelectedFieldsToJSON();
    }

    private JSONObject convertSelectedFieldsToJSON(){
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("attackId",getAttack_id());
            jsonObj.put("localIP",this.externalIP);
            jsonObj.put("attackerIP",this.remoteIP);
            jsonObj.put("remotePort",this.remotePort);
            jsonObj.put("localPort",this.localPort);
            jsonObj.put("attackTime",this.timestampLocation);
            jsonObj.put("protocol",this.protocol);
            jsonObj.put("packet",getPacket());
            jsonObj.put("attackType",getStringMessageType());
            jsonObj.put("profile", ProfileManager.getProfile());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObj;
    }

    private JSONObject convertAllFieldsToJSON(){
        JSONObject jsonObj = new JSONObject();

        for (Field f : RecordAll.class.getDeclaredFields()) {
            try {
                jsonObj.put(f.getName(),f.get(this));
            } catch (JSONException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return jsonObj;

    }

}
