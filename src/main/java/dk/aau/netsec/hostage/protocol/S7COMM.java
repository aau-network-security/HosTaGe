package dk.aau.netsec.hostage.protocol;

import java.util.ArrayList;
import java.util.List;

import dk.aau.netsec.hostage.wrapper.Packet;


/**
 * Created by Shreyas Srinivasa on 06.07.15.
 */
public class S7COMM implements Protocol {
    private int port = 102;

    @Override
    public int getPort() { return port; }

    @Override
    public void setPort(int port){ this.port = port;}

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }


    //S7COMM Siemens Simatic Parameter codes

    public static String DIAGNOSTICS = "0x00";
    public static String CONNECT = "0x0e";
    public static String DATA = "0x0f";
    public static String READ = "0x04";
    public static String WRITE = "0x05";
    public static String REQUEST_DOWNLOAD="0x1a";
    public static String DOWNLOAD_BLOCK="0x1b";
    public static String END_DOWNLOAD="0x1c";
    public static String START_UPLOAD="0x1d";
    public static String UPLOAD="0x1e";
    public static String END_UPLOAD="0x1f";

    public static final int READ_COILS = 1;
    public static final int READ_INPUT_DISCRETES = 2;
    public static final int READ_HOLDING_REGISTERS=3;
    public static final int READ_INPUT_REGISTERS = 4;
    public static final int WRITE_COIL = 5;
    public static final int WRITE_SINGLE_REGISTER = 6;


    @Override
    public List<Packet> processMessage(Packet requestPacket) {

        List<Packet> responsePackets = new ArrayList<Packet>();

        byte[] request = null;
        if (requestPacket != null) {
            request = requestPacket.getBytes();

            //getRequestType(request);

           // responsePackets.add(requestPacket); // Response packets have to be studied yet

            responsePackets=processRequest(request,getRequestType(request));

        }


        return responsePackets;
    }

    private List<Packet> processRequest(byte[] request, int requestType) {


        List<Packet> responsePackets = new ArrayList<Packet>();

        return  responsePackets;


    }

    @Override
    public TALK_FIRST whoTalksFirst() {
        return null;
    }

    @Override
    public String toString(){
        return "S7COMM";
    }


    private int getRequestType(byte[] request) {

        int requestType=request[7];

        if (requestType == 5) {
            requestType = WRITE_COIL;
        } else if (requestType == 1) {
            requestType = READ_COILS;
        } else if (requestType == 6) {
            requestType = WRITE_SINGLE_REGISTER;
        } else if (requestType == 4) {
            requestType = READ_INPUT_REGISTERS;
        }
        else if (requestType==2){
            requestType = READ_INPUT_DISCRETES;
        }
        else if (requestType==3){
            requestType = READ_HOLDING_REGISTERS;
        }

        System.out.println(requestType);
        return requestType;
    }






}
