package de.tudarmstadt.informatik.hostage.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Alarm to call multistage service
 */
public class MultiStageAlarm extends BroadcastReceiver {

    /**
     * Calls Mutlistage service when receives an attack.
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        //Toast.makeText(MainActivity.getInstance().getApplicationContext(),"Scanning for MultiStage Attacks...",Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, MultiStage.class));
        } else {
            context.startService(new Intent(context, MultiStage.class));
        }
    }

    public void SetAlarm(Context context) {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, MultiStageAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 1, pi); // Millisec * Second * Minute

    }

    public void CancelAlarm(Context context) {
        Intent intent = new Intent(context, MultiStageAlarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }



}

