package hostage.logging;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.tudarmstadt.informatik.hostage.logging.AttackRecord;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AttackRecordTest {
    private long attack_id;
    private long sync_id;
    private String bssid;
    private String device;
    private String protocol;
    private String localIP;
    private int localPort;
    private String remoteIP;
    private int remotePort;
    private String externalIP;
    private int wasInternalAttack;

    @Before
    public void setUp() {
        attack_id = 1234L;
        sync_id = 1234L;
        bssid = "Test";
        device = "TestDevice";
        protocol = "Http";
        localIP = "127.0.0.1";
        localPort = 80;
        remoteIP = "127.0.0.1";
        remotePort = 3306;
        externalIP = "127.0.0.1";
        wasInternalAttack = 1;


    }

    @Test
    public void attackRecordParcelTest(){
        Parcel parcel = Parcel.obtain();

        parcel.writeLong(attack_id);
        parcel.writeString(protocol);
        parcel.writeString(localIP);
        parcel.writeInt(localPort);
        parcel.writeString(remoteIP);
        parcel.writeInt(remotePort);
        parcel.writeString(externalIP);
        parcel.writeInt(wasInternalAttack);
        parcel.writeString(bssid);
        parcel.writeString(device);
        parcel.writeLong(sync_id);

        AttackRecord record = new AttackRecord(parcel);

        assertTrue(record.getAttack_id() == 1234L);
        assertTrue(record.getSync_id() == 1234L) ;
        assertTrue(record.getLocalIP().equals("127.0.0.1"));
        assertTrue(record.getLocalPort() == 80) ;
        assertTrue(record.getRemoteIP().equals("127.0.0.1"));
        assertTrue(record.getRemotePort() == 3306) ;
        assertTrue(record.getExternalIP().equals("127.0.0.1"));
        assertTrue(record.getWasInternalAttack()) ;
        assertTrue(record.getBssid().equals("Test"));
        assertTrue(record.getDevice().equals("TestDevice"));
        assertTrue(record.getProtocol().equals("Http"));



    }
}
