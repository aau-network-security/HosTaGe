package de.tudarmstadt.informatik.hostage;

import android.app.Application;

import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.GoogleAnalytics;

/**
 * Created by Fabio Arnold on 28.03.14.
 */
public class HostageApplication extends Application {
	private Tracker mAppTracker = null;

	public synchronized Tracker getTracker() {
		GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
		if (mAppTracker == null) {
			mAppTracker = analytics.newTracker(R.xml.app_tracker);
		}
		return mAppTracker;
	}
}
