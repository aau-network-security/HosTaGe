package dk.aau.netsec.hostage.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.logging.PcapStorageManager;
import dk.aau.netsec.hostage.system.Device;

/**
 * @author Alexander Brakowski
 * @created 24.02.14 23:37
 * @modified Shreyas Srinivasa
 */
public class SettingsFragment extends UpNavigatibleFragment {
    private View v;
    private Bundle savedInstanceState;
    private PcapStorageManager mPcapStorageManager;
    private FragmentManager manager;
    private Uri mFolderUri;
    private CheckBox pcapCheckbox;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        getActivity().setTitle(getResources().getString(R.string.drawer_settings));

        v = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView rootedText = v.findViewById(R.id.settings_device_rooted);

        if (Device.isRooted()) {
            rootedText.setText(R.string.yes);
            rootedText.setTextColor(getResources().getColor(R.color.holo_dark_green));
        } else {
            rootedText.setText(R.string.no);
            rootedText.setTextColor(getResources().getColor(R.color.holo_red));
        }

        mPcapStorageManager = PcapStorageManager.getPcapStorageManagerInstance(getContext());

        pcapCheckbox = v.findViewById(R.id.pcap_checkbox);
        setPcapChecked();

        pcapCheckbox.setOnClickListener((View v) -> {
            if (pcapCheckbox.isChecked()) {
                mPcapStorageManager.enablePcapLogging(this);
            } else {
                mPcapStorageManager.disablePcapLogging();
            }
        });

        LinearLayout pcapSettingView = v.findViewById(R.id.pcap_setting_layout);

        pcapSettingView.setOnClickListener((View v) -> {
            if (pcapCheckbox.isChecked()) {
                mPcapStorageManager.disablePcapLogging();
                pcapCheckbox.setChecked(false);
            } else {
//                    TODO adjust box checked or not
                mPcapStorageManager.enablePcapLogging(this);
            }
        });

        LinearLayout locationSelector = v.findViewById(R.id.pcap_location_preference);
        locationSelector.setOnClickListener((View v) -> {
            mPcapStorageManager.selectLocation(this);
        });

        setLocationSummaryText();

        return v;
    }

    /**
     * TODO write javadoc
     */
    public void setPcapChecked(){
        pcapCheckbox.setChecked(mPcapStorageManager.isPcapLogEnabled());
    }

    /**
     * TODO write javadoc
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PcapStorageManager.ACTION_PICK_FOLDER_AND_ENABLE) {
            mFolderUri = data.getData();

//            mPcapStorageManager.writeTestFile(mFolderUri);

            mPcapStorageManager.locationSelected(mFolderUri, true);

            setLocationSummaryText();

            CheckBox pcapCheckbox = v.findViewById(R.id.pcap_checkbox);
            pcapCheckbox.setChecked(true);
        } else if (requestCode == PcapStorageManager.ACTION_PICK_FOLDER) {
            mFolderUri = data.getData();

            mPcapStorageManager.locationSelected(mFolderUri, false);

            setLocationSummaryText();
        }
    }

    /**
     * TODO write javadoc
     */
    private void setLocationSummaryText() {
        TextView locationSummary = v.findViewById(R.id.pcap_location_summary);

        String locationSummaryText = mPcapStorageManager.getStorageLocationPath();

        if (locationSummaryText != null) {
            locationSummary.setText(locationSummaryText);
        }
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        manager = getFragmentManager();
        manager.beginTransaction().replace(R.id.settings_fragment_container, new PreferenceHostageFragment()).commit();
    }

    private void removeSettingsFragment() {
        Fragment fragment = manager.findFragmentById(R.id.settings_fragment_container);
        FragmentTransaction fragmentTransaction = manager.beginTransaction();
        fragmentTransaction.remove(fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (v != null) {
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
