package dk.aau.netsec.hostage.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import org.greenrobot.greendao.DaoLog;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.logging.AttackRecordDao;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.logging.MessageRecordDao;
import dk.aau.netsec.hostage.logging.NetworkRecordDao;

public class HostageContentProvider extends ContentProvider {

	public static final String AUTHORITY = "dk.aau.netsec.hostage.provider";

	public static final String BASE_PATH = "";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

	public static final Uri CONTENT_URI_NETWORK = Uri.parse("content://" + AUTHORITY + "/network");
	public static final Uri CONTENT_URI_ATTACK = Uri.parse("content://" + AUTHORITY + "/attack");
	public static final Uri CONTENT_URI_PACKET = Uri.parse("content://" + AUTHORITY + "/packet");

	public static final Uri CONTENT_URI_RECORD_OVERVIEW = Uri.parse("content://" + AUTHORITY + "/record-overview");

	private static final int NETWORK_ALL = 11;
	private static final int NETWORK_ONE = 12;
	private static final int ATTACK_ALL = 21;
	private static final int ATTACK_ONE = 22;
	private static final int PACKET_ALL = 31;
	private static final int PACKET_ONE = 32;

	private static final int NETWORK_OVERVIEW_ALL = 101;
	private static final int NETWORK_OVERVIEW_ONE = 102;

	private static final UriMatcher uriMatcher;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "network", NETWORK_ALL);
		uriMatcher.addURI(AUTHORITY, "network/#", NETWORK_ONE);
		uriMatcher.addURI(AUTHORITY, "attack", ATTACK_ALL);
		uriMatcher.addURI(AUTHORITY, "attack/#", ATTACK_ONE);
		uriMatcher.addURI(AUTHORITY, "packet", PACKET_ALL);
		uriMatcher.addURI(AUTHORITY, "packet/#", PACKET_ONE);
		uriMatcher.addURI(AUTHORITY, "record-overview", NETWORK_OVERVIEW_ALL);
		uriMatcher.addURI(AUTHORITY, "record-overview/#", NETWORK_OVERVIEW_ONE);
	}

	//private HostageDBOpenHelper mDBOpenHelper;
	private DaoSession daoSession = null;


	@Override
	public boolean onCreate() {
		//mDBOpenHelper = new HostageDBOpenHelper(getContext());
		daoSession = ((HostageApplication) getContext()).getDaoSession();

		DaoLog.d("Content Provider started: " + CONTENT_URI);

		return true;
	}

	protected SQLiteDatabase getDatabase() {
		if(daoSession == null) {
			throw new IllegalStateException("DaoSession must be set during content provider is active");
		}
		return ((HostageApplication) getContext()).getDb();
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		//SQLiteDatabase db = mDBOpenHelper.getWritableDatabase();
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		int uriMatch = uriMatcher.match(uri);

		if (isNetworkUriMatch(uriMatch)) {
			queryBuilder.setTables(NetworkRecordDao.TABLENAME);
		} else if (isAttackUriMatch(uriMatch)) {
			queryBuilder.setTables(AttackRecordDao.TABLENAME);
		} else if (isPacketUriMatch(uriMatch)) {
			queryBuilder.setTables(MessageRecordDao.TABLENAME);
		}

		if (uriMatch == NETWORK_ONE) {
			String rowID = uri.getPathSegments().get(1);
			queryBuilder.appendWhere(NetworkRecordDao.Properties.Bssid + "=" + rowID);
		} else if (uriMatch == ATTACK_ONE) {
			String rowID = uri.getPathSegments().get(1);
			queryBuilder.appendWhere(AttackRecordDao.Properties.Attack_id + "=" + rowID);
		} else if (uriMatch == PACKET_ONE) {
			String rowID = uri.getPathSegments().get(1);
			queryBuilder.appendWhere(MessageRecordDao.Properties.Id + "=" + rowID);
		}
		SQLiteDatabase db = getDatabase();
        return queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = getDatabase();

		int uriMatch = uriMatcher.match(uri);

		if (uriMatch == NETWORK_ONE) {
			String rowID = uri.getPathSegments().get(1);
			selection = NetworkRecordDao.Properties.Bssid  + "=" + rowID + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "");
		} else if (uriMatch == ATTACK_ONE) {
			String rowID = uri.getPathSegments().get(1);
			selection = AttackRecordDao.Properties.Attack_id + "=" + rowID + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "");
		} else if (uriMatch == PACKET_ONE) {
			String rowID = uri.getPathSegments().get(1);
			selection = MessageRecordDao.Properties.Id + "=" + rowID + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "");
		}

		if (selection == null) {
			selection = "1";
		}

		int deleteCount = 0;
		if (isNetworkUriMatch(uriMatch)) {
			deleteCount = db.delete(NetworkRecordDao.TABLENAME, selection, selectionArgs);
		} else if (isAttackUriMatch(uriMatch)) {
			deleteCount = db.delete(AttackRecordDao.TABLENAME, selection, selectionArgs);
		} else if (isPacketUriMatch(uriMatch)) {
			deleteCount = db.delete(MessageRecordDao.TABLENAME, selection, selectionArgs);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return deleteCount;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = getDatabase();

		int uriMatch = uriMatcher.match(uri);

		long id = -1;
		Uri insertedId = null;
		if (isNetworkUriMatch(uriMatch)) {
			id = db.insert(NetworkRecordDao.TABLENAME, null, values);
			if (id > -1) {
				insertedId = ContentUris.withAppendedId(CONTENT_URI_NETWORK, id);
			}
		} else if (isAttackUriMatch(uriMatch)) {
			id = db.insert(AttackRecordDao.TABLENAME, null, values);
			if (id > -1) {
				insertedId = ContentUris.withAppendedId(CONTENT_URI_ATTACK, id);
			}
		} else if (isPacketUriMatch(uriMatch)) {
			id = db.insert(MessageRecordDao.TABLENAME, null, values);
			if (id > -1) {
				insertedId = ContentUris.withAppendedId(CONTENT_URI_PACKET, id);
			}
		}

		if (id > -1) {
			getContext().getContentResolver().notifyChange(insertedId, null);
			return insertedId;
		}

		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = getDatabase();

		int uriMatch = uriMatcher.match(uri);

		if (uriMatch == NETWORK_ONE) {
			String rowID = uri.getPathSegments().get(1);
			selection = NetworkRecordDao.Properties.Bssid + "=" + rowID + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "");
		} else if (uriMatch == ATTACK_ONE) {
			String rowID = uri.getPathSegments().get(1);
			selection =  AttackRecordDao.Properties.Attack_id + "=" + rowID + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "");
		} else if (uriMatch == PACKET_ONE) {
			String rowID = uri.getPathSegments().get(1);
			selection = MessageRecordDao.Properties.Id  + "=" + rowID + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ")" : "");
		}

		int updateCount = 0;
		if (isNetworkUriMatch(uriMatch)) {
			updateCount = db.update(NetworkRecordDao.TABLENAME, values, selection, selectionArgs);
		} else if (isAttackUriMatch(uriMatch)) {
			updateCount = db.update(AttackRecordDao.TABLENAME, values, selection, selectionArgs);
		} else if (isPacketUriMatch(uriMatch)) {
			updateCount = db.update(MessageRecordDao.TABLENAME, values, selection, selectionArgs);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return updateCount;
	}

	@Override
	public String getType(Uri uri) {
		int uriMatch = uriMatcher.match(uri);

		if (isNetworkUriMatch(uriMatch)) {
			if (uriMatch == NETWORK_ONE) {
				return "vnd.android.cursor.item/vnd.dk.aau.netsec.hostage.provider.network";
			}
			return "vnd.android.cursor.dir/vnd.dk.aau.netsec.hostage.provider.network";
		} else if (isAttackUriMatch(uriMatch)) {
			if (uriMatch == ATTACK_ONE) {
				return "vnd.android.cursor.item/vnd.dk.aau.netsec.hostage.provider.attack";
			}
			return "vnd.android.cursor.dir/vnd.dk.aau.netsec.hostage.provider.attack";
		} else if (isPacketUriMatch(uriMatch)) {
			if (uriMatch == PACKET_ONE) {
				return "vnd.android.cursor.item/vnd.dk.aau.netsec.hostage.provider.packet";
			}
			return "vnd.android.cursor.dir/vnd.dk.aau.netsec.hostage.provider.packet";
		}

		return null;
	}

	private boolean isNetworkUriMatch(int uriMatch) {
        return uriMatch == NETWORK_ALL || uriMatch == NETWORK_ONE;
    }

	private boolean isAttackUriMatch(int uriMatch) {
        return uriMatch == ATTACK_ALL || uriMatch == ATTACK_ONE;
    }

	private boolean isPacketUriMatch(int uriMatch) {
        return uriMatch == PACKET_ALL || uriMatch == PACKET_ONE;
    }

}
