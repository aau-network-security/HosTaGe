package dk.aau.netsec.hostage.logging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class SyncInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The first Message that should be sent when two devices want to synchronize.
     * It should contain a Map of devices and either the latest attack ID the device has in the database
     * or the time of the latest attack.
     *
     *
     * | A -> SyncInfo -> B |
     * |                    | ----- SyncInfo independent
     * | A <- SyncInfo <- B |
     *
     *
     * Now A looks what it has that B does not have, by comparing the device ID's and the newest entry indicator,
     * and sends it to B. (SyncData object)
     * B does the same, now these two devices should be synchronized.
     */
    public HashMap<String, Long> deviceMap;
    public ArrayList<String> bssids;

    public SyncInfo(){
        this.deviceMap = new HashMap<String, Long>();
        this.bssids = new ArrayList<String>();
    }
}
