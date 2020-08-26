package dk.aau.netsec.hostage.ui.fragment;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;


import dk.aau.netsec.hostage.HostageApplication;


/**
 * @author Alexander Brakowski
 * @created 01.04.14 19:04
 */
public class TrackerFragment extends Fragment {
	private View v;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		v = super.onCreateView(inflater, container, savedInstanceState);

		final Activity activity = getActivity();

		if (activity != null) {
			// tracking stuff
			Tracker t = ((HostageApplication) activity.getApplication()).getTracker();
			t.setScreenName(this.getClass().getName());
			t.send(new HitBuilders.AppViewBuilder().build());
		}
		return v;
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(v!=null) {
			unbindDrawables(v);
			v=null;
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if(v!=null) {
			unbindDrawables(v);
			v=null;
		}
	}

	private void unbindDrawables(View view) {
		if (view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			((ViewGroup) view).removeAllViews();
		}
	}
}
