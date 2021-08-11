package dk.aau.netsec.hostage.system;

import static dk.aau.netsec.hostage.system.iptablesUtils.Api.runCommandWithHandle;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/***
 * This service handles the task of running tcpdump and writing out it's output to a file in the
 * folder specified by the user.
 *
 * The logging can operate in two modes. <i>Text mode</i> outputs tcpdump logs in a text format onto a
 * text file in the user-specified folder.
 *
 * <i>PCAP mode</i> saves the output as .cap files, which can be opened by Wireshark or the like.
 * When operating in this mode, tcpdump outputs the logs periodically into an app-private files
 * folder. Another thread then copies these files into the user-specified folder. This setup is
 * necessary, since tcpdump cannot reliably write files direcly to the user-specified folder on
 * all API verions (due to restrictions in app's access to user files on API >= 28).
 *
 * @author Filip Adamik
 * Created on 10-08-2021
 */

public class PcapLoggingService extends Service {

    private Uri outputFolder;
    private int captureType;
    private int logRotationSeconds;

    private Context context;
    private File filesDir;

    private Process tcpdumpProcess;
    private Thread tcpdumpThread;
    private Thread fileCopyThread = null;

    public static final int LOG_TYPE_TEXT = 180;
    public static final int LOG_TYPE_PCAP = 561;

    public static final String PCAP_SERVICE_INTENT_TYPE = "pcap_intent_type";
    public static final String PCAP_SERVICE_INTENT_URI = "pcap_intent_uri";
    public static final String PCAP_SERVICE_INTENT_SECONDS = "pcap_intent_seconds";

    public static final String TAG = "PCAP Logging Service";

    /**
     * Launches the service and starts the logging.
     *
     * @param intent Starting intent. It has to carry the Uri of the desired output folder and should
     *               include the desired logging type (text/Pcap) and log rotation period.
     * @return Specifies that service should be restarted, if killed due to low memory.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        captureType = intent.getIntExtra(PCAP_SERVICE_INTENT_TYPE, LOG_TYPE_PCAP);
        outputFolder = Uri.parse(intent.getStringExtra(PCAP_SERVICE_INTENT_URI));
        logRotationSeconds = intent.getIntExtra(PCAP_SERVICE_INTENT_SECONDS, 10);
        context = this;
        filesDir = getFilesDir();

        // Determine thread based on logging type.
        if (captureType == LOG_TYPE_TEXT) {
            tcpdumpThread = new TextLogThread();
        } else {
            tcpdumpThread = new PcapLogThread();
            fileCopyThread = new FileCopyThread();
        }

        // Start logging thread and fileCopy thread if applicable.
        tcpdumpThread.start();
        if (fileCopyThread != null) {
            fileCopyThread.start();
        }

        return START_STICKY;
    }

    /**
     * This service does not allow binding and therefore this method returns null.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called when service is stopped due to logging disabled by user or low memory.
     * <p>
     * tcpdump was launched with su privileges and therefore must be killed with su (calling
     * tcpdumpThread.destroy() would not kill the process).
     */
    @Override
    public void onDestroy() {
        //TODO maybe keep tdpdump alive if killed due to low memory?
        String command = "su -c pkill -SIGINT tcpdump";
        runCommandWithHandle(command);

        if (fileCopyThread != null) {
            fileCopyThread.interrupt();
        }

        Log.d(TAG, "PCAP writer stopped");

        // Copy the last file, which was saved after tcpdump was interrupted.
        copyFilesToUserStorage(true);

        super.onDestroy();
    }

    /**
     * Copy files from app files dir to the output folder specified by the user. The pcapLogs subfolder
     * in app files dir is repeatedly iterated through. In each iteration, the oldest file is copied
     * and then deleted.
     * <p>
     * Unless copyLastFile is set to true, the newest file in the folder left intact (as tcpdump is
     * writing into this file).
     *
     * @param copyLastFile Set to true when tcpdump has been stopped and the last file should also
     *                     be copied.
     */
    private void copyFilesToUserStorage(boolean copyLastFile) {
        int totalFiles;
        String oldestFileName;
        Date oldestFileDate;
        FileInputStream fromFileStream = null;
        FileOutputStream toFileStream = null;

        File pcapLogsDir = new File(Paths.get(filesDir.getAbsolutePath(), "pcapLogs").toString());

        // Find oldest file and copy it to user storage, until there is only one file left.
        do {
            totalFiles = 0;
            oldestFileName = null;
            oldestFileDate = null;

            //Loop through dir
            for (String filename : pcapLogsDir.list()) {

                //Identify the oldest file with the correct filename structure (date at the end)
                try {
                    DateFormat format = new SimpleDateFormat("yyMMdd-HHmmss");
                    Date parsed = format.parse(filename.substring(filename.length() - 13));

                    totalFiles += 1;

                    if (oldestFileDate == null || parsed.before(oldestFileDate)) {
                        oldestFileName = filename;
                        oldestFileDate = (Date) parsed.clone();
                    }
                } catch (ParseException pe) {
                    pe.printStackTrace();
                    continue;
                }
            }

            // Copy the oldest file to user storage
            if (totalFiles > 1 || (totalFiles == 1 && copyLastFile)) {
                try {
                    File sourceLogFile = new File(Paths.get(pcapLogsDir.getAbsolutePath(), oldestFileName).toString());

                    fromFileStream = new FileInputStream(sourceLogFile);
                    toFileStream = openFileForWriting(oldestFileName);

                    copyFiles(fromFileStream, toFileStream);

                    sourceLogFile.delete();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } finally {
                    closeStream(fromFileStream);
                    closeStream(toFileStream);
                }
            }
        } while (totalFiles > 1 || (totalFiles == 1 && copyLastFile));
    }

    /**
     * Create a new file in the output folder specified by the user. Depending on the capture type,
     * either a text file or .cap file is created.
     *
     * @param fileName Filename of the newly created file.
     * @return Writeable FileOutputStream
     * @throws FileNotFoundException This exception should never be thown.
     */
    private FileOutputStream openFileForWriting(String fileName) throws FileNotFoundException {
        DocumentFile dirFile = DocumentFile.fromTreeUri(context, outputFolder);

        DocumentFile file;
        if (captureType == LOG_TYPE_PCAP) {
            file = dirFile.createFile("application/vnd.tcpdump.pcap", fileName);
        } else {
            file = dirFile.createFile("text/plain", "pcapLog");
        }

        Uri uri = file.getUri();

        ParcelFileDescriptor pfd = context.getContentResolver().
                openFileDescriptor(uri, "w");

        return new FileOutputStream(pfd.getFileDescriptor());
    }

    /**
     * Close stream safely (check if exists and catch IOException).
     *
     * @param stream Stream to be closed.
     */
    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ioexc) {
                ioexc.printStackTrace();
            }
        }
    }

    /**
     * Copy file contents from one file to another file.
     *
     * @param fromFileStream Filestream of the source file.
     * @param toFileStream   Filestream of the destination file.
     * @throws IOException
     */
    private static void copyFiles(FileInputStream fromFileStream, FileOutputStream toFileStream) throws IOException {
        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = fromFileStream.getChannel();
            destination = toFileStream.getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * Start logging in PCAP mode.
     * <p>
     * App files dir and user-specified log rotation is given as options to tcpdump. The files
     * produced in the app files dir are copied to user-specified folder by {@link FileCopyThread}.
     */
    private class PcapLogThread extends Thread {
        @Override
        public void run() {
            Log.d(TAG, "PCAP Writer Starting in CAP mode.");

            File cacheDirPath = new File(Paths.get(filesDir.getAbsolutePath(), "pcapLogs").toString());

            if (!cacheDirPath.exists()) {
                cacheDirPath.mkdir();
            }

            Path outpath = Paths.get(cacheDirPath.getAbsolutePath(), "pcapLog-%y%m%d-%H%M%S");

            String command = "su -c tcpdump -i any -s 65535 -G " + Integer.toString(logRotationSeconds)
                    + " -w " + outpath.toString();
            tcpdumpProcess = runCommandWithHandle(command);
        }
    }

    /**
     * Start logging in text mode.
     * <p>
     * The text output of tcpdump is grabbed from process' stdout and written directly to file in
     * the output folder specified by the user.
     */
    private class TextLogThread extends Thread {
        @Override
        public void run() {
            Log.d(TAG, "PCAP Writer Starting in Text Mode");

            //Run tcpdump
            String command = "su -c tcpdump -i any -s 65535";
            tcpdumpProcess = runCommandWithHandle(command);

            //Prepare process stdout and file output.
            InputStream processOutput = tcpdumpProcess.getInputStream();
            FileOutputStream fileOutputStream = null;

            //Prepare writing buffer and helpers
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(processOutput));
            char[] outputChar = new char[4096];
            int outputLength;
            StringBuffer outputBuffer = new StringBuffer();

            try {
                fileOutputStream = openFileForWriting("pcapLog");

                //Write to file while there is more process output in stdout
                while ((outputLength = outputReader.read(outputChar)) > 0) {
                    outputBuffer.append(outputChar, 0, outputLength);
                    fileOutputStream.write(outputBuffer.toString().getBytes());

                    Log.d(TAG, "Written PCAP logs of " + Integer.toString(outputLength) + " bytes.");
                }
            } catch (IOException fnfE) {
                fnfE.printStackTrace();
            } finally {
                //Close file output
                closeStream(fileOutputStream);
            }
        }
    }

    /**
     * Launch a file copy thread. This thread will periodically scan the app file directory for
     * new files to copy.
     */
    private class FileCopyThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep(logRotationSeconds * 500L);
                    copyFilesToUserStorage(false);
                }
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }
}
