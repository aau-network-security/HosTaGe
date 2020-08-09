package de.tudarmstadt.informatik.hostage.protocol.cifs;

import org.alfresco.jlan.debug.DebugConfigSection;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.auth.CifsAuthenticator;
import org.alfresco.jlan.server.auth.ClientInfo;
import org.alfresco.jlan.server.auth.LocalAuthenticator;
import org.alfresco.jlan.server.auth.UserAccount;
import org.alfresco.jlan.server.auth.UserAccountList;
import org.alfresco.jlan.server.auth.acl.DefaultAccessControlManager;
import org.alfresco.jlan.server.config.CoreServerConfigSection;
import org.alfresco.jlan.server.config.GlobalConfigSection;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.config.SecurityConfigSection;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.server.filesys.DiskDeviceContext;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.DiskSharedDevice;
import org.alfresco.jlan.server.filesys.FilesystemsConfigSection;
import org.alfresco.jlan.server.filesys.SrvDiskInfo;
import org.alfresco.jlan.smb.server.CIFSConfigSection;
import org.springframework.extensions.config.element.GenericConfigElement;

import java.io.File;

import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

public class JLANFileServerConfiguration extends ServerConfiguration {
    private static final String HOSTNAME = "HOME-PC";
    private static final String DOMAINNAME = "WORKGROUP";
    private static final int defaultPort = 1025;


    private static final int DefaultThreadPoolInit  = 25;
    private static final int DefaultThreadPoolMax   = 50;

    private static final int[] DefaultMemoryPoolBufSizes  = { 256, 4096, 16384, 66000 };
    private static final int[] DefaultMemoryPoolInitAlloc = {  20,   20,     5,     5 };
    private static final int[] DefaultMemoryPoolMaxAlloc  = { 100,   50,    50,    50 };


    public JLANFileServerConfiguration() throws InvalidConfigurationException, DeviceContextException {
        super(HOSTNAME);
        setServerName(HOSTNAME);

        // DEBUG
        setDebugConfig();
        // CORE
        setCoreConfig();
        // GLOBAL
        GlobalConfigSection globalConfig = new GlobalConfigSection(this);
        // SECURITY
        SecurityConfigSection secConfig = setUpSecurity();
        // SHARES
        FilesystemsConfigSection filesysConfig = setUpFolders();
        setUpShareFolder(secConfig,filesysConfig);
        // SMB
        CIFSConfigSection cifsConfig = new CIFSConfigSection(this);
        cifsConfig.setHostAnnouncer(true);
        cifsConfig.setServerName(HOSTNAME);
        cifsConfig.setDomainName(DOMAINNAME);
        cifsConfig.setDatagramPort(8888);
        cifsConfig.setSessionPort(8889);
        cifsConfig.setNameServerPort(8810);
        cifsConfig.setTcpipSMBPort(defaultPort);
        cifsConfig.setHostAnnouncerPort(8888);
        cifsConfig.setHostAnnounceInterval(5);
        cifsConfig.setHostAnnouncer(true);

        CifsAuthenticator authenticator = setUpAuthenticator();
        final GenericConfigElement authenticatorConfigElement = new GenericConfigElement("authenticator");
        authenticator.initialize(this, authenticatorConfigElement);
        cifsConfig.setAuthenticator(authenticator);
        cifsConfig.setHostAnnounceDebug(true);
        cifsConfig.setNetBIOSDebug(true);
        cifsConfig.setSessionDebugFlags(-1);
        cifsConfig.setTcpipSMB(true);
    }

    private void setDebugConfig() throws InvalidConfigurationException {
        DebugConfigSection debugConfig = new DebugConfigSection(this);
        final GenericConfigElement debugConfigElement = new GenericConfigElement("output");
        final GenericConfigElement logLevelConfigElement = new GenericConfigElement("logLevel");
        logLevelConfigElement.setValue("Debug");
        debugConfig.setDebug("org.alfresco.jlan.debug.ConsoleDebug", debugConfigElement);

    }

    private void setCoreConfig() throws InvalidConfigurationException {
        CoreServerConfigSection coreConfig = new CoreServerConfigSection(this);
        coreConfig.setMemoryPool( DefaultMemoryPoolBufSizes, DefaultMemoryPoolInitAlloc, DefaultMemoryPoolMaxAlloc);
        coreConfig.setThreadPool(DefaultThreadPoolInit, DefaultThreadPoolMax);
        coreConfig.getThreadPool().setDebug(true);
    }

    private SecurityConfigSection setUpSecurity() throws InvalidConfigurationException {
        SecurityConfigSection secConfig = new SecurityConfigSection(this);
        DefaultAccessControlManager accessControlManager = new DefaultAccessControlManager();
        accessControlManager.setDebug(true);
        accessControlManager.initialize(this, new GenericConfigElement("aclManager"));
        secConfig.setAccessControlManager(accessControlManager);
        secConfig.setJCEProvider("cryptix.jce.provider.CryptixCrypto");
        final UserAccountList userAccounts = new UserAccountList();
        final UserAccount account = new UserAccount();
        account.setUserName("GUEST");
        account.setPassword("1234");
        userAccounts.addUser(account);
        secConfig.setUserAccounts(userAccounts);

        return secConfig;
    }

    private FilesystemsConfigSection setUpFolders(){
        FilesystemsConfigSection filesysConfig = new FilesystemsConfigSection(this);
        File cacheFolder = MainActivity.getContext().getExternalCacheDir();
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
        addins.mkdir();

        return filesysConfig;
    }

    private void setUpShareFolder(SecurityConfigSection secConfig,FilesystemsConfigSection filesysConfig) throws DeviceContextException {
        DiskInterface diskInterface = new org.alfresco.jlan.smb.server.disk.JavaFileDiskDriver();
        final GenericConfigElement driverConfig = new GenericConfigElement("driver");
        final GenericConfigElement localPathConfig = new GenericConfigElement("LocalPath");
        localPathConfig.setValue(".");
        driverConfig.addChild(localPathConfig);
        DiskDeviceContext diskDeviceContext = (DiskDeviceContext) diskInterface.createContext("JLANSHARE", driverConfig);
        diskDeviceContext.setShareName("JLANSHARE");
        diskDeviceContext.setConfigurationParameters(driverConfig);
        diskDeviceContext.enableChangeHandler(false);
        diskDeviceContext.setDiskInformation(new SrvDiskInfo(2560000, 64, 512, 2304000));// Default to a 80Gb sized disk with 90% free space
        DiskSharedDevice diskDev = new DiskSharedDevice("JLANSHARE", diskInterface, diskDeviceContext);
        diskDev.setConfiguration(this);
        diskDev.setAccessControlList(secConfig.getGlobalAccessControls());
        diskDeviceContext.startFilesystem(diskDev);
        filesysConfig.addShare(diskDev);
    }

    private CifsAuthenticator setUpAuthenticator(){
        final CifsAuthenticator authenticator = new LocalAuthenticator() {
            @Override
            public int authenticateUser(ClientInfo client, SrvSession sess, int alg) {
                return AUTH_ALLOW;
            }
        };
        authenticator.setDebug(true);
        authenticator.setAllowGuest(true);
        authenticator.setAccessMode(CifsAuthenticator.USER_MODE);

        return authenticator;
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
