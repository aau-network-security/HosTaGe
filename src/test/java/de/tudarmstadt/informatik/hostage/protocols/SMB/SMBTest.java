package de.tudarmstadt.informatik.hostage.protocols.SMB;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;


import org.alfresco.jlan.netbios.server.NetBIOSNameServer;
import org.alfresco.jlan.server.NetworkServer;
import org.alfresco.jlan.server.SessionListener;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.smb.server.SMBServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tudarmstadt.informatik.hostage.protocols.AMQP.LogBackFilter;
import de.tudarmstadt.informatik.hostage.protocols.AMQP.LogbackSpy;

public class SMBTest {

    final static ArrayList<String> packets = new ArrayList<>();
    final static ArrayList<String> prints = new ArrayList<>();

    @Before
    public void testSecondSMB() throws IOException, InvalidConfigurationException, DeviceContextException {
        ServerConfiguration cfg = new JLANFileServerConfiguration();

        NetBIOSNameServer netBIOSNameServer = new NetBIOSNameServer(cfg);
        netBIOSNameServer.addServerListener((server1, event) -> {
        });

        netBIOSNameServer.addSessionListener(new SessionListener() {
            @Override
            public void sessionClosed(SrvSession sess) {
                packets.add("closed");
                System.out.println("Closed...............");

            }

            @Override
            public void sessionCreated(SrvSession sess) {
                packets.add("open");
                System.out.println("Open...............");


            }

            @Override
            public void sessionLoggedOn(SrvSession sess) {

                packets.add("loggedon");
                System.out.println("Logged...............");

            }
        });

        cfg.addServer(netBIOSNameServer);



        SMBServer smbServer = new SMBServer(cfg);
        smbServer.addServerListener((server1, event) -> {
            System.out.println("Server!!..............."+ server1.getSecurityConfiguration().getUserAccounts().getUserAt(0));

        });

        smbServer.addSessionListener(new SessionListener() {
            @Override
            public void sessionClosed(SrvSession sess) {
                packets.add("closed");
                System.out.println("Closed!!...............");

            }

            @Override
            public void sessionCreated(SrvSession sess) {
                packets.add("open");
                System.out.println("Open!!...............");


            }

            @Override
            public void sessionLoggedOn(SrvSession sess) {

                packets.add("loggedon");
                System.out.println("Logged!!...............");


            }
        });
        cfg.addServer(smbServer);
        // start servers
        for (int i = 0; i < cfg.numberOfServers(); i++) {
            NetworkServer server = cfg.getServer(i);
            server.startServer();
        }
        System.setOut(new MyFilterPrintStream(System.out));

    }

    @Test
    public void testClient() throws IOException {
        SMBClient client = new SMBClient();

        try (Connection connection = client.connect("0.0.0.0",8812)) {
            AuthenticationContext ac = new AuthenticationContext("GUEST", "1234".toCharArray(), "MYDOMAIN");
            Session session = connection.authenticate(ac);

            // Connect to Share
//            try (DiskShare share = (DiskShare) session.connectShare("JLANSHARE")) {
//                System.out.println(share.fileExists("Windows"));
//            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @After
    public void tesPrints(){
        System.out.println("Output prints: "+ prints.get(0));

    }

    public class MyFilterPrintStream extends PrintStream {
        public MyFilterPrintStream(OutputStream out) {
            super(out);
        }

        @Override
        public void print(String s) {
            final Pattern secondPattern = Pattern.compile("\\Q[\\ESMB\\Q]\\E Connection from*");
            Matcher matcher = secondPattern.matcher(s);
            if (matcher.find()) {
                // ... process output string here ...
                prints.add(s);
            }
            // pass along to actual console output
            super.print(s);
        }
    }

}
