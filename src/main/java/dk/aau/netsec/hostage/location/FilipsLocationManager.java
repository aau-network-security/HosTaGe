package dk.aau.netsec.hostage.location;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

import dk.aau.netsec.hostage.ui.activity.MainActivity;

/**
 * @author Filip Adamik
 * Created on 24/06/2021
 */
public class FilipsLocationManager {

    Location mLatestLocation;
    private Context mContext;
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private Consumer<Location> mLocationConsumer;
    private boolean locationPermissionDenied;

    private static WeakReference<FilipsLocationManager> mLocationManagerInstance;

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    public FilipsLocationManager(Context context) {
        mContext = context;
        mLocationManagerInstance = new WeakReference<>(this);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

                if (isBetterLocation(location)) {
                    mLatestLocation = location;
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

    public static FilipsLocationManager getLocationManagerInstance() {
        return mLocationManagerInstance.get();
    }

    public void userHasDeniedLocation() {
        locationPermissionDenied = true;

    }

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

    public void startUpdatingLocation() throws LocationException {
        String preferedProvider;

        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            preferedProvider = LocationManager.GPS_PROVIDER;
        } else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            preferedProvider = LocationManager.NETWORK_PROVIDER;
        } else {
            throw new LocationException("No network provider enabled");
        }


        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            throw new LocationException("Location permission has not been granted");
        }

        mLocationManager.requestLocationUpdates(preferedProvider, 0, 0, mLocationListener);
    }

    public void stopUpdatingLocation() {
        mLocationManager.removeUpdates(mLocationListener);
    }

    public void permissionGrantedCallback() {

        // getCurrentLocation is only supported in API 30 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            TODO address older versions of Android
            mLocationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, mContext.getMainExecutor(), mLocationConsumer);
        } else {
            try {
                startUpdatingLocation();
            } catch (LocationException le) {
//                TODO handle (should not ever happen)
                ;
            }
        }
    }

    public void requestBackgroundLocation() {

        if (ActivityCompat.shouldShowRequestPermissionRationale((MainActivity) mContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
            dialog.setTitle("This app uses background location");
            dialog.setMessage("Background location is needed to enable attack data collection while the app is in the background.");
            dialog.setCancelable(true);
            dialog.setNeutralButton("Understood", ((dialog1, which) -> {
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

//            ActivityCompat.requestPermissions(MainActivity.getInstance(), new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, MainActivity.LOCATION_PERMISSION_REQUEST_CODE);
//        }

    }

//    private void requestBackgroundLocationPermission(){
//
//    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        // If needed, show explanation
        if (ActivityCompat.shouldShowRequestPermissionRationale((MainActivity) mContext, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
            dialog.setTitle("This app uses location");
            dialog.setMessage("Location is needed to enable accurate attack data collection.");
            dialog.setCancelable(true);
            dialog.setNeutralButton("Understood", ((dialog1, which) -> {
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

    void showLocationRequestDialog(String locationPermissionType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            TODO address older versions of Android

            if (locationPermissionType == Manifest.permission.ACCESS_FINE_LOCATION || locationPermissionType == Manifest.permission.ACCESS_COARSE_LOCATION) {
                ActivityCompat.requestPermissions(MainActivity.getInstance(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainActivity.LOCATION_PERMISSION_REQUEST_CODE);
            } else if (locationPermissionType == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
                ActivityCompat.requestPermissions(MainActivity.getInstance(), new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, MainActivity.LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * Determines whether one Location reading is better than the current
     * Location fix
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
