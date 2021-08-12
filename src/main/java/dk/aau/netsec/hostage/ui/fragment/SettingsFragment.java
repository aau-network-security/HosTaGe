package dk.aau.netsec.hostage.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.google.android.material.switchmaterial.SwitchMaterial;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.logging.PcapStorageManager;
import dk.aau.netsec.hostage.system.Device;

//TODO extract strings from here and from related XML file

/**
 * @author Alexander Brakowski
 * @created 24.02.14 23:37
 * @modified Shreyas Srinivasa, Filip Adamik
 */
public class SettingsFragment extends UpNavigatibleFragment {
    private View v;
    private Bundle savedInstanceState;
    private PcapStorageManager mPcapStorageManager;
    private FragmentManager manager;
    private Uri mFolderUri;
    private SwitchMaterial pcapSwitch;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        getActivity().setTitle(getResources().getString(R.string.drawer_settings));

        v = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView rootedText = v.findViewById(R.id.settings_device_rooted);

        if (Device.isRooted()) {
            rootedText.setText(R.string.yes);
            rootedText.setTextColor(getResources().getColor(R.color.holo_dark_green));

            mPcapStorageManager = PcapStorageManager.getPcapStorageManagerInstance(getContext());

            initialiseLoggingSwitch();
            initialiseLocationSelector();
            initialiseRotationPeriodSelector();

        } else {
            rootedText.setText(R.string.no);
            rootedText.setTextColor(getResources().getColor(R.color.holo_red));

            disablePcapSettings();
        }

        return v;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        manager = getFragmentManager();
        manager.beginTransaction().replace(R.id.settings_fragment_container, new PreferenceHostageFragment()).commit();
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
    private void initialiseLoggingSwitch() {
        pcapSwitch = v.findViewById(R.id.pcap_checkbox);

        // React to clicks on the Switch
        pcapSwitch.setOnClickListener((View v) -> {
            if (pcapSwitch.isChecked()) {
                mPcapStorageManager.enablePcapLogging(this);
            } else {
                mPcapStorageManager.disablePcapLogging();
            }
        });

        // React to clicks on the entire row
        LinearLayout pcapSettingView = v.findViewById(R.id.pcap_setting_layout);
        pcapSettingView.setOnClickListener((View v) -> {
            if (pcapSwitch.isChecked()) {
                mPcapStorageManager.disablePcapLogging();
                pcapSwitch.setChecked(false);
            } else {
//                    TODO adjust box checked or not
                mPcapStorageManager.enablePcapLogging(this);
            }
        });

        setPcapChecked();
    }

    /**
     * TODO write javadoc
     */
    private void initialiseLocationSelector() {
        LinearLayout locationSelector = v.findViewById(R.id.pcap_location_preference);
        locationSelector.setOnClickListener((View v) -> {
            mPcapStorageManager.selectLocation(this);
        });

        setLocationSummaryText();
    }

    /**
     * TODO write javadoc
     */
    private void initialiseRotationPeriodSelector() {
        LinearLayout logRotationSelector = v.findViewById(R.id.pcap_log_rotation_preference);
        logRotationSelector.setOnClickListener((View v) -> {
            showLogRotationSelectionDialog();
        });

        setLogRotationPeriod();
    }

    /**
     * TODO write javadoc
     */
    private void showLogRotationSelectionDialog() {
        int[] durations = {10, 30, 60, 90, 180};
        String[] durationItems = new String[durations.length];

        for (int i = 0; i < durations.length; i++) {
            durationItems[i] = Integer.toString(durations[i]) + " seconds";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select log rotation period");
        builder.setItems(durationItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPcapStorageManager.logRotationPeriodSelected(durations[which]);

                setLogRotationPeriod();
            }
        });
        builder.show();
    }

    /**
     * TODO write javadoc
     */
    public void setLogRotationPeriod() {
        int period = mPcapStorageManager.getLogRotationPeriod();

        TextView periodView = v.findViewById(R.id.pcap_log_rotation_summary);
        periodView.setText(Integer.toString(period) + " seconds");
    }

    /**
     * TODO write javadoc
     */
    public void setPcapChecked() {
        pcapSwitch.setChecked(mPcapStorageManager.isPcapLogEnabled());
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

    /**
     * TODO write javadoc
     */
    private void disablePcapSettings() {
        pcapSwitch = v.findViewById(R.id.pcap_checkbox);
        pcapSwitch.setClickable(false);

        TextView pcapToggleText = v.findViewById(R.id.pcap_log_toggle);
        pcapToggleText.setEnabled(false);

        TextView pcapToggleSummary = v.findViewById(R.id.pcap_log_summary);
        pcapToggleSummary.setText("PCAP logging is only available on rooted devices");

        LinearLayout locationPreference = v.findViewById(R.id.pcap_location_preference);
        locationPreference.setVisibility(View.GONE);

        LinearLayout rotationPreference = v.findViewById(R.id.pcap_log_rotation_preference);
        rotationPreference.setVisibility(View.GONE);
    }

    private void removeSettingsFragment() {
        Fragment fragment = manager.findFragmentById(R.id.settings_fragment_container);
        FragmentTransaction fragmentTransaction = manager.beginTransaction();
        fragmentTransaction.remove(fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
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
