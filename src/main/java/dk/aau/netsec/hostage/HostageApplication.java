package dk.aau.netsec.hostage;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import dk.aau.netsec.hostage.logging.DaoMaster;
import dk.aau.netsec.hostage.logging.DaoSession;

/**
 * Created by Fabio Arnold on 28.03.14.
 */
public class HostageApplication extends Application {
	private Tracker mAppTracker = null;
    private SQLiteDatabase db;
    private DaoSession mDaoSession;
	public static HostageApplication instances;


	public synchronized Tracker getTracker() {
		GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
		if (mAppTracker == null) {
			mAppTracker = analytics.newTracker(R.xml.app_tracker);
		}
		return mAppTracker;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		instances = this;
		setDatabase();
		crashlyticsSetup();
	}

	private void crashlyticsSetup(){
		FirebaseApp.initializeApp(this);
		FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
		crashlytics.sendUnsentReports();
		//Crashlytics.getInstance().crash(); //force crash to check-only for testing!
	}
	/**
	 * Setting up green Dao
	 */
	private void setDatabase() {
		// Note: The default DaoMaster.DevOpenHelper deletes all tables when the database is upgraded, meaning that this will result in data loss.
        DaoMaster.DevOpenHelper mHelper = new DaoMaster.DevOpenHelper(this, "hostage-db", null);
		db = mHelper.getWritableDatabase();
		// Note: This database connection belongs to DaoMaster, so multiple sessions refer to the same database connection.
        DaoMaster mDaoMaster = new DaoMaster(db);
		mDaoSession = mDaoMaster.newSession();

	}
	public static HostageApplication getInstances(){
		return instances;
	}

	public DaoSession getDaoSession() {
		return mDaoSession;
	}
	public SQLiteDatabase getDb() {
		return db;
	}
}
