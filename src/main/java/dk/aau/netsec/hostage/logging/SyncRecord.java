package dk.aau.netsec.hostage.logging;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

import java.util.ArrayList;


/**
 * Created by Julien on 08.12.2014.
 */
public class SyncRecord implements Parcelable, Serializable {

    private static final long serialVersionUID = 7106818788090434192L;


    private long attack_id;
    private long sync_id;
    private String bssid;
    private String device;
    private String protocol;
    private String localIP;
    private int localPort;
    private String remoteIP;
    private int remotePort;
    private String externalIP;
    private int wasInternalAttack; // 1 if attacker ip and local ip were in same subnet, else 0

    transient private AttackRecord attackRecord;

    // attack
    //private int id;
    //private long timestamp;
    //private MessageRecord.TYPE type;
    //private String packet;

    private ArrayList<MessageRecord> messageRecords;

    public static final Parcelable.Creator<SyncRecord> CREATOR = new Parcelable.Creator<SyncRecord>() {
        @Override
        public SyncRecord createFromParcel(Parcel source) {
            return new SyncRecord(source);
        }

        @Override
        public SyncRecord[] newArray(int size) {
            return new SyncRecord[size];
        }
    };

    public SyncRecord() {

    }

    public SyncRecord(AttackRecord attackRecord){
        this.attackRecord = attackRecord;
        this.setAttack_id(attackRecord.getAttack_id());
        this.setProtocol(attackRecord.getProtocol());
        this.setExternalIP(attackRecord.getExternalIP());
        this.setLocalPort(attackRecord.getLocalPort());
        this.setRemoteIP(attackRecord.getRemoteIP());
        this.setRemotePort(attackRecord.getRemotePort());
        this.setWasInternalAttack(attackRecord.getWasInternalAttack());
        this.setBssid(attackRecord.getBssid());
        this.setSync_id(attackRecord.getSync_id());
        this.setDevice(attackRecord.getDevice());
    }

    public SyncRecord(Parcel source) {
        super();
        this.attack_id = source.readLong();
        this.protocol = source.readString();
        this.localIP = source.readString();
        this.localPort = source.readInt();
        this.remoteIP = source.readString();
        this.remotePort = source.readInt();
        this.externalIP = source.readString();
        this.wasInternalAttack = source.readInt();
        this.bssid = source.readString();
        this.device = source.readString();
        this.sync_id = source.readLong();

        this.messageRecords = source.readArrayList(MessageRecord.class.getClassLoader());


        this.attackRecord = null;
        AttackRecord attack= this.getAttackRecord();


        //this.id = source.readInt();
        //this.timestamp = source.readLong();
        //this.type = MessageRecord.TYPE.valueOf(source.readString());
        //this.packet = source.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(attack_id);
        dest.writeString(protocol);
        dest.writeString(localIP);
        dest.writeInt(localPort);
        dest.writeString(remoteIP);
        dest.writeInt(remotePort);
        dest.writeString(externalIP);
        dest.writeInt(wasInternalAttack);
        dest.writeString(bssid);
        dest.writeString(device);
        dest.writeLong(sync_id);

        dest.writeList(this.messageRecords);
        //dest.writeInt(id);
        //dest.writeLong(timestamp);
        //dest.writeString(type.name());
        //dest.writeString(packet);
    }


    public AttackRecord getAttackRecord(){
        if (this.attackRecord == null){
            AttackRecord record = new AttackRecord(true);

            this.attack_id = record.getAttack_id();
            record.setProtocol(this.protocol);
            record.setSync_id(this.sync_id);
            record.setLocalIP(this.localIP);
            record.setLocalPort(this.localPort);
            record.setBssid(this.bssid);
            record.setDevice(this.device);
            record.setExternalIP(this.externalIP);
            record.setWasInternalAttack(this.getWasInternalAttack());
            record.setRemoteIP(this.remoteIP);
            record.setRemotePort(this.remotePort);
            this.attackRecord = record;

            if (messageRecords != null){
                for (MessageRecord messageRecord : this.messageRecords){
                    messageRecord.setAttack_id(record.getAttack_id());
                }
            }
        }

        return this.attackRecord;
    }

    public ArrayList<MessageRecord> getMessageRecords(){
        return this.messageRecords;
    }

    public void setMessageRecords(ArrayList<MessageRecord> mr){
        this.messageRecords = mr;
    }

    /**
     * @return the attack_id
     */
    public long getAttack_id() {
        return attack_id;
    }

    /**
     * @param attack_id
     *            the attack_id to set
     */
    public void setAttack_id(long attack_id) {
        this.attack_id = attack_id;
    }

    /**
     * @return the bssid
     */
    public String getBssid() {
        return bssid;
    }

    /**
     * @param bssid
     *            the bssid to set
     */
    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol
     *            the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the localIP
     */
    public String getLocalIP() {
        return localIP;
    }

    /**
     * @param localIP
     *            the localIP to set
     */
    public void setLocalIP(String localIP) {
        this.localIP = localIP;
    }

    /**
     * @return the localPort
     */
    public int getLocalPort() {
        return localPort;
    }

    /**
     * @param localPort
     *            the localPort to set
     */
    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    /**
     * @return the remoteIP
     */
    public String getRemoteIP() {
        return remoteIP;
    }

    /**
     * @param remoteIP
     *            the remoteIP to set
     */
    public void setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
    }

    /**
     * @return the remotePort
     */
    public int getRemotePort() {
        return remotePort;
    }

    public long getSync_id(){return sync_id;}
    public String getDevice(){return device;}
    public void setDevice(String d){this.device = d;}
    public void setSync_id(long i){this.sync_id = i;}
    /**
     * @param remotePort
     *            the remotePort to set
     */
    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    /**
     * @return the externalIP
     */
    public String getExternalIP() {
        return externalIP;
    }

    /**
     * @param externalIP
     *            the externalIP to set
     */
    public void setExternalIP(String externalIP) {
        this.externalIP = externalIP;
    }

    public boolean getWasInternalAttack() {return wasInternalAttack == 1;}
    public void setWasInternalAttack(boolean b) {wasInternalAttack = b ? 1 : 0;}


    /**
     * @return the number_of_attacks
     */
    @Deprecated
    public long getNumber_of_attacks() {
        return number_of_attacks;
    }
    /**
     * @param number_of_attacks the number_of_attacks to set
     */
    @Deprecated
    public void setNumber_of_attacks(long number_of_attacks) {
        this.number_of_attacks = number_of_attacks;
    }
    /**
     * @return the number_of_portscans
     */
    @Deprecated
    public long getNumber_of_portscans() {
        return number_of_portscans;
    }
    /**
     * @param number_of_portscans the number_of_portscans to set
     */
    @Deprecated
    public void setNumber_of_portscans(long number_of_portscans) {
        this.number_of_portscans = number_of_portscans;
    }

    private long number_of_attacks;
    private long number_of_portscans;

    @Override
    public String toString() {
        return "SyncRecord{" +
                "attack_id=" + attack_id +
                ", sync_id=" + sync_id +
                ", bssid='" + bssid + '\'' +
                ", device='" + device + '\'' +
                ", protocol='" + protocol + '\'' +
                ", localIP='" + localIP + '\'' +
                ", localPort=" + localPort +
                ", remoteIP='" + remoteIP + '\'' +
                ", remotePort=" + remotePort +
                ", externalIP='" + externalIP + '\'' +
                ", wasInternalAttack=" + wasInternalAttack +
                ", attackRecord=" + attackRecord +
                ", messageRecords=" + messageRecords +
                ", number_of_attacks=" + number_of_attacks +
                ", number_of_portscans=" + number_of_portscans +
                '}';
    }
}

