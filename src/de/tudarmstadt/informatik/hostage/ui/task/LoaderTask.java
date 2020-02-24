package de.tudarmstadt.informatik.hostage.ui.task;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

/**
 * Created by Julien on 23.03.14.
 */
public class LoaderTask extends AsyncTask<Void, Void, Void> {

    private TaskListener listener;

    @SuppressLint("ValidFragment")
    public interface TaskListener {
        public void doInBackgorund();
        public void onFinish();
    }

    public LoaderTask(TaskListener listener){
        super();
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(Void... unused) {
        if (this.listener != null){
            this.listener.doInBackgorund();
        }
        return(null);
    }

    @Override
    protected void onPostExecute(Void unused) {
        if (this.listener != null){
            this.listener.onFinish();
        }
    }
}