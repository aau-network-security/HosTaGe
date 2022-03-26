package dk.aau.netsec.hostage.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.ui.activity.MainActivity;
import dk.aau.netsec.hostage.ui.model.LogFilter;

/**
 * Created by Fabio Arnold on 03.03.14.
 * displays details about the current connection
 */
public class ConnectionInfoDialogFragment extends DialogFragment {
	private View view;

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
		view = localInflater.inflate(R.layout.fragment_connectioninfo_dialog, null);

		// assign values in layout
		if (view != null) {
			((TextView)view.findViewById(R.id.connectioninfo_ssid_value)).setText(ssid);
			((TextView)view.findViewById(R.id.connectioninfo_bssid_value)).setText(bssid);
			((TextView)view.findViewById(R.id.connectioninfo_internalip_value)).setText(internalIP);
			((TextView)view.findViewById(R.id.connectioninfo_externalip_value)).setText(externalIP);
		}


		return getConnectionInfoDialog(ssid).create();
	}

	private MaterialAlertDialogBuilder getConnectionInfoDialog(String ssid){
		// capture the SSID for the button action
		final String filterSSID = ssid;
		// build the actual dialog
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());

		builder.setView(view);
		builder.setTitle(R.string.title_connection_info);
		builder.setIcon(getResources().getDrawable(R.drawable.ic_info_dark_grey_icon));
		builder.setPositiveButton(R.string.show_records, (dialog, which) -> {
			showRecords(filterSSID);
		});
		builder.setNegativeButton(R.string.close, null);

		return builder;
	}

	private void showRecords(String filterSSID){
		ArrayList<String> ssids = new ArrayList<>();
		ssids.add(filterSSID);

		LogFilter filter = new LogFilter();
		filter.setESSIDs(ssids);

		RecordOverviewFragment recordOverviewFragment = new RecordOverviewFragment();
		recordOverviewFragment.setFilter(filter);
		recordOverviewFragment.setGroupKey("ESSID");
		MainActivity.getInstance().injectFragment(recordOverviewFragment);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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
