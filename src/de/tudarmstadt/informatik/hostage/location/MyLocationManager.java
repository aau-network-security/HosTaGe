package de.tudarmstadt.informatik.hostage.location;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * This Class is used to get Location data. You can get the last found Location or start searching for new location data.
 * @author Lars Pandikow
 */
public class MyLocationManager {

	/**
	 * TimerTask to stop updates after a given time.
	 */
	class StopTask extends TimerTask {
		@Override
		public void run() {
			stopUpdates();
		}
	}

	private LocationManager locationManager;
	/**
	 * Static variable that always holds the newest location update.
	 */
	private static Location newestLocation = null;

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	public static Location getNewestLocation() {
		return newestLocation;
	}

	// Define a listener that responds to location updates
	LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			// Called when a new location is found by the network location
			// provider.
			if (isBetterLocation(location, newestLocation)) {
				newestLocation = location;
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	public MyLocationManager(Context context) {
		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		newestLocation = locationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	}

	/**
	 * Starts updating the location data for the given amount of time. Calls
	 * itself recursive if no location data has been found yet and there are
	 * still attempts left.
	 * 
	 * @param time
	 *            Time to update location data
	 * @param attempts
	 *            Remaining attempts for recieving location data
	 */
	public void getUpdates(long time, int attempts) {
		startUpdates();
		attempts--;
		Timer timer1 = new Timer();
		timer1.schedule(new StopTask(), time);
		if (newestLocation == null && attempts > 0)
			getUpdates(time, attempts);
	}

	/**
	 * Start updating
	 * {@link de.tudarmstadt.informatik.hostage.location.MyLocationManager#newestLocatio
	 * newestLocation} if a location provider is enabled and available.
	 */
	public void startUpdates() {
		boolean gpsEnabled = false;
		boolean networkEnabled = false;
		// exceptions will be thrown if provider is not permitted.
		try {
			gpsEnabled = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}
		try {
			networkEnabled = locationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
		}

		// don't start listeners if no provider is enabled
		if (!gpsEnabled && !networkEnabled)
			return;

		// Register the listener with the Location Manager to receive location updates
		if (gpsEnabled)
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		if (networkEnabled)
			locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	}

	/**
	 * Stop updating the location.
	 */
	public void stopUpdates() {
		locationManager.removeUpdates(locationListener);
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	private boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isMoreAccurate = accuracyDelta < 0;

		// Determine location quality using a combination of timeliness and
		// accuracy
        return isMoreAccurate;
    }
}
