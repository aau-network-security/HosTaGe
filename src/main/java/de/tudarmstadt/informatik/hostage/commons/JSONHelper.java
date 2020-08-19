package de.tudarmstadt.informatik.hostage.commons;


import org.json.JSONArray;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import de.tudarmstadt.informatik.hostage.logging.RecordAll;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;


public class JSONHelper {
    private static final String PERSIST_FILENAME = "publish.json";
    File file = new File("/data/data/" + MainActivity.getContext().getPackageName() + "/" + PERSIST_FILENAME);


    public void persistData(ArrayList<RecordAll> records){
        try {
            int BUFFER_SIZE = 8192;
            String UTF8 = "utf8";
            FileOutputStream fout = new FileOutputStream(file);
            BufferedWriter fnw = new BufferedWriter(new OutputStreamWriter(fout, UTF8), BUFFER_SIZE);

            JSONArray arr = new JSONArray();
            for(RecordAll record: records) {
                arr.put(record.toJSON());
            }
            fnw.write(arr.toString());
            fnw.close();
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getFilePath(){
        return file.getAbsolutePath();
    }
}
