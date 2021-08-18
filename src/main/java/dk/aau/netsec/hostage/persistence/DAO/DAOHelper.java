package dk.aau.netsec.hostage.persistence.DAO;

import android.content.Context;

import dk.aau.netsec.hostage.logging.DaoSession;

public class DAOHelper {

    private DaoSession daoSession;
    private final MessageRecordDAO messageRecordDAO;
    private final NetworkRecordDAO networkRecordDAO;
    private final ProfileDAO profileDAO;
    private final SyncInfoRecordDAO syncInfoRecordDAO;
    private final AttackRecordDAO attackRecordDAO;
    private final SyncDeviceDAO syncDeviceDAO;

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
