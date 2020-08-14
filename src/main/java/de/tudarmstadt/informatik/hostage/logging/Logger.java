package de.tudarmstadt.informatik.hostage.logging;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import de.tudarmstadt.informatik.hostage.HostageApplication;
import de.tudarmstadt.informatik.hostage.persistence.DAO.AttackRecordDAO;
import de.tudarmstadt.informatik.hostage.persistence.DAO.DAOHelper;
import de.tudarmstadt.informatik.hostage.persistence.DAO.MessageRecordDAO;
import de.tudarmstadt.informatik.hostage.persistence.DAO.NetworkRecordDAO;


/**
 * The Logger is used to write the database in dedicated worker threads and implements a message queue.
 * @author Mihai Plasoianu
 * @author Lars Pandikow
 *
 */
public class Logger extends IntentService {

	private static final String ACTION_LOG_MESSAGE = "de.tudarmstadt.informatik.hostage.action.LOG_MESSAGE";
	private static final String ACTION_LOG_ATTACK = "de.tudarmstadt.informatik.hostage.action.LOG_ATTACK";
	private static final String ACTION_LOG_NETWORK = "de.tudarmstadt.informatik.hostage.action.LOG_NETWORK";
	private static final String ACTION_LOG_PORTSCAN = "de.tudarmstadt.informatik.hostage.action.LOG_PORTSCAN";
	private static final String ACTION_LOG_MULTISTAGE = "de.tudarmstadt.informatik.hostage.action.LOG_MULTISTAGE";

	private static final String EXTRA_RECORD = "de.tudarmstadt.informatik.hostage.extra.RECORD";
	private static final String EXTRA_RECORD2 = "de.tudarmstadt.informatik.hostage.extra.RECORD2";
	private static final String EXTRA_RECORD3 = "de.tudarmstadt.informatik.hostage.extra.RECORD3";
	private static final String EXTRA_TIMESTAMP = "de.tudarmstadt.informatik.hostage.extra.TIMESTAMP";

	/**
	 * Adds a single MessageRecord to the Database.
	 * @param context Context needed to access database
	 * @param record The MessageRecord to be added
	 */
	public static void log(Context context, MessageRecord record) {
		Intent intent = new Intent(context, Logger.class);
		intent.setAction(ACTION_LOG_MESSAGE);
		intent.putExtra(EXTRA_RECORD, (Parcelable)record);
		context.startService(intent);
	}
	
	/**
	 * Adds a single AttackRecord to the Database.
	 * @param context Context needed to access database
	 * @param record The AttackRecord to be added
	 */
	public static void log(Context context, AttackRecord record) {
		Intent intent = new Intent(context, Logger.class);
		intent.setAction(ACTION_LOG_ATTACK);
		intent.putExtra(EXTRA_RECORD, (Parcelable)record);
		context.startService(intent);
	}
	
	/**
	 * Adds a single NetworkRecord to the Database.
	 * @param context Context needed to access database
	 * @param record The NetworkRecord to be added
	 */
	public static void log(Context context, NetworkRecord record) {
		Intent intent = new Intent(context, Logger.class);
		intent.setAction(ACTION_LOG_NETWORK);
		intent.putExtra(EXTRA_RECORD, (Parcelable)record);
		context.startService(intent);
	}
	
	/**
	 * Adds a port scan entry to the database
	 * @param context Context needed to access database
	 * @param attackRecord AttackRecord containing the attack information of the port scan
	 * @param netRecord NetworkRecord containing the network information of the port scan
	 * @param timestamp Timestamp of the port scan
	 */
	public static void logPortscan(Context context, AttackRecord attackRecord, NetworkRecord netRecord, long timestamp){
		Intent intent = new Intent(context, Logger.class);
		intent.setAction(ACTION_LOG_PORTSCAN);
		intent.putExtra(EXTRA_RECORD, (Parcelable) attackRecord);
		intent.putExtra(EXTRA_RECORD2, (Parcelable) netRecord);
		intent.putExtra(EXTRA_TIMESTAMP, timestamp);
		context.startService(intent);

	}

	/**
	 * Adds a Multi Stage Attack entry to the database
	 * @param context
	 * @param attackRecord
	 * @param networkRecord
	 * @param timestamp
	 */
	public static void logMultiStageAttack(Context context,AttackRecord attackRecord,NetworkRecord networkRecord, MessageRecord messageRecord, long timestamp){
		Intent intent = new Intent(context, Logger.class);
		intent.setAction(ACTION_LOG_MULTISTAGE);
		intent.putExtra(EXTRA_RECORD, (Parcelable) attackRecord);
		intent.putExtra(EXTRA_RECORD2, (Parcelable)networkRecord);
		intent.putExtra(EXTRA_RECORD3,(Parcelable)messageRecord);
		intent.putExtra(EXTRA_TIMESTAMP, timestamp);
		context.startService(intent);

	}

	private DaoSession dbSession;
	private DAOHelper daoHelper;
	public Logger() {
		super("Logger");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		dbSession = HostageApplication.getInstances().getDaoSession();
		daoHelper = new DAOHelper(dbSession,this);
	}

	private void handleActionLog(MessageRecord record) {
		daoHelper.getMessageRecordDAO().insert(record);
	}
	private void handleActionLog(AttackRecord record) {
		daoHelper.getAttackRecordDAO().addAttackRecord(record);
		daoHelper.getAttackRecordDAO().updateSyncAttackCounter(record);
	}
	private void handleActionLog(NetworkRecord record) {
		daoHelper.getNetworkRecordDAO().insert(record);
	}

	/**
	 * Method to handle the Intent created by the public interface.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();
			if (ACTION_LOG_MESSAGE.equals(action)) {
				final MessageRecord record = intent.getParcelableExtra(EXTRA_RECORD);
				handleActionLog(record);
			}else if(ACTION_LOG_ATTACK.equals(action)){
				final AttackRecord record = intent.getParcelableExtra(EXTRA_RECORD);
				handleActionLog(record);
			}else if(ACTION_LOG_NETWORK.equals(action)){
				final NetworkRecord record = intent.getParcelableExtra(EXTRA_RECORD);
				handleActionLog(record);
			}else if(ACTION_LOG_PORTSCAN.equals(action)){
				final AttackRecord attackRecord = intent.getParcelableExtra(EXTRA_RECORD);
				final NetworkRecord networkRecord = intent.getParcelableExtra(EXTRA_RECORD2);
				attackRecord.setRecord(networkRecord);

				MessageRecord messageRecord = new MessageRecord(true);
				messageRecord.setAttack_id(attackRecord.getAttack_id());
				messageRecord.setRecord(attackRecord);
				//messageRecord.setId(0);
				messageRecord.setPacket("");
				messageRecord.setTimestamp(intent.getLongExtra(EXTRA_TIMESTAMP, 0));
				messageRecord.setType(MessageRecord.TYPE.RECEIVE);
				handleActionLog(attackRecord);
				handleActionLog(networkRecord);
				handleActionLog(messageRecord);
			} else if(ACTION_LOG_MULTISTAGE.equals(action)) {
				final AttackRecord attackRecord = intent.getParcelableExtra(EXTRA_RECORD);
				final NetworkRecord networkRecord = intent.getParcelableExtra(EXTRA_RECORD2);
				final MessageRecord msgRecord = intent.getParcelableExtra(EXTRA_RECORD3);
				msgRecord.setRecord(attackRecord);
				attackRecord.setRecord(networkRecord);

				handleActionLog(attackRecord);
				handleActionLog(networkRecord);
				handleActionLog(msgRecord);

			}
		}
	}

}
