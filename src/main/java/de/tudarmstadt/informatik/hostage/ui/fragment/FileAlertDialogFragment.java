package de.tudarmstadt.informatik.hostage.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.persistence.ProfileManager;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;


/**
 * Created by Fabio Arnold on 03.03.14.
 * displays details about the current connection
 */
public class FileAlertDialogFragment extends DialogFragment {
    public Dialog onCreateDialog(Bundle savedInstance) {
        // the data we want to display
        String fname= "undefined";
        String ssid = "undefined";
        String protocol = "undefined";
        ProfileManager mProfileManager;
        mProfileManager = ProfileManager.getInstance();




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
        View view = localInflater.inflate(R.layout.fragment_file_alert, null);

        // assign values in layout
        if (view != null) {
            ((TextView)view.findViewById(R.id.FileAlertText)).setText(HelperUtils.fileSHA256);

        }

        // capture the SSID for the button action
        final String filterSSID = ssid;
        final String filterProtocol = protocol;

        // build the actual dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        //  builder.setView(view);
        builder.setTitle("File Injection Alert");
        builder.setMessage("File Injected:" + fname + "\nPath" + HelperUtils.getFilePath() + "\nSHA256:" + HelperUtils.fileSHA256);
        builder.setIcon(android.R.drawable.ic_dialog_info);

        builder.setPositiveButton("SCAN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<String> ssids = new ArrayList<String>();
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
            }
        });
        builder.setNegativeButton(R.string.close, null);

        return builder.create();

    }
}