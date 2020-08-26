package dk.aau.netsec.hostage.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import java.util.ArrayList;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.ui.activity.MainActivity;
import dk.aau.netsec.hostage.ui.model.LogFilter;

/**
 * Created by Fabio Arnold on 03.03.14.
 * displays details about the current connection
 */
public class FileAlertDialogFragment extends DialogFragment {
    private View view;
    public Dialog onCreateDialog(Bundle savedInstance) {
        // the data we want to display
        String fname= "undefined";
        String ssid = "undefined";
        String protocol = "undefined";

        // get infos about the current connection using SharedPreferences
        final Activity activity = getActivity();
        if (activity != null) {
            fname = HelperUtils.getFileName();
            SharedPreferences sharedPreferences = activity.getSharedPreferences(getString(R.string.connection_info), Context.MODE_PRIVATE);
            ssid = sharedPreferences.getString(getString(R.string.connection_info_ssid), "");
            protocol = "SMB";
        }

        // inflate the layout with a dark theme
        Context context = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo);
        LayoutInflater localInflater = getActivity().getLayoutInflater().cloneInContext(context);
        view = localInflater.inflate(R.layout.fragment_file_alert, null);

        // assign values in layout
        if (view != null) {
            ((TextView)view.findViewById(R.id.FileAlertText)).setText(HelperUtils.fileSHA256);

        }

        // capture the SSID for the button action
        final String filterProtocol = protocol;

        // build the actual dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        //  builder.setView(view);
        builder.setTitle("File Injection Alert");
        builder.setMessage("File Injected:" + fname + "\nPath" + HelperUtils.getFilePath() + "\nSHA256:" + HelperUtils.fileSHA256);
        builder.setIcon(android.R.drawable.ic_dialog_info);

        builder.setPositiveButton("SCAN", (dialog, which) -> {
            ArrayList<String> ssids = new ArrayList<>();
            ssids.add(filterProtocol);
            LogFilter filter = new LogFilter();
            filter.setESSIDs(ssids);
           // ScanFileFragment.path = HelperUtils.getFilePath();
            ScanFileFragment scanFileFragment = new ScanFileFragment();
           // scanFileFragment.scanfile(HelperUtils.getFilePath());
            MainActivity.getInstance().injectFragment(scanFileFragment);

            /*RecordOverviewFragment recordOverviewFragment = new RecordOverviewFragment();
            recordOverviewFragment.setFilter(filter);
            recordOverviewFragment.setGroupKey("Protocol");
            MainActivity.getInstance().injectFragment(recordOverviewFragment);*/
        });
        builder.setNegativeButton(R.string.close, null);

        return builder.create();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(view!=null) {
            unbindDrawables(view);
            view=null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(view!=null) {
            unbindDrawables(view);
            view=null;
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