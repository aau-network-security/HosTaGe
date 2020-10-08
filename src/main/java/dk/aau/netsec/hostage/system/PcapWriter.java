package dk.aau.netsec.hostage.system;
import android.view.View;
import android.widget.Button;
import dk.aau.netsec.hostage.R;
import static dk.aau.netsec.hostage.system.iptablesUtils.Api.remountSystem;
import static dk.aau.netsec.hostage.system.iptablesUtils.Api.runCommand;

public class PcapWriter {
    Button enable;
    Button stop;

    public PcapWriter(View rootView){
        enable = (Button) rootView.findViewById(R.id.enable_pcap);
        stop = (Button) rootView.findViewById(R.id.stop_pcap);
    }

    public void initializeButtons(){
        startButton();
        stopButton();
    }

    private void startButton(){
        enable.setOnClickListener(v -> {
            runTcpdumpPcap();
            enable.setPressed(true);
            stop.setPressed(false);
        });
    }
    private void stopButton(){
        stop.setOnClickListener(v -> {
            stopTcpdumpPcap();
            stop.setPressed(true);
            enable.setPressed(true);
        });
    }

    private static void runTcpdumpPcap(){
        String command = "su -c tcpdump -vv -i any 'dst host 192.168.1.11 and " +
                "(dst port 80 or 7 or 21 or 3306 or 502 or 102 or 161 or 5060 or 22 or 25 or 23 or 443 or 1883 or 5683 or 5672 or 1025)' " +
                "-s 65535 -C 500 -w /sdcard/pcap/save.pcap";
        remountSystem();
        runCommand(command);
    }

    private static void stopTcpdumpPcap(){
        String command ="su -c pkill -SIGINT tcpdump";
        runCommand(command);
    }
}
