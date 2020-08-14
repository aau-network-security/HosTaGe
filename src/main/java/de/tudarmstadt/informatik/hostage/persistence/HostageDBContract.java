package de.tudarmstadt.informatik.hostage.persistence;

import android.provider.BaseColumns;

/**
 * Contract class defines names for the {@link HostageDBOpenHelper}.
 * @author Mihai Plasoianu
 * @author Lars Pandikow
 */
@Deprecated
public final class HostageDBContract {

	public static abstract class NetworkEntry implements BaseColumns {
		public static final String TABLE_NAME = "network";
		public static final String COLUMN_NAME_BSSID = "_bssid";
		public static final String COLUMN_NAME_SSID = "ssid";
		public static final String COLUMN_NAME_LATITUDE = "latitude";
		public static final String COLUMN_NAME_LONGITUDE = "longitude";
		public static final String COLUMN_NAME_ACCURACY = "accuracy";
		public static final String COLUMN_NAME_GEO_TIMESTAMP = "geo_timestamp";

		public static final String KEY_ID = COLUMN_NAME_BSSID;
	}

	public static abstract class AttackEntry implements BaseColumns {
		public static final String TABLE_NAME = "attack";
		public static final String COLUMN_NAME_ATTACK_ID = "_attack_id";
		public static final String COLUMN_NAME_PROTOCOL = "protocol";
		public static final String COLUMN_NAME_EXTERNAL_IP = "externalIP";
		public static final String COLUMN_NAME_LOCAL_IP = "localIP";
		public static final String COLUMN_NAME_LOCAL_PORT = "localPort";
		public static final String COLUMN_NAME_REMOTE_IP = "remoteIP";
		public static final String COLUMN_NAME_REMOTE_PORT = "remotePort";
		public static final String COLUMN_NAME_BSSID = "_bssid";
        public static final String COLUMN_NAME_DEVICE = "_device";
        public static final String COLUMN_NAME_SYNC_ID = "_sync_id";
		public static final String COLUMN_NAME_INTERNAL_ATTACK = "internalAttack";

		public static final String KEY_ID = COLUMN_NAME_ATTACK_ID;
	}

	public static abstract class PacketEntry implements BaseColumns {
		public static final String TABLE_NAME = "packet";
		public static final String COLUMN_NAME_ID = "_id";
		public static final String COLUMN_NAME_ATTACK_ID = "_attack_id";
		public static final String COLUMN_NAME_TYPE = "type";
		public static final String COLUMN_NAME_PACKET_TIMESTAMP = "packet_timestamp";
		public static final String COLUMN_NAME_PACKET = "packet";

		public static final String KEY_ID = COLUMN_NAME_ID;
	}
	
	public static abstract class SyncDeviceEntry implements BaseColumns {
		public static final String TABLE_NAME = "sync_devices";
		public static final String COLUMN_NAME_DEVICE_ID = "_device_id";	
		public static final String COLUMN_NAME_DEVICE_TIMESTAMP = "last_sync_timestamp";
        public static final String COLUMN_NAME_HIGHEST_ATTACK_ID = "highest_attack_id";
		
		public static final String KEY_ID = COLUMN_NAME_DEVICE_ID;
	}
	
	public static abstract class SyncInfoEntry implements BaseColumns {
		public static final String TABLE_NAME = "sync_info";
		public static final String COLUMN_NAME_DEVICE_ID = "_device_id";	
		public static final String COLUMN_NAME_BSSID = "_bssid";
		public static final String COLUMN_NAME_NUMBER_ATTACKS = "number_of_attacks";
		public static final String COLUMN_NAME_NUMBER_PORTSCANS = "number_of_portscans";		
		
		public static final String KEY_ID = COLUMN_NAME_DEVICE_ID;
	}
	
	public static abstract class ProfileEntry implements BaseColumns {
		public static final String TABLE_NAME = "profiles";
		public static final String COLUMN_NAME_PROFILE_ID = "_profile_id";	
		public static final String COLUMN_NAME_PROFILE_NAME = "profile_name";
		public static final String COLUMN_NAME_PROFILE_DESCRIPTION = "profile_description";
		public static final String COLUMN_NAME_PROFILE_ICON = "profile_icon";		
		public static final String COLUMN_NAME_PROFILE_EDITABLE = "profile_editable";	
		public static final String COLUMN_NAME_PROFILE_ACTIVE = "profile_active";	
		public static final String COLUMN_NAME_PROFILE_ICON_NAME = "profile_icon_name";	
		
		public static final String KEY_ID = COLUMN_NAME_PROFILE_ID;
	}
}
