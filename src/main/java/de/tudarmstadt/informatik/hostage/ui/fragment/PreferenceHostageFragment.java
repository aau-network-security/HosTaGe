package de.tudarmstadt.informatik.hostage.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;
import java.util.HashSet;
import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.services.MultiStageAlarm;

/**
 * Manages and creates the application preferences view
 *
 * @author Alexander Brakowski
 * @created 02.03.14 21:03
 * @modified Shreyas Srinivasa
*/
public class PreferenceHostageFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
	/**
	 * Contains preferences for which to display a preview of the value in the summary
	 */
	private HashSet<String> mPrefValuePreviewSet;
	MultiStageAlarm alarm = new MultiStageAlarm();
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		// these preferences are all text preferences
		final String[] textPreferences = new String[]{
				"pref_external_location",
				"pref_hpfeeds_server",
				"pref_max_connections",
				"pref_timeout",
				"pref_sleeptime",
				"pref_location_time",
				"pref_location_retries",
				"pref_portscan_timeout"
		};

		mPrefValuePreviewSet = new HashSet<>();
		mPrefValuePreviewSet.add("pref_external_location");
		//mPrefValuePreviewSet.add("pref_hpfeeds_server");

		addPreferencesFromResource(R.xml.settings_preferences);

		for(String k: textPreferences){
			updatePreferenceSummary(k);
		}
	}
	/**
	 * Updates the summary text of the given preference
	 *
	 * @param key the preference key
	 */
	private void updatePreferenceSummary(String key){
		Preference p = findPreference(key);
		SharedPreferences sharedPreferences = this.getPreferenceManager().getSharedPreferences();

		if(p instanceof EditTextPreference) {
			if (mPrefValuePreviewSet.contains(key)) {
				p.setSummary(sharedPreferences.getString(key, ""));
			}
		}
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

	@Override
	public void onStop() {
		super.onStop();
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePreferenceSummary(key);
		CheckBoxPreference checkboxPrefMultiStage = (CheckBoxPreference)getPreferenceManager().findPreference("pref_multistage");

		if(checkboxPrefMultiStage != null)
			checkMultistage(checkboxPrefMultiStage);
	}

	private void checkMultistage(CheckBoxPreference checkboxPrefMultiStage){
		checkboxPrefMultiStage.setOnPreferenceChangeListener(((preference, newValue) -> {
			boolean myValue = (Boolean) newValue;

			if (myValue) {
				startMultiStage();
			}
			else {
				stopMultiStage();
			}
			return true;
		}));

	}

	public void stopMultiStage() {
		Context context =Hostage.getContext();
		alarm.CancelAlarm(context);
	}

	private void startMultiStage() {
		Context context = Hostage.getContext();
		Intent intent = new Intent(context, MultiStageAlarm.class);

		if (alarm != null) {
			alarm.onReceive(context,intent);
			alarm.SetAlarm(context);
		} else {
			Toast.makeText(context, "Alarm is null", Toast.LENGTH_SHORT).show();
		}
	}
}
