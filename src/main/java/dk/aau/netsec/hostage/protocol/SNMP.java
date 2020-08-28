package dk.aau.netsec.hostage.protocol;

import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;
import org.snmp4j.transport.TransportMappings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dk.aau.netsec.hostage.protocol.utils.snmpUtils.MOTableBuilder;
import dk.aau.netsec.hostage.wrapper.Packet;

/**
 * Created by root on 06.07.15.
 */
    public class SNMP extends BaseAgent implements Protocol {
    private int port = 161;

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

    @Override
    public List<Packet> processMessage(Packet requestPacket) {
        List<Packet> responsePackets = new ArrayList<>();
        responsePackets.add(requestPacket);

        try {
            setUp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return responsePackets;
    }

    @Override
    public TALK_FIRST whoTalksFirst() {
        return TALK_FIRST.CLIENT;
    }

    @Override
    public String toString() {
        return "SNMP";
    }

    private String address;

    public SNMP(String address) throws IOException {

            super(new File("conf.agent"), new File("bootCounter.agent"),
                    new CommandProcessor(
                            new OctetString(MPv3.createLocalEngineID())));
            this.address = address;
        }

    @Override
    protected void addCommunities(SnmpCommunityMIB snmpCommunityMIB) {
            Variable[] com2sec = new Variable[]{
                new OctetString("public"),
                new OctetString("cpublic"), // security name
                getAgent().getContextEngineID(), // local engine ID
                new OctetString("public"), // default context name
                new OctetString(), // transport tag
                new Integer32(StorageType.nonVolatile), // storage type
                new Integer32(RowStatus.active) // row status
        };

        MOTableRow row = snmpCommunityMIB.getSnmpCommunityEntry().createRow(
            new OctetString("public2public").toSubIndex(true), com2sec);
            snmpCommunityMIB.getSnmpCommunityEntry().addRow((SnmpCommunityMIB.SnmpCommunityEntryRow) row);
    }

    @Override
    protected void addNotificationTargets(SnmpTargetMIB snmpTargetMIB, SnmpNotificationMIB snmpNotificationMIB) {}

    @Override
    protected void addUsmUser(USM usm) {}

    @Override
    protected void addViews(VacmMIB vacmMIB) {
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString("cpublic"), new OctetString("v1v2group"), StorageType.nonVolatile);

        vacmMIB.addAccess(new OctetString("v1v2group"), new OctetString("public"),
                SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
                new OctetString("fullWriteView"), new OctetString("fullNotifyView"), StorageType.nonVolatile);

        vacmMIB.addViewTreeFamily(new OctetString("fullReadView"), new org.snmp4j.smi.OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
    }

    @Override
    protected void registerManagedObjects() {}

    @Override
    protected void unregisterManagedObjects() {}

    public SNMP() {
        super("");
    }

    public void start() throws IOException{
        init();
        try {
            setUp();
        } catch (Exception e) {
            e.printStackTrace();
        }
            // This method reads some old config from a file and causes
            // unexpected behavior.
            // loadConfig(ImportModes.REPLACE_CREATE);
        addShutdownHook();
        getServer().addContext(new OctetString("public"));
        finishInit();
        run();
        sendColdStartNotification();
    }

    protected void initTransportMappings() {
        transportMappings = new TransportMapping[1];
        Address addr = GenericAddress.parse(address);

        TransportMapping tm = TransportMappings.getInstance().createTransportMapping(addr);
        transportMappings[0] = tm;
    }


    public void registerManagedObject(ManagedObject mo) {
        try {
            server.register(mo, null);
        } catch (DuplicateRegistrationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void unregisterManagedObject(MOGroup moGroup){
        moGroup.unregisterMOs(server, getContext(moGroup));
    }


    // standard in RFC-1213
    static final OID interfacesTable = new OID(".1.3.6.1.2.1.2.2.1");
    public static void setUp () throws Exception {
        SNMP agent = new SNMP("0.0.0.0/161");
        agent.start();

        MOTableBuilder builder = new MOTableBuilder(interfacesTable)
                .addColumnType(SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_ONLY)
                .addColumnType(SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY)
                .addColumnType(SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_ONLY)
                .addColumnType(SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_ONLY)
                .addColumnType(SMIConstants.SYNTAX_GAUGE32, MOAccessImpl.ACCESS_READ_ONLY)
                .addColumnType(SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY)
                .addColumnType(SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_ONLY)
                .addColumnType(SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_ONLY)

                .addRowValue(new Integer32(1))
                .addRowValue(new OctetString("loopback"))
                .addRowValue(new Integer32(24))
                .addRowValue(new Integer32(1500))
                .addRowValue(new Gauge32(10000000))
                .addRowValue(new OctetString("00:00:00:00:01"))
                .addRowValue(new Integer32(1500))
                .addRowValue(new Integer32(1500))
    //next row
                .addRowValue(new Integer32(2))
                .addRowValue(new OctetString("eth0"))
                .addRowValue(new Integer32(24))
                .addRowValue(new Integer32(1500))
                .addRowValue(new Gauge32(10000000))
                .addRowValue(new OctetString("00:00:00:00:02"))
                .addRowValue(new Integer32(1500))
                .addRowValue(new Integer32(1500));

            agent.registerManagedObject(builder.build());

            // Setup the client to use our newly started agent
            //client = new SimpleSnmpClient("udp:127.0.0.1/2001");
        }
    }


