package dk.aau.netsec.hostage.sync.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dk.aau.netsec.hostage.location.CustomLocationManager;
import dk.aau.netsec.hostage.location.LocationException;
import dk.aau.netsec.hostage.logging.NetworkRecord;
import dk.aau.netsec.hostage.logging.Record;
import dk.aau.netsec.hostage.logging.RecordAll;
import dk.aau.netsec.hostage.logging.SyncData;
import dk.aau.netsec.hostage.logging.SyncInfo;
import dk.aau.netsec.hostage.logging.SyncRecord;
import dk.aau.netsec.hostage.net.MySSLSocketFactory;
import dk.aau.netsec.hostage.sync.Synchronizer;


/**
 * Created by abrakowski
 */
public class SyncUtils {
    public static final int SYNC_SUCCESSFUL = 0x0;
    private static final long SYNC_FREQUENCY_MINUTES = 5;
    private static final long SYNC_FREQUENCY_UNIT = 60;
    private static final long SYNC_FREQUENCY = SYNC_FREQUENCY_UNIT * SYNC_FREQUENCY_MINUTES;  // 5 min (in seconds)

    public static final String CONTENT_AUTHORITY = "dk.aau.netsec.hostage.androidsync";
    private static final String PREF_SETUP_COMPLETE = "sync_setup_complete";
    private static final String PREF_SYNC_INTERNAL_FREQUENCY = "pref_sync_internal_frequency";
    private static final String PREF_SYNC_FREQUENCY = "pref_sync_frequency";

    private static final Map<String, Integer> protocolsTypeMap;

    static {
        protocolsTypeMap = new HashMap<String, Integer>();
        protocolsTypeMap.put("UNKNOWN", 0);
        protocolsTypeMap.put("ECHO", 1);
        protocolsTypeMap.put("GHOST", 2);
        protocolsTypeMap.put("PORTSCAN", 11);
        protocolsTypeMap.put("SSH", 20);
        protocolsTypeMap.put("MySQL", 31);
        protocolsTypeMap.put("SMB", 40);
        protocolsTypeMap.put("SIP", 50);
        protocolsTypeMap.put("FTP", 60);
        protocolsTypeMap.put("HTTP", 70);
        protocolsTypeMap.put("HTTPS", 71);
        protocolsTypeMap.put("TELNET", 80);
    }

    /**
     * Create an entry for this application in the system account list, if it isn't already there.
     *
     * @param context Context
     */
    public static void CreateSyncAccount(Context context) {
        boolean newAccount = false;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean setupComplete = preferences.getBoolean(PREF_SETUP_COMPLETE, false);

        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = HostageAccountService.GetAccount();
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(account, null, null)) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            long syncFrequency = pref.getInt("pref_sync_frequency", 5 * 60); // default is 5min
            ContentResolver.addPeriodicSync(
                    account, CONTENT_AUTHORITY, new Bundle(), SYNC_FREQUENCY);
            preferences.edit().putLong(PREF_SYNC_INTERNAL_FREQUENCY, SYNC_FREQUENCY).commit();
            newAccount = true;
        }

        // Schedule an initial sync if we detect problems with either our account or our local
        // data has been deleted. (Note that it's possible to clear app data WITHOUT affecting
        // the account list, so wee need to check both.)
        if (newAccount || !setupComplete) {
            TriggerRefresh();
            preferences.edit().putBoolean(PREF_SETUP_COMPLETE, true).commit();
        }

        if (setupComplete) {
            long syncFrequency = Long.valueOf(preferences.getString(PREF_SYNC_FREQUENCY, "" + SYNC_FREQUENCY_MINUTES)) * SYNC_FREQUENCY_UNIT;
            long internalFrequency = preferences.getLong(PREF_SYNC_INTERNAL_FREQUENCY, SYNC_FREQUENCY);

            if (syncFrequency != internalFrequency) {
                ContentResolver.removePeriodicSync(account, CONTENT_AUTHORITY, new Bundle());
                ContentResolver.addPeriodicSync(account, CONTENT_AUTHORITY, new Bundle(), syncFrequency);

                preferences.edit().putLong(PREF_SYNC_INTERNAL_FREQUENCY, syncFrequency).commit();
            }
        }
    }

    public static void TriggerRefresh() {
        Bundle b = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(
                HostageAccountService.GetAccount(),      // Sync account
                CONTENT_AUTHORITY,                       // Content authority
                b);                                      // Extras
    }


    public static String getProtocolFromInt(int p) {
        for (Map.Entry<String, Integer> entry : protocolsTypeMap.entrySet()) {
            if (entry.getValue() == p) return entry.getKey();
        }

        return "UNKNOWN";
    }

    public static void appendRecordToStringWriter(Record record, Writer stream) {
        try {
            stream.append(
                    "{" +
                            "\"sensor\":{" +
                            "\"name\":\"HosTaGe\"," +
                            "\"type\":\"Honeypot\"" +
                            "}," +
                            "\"src\":{" +
                            "\"ip\":\"" + record.getRemoteIP() + "\"," +
                            "\"port\":" + record.getRemotePort() +
                            "}," +
                            "\"dst\":{" +
                            "\"ip\":\"" + record.getLocalIP() + "\"," +
                            "\"port\":" + record.getLocalPort() +
                            "}," +
                            "\"type\":" + (protocolsTypeMap.containsKey(record.getProtocol()) ? protocolsTypeMap.get(record.getProtocol()) : 0) + "," +
                            "\"log\":\"" + record.getProtocol() + "\"," +
                            "\"md5sum\":\"\"," +
                            "\"date\":" + (int) (record.getTimestamp() / 1000) + "," +
                            "\"bssid\":\"" + record.getBssid() + "\"," +
                            "\"ssid\":\"" + record.getSsid() + "\"," +
                            "\"device\":\"" + record.getDevice() + "\"," +
                            "\"sync_id\":\"" + record.getSync_id() + "\"," +
                            "\"internal_attack\":\"" + record.getWasInternalAttack() + "\"," +
                            "\"external_ip\":\"" + record.getExternalIP() + "\"" +
                            "}\n"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendRecordToStringWriter(RecordAll record, Writer stream) {
        try {
            stream.append(
                    "{" +
                            "\"sensor\":{" +
                            "\"name\":\"HosTaGe\"," +
                            "\"type\":\"Honeypot\"" +
                            "}," +
                            "\"src\":{" +
                            "\"ip\":\"" + record.getRemoteIP() + "\"," +
                            "\"port\":" + record.getRemotePort() +
                            "}," +
                            "\"dst\":{" +
                            "\"ip\":\"" + record.getLocalIP() + "\"," +
                            "\"port\":" + record.getLocalPort() +
                            "}," +
                            "\"type\":" + (protocolsTypeMap.containsKey(record.getProtocol()) ? protocolsTypeMap.get(record.getProtocol()) : 0) + "," +
                            "\"log\":\"" + record.getProtocol() + "\"," +
                            "\"md5sum\":\"\"," +
                            "\"date\":" + (int) (record.getTimestamp() / 1000) + "," +
                            "\"bssid\":\"" + record.getBssid() + "\"," +
                            "\"ssid\":\"" + record.getSsid() + "\"," +
                            "\"device\":\"" + record.getDevice() + "\"," +
                            "\"sync_id\":\"" + record.getSync_id() + "\"," +
                            "\"internal_attack\":\"" + record.getWasInternalAttack() + "\"," +
                            "\"external_ip\":\"" + record.getExternalIP() + "\"" +
                            "}\n"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    public static boolean uploadRecordsToServer(String entity, String serverAddress) {
        HttpPost httppost;
        try {
            HttpClient httpClient = createHttpClient();
            // Create HttpPost
            httppost = new HttpPost(serverAddress);

            StringEntity se = new StringEntity(entity);
            httppost.addHeader("content-type", "application/json+newline");
            httppost.setEntity(se);

            // Execute HttpPost
            HttpResponse response = httpClient.execute(httppost);

            if (response.getStatusLine().getStatusCode() >= 400 && response.getStatusLine().getStatusCode() < 600) {
                return false;
            }
            Log.i("TracingSyncService", "Status Code: " + response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static <T> T downloadFromServer(String address, Class<T> klass) {
        HttpGet httpget;

        try {
            HttpClient httpClient = createHttpClient();

            httpget = new HttpGet(address);
            httpget.addHeader("Accept", "application/json");

            HttpResponse response = httpClient.execute(httpget);
            Log.i("downloadFromServer", "Status Code: " + response.getStatusLine().getStatusCode());

            if (response.getStatusLine().getStatusCode() >= 400 && response.getStatusLine().getStatusCode() < 600) {
                return klass.newInstance();
            }

            return klass.getConstructor(klass).newInstance(readResponseToString(response));
        } catch (IOException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String readResponseToString(HttpResponse response) {
        StringBuilder builder = new StringBuilder();

        try {
            BufferedReader bReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line;

            while ((line = bReader.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    public static SyncData getSyncDataFromTracing(Context context, Synchronizer synchronizer, long fromTime) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String serverAddress = pref.getString("pref_upload_server", "https://www.tracingmonitor.org");
        //String serverAddress = pref.getString("pref_upload_server", "https://ssi.cased.de");


        HttpPost httppost;
//        TODO take a better look at the exceptions thrown from this flow.
        try {
            HttpClient httpClient = createHttpClient();
            // Create HttpPost
            httppost = new HttpPost(serverAddress + "/sync");

            SyncInfo info = synchronizer.getSyncInfo();

            JSONArray deviceMap = new JSONArray();
            for (Map.Entry<String, Long> entry : info.deviceMap.entrySet()) {
                JSONObject m = new JSONObject();
                m.put("sync_id", entry.getValue());
                m.put("device", entry.getKey());
                deviceMap.put(m);
            }

            JSONObject condition = new JSONObject();

            /*if(fromTime > 0){
                Calendar calendar = GregorianCalendar.getInstance();
                calendar.setTimeInMillis(fromTime);

                condition.put("date", fromCalendar(calendar));
            }*/

            String country = null;

            Location location = CustomLocationManager.getLocationManagerInstance(null).getLatestLocation();

            if (location != null) {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> fromLocation = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                if (fromLocation.size() > 0) {
                    Address address = fromLocation.get(0);
                    country = address.getCountryCode();
                }
            }

            if (country == null) {
                // We could not get the gps coordinates, try to retrieve the country code from the SIM card
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                country = tm.getNetworkCountryIso();
            }

            if (country == null || country.trim().isEmpty())
                country = Locale.getDefault().getCountry();

            condition.put("country", country);

            JSONObject req = new JSONObject();
            req.put("condition", condition);
            req.put("info", deviceMap);


            StringEntity se = new StringEntity(req.toString());
            httppost.addHeader("content-type", "application/json");
            httppost.setEntity(se);

            // Execute HttpPost
            HttpResponse response = httpClient.execute(httppost);
            Log.i("TracingSyncService", "Status Code: " + response.getStatusLine().getStatusCode());
            if (response.getStatusLine().getStatusCode() >= 400 && response.getStatusLine().getStatusCode() < 600) {
                return null;
            }

            String responseBody = readResponseToString(response);
            JSONArray syncData;

            // ensure, that the received data is an array
            try {
                syncData = new JSONArray(responseBody);
            } catch (JSONException ex) {
                ex.printStackTrace();
                return null;
            }

            ArrayList<SyncRecord> syncRecords = new ArrayList<SyncRecord>();
            Map<String, NetworkRecord> networkRecordMap = new HashMap<String, NetworkRecord>();

            SyncData result = new SyncData();

            for (int i = 0; i < syncData.length(); i++) {
                try {
                    JSONObject item = syncData.getJSONObject(i);
                    JSONObject src = item.getJSONObject("src");
                    JSONArray src_ll = src.getJSONArray("ll");

                    JSONObject dst = item.getJSONObject("dst");
                    JSONArray dst_ll = dst.getJSONArray("ll");

                    Calendar date = toCalendar(item.getString("date"));

                    if (!networkRecordMap.containsKey(item.getString("bssid"))) {
                        NetworkRecord networkRecord = new NetworkRecord();
                        networkRecord.setAccuracy(0);
                        networkRecord.setBssid(item.getString("bssid"));
                        networkRecord.setSsid(item.getString("ssid"));
                        networkRecord.setLatitude(dst_ll.getDouble(1));
                        networkRecord.setLatitude(dst_ll.getDouble(0));
                        networkRecord.setTimestampLocation(date.getTimeInMillis());
                        networkRecordMap.put(item.getString("bssid"), networkRecord);
                    }

                    SyncRecord record = new SyncRecord();
                    record.setBssid(item.getString("bssid"));
                    record.setAttack_id(i);
                    record.setDevice(item.getString("device"));
                    record.setSync_id(item.getLong("sync_id"));
                    record.setProtocol(getProtocolFromInt(item.getInt("type")));
                    record.setLocalIP(dst.getString("ip"));
                    record.setLocalPort(dst.getInt("port"));
                    record.setRemoteIP(src.getString("ip"));
                    record.setRemotePort(src.getInt("port"));
                    record.setExternalIP(item.has("external_ip") ? item.getString("external_ip") : "0.0.0.0");
                    record.setWasInternalAttack(item.has("internal_attack") && item.getBoolean("internal_attack"));

                    syncRecords.add(record);
                } catch (org.json.JSONException jsonException) {
                    jsonException.printStackTrace();
                    continue;
                } catch (java.text.ParseException textPE) {
                    textPE.printStackTrace();
                    continue;
                }
            }

            result.networkRecords = new ArrayList<NetworkRecord>(networkRecordMap.values());
            result.syncRecords = syncRecords;

            return result;
        } catch (ClientProtocolException cpe) {
            cpe.printStackTrace();
            return null;
        } catch (java.io.IOException ioException) {
            ioException.printStackTrace();
            return null;
        } catch (org.json.JSONException jsonException) {
            jsonException.printStackTrace();
            return null;
        } catch (LocationException le) {
            le.printStackTrace();
            return null;
        }
    }

    public static String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String[] convertMapToStringArray(Map<String, String> map) {
        String[] array = new String[map.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            array[i] = entry.getKey();
            array[i + 1] = entry.getValue();
            i += 2;
        }

        return array;
    }

    public static String buildUrlFromBase(String baseUrl, String... query) {
        StringBuilder sb = new StringBuilder(baseUrl);

        if (query.length >= 2) {
            sb.append("?");
        }

        for (int i = 0; i < query.length - 2; i += 2) {
            if (i > 0) {
                sb.append("&");
            }

            sb.append(String.format("%s=%s",
                    urlEncodeUTF8(query[i]),
                    urlEncodeUTF8(query[i + 1])
            ));
        }

        return sb.toString();
    }

    public static String buildUrlFromBase(String baseUrl, Map<String, String> query) {
        return buildUrlFromBase(baseUrl, convertMapToStringArray(query));
    }

    public static String buildUrl(String protocol, String domain, int port, String path, String... query) {
        return buildUrlFromBase(
                String.format("%s://%s:%d/%s", urlEncodeUTF8(protocol), urlEncodeUTF8(domain), port, path),
                query
        );
    }

    public static String buildUrl(String protocol, String domain, int port, String path, Map<String, String> query) {
        return buildUrl(protocol, domain, port, path, convertMapToStringArray(query));
    }

    public static List<String[]> getCountriesFromServer(String serverAddress) {
        List<String[]> ret = new ArrayList<String[]>();
        JSONArray array = downloadFromServer(serverAddress + "/get_countries", JSONArray.class);

        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject ob = array.getJSONObject(i);
                ret.add(new String[]{ob.getString("cc"), ob.getString("country")});
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static String fromCalendar(final Calendar calendar) {
        Date date = calendar.getTime();
        String formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .format(date);
        return formatted.substring(0, 22) + ":" + formatted.substring(22);
    }

    public static Calendar toCalendar(final String iso8601string)
            throws ParseException {
        Calendar calendar = GregorianCalendar.getInstance();
        String s = iso8601string.replace("Z", "0+0000");

        try {
            s = s.substring(0, 22) + s.substring(23);  // to get rid of the ":"
        } catch (IndexOutOfBoundsException e) {
            throw new ParseException("Invalid length", 0);
        }

        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(s);
        calendar.setTime(date);
        return calendar;
    }

    public static JSONArray retrieveNewAttacks(Context context, boolean fromPosition) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String serverAddress = pref.getString("pref_download_server", "https://www.tracingmonitor.org");
        //String serverAddress = pref.getString("pref_download_server", "http://ssi.cased.de/api");
        long lastDownloadTime = pref.getLong("pref_download_last_time", 0);

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeInMillis(lastDownloadTime);

        String baseUri = serverAddress + "/get_attacks";
        Map<String, String> query = new HashMap<String, String>();
        query.put("start", fromCalendar(calendar));

        if (fromPosition) {
            try {
                Location location = CustomLocationManager.getLocationManagerInstance(null).getLatestLocation();

                query.put("latitude", String.valueOf(location.getLatitude()));
                query.put("longitude", String.valueOf(location.getLongitude()));
                query.put("distance", "300");

            } catch (LocationException le) {

            }
        }

        return downloadFromServer(buildUrlFromBase(baseUri, "start", fromCalendar(calendar)), JSONArray.class);
    }

    public static HttpClient createHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e) {
            e.printStackTrace();
            return new DefaultHttpClient();
        }
    }
}