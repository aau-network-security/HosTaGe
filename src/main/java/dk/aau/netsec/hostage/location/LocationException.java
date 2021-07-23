package dk.aau.netsec.hostage.location;

import androidx.annotation.Nullable;

/**
 * Custom exception related to Location issues of the {@link CustomLocationManager}
 *
 * @author Filip Adamik
 * Created on 24/06/2021
 */
public class LocationException extends Exception {

    /**
     * Throw a new exception.
     *
     * @param message Exception message. Typically a lacking location permission or inactive
     *                location provider.
     */
    public LocationException(@Nullable String message) {
        super(message);
    }
}
