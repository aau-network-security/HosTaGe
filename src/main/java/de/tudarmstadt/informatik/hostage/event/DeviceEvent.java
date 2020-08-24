package de.tudarmstadt.informatik.hostage.event;

import android.content.Context;

import com.jaredrummler.android.device.DeviceName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import de.tudarmstadt.informatik.hostage.commons.JSONHelper;
import de.tudarmstadt.informatik.hostage.system.Device;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

public class DeviceEvent implements Event{
    Context context;
    String manufacturer;
    String model;
    String deviceName;
    private static final String PERSIST_FILENAME = "deviceInfo.json";
    File file = new File("/data/data/" + MainActivity.getContext().getPackageName() + "/" + PERSIST_FILENAME);

    public DeviceEvent(Context context){
        this.context = context;
    }

    public void getDeviceInfo() {
        DeviceName.with(context).request((info, error) -> {
            manufacturer = info.manufacturer;  // "Samsung"
            model = info.model;                // "SM-G955W"
            deviceName = info.getName();       // "Galaxy S8+"
        });
    }


    private JSONArray persistData() {
        JSONArray array = new JSONArray();
        array.put(this.toJSON());

        return array;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("deviceid",deviceId);
            jsonObject.put("manufacturer",manufacturer);
            jsonObject.put("model",model);
            jsonObject.put("deciveName",deviceName);
            jsonObject.put("rooted",Device.isRooted());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public void writeData() {
        JSONHelper jsonHelper = new JSONHelper();
        jsonHelper.jsonWriter(persistData(),file);
    }
}
