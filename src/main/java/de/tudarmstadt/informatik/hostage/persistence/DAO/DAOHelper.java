package de.tudarmstadt.informatik.hostage.persistence.DAO;

import android.content.Context;

import de.tudarmstadt.informatik.hostage.logging.DaoSession;

public class DAOHelper {

    private DaoSession daoSession;
    private MessageRecordDAO messageRecordDAO;
    private NetworkRecordDAO networkRecordDAO;
    private ProfileDAO profileDAO;
    private SyncInfoRecordDAO syncInfoRecordDAO;
    private AttackRecordDAO attackRecordDAO;
    private SyncDeviceDAO syncDeviceDAO;

    public DAOHelper(DaoSession daoSession, Context context){
        this.daoSession= daoSession;
        this.messageRecordDAO = new MessageRecordDAO(daoSession);
        this.networkRecordDAO = new NetworkRecordDAO(daoSession);
        this.profileDAO = new ProfileDAO(daoSession);
        this.syncInfoRecordDAO = new SyncInfoRecordDAO(daoSession);
        this.attackRecordDAO = new AttackRecordDAO(daoSession,context);
        this.syncDeviceDAO = new SyncDeviceDAO(daoSession,context);

    }

    public DAOHelper(DaoSession daoSession){
        this.daoSession= daoSession;
        this.messageRecordDAO = new MessageRecordDAO(daoSession);
        this.networkRecordDAO = new NetworkRecordDAO(daoSession);
        this.profileDAO = new ProfileDAO(daoSession);
        this.syncInfoRecordDAO = new SyncInfoRecordDAO(daoSession);
        this.attackRecordDAO = new AttackRecordDAO(daoSession);
        this.syncDeviceDAO = new SyncDeviceDAO(daoSession);

    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public void setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
    }

    public MessageRecordDAO getMessageRecordDAO() {
        return messageRecordDAO;
    }

    public NetworkRecordDAO getNetworkRecordDAO() {
        return networkRecordDAO;
    }

    public ProfileDAO getProfileDAO() {
        return profileDAO;
    }

    public SyncInfoRecordDAO getSyncInfoRecordDAO() {
        return syncInfoRecordDAO;
    }

    public AttackRecordDAO getAttackRecordDAO() {
        return attackRecordDAO;
    }

    public SyncDeviceDAO getSyncDeviceDAO() {
        return syncDeviceDAO;
    }

}
