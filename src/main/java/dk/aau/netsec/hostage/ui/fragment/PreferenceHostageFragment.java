package dk.aau.netsec.hostage.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.services.MultiStageAlarm;

/**
 * Manages and creates the application preferences view
 *
 * @author Alexander Brakowski
 * @created 02.03.14 21:03
 * @modified Shreyas Srinivasa
 */
public class PreferenceHostageFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    private static final int REQUEST_CODE_ALERT_RINGTONE = 1111;
    public static final String FRAGMENT_TAG = "my_preference_fragment";
    public static final String PREFERENCE_KEY_MULTISTAGE = "pref_multistage";
    public static final String PREFERENCE_KEY_HPFEEDS = "pref_hpfeeds_server";

    /**
     * Contains preferences for which to display a preview of the value in the summary
     */
    private HashSet<String> mPrefValuePreviewSet;
    MultiStageAlarm alarm = new MultiStageAlarm();
    private static Boolean enabledHpfeeds = false;
    private static Boolean enabledMultistage = false;

    /**
     * Called during {@link #onCreate(Bundle)} to supply the preferences for this fragment.
     * directly or via helper methods such as {@link #addPreferencesFromResource(int)}.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     *                           this is the state.
     * @param rootKey            If non-null, this preference fragment should be rooted at the
     * @deprecated Use {@link PreferenceFragmentCompat} instead
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // these preferences are all text preferences
        final String[] textPreferences = new String[]{
                "pref_max_connections",
                "pref_timeout",
                "pref_sleeptime",
                "pref_portscan_timeout"
        };

        mPrefValuePreviewSet = new HashSet<>();

        setPreferencesFromResource(R.xml.settings_preferences, rootKey);

        for (String k : textPreferences) {
            updatePreferenceSummary(k);
        }
    }


    /**
     * Updates the summary text of the given preference
     *
     * @param key the preference key
     */
    private void updatePreferenceSummary(String key) {
        Preference p = findPreference(key);
        SharedPreferences sharedPreferences = this.getPreferenceManager().getSharedPreferences();

        if (p instanceof EditTextPreference) {
            if (mPrefValuePreviewSet.contains(key)) {
                p.setSummary(sharedPreferences.getString(key, ""));
            }
        }
    }

    /**
     * Workaround for RingtonePreference missing in Android X library.
     * Refer to issue https://issuetracker.google.com/issues/37057453#comment3
     *
     * @param preference ringtone Preference
     * @return Preference
     */
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals("pref_notification_sound")) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);

            String existingValue = getRingtonePreferenceValue();
            if (existingValue != null) {
                if (existingValue.length() == 0) {
                    // Select "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
                }
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
            }

            startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE);
            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }

    private String getRingtonePreferenceValue() {
        return this.getPreferenceManager().getSharedPreferences().getString("pref_notification_sound", "content://settings/system/notification_sound");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ALERT_RINGTONE && data != null) {
            Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (ringtone != null) {
                setRingtonPreferenceValue(ringtone.toString());
            } else {
                // "Silent" was selected
                setRingtonPreferenceValue("");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setRingtonPreferenceValue(String ringtone) {
        this.getPreferenceManager().getSharedPreferences().edit().putString("pref_notification_sound", ringtone).apply();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreferenceSummary(key);

        if (key.equals(PREFERENCE_KEY_MULTISTAGE)) {
            checkMultistage();
        } else if (key.equals(PREFERENCE_KEY_HPFEEDS)) {
            checkHpfeeds();
        }
    }

    private void checkMultistage() {
        CheckBoxPreference checkboxPrefMultiStage = (CheckBoxPreference) getPreferenceManager().findPreference(PREFERENCE_KEY_MULTISTAGE);
        if (checkboxPrefMultiStage.isChecked()) {
            confirmMultistage(checkboxPrefMultiStage).create().show();
        } else {
            stopMultiStage();
            enabledMultistage = false;
        }
    }

    private void checkHpfeeds() {
        CheckBoxPreference checkboxPrefHpfeeds = (CheckBoxPreference) getPreferenceManager().findPreference(PREFERENCE_KEY_HPFEEDS);

        if (checkboxPrefHpfeeds.isChecked() && !enabledHpfeeds)
            confirmHpfeeds(checkboxPrefHpfeeds).create().show();
    }

    private MaterialAlertDialogBuilder confirmMultistage(CheckBoxPreference checkboxPrefMultiStage) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(R.string.warning);
        builder.setMessage(R.string.multistage_warning);
        builder.setPositiveButton(R.string.enable, (dialog, which) -> {
            startMultiStage();
            enabledMultistage = true;
        });
        builder.setNegativeButton(R.string.close, (dialog, which) -> {
            checkboxPrefMultiStage.setChecked(false);
            enabledMultistage = false;
        });

        return builder;
    }

    private MaterialAlertDialogBuilder confirmHpfeeds(CheckBoxPreference checkboxPrefHpFeeds) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(R.string.data_disclaimer);
//		TODO extract strings
        builder.setMessage("The data collected from HosTaGe attack records will be stored for Advanced Collaborative Threat Intelligence. GDPR sensitive data include but are not limited to the public IP address of the publishing HosTaGe device. Please note that the physical geographical location of the participating HosTaGe publisher can be determined with the public IP address. Furthermore, hpfeeds include the IP address and the ports of the attack sources. Exclusive access to the hpfeeds repository is provided only with an internal review process.\n" +
                "\n" +
                "By enabling hpfeeds, the user of this app agrees to send Aalborg University (Denmark) attack data and provides his consent to use this data to process threat intelligence. The user has the right to ask for the deletion of the data published by him/her. Please contact hostage@es.aau.dk for queries and more information.");
        builder.setPositiveButton(R.string.enable, (dialog, which) -> {
            enabledHpfeeds = true;
        });
        builder.setNegativeButton(R.string.close, (dialog, which) -> {
            checkboxPrefHpFeeds.setChecked(false);
            enabledHpfeeds = false;
        });

        return builder;
    }

    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    //TODO Replace with separate Fragments as suggested in https://developer.android.com/guide/topics/ui/settings/organize-your-settings#split_your_hierarchy_into_multiple_screens

    /**
     * SubsScreens in Android X are no longer Supported.
     * Workaround with https://stackoverflow.com/questions/32494548/how-to-move-back-from-preferences-subscreen-to-main-screen-in-preferencefragment
     *
     * @param preferenceFragmentCompat preferenceFragmentCompat
     * @param preferenceScreen         preferenceScreen
     * @return back in the initial screen
     */
    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        PreferenceHostageFragment fragment = new PreferenceHostageFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);
        ft.replace(R.id.settings_fragment_container, fragment, preferenceScreen.getKey());
        ft.addToBackStack(preferenceScreen.getKey());
        ft.commit();
        return true;
    }


    public void stopMultiStage() {
        Context context = Hostage.getContext();
        alarm.cancelAlarm(context);
    }

    private void startMultiStage() {
        Context context = Hostage.getContext();
        Intent intent = new Intent(context, MultiStageAlarm.class);

        if (alarm != null) {
            alarm.onReceive(context, intent);
            alarm.setAlarm(context);
        } else {
            Snackbar.make(getView(), "Alarm is null", Snackbar.LENGTH_SHORT).show();
        }
    }
}
