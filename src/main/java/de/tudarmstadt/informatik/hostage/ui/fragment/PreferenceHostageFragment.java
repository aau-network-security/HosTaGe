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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
	private static Boolean enabledHpfeeds = false;
	private static Boolean enabledMultistage = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		// these preferences are all text preferences
		final String[] textPreferences = new String[]{
				"pref_external_location",
				"pref_max_connections",
				"pref_timeout",
				"pref_sleeptime",
				"pref_location_time",
				"pref_location_retries",
				"pref_portscan_timeout"
		};

		mPrefValuePreviewSet = new HashSet<>();
		mPrefValuePreviewSet.add("pref_external_location");

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
		CheckBoxPreference checkboxPrefHpfeeds = (CheckBoxPreference)getPreferenceManager().findPreference("pref_hpfeeds_server");
		if(checkboxPrefMultiStage !=null)
			checkMultistage(checkboxPrefMultiStage);
		if(checkboxPrefHpfeeds !=null)
			checkHpfeeds(checkboxPrefHpfeeds);
	}

	private void checkMultistage(CheckBoxPreference checkboxPrefMultiStage){
		if(checkboxPrefMultiStage.isChecked() && !enabledMultistage)
			confirmMultistage(checkboxPrefMultiStage).create().show();
		checkboxPrefMultiStage.setOnPreferenceChangeListener(((preference, newValue) -> {
			boolean myValue = (Boolean) newValue;
			if (!myValue) {
				stopMultiStage();
				enabledMultistage = false;
			}
			return true;
		}));

	}

	private void checkHpfeeds(CheckBoxPreference checkboxPrefHpfeeds){
		if(checkboxPrefHpfeeds.isChecked() && !enabledHpfeeds)
			confirmHpfeeds(checkboxPrefHpfeeds).create().show();
	}

	private MaterialAlertDialogBuilder confirmMultistage(CheckBoxPreference checkboxPrefMultiStage){
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
		builder.setTitle("Attention");
		builder.setMessage("if you enable this service,it may use a lot of memory and drain your battery faster.");
		builder.setPositiveButton("Enable", (dialog, which) -> {
			startMultiStage();
			enabledMultistage = true;
		});
		builder.setNegativeButton(R.string.close, (dialog, which) -> {
			checkboxPrefMultiStage.setChecked(false);
			enabledMultistage = false;
		});

		return builder;
	}

	private MaterialAlertDialogBuilder confirmHpfeeds(CheckBoxPreference checkboxPrefHpFeeds){
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
		builder.setTitle("GDPR Disclaimer");
		builder.setMessage("The data collected from HosTaGe attack records will be stored for Advanced Collaborative Threat Intelligence. GDPR sensitive data include but are not limited to the public IP address of the publishing HosTaGe device. Please note that the physical geographical location of the participating HosTaGe publisher can be determined with the public IP address. Furthermore, hpfeeds include the IP address and the ports of the attack sources. Exclusive access to the hpfeeds repository is provided only with an internal review process.\n" +
				"\n" +
				"By enabling hpfeeds, the user of this app agrees to send Aalborg University (Denmark) attack data and provides his consent to use this data to process threat intelligence. The user has the right to ask for the deletion of the data published by him/her. Please contact hostage@es.aau.dk for queries and more information.");
		builder.setPositiveButton("Enable", (dialog, which) -> {
			enabledHpfeeds = true;
		});
		builder.setNegativeButton(R.string.close, (dialog, which) -> {
			checkboxPrefHpFeeds.setChecked(false);
			enabledHpfeeds = false;
		});

		return builder;
	}


	public void stopMultiStage() {
		Context context =Hostage.getContext();
		alarm.cancelAlarm(context);
	}

	private void startMultiStage() {
		Context context = Hostage.getContext();
		Intent intent = new Intent(context, MultiStageAlarm.class);

		if (alarm != null) {
			alarm.onReceive(context,intent);
			alarm.setAlarm(context);
		} else {
			Toast.makeText(context, "Alarm is null", Toast.LENGTH_SHORT).show();
		}
	}
}
