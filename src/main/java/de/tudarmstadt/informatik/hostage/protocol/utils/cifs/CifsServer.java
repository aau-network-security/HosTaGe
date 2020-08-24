package de.tudarmstadt.informatik.hostage.protocol.utils.cifs;

import org.alfresco.jlan.server.NetworkServer;
import org.alfresco.jlan.server.SessionListener;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.smb.server.SMBServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.tudarmstadt.informatik.hostage.Handler;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.MyLinkedMap;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.protocol.SMB;
import de.tudarmstadt.informatik.hostage.protocol.commons.logWatchers.InterceptSysout;
import de.tudarmstadt.informatik.hostage.protocol.commons.patterns.IpPattern;


/**
 * HostageV3
 * ================
 * @author Alexander Brakowski
 * @author Daniel Lazar
 * on 19.03.15.
 */
public class CifsServer  {

    protected final de.tudarmstadt.informatik.hostage.protocol.SMB SMB;
    protected final FileInject fileInject;
    ServerConfiguration cfg = new JLANFileServerConfiguration();

    private int defaultPort=1025;

    public CifsServer(SMB SMB, FileInject fileInject) throws InvalidConfigurationException, DeviceContextException {
        super();
        this.SMB = SMB;
        this.fileInject = fileInject;
    }

    public void run() throws IOException {
        SMBServer smbServer = new SMBServer(cfg);
        smbServer.addServerListener((server1, event) -> {
            System.out.println("Server started with users: "+ server1.getSecurityConfiguration().getUserAccounts().getUserAt(0));
        });
        InterceptSysout interceptPackets =  new InterceptSysout(System.out);
        System.setOut(interceptPackets);

        smbServer.addSessionListener(new SessionListener() {
            @Override
            public void sessionClosed(SrvSession sess) {
                SMB.log(MessageRecord.TYPE.RECEIVE, "SESSION CLOSED", defaultPort, sess.getRemoteAddress(), getRemotePort(interceptPackets.getPacket()));
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

                SMB.log(MessageRecord.TYPE.RECEIVE, "SESSION CREATED", defaultPort, sess.getRemoteAddress(), getRemotePort(interceptPackets.getPacket()));

            }

            @Override
            public void sessionLoggedOn(SrvSession sess) {
                SMB.log(MessageRecord.TYPE.RECEIVE, "SESSION LOGGED ON", defaultPort, sess.getRemoteAddress(), getRemotePort(interceptPackets.getPacket()));

            }
        });

        cfg.addServer(smbServer);

        for(int i=0; i<cfg.numberOfServers(); i++){
            NetworkServer server = cfg.getServer(i);
            server.startServer();
        }
    }

    /**
     * stops the server
     */
    public void stop(){
        for(int i=0; i<cfg.numberOfServers(); i++){
            NetworkServer server = cfg.getServer(i);
            server.shutdownServer(true);
        }
    }

    /**
     * First inserted port is the remote one.
     * @return Remote port of the attacker.
     */
    private static int getRemotePort(String portPacket){
        MyLinkedMap<Integer,String> remotePorts = IpPattern.getsAllIpsPorts(portPacket);
        if(!remotePorts.isEmpty() )
            return remotePorts.getEntry(0).getKey();
        return 0;
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

}
