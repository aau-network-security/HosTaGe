package dk.aau.netsec.hostage.protocol.commons.logWatchers;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterceptSysout extends PrintStream {
    final static ArrayList<String> packets = new ArrayList<>();

    public InterceptSysout(OutputStream out) {
        super(out);
    }

    /**
     * Intercept System.out.println without disturbing the console.
     * The packets from SMP protocol contain the connection received with the real port.
     * @param output System.out output as a stream.
     */
    @Override
    public void print(String output) {
        final Pattern secondPattern = Pattern.compile("\\Q[\\ESMB\\Q]\\E Connection from*");
        Matcher matcher = secondPattern.matcher(output);
        if (matcher.find()) {
            packets.add(output);
        }
        super.print(output);
    }

    public ArrayList<String> getPackets(){
        return packets;
    }

    public String getPacket(){
        return packets.get(0);
    }

}
