package de.tudarmstadt.informatik.hostage.model;

import org.greenrobot.greendao.test.AbstractDaoTestLongPk;

import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.model.ProfileDao;

public class ProfileTest extends AbstractDaoTestLongPk<ProfileDao, Profile> {

    public ProfileTest() {
        super(ProfileDao.class);
    }

    @Override
    protected Profile createEntity(Long key) {
        Profile entity = new Profile();
        entity.setId(key);
//        entity.setId();
//        entity.setMActivated();
//        entity.setMEditable();
        return entity;
    }

}
