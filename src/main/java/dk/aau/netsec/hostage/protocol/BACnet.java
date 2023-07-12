package dk.aau.netsec.hostage.protocol;

//import java.util.List;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;

import java.io.IOException;
import java.util.List;
import dk.aau.netsec.hostage.wrapper.Packet;

public class BACnet implements Protocol {
    private final static String defaultAddress="0.0.0.0";//change to your IP.
    private final static int defaultPort = 0xBAC0; //47808
    private int port = 47808;
    private static boolean serverStarted = false; //prevents the server from starting multiple times from the threads


    public void serverStop() {

    }


    @Override
    public int getPort() {
        return defaultPort;
    }

    @Override
    public void setPort(int port) {
        this.port = defaultPort;

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
        return null;
    }

    @Override
    public TALK_FIRST whoTalksFirst() {
        return TALK_FIRST.SERVER;
    }


    /**
     * Starts the BACnet server.
     * @throws IOException
     */
    public void startBACNetServer() throws Throwable {
        IpNetwork network = new IpNetworkBuilder().port(port).build();
        Transport transport = new DefaultTransport(network);

        int localDeviceID = 10000 + (int) ( Math.random() * 10000);
        LocalDevice localDevice = new LocalDevice(localDeviceID, transport);
        localDevice.initialize();

        System.out.println("Local device is running with device id " + localDeviceID);

        ObjectIdentifier objectId = new ObjectIdentifier(ObjectType.analogValue, 1);

        // BACnetObject object = new BACnetObject(localDevice, objectId);
        BACnetObject object = new BACnetObject(objectId, "B'U'TOa");

        object.writeProperty(PropertyIdentifier.presentValue, new Real(12.3f));
        object.writeProperty(PropertyIdentifier.description, new CharacterString("Temperaturwert"));
        object.writeProperty(PropertyIdentifier.units, EngineeringUnits.degreesCelsius);
        object.writeProperty(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));
        object.writeProperty(PropertyIdentifier.eventState, EventState.normal);
        object.writeProperty(PropertyIdentifier.outOfService, new com.serotonin.bacnet4j.type.primitive.Boolean(false));

        localDevice.addObject(object);

        serverStarted=true;

        //return server;
    }

    public BACnet(){

        if(!serverStarted) {
            try {
                startBACNetServer();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}
