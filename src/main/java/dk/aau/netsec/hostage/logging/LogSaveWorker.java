package dk.aau.netsec.hostage.logging;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.commons.JSONHelper;
import dk.aau.netsec.hostage.logging.formatter.Formatter;
import dk.aau.netsec.hostage.logging.formatter.TraCINgFormatter;
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
        daoHelper = new DAOHelper(dbSession, filipsContext);

        Uri uri = Uri.parse(getInputData().getString(RecordOverviewFragment.WORKER_DATA_URI_KEY));
        int exportFormat = getInputData().getInt(RecordOverviewFragment.LOG_EXPORT_FORMAT, RecordOverviewFragment.EXPORT_FORMAT_POSITION_PLAINTEXT);

        if (exportFormat == RecordOverviewFragment.EXPORT_FORMAT_POSITION_JSON) {

            writeJSONFile(uri);

            return Result.success();
        } else if (exportFormat == RecordOverviewFragment.EXPORT_FORMAT_POSITION_PLAINTEXT) {

            writePlaintextFile(uri);

            return Result.success();
        }

        return Result.failure();
    }

    public static String getFileName(int exportFormat) {
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy_HHmm");
        Date date = new Date(System.currentTimeMillis());

        if (exportFormat == RecordOverviewFragment.EXPORT_FORMAT_POSITION_JSON) {
            return "hostage_" + formatter.format(date) + ".json";
        } else {
            return "hostage_" + formatter.format(date) + ".txt";
        }
    }

    private void writePlaintextFile(Uri outputFileUri) {
        Formatter formatter = TraCINgFormatter.getInstance();
        ArrayList<RecordAll> records = daoHelper.getAttackRecordDAO().getAllRecords();

        OutputStream outputStream;
        try {
            outputStream = filipsContext.getContentResolver().openOutputStream(outputFileUri);

            for (RecordAll record : records) {
                outputStream.write((record.toString(formatter)).getBytes());
            }

            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeJSONFile(@NonNull Uri outputFileUri) {
        JSONHelper jsonHelper = new JSONHelper();
        ArrayList<RecordAll> records = daoHelper.getAttackRecordDAO().getAllRecords();

        OutputStream outputStream;
        try {
            outputStream = filipsContext.getContentResolver().openOutputStream(outputFileUri);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream, "utf8"), 8192);

            JSONArray arr = new JSONArray();
            for (RecordAll record : records) {
                arr.put(record.toJSON());
            }

            bw.write(arr.toString());
            bw.flush();
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
