package dk.aau.netsec.hostage.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.LocationSource;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import dk.aau.netsec.hostage.R;

/**
 * CustomLocationManager is responsible for all location-related tasks in the app. This includes
 * obtaining permissions for accessing location and background location from the user,
 * and periodically obtaining new location from the device.
 * <p>
 * CustomLocationManager uses a singleton pattern and only a single instance should exist at any
 * given time.
 *
 * @author Filip Adamik
 * Created on 24/06/2021
 */
public class CustomLocationManager {

    Location mLatestLocation;
    private final LocationListener mLocationListener;
    private final LocationManager mLocationManager;
    final Set<LocationSource.OnLocationChangedListener> mListOfListeners;
    private boolean mReceivingUpdates = false;
    private boolean mKeepLocationUpdated = false;

    private static WeakReference<CustomLocationManager> mLocationManagerInstance;

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 816;
    public static final int LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE = 817;

    /**
     * Create a new CustomLocationManager. This constructor is only to be used internally. To obtain
     * an instance of CustomLocationManager, use {@link #getLocationManagerInstance(Context)}
     *
     * @param context app context, needed to retrieve system service.
     */
    private CustomLocationManager(Context context) {
        mLocationManagerInstance = new WeakReference<>(this);
        mListOfListeners = new HashSet<>();

        mLocationListener = location -> {
            mLatestLocation = location;

            for (LocationSource.OnLocationChangedListener listener : mListOfListeners) {
                listener.onLocationChanged(location);
            }
        };

        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Public method to obtain an instance of CustomLocationManager. If an instance exists already,
     * it is returned. If no instance exists (has not been created yet, or has been destroyed),
     * it attempts to create a new instance. For this, application context is needed. If context is
     * not provided, throws a {@link LocationException}.
     *
     * @param context Context to create a new instance of CustomLocationManager
     * @return Single instance of CustomLocationManager.
     * @throws LocationException thrown when no instance exists and context was not provided to create
     *                           a new instance.
     */
    @Nullable
    public static CustomLocationManager getLocationManagerInstance(@Nullable Context context) throws LocationException {
        if (mLocationManagerInstance == null || mLocationManagerInstance.get() == null) {
            if (context == null) {
                throw new LocationException("Could not initialize CustomLocationManager, due to Context not provided.");
            }

            return new CustomLocationManager(context);
        }

        return mLocationManagerInstance.get();
    }

    /**
     * Returns true if the app currently has foreground location permission.
     *
     * @param context Context is required to check for permissions
     * @return true if app has permission
     */
    public boolean isLocationPermissionGranted(Context context) {
        return (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Obtain location permission from the user. This will display a prominent disclosure, informing
     * the user why the location is being requested.
     *
     * @param fragment The result of the permission request is delivered back to the fragment's
     *                 onRequestPermissionResult
     */
    public void getLocationPermission(Fragment fragment) {
        requestForegroundLocationPermission(fragment);
    }

    /**
     * Returns trie if app currently has background location permission. On older devices,
     * always return true.
     *
     * @param context Context is required to check for permissions
     * @return true if the app has background locatin permissions or if API is below 29
     */
    public boolean isBackgroundPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return (context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    /**
     * Obtain background location permission from the user. This will display a prominent disclosure
     * informing the user why background location is being requested.
     *
     * @param fragment The result of the permission request is delivered back to the fragment's
     *                 onRequestPermissionResult
     */
    public void getBackgroundPermission(Fragment fragment) {
        requestBackgroundLocationPermission(fragment);
    }

    /**
     * Register a callback function where all location updates will be delivered. This will start
     * polling the sensors for periodic location updates
     *
     * @param callback method to deliver the location updates
     * @throws LocationException if no location providers are enabled on device
     * @throws SecurityException if Location Permissions are missing
     */
    public void startReceivingLocation(LocationSource.OnLocationChangedListener callback) throws LocationException, SecurityException {
        registerLocationListener(callback);
    }

    /**
     * Stop receiving location updates. If there are no more listeners, or location is no longer
     * kept updated by keepLocationUpdated, this will stop polling device sensors.
     *
     * @param callback Method that was previously registered to receive location updates
     */
    public void stopReceivingLocation(LocationSource.OnLocationChangedListener callback) {
        unregisterLocationListener(callback);
    }

    /**
     * Keep location updated without explicitly registering a listener. This will start polling
     * device sensors for location updates.
     *
     * @param keepUpdated set to true if location is to be kept updated
     * @throws LocationException if no location providers are enabled
     */
    public void keepLocationUpdated(boolean keepUpdated) throws LocationException {
        mKeepLocationUpdated = keepUpdated;

        if (keepUpdated) {
            startUpdatingLocation();
        } else {
            stopIfNoListeners();
        }
    }

    /**
     * Get single last location. This does not poll device location sensors.
     *
     * @return Last location update. Could be old.
     * @throws LocationException If no previous location is available
     */
    public Location getLatestLocation() throws LocationException {
        return getLatestLocation(null);
    }

    /**
     * Get single last location known to CustomLocationManager (more accurate), or obtain last known
     * device location (less accurate).
     * <p>
     * Last known device location is requested only if context is provided and
     * location permission has been granted.
     *
     * @param context Context is required to check for location permission
     * @return Last known location
     * @throws LocationException if no previous location is known.
     */
    @SuppressLint("MissingPermission")
    public Location getLatestLocation(Context context) throws LocationException {
        if (mLatestLocation != null) {
            return mLatestLocation;

        } else if (context != null && isLocationPermissionGranted(context)) {
            return mLocationManager.getLastKnownLocation(getBestProvider());
        }

        throw new LocationException("Latest Location could not be obtained");
    }


    /**
     * Register a location listener. This listener will get called whenever a new location is provided.
     * <p>
     * Start periodically updating location, if not doing that already.
     *
     * @param listener {@link com.google.android.gms.maps.LocationSource.OnLocationChangedListener}
     *                 to be registered
     * @throws LocationException Thrown when no location provider is currently enabled.
     * @throws SecurityException Thrown when the location permission has not been granted
     */
    private void registerLocationListener(@NonNull LocationSource.OnLocationChangedListener listener) throws LocationException, SecurityException {
        // Start updating location, if not already
        if (!mReceivingUpdates) {
            startUpdatingLocation();
        }

        mListOfListeners.add(listener);
    }

    /**
     * Deregister a location listener, if it has been registered.
     * <p>
     * If there are no more listeners registered, stop periodically updating location.
     *
     * @param listener Listener to deregister.
     */
    private void unregisterLocationListener(@NonNull LocationSource.OnLocationChangedListener listener) {
        try {
            mListOfListeners.remove(listener);
        } catch (NoSuchElementException nse) {
            nse.printStackTrace();
        }

        stopIfNoListeners();
    }

    /**
     * Stop updating location if there are no registered listeners or keepLocationUpdated has not
     * been set.
     */
    private void stopIfNoListeners() {
        if (mListOfListeners.isEmpty() && !mKeepLocationUpdated) {
            stopUpdatingLocation();
        }
    }

    /**
     * Internal use only.
     * <p>
     * Starts periodically updating location.
     *
     * @throws LocationException Thrown when no location providers are available (e.g. GPS, WiFi
     *                           and Cellular all turned off)
     * @throws SecurityException Thrown when location permission has not been granted
     */
    private void startUpdatingLocation() throws LocationException, SecurityException {
        String preferredProvider;

//        TODO consider replacing with getBestProvider()
        // Check provider availability (starting with most accurate
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            preferredProvider = LocationManager.GPS_PROVIDER;
        } else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            preferredProvider = LocationManager.NETWORK_PROVIDER;
        } else {
            throw new LocationException("No network provider enabled");
        }

        // Request periodic location updates.
        mLocationManager.requestLocationUpdates(preferredProvider, 0, 0, mLocationListener);
        mReceivingUpdates = true;
    }

    /**
     * Internal use only.
     * <p>
     * Stop receiving periodic location updates.
     */
    private void stopUpdatingLocation() {
        mLocationManager.removeUpdates(mLocationListener);
        mReceivingUpdates = false;
    }

    /**
     * Launch a location permission request. Display the prominent disclosure for using location.
     *
     * @param fragment is required to obtain context for construction of the prominent disclosure,
     *                 for requesting permission and for delivering request result.
     */
    private void requestForegroundLocationPermission(Fragment fragment) {
        Context context = fragment.getContext();

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(R.string.uses_location);
        dialog.setMessage(R.string.uses_location_reason);
        dialog.setCancelable(true);
        dialog.setPositiveButton(R.string.ok, ((dialog1, which)
                -> showLocationRequestDialog(fragment, Manifest.permission.ACCESS_FINE_LOCATION)));

        AlertDialog alertDialog = dialog.create();
        alertDialog.show();
    }

    /**
     * Launch a background location permission request. Show a rationale explaining this request if
     * needed.
     *
     * @param fragment is required to obtain context for construction of the prominent disclosure,
     *                 for requesting permission and for delivering request result.
     */
    private void requestBackgroundLocationPermission(Fragment fragment) {
        Context context = fragment.getContext();

//        Only relevant on API >= 29
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(R.string.uses_background_location);
            dialog.setMessage(R.string.uses_background_location_reason);
            dialog.setCancelable(true);

            dialog.setPositiveButton(R.string.ok, ((dialog1, which)
                    -> showLocationRequestDialog(fragment, Manifest.permission.ACCESS_BACKGROUND_LOCATION)));


            AlertDialog alertDialog = dialog.create();
            alertDialog.show();
        }
    }

    /**
     * Display the actual dialog asking the user for permissions.
     *
     * @param fragment               fragment is required to request permissions and to deliver
     *                               permission request result
     * @param locationPermissionType Ask for either foreground or background location permission.
     */
    void showLocationRequestDialog(Fragment fragment, String locationPermissionType) {
        if (locationPermissionType.equals(Manifest.permission.ACCESS_FINE_LOCATION) || locationPermissionType.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            fragment.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);

            // Only relevant on newer APIs
        } else if (locationPermissionType.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fragment.requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Find best provider for our use (we don't care about speed, altitude or bearing)
     *
     * @return LocationProvider
     */
    private String getBestProvider() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);

        return mLocationManager.getBestProvider(criteria, false);
    }
}
