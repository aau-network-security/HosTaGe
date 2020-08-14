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

/**
 * Holds all necessary information about a single attack.
 */
@Entity(nameInDb = "attack")
public class AttackRecord extends  RecordAll implements Parcelable, Serializable{
	@Transient
	private static final long serialVersionUID = 6111024905373724227L;
	@Id(autoincrement = true)
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
	private boolean wasInternalAttack= true;
    @ToOne(joinProperty = "bssid")
	private NetworkRecord record;

	@ToOne(joinProperty = "device")
	private SyncDevice syncDevice;


	public static final Parcelable.Creator<AttackRecord> CREATOR = new Parcelable.Creator<AttackRecord>() {
		@Override
		public AttackRecord createFromParcel(Parcel source) {
			return new AttackRecord(source);
		}

		@Override
		public AttackRecord[] newArray(int size) {
			return new AttackRecord[size];
		}
	};
	/** Used to resolve relations */
	@Generated(hash = 2040040024)
	private transient DaoSession daoSession;
	/** Used for active entity operations. */
	@Generated(hash = 1891517836)
	private transient AttackRecordDao myDao;
	@Generated(hash = 1541759297)
	private transient String record__resolvedKey;
	@Generated(hash = 768043601)
	private transient String syncDevice__resolvedKey;


	public AttackRecord(Parcel source) {
		this.attack_id = source.readLong();
		this.protocol = source.readString();
		this.localIP = source.readString();
		this.localPort = source.readInt();
		this.remoteIP = source.readString();
		this.remotePort = source.readInt();
		this.externalIP = source.readString();
		this.wasInternalAttack = source.readByte() != 0;
		this.bssid = source.readString();
        this.device = source.readString();
        this.sync_id = source.readLong();
	}

    public AttackRecord(boolean autoincrement){
        if (autoincrement){
        	if(MainActivity.getContext() == null){
				this.attack_id = 0;
				this.sync_id = 0;

				return;
			}

			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext());

            SharedPreferences.Editor editor = pref.edit();
            Long attack_id = pref.getLong("ATTACK_ID_COUNTER", 0);
            editor.putLong("ATTACK_ID_COUNTER", attack_id + 1);
            editor.commit();
            this.attack_id = attack_id;
            this.sync_id = attack_id;

            SyncDevice currentDevice = SyncDevice.currentDevice();
			if(currentDevice!=null) {
				currentDevice.setHighest_attack_id(attack_id);
				this.setDevice(currentDevice.getDeviceID());
			}
        }

    }

				@Generated(hash = 392632362)
				public AttackRecord(long attack_id, long sync_id, String bssid, String device, String protocol,
						String localIP, int localPort, String remoteIP, int remotePort, String externalIP,
						boolean wasInternalAttack) {
					this.attack_id = attack_id;
					this.sync_id = sync_id;
					this.bssid = bssid;
					this.device = device;
					this.protocol = protocol;
					this.localIP = localIP;
					this.localPort = localPort;
					this.remoteIP = remoteIP;
					this.remotePort = remotePort;
					this.externalIP = externalIP;
					this.wasInternalAttack = wasInternalAttack;
				}

				@Generated(hash = 1792156255)
				public AttackRecord() {
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
		dest.writeByte((byte) (wasInternalAttack ? 1 : 0));
		dest.writeString(bssid);
        dest.writeString(device);
        dest.writeLong(sync_id);
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

	public boolean getWasInternalAttack() {return wasInternalAttack;}

	public void setWasInternalAttack(boolean wasInternalAttack) {
		this.wasInternalAttack = wasInternalAttack;
	}

	/** To-one relationship, resolved on first access. */
	@Generated(hash = 234930298)
	public NetworkRecord getRecord() {
		String __key = this.bssid;
		if (record__resolvedKey == null || record__resolvedKey != __key) {
			final DaoSession daoSession = this.daoSession;
			if (daoSession == null) {
				throw new DaoException("Entity is detached from DAO context");
			}
			NetworkRecordDao targetDao = daoSession.getNetworkRecordDao();
			NetworkRecord recordNew = targetDao.load(__key);
			synchronized (this) {
				record = recordNew;
				record__resolvedKey = __key;
			}
		}
		return record;
	}

	/** called by internal mechanisms, do not call yourself. */
	@Generated(hash = 502198762)
	public void setRecord(NetworkRecord record) {
		synchronized (this) {
			this.record = record;
			bssid = record == null ? null : record.getBssid();
			record__resolvedKey = bssid;
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

	/** To-one relationship, resolved on first access. */
	@Generated(hash = 555758685)
	public SyncDevice getSyncDevice() {
		String __key = this.device;
		if (syncDevice__resolvedKey == null || syncDevice__resolvedKey != __key) {
			final DaoSession daoSession = this.daoSession;
			if (daoSession == null) {
				throw new DaoException("Entity is detached from DAO context");
			}
			SyncDeviceDao targetDao = daoSession.getSyncDeviceDao();
			SyncDevice syncDeviceNew = targetDao.load(__key);
			synchronized (this) {
				syncDevice = syncDeviceNew;
				syncDevice__resolvedKey = __key;
			}
		}
		return syncDevice;
	}

	/** called by internal mechanisms, do not call yourself. */
	@Generated(hash = 1137344764)
	public void setSyncDevice(SyncDevice syncDevice) {
		synchronized (this) {
			this.syncDevice = syncDevice;
			device = syncDevice == null ? null : syncDevice.getDeviceID();
			syncDevice__resolvedKey = device;
		}
	}

	public void setAttack_id(Long attack_id) {
		this.attack_id = attack_id;
	}

	/** called by internal mechanisms, do not call yourself. */
	@Generated(hash = 1925184755)
	public void __setDaoSession(DaoSession daoSession) {
		this.daoSession = daoSession;
		myDao = daoSession != null ? daoSession.getAttackRecordDao() : null;
	}


}
