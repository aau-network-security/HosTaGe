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
import android.widget.Toast;
import androidx.preference.PreferenceManager;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.commons.JSONHelper;
import dk.aau.netsec.hostage.logging.formatter.Formatter;
import dk.aau.netsec.hostage.logging.formatter.TraCINgFormatter;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;


/**
 * The LogExport is used to write a text representation of all logs in the database.
 * The Service runs in its own worker thread.
 * @author Lars Pandikow
 */
public class LogExport extends IntentService {
//	public static final String ACTION_EXPORT_DATABASE = "dk.aau.netsec.hostage.logging.ACTION_EXPORT_DATABASE";
//	public static final String FORMAT_EXPORT_DATABASE = "dk.aau.netsec.hostage.logging.FORMAT_EXPORT_DATABASE";

//	static Handler mMainThreadHandler = null;

//	static SharedPreferences pref;
//	static DaoSession dbSession;
//	static DAOHelper daoHelper;
//	public static Formatter formatter;
	public LogExport() {
		super(LogExport.class.getName());

	}
	
	@Override
	public void onCreate() {
		super.onCreate();
//		pref = PreferenceManager.getDefaultSharedPreferences(this);
//		dbSession = HostageApplication.getInstances().getDaoSession();
//		daoHelper = new DAOHelper(dbSession,this);
//		mMainThreadHandler = new Handler();
	}
	
	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns, IntentService
	 * stops the service, as appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
//		if (intent != null) {
//			final String action = intent.getAction();
//
//			if (ACTION_EXPORT_DATABASE.equals(action)) {
//				final int format = intent.getIntExtra(FORMAT_EXPORT_DATABASE, 0);
//				formatter = (format == 0 ? TraCINgFormatter.getInstance() : null);
//
//				Intent filipsIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
//
//				filipsIntent.setType("application/json");
//
//				filipsIntent.putExtra(Intent.EXTRA_TITLE, getFileName("file",".json"));
//
////				startActivityForResult
//
//
////				if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
////					if(format == 0) {
////						exportDatabase(formatter);
////					} else if(format == 1) {
////						exportJSONFormat();
////					}
////				}
////				else{
////					Toast.makeText(this, "Could not write out to storage", Toast.LENGTH_SHORT).show();
////				}
//			}
//		}
	}

	private void exportJSONFormat(){
//		JSONHelper jsonHelper = new JSONHelper();
//		try {
//			File file = getDirFile("file", ".json");
//			String filename = getFileName("file",".json");
//			ArrayList<RecordAll> records = daoHelper.getAttackRecordDAO().getAllRecords();
//			jsonHelper.jsonWriter(records,file);
//
//			makeToast(filename+" saved on external (if you have an sd card) or internal memory! ", Toast.LENGTH_LONG);
//		}catch (Exception e){
//			makeToast("Could not write to a JSON File in SD Card or Internal Storage",Toast.LENGTH_SHORT);
//		}

	}

//	private static File getDirFile(String format,String extension){
//		String filename = getFileName(format,extension);
//		String externalLocation = pref.getString("pref_external_location", "");
//
//		String root = getExternalStoragePath();
//		File dir = new File(root + externalLocation);
//		dir.mkdirs();
//		File file = new File(dir, filename);
//
//		return file;
//	}

//	private static String getExternalStoragePath(){
//		String path = System.getenv("SECONDARY_STORAGE"); //SD card
//		if(path!=null && !path.trim().isEmpty()){
//			return path;
//		}
//		return System.getenv("EXTERNAL_STORAGE"); //internal Storage
//	}
//
//	public static String getFileName(String format,String extension){
//		return "hostage_" + (format) + "_"+ System.currentTimeMillis() + extension;
//	}
	
	/**
	 * Exports all records in a given format. Before exporting checks export
	 * hostage.location from preferences.
	 * 
	 * @param format Integer coded export format
	 * @see RecordAll #toString(int)
	 */
//	public static void exportDatabase(Formatter format) {
//		try {
//			FileOutputStream log;
//			String filename = getFileName(format.toString(),".log");
//			log = new FileOutputStream(getDirFile(format.toString(),".log"));
//
//			ArrayList<RecordAll> records = daoHelper.getAttackRecordDAO().getAllRecords();
//			for (RecordAll record : records) {
//				log.write((record.toString(format)).getBytes());
//			}
//			log.flush();
//			log.close();
//			makeToast(filename + " saved on external (if you have an sd card) or internal memory! ", Toast.LENGTH_LONG);
//		} catch (Exception e) {
//			makeToast("Could not write to SD Card or Internal Storage", Toast.LENGTH_SHORT);
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 * Checks if external storage is available for read and write.
//	 *
//	 * @return True if external storage is available for read and write, else
//	 *         false.
//	 */
//	private static boolean isExternalStorageWritable() {
//		String state = Environment.getExternalStorageState();
//        return Environment.MEDIA_MOUNTED.equals(state);
//    }
//
//	private static void makeToast(final String text, final int length){
//		mMainThreadHandler.post(() -> Toast.makeText(Hostage.getContext(), text, length).show());
//	}

}
