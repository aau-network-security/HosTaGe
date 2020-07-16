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
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;

/**
 * Created by Fabio Arnold on 03.03.14.
 * displays details about the current connection
 */
public class ConnectionInfoDialogFragment extends DialogFragment {
	public Dialog onCreateDialog(Bundle savedInstance) {
		// the data we want to display
		String ssid = "undefined";
		String bssid = "undefined";
		String internalIP = "undefined";
		String externalIP = "undefined";

		// get infos about the current connection using SharedPreferences
		final Activity activity = getActivity();
		if (activity != null) {
			SharedPreferences sharedPreferences = activity.getSharedPreferences(getString(R.string.connection_info), Context.MODE_PRIVATE);
			ssid = sharedPreferences.getString(getString(R.string.connection_info_ssid), "");
			bssid = sharedPreferences.getString(getString(R.string.connection_info_bssid), "");
			internalIP = HelperUtils.inetAddressToString(
					sharedPreferences.getInt(getString(R.string.connection_info_internal_ip), 0));
			externalIP = sharedPreferences.getString(
					getString(R.string.connection_info_external_ip), "");
		}

		// inflate the layout with a dark theme
		Context context = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo);
		LayoutInflater localInflater = getActivity().getLayoutInflater().cloneInContext(context);
		View view = localInflater.inflate(R.layout.fragment_connectioninfo_dialog, null);

		// assign values in layout
		if (view != null) {
			((TextView)view.findViewById(R.id.connectioninfo_ssid_value)).setText(ssid);
			((TextView)view.findViewById(R.id.connectioninfo_bssid_value)).setText(bssid);
			((TextView)view.findViewById(R.id.connectioninfo_internalip_value)).setText(internalIP);
			((TextView)view.findViewById(R.id.connectioninfo_externalip_value)).setText(externalIP);
		}

		// capture the SSID for the button action
		final String filterSSID = ssid;

		// build the actual dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_DARK);
		builder.setView(view);
		builder.setTitle(R.string.title_connection_info);
		builder.setIcon(android.R.drawable.ic_dialog_info);
		builder.setPositiveButton(R.string.show_records, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ArrayList<String> ssids = new ArrayList<String>();
				ssids.add(filterSSID);

				LogFilter filter = new LogFilter();
				filter.setESSIDs(ssids);

				RecordOverviewFragment recordOverviewFragment = new RecordOverviewFragment();
				recordOverviewFragment.setFilter(filter);
				recordOverviewFragment.setGroupKey("ESSID");

				MainActivity.getInstance().injectFragment(recordOverviewFragment);
			}
		});
		builder.setNegativeButton(R.string.close, null);

		return builder.create();
	}
}
