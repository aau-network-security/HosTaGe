
import android.content.Context;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.tudarmstadt.informatik.hostage.Hostage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hostage.class})
public class HostageTest {

    @Test
    public void testContext(){
        Context context = null;
        PowerMockito.mockStatic(Hostage.class);
        when(Hostage.getContext()).thenReturn(context);

    }

    @Test
    public void testBroadcast(){
        Hostage hostage = PowerMockito.mock(Hostage.class);
        assertFalse(hostage.startListener("HTTP",5555));

    }
}
