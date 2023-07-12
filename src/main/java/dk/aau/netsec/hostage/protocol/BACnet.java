package dk.aau.netsec.hostage.protocol;

import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.service.Service;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyRequest;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.service.unconfirmed.IAmRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.event.DeviceEventListener;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Sequence;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.MessagePriority;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.DiscoveryUtils;
import com.serotonin.bacnet4j.util.RequestUtils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import dk.aau.netsec.hostage.protocol.utils.bacnetUtils.BACnetHandler;
import dk.aau.netsec.hostage.wrapper.Packet;

public class BACnet implements Protocol {
    private static int port = 0xBAC0;
    static IpNetwork network = new IpNetworkBuilder().port(port).build();
    static Transport transport = new DefaultTransport(network);
    static int localDeviceID = 10000 + (int) (Math.random() * 10000);
    static LocalDevice localDevice = new LocalDevice(localDeviceID, transport);

    private static boolean localDeviceStarted = false;

    private static Logger logger = LoggerFactory.getLogger(BACnet.class);

    //private BACnetHandler BACnetHandler = new BACnetHandler(localDeviceID, transport);


    public BACnet() throws Exception {
        if (!localDeviceStarted) {
            startLocalDevice();
        }
    }

    private void startLocalDevice() throws Exception {
        localDevice.initialize();
        localDevice.getConfiguration();
        if (localDevice.isInitialized())
            System.out.println("BACnet Device is initialized...");

        localDevice.sendGlobalBroadcast(new WhoIsRequest());
        System.in.read();
        localDevice.sendGlobalBroadcast(localDevice.getIAm());

        //localDevice.getConfiguration();
        ObjectIdentifier objectId = new ObjectIdentifier(ObjectType.analogValue, 1);
        BACnetObject object = new BACnetObject(objectId, "B'U'TOa");
        object.writeProperty(PropertyIdentifier.presentValue, new Real(12.3f));
        object.writeProperty(PropertyIdentifier.description, new CharacterString("Temperature value"));
        object.writeProperty(PropertyIdentifier.units, EngineeringUnits.degreesCelsius);
        object.writeProperty(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));
        object.writeProperty(PropertyIdentifier.eventState, EventState.normal);
        object.writeProperty(PropertyIdentifier.outOfService, new com.serotonin.bacnet4j.type.primitive.Boolean(false));

        localDevice.addObject(object);

        localDevice.getEventHandler().addListener(new DeviceEventAdapter(){

        //localDevice.terminate();
            @Override
            public void iAmReceived(RemoteDevice remoteDevice) {
            System.out.println("Sending BACnet Who Is Request...");
            localDevice.sendGlobalBroadcast(new WhoIsRequest());

            try {
                Thread.sleep(5*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("BACnet I AM received:");
            System.out.println(remoteDevice.toExtendedString());
            System.out.println("BACnet Device ID: " + remoteDevice.getInstanceNumber());
            System.out.println("BACnet Address: " + remoteDevice.getAddress().getDescription());

        }});
        localDevice.sendGlobalBroadcast(new WhoIsRequest());

        localDevice.getEventHandler().addListener(new DeviceEventListener() {
            @Override
            public void listenerException(Throwable throwable) {

            }

            @Override
            public void iAmReceived(RemoteDevice remoteDevice) {
                System.out.println("Received IAm from BACnet device " + remoteDevice.getInstanceNumber());
                localDevice.sendGlobalBroadcast(localDevice.getIAm());

            }

            @Override
            public boolean allowPropertyWrite(Address address, BACnetObject baCnetObject, PropertyValue propertyValue) {
                return false;
            }

            @Override
            public void propertyWritten(Address address, BACnetObject baCnetObject, PropertyValue propertyValue) {

            }

            @Override
            public void iHaveReceived(RemoteDevice remoteDevice, RemoteObject remoteObject) {

            }

            @Override
            public void covNotificationReceived(UnsignedInteger unsignedInteger, RemoteDevice remoteDevice, ObjectIdentifier objectIdentifier, UnsignedInteger unsignedInteger1, SequenceOf<PropertyValue> sequenceOf) {

            }

            @Override
            public void eventNotificationReceived(UnsignedInteger unsignedInteger, RemoteDevice remoteDevice, ObjectIdentifier objectIdentifier, TimeStamp timeStamp, UnsignedInteger unsignedInteger1, UnsignedInteger unsignedInteger2, EventType eventType, CharacterString characterString, NotifyType notifyType, Boolean aBoolean, EventState eventState, EventState eventState1, NotificationParameters notificationParameters) {

            }

            @Override
            public void textMessageReceived(RemoteDevice remoteDevice, Choice choice, MessagePriority messagePriority, CharacterString characterString) {

            }

            @Override
            public void privateTransferReceived(Address address, UnsignedInteger unsignedInteger, UnsignedInteger unsignedInteger1, Sequence sequence) {

            }

            @Override
            public void reinitializeDevice(Address address, ReinitializeDeviceRequest.ReinitializedStateOfDevice reinitializedStateOfDevice) {

            }


            @Override
            public void synchronizeTime(Address address, DateTime dateTime, boolean b) {

            }

            //@Override
            public void requestReceived(Address address, Service service) {
                if (service instanceof WhoIsRequest) {
                    System.out.println("Received whois BACnet request");
                    localDevice.send(address, localDevice.getIAm());
                }
            }
        });


        localDevice.getEventHandler().addListener(new DeviceEventAdapter() {
            public AcknowledgementService handleRequest(ConfirmedRequestService request) {
                if (request instanceof ReadPropertyRequest) {
                    ReadPropertyRequest readRequest = (ReadPropertyRequest) request;
                    // ...
                } else if (request instanceof WritePropertyRequest) {
                    WritePropertyRequest writeRequest = (WritePropertyRequest) request;
                    // ...
                } else {
                    // Handle other types of requests
                    // ...
                }
                return null;
            }

            public AcknowledgementService handleRequest(UnconfirmedRequestService request) {
                localDevice.sendGlobalBroadcast(new WhoIsRequest());

                if (request instanceof WhoIsRequest) {
                    localDevice.sendGlobalBroadcast(localDevice.getIAm());

                } else if (request instanceof IAmRequest) {
                    System.out.println("Received IAm from a BACnet device");
                    // ...
                } else {
                    // Handle other types of requests
                    // ...
                }
                return null;
            }
        });

        localDevice.sendGlobalBroadcast(new WhoIsRequest());
        System.out.println("Ready to receive BACnet Who-Is requests");

        // Wait for responses
        //Thread.sleep(2000);

        // Get the discovered remote devices
        List<RemoteDevice> remoteDevices = localDevice.getRemoteDevices();

        if (!remoteDevices.isEmpty()) {
            // Get the first remote device
            RemoteDevice remoteDevice = remoteDevices.get(0);
            System.out.println("Remote BACnet Device: " + remoteDevice);

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
    public List<Packet> processMessage(Packet requestPacket) {
        List<Packet> responsePackets = new ArrayList<Packet>();



        byte[] request = null;
        if (requestPacket != null) {
            request = requestPacket.getBytes();


            getRequestType(request);

            responsePackets=processRequest(request,getRequestType(request));



       }


        return null;
    }
    private List<Packet> processRequest(byte[] request,int requestType) {
        String protocol = "BACnet";
        List<Packet> responsePackets = new ArrayList<Packet>();

        switch (requestType) {

            case who_Is:
                try {
                    localDevice.initialize();
                    ObjectIdentifier objectId = null;
                    IAmRequest iAm = new IAmRequest(objectId,new UnsignedInteger(1476), Segmentation.segmentedBoth,new UnsignedInteger(260));
                    localDevice.sendGlobalBroadcast(iAm);
                    responsePackets.add(new Packet(iAm + "\r\n", protocol));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;

            //case who_Has:

        }
        return responsePackets;
    }

    @Override
    public String toString() {
        return "BACnet";
    }

    @Override
    public TALK_FIRST whoTalksFirst() {
        return TALK_FIRST.CLIENT;
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

            System.out.println("BACnet request Type:" + requestType);
            return requestType;
        }


    public static void localDeviceStop(){
        if(localDeviceStarted) {
            localDevice.terminate();
            localDeviceStarted = false;
        }
    }


}