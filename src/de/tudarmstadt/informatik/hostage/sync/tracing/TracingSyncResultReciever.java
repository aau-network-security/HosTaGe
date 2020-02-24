package de.tudarmstadt.informatik.hostage.sync.tracing;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
 
/**
 * Helper Class to send messages from {@link TracingSyncService} to {@link TracingSyncActivity}.
 * @author Lars Pandikow
 */
public class TracingSyncResultReciever extends ResultReceiver {
    private Receiver mReceiver;
 
    public TracingSyncResultReciever(Handler handler) {
        super(handler);
    }
 
    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }
 
    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);
    }
 
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
}