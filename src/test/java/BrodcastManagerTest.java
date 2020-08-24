import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LocalBroadcastManager.class})
public class BrodcastManagerTest {
    @Mock
    private Intent intent;
    @Test
    public void testBroadcast() throws Exception {
        MockitoAnnotations.initMocks(this);
        Context context = mock(Context.class);
        PowerMockito.whenNew(Intent.class).withArguments(String.class).thenReturn(intent);
        PowerMockito.mockStatic(LocalBroadcastManager.class);
        LocalBroadcastManager instance = mock(LocalBroadcastManager.class);
        PowerMockito.when(LocalBroadcastManager.getInstance(context)).thenReturn(instance);

        String [] values = new String [] {"de.tudarmstadt.informatik.hostage.BROADCAST.STARTED","HTTP","true"};
        intent.putExtra("SENDER", " de.tudarmstadt.informatik.hostage.Handler");
        intent.putExtra("VALUES", values);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
