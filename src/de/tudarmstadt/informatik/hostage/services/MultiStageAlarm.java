package de.tudarmstadt.informatik.hostage.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.widget.Toast;

import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.Listener;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

/**
 * Alarm to call multistage service
 */
public class MultiStageAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Toast.makeText(MainActivity.getInstance().getApplicationContext(),"Scanning for MultiStage Attacks...",Toast.LENGTH_SHORT).show();
        Intent i = new Intent(context, MultiStage.class);
        context.startService(i);

    }

    public void SetAlarm(Context context)
    {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, MultiStageAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 1, pi); // Millisec * Second * Minute

    }

    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, MultiStageAlarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }



}

