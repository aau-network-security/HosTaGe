package dk.aau.netsec.hostage.location;

/**
 * @author Filip Adamik
 * Created on 24/06/2021
 */
public class LocationException extends Exception {

    public LocationException(String message) {
//        super("The Location Permission was not granted")
        super(message);
    }

}
