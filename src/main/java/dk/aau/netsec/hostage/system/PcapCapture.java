package dk.aau.netsec.hostage.system;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static dk.aau.netsec.hostage.system.iptablesUtils.Api.runCommandWithHandle;

import androidx.documentfile.provider.DocumentFile;

/**
 * TODO write javadoc
 */
public class PcapCapture extends AsyncTask<Void, Void, Void> {
    private Context context;
    private Uri outputFolder;
    private Process tcpdumpProcess;
    private int captureType;

    public static final int LOG_TYPE_TEXT = 180;
    public static final int LOG_TYPE_PCAP = 561;

    /**
     * TODO write javadoc
     *
     * @param context
     * @param outputFolder
     */
    public PcapCapture(Context context, Uri outputFolder) {
        this.context = context;
        this.outputFolder = outputFolder;
        this.captureType = LOG_TYPE_PCAP;
    }

    /**
     * TODO write javadoc
     *
     * @param context
     * @param outputFolder
     * @param captureType
     */
    public PcapCapture(Context context, Uri outputFolder, int captureType) {
        this.context = context;
        this.outputFolder = outputFolder;
        this.captureType = captureType;
    }

    /**
     * TODO write javadoc
     *
     * @param voids
     * @return
     */
    @Override
    protected Void doInBackground(Void... voids) {
        if (captureType == LOG_TYPE_PCAP){
            runTcpdumpPcap(context, outputFolder);
        } else {
            runTcpdumpLogs(context, outputFolder);
        }
        return null;
    }

    /**
     * TODO write javadoc
     *
     * @param context
     * @param outputFolder
     */
    private void runTcpdumpLogs(Context context, Uri outputFolder) {
        Log.d("filipko", "PCAP Writer Starting");

        String command = "su -c tcpdump -i any -s 65535";

        tcpdumpProcess = runCommandWithHandle(command);
        InputStream processOutput = tcpdumpProcess.getInputStream();
        FileOutputStream fileOutputStream = null;

        BufferedReader outputReader = new BufferedReader(new InputStreamReader(processOutput));
        char[] outputChar = new char[4096];
        int outputLength;
        StringBuffer outputBuffer = new StringBuffer();

        try {
            fileOutputStream = openFileForWriting(outputFolder);

            while ((outputLength = outputReader.read(outputChar)) > 0) {
                outputBuffer.append(outputChar, 0, outputLength);
                fileOutputStream.write(outputBuffer.toString().getBytes());

                Log.d("filipko", "written pcap logs of " + Integer.toString(outputLength) + " bytes.");
            }
        } catch (IOException fnfE) {
            fnfE.printStackTrace();
        } finally {
            closeStream(fileOutputStream);
        }
    }

    /**
     * TODO write javadoc
     *
     * @param context
     * @param outputFolder
     */
    private void runTcpdumpPcap(Context context, Uri outputFolder) {
        Log.d("filipko", "PCAP Writer Starting");

        String command = "su -c tcpdump -i any -s 65535 -U -w -";

        tcpdumpProcess = runCommandWithHandle(command);
        InputStream processOutput = tcpdumpProcess.getInputStream();
        FileOutputStream fileOutputStream = null;

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        int outputLength;
        byte[] outputBytes = new byte[4096];

        int totalOutputLength = 0;

        try {
            fileOutputStream = openFileForWriting(outputFolder);

            while ((outputLength = processOutput.read(outputBytes, 0, outputBytes.length)) != -1) {
                totalOutputLength += outputLength;

                outputBuffer.write(outputBytes, 0, outputLength);
                outputBuffer.writeTo(fileOutputStream);

                Log.d("filipko", "written pcap logs of " + Integer.toString(outputBytes.length) + " bytes.");

//                TODO fix log rotation
//                Log rotation, currently not working as expected due to log file being cut mid-packet (based strictly on size)
                if (totalOutputLength > 10000){
                    closeStream(fileOutputStream);
                    outputBuffer = new ByteArrayOutputStream();

                    fileOutputStream = openFileForWriting(outputFolder);
                    totalOutputLength = 0;
                    Log.d("filipko", "file size limit reached, opened a new file.");
                }

            }
        } catch (IOException fnfE) {
            fnfE.printStackTrace();
        } finally {
            closeStream(fileOutputStream);
        }
    }

    /**
     * TODO write javadoc
     *
     * @param outputFolder
     * @param outputType
     * @return
     * @throws FileNotFoundException
     */
    private FileOutputStream openFileForWriting(Uri outputFolder) throws FileNotFoundException {
        DocumentFile dirFile = DocumentFile.fromTreeUri(context, outputFolder);

        DocumentFile file;
        if (captureType == LOG_TYPE_PCAP) {
            file = dirFile.createFile("application/vnd.tcpdump.pcap", "pcapLog");
        } else {
            file = dirFile.createFile("text/plain", "pcapLog");
        }

        Uri uri = file.getUri();

        ParcelFileDescriptor pfd = context.getContentResolver().
                openFileDescriptor(uri, "w");

        return new FileOutputStream(pfd.getFileDescriptor());
    }

    /**
     * TODO write javadoc
     *
     * @param stream
     */
    private void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ioexc) {
                ioexc.printStackTrace();
            }
        }
    }

    /**
     * TODO write javadoc
     */
    public void stopTcpdumpPcap() {
        if (tcpdumpProcess != null) {
            tcpdumpProcess.destroy();
            Log.d("filipko", "PCAP writer stopped");
        }
    }
}
