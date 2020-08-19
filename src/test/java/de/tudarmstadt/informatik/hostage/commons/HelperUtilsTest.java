package de.tudarmstadt.informatik.hostage.commons;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class HelperUtilsTest {

    @Test
    public void ipTest() throws UnknownHostException {
        String ip = "109.242.187.121";
        String cidrRange = "192.168.1.5/24";
        SubnetUtils utils = new SubnetUtils(cidrRange);
        boolean isInRange = utils.getInfo().isInRange(ip);

        System.out.println(isInRange);

        InetAddress netmask = InetAddress.getByName("255.255.255.0");

//        int remoteIPAddress = HelperUtils.getInetAddress(InetAddress.getByName(ip));
//        int internalIPAddress = HelperUtils.getInetAddress(InetAddress.getByName(internal));


    }

}
