package de.tudarmstadt.informatik.hostage.ui.fragment;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.system.Device;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;


/**
 * @author Alexander Brakowski
 * @created 24.02.14 23:37
 * @modified Shreyas Srinivasa
 */
public class SettingsFragment extends UpNavigatibleFragment {
	private TextView mPorthackText;
	private Button mPorthackInstallButton;
	private Button mPorthackUninstallButton;
	private Button MultiStageEnabled;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		getActivity().setTitle(getResources().getString(R.string.drawer_settings));
		View v = inflater.inflate(R.layout.fragment_settings, container, false);

		TextView rootedText = v.findViewById(R.id.settings_device_rooted);
		TextView iptablesText = v.findViewById(R.id.settings_iptables_available);
		mPorthackText = v.findViewById(R.id.settings_porthack_installed);
		mPorthackInstallButton = v.findViewById(R.id.settings_deploy_porthack);
		mPorthackUninstallButton = v.findViewById(R.id.settings_uninstall_porthack);

		if (Device.isRooted()) {
			rootedText.setText(R.string.yes);
			rootedText.setTextColor(getResources().getColor(R.color.holo_dark_green));
		} else {
			rootedText.setText(R.string.no);
			rootedText.setTextColor(getResources().getColor(R.color.holo_red));
		}

		if (Device.isPortRedirectionAvailable()) {
			iptablesText.setText(R.string.yes);
			iptablesText.setTextColor(getResources().getColor(R.color.holo_dark_green));
		} else {
			iptablesText.setText(R.string.no);
			iptablesText.setTextColor(getResources().getColor(R.color.holo_red));
		}

		updatePorthackStatus();

		mPorthackInstallButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Device.deployPorthack();
				updatePorthackStatus();
			}
		});
		mPorthackUninstallButton.setOnClickListener(new View.OnClickListener() {
			@Override
		public void onClick(View v) {
				Device.uninstallPorthack();
				updatePorthackStatus();
			}
		});

		v.findViewById(R.id.porthack_info_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance());
				builder.setMessage(Html.fromHtml(getString(R.string.porthack_explanation)));
				AlertDialog alert = builder.create();
				alert.show();
			}
		});

		return v;
	}

	private void updatePorthackStatus() {
		Device.checkCapabilities(); // get current situation

		if (Device.isPorthackInstalled()) {
			mPorthackText.setText(R.string.yes);
			mPorthackText.setTextColor(getResources().getColor(R.color.holo_dark_green));
			mPorthackInstallButton.setEnabled(false); // we're only able to deploy if the device is rooted
			mPorthackInstallButton.setVisibility(View.GONE);
			mPorthackUninstallButton.setEnabled(true);
			mPorthackUninstallButton.setVisibility(View.VISIBLE);
		} else {
			mPorthackText.setText(R.string.no);
			mPorthackText.setTextColor(getResources().getColor(R.color.holo_red));
			// we're only able to deploy if the device is rooted
			mPorthackInstallButton.setEnabled(Device.isRooted());
			mPorthackInstallButton.setVisibility(Device.isRooted() ? View.VISIBLE : View.GONE);
			mPorthackUninstallButton.setEnabled(false);
			mPorthackUninstallButton.setVisibility(View.GONE);
		}
	}

	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		FragmentManager manager = this.getFragmentManager();
		manager.beginTransaction().replace(R.id.settings_fragment_container, new PreferenceHostageFragment()).commit();
	}



}
