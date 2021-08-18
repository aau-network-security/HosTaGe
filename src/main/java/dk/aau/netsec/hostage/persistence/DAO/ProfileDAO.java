package dk.aau.netsec.hostage.persistence.DAO;


import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.model.Profile;
import dk.aau.netsec.hostage.model.ProfileDao;

public class ProfileDAO extends DAO {
    private final DaoSession daoSession;

    public ProfileDAO(DaoSession daoSession){
        this.daoSession= daoSession;

    }
    /**
     * Adds a given {@link Profile} to the database.
     *
     * @param record
     *            The added {@link Profile} .
     */
    public  void insert(Profile record){
        ProfileDao recordDao = this.daoSession.getProfileDao();
        insertElement(recordDao,record);
    }

}
