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
import java.util.function.Consumer;

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
//TODO analyze and optimize class
public class CustomLocationManager {

    Location mLatestLocation;
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private Consumer<Location> mLocationConsumer;
    Set<LocationSource.OnLocationChangedListener> mListOfListeners;
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

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                mLatestLocation = location;

                for (LocationSource.OnLocationChangedListener listener : mListOfListeners) {
                    listener.onLocationChanged(location);
                }

            }
        };

        mLocationConsumer = new Consumer<Location>() {
            @Override
            public void accept(Location location) {
                mLatestLocation = location;
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

    public boolean isLocationPermissionGranted(Context context) {
        return (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * TODO write javadoc
     *
     * @param fragment
     */
    public void getLocationPermission(Fragment fragment) {
        requestForegroundLocationPermission(fragment);
    }

    /**
     * TODO write javadoc
     *
     * @param context
     * @return
     */
    public boolean isBackgroundPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return (context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    /**
     * TODO write javadoc
     *
     * @param fragment
     */
    public void getBackgroundPermission(Fragment fragment) {
        requestBackgroundLocationPermission(fragment);
    }

    /**
     * TODO write javadoc
     *
     * @param callback
     * @throws LocationException
     * @throws SecurityException
     */
    public void startReceiveingLocation(LocationSource.OnLocationChangedListener callback) throws LocationException, SecurityException {
        registerLocationListener(callback);
    }

    /**
     * TODO write javadoc
     *
     * @param callback
     */
    public void stopReceivingLocation(LocationSource.OnLocationChangedListener callback) {
        unregisterLocationListener(callback);
    }

    /**
     * TODO write javadoc
     *
     * @param keepUpdated
     * @throws LocationException
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
     * TODO write javadoc
     *
     * @return
     * @throws LocationException
     */
    public Location getLatestLocation() throws LocationException {
        return getLatestLocation(null);
    }

    /**
     * TODO write javadoc
     *
     * @param context
     * @return
     * @throws LocationException
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
     * TODO write javadoc
     *
     * @return
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

    /**
     * Register a location listener. This listener will get called whenever a new location is provided.
     * <p>
     * Start periodically updating location, if not doing that already.
     *
     * @param listener {@link com.google.android.gms.maps.LocationSource.OnLocationChangedListener}
     *                 to be registered
     * @throws LocationException Thrown when the location permission has not been granted, or no
     *                           location provider is currently enabled.
     *                           TODO update javadoc
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
     * TOdO write javadoc
     */
    private void stopIfNoListeners() {
        if (mListOfListeners.isEmpty() && !mKeepLocationUpdated) {
            stopUpdatingLocation();
        }
    }

    /**
     * Internal use only.
     * <p>
     * TODO update javadoc
     * Starts periodically updating location.
     *
     * @throws LocationException Thrown when the location permission has not been granted, or if
     *                           no location providers are available (e.g. GPS, WiFi and Cellular
     *                           all turned off)
     */
    private void startUpdatingLocation() throws LocationException, SecurityException {
        String preferredProvider;

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
     * Launch a location permission request. Display the rationale for using location, if necessary.
     * TODO update javadoc
     */
    private void requestForegroundLocationPermission(Fragment fragment) {
        // If needed, show explanation
        Context context = fragment.getContext();

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(context.getResources().getString(R.string.uses_location));
        dialog.setMessage(context.getResources().getString(R.string.uses_location_reason));
        dialog.setCancelable(true);
        dialog.setPositiveButton(context.getResources().getString(R.string.ok), ((dialog1, which) -> {
            showLocationRequestDialog(fragment, Manifest.permission.ACCESS_FINE_LOCATION);
        }));

        AlertDialog alertDialog = dialog.create();
        alertDialog.show();
    }

    /**
     * Launch a background location permission request. Show a rationale explaining this request if
     * needed.
     * TODO update javadoc
     */
    private void requestBackgroundLocationPermission(Fragment fragment) {
        Context context = fragment.getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(context.getResources().getString(R.string.uses_background_location));
            dialog.setMessage(context.getResources().getString(R.string.uses_background_location_reason));
            dialog.setCancelable(true);

            dialog.setPositiveButton(context.getResources().getString(R.string.ok), ((dialog1, which) -> {
                showLocationRequestDialog(fragment, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }));

            AlertDialog alertDialog = dialog.create();
            alertDialog.show();

        }
    }

    /**
     * Display the actual dialog asking the user for permissions.
     * <p>
     * TODO update javadoc
     *
     * @param locationPermissionType Ask for either foreground or background location permission.
     */
    void showLocationRequestDialog(Fragment fragmnent, String locationPermissionType) {
        if (locationPermissionType == Manifest.permission.ACCESS_FINE_LOCATION || locationPermissionType == Manifest.permission.ACCESS_COARSE_LOCATION) {
            fragmnent.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);

            // Only relevant on newer APIs
        } else if (locationPermissionType == Manifest.permission.ACCESS_BACKGROUND_LOCATION &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fragmnent.requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE);
        }
    }
}
