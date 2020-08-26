package dk.aau.netsec.hostage.sync.wifi_direct;

/**
 * Created by Julien on 18.02.2015.
 */
public class TimeoutTask extends BackgroundTask {

    private int seconds;

    public TimeoutTask(int seconds, BackgroundTaskCompletionListener l){
        super(l);
        this.seconds = seconds;
    }

    public String performInBackground(){
        try {
            Thread.sleep(this.seconds * 1000);
        } catch (InterruptedException e){
            // what ever
            return e.getLocalizedMessage();
        }
        return BACKGROUND_TASK_MESSAGE_SUCCESS;
    }
}
