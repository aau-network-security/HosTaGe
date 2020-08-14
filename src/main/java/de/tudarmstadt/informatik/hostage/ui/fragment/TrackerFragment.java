package de.tudarmstadt.informatik.hostage.ui.fragment;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import de.tudarmstadt.informatik.hostage.HostageApplication;


/**
 * @author Alexander Brakowski
 * @created 01.04.14 19:04
 */
public class TrackerFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		final Activity activity = getActivity();

		if (activity != null) {
			// tracking stuff
			Tracker t = ((HostageApplication) activity.getApplication()).getTracker();
			t.setScreenName(this.getClass().getName());
			t.send(new HitBuilders.AppViewBuilder().build());
		}

		return v;
	}
}
