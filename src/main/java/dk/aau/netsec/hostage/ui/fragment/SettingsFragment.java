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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.switchmaterial.SwitchMaterial;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.logging.PcapLoggingManager;
import dk.aau.netsec.hostage.system.Device;

/**
 * @author Alexander Brakowski
 * @created 24.02.14 23:37
 * @modified Shreyas Srinivasa, Filip Adamik
 */
public class SettingsFragment extends UpNavigatibleFragment {
    private View v;
    private Bundle savedInstanceState;
    private PcapLoggingManager mPcapLoggingManager;
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

            mPcapLoggingManager = PcapLoggingManager.getPcapLoggingManagerInstance(getContext());

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
     * This method is called when the user has selected the PCAP log output folder and has returned
     * to the Settings fragment.
     *
     * If the folder picker dialog was launched after a user requested to activate PCAP logging,
     * but the output location has not been selected or is not writable, this method updates the UI
     * and requests the {@link PcapLoggingManager} to start PCAP logging.
     *
     * If the folder picker dialog was launched by user clicking on the PCAP Output Location setting,
     * update the UI and pass the location to {@link PcapLoggingManager}, without turning the PCAP
     * logging on or off.
     *
     * @param requestCode Request code indicating what request is being completed
     * @param resultCode Not used
     * @param data Data including the Uri of the output folder selected by the user
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PcapLoggingManager.ACTION_PICK_FOLDER_AND_ENABLE) {
            mFolderUri = data.getData();
            mPcapLoggingManager.locationSelected(mFolderUri, true);

            setLocationSummaryText();
            setPcapChecked();

        } else if (requestCode == PcapLoggingManager.ACTION_PICK_FOLDER) {
            mFolderUri = data.getData();
            mPcapLoggingManager.locationSelected(mFolderUri, false);

            setLocationSummaryText();
        }
    }

    /**
     * Initialise the main PCAP logging switch. Assign on-click listeners to the Switch and to the
     * entire setting row.
     */
    private void initialiseLoggingSwitch() {
        pcapSwitch = v.findViewById(R.id.pcap_switch);

        // React to clicks on the Switch
        pcapSwitch.setOnClickListener((View v) -> {
            if (pcapSwitch.isChecked()) {
                mPcapLoggingManager.enablePcapLogging(this);
            } else {
                mPcapLoggingManager.disablePcapLogging();
            }
        });

        // React to clicks on the entire row
        LinearLayout pcapSettingView = v.findViewById(R.id.pcap_setting_layout);
        pcapSettingView.setOnClickListener((View v) -> {
            if (pcapSwitch.isChecked()) {
                mPcapLoggingManager.disablePcapLogging();
                pcapSwitch.setChecked(false);
            } else {
                mPcapLoggingManager.enablePcapLogging(this);
            }
        });

        setPcapChecked();
    }

    /**
     * Initialise the location setting by assinging an on-click listener and updating UI with
     * the location value from {@link PcapLoggingManager}
     */
    private void initialiseLocationSelector() {
        LinearLayout locationSelector = v.findViewById(R.id.pcap_location_preference);
        locationSelector.setOnClickListener((View v) -> {
            mPcapLoggingManager.selectLocation(this);
        });

        setLocationSummaryText();
    }

    /**
     * Initialise log rotation setting by assinging an on-click listener and updating UI with
     * the value from {@link PcapLoggingManager}
     */
    private void initialiseRotationPeriodSelector() {
        LinearLayout logRotationSelector = v.findViewById(R.id.pcap_log_rotation_preference);
        logRotationSelector.setOnClickListener((View v) -> {
            showLogRotationSelectionDialog();
        });

        setLogRotationPeriod();
    }

    /**
     * Show dialog prompting the user to select log rotation period from a list of pre-defined
     * options
     * <p>
     * Notify {@link PcapLoggingManager} of the result and update UI accordingly
     */
    private void showLogRotationSelectionDialog() {
        int[] durations = PcapLoggingManager.PCAP_LOG_DURATION_OPTIONS;

        // Construct a string array for AlertDialog builder
        String[] durationItems = new String[durations.length];
        for (int i = 0; i < durations.length; i++) {
            durationItems[i] = Integer.toString(durations[i]) + " " + getString(R.string.seconds);
        }

        // Create and display the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.pcap_log_rotation_dialog);
        builder.setItems(durationItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPcapLoggingManager.logRotationPeriodSelected(durations[which]);

                setLogRotationPeriod();
            }
        });
        builder.show();
    }

    /**
     * Set switch in checked or un-checked state, based on the value retrieved from
     * {@link PcapLoggingManager}
     */
    public void setPcapChecked() {
        pcapSwitch.setChecked(mPcapLoggingManager.isPcapLogEnabled());
    }

    /**
     * Retrieve user-friendly location path from {@link PcapLoggingManager} and display it in
     * the location setting summary.
     */
    private void setLocationSummaryText() {
        String locationSummaryText = mPcapLoggingManager.getOutputLocationPath();

        if (locationSummaryText != null) {
            TextView locationSummary = v.findViewById(R.id.pcap_location_summary);
            locationSummary.setText(locationSummaryText);
        }
    }

    /**
     * Retrieve the log rotation period from {@link PcapLoggingManager} and display it in the
     * Log Rotation setting summary.
     */
    public void setLogRotationPeriod() {
        int period = mPcapLoggingManager.getLogRotationPeriod();

        TextView periodView = v.findViewById(R.id.pcap_log_rotation_summary);
        periodView.setText(Integer.toString(period) + " " + getString(R.string.seconds));
    }

    /**
     * Disable PCAP settings on non-rooted devices and display a 'not available' message
     */
    private void disablePcapSettings() {
        pcapSwitch = v.findViewById(R.id.pcap_switch);
        pcapSwitch.setClickable(false);

        TextView pcapToggleText = v.findViewById(R.id.pcap_log_toggle);
        pcapToggleText.setEnabled(false);

        TextView pcapToggleSummary = v.findViewById(R.id.pcap_log_summary);
        pcapToggleSummary.setText(R.string.pcap_not_available);

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
