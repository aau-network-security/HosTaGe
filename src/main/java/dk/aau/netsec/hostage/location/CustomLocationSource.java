package dk.aau.netsec.hostage.location;

import android.location.Location;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.LocationSource;

/**
 * @author Filip Adamik
 * Created on 28/06/2021
 */
public class CustomLocationSource implements LocationSource {

    private FilipsLocationManager mLocationManager;
    private OnLocationChangedListener mListener;

    public CustomLocationSource(FilipsLocationManager locationManager){
            mLocationManager = locationManager;
    }

    @Override
    public void activate(@NonNull LocationSource.OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        mLocationManager.registerCustomLocationListener(onLocationChangedListener);
    }

    @Override
    public void deactivate() {
        mLocationManager.unregisterCustomLocationListener(mListener);
    }


}
