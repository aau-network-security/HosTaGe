package de.tudarmstadt.informatik.hostage.protocol.cifs;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;

import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.core.DeviceContext;
import org.alfresco.jlan.server.filesys.FileExistsException;
import org.alfresco.jlan.server.filesys.FileName;
import org.alfresco.jlan.server.filesys.FileOpenParams;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.server.filesys.TreeConnection;
import org.alfresco.jlan.smb.server.disk.JavaFileDiskDriver;
import org.alfresco.jlan.smb.server.disk.JavaNetworkFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.protocol.SMB;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.fragment.FileAlertDialogFragment;
import virustotalapi.ReportScan;
import virustotalapi.VirusTotal;

/**
 * HostageV3
 * ================
 * @author Alexander Brakowski
 * @author Daniel Lazar
 * @author Shreyas Srinivasa
 *
 * This is a pseudo file disk driver, which overwrites the libs JavaFileDiskDriver,
 * so that we can get more information about the attack
 */
public class PseudoJavaFileDiskDriver extends JavaFileDiskDriver {

    private static class PseudoJavaNetworkFile extends JavaNetworkFile {
        protected final SMB SMB;
        private final SrvSession sess;
        boolean wasWrittenTo = false;
        private  final FileInject fileInject;

        public PseudoJavaNetworkFile(File file, String netPath, SMB SMB, SrvSession sess, FileInject fileInject) {
            super(file, netPath);
            this.SMB = SMB;
            this.sess = sess;
            this.fileInject = fileInject;
        }


        /**
         * method that checks if the file was just written, then gets the MD5 checksum of the
         * file and logs it. Afterwards the file gets deleted.
         * @throws java.io.IOException
         */
        public void closeFile() throws java.io.IOException {
            super.closeFile();
            if(wasWrittenTo){
                HelperUtils.setIsFileInjected(true);
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA256");
                    FileInputStream fis = new FileInputStream(m_file);

                    byte[] buffer = new byte[8192];
                    int numOfBytesRead;
                    while( (numOfBytesRead = fis.read(buffer)) > 0){
                        digest.update(buffer, 0, numOfBytesRead);
                    }

                    byte[] hash = digest.digest();
                    String checksum = new BigInteger(1, hash).toString(16);

                    StringBuilder sb = new StringBuilder();

                    //Creates use of Virustotal api

                    VirusTotal VT = new VirusTotal("111c226204f5de7228563bbca91c5860e4965fbe936307dffa8f2f2d575ff292"); // Virus Total API Key

                    Set<ReportScan> Report = VT.ReportScan(checksum); //The SHA256 file

                    for (ReportScan report : Report) {

                        if (report.getDetected().contentEquals("true")){

                            if (report.getVendor().contentEquals("McAfee")||report.getVendor().contentEquals("Microsoft")||report.getVendor().contentEquals("AVG")||report.getVendor().contentEquals("Symantec")||report.getVendor().contentEquals("CAT-QuickHeal")||report.getVendor().contentEquals("TrendMicro")||report.getVendor().contentEquals("Kaspersky"))

                            {
                                sb.append("\n\nVendor: " + report.getVendor() + " \nDetected: " + report.getDetected() + " \nMalware Name: " + report.getMalwarename());
                            }
                        }

                    }
                    //Setting the display component with the results obtained from Virustotal
                    String message = "File received: " + m_file.getName() + "\n\nCHECKSUM:\n" + checksum+"\n Scroll Down for Malware Details"+sb.toString();
                    fileInject.log(MessageRecord.TYPE.RECEIVE, message, 445, sess.getRemoteAddress(), 445);

                    HelperUtils.setFileName(m_file.getName());
                    HelperUtils.setFilePath(m_file.getPath());
                    HelperUtils.setFileSHA256(checksum);

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }


                wasWrittenTo = true;    // Saving file in phones memory
            }
        }

        public void writeFile(byte[] buf, int len, int pos)
                throws java.io.IOException {
            super.writeFile(buf, len, pos);
            wasWrittenTo = true;
        }

        public void writeFile(byte[] buf, int len, int pos, long offset)
                throws java.io.IOException {
            super.writeFile(buf, len, pos, offset);
            wasWrittenTo = true;
        }
    }

    private final SMB SMB;
    private final FileInject fileInject;

    public PseudoJavaFileDiskDriver(SMB SMB, FileInject fileInject) {
        this.SMB = SMB;
        this.fileInject = fileInject;
    }

    public NetworkFile createFile(SrvSession sess, TreeConnection tree, FileOpenParams params)
            throws java.io.IOException {
        DeviceContext ctx = tree.getContext();
        String fname = FileName.buildPath(ctx.getDeviceName(), params.getPath(), null, java.io.File.separatorChar);

        //  Check if the file already exists

        File file = new File(fname);

        String path = file.getAbsolutePath();
        if (file.exists())
            throw new FileExistsException();

        //  Create the new file

        FileWriter newFile = new FileWriter(fname, false);


        newFile.close();

        //  Create a Java network file
        file = new File(fname);
        PseudoJavaNetworkFile netFile = new PseudoJavaNetworkFile(file, params.getPath(), SMB, sess, fileInject);
        netFile.setGrantedAccess(NetworkFile.READWRITE);
        netFile.setFullName(params.getPath());

        //  Return the network file
        return netFile;

    }

    private static void displayAlert() {

        Context context = null;
        final Activity activity = MainActivity.getInstance();

        final FragmentManager fragmentManager = activity.getFragmentManager();
        if (fragmentManager != null) {
            FileAlertDialogFragment fileAlertDialogFragment = new FileAlertDialogFragment();
            fileAlertDialogFragment.show(fragmentManager.beginTransaction(), fileAlertDialogFragment.getTag());
        }

    }

}