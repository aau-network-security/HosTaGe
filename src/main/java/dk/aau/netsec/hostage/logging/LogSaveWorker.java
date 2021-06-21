package dk.aau.netsec.hostage.logging;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.commons.JSONHelper;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.ui.fragment.RecordOverviewFragment;

/**
 * @author Filip Adamik
 * Created on 21/06/2021
 */
public class LogSaveWorker extends Worker {
    Context filipsContext;
    DaoSession dbSession;
    DAOHelper daoHelper;

    public LogSaveWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        filipsContext = context;

    }

    @Override
    public Result doWork() {

        dbSession = HostageApplication.getInstances().getDaoSession();
        daoHelper = new DAOHelper(dbSession,filipsContext);

        //        String filipsDataToWrite = getInputData().getString("filipsKey");
        Uri uri = Uri.parse(getInputData().getString("filipsHorribleUri"));
        int exportFormat = getInputData().getInt(RecordOverviewFragment.LOG_EXPORT_FORMAT, RecordOverviewFragment.POSITION_EXPORT_FORMAT_PLAINTEXT);



        writeJSONFile(uri);
        return Result.success();
    }

//    private void exportJSONFormat(){

//        try {
//
//
//
//
//            makeToast(filename+" saved on external (if you have an sd card) or internal memory! ", Toast.LENGTH_LONG);
//        }catch (Exception e){
//            makeToast("Could not write to a JSON File in SD Card or Internal Storage",Toast.LENGTH_SHORT);
//        }

    private void writeJSONFile(@NonNull Uri uri) {
        JSONHelper jsonHelper = new JSONHelper();
        ArrayList<RecordAll> records = daoHelper.getAttackRecordDAO().getAllRecords();


        OutputStream outputStream;
        try {
            outputStream = filipsContext.getContentResolver().openOutputStream(uri);

//            jsonHelper.jsonWriter(records, outputStream);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream, "utf8"),8192);
//

            JSONArray arr = new JSONArray();
            for(RecordAll record: records) {
                arr.put(record.toJSON());
            }
            bw.write(arr.toString());
//
//            bw.write();
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
