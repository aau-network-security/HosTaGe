package de.tudarmstadt.informatik.hostage.protocol.cifs;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import org.alfresco.jlan.app.JLANCifsServer;
import org.alfresco.jlan.app.XMLServerConfiguration;
import org.alfresco.jlan.netbios.NetworkSettings;
import org.alfresco.jlan.server.NetworkServer;
import org.alfresco.jlan.server.ServerListener;
import org.alfresco.jlan.server.SessionListener;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.filesys.DiskDeviceContext;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.DiskSharedDevice;
import org.alfresco.jlan.server.filesys.FileListener;
import org.alfresco.jlan.server.filesys.FilesystemsConfigSection;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.server.filesys.NetworkFileServer;
import org.alfresco.jlan.server.filesys.SrvDiskInfo;
import org.alfresco.jlan.smb.server.CIFSConfigSection;
import org.springframework.extensions.config.element.GenericConfigElement;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.tudarmstadt.informatik.hostage.Handler;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.protocol.SMB;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

/**
 * HostageV3
 * ================
 * @author Alexander Brakowski
 * @author Daniel Lazar
 * on 19.03.15.
 */
public class CifsServer extends JLANCifsServer {

    private final XMLServerConfiguration serverConfiguration;
    protected final SMB SMB;
    protected final FileInject fileInject;

    public CifsServer(XMLServerConfiguration serverConfiguration, SMB SMB, FileInject fileInject){
        super();

        this.SMB = SMB;
        this.serverConfiguration = serverConfiguration;
        this.fileInject = fileInject;
    }

    public void run() throws Exception {
        CIFSConfigSection cifsConfigSection = (CIFSConfigSection) serverConfiguration.getConfigSection(CIFSConfigSection.SectionName);
        cifsConfigSection.setBroadcastMask(getBroadcastAddress());
        //enables the host announcer, so that it should appear in network neighborhood
        cifsConfigSection.setHostAnnouncer(true);
        cifsConfigSection.setDisableNIOCode(true);

        int ip = SMB.getLocalIp();

        //binding to one address on both SMB and NB
        cifsConfigSection.setSMBBindAddress(intToInetAddress(ip));
        cifsConfigSection.setNetBIOSBindAddress(intToInetAddress(ip));
        //setting the broadcast mask for the network
        NetworkSettings.setBroadcastMask(getBroadcastAddress());


        if(cifsConfigSection.hasNetBIOSSMB()){
            //adding a Netbios Server for the nameservice
            serverConfiguration.addServer(createNetBIOSServer(serverConfiguration));
        }

        //adding a SMB server
        serverConfiguration.addServer(createSMBServer(serverConfiguration));

        FilesystemsConfigSection filesysConfig = (FilesystemsConfigSection) serverConfiguration.getConfigSection(FilesystemsConfigSection.SectionName);

        //creating a file which references to the apps cache folder
        File cacheFolder = MainActivity.context.getExternalCacheDir();
        File jlanFolder = new File(cacheFolder.getAbsolutePath() + "/jlan");
        deleteRecursive(jlanFolder);

        //creating folders which appear if an attacker opens a session
        File anotherFolder = new File(jlanFolder.getAbsolutePath() + "/Windows");
        File progData = new File(jlanFolder.getAbsolutePath() + "/ProgramData");
        File users = new File(jlanFolder.getAbsolutePath() + "/Users");
        File temp= new File(jlanFolder.getAbsolutePath() + "/temp");
        File addins = new File(jlanFolder.getAbsolutePath()+"/addins"); // this folder created as an environment setup for zero days for STUXNET propagation

        jlanFolder.mkdir();
        anotherFolder.mkdir();
        progData.mkdir();
        users.mkdir();
        temp.mkdir();

        // SHARES
        DiskInterface diskInterface = new PseudoJavaFileDiskDriver(SMB, fileInject);
        final GenericConfigElement driverConfig = new GenericConfigElement("driver");
        final GenericConfigElement localPathConfig = new GenericConfigElement("LocalPath");
        localPathConfig.setValue(jlanFolder.getAbsolutePath());
        driverConfig.addChild(localPathConfig);
        DiskDeviceContext diskDeviceContext = (DiskDeviceContext) diskInterface.createContext("SHARE", driverConfig);
        diskDeviceContext.setShareName("SHARE");
        diskDeviceContext.setConfigurationParameters(driverConfig);
        diskDeviceContext.enableChangeHandler(false);
        diskDeviceContext.setDiskInformation(new SrvDiskInfo(2560000, 64, 512, 2304000));// Default to a 80Gb sized disk with 90% free space
        DiskSharedDevice diskDev = new DiskSharedDevice("SHARE", diskInterface, diskDeviceContext);
        diskDev.setConfiguration(serverConfiguration);
        diskDeviceContext.startFilesystem(diskDev);
        filesysConfig.addShare(diskDev);

        for(int i=0; i<serverConfiguration.numberOfServers(); i++){
            NetworkServer server = serverConfiguration.getServer(i);

            if(server instanceof NetworkFileServer){
                NetworkFileServer fileServer = (NetworkFileServer) server;
                fileServer.addFileListener(new FileListener() {
                    @Override
                    public void fileClosed(SrvSession sess, NetworkFile file) {
                    }




                    @Override
                    public void fileOpened(SrvSession sess, NetworkFile file) {

                        file.getName();
                        System.out.print(file.getName());

                        file.getFullName();

                    }
                });
            }

            server.addServerListener(new ServerListener() {
                @Override
                public void serverStatusEvent(NetworkServer server, int event) {
                }
            });

            server.addSessionListener(new SessionListener() {
                @Override
                public void sessionClosed(SrvSession sess) {


                    SMB.log(MessageRecord.TYPE.RECEIVE, "SESSION CLOSED", 139, sess.getRemoteAddress(), 139);
                }

                @Override
                public void sessionCreated(SrvSession sess) {
                    SMB.getListener().getService().notifyUI(Handler.class.getName(),
                            new String[] {
                                    SMB.getListener().getService().getString(R.string.broadcast_started),
                                    SMB.getListener().getProtocol().toString(),
                                    Integer.toString(SMB.getListener().getPort())
                            }
                    );

                    SMB.log(MessageRecord.TYPE.RECEIVE, "SESSION CREATED", 139, sess.getRemoteAddress(), 139);

                }

                @Override
                public void sessionLoggedOn(SrvSession sess) {

                    SMB.log(MessageRecord.TYPE.RECEIVE, "SESSION LOGGED ON", 139, sess.getRemoteAddress(), 139);

                }
            });
            server.getShareMapper();
            server.startServer();
        }
    }

    /**
     * stops the server
     */
    public void stop(){
        for(int i=0; i<serverConfiguration.numberOfServers(); i++){
            NetworkServer server = serverConfiguration.getServer(i);
            server.shutdownServer(true);
        }
    }

    /**
     * helper method to obtain the broadcast address
     */
    private String getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) MainActivity.context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) (broadcast >> (k * 8));
        return InetAddress.getByAddress(quads).toString().substring(1);
    }

    /**
     * helper method to convert the ip from int to InetAddress
     */
    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }
/**
 * helper method to delete the contents of the
 * apps cache folder
 */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }
}
