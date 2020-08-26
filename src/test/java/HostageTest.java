
import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import dk.aau.netsec.hostage.Hostage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hostage.class})
public class HostageTest {

    @Test
    public void testContext(){
        Context context = mock(Context.class);
        PowerMockito.mockStatic(Hostage.class);
        when(Hostage.getContext()).thenReturn(context);

    }

    @Test
    public void testBroadcast(){
        Hostage hostage = mock(Hostage.class);
        Context context = mock(Context.class);

        String [] values = new String [] {"dk.aau.netsec.hostage.BROADCAST.STARTED","HTTP","true"};
        hostage.notifyUI(" dk.aau.netsec.hostage.Handler",values);
        System.out.println(hostage.hasActiveAttacks());
        assertFalse(hostage.startListener("HTTP",5555));

    }
}
