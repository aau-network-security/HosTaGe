package dk.aau.netsec.hostage.event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import dk.aau.netsec.hostage.commons.JSONHelper;
import dk.aau.netsec.hostage.ui.activity.MainActivity;

public class BeaconEvent implements Event {
    private static final String PERSIST_FILENAME = "beaconInfo.json";
    File file = new File("/data/data/" + MainActivity.getContext().getPackageName() + "/" + PERSIST_FILENAME);

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("deviceid",deviceId);
            jsonObject.put("open","working");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }
    private JSONArray persistData() {
        JSONArray array = new JSONArray();
        array.put(this.toJSON());

        return array;
    }
    @Override
    public void writeData() {
        JSONHelper jsonHelper = new JSONHelper();
        jsonHelper.jsonWriter(persistData(),file);
    }
}
