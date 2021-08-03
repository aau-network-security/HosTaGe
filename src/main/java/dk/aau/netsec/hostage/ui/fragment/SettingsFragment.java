package dk.aau.netsec.hostage.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.logging.PcapStorageManager;
import dk.aau.netsec.hostage.system.Device;
import dk.aau.netsec.hostage.system.PcapWriter;

import static dk.aau.netsec.hostage.system.PcapWriter.on;

import com.google.android.material.snackbar.Snackbar;

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
	Button enable;
	Button stop;
	PcapWriter pcapWriter;

    PcapStorageManager pcapStorageManager;

    Uri mFolderUri;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.inflater = inflater;
        this.container = container;
        this.savedInstanceState = savedInstanceState;
        getActivity().setTitle(getResources().getString(R.string.drawer_settings));
        v = inflater.inflate(R.layout.fragment_settings, container, false);

		TextView rootedText = v.findViewById(R.id.settings_device_rooted);

//        initPcap();
        pcapStorageManager = PcapStorageManager.getPcapStorageManagerInstance(getContext());

		if (Device.isRooted()) {
			rootedText.setText(R.string.yes);
			rootedText.setTextColor(getResources().getColor(R.color.holo_dark_green));
		} else {
			rootedText.setText(R.string.no);
			rootedText.setTextColor(getResources().getColor(R.color.holo_red));
		}

        CheckBox pcapCheckbox = v.findViewById(R.id.pcap_checkbox);
        pcapCheckbox.setChecked(pcapStorageManager.retrievePcapLogSetting());

        Fragment myFragment = this;

        pcapCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pcapCheckbox.isChecked()) {
                    pcapStorageManager.enablePcapLogging(myFragment);
                } else {
                    pcapStorageManager.disablePcapLogging();
                }
            }
        });

        View.OnClickListener filipsClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pcapCheckbox.isChecked()) {
                    pcapStorageManager.disablePcapLogging();
                    pcapCheckbox.setChecked(false);
                } else {
//                    TODO adjust box checked or not

                    pcapStorageManager.enablePcapLogging(myFragment);
                }
            }
        };

        LinearLayout locationSelector = v.findViewById(R.id.location_selector);
        locationSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pcapStorageManager.selectLocation(myFragment);
            }
        });

//        TODO rename here and re-ID in XML
        TextView locationSummary = v.findViewById(R.id.pcap_location_summary);

        String locationSummaryText = pcapStorageManager.getStorageLocationPath();
        if (locationSummaryText != null){
            locationSummary.setText(locationSummaryText);
        }


        LinearLayout pcapSettingView = v.findViewById(R.id.pcap_setting_layout);
        pcapSettingView.setOnClickListener(filipsClickListener);

        return v;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PcapStorageManager.ACTION_PICK_FOLDER_AND_ENABLE) {
            mFolderUri = data.getData();

//            pcapStorageManager.writeTestFile(mFolderUri);


            pcapStorageManager.locationSelected(mFolderUri, true);

            //        TODO rename here and re-ID in XML
            TextView locationSummary = v.findViewById(R.id.pcap_location_summary);

            locationSummary.setText(pcapStorageManager.getStorageLocationPath());


            CheckBox pcapCheckbox = v.findViewById(R.id.pcap_checkbox);
            pcapCheckbox.setChecked(true);
        }
        else if (requestCode == PcapStorageManager.ACTION_PICK_FOLDER){
            mFolderUri = data.getData();

            pcapStorageManager.locationSelected(mFolderUri, false);

            //        TODO rename here and re-ID in XML
            TextView locationSummary = v.findViewById(R.id.pcap_location_summary);
            locationSummary.setText(pcapStorageManager.getStorageLocationPath());

        }
    }

//    private void initPcap() {
//        pcapWriter = new PcapWriter(v);
//        pcapWriter.initializeButtons();
//        enable = pcapWriter.getEnabledButton();
//        stop = pcapWriter.getStopButton();
//    }

	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		manager = getFragmentManager();
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
		if(on)
			enable.setPressed(true);
		else
			stop.setPressed(true);
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
