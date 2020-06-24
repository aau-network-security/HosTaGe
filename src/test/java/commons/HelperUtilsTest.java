package commons;

import org.junit.Test;

import de.tudarmstadt.informatik.hostage.commons.HelperUtils;

public class HelperUtilsTest {

    @Test
    public void ipTest(){
        HelperUtils helperUtils = new HelperUtils();

        String ip = "100.92.38.119";

        System.out.println(helperUtils.ipToLongTest(ip));
        System.out.println(helperUtils.IPtoIntTest(ip));

    }
}
