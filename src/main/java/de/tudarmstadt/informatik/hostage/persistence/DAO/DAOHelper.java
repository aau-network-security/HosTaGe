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

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public void setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
    }

    public MessageRecordDAO getMessageRecordDAO() {
        return messageRecordDAO;
    }

    public void setMessageRecordDAO(MessageRecordDAO messageRecordDAO) {
        this.messageRecordDAO = messageRecordDAO;
    }

    public NetworkRecordDAO getNetworkRecordDAO() {
        return networkRecordDAO;
    }

    public void setNetworkRecordDAO(NetworkRecordDAO networkRecordDAO) {
        this.networkRecordDAO = networkRecordDAO;
    }

    public ProfileDAO getProfileDAO() {
        return profileDAO;
    }

    public void setProfileDAO(ProfileDAO profileDAO) {
        this.profileDAO = profileDAO;
    }

    public SyncInfoRecordDAO getSyncInfoRecordDAO() {
        return syncInfoRecordDAO;
    }

    public void setSyncInfoRecordDAO(SyncInfoRecordDAO syncInfoRecordDAO) {
        this.syncInfoRecordDAO = syncInfoRecordDAO;
    }

    public AttackRecordDAO getAttackRecordDAO() {
        return attackRecordDAO;
    }

    public void setAttackRecordDAO(AttackRecordDAO attackRecordDAO) {
        this.attackRecordDAO = attackRecordDAO;
    }

    public SyncDeviceDAO getSyncDeviceDAO() {
        return syncDeviceDAO;
    }

    public void setSyncDeviceDAO(SyncDeviceDAO syncDeviceDAO) {
        this.syncDeviceDAO = syncDeviceDAO;
    }
}
