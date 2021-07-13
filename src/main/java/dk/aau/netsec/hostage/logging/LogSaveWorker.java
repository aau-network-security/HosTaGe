package dk.aau.netsec.hostage.logging;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
 * The LogSaveWorker is used to save all logs to a file. The user can adjust file name and
 * location before the file is created. The logs are exported either as JSON or a plaintext
 * file, depending on what the user selected in {@link RecordOverviewFragment}.
 * <p>
 * The file is written using a background worker.
 *
 * @author Filip Adamik
 * Created on 21/06/2021
 */
public class LogSaveWorker extends Worker {
    private Context mContext;

    /**
     * Create the LogSaveWorker and save context for later use.
     */
    public LogSaveWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    /**
     * Main method to launch a background worker task. Run from {@link RecordOverviewFragment}.
     * <p>
     * Depending on the user choice, export file either as plaintext or JSON.
     *
     * @return worker task result status.
     */
    @Override
    public Result doWork() {
        // Get record data
        DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
        DAOHelper mDaoHelper = new DAOHelper(dbSession, mContext);
        ArrayList<RecordAll> records = mDaoHelper.getAttackRecordDAO().getAllRecords();

        // Get output file and format
        Uri uri = Uri.parse(getInputData().getString(RecordOverviewFragment.WORKER_DATA_URI_KEY));
        int exportFormat = getInputData().getInt(RecordOverviewFragment.LOG_EXPORT_FORMAT, RecordOverviewFragment.EXPORT_FORMAT_POSITION_PLAINTEXT);

        try {
            // Export as plaintext
            if (exportFormat == RecordOverviewFragment.EXPORT_FORMAT_POSITION_PLAINTEXT) {
                writePlaintextFile(uri, records);
                return Result.success();
            }
            // Export as JSON
            else if (exportFormat == RecordOverviewFragment.EXPORT_FORMAT_POSITION_JSON) {
                writeJSONFile(uri, records);
                return Result.success();

            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return Result.failure();
    }

    /**
     * Determine the file name and extension from current time and desired export format
     *
     * @param exportFormat Export format indicator (JSON or plaintext)
     * @return Filename in format <i>hostage_ddMMyyy_HHmm.extension</i>, for example
     * <i>hostage_22062021_2351.json</i>
     */
    public static String getFileName(int exportFormat) {
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy_HHmm");
        Date date = new Date(System.currentTimeMillis());

        if (exportFormat == RecordOverviewFragment.EXPORT_FORMAT_POSITION_JSON) {
            return "hostage_" + formatter.format(date) + ".json";
        } else {
            return "hostage_" + formatter.format(date) + ".txt";
        }
    }

    /**
     * Write file as plaintext the newly created file location.
     *
     * @param outputFileUri Reference to output file picked by the user.
     * @param records       Logs to be written to the file.
     * @throws IOException File operation exception.
     */
    private void writePlaintextFile(Uri outputFileUri, ArrayList<RecordAll> records) throws IOException {
        Formatter formatter = TraCINgFormatter.getInstance();

        OutputStream outputStream;
        outputStream = mContext.getContentResolver().openOutputStream(outputFileUri);

        for (RecordAll record : records) {
            outputStream.write((record.toString(formatter)).getBytes());
        }

        outputStream.flush();
        outputStream.close();

    }

    /**
     * Write file as plaintext the newly created file location.
     *
     * @param outputFileUri Reference to output file picked by the user.
     * @param records       Logs to be written to the file.
     * @throws IOException File operation exception.
     */
    private void writeJSONFile(@NonNull Uri outputFileUri, ArrayList<RecordAll> records) throws IOException {
        JSONHelper jsonHelper = new JSONHelper();

        OutputStream outputStream;
        outputStream = mContext.getContentResolver().openOutputStream(outputFileUri);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream, "utf8"), 8192);

        JSONArray arr = new JSONArray();
        for (RecordAll record : records) {
            arr.put(record.toJSON());
        }

        bw.write(arr.toString());
        bw.flush();
        bw.close();
    }

}
