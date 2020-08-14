package de.tudarmstadt.informatik.hostage.persistence.DAO;


import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.model.ProfileDao;

public class ProfileDAO extends DAO {
    private DaoSession daoSession;

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
