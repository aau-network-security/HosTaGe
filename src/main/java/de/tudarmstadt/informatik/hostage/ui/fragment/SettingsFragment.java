package de.tudarmstadt.informatik.hostage.ui.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.system.Device;

/**
 * @author Alexander Brakowski
 * @created 24.02.14 23:37
 * @modified Shreyas Srinivasa
 */
public class SettingsFragment extends UpNavigatibleFragment {
	private View v;
	private LayoutInflater inflater;
	private ViewGroup container;
	private Bundle savedInstanceState;
	FragmentManager manager;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		this.inflater = inflater;
		this.container= container;
		this.savedInstanceState = savedInstanceState;
		getActivity().setTitle(getResources().getString(R.string.drawer_settings));
		v = inflater.inflate(R.layout.fragment_settings, container, false);

		TextView rootedText = v.findViewById(R.id.settings_device_rooted);
		TextView iptablesText = v.findViewById(R.id.settings_iptables_available);

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

		return v;
	}

	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		manager = this.getFragmentManager();
		manager.beginTransaction().replace(R.id.settings_fragment_container, new PreferenceHostageFragment()).commit();
	}

	private void removeSettingsFragment(){
		Fragment fragment = manager.findFragmentById(R.id.settings_fragment_container);
		FragmentTransaction fragmentTransaction = manager.beginTransaction();
		fragmentTransaction.remove(fragment);
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(v!=null){
			unbindDrawables(v);
			v=null;
			removeSettingsFragment();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		onCreateView(inflater,container,savedInstanceState);
		manager.beginTransaction().replace(R.id.settings_fragment_container, new PreferenceHostageFragment()).commit();
	}

	@Override
	public void onStop() {
		super.onStop();
		if(v!=null) {
			unbindDrawables(v);
			v = null;
			removeSettingsFragment();
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
