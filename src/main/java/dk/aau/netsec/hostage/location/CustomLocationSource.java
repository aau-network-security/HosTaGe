package dk.aau.netsec.hostage.location;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.LocationSource;

/**
 * Custom LocationSource provider for Google Map in the {@link dk.aau.netsec.hostage.ui.fragment.ThreatMapFragment}.
 * This is necessary to allow the use of {@link CustomLocationManager}.
 *
 * The implemented methods of {@link LocationSource} are mapped to respective methods of CustomLocationManager.
 *
 * @author Filip Adamik
 * Created on 28/06/2021
 */
public class CustomLocationSource implements LocationSource {

    private CustomLocationManager mLocationManager;
    private OnLocationChangedListener mListener;

    /**
     * Save the reference to the existing {@link CustomLocationManager} for later use.
     *
     * @param locationManager
     */
    public CustomLocationSource(@NonNull CustomLocationManager locationManager) {
        mLocationManager = locationManager;
    }

    @Override
    public void activate(@NonNull LocationSource.OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;

        try {
            mLocationManager.registerCustomLocationListener(onLocationChangedListener);
        } catch (LocationException le) {
            le.printStackTrace();
        }
    }

    @Override
    public void deactivate() {
        mLocationManager.unregisterCustomLocationListener(mListener);
    }
}
