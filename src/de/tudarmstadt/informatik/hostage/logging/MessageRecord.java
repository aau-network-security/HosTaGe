package de.tudarmstadt.informatik.hostage.logging;

import java.io.Serializable;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

/**
 * Holds all necessary information about a single message exchanged during an attack.
 */
public class MessageRecord implements Parcelable, Serializable{
	
	private static final long serialVersionUID = -5936572995202342935L;
	
	public static enum TYPE {
		SEND, RECEIVE
	};
	
	// attack
	private int id;
	private long attack_id;
	private long timestamp;
	private TYPE type;
	private String packet;
	
    public static final Parcelable.Creator<MessageRecord> CREATOR = new Parcelable.Creator<MessageRecord>() {
    	@Override
        public MessageRecord createFromParcel(Parcel source) {
                    return new MessageRecord(source);
            }

            @Override
            public MessageRecord[] newArray(int size) {
            	return new MessageRecord[size];
            }
    };


    public MessageRecord() {

    }

    public MessageRecord(boolean autoincrement){
        if (autoincrement){
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext());

            SharedPreferences.Editor editor = pref.edit();
            int message_id = pref.getInt("MESSAGE_ID_COUNTER", 0);
            editor.putInt("MESSAGE_ID_COUNTER", message_id + 1);
            editor.commit();
            this.id = message_id;
        }
    }

    public MessageRecord(Parcel source) {
            this.id = source.readInt();
            this.attack_id = source.readLong();
            this.timestamp = source.readLong();
            this.type = TYPE.valueOf(source.readString());
            this.packet = source.readString(); 
    }

	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeLong(attack_id);
        dest.writeLong(timestamp);
        dest.writeString(type.name());
        dest.writeString(packet);
	}
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * @return the attack_id
	 */
	public long getAttack_id() {
		return attack_id;
	}
	/**
	 * @param attack_id the attack_id to set
	 */
	public void setAttack_id(long attack_id) {
		this.attack_id = attack_id;
	}
	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}
	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	/**
	 * @return the type
	 */
	public TYPE getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(TYPE type) {
		this.type = type;
	}
	/**
	 * @return the packet
	 */
	public String getPacket() {
		return packet;
	}

    @Override
    public String toString() {
        return "MessageRecord{" +
                "id=" + id +
                ", attack_id=" + attack_id +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", packet='" + packet + '\'' +
                '}';
    }

    /**
	 * @param packet the packet to set
	 */
	public void setPacket(String packet) {
		this.packet = packet;
	}
}
