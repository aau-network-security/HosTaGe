
import android.content.Context;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

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

        String [] values = new String [] {"de.tudarmstadt.informatik.hostage.BROADCAST.STARTED","HTTP","true"};
        hostage.notifyUI(" de.tudarmstadt.informatik.hostage.Handler",values);
        System.out.println(hostage.hasActiveAttacks());
        assertFalse(hostage.startListener("HTTP",5555));

    }
}
