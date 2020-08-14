package de.tudarmstadt.informatik.hostage.sync.wifi_direct;

import android.os.AsyncTask;


/**
 * Created by Julien on 07.01.2015.
 */
public class BackgroundTask extends AsyncTask<Void, Void, String> {

    public static String BACKGROUND_TASK_MESSAGE_SUCCESS = "success";

    /**
     * This listener calls didSucceed if the method performInBackground will return true, otherwise it calls didFail.
     */
    public interface BackgroundTaskCompletionListener {
        void didSucceed();
        void didFail(String errorMessage);
    }

    private boolean isInterrupted;

    private boolean wasSuccessfully;
    private String errorMessage;

    public void interrupt(boolean b){
        this.isInterrupted = b;
    }

    public boolean isInterrupted() {
        return isInterrupted;
    }

    private BackgroundTaskCompletionListener completion_listener;

    public BackgroundTask(BackgroundTaskCompletionListener l){
        super();
        this.completion_listener = l;
    }

    /**
     * Interrupts the background process.
     * @param isInterrupted
     */
    public void setInterrupted(boolean isInterrupted) {
        this.isInterrupted = isInterrupted;
    }


    /**
     * Do any stuff here if it should run in the background. It should return true or false if the process was successfully.
     * @return String error message, default is BACKGROUND_TASK_MESSAGE_SUCCESS
     */
    public String performInBackground(){
        return BACKGROUND_TASK_MESSAGE_SUCCESS;
    }

    @Override
    protected String doInBackground(Void... params) {
        String message = this.performInBackground();
        this.wasSuccessfully = message == null || message.equals(BACKGROUND_TASK_MESSAGE_SUCCESS);
        this.errorMessage = message;
        return message;
    }

    /*
     * (non-Javadoc)
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(String result) {
        if (this.completion_listener != null) {
            if (this.wasSuccessfully) {
                this.completion_listener.didSucceed();
            }else {
                this.completion_listener.didFail(this.errorMessage);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see android.os.AsyncTask#onPreExecute()
     */
    @Override
    protected void onPreExecute() {


    }

}