package de.tudarmstadt.informatik.hostage.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;


import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;


import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.HostageApplication;
import de.tudarmstadt.informatik.hostage.logging.formatter.Formatter;
import de.tudarmstadt.informatik.hostage.logging.formatter.TraCINgFormatter;
import de.tudarmstadt.informatik.hostage.persistence.DAO.AttackRecordDAO;
import de.tudarmstadt.informatik.hostage.persistence.DAO.DAOHelper;

/**
 * The LogExport is used to write a text representation of all logs in the database.
 * The Service runs in its own worker thread.
 * @author Lars Pandikow
 */
public class LogExport extends IntentService{
	
	public static final String ACTION_EXPORT_DATABASE = "de.tudarmstadt.informatik.hostage.hostage.logging.ACTION_EXPORT_DATABASE";
	public static final String FORMAT_EXPORT_DATABASE = "de.tudarmstadt.informatik.hostage.hostage.logging.FORMAT_EXPORT_DATABASE";

	Handler mMainThreadHandler = null;

	SharedPreferences pref;
	//HostageDBOpenHelper dbh;
	DaoSession dbSession;
	//AttackRecordDAO attackRecordDAO;
	DAOHelper daoHelper;
	
	public LogExport() {
		super(LogExport.class.getName());
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		//dbh = new HostageDBOpenHelper(this);
		dbSession = HostageApplication.getInstances().getDaoSession();
		daoHelper = new DAOHelper(dbSession,this);
		mMainThreadHandler = new Handler();
	}
	
	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns, IntentService
	 * stops the service, as appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();

			if (ACTION_EXPORT_DATABASE.equals(action)) {
				final int format = intent.getIntExtra(FORMAT_EXPORT_DATABASE, 0);
				Formatter formatter = (format == 1 ? TraCINgFormatter.getInstance() : null);
				exportDatabase(formatter);
			}
		}
	}
	
	/**
	 * Exports all records in a given format. Before exporting checks export
	 * hostage.location from preferences.
	 * 
	 * @param format
	 *            Integer coded export format
	 * @see Record #toString(int)
	 */
	private void exportDatabase(Formatter format) {
		try {
			FileOutputStream log;
			String filename = "hostage_" + (format == null ? "default" : format.toString()) + "_"+ System.currentTimeMillis() + ".log";
			String externalLocation = pref.getString("pref_external_location", "");
            String root = Environment.getExternalStorageDirectory().toString();
			if (root != null && isExternalStorageWritable()) {
				File dir = new File(root + externalLocation);
				dir.mkdirs();
				File file = new File(dir, filename);
				log = new FileOutputStream(file);
			} else {
				makeToast("Could not write to SD Card",Toast.LENGTH_SHORT);
				return;
			}

			ArrayList<RecordAll> records = daoHelper.getAttackRecordDAO().getAllRecords();
			for (RecordAll record : records) {
				log.write((record.toString(format)).getBytes());
			}
			log.flush();
			log.close();
			makeToast(filename + " saved on external memory! ", Toast.LENGTH_LONG);
		} catch (Exception e) {
			makeToast("Could not write to SD Card", Toast.LENGTH_SHORT);
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks if external storage is available for read and write.
	 * 
	 * @return True if external storage is available for read and write, else
	 *         false.
	 */
	private boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

	private void makeToast(final String text, final int length){
		mMainThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), text, length).show();
			}
		});
	}
}
