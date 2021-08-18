package dk.aau.netsec.hostage.persistence;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.Listener;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.model.Profile;
import dk.aau.netsec.hostage.ui.activity.MainActivity;
import dk.aau.netsec.hostage.ui.adapter.ProfileRecyclerAdapter;


/**
 * The profile manager is responsible for persisting and deleting profiles
 *
 * @author Alexander Brakowski
 * @created 10.02.14 20:24
 */
public class ProfileManager {

    /**
     * The singleton instance holder
     */
    private static ProfileManager INSTANCE = null;

    /**
     * An recycler adapter, which the profile manager informs about data changes
     */
    private ProfileRecyclerAdapter mProfileRecyclerAdapter = null;

    /**
     * Holds a reference to the currently active profile
     */
    private Profile mCurrentActivatedProfile = null;

    /**
     * Holds a reference to the random profile
     */
    private Profile mRandomProfile = null;

    /**
     * The profiles are being serialized and persisted into this file
     */
    private static final String PERSIST_FILENAME = "hostage_profiles.json";

    /**
     * Hold the current profile id, it will be incremented each time a new profile is added.
     * The new profile will get the newly incremented value as an id.
     */
    public int mIncrementValue = 1;

    /**
     * Holds all the available profiles. The key in the map is the ID of the profile.
     */
    public HashMap<Integer, Profile> mProfiles;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mSharedEditor;

    /**
     * Since the profile manager should only have one instance in the whole app, we are using the singleton pattern.
     * This method creates a new instance of the profile manager, if no instance already exists, and returns it.
     *
     * @return the singleton instance
     */
    public static ProfileManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ProfileManager();
        }

        if (INSTANCE.getNumberOfProfiles() == 0) {
            INSTANCE.loadData();
        }

        return INSTANCE;
    }

    /**
     * A private constructor, that can/should only be called by getInstance, since we want to enforce the usage of the singleton.
     */
    private ProfileManager() {
        mProfiles = new HashMap<Integer, Profile>();

        String sharedPreferencePath = MainActivity.getContext().getString(R.string.shared_preference_path);
        mSharedPreferences = MainActivity.getContext().getSharedPreferences(sharedPreferencePath, Hostage.MODE_PRIVATE);
        mSharedEditor = mSharedPreferences.edit();
        mSharedEditor.apply();
    }

    /**
     * Reads all data from an inputstream and appends it to a string
     *
     * @param is The input stream to read the data from
     * @return the whole data from the input stream as an string
     */
    public String readAll(final InputStream is) {
        if (null == is) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int rc;
        try {
            while ((rc = is.read()) >= 0) {
                sb.append((char) rc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }


    /**
     * Reads all the data of the the profiles, that were persisted and unserializes them.
     * <p>
     * The profiles were serialized into JSON and persisted into the android private file.
     * See {@see ProfileManager#persistData}.
     */
    public void loadData() {
        try {
            if (loadDefaultData())
                return;
            String UTF8 = "utf8";
            int BUFFER_SIZE = 8192;
            BufferedReader fbr = new BufferedReader(new InputStreamReader(MainActivity.getContext().openFileInput(PERSIST_FILENAME), UTF8), BUFFER_SIZE);

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = fbr.readLine()) != null) {
                sb.append(line);
            }

            JSONArray arr = new JSONArray(sb.toString());
            fbr.close();
            loadJson(arr);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (mProfiles.size() == 0) {
                this.fillWithDefaultData();
            }

            if (this.mRandomProfile != null) {
                randomizeProtocols(mRandomProfile);
            }
        }
    }

    private boolean loadDefaultData() {
        boolean profileExist = new File(PERSIST_FILENAME).exists();
        if (!profileExist) {
            fillDefaultData();

            return true;
        } else {
            return false;
        }
    }

    private void fillDefaultData() {
        if (mProfiles.size() == 0) {
            this.fillWithDefaultData();
        }

        if (this.mRandomProfile != null) {
            randomizeProtocols(mRandomProfile);
        }
    }


    private void loadJson(JSONArray arr) throws JSONException {
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);

            Profile p = new Profile();
            p.fromJSON(obj);

            mProfiles.put(p.mId, p);

            if (p.mId > mIncrementValue) {
                mIncrementValue = p.mId;
            }

            if (p.mActivated) {
                activateProfile(p, false);
            }

            if (p.mIsRandom) {
                this.mRandomProfile = p;
            }
        }
    }

    /**
     * All the profiles that are hold by the profile manager are being serialized into JSON and then written into an private android file.
     */
    public void persistData() {
        try {
            int BUFFER_SIZE = 8192;
            String UTF8 = "utf8";

            FileOutputStream fout = MainActivity.getContext().openFileOutput(PERSIST_FILENAME, Context.MODE_PRIVATE);
            BufferedWriter fnw = new BufferedWriter(new OutputStreamWriter(fout, UTF8), BUFFER_SIZE);

            JSONArray arr = new JSONArray();
            for (Profile p : mProfiles.values()) {
                arr.put(p.toJSON());
            }

            fnw.write(arr.toString());

            fnw.close();
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Retrieves all the profiles as an List
     *
     * @return a list that holds all the profiles
     */
    public List<Profile> getProfilesList() {
        return new ArrayList<Profile>(getProfilesCollection());
    }

    /**
     * Retrieves all the profiles as an collection
     *
     * @return a collection of all the profiles
     */
    public Collection<Profile> getProfilesCollection() {
        if (mProfiles.size() == 0 || mProfiles == null) {
            this.loadData();
        }

        return mProfiles.values();
    }

    /**
     * Retrieves an map of all the profiles. The key in the map is the ID of an profile.
     *
     * @return a map of profiles
     */
    public Map<Integer, Profile> getMapProfiles() {
        return mProfiles;
    }

    /**
     * Activates and deactivates protocols randomly in the given profile
     *
     * @param profile the profile to randomize the protocols for
     */
    public void randomizeProtocols(Profile profile) {
        LinkedList<String> protocols = new LinkedList<String>(Arrays.asList(MainActivity.getContext().getResources().getStringArray(R.array.protocols)));
        profile.mActiveProtocols.clear();

        Random rand = new Random();
        int numberOfProtocolsToActivate = rand.nextInt(protocols.size()) + 1;

        while (numberOfProtocolsToActivate-- > 0) {
            int randomIndex = rand.nextInt(protocols.size());
            String protocol = protocols.get(randomIndex);

            profile.mActiveProtocols.put(protocol, true);
            protocols.remove(protocol);
        }

        persistData();
    }

    /**
     * Adds or updates a given profile.
     *
     * @param profile the profile to persist
     * @return the id the profile was assigned to
     */
    public int persistProfile(Profile profile) {
        if (profile.mId == -1) {
            profile.mId = ++mIncrementValue;
        }

        mProfiles.put(profile.mId, profile);

        this.persistData();

        if (this.mProfileRecyclerAdapter != null) {
            this.mProfileRecyclerAdapter.notifyDataSetChanged();
        }

        return profile.mId;
    }

    /**
     * Retrieves the profile with the given id
     *
     * @param id the id of the profile
     * @return the profile
     */
    public Profile getProfile(int id) {
        if (mProfiles.size() == 0) {
            loadData();
        }

        if (this.mProfiles.containsKey(id)) {
            return this.mProfiles.get(id);
        }

        return null;
    }

    /**
     * Adds a profile
     *
     * @param profile the profile to add
     */
    public void addProfile(Profile profile) {
        this.addProfile(profile, true);
    }

    /**
     * Adds a given profile to the profile manager.
     *
     * @param profile the profile to add
     * @param persist true,  if the profile should also be persisted immediately,
     *                false, if the profile should just be added internally without being persisted
     *                (Note: you can still call persistData later to persist all the profiles)
     */
    public void addProfile(Profile profile, boolean persist) {

        if (profile.mId == -1) {
            profile.mId = ++mIncrementValue;
        }

        mProfiles.put(profile.mId, profile);

        if (persist) {
            persistData();
        }

        if (this.mProfileRecyclerAdapter != null) {
            this.mProfileRecyclerAdapter.addProfile(profile);
        }
    }

    /**
     * Deletes a given profile. These changes will be persisted immediately.
     *
     * @param profile the profile to delete
     */
    public void deleteProfile(Profile profile) {
        if (this.mProfiles.containsKey(profile.mId)) {
            Profile p = getProfile(profile.mId);
            this.mProfiles.remove(profile.mId);

            if (p.mActivated || this.mCurrentActivatedProfile.mId == p.mId) {
                mCurrentActivatedProfile = mRandomProfile;
                mRandomProfile.mActivated = true;
            }

            this.persistData();
            //this.dbh.deleteProfile(profile.mId);

            if (this.mProfileRecyclerAdapter != null) {
                this.mProfileRecyclerAdapter.removeProfile(profile);
                this.mProfileRecyclerAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Removes all profiles.
     */
    public void clearProfiles() {
        mProfiles.clear();
        persistData();
    }

    /**
     * Same as {@see activateProfile(Profile profile, boolean persist)} but with persist arg being always true
     *
     * @param profile the profile to active
     */
    public void activateProfile(Profile profile) {
        this.activateProfile(profile, true);
    }

    /**
     * Makes a given profile active.
     *
     * @param profile the profile to activate
     * @param persist indicates if the profile should be persisted after activating
     */
    public void activateProfile(Profile profile, boolean persist) {
        if (profile.equals(this.mCurrentActivatedProfile) || (mCurrentActivatedProfile != null && profile.mId == mCurrentActivatedProfile.mId))
            return;

        if (this.mCurrentActivatedProfile != null) {
            this.mCurrentActivatedProfile.mActivated = false;
            this.persistProfile(this.mCurrentActivatedProfile);
        }

        profile.mActivated = true;
        this.mCurrentActivatedProfile = profile;
        this.mProfiles.put(profile.mId, profile);

        mSharedEditor.putString("os", profile.mLabel);
        mSharedEditor.commit();

        if (persist) persistData();

        if (MainActivity.getInstance().getHostageService() != null) {
            if (MainActivity.getInstance().getHostageService().hasRunningListeners()) {
                List<String> protocolsToStart = profile.getActiveProtocols();

                for (Listener listener : MainActivity.getInstance().getHostageService().getListeners()) {
                    if (listener.isRunning()) {
                        MainActivity.getInstance().getHostageService().stopListenerAllPorts(listener.getProtocolName());

                    }
                }
                MainActivity.getInstance().startMonitorServices(protocolsToStart);
            }
        }
    }

    /**
     * Checks if the "random" profile is currently active
     *
     * @return true,  if active
     * false, otherwise
     */
    public boolean isRandomActive() {
        return this.mCurrentActivatedProfile.equals(this.mRandomProfile);
    }

    /**
     * Retrieves the "random" profile
     *
     * @return the "random" profile
     */
    public Profile getRandomProfile() {
        return this.mRandomProfile;
    }

    /**
     * Retrieves the currently active profile
     *
     * @return the active profile
     */
    public Profile getCurrentActivatedProfile() {
        return mCurrentActivatedProfile;
    }

    /**
     * Sets the list adapter that should also be managed by the profile manager
     *
     * @param profileRecyclerAdapter the list adapter to manage
     */
    public void setProfileListAdapter(ProfileRecyclerAdapter profileRecyclerAdapter) {
        this.mProfileRecyclerAdapter = profileRecyclerAdapter;
    }


    /**
     * Retrieves the list adapter, that is being managed by the profile manager
     *
     * @return the list adapter
     */
    public ProfileRecyclerAdapter getProfileRecyclerAdapter() {
        return this.mProfileRecyclerAdapter;
    }

    /**
     * Retrieves the number of profiles
     *
     * @return the number of profiles
     */
    public int getNumberOfProfiles() {
        return mProfiles.size();
    }


    /**
     * Pick n numbers between 0 (inclusive) and k (inclusive)
     * While there are very deterministic ways to do this,
     * for large k and small n, this could be easier than creating
     * an large array and sorting, i.e. k = 10,000
     */
    public Set<Integer> pickRandom(int n, int s, int k) {
        Random random = new Random(); // if this method is used often, perhaps define random at class level
        Set<Integer> picked = new HashSet<Integer>();
        while (picked.size() < n) {
            picked.add(random.nextInt(k - s) + s);
        }
        return picked;
    }

    /**
     * Fills the profiles manager with default profiles
     */
    public void fillWithDefaultData() {
        addWindowsSevenProfile();
        addWindowsXPProfile();
        addServerHTTPProfile();
        addServerWebProfile();
        addUnixMachineProfile();
        addLinuxMachineProfile();
        addVoipServer();
        addRandomProfile();
        addNuclearPlantProfile();
        addModbusMasterProfile();
        //addSNMPProfile();
        addVigilantProfile();
        addMQTTBrokerProfile();
        addMQTTSensorProfile();
        addSmokeSensorProfile();
        addArduinoProfile();
        addWaterPlantProfile();

        persistData();
    }

    private void addWindowsSevenProfile() {
        Profile windowsSeven = new Profile(
                27,
                "Windows 7",
                MainActivity.getInstance().getString(R.string.profile_seven_desc),
                R.drawable.ic_profile_vista,
                false
        );

        windowsSeven.mActiveProtocols.put("SMB", true);
        windowsSeven.mActiveProtocols.put("ECHO", true);

        this.addProfile(windowsSeven, false);
    }

    private void addWindowsXPProfile() {
        Profile windowsXP = new Profile(
                28,
                "Windows XP",
                MainActivity.getInstance().getString(R.string.profile_xp_desc),
                R.drawable.ic_profile_xp,
                false
        );

        windowsXP.mActiveProtocols.put("SMB", true);
        windowsXP.mActiveProtocols.put("ECHO", true);

        this.addProfile(windowsXP, false);

    }

    private void addServerHTTPProfile() {
        Profile serverHTTP = new Profile(
                19,
                "Web Server Apache",
                MainActivity.getInstance().getString(R.string.profile_webserv_apache_desc),
                R.drawable.ic_profile_apache,
                false
        );

        serverHTTP.mActiveProtocols.put("HTTP", true);
        serverHTTP.mActiveProtocols.put("HTTPS", true);
        serverHTTP.mActiveProtocols.put("MySQL", true);

        this.addProfile(serverHTTP, false);

    }

    private void addServerWebProfile() {
        Profile serverWeb = new Profile(
                20,
                "Web Server IIS",
                MainActivity.getInstance().getString(R.string.profile_webserv_iis_desc),
                R.drawable.ic_profile_apache,
                false
        );

        serverWeb.mActiveProtocols.put("HTTP", true);
        serverWeb.mActiveProtocols.put("HTTPS", true);
        serverWeb.mActiveProtocols.put("FTP", true);

        this.addProfile(serverWeb, false);

    }

    private void addUnixMachineProfile() {
        Profile unixMachine = new Profile(
                21,
                "\"Hardened\" Linux system ",
                MainActivity.getInstance().getString(R.string.profile_linux_hard_desc),
                R.drawable.ic_profile_unix,
                false
        );

        unixMachine.mActiveProtocols.put("SSH", true);

        this.addProfile(unixMachine, false);

    }

    private void addLinuxMachineProfile() {
        Profile linuxMachine = new Profile(
                22,
                "Linux system",
                MainActivity.getInstance().getString(R.string.profile_linux_desc),
                R.drawable.ic_profile_linux,
                false
        );

        linuxMachine.mActiveProtocols.put("FTP", true);
        linuxMachine.mActiveProtocols.put("TELNET", true);
        linuxMachine.mActiveProtocols.put("HTTP", true);
        linuxMachine.mActiveProtocols.put("HTTPS", true);
        linuxMachine.mActiveProtocols.put("MySQL", true);

        this.addProfile(linuxMachine, false);

    }

    private void addVoipServer() {
        Profile voipServer = new Profile(
                23,
                "VOIP Server",
                MainActivity.getInstance().getString(R.string.profile_voip_desc),
                R.drawable.ic_profile_asterisks,
                false
        );

        voipServer.mActiveProtocols.put("SIP", true);

        this.addProfile(voipServer, false);

    }

    private void addRandomProfile() {
        Profile randomProfile = new Profile(
                30,
                "Random",
                MainActivity.getInstance().getString(R.string.profile_random_desc),
                R.drawable.ic_launcher,
                false
        );

        randomProfile.mIsRandom = true;

        this.addProfile(randomProfile, false);

    }

    private void addNuclearPlantProfile() {
        Profile nuclearPlant = new Profile(
                8,
                "Nuclear Power Plant",
                MainActivity.getInstance().getString(R.string.profile_nuclearPower_desc),
                R.drawable.ic_nuclearpp,
                false
        );

        nuclearPlant.mActiveProtocols.put("MODBUS", true);
        nuclearPlant.mActiveProtocols.put("HTTP", true);
        nuclearPlant.mActiveProtocols.put("FTP", true);
        nuclearPlant.mActiveProtocols.put("TELNET", true);
        nuclearPlant.mActiveProtocols.put("S7COMM", true);
        nuclearPlant.mActiveProtocols.put("SMTP", true);

        //Activate as default profile
        nuclearPlant.mActivated = true;
        this.activateProfile(nuclearPlant, false);

        this.addProfile(nuclearPlant, false);
    }

    private void addWaterPlantProfile() {
        Profile waterPlant = new Profile(
                9,
                "Water Distribution & Treatment Plant",
                MainActivity.getInstance().getString(R.string.profile_waterPlant_desc),
                R.drawable.ic_profile_water_dist,
                false
        );

        waterPlant.mActiveProtocols.put("MODBUS", true);
        waterPlant.mActiveProtocols.put("HTTP", true);
        waterPlant.mActiveProtocols.put("FTP", true);
        waterPlant.mActiveProtocols.put("TELNET", true);
        this.addProfile(waterPlant, false);

    }

    private void addModbusMasterProfile() {
        Profile modbusMaster = new Profile(
                10,
                "Modbus Master",
                MainActivity.getInstance().getString(R.string.profile_modbusMater_desc),
                R.drawable.ic_modbus_master,
                false
        );

        modbusMaster.mActiveProtocols.put("MODBUS", true);
        modbusMaster.mActiveProtocols.put("SMB", true);
        modbusMaster.mActiveProtocols.put("S7COMM", true);
        modbusMaster.mActiveProtocols.put("ECHO", true);

        this.addProfile(modbusMaster, false);

    }

    private void addSNMPProfile() {
        Profile SNMPProfile = new Profile(
                11,
                "SNMP",
                "This profile provides SNMP service",
                R.drawable.ic_profile_snmp,
                false

        );

        SNMPProfile.mActiveProtocols.put("SNMP", true);
        this.addProfile(SNMPProfile, false);
    }

    private void addVigilantProfile() {
        Profile vigilantProfile = new Profile(
                29,
                "Vigilant",
                MainActivity.getInstance().getString(R.string.profile_vigilant_desc),
                R.drawable.ic_profile_vigilant,
                false
        );

        for (String protocol : MainActivity.getContext().getResources().getStringArray(R.array.protocols)) {
            vigilantProfile.mActiveProtocols.put(protocol, true);
        }

        this.addProfile(vigilantProfile, false);
    }

    private void addMQTTBrokerProfile() {
        Profile mqttBroker = new Profile(
                13,
                "MQTT Broker",
                "This profile simulates an MQTT Broker",
                R.drawable.ic_profile_broker,
                false
        );

        mqttBroker.mActiveProtocols.put("MQTT", true);
        this.addProfile(mqttBroker, false);

    }

    private void addMQTTSensorProfile() {
        Profile mqttSensor = new Profile(
                14,
                "MQTT Sensor",
                "This profile simulates a Humidity/Temperature Sensor",
                R.drawable.ic_profile_sensor,
                false
        );

        mqttSensor.mActiveProtocols.put("MQTT", true);
        this.addProfile(mqttSensor, false);
    }

    private void addSmokeSensorProfile() {
        Profile smokeSensor = new Profile(
                15,
                "ESP8266 Smoke Sensor",
                "This profile simulates an ESP8266 smoke detector",
                R.drawable.ic_profile_smoke_detector,
                false
        );

        smokeSensor.mActiveProtocols.put("COAP", true);
        this.addProfile(smokeSensor, false);
    }

    private void addArduinoProfile() {
        Profile arduino = new Profile(
                16,
                "Arduino",
                "This profile simulates an Arduino device",
                R.drawable.ic_profile_arduino,
                false
        );

        arduino.mActiveProtocols.put("HTTP", true);
        arduino.mActiveProtocols.put("MQTT", true);
        arduino.mActiveProtocols.put("AMQP", true);
        arduino.mActiveProtocols.put("COAP", true);
        arduino.mActiveProtocols.put("TELNET", true);

        this.addProfile(arduino, false);
    }

    public static String getProfile() {
        String sharedPreferencePath = Hostage.getContext().getString(
                R.string.shared_preference_path);
        return Hostage.getContext().getSharedPreferences(sharedPreferencePath, Context.MODE_PRIVATE).getString("os", "");
    }


}
