package dk.aau.netsec.hostage.event;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public interface Event {
    String deviceId = UUID.randomUUID().toString();

    JSONObject toJSON();
    void writeData();
}
