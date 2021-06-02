package dk.aau.netsec.hostage.commons;

import org.apache.http.HttpVersion;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;


import dk.aau.netsec.hostage.logging.Record;
import dk.aau.netsec.hostage.logging.formatter.TraCINgFormatter;
import dk.aau.netsec.hostage.net.MySSLSocketFactory;

import static android.content.ContentValues.TAG;

/**
 * Helper class with some static methods for general usage.
 *
 * @author Lars Pandikow
 * @author Wulf Pfeiffer
 *
 */
public final class HelperUtils {

	//Getter and setters for detection of file injection

	public static String filePath;
	public static String getFilePath() {
		return filePath;
	}
	public static void setFilePath(String filePath) {
		HelperUtils.filePath = filePath;
	}



	public static String fileName;
	public static String getFileName() {
		return fileName;
	}
	public static void setFileName(String fileName) {
		HelperUtils.fileName = fileName;
	}




	public static String fileSHA256;
	public static String getFileSHA256() {
		return fileSHA256;
	}
	public static void setFileSHA256(String fileSHA256) {
		HelperUtils.fileSHA256 = fileSHA256;
	}




	public static boolean isFileInjected;
	public static boolean isFileInjected() {return isFileInjected;}
	public static void setIsFileInjected(boolean isFileInjected) {HelperUtils.isFileInjected = isFileInjected;}





	/**
	 * Converts a byte array into a hexadecimal String, e.g. {0x00, 0x01} to
	 * "00, 01".
	 *
	 * @param bytes
	 *            that will be converted.
	 * @return converted String.
	 */
	public static String bytesToHexString(byte[] bytes) {
		char[] hexArray = "0123456789ABCDEF".toCharArray();
		int v;
		StringBuffer buffer = new StringBuffer();
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			buffer.append(hexArray[v >>> 4]);
			buffer.append(hexArray[v & 0x0F]);
			if (j < bytes.length - 1)
				buffer.append(", ");
		}
		return buffer.toString();
	}

	/**
	 * Converts a byte[] to a String, but only characters in ASCII between 32
	 * and 127
	 *
	 * @param bytes
	 *            that are converted
	 * @return converted String
	 */
	public static String byteToStr(byte[] bytes) {
		int size = 0;
		for(byte b : bytes) {
			if(isLetter((char) b)) {
				size++;
			}
		}
		char[] chars = new char[size];
		for (int i = 0, j = 0; i < bytes.length && j < size; i++) {
			if (isLetter((char) bytes[i])) {
				chars[j] = (char) bytes[i];
				j++;
			}
		}
		return new String(chars);
	}

	/**
	 * Concatenates several byte arrays.
	 *
	 * @param bytes
	 *            The byte arrays.
	 * @return A single byte arrays containing all the bytes from the given
	 *         arrays in the order they are given.
	 */
	public static byte[] concat(byte[]... bytes) {
		int newSize = 0;
		for (byte[] b : bytes)
			if (b != null)
				newSize += b.length;
		byte[] dst = new byte[newSize];

		int currentPos = 0;
		int newPos;
		for (byte[] b : bytes) {
			if (b != null) {
				newPos = b.length;
				System.arraycopy(b, 0, dst, currentPos, newPos);
				currentPos += newPos;
			}
		}
		return dst;
	}

	/**
	 * Puts a 0x00 byte between each byte in a byte array.
	 *
	 * @param bytes
	 *            that need to be filled with 0x00.
	 * @return filled byte array.
	 */
	public static byte[] fillWithZero(byte[] bytes) {
		byte[] newBytes = new byte[(bytes.length * 2)];
		for (int i = 0, j = 0; i < bytes.length && j < newBytes.length; i++, j = j + 2) {
			newBytes[j] = bytes[i];
			newBytes[j + 1] = 0x00;
		}
		return newBytes;
	}

	/**
	 * Puts a 0x00 byte between each byte and another 2 0x00 bytes at the end of
	 * a byte array.
	 *
	 * @param bytes
	 *            that need to be filled with 0x00.
	 * @return filled byte array.
	 */
	public static byte[] fillWithZeroExtended(byte[] bytes) {
		byte[] zeroBytes = fillWithZero(bytes);
		byte[] newBytes = new byte[zeroBytes.length + 2];
		newBytes = HelperUtils.concat(zeroBytes, new byte[] { 0x00, 0x00 });
		return newBytes;
	}



	/**
	 * Gets BSSID of the wireless network.
	 *
	 * @param context
	 *            Needs a context to get system recourses.
	 * @return BSSID of wireless network if connected, else null.
	 */
	public static String getBSSID(Context context) {
		String bssid = null;
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo != null && networkInfo.isConnected()) {
			final WifiManager wifiManager = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
			if (connectionInfo != null
					&& !TextUtils.isEmpty(connectionInfo.getSSID())) {
				bssid = connectionInfo.getBSSID();
			}
		}
		return bssid;
	}
	/**
	 * Creates and Http Client of the wireless network.

	 * @return DefaultHttpClient returns the client.
	 */

	public static HttpClient createHttpClient() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(
					params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			e.printStackTrace();
			return new DefaultHttpClient();
		}
	}

	public static boolean uploadSingleRecord(Context context, Record record) {
		// Create a https client. Uses MySSLSocketFactory to accept all
		// certificates
		HttpClient httpclient = HelperUtils.createHttpClient();
		HttpPost httppost;

		try {
			// Create HttpPost
			httppost = new HttpPost(PreferenceManager
					.getDefaultSharedPreferences(context).getString(
							"pref_upload", "https://www.tracingmonitor.org"));

			// Create JSON String of Record
			StringEntity se = new StringEntity(record.toString(TraCINgFormatter.getInstance()));

			httppost.setEntity(se);

			// Execute HttpPost
			httpclient.execute(httppost);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Gets internal IP address of the device in a wireless network.
	 *
	 * @param context
	 *            Needs a context to get system recourses.
	 * @return internal IP of the device in a wireless network if connected,
	 *         else null.
	 */
	public static String getInternalIP(Context context) {
		String ipAddress = null;
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo != null && networkInfo.isConnected()) {
			final WifiManager wifiManager = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
			if (connectionInfo != null) {
				try {
					ipAddress = InetAddress.getByAddress(
							unpackInetAddress(connectionInfo.getIpAddress()))
							.getHostAddress();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}
		return ipAddress;
	}

	/**
	 * Gets SSID of the wireless network.
	 *
	 * @param context
	 *            Needs a context to get system recourses
	 * @return SSID of wireless network if connected, else null.
	 */
	public static String getSSID(Context context) {
		String ssid = null;
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo != null && networkInfo.isConnected()) {
			final WifiManager wifiManager = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
			if (connectionInfo != null
					&& !TextUtils.isEmpty(connectionInfo.getSSID())) {
				ssid = connectionInfo.getSSID();
				if (ssid.startsWith("\"") && ssid.endsWith("\"")) { // trim those quotes
					ssid = ssid.substring(1, ssid.length() - 1);
				}
			}
		}
		return ssid;
	}

	/**
	 * Gets the mac address of the devicek.
	 *
	 * @param context
	 *            Needs a context to get system recourses
	 * @return MAC address of the device.
	 */
	public static String getMacAdress(Context context) {
		String mac = null;
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo connectionInfo = wifiManager.getConnectionInfo();
		mac = connectionInfo.getMacAddress();
		return mac;
	}

	/**
	 * Produces a random String. The String can be of random length (minimum 1)
	 * with a maximum length, or it can be forced to have the length that was
	 * given.
	 *
	 * @param length
	 *            maximal / forced length of String.
	 * @param forceLength
	 *            forces the String to be exact the given length instead of
	 *            maximum
	 * @return random String.
	 */
	public static String getRandomString(int length, boolean forceLength) {
		SecureRandom rndm = new SecureRandom();
		char[] c = new char[forceLength ? length : rndm.nextInt(length - 1) + 1];
		for (int i = 0; i < c.length; i++) {
			c[i] = (char) (rndm.nextInt(95) + 32);
		}
		return new String(c);
	}

	/**
	 * Converts a String into a byte array, e.g. "00, 01" to {0x00, 0x01}.
	 *
	 * @param string
	 *            that will be converted.
	 * @return converted byte array.
	 */
	public static byte[] hexStringToBytes(String string) {
		String[] hexStrings = string.split(", ");
		byte[] bytes = new byte[hexStrings.length];
		for (int j = 0; j < hexStrings.length; j++) {
			bytes[j] = (byte) ((Character.digit(hexStrings[j].charAt(0), 16) << 4) + Character
					.digit(hexStrings[j].charAt(1), 16));
		}
		return bytes;
	}

	/**
	 * Generates a random byte[] of a specified size
	 *
	 * @param size
	 *            of the byte[]
	 * @return random byte[]
	 */
	public static byte[] randomBytes(int size) {
		byte[] bytes = new byte[size];
		SecureRandom rdm = new SecureRandom();
		rdm.nextBytes(bytes);
		return bytes;
	}

	/**
	 * Turns around the values of an byte[], e.g. {0x00, 0x01, 0x02} turns into
	 * {0x02, 0x01, 0x00}.
	 *
	 * @param bytes
	 *            array that is turned.
	 * @return turned array.
	 */
	public static byte[] turnByteArray(byte[] bytes) {
		byte[] tmp = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			tmp[i] = bytes[bytes.length - 1 - i];
		}
		return tmp;
	}

	/**
	 * Determines if a character is in ASCII between 32 and 126
	 *
	 * @param character
	 *            that is checked
	 * @return true if the character is between 32 and 126, else false
	 */
	private static boolean isLetter(char character) {
		return (character > 31 && character < 127);
	}

	public static byte[] unpackInetAddress(int bytes) {
		return new byte[] { (byte) ((bytes) & 0xff),
				(byte) ((bytes >>> 8) & 0xff), (byte) ((bytes >>> 16) & 0xff),
				(byte) ((bytes >>> 24) & 0xff) };
	}

	public static int packInetAddress(byte[] bytes) {

		long b0 = bytes[0]; if (b0 < 0) b0 = 256 + b0;
		long b1 = bytes[1]; if (b1 < 0) b1 = 256 + b1;
		long b2 = bytes[2]; if (b2 < 0) b2 = 256 + b2;
		long b3 = bytes[3]; if (b3 < 0) b3 = 256 + b3;
		long packed = b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
		if (packed >= (1l << 31)) {
			packed -= (1l << 32) - 1l;
		}
		return (int)packed;
	}

	public static String inetAddressToString(int address) {
		return (address & 0xFF) + "."
				+ ((address >>> 8) & 0xFF) + "."
				+ ((address >>> 16) & 0xFF) + "."
				+ ((address >>> 24) & 0xFF);
	}

	public static boolean isWifiConnected(Context context){
		if(context == null) return false;
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		return mWifi.isConnected();
	}

	public static boolean isCellConnected(Context context){
		if(context == null) return false;
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		boolean isMobileConn = false;

		for (Network network : connManager.getAllNetworks()) {
			NetworkInfo networkInfo = connManager.getNetworkInfo(network);



			if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
				return isMobileConn |= networkInfo.isConnected();
			}
		}
		return isMobileConn;
	}

	public  static boolean isCellurarConnected(Context context){

		if(context == null) return false;
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		boolean connected = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.isConnectedOrConnecting();

		return  connected;

	}

	/**
	 * Gets the ip-address for Cellular Networks
	 *
	 * @return ipaddress returns the ip address
	 */


	public static int getCellularIP() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						String ip = Formatter.formatIpAddress(inetAddress.hashCode());
						Log.i(TAG, "***** IP="+ ip);
						return getInetAddress(InetAddress.getByName((ip)));
					}
				}
			}
		} catch (SocketException | UnknownHostException ex) {
			Log.e(TAG, ex.toString());
		}
		return 0;
	}

	/**
	 * Converts a String type IP to int.
	 * @param address
	 * @return the converted ip
	 */
	public static int IPtoInt(String address){
		int result = 0;

		// iterate over each octet
		for(String part : address.split(Pattern.quote("."))) {
			// shift the previously parsed bits over by 1 byte
			result = result << 8;
			// set the low order bits to the current octet
			result |= Integer.parseInt(part);
		}

		return  result;
	}

	public static long ipToLong(String ipAddress) {

		String[] ipAddressInArray = ipAddress.split("\\.");

		long result = 0;
		for (int i = 0; i < ipAddressInArray.length; i++) {

			int power = 3 - i;
			int ip = Integer.parseInt(ipAddressInArray[i]);
			result += ip * Math.pow(256, power);

		}

		return result;
	}

	public static boolean isNetworkAvailable(Context context) {
		if(context == null) return false;

		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	public static int getRedirectedPort(int port){
		return port + 1024 + 27113;
	}

	/**
	 * Converts InetAddress to int
	 * @param address
	 * @return
	 */
	public static int getInetAddress(InetAddress address){
		int result = 0;
		for (byte b: address.getAddress()) {
			result = result << 8 | (b & 0xFF);
		}

		return result;
	}

	private static int convertNetmaskToCIDR(InetAddress netmask){
		byte[] netmaskBytes = netmask.getAddress();
		int cidr = 0;
		boolean zero = false;
		for(byte b : netmaskBytes){
			int mask = 0x80;

			for(int i = 0; i < 8; i++){
				int result = b & mask;
				if(result == 0){
					zero = true;
				}else if(zero){
					return 24;

				} else {
					cidr++;
				}
				mask >>>= 1;
			}
		}
		if(cidr==0)
			return 24;
		return cidr;
	}

	public static int getPrefix(String[] netmask) throws UnknownHostException {
		InetAddress mask = InetAddress.getByName(Arrays.toString(netmask).replace("[","").replace("]",""));

		return convertNetmaskToCIDR(mask);
	}


	public static String intToStringIp(int internalIPAddress){
		String internalIp = String.format("%d.%d.%d.%d",
				(internalIPAddress & 0xff),
				(internalIPAddress >> 8 & 0xff),
				(internalIPAddress >> 16 & 0xff),
				(internalIPAddress >> 24 & 0xff));

		return internalIp;
	}


	public static String getNetworkClass(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();

		TelephonyManager teleMan = (TelephonyManager)
				context.getSystemService(Context.TELEPHONY_SERVICE);
		if (info == null || !info.isConnected())
			return "-"; // not connected
		if (info.getType() == ConnectivityManager.TYPE_WIFI)
			return "WIFI";
		if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
			int networkType = info.getSubtype();
			switch (networkType) {
				case TelephonyManager.NETWORK_TYPE_GPRS:
				case TelephonyManager.NETWORK_TYPE_EDGE:
				case TelephonyManager.NETWORK_TYPE_CDMA:
				case TelephonyManager.NETWORK_TYPE_1xRTT:
				case TelephonyManager.NETWORK_TYPE_IDEN:     // api< 8: replace by 11
				case TelephonyManager.NETWORK_TYPE_GSM:      // api<25: replace by 16
					return "2G";
				case TelephonyManager.NETWORK_TYPE_UMTS:
				case TelephonyManager.NETWORK_TYPE_EVDO_0:
				case TelephonyManager.NETWORK_TYPE_EVDO_A:
				case TelephonyManager.NETWORK_TYPE_HSDPA:
				case TelephonyManager.NETWORK_TYPE_HSUPA:
				case TelephonyManager.NETWORK_TYPE_HSPA:
				case TelephonyManager.NETWORK_TYPE_EVDO_B:   // api< 9: replace by 12
				case TelephonyManager.NETWORK_TYPE_EHRPD:    // api<11: replace by 14
				case TelephonyManager.NETWORK_TYPE_HSPAP:    // api<13: replace by 15
				case TelephonyManager.NETWORK_TYPE_TD_SCDMA: // api<25: replace by 17
					return "3G";
				case TelephonyManager.NETWORK_TYPE_LTE:      // api<11: replace by 13
				case TelephonyManager.NETWORK_TYPE_IWLAN:    // api<25: replace by 18
				case 19: // LTE_CA
					return "4G";
				default:
					return "?";
			}
		}
		return "?";
	}


	//just for debugging purpose
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}