package de.tudarmstadt.informatik.hostage.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Set;

import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import virustotalapi.ReportScan;
import virustotalapi.VirusTotal;


public class ScanFileFragment extends Fragment {
    public static String filePath;
    public String result;
    private View rootView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final Activity activity = getActivity();
        if (activity != null) {
            activity.setTitle("Scan File");
        }

        rootView = inflater.inflate(R.layout.fragment_scan_file, container, false);
       // PackageManager manager = Hostage.getContext().getPackageManager();
        //PackageInfo info = null;
        /*try {
            info = manager.getPackageInfo(Hostage.getContext().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }*/

        TextView scanResult = rootView.findViewById(R.id.scanResult);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            scanResult.setText(scanFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //  scanfile(HelperUtils.filePath);
      //  result=getFileScanReport();

//        TextView version = (TextView) rootView.findViewById(R.id.hostageVersion);

        /*version.setText("ver. "+versionApp);
        hostage.setMovementMethod(LinkMovementMethod.getInstance());
        version.setMovementMethod(LinkMovementMethod.getInstance());
*/
        return rootView;
    }

  //  public String scanner;
  //  public String path= HelperUtils.getFilePath();

    public String scanFile() throws IOException {

        StringBuilder sb = new StringBuilder();

        VirusTotal VT = new VirusTotal("111c226204f5de7228563bbca91c5860e4965fbe936307dffa8f2f2d575ff292"); // Your Virus Total API Key

        Set<ReportScan> Report = VT.ReportScan(HelperUtils.fileSHA256); //The SHA256 file

        for (ReportScan report : Report) {
            if (report.getDetected().contentEquals("true")) {
                sb.append("\nAV: ").append(report.getVendor()).append(" Detected: ").append(report.getDetected()).append(" Update: ").append(report.getUpdate()).append(" Malware Name: ").append(report.getMalwarename());
            }
        }
        return sb.toString();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(rootView!=null) {
            unbindDrawables(rootView);
            rootView=null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(rootView!=null) {
            unbindDrawables(rootView);
            rootView=null;
        }
    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

}
