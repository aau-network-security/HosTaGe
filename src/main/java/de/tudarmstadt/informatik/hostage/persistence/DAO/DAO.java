package de.tudarmstadt.informatik.hostage.persistence.DAO;

import android.os.Build;
import android.support.annotation.RequiresApi;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import de.tudarmstadt.informatik.hostage.logging.DaoSession;


public abstract class DAO {
    public <T> List<T> selectElements(AbstractDao<T, ?> dao) {
        if (dao == null) {
            return null;
        }
        QueryBuilder<T> qb = dao.queryBuilder();
        return qb.list();
    }

    public <T> List<T> selectElementsOffset(AbstractDao<T, ?> dao,int offset,int limit) {
        if (dao == null) {
            return null;
        }
        QueryBuilder<T> qb = dao.queryBuilder();
        qb.offset(offset).limit(limit).build();
        return qb.list();
    }

    public <T> void insertElements(AbstractDao<T, ?> absDao, List<T> items) {
        if (items == null || items.size() == 0 || absDao == null) {
            return;
        }
        absDao.insertOrReplaceInTx(items);
    }

    public <T> T insertElement(AbstractDao<T, ?> absDao, T item) {
        if (item == null || absDao == null) {
            return null;
        }
        absDao.insertOrReplaceInTx(item);
        return item;
    }

    public <T> void updateElements(AbstractDao<T, ?> absDao, List<T> items) {
        if (items == null || items.size() == 0 || absDao == null) {
            return;
        }
        absDao.updateInTx(items);
    }


    public <T> void updateElement(AbstractDao<T, ?> absDao,T item) {
        if (item == null || absDao == null) {
            return;
        }
        absDao.updateInTx(item);
    }

    public <T> T selectElementByCondition(AbstractDao<T, ?> absDao,
                                          WhereCondition... conditions) {
        if (absDao == null) {
            return null;
        }
        QueryBuilder<T> qb = absDao.queryBuilder();
        for (WhereCondition condition : conditions) {
            qb = qb.where(condition);
        }
        List<T> items = qb.list();
        return items != null && items.size() > 0 ? items.get(0) : null;
    }

    public <T> List<T> selectElementsByCondition(AbstractDao<T, ?> absDao,
                                                 WhereCondition... conditions) {
        if (absDao == null) {
            return null;
        }
        QueryBuilder<T> qb = absDao.queryBuilder();
        for (WhereCondition condition : conditions) {
            qb = qb.where(condition);
        }
        List<T> items = qb.list();
        return items != null ? items : null;
    }

    public <T> List<T> selectElementsByConditionAndSort(AbstractDao<T, ?> absDao,
                                                        Property sortProperty,
                                                        String sortStrategy,
                                                        WhereCondition... conditions) {
        if (absDao == null) {
            return null;
        }
        QueryBuilder<T> qb = absDao.queryBuilder();
        for (WhereCondition condition : conditions) {
            qb = qb.where(condition);
        }
        qb.orderCustom(sortProperty, sortStrategy);
        List<T> items = qb.list();
        return items != null ? items : null;
    }

    public <T> List<T> selectElementsByConditionAndSortWithNullHandling(AbstractDao<T, ?> absDao,
                                                                        Property sortProperty,
                                                                        boolean handleNulls,
                                                                        String sortStrategy,
                                                                        WhereCondition... conditions) {
        if (!handleNulls) {
            return selectElementsByConditionAndSort(absDao, sortProperty, sortStrategy, conditions);
        }
        if (absDao == null) {
            return null;
        }
        QueryBuilder<T> qb = absDao.queryBuilder();
        for (WhereCondition condition : conditions) {
            qb = qb.where(condition);
        }
        qb.orderRaw("(CASE WHEN " + "T." + sortProperty.columnName + " IS NULL then 1 ELSE 0 END)," + "T." + sortProperty.columnName + " " + sortStrategy);
        List<T> items = qb.list();
        return items != null ? items : null;
    }

    public <T, V extends Class> List<T> selectByJoin(AbstractDao<T, ?> absDao,
                                                     V className,
                                                     Property property, WhereCondition whereCondition) {
        QueryBuilder<T> qb = absDao.queryBuilder();
        qb.join(className, property).where(whereCondition);
        return qb.list();
    }

    public <T> void deleteElementsByCondition(AbstractDao<T, ?> absDao,
                                              WhereCondition... conditions) {
        if (absDao == null) {
            return;
        }
        QueryBuilder<T> qb = absDao.queryBuilder();
        for (WhereCondition condition : conditions) {
            qb = qb.where(condition);
        }
        List<T> list = qb.list();
        absDao.deleteInTx(list);
    }

    public <T> T deleteElement(DaoSession session, AbstractDao<T, ?> absDao, T object) {
        if (absDao == null) {
            return null;
        }
        absDao.delete(object);
        session.clear();
        return object;
    }

    public <T, V extends Class> void deleteByJoin(AbstractDao<T, ?> absDao,
                                                  V className,
                                                  Property property, WhereCondition whereCondition) {
        QueryBuilder<T> qb = absDao.queryBuilder();
        qb.join(className, property).where(whereCondition);
        qb.buildDelete().executeDeleteWithoutDetachingEntities();
    }

    public <T> void deleteAllFromTable(AbstractDao<T, ?> absDao) {
        if (absDao == null) {
            return;
        }
        absDao.deleteAll();
    }

    public <T> long countElements(AbstractDao<T, ?> absDao) {
        if (absDao == null) {
            return 0;
        }
        return absDao.count();
    }

public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
