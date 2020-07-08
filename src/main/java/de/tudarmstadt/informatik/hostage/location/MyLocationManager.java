package de.tudarmstadt.informatik.hostage.location;

import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;


/**
 * This Class is used to get Location data. You can get the last found Location or start searching for new hostage.location data.
 * @author Lars Pandikow
 */
public class MyLocationManager implements ActivityCompat.OnRequestPermissionsResultCallback {
	private Context context;
	private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

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
	 * Static variable that always holds the newest hostage.location update.
	 */
	private static Location newestLocation = null;

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	public static Location getNewestLocation() {
		return newestLocation;
	}

	// Define a listener that responds to hostage.location updates
	LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			// Called when a new hostage.location is found by the network hostage.location
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
		this.context = context;
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(MainActivity.getInstance(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);

			return;
		}
		newestLocation = locationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

	}


	/**
	 * Starts updating the hostage.location data for the given amount of time. Calls
	 * itself recursive if no hostage.location data has been found yet and there are
	 * still attempts left.
	 *
	 * @param time
	 *            Time to update hostage.location data
	 * @param attempts
	 *            Remaining attempts for recieving hostage.location data
	 */
	public void getUpdates(long time, int attempts, Context context) {
		startUpdates(context);

		attempts--;
		Timer timer1 = new Timer();
		timer1.schedule(new StopTask(), time);
		if (newestLocation == null && attempts > 0)
			getUpdates(time, attempts, context);
	}

	/**
	 * Start updating
	 * {@link MyLocationManager#newestLocation
	 * newestLocation} if a hostage.location provider is enabled and available.
	 */

	public void startUpdates(Context context) {
		boolean gpsEnabled = false;
		boolean networkEnabled = false;
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		// exceptions will be thrown if provider is not permitted.
		try {
			gpsEnabled = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			networkEnabled = locationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// don't start listeners if no provider is enabled
		if (!gpsEnabled && !networkEnabled)
			return;

		// Register the listener with the Location Manager to receive hostage.location updates
		if (gpsEnabled)
			requestLocationUpdates( networkEnabled, context);
	}

	private void requestLocationUpdates(boolean networkEnabled,Context context) {
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

			ActivityCompat.requestPermissions(MainActivity.getInstance(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);

			return;
		}
		locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		if (networkEnabled)
			locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

	}


    /**
     * Callback for requestPermission method. Creates an AlertDialog for the user in order to allow the permissions or not.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == 10) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

			} else {
				if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.getInstance(), Manifest.permission.ACCESS_FINE_LOCATION)) {
					AlertDialog.Builder dialog = new AlertDialog.Builder(context);
					dialog.setTitle("Permission Required");
					dialog.setCancelable(false);
					dialog.setMessage("You have to Allow permission to access user location");
					dialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package",
									context.getPackageName(), null));
						}
					});
					AlertDialog alertDialog = dialog.create();
					alertDialog.show();
				}
				//Code for deny if needed
			}
		}
	}


	/**
	 * Stop updating the hostage.location.
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
			// A new hostage.location is always better than no hostage.location
			return true;
		}

		// Check whether the new hostage.location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;

		// If it's been more than two minutes since the current hostage.location, use
		// the new hostage.location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new hostage.location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new hostage.location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());

		// Determine hostage.location quality using a combination of timeliness and
		// accuracy
        return accuracyDelta < 0;
    }
}
