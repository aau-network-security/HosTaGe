package hostage.logging.formatter.protocol;

import org.junit.Test;

import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.logging.formatter.protocol.MySQL;


public class MySQLTest {


    @Test
    public void testFormat(){
        byte[] protocol = { 0x0a };
        byte[] versionFin = { 0x00 };
        byte[] version = { 0x00 };
        byte[] thread = { 0x2a, 0x00, 0x00, 0x00 };
        byte[] salt = { 0x44, 0x64, 0x49, 0x7e, 0x60, 0x48, 0x25, 0x7e, 0x00 };
        byte[] capabilities = { (byte) 0xff, (byte) 0xf7 };
        byte[] language = { 0x08 };
        byte[] status = { 0x02, 0x00 };
        byte[] unused = { 0x0f, (byte) 0x80, 0x15, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        byte[] salt2 = { 0x6c, 0x26, 0x71, 0x2c, 0x25, 0x72, 0x31, 0x3d, 0x7d,
                0x21, 0x26, 0x3b, 0x00 };
        String payload = "mysql_native_password";
        byte[] fin = { 0x00 };

        byte[] response = HelperUtils.concat(protocol, version,versionFin,
                thread, salt, capabilities, language, status, unused, salt2,
                payload.getBytes(), fin);
        String s = new String(response);
        //Sample TCP Packet
        String samplePacket = "\\x45\\x00\\x00\\x28\\xab\\xcd\\x00\\x00\\x40" +
                "\\x06\\xa6\\xec\\x0a\\x0a\\x0a\\x02\\x0a\\x0a\\x0a\\x01\\x30\\x39" +
                "\\x00\\x50\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x50\\x02\\x71\\x1d";

        MySQL protocolSql = new MySQL();
        String format = protocolSql.format(s);
        System.out.println(format);




    }
}
