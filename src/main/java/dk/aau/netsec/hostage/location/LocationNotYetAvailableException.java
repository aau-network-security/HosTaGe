package dk.aau.netsec.hostage.location;

import androidx.annotation.Nullable;

/**
 * Custom exception that is to be thrown when Location permission has been granted, but location
 * could not be obtained timely.
 *
 * This allows the {@link CustomLocationManager} distinguish a temporary unavailable location from
 * other, more general location issues, represented by parent {@link LocationException}.
 *
 * @author Filip Adamik
 * Created on 20/07/2021
 */
public class LocationNotYetAvailableException extends LocationException {

    /**
     * Throw a new exception.
     *
     * @param message Exception message.
     */
    public LocationNotYetAvailableException(@Nullable String message){
        super(message);
    }
}
