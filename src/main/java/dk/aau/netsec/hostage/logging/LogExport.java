package dk.aau.netsec.hostage.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;


import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;


import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.logging.formatter.Formatter;
import dk.aau.netsec.hostage.logging.formatter.TraCINgFormatter;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;


/**
 * The LogExport is used to write a text representation of all logs in the database.
 * The Service runs in its own worker thread.
 * @author Lars Pandikow
 */
public class LogExport extends IntentService {
	public static final String ACTION_EXPORT_DATABASE = "dk.aau.netsec.hostage.logging.ACTION_EXPORT_DATABASE";
	public static final String FORMAT_EXPORT_DATABASE = "dk.aau.netsec.hostage.logging.FORMAT_EXPORT_DATABASE";

	static Handler mMainThreadHandler = null;

	static SharedPreferences pref;
	static DaoSession dbSession;
	static DAOHelper daoHelper;
	public static Formatter formatter;
	
	public LogExport() {
		super(LogExport.class.getName());

	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		pref = PreferenceManager.getDefaultSharedPreferences(this);
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
				formatter = (format == 1 ? TraCINgFormatter.getInstance() : null);
				if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
					exportDatabase(formatter);
				}
			}
		}
	}
	
	/**
	 * Exports all records in a given format. Before exporting checks export
	 * hostage.location from preferences.
	 * 
	 * @param format
	 *            Integer coded export format
	 * @see RecordAll #toString(int)
	 */
	public static void exportDatabase(Formatter format) {
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
	private static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

	private static void makeToast(final String text, final int length){
		mMainThreadHandler.post(() -> Toast.makeText(Hostage.getContext(), text, length).show());
	}

}
