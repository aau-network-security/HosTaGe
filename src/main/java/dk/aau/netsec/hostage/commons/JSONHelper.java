package dk.aau.netsec.hostage.commons;


import org.json.JSONArray;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import dk.aau.netsec.hostage.logging.RecordAll;
import dk.aau.netsec.hostage.ui.activity.MainActivity;


public class JSONHelper {

    public void jsonWriter(JSONArray arr,File file){
        try {
            int BUFFER_SIZE = 8192;
            String UTF8 = "utf8";
            FileOutputStream fout = new FileOutputStream(file);
            BufferedWriter fnw = new BufferedWriter(new OutputStreamWriter(fout, UTF8), BUFFER_SIZE);

            fnw.write(arr.toString());
            fnw.close();
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void jsonWriter(ArrayList<RecordAll> records,File filepath){
        try {
            int BUFFER_SIZE = 8192;
            String UTF8 = "utf8";
            FileOutputStream fout = new FileOutputStream(filepath);
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

    public String getFilePath(File file){
        return file.getAbsolutePath();
    }

}
