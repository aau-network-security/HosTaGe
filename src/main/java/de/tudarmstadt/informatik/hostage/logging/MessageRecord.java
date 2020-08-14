package de.tudarmstadt.informatik.hostage.logging;

import java.io.Serializable;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.annotation.Transient;


import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;


import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.NotNull;

/**
 * Holds all necessary information about a single message exchanged during an attack.
 */
@Entity
public class MessageRecord extends RecordAll implements Parcelable, Serializable{
	
	private static final long serialVersionUID = -5936572995202342935L;

	public enum TYPE {
		SEND, RECEIVE
	}

    // attack
	@Id(autoincrement = true)
	private long id;
	private long attack_id;
	private long timestamp;
	@Transient
	private TYPE type;
	private String packet;
    @ToOne(joinProperty = "attack_id")
    private AttackRecord record;
	
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
				/** Used to resolve relations */
				@Generated(hash = 2040040024)
				private transient DaoSession daoSession;
				/** Used for active entity operations. */
				@Generated(hash = 1175120554)
				private transient MessageRecordDao myDao;
				@Generated(hash = 818274295)
				private transient Long record__resolvedKey;


    public MessageRecord() {
    	super();

    }

    public MessageRecord(boolean autoincrement){
        if (autoincrement){
			if(MainActivity.getContext() == null){
				this.id=0;
				return;
			}

			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext());

            SharedPreferences.Editor editor = pref.edit();
            long message_id = pref.getLong("MESSAGE_ID_COUNTER", 0L);
            editor.putLong("MESSAGE_ID_COUNTER", message_id + 1L);
            editor.commit();
            this.id = message_id;
		}
    }

    public MessageRecord(Parcel source) {
            this.id = source.readLong();
            this.attack_id = source.readLong();
            this.timestamp = source.readLong();
            this.type = TYPE.valueOf(source.readString());
            this.packet = source.readString(); 
    }

				@Generated(hash = 946933615)
				public MessageRecord(long id, long attack_id, long timestamp, String packet) {
					this.id = id;
					this.attack_id = attack_id;
					this.timestamp = timestamp;
					this.packet = packet;
				}

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(attack_id);
        dest.writeLong(timestamp);
        dest.writeString(type.name());
        dest.writeString(packet);
	}
	
	/**
	 * @return the id
	 */
	@Override
	public long getId() {
		return id;
	}

	/**
	 * @return the attack_id
	 */
	@Override
	public long getAttack_id() {
		return attack_id;
	}
	/**
	 * @param attack_id the attack_id to set
	 */
	@Override
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
	@Override
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	/**
	 * @return the type
	 */
	@Override
	public TYPE getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	@Override
	public void setType(TYPE type) {
		this.type = type;
	}
	/**
	 * @return the packet
	 */
	@Override
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

				/** To-one relationship, resolved on first access. */
				@Generated(hash = 1323618058)
				public AttackRecord getRecord() {
					long __key = this.attack_id;
					if (record__resolvedKey == null || !record__resolvedKey.equals(__key)) {
						final DaoSession daoSession = this.daoSession;
						if (daoSession == null) {
							throw new DaoException("Entity is detached from DAO context");
						}
						AttackRecordDao targetDao = daoSession.getAttackRecordDao();
						AttackRecord recordNew = targetDao.load(__key);
						synchronized (this) {
							record = recordNew;
							record__resolvedKey = __key;
						}
					}
					return record;
				}

				/** called by internal mechanisms, do not call yourself. */
				@Generated(hash = 2111506452)
				public void setRecord(@NotNull AttackRecord record) {
					if (record == null) {
						throw new DaoException("To-one property 'attack_id' has not-null constraint; cannot set to-one to null");
					}
					synchronized (this) {
						this.record = record;
						attack_id = record.getAttack_id();
						record__resolvedKey = attack_id;
					}
				}

				/**
				 * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
				 * Entity must attached to an entity context.
				 */
				@Generated(hash = 128553479)
				public void delete() {
					if (myDao == null) {
						throw new DaoException("Entity is detached from DAO context");
					}
					myDao.delete(this);
				}

				/**
				 * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
				 * Entity must attached to an entity context.
				 */
				@Generated(hash = 1942392019)
				public void refresh() {
					if (myDao == null) {
						throw new DaoException("Entity is detached from DAO context");
					}
					myDao.refresh(this);
				}

				/**
				 * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
				 * Entity must attached to an entity context.
				 */
				@Generated(hash = 713229351)
				public void update() {
					if (myDao == null) {
						throw new DaoException("Entity is detached from DAO context");
					}
					myDao.update(this);
				}

				public void setId(long id) {
					this.id = id;
				}

				public void setId(Long id) {
					this.id = id;
				}

				/** called by internal mechanisms, do not call yourself. */
				@Generated(hash = 450551306)
				public void __setDaoSession(DaoSession daoSession) {
					this.daoSession = daoSession;
					myDao = daoSession != null ? daoSession.getMessageRecordDao() : null;
				}



}
