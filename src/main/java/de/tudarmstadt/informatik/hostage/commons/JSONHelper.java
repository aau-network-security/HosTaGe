package de.tudarmstadt.informatik.hostage.commons;

import android.content.Context;

import org.json.JSONArray;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import de.tudarmstadt.informatik.hostage.logging.RecordAll;
import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

public class JSONHelper {

    private final static String PERSIST_FILENAME = "upload.json";

    public void persistData(RecordAll record){
        try {
            int BUFFER_SIZE = 8192;
            String UTF8 = "utf8";

            FileOutputStream fout = MainActivity.getContext().openFileOutput(PERSIST_FILENAME, Context.MODE_PRIVATE);
            BufferedWriter fnw = new BufferedWriter(new OutputStreamWriter(fout, UTF8), BUFFER_SIZE);

            JSONArray arr = new JSONArray();
            arr.put(record.toJSON());

            fnw.write(arr.toString());

            fnw.close();
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
