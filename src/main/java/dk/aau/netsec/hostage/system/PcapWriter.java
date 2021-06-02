package dk.aau.netsec.hostage.system;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.commons.HelperUtils;

import static dk.aau.netsec.hostage.system.iptablesUtils.Api.remountSystem;
import static dk.aau.netsec.hostage.system.iptablesUtils.Api.runCommand;

public class PcapWriter {
   private Button enable;
   private Button stop;
   private static String  internalIP;
   public static boolean on = false;

    public PcapWriter(View rootView){
        enable = (Button) rootView.findViewById(R.id.enable_pcap);
        stop = (Button) rootView.findViewById(R.id.stop_pcap);
        SharedPreferences sharedPreferences = Hostage.getContext().getSharedPreferences(Hostage.getContext().getString(R.string.connection_info), Context.MODE_PRIVATE);
        internalIP = HelperUtils.inetAddressToString(sharedPreferences.getInt(Hostage.getContext().getString(R.string.connection_info_internal_ip), 0));
    }

    public void initializeButtons(){
        stayPressedEnable();
        stayPressedStop();
    }

    private void startButton() {
        enable.setOnClickListener(v -> {
            PcapCapture pcapCapture = new PcapCapture();
            pcapCapture.execute();

        });

    }
    @SuppressLint("ClickableViewAccessibility")
    private void stayPressedEnable(){
        startButton();

        enable.setOnTouchListener((view, motionEvent) -> {
            enable.setPressed(true);
            stop.setPressed(false);
            on = true;
            return true;
        });

        enable.performClick();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void stayPressedStop(){
        stopButton();

        stop.setOnTouchListener((view, motionEvent) -> {
            enable.setPressed(false);
            stop.setPressed(true);
            on = false;
            return true;
        });
      stop.performClick();
    }
    private void stopButton(){
        stop.setOnClickListener(v -> {
            stopTcpdumpPcap();
        });
    }

    public Button getEnabledButton(){
        return enable;
    }

    public Button getStopButton(){
        return stop;
    }

    private static void runTcpdumpPcap(){

                //String command = "su -c tcpdump -vv -i any 'dst host "+internalIP+ " and (dst port 80 or 7 or 21 or 3306 or 502 or 102 or 161 or 5060 or 22 or 25 or 23 or 443 or 1883 or 5683 or 5672 or 1025)' -s 65535 -C 900 -w /sdcard/sdcard/pcap/save.pcap";
        String fileName = new SimpleDateFormat("yyyyMMddHHmmss'.pcap'").format(new Date());
        String command = "su -c tcpdump -i any -s 65535 -w /sdcard/"+fileName;

        try {
            //remountSystem();
            runCommand(command);
            Log.d("DEBUG", "PCAP Writer Started");

        } catch (Exception pcape) {
            pcape.printStackTrace();
            Log.d("ERROR", "PCAP Error:"+pcape);
        }

    }

    private static void stopTcpdumpPcap(){
        String command ="su -c pkill -SIGINT tcpdump";
        runCommand(command);
    }

    /**
     * Make pcap capturing as an AsyncTask so the app won't crash
     */
    private static class PcapCapture extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            runTcpdumpPcap();
            return null;
        }
    }
}
