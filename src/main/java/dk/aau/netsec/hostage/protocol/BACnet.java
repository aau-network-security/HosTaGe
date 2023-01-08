package dk.aau.netsec.hostage.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import dk.aau.netsec.hostage.wrapper.Packet;

public class BACnet implements Protocol {
    private final static String defaultAddress="0.0.0.0";//change to your IP.
    private final static int defaultPort = 47808;
    private int port = 47808;
    private static boolean serverStarted = false; //prevents the server from starting multiple times from the threads
    private static InetAddress address=null;
    static {
        try {
            address = InetAddress.getByName(defaultAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    public static final int Bacnet_SERVICE = 17; //for detection using metasploit module

    //BACnet unconfirmed service
    public static final int i_Am = 0;
    public static final int i_Have = 1;
    public static final int unconfirmedCOVNotification = 2;
    public static final int unconfirmedEventNotification = 3;
    public static final int unconfirmedPrivateTransfer = 4;
    public static final int unconfirmedTextMessage = 5;
    public static final int timeSynchronization = 6;
    public static final int who_Has = 7;
    public static final int who_Is = 8;
    public static final int utcTimeSynchonization = 9;

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String toString() {
        return "BACnet";
    }

    @Override
    public TALK_FIRST whoTalksFirst()  {
        return TALK_FIRST.SERVER;
    }

    @Override
    public List<Packet> processMessage(Packet requestPacket) {
        List<Packet> responsePackets = new ArrayList<Packet>();

        byte[] request = null;
        if (requestPacket != null) {
            request = requestPacket.getBytes();

            //getRequestType(request);
            responsePackets=processRequest(request,getRequestType(request));

        }


        return responsePackets;
    }

    private List<Packet> processRequest(byte[] request,int requestType) {
        String protocol = "BACnet";
        List<Packet> responsePackets = new ArrayList<Packet>();
        switch (requestType) {

            case who_Is:
                responsePackets.add(new Packet("810b00190120ffff00ff1000c4020000042205b89100220104",protocol));
                //responsePackets.add(new Packet(request,getDeviceInfo()));
                break;

            //case who_Has:

        }
        return responsePackets;
    }



    //Device Information
    private String DeviceInfo = getDeviceInfo();
    private String getDeviceInfo() {
        DeviceInfo = "31303530363330"; // Hex value for 1050630
        //DeviceInfo = "5369656D656E73204275696C64696E6720546563686E6F6C6F676965732050584335302D452E44"; // Hex value for Siemens Building Technologies PXC50-E.D
        return DeviceInfo;
    }



    private int getRequestType(byte[] request) {

            int requestType = request[10];

            if (requestType == 0) {
                requestType = i_Am;
            } else if (requestType == 1) {
                requestType = i_Have;
            } else if (requestType == 2) {
                requestType = unconfirmedCOVNotification;
            } else if (requestType == 3) {
                requestType = unconfirmedEventNotification;
            } else if (requestType == 4) {
                requestType = unconfirmedPrivateTransfer;
            } else if (requestType == 5) {
                requestType = unconfirmedTextMessage;
            } else if (requestType == 6) {
                requestType = timeSynchronization;
            }
            else if (requestType == 7) {
                requestType = who_Has;
            }
            else if (requestType == 8) {
                requestType = who_Is;
            }
            else if (requestType == 9) {
                requestType = utcTimeSynchonization;
            }

            System.out.println(requestType);
            return requestType;
        }


    public static void serverStop(){
        if(serverStarted) {
            serverStarted = false;
        }
    }


}