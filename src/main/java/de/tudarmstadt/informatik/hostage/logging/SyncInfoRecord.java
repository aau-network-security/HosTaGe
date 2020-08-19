package de.tudarmstadt.informatik.hostage.logging;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.DaoException;

/**
 * Holds the Information a specific device gathered about a network identified by its BSSID.
 * @author Lars Pandikow
 */
@Entity
public class SyncInfoRecord implements Serializable{

	private static final long serialVersionUID = 7156818788190434192L;
	@Id
	private String deviceID;
	private String BSSID;
	private long number_of_attacks;
	private long number_of_portscans;
    @ToOne(joinProperty = "BSSID")
    private NetworkRecord record;
				/** Used to resolve relations */
				@Generated(hash = 2040040024)
				private transient DaoSession daoSession;
				/** Used for active entity operations. */
				@Generated(hash = 393982986)
				private transient SyncInfoRecordDao myDao;
				@Generated(hash = 1541759297)
				private transient String record__resolvedKey;

	@Generated(hash = 1640797190)
	public SyncInfoRecord(String deviceID, String BSSID, long number_of_attacks,
			long number_of_portscans) {
		this.deviceID = deviceID;
		this.BSSID = BSSID;
		this.number_of_attacks = number_of_attacks;
		this.number_of_portscans = number_of_portscans;
	}
	@Generated(hash = 1014952315)
	public SyncInfoRecord() {
	}

	/**
	 * @return the deviceID
	 */
	public String getDeviceID() {
		return deviceID;
	}
	/**
	 * @param deviceID the deviceID to set
	 */
	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}
	/**
	 * @return the bSSID
	 */
	public String getBSSID() {
		return BSSID;
	}
	/**
	 * @param bSSID the bSSID to set
	 */
	public void setBSSID(String bSSID) {
		BSSID = bSSID;
	}
	/**
	 * @return the number_of_attacks
	 */
	public long getNumber_of_attacks() {
		return number_of_attacks;
	}
	/**
	 * @param number_of_attacks the number_of_attacks to set
	 */
	public void setNumber_of_attacks(long number_of_attacks) {
		this.number_of_attacks = number_of_attacks;
	}
	/**
	 * @return the number_of_portscans
	 */
	public long getNumber_of_portscans() {
		return number_of_portscans;
	}
	/**
	 * @param number_of_portscans the number_of_portscans to set
	 */
	public void setNumber_of_portscans(long number_of_portscans) {
		this.number_of_portscans = number_of_portscans;
	}
	/** To-one relationship, resolved on first access. */
	@Generated(hash = 549930765)
	public NetworkRecord getRecord() {
		String __key = this.BSSID;
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
	@Generated(hash = 1503985823)
	public void setRecord(NetworkRecord record) {
		synchronized (this) {
			this.record = record;
			BSSID = record == null ? null : record.getBssid();
			record__resolvedKey = BSSID;
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
	/** called by internal mechanisms, do not call yourself. */
	@Generated(hash = 650855813)
	public void __setDaoSession(DaoSession daoSession) {
		this.daoSession = daoSession;
		myDao = daoSession != null ? daoSession.getSyncInfoRecordDao() : null;
	}

}
