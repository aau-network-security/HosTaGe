package de.tudarmstadt.informatik.hostage.protocol.commons.patterns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tudarmstadt.informatik.hostage.commons.MyLinkedMap;

public class IpPattern {

    /**
     * Regex pattern matching Groups 1 and 2 will hold IP and port, respectively.
     * "(" capturing group,
     * "\d" Matches any digit character (0-9). Equivalent to [0-9].
     * "{1,3} or {1,5} Matches the specified quantity of the previous token.
     * "\." Matches a "." character.
     * @return receiver and sender ip and port.
     */
    public static MyLinkedMap<Integer,String> getsAllIpsPorts(String capturePacket) {
        MyLinkedMap<Integer,String> allIpsPorts = new MyLinkedMap<>(); //keeps the insertion order.
        final Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})");
        Matcher matcher = pattern.matcher(capturePacket);
        while (matcher.find()) {
            allIpsPorts.put(Integer.valueOf(matcher.group(2)),matcher.group(1));//group 2 port, group 1 IP.
        }
        return allIpsPorts;
    }

}
