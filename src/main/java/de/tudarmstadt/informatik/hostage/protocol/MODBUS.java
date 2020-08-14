package de.tudarmstadt.informatik.hostage.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.tudarmstadt.informatik.hostage.wrapper.Packet;


/**
 * Created by Shreyas Srinivasa on 25.05.15.
 *
 * Modbus serial communications protocol on industraial PLCs
 */
public class MODBUS implements Protocol {


    private int port = 502;

    @Override
    public int getPort() { return port; }

    @Override
    public void setPort(int port){ this.port = port;}

    public boolean isClosed() {
        return false;
    }

    public boolean isSecure() {
        return false;
    }

    private StringBuffer command = new StringBuffer();


    @Override
    public String toString() {
        return "MODBUS";
    }

    @Override
    public TALK_FIRST whoTalksFirst() {
        return TALK_FIRST.CLIENT;
    }

    //Declarations

    HashMap<Integer,Integer> coil = new HashMap<Integer,Integer>();
    HashMap<Integer,Integer> register = new HashMap<Integer,Integer>();
    HashMap<Integer,Integer> discreteInput = new HashMap<Integer,Integer>();



    //Function Request Codes
    public static final int READ_COILS = 1;
    public static final int READ_INPUT_DISCRETES = 2;
    public static final int READ_HOLDING_REGISTERS=3;
    public static final int READ_INPUT_REGISTERS = 4;
    public static final int WRITE_COIL = 5;
    public static final int WRITE_SINGLE_REGISTER = 6;
    public static final int MODBUS_SERVICE = 17; //for detection using metasploit module
    public static final int MODBUS_DISCOVER=1;


    public int sid=1; // Denotes the Unit Number or Slave_ID of the device

    public static final int COIL_MAX_DATA_ADDRESS = 128; // Max coil data address

    public static final int COIL_START_ADDRESS = 1; // Start address of coil

    public static final int DISCRETE_MAX_DATA_ADDRESS = 10032; // Max DISCRETE_INPUT data address

    public static final int DISCRETE_START_ADDRESS = 10001; // Start address of DISCRETE_INPUT

    public static final int ANALOG_INPUT_MAX_DATA_ADDRESS = 30009; //Max ANALOG_INPUT data address

    public static final int ANALOG_INPUT_START_ADDRESS = 30001;//Start address of ANALOG_INPUT

    public static final int HOLDING_REGISTERS_MAX_ADDRESS = 40009; // MAX HOLDING_REGISTER data_address

    public static final int HOLDING_REGISTERS_START_ADDRESS = 40001; // Start address of the HOLDING_REGISTER


    @Override
    public List<Packet> processMessage(Packet requestPacket) {
        List<Packet> responsePackets = new ArrayList<Packet>();



        byte[] request = null;
        if (requestPacket != null) {
            request = requestPacket.getBytes();


           // getRequestType(request);

            responsePackets=processRequest(request,getRequestType(request));



        }


        return responsePackets;
    }



    private List<Packet> processRequest(byte[] request,int requestType) {

        List<Packet> responsePackets = new ArrayList<Packet>();
        switch (requestType){

            case MODBUS_SERVICE:
               // responsePackets.add(new Packet(request,getDeviceInfo()));
                responsePackets.add(new Packet(getDeviceInfo()+"\r\n","EE:FF:66:88:GH:JI:DJ"));
                System.out.println(responsePackets);
                break;

            case READ_INPUT_REGISTERS:

                sid=(request[6]);
                int registerAddress = (request[9]);


                if(sid==1){
                    //Exception packet
                }

                else if(sid==2 && registerAddress >= ANALOG_INPUT_START_ADDRESS && registerAddress<=ANALOG_INPUT_MAX_DATA_ADDRESS) {
                    request[9] = (byte) readRegister(registerAddress);
                    responsePackets.add(new Packet(request, getDeviceInfo()));

                }

                else if(sid==2 && registerAddress < ANALOG_INPUT_START_ADDRESS || registerAddress > ANALOG_INPUT_MAX_DATA_ADDRESS ){
                    //Exception packet
                }

                break;

            case READ_HOLDING_REGISTERS:
                sid=request[6];
                int holdingRegisterAddress=request[9];

                if (sid==1){
                    //exception packet
                }

                else if (sid==2 && holdingRegisterAddress >=HOLDING_REGISTERS_START_ADDRESS && holdingRegisterAddress <= HOLDING_REGISTERS_MAX_ADDRESS){

                    request[9] = (byte) readRegister(holdingRegisterAddress);
                    responsePackets.add(new Packet(request, getDeviceInfo()));

                }

                else if(sid==2 && holdingRegisterAddress < HOLDING_REGISTERS_START_ADDRESS || holdingRegisterAddress > HOLDING_REGISTERS_MAX_ADDRESS ){
                    //Exception packet
                }

            case READ_COILS:

                sid= (request[6]);
                int address = (request[9]);

                if(sid==1&&address<COIL_MAX_DATA_ADDRESS && address>=COIL_START_ADDRESS){

                    request[5]=4;

                    request[9]=(byte)readCoil(address);
                    responsePackets.add(new Packet(request,getDeviceInfo()));


                }

                //Imitating Siemens Simatic S7-200 Architecture
                else if(sid==1 && address<COIL_START_ADDRESS || address>COIL_MAX_DATA_ADDRESS){

                  request[7]=(byte)129;
                  request[8]=(byte)2;
                  request[9]=0;
                  request[10]=0;
                  request[11]=0;

                  responsePackets.add(new Packet(request,getDeviceInfo()));

                }

                else if(sid==2){
                    //Exception packet
                }


                break;


            case READ_INPUT_DISCRETES:

                sid =request[6];
                int inputAddress = (request[9]);

                if(sid==1&& inputAddress>DISCRETE_MAX_DATA_ADDRESS || inputAddress<DISCRETE_START_ADDRESS){
                    request[7]=(byte)129;
                    request[8]=(byte)2;
                    request[9]=0;
                    request[10]=0;
                    request[11]=0;

                    responsePackets.add(new Packet(request,getDeviceInfo()));
                }

                else if(sid==1&&inputAddress<DISCRETE_MAX_DATA_ADDRESS && inputAddress>=DISCRETE_START_ADDRESS){


                    request[5]=4;
                    request[9]=(byte)readDiscrete(inputAddress);
                    responsePackets.add(new Packet(request,getDeviceInfo()));

                }

                else if(sid==2){
                    //Exception packet
                }

                break;

            case WRITE_COIL:

                sid=request[6];

                int coilAddress = (request[9]);
                int coilData = (request[10]);

                if(sid==2){
                    //Exception packet
                }

                else if(sid==1 && coilAddress>COIL_MAX_DATA_ADDRESS){
                    //exception packet
                }

                else if(sid==1 && coilAddress<=COIL_MAX_DATA_ADDRESS && coilAddress>=COIL_START_ADDRESS) {
                    writeCoil(coilAddress, coilData);
                    responsePackets.add(new Packet(request, getDeviceInfo()));
                }
                break;


            case WRITE_SINGLE_REGISTER:

                sid = request[6];
                int regAddress=(request[9]);
                int regData=(request[10]);

                if (sid==1){
                    //exception
                }

                else if(sid==2 && regAddress >= ANALOG_INPUT_START_ADDRESS && regAddress<=ANALOG_INPUT_MAX_DATA_ADDRESS) {

                    writeSingleRegister(regAddress,regData);
                    responsePackets.add(new Packet(request,getDeviceInfo()));


                }

                else if(sid==2 && regAddress < ANALOG_INPUT_START_ADDRESS || regAddress > ANALOG_INPUT_MAX_DATA_ADDRESS ){
                    //Exception packet
                }

                else if (sid==2 && regAddress >=HOLDING_REGISTERS_START_ADDRESS && regAddress <= HOLDING_REGISTERS_MAX_ADDRESS){


                        writeSingleRegister(regAddress,regData);
                        responsePackets.add(new Packet(request,getDeviceInfo()));

                }

                else if(sid==2 && regAddress < HOLDING_REGISTERS_START_ADDRESS || regAddress > HOLDING_REGISTERS_MAX_ADDRESS ) {

                    //Exception Packet

                }

                break;

            default:
                break;

        }
    return responsePackets;
    }


    //Read Coil function
    public int readCoil(int address) {

        address+=1;//has an offset 1

        if (coil.containsKey(address)) {
            int val = coil.get(address);
            return val;
        } else {
            coil.put(address, rand());
            //System.out.println(coil);

            int val = coil.get(address);
            System.out.println("Address:" + address + "Data:" + val);
            return val;
        }
    }
    //Random input of 0 & 1 for coils
    private int rand() {

        int num =(Math.random()<0.5)?0:1;
        return num;
    }


    //Device Information
    private String DeviceInfo = getDeviceInfo();

    private String getDeviceInfo() {

        DeviceInfo = "5369656d656e732053494d415449432053372d323030"; // Hex value for Simatic S7 200
        return DeviceInfo;
    }




    private int readRegister(int registerAddress) {

       // registerAddress+=30001; //Offset of 30001 Check the packet in wireshark and decide to put offset

        if (register.containsKey(registerAddress)) {
            int val = register.get(registerAddress);
            return val;
        } else {
            register.put(registerAddress, randvalue());
            int val = register.get(registerAddress);
            System.out.println("Address:" + registerAddress + "Data:" + val);
            return val;
        }



    }

    private int randvalue(){

        int num =(Math.random()<0.5)?0:255; //Max Hex value that can be stored in 10 bit Binary is 255
        return num;

    }



    private int writeSingleRegister(int regAddress, int regData){

        //regData+=30001;
        register.put(regAddress, regData);
        int val = register.get(regAddress);
        return val;

    }


    private int writeCoil(int coilAddress, int coilData) {

        coilAddress+=1;//offset 1

        coil.put(coilAddress,coilData);

        int val= coil.get(coilAddress);
        return val;

    }



    //Read Coil function
    public int readDiscrete(int address) {

        address+=1;//offset 1

        if (discreteInput.containsKey(address)) {
            int val = discreteInput.get(address);
            return val;
        } else {
            discreteInput.put(address, rand());
            //System.out.println(coil);

            int val = discreteInput.get(address);
            System.out.println("Address:" + address + "Data:" + val);
            return val;
        }
    }





    /* gets the type of request made from the master */
    private int getRequestType(byte[] request) {
        if(request.length !=0) {
            int requestType = request[7];

            if (requestType == 17) {
                requestType = MODBUS_SERVICE;
            } else if (requestType == 5) {
                requestType = WRITE_COIL;
            } else if (requestType == 1) {
                requestType = READ_COILS;
            } else if (requestType == 6) {
                requestType = WRITE_SINGLE_REGISTER;
            } else if (requestType == 4) {
                requestType = READ_INPUT_REGISTERS;
            } else if (requestType == 2) {
                requestType = READ_INPUT_DISCRETES;
            } else if (requestType == 3) {
                requestType = READ_HOLDING_REGISTERS;
            }
            System.out.println(requestType);
            return requestType;
        }
        int requestType = MODBUS_SERVICE;

        return requestType;

    }




}

