package dk.aau.netsec.hostage.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.LocationSource;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.ui.activity.MainActivity;

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
    private Context mContext;
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private Consumer<Location> mLocationConsumer;
    private boolean locationPermissionDenied;
    Set<LocationSource.OnLocationChangedListener> mListOfListeners;
    private boolean mReceivingUpdates = false;

    private static WeakReference<CustomLocationManager> mLocationManagerInstance;

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /**
     * Create a new CustomLocationManager. This constructor is only to be used internally. To obtain
     * an instance of CustomLocationManager, use {@link #getLocationManagerInstance(Context)}
     *
     * @param context app context, needed to retrieve system service.
     */
    private CustomLocationManager(@NonNull Context context) {
        mLocationManagerInstance = new WeakReference<>(this);
        mContext = context;
        mListOfListeners = new HashSet<>();

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

//                if (true) { //only for testing
                if (isBetterLocation(location)) {
                    mLatestLocation = location;

                    for (LocationSource.OnLocationChangedListener listener : mListOfListeners) {
                        listener.onLocationChanged(location);
                    }
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

    /**
     * Returns latest available location. If no previous location is available, it will attempt to
     * obtain a new location, unless user already denied a location permission.
     * <p>
     * If a location permission has previously been denied, it throws a {@link LocationException}
     * <p>
     * If the user has granted a location permission, but no previous location exists, this method
     * may temporarily return null, in order not to hang while fresh location is retrieved.
     *
     * @return Latest available location. Can return null if location is not ready yet.
     * @throws LocationException if Location permission was denied by the user.
     */
    @Nullable
    public Location getLatestLocation() throws LocationException {
        if (mLatestLocation != null) {
            return mLatestLocation;
        } else if (!locationPermissionDenied) {
            updateLocation();
            return mLatestLocation;
        } else {
            throw new LocationException("Location permission has not been granted");
        }
    }

    /**
     * Launch a background location permission request. Show a rationale explaining this request if
     * needed.
     */
    @SuppressLint("InlinedApi")
    public void requestBackgroundLocation() {
        if (ActivityCompat.shouldShowRequestPermissionRationale((MainActivity) mContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
            dialog.setTitle(mContext.getResources().getString(R.string.uses_background_location));
            dialog.setMessage(mContext.getResources().getString(R.string.uses_background_location_reason));
            dialog.setCancelable(true);
            dialog.setNeutralButton(mContext.getResources().getString(R.string.ok), ((dialog1, which) -> {
                showLocationRequestDialog(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }));

            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    showLocationRequestDialog(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                }
            });

            AlertDialog alertDialog = dialog.create();
            alertDialog.show();
        } else {
            showLocationRequestDialog(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
    }

    /**
     * Save the information that user has already denied a location permission request. This
     * is useful so we do not keep asking the user repeatedly.
     *
     * @param locationPermissionDenied true if user has denied a location permission request.
     */
    public void userHasDeniedLocation(boolean locationPermissionDenied) {
        this.locationPermissionDenied = locationPermissionDenied;
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
     */
    public void registerCustomLocationListener(@NonNull LocationSource.OnLocationChangedListener listener) throws LocationException {
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
    public void unregisterCustomLocationListener(@NonNull LocationSource.OnLocationChangedListener listener) {
        try {
            mListOfListeners.remove(listener);

        } catch (NoSuchElementException nse) {
            nse.printStackTrace();
        }

        // If last listener has been removed, stop updating location.
        if (mListOfListeners.isEmpty()) {
            stopUpdatingLocation();
        }
    }

    /**
     * Called from {@link MainActivity} if the user grants the location permission.
     * <p>
     * If API is 30 or above, update location a single time. API 29 and below, obtaining one-time
     * location is not possible, so start with periodic location updates.
     */
    @SuppressLint("MissingPermission")
    public void permissionGrantedCallback() {
        locationPermissionDenied = false;
        // getCurrentLocation is only supported in API 30 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mLocationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, mContext.getMainExecutor(), mLocationConsumer);
        } else {
            try {
                startUpdatingLocation();
            } catch (LocationException le) {
                le.printStackTrace();
            }
        }
    }

    /**
     * Internal use only.
     * <p>
     * If location permission has not been granted, ask for it. Otherwise retrieve the latest location.
     */
    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            permissionGrantedCallback();
        }
    }

    /**
     * Internal use only.
     * <p>
     * Starts periodically updating location.
     *
     * @throws LocationException Thrown when the location permission has not been granted, or if
     *                           no location providers are available (e.g. GPS, WiFi and Cellular
     *                           all turned off)
     */
    private void startUpdatingLocation() throws LocationException {
        String preferredProvider;

        // Check provider availability (starting with most accurate
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            preferredProvider = LocationManager.GPS_PROVIDER;
        } else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            preferredProvider = LocationManager.NETWORK_PROVIDER;
        } else {
            throw new LocationException("No network provider enabled");
        }

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            throw new LocationException("Location permission has not been granted");
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
     */
    private void requestLocationPermission() {
        // If needed, show explanation
        if (ActivityCompat.shouldShowRequestPermissionRationale((MainActivity) mContext, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
            dialog.setTitle(mContext.getResources().getString(R.string.uses_location));
            dialog.setMessage(mContext.getResources().getString(R.string.uses_location_reason));
            dialog.setCancelable(true);
            dialog.setNeutralButton(mContext.getResources().getString(R.string.ok), ((dialog1, which) -> {
                showLocationRequestDialog(Manifest.permission.ACCESS_FINE_LOCATION);
            }));

            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    showLocationRequestDialog(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            });

            AlertDialog alertDialog = dialog.create();
            alertDialog.show();
        } else {
            showLocationRequestDialog(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Display the actual dialog asking the user for permissions.
     *
     * @param locationPermissionType Ask for either foreground or background location permission.
     */
    void showLocationRequestDialog(String locationPermissionType) {
        if (locationPermissionType == Manifest.permission.ACCESS_FINE_LOCATION || locationPermissionType == Manifest.permission.ACCESS_COARSE_LOCATION) {
            ActivityCompat.requestPermissions(MainActivity.getInstance(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainActivity.LOCATION_PERMISSION_REQUEST_CODE);

            // Only relevant on newer APIs
        } else if (locationPermissionType == Manifest.permission.ACCESS_BACKGROUND_LOCATION &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(MainActivity.getInstance(), new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, MainActivity.LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new
     *                            one
     */
    boolean isBetterLocation(Location location) {
        if (mLatestLocation == null) {
            // A new hostage.location is always better than no hostage.location
            return true;
        }

        // Check whether the new hostage.location fix is newer or older
        long timeDelta = location.getTime() - mLatestLocation.getTime();
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
        int accuracyDelta = (int) (location.getAccuracy() - mLatestLocation
                .getAccuracy());

        // Determine hostage.location quality using a combination of timeliness and
        // accuracy
        return accuracyDelta < 0;
    }
}
