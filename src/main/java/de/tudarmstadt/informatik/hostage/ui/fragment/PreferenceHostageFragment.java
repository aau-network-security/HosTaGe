package de.tudarmstadt.informatik.hostage.ui.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.util.HashSet;

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

	MultiStageAlarm alarm;


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);


		// these preferences are all text preferences
		final String[] textPreferences = new String[]{
				"pref_external_location",
				"pref_upload_server",
                "pref_download_server",
				"pref_max_connections",
				"pref_timeout",
				"pref_sleeptime",
				"pref_location_time",
				"pref_location_retries",
				"pref_portscan_timeout"
		};

		mPrefValuePreviewSet = new HashSet<String>();
		mPrefValuePreviewSet.add("pref_external_location");
		mPrefValuePreviewSet.add("pref_upload_server");

		addPreferencesFromResource(R.xml.settings_preferences);

		/*final Context context;

		context= Hostage.getContext().getApplicationContext();
		final CheckBoxPreference multi = (CheckBoxPreference)getPreferenceManager().findPreference("pref_multistage");
		multi.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean myValue = (Boolean) newValue;

				MultiStageAlarm alarm = new MultiStageAlarm();

				if(myValue){
					if (alarm != null) {
						alarm.SetAlarm(context);
					}
				}
				else if(!myValue){
					alarm.CancelAlarm(context);
				}

				return false;
			}
		});*/






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

		if(p != null && p instanceof EditTextPreference) {
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
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

/*	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		super.onPreferenceTreeClick(preferenceScreen, preference);

		if (preference instanceof PreferenceScreen) {
			if(MainActivity.getInstance().mDisplayedFragment != null && MainActivity.getInstance().mDisplayedFragment instanceof UpNavigatible){
				((UpNavigatible) MainActivity.getInstance().mDisplayedFragment).setUpNavigatible(true);
				((UpNavigatible) MainActivity.getInstance().mDisplayedFragment).setUpFragment(SettingsFragment.class);
				MainActivity.getInstance().setDrawerIndicatorEnabled(false);
				return true;
			}
		}

		return false;
	}*/

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePreferenceSummary(key);

		CheckBoxPreference checkboxPref1 = (CheckBoxPreference)getPreferenceManager().findPreference("pref_multistage");

		checkboxPref1.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference, Object newValue) {

				boolean myValue = (Boolean) newValue;

				if (myValue)
					//startService(new Intent(getActivity(), MultiStage.class));
					getActivity().startService(new Intent(getActivity(), MultiStageAlarm.class));


				else
					//	stopService(new Intent(getActivity(), MultiStage.class));
						getActivity().stopService(new Intent(getActivity(),MultiStageAlarm.class));
					//


/*
				Intent myIntent = new Intent(getActivity(), MultiStageAlarm.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, myIntent, 0);
				AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
				// cancel the alarm
				alarmManager.cancel(pendingIntent);
				// delete the PendingIntent
				pendingIntent.cancel();*/

				return true;
			}
		});



	}
}
