package dk.aau.netsec.hostage.ui.fragment;

import static dk.aau.netsec.hostage.location.CustomLocationManager.LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE;
import static dk.aau.netsec.hostage.location.CustomLocationManager.LOCATION_PERMISSION_REQUEST_CODE;
import static dk.aau.netsec.hostage.ui.fragment.opengl.ThreatIndicatorGLRenderer.ThreatLevel.LIVE_THREAT;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.location.CustomLocationManager;
import dk.aau.netsec.hostage.location.LocationException;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.model.Profile;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.persistence.ProfileManager;
import dk.aau.netsec.hostage.ui.activity.MainActivity;
import dk.aau.netsec.hostage.ui.fragment.opengl.ThreatIndicatorGLRenderer;
import dk.aau.netsec.hostage.ui.model.LogFilter;


/**
 * @author Alexander Brakowski
 * @created 13.01.14 19:06
 */

//TODO refactor and streamline this fragment, it's messy
public class HomeFragment extends Fragment {
    private SwitchMaterial mHomeSwitchConnection;
    private TextView mHomeTextName;
    private TextView mHomeTextSecurity;
    private TextView mHomeTextAttacks;
    private TextView mHomeTextProfile;
    private TextView mHomeTextProfileHeader;
    private ImageView mHomeProfileImage;
    private ImageView mHomeConnectionInfoButton;
    private ImageView mHomeAndroidImage;
    private View mRootView;

    private BroadcastReceiver mReceiver;

    private int mDefaultTextColor;
    private ProfileManager mProfileManager;
    private SharedPreferences mConnectionInfo;

    private DAOHelper daoHelper;

    private CustomLocationManager customLocationManager;
    private List<String> protocols;
    private boolean protocolActivated;

    private boolean mReceiverRegistered;
    private final boolean mRestoredFromSaved = false;
    private boolean isActive = false;
    private boolean isConnected = false;
    private static boolean updatedImageView = false;

    private Thread updateUIThread;
    private static ThreatIndicatorGLRenderer.ThreatLevel mThreatLevel = ThreatIndicatorGLRenderer.ThreatLevel.NOT_MONITORING;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final AppCompatActivity activity = MainActivity.getInstance();
        if (isAdded() && activity != null) {
            activity.setTitle(getActivity().getString(R.string.drawer_overview));
        }

        DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
        daoHelper = new DAOHelper(dbSession, getActivity());


        mConnectionInfo = getActivity().getSharedPreferences(getActivity().getString(R.string.connection_info), Context.MODE_PRIVATE);

        mProfileManager = ProfileManager.getInstance();

        mRootView = inflater.inflate(R.layout.fragment_home, container, false);

        mHomeSwitchConnection = mRootView.findViewById(R.id.home_switch_connection);
        mHomeTextName = mRootView.findViewById(R.id.home_text_name);
        mHomeTextSecurity = mRootView.findViewById(R.id.home_text_security);
        mHomeTextAttacks = mRootView.findViewById(R.id.home_text_attacks);
        mHomeTextProfile = mRootView.findViewById(R.id.home_text_profile);
        mHomeTextProfileHeader = mRootView.findViewById(R.id.home_text_profile_header);
        mHomeProfileImage = mRootView.findViewById(R.id.home_image_profile);
        mHomeConnectionInfoButton = mRootView.findViewById(R.id.home_button_connection_info);
//        assignViews();

        //addAndroidIcon();
        addThreatAnimation();

        addConnectionInfoButton();

        mDefaultTextColor = mHomeTextName.getCurrentTextColor();

        setStateNotActive(true);
        setStateNotConnected();

        addBroadcastReceiver();

//        mHomeSwitchConnection.setSaveEnabled(false);
        updateUI();

        mHomeSwitchConnection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { // switch activated
                if (!HelperUtils.isNetworkAvailable(getActivity())) {
                    noConnectionAlertDialog();
                } else { // network available
                    protocolActivated = false;

                    if (ProfileManager.getInstance().getCurrentActivatedProfile() == null) {
                        MainActivity.getInstance().startMonitorServices(Arrays.asList(
                                getResources().getStringArray(R.array.protocols)));
                    } else {
                        ProfileManager profileManager = ProfileManager.getInstance();

                        if (profileManager.isRandomActive()) {
                            profileManager.randomizeProtocols(profileManager.getRandomProfile());
                        }
                        Profile currentProfile = profileManager.getCurrentActivatedProfile();
                        protocols = currentProfile.getActiveProtocols();
                        if (protocols.size() > 0) {
                            if (arePermissionsReady()) {
                                startMonitoring();
                            }
                            ;
                        } else {
                            noServicesAlertDialog();

                        }
                    }
                }
            } else { // switch deactivated
                if (MainActivity.getInstance().getHostageService() != null) {
                    MainActivity.getInstance().getHostageService().stopListeners();
                    MainActivity.getInstance().stopAndUnbind();

                    try {
                        customLocationManager = CustomLocationManager.getLocationManagerInstance(getContext());
                        customLocationManager.keepLocationUpdated(false);
                    } catch (LocationException le) {
                        le.printStackTrace();
                    }
                }
                initAsNotActive();
            }
        });


        mRootView.findViewById(R.id.home_profile_details).setOnClickListener(v -> {
            Fragment fragment = new ProfileManagerFragment();
            MainActivity.getInstance().injectFragment(fragment);
        });

        View.OnClickListener attackClickListener = v -> loadAttackListener();

        mHomeTextAttacks.setOnClickListener(attackClickListener);
        mHomeTextSecurity.setOnClickListener(attackClickListener);

        return mRootView;
    }

    private void registerBroadcastReceiver() {
        if (!mReceiverRegistered) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, new IntentFilter(getActivity().getString(R.string.broadcast)));
            this.mReceiverRegistered = true;
        }
    }

    private void unregisterBroadcastReceiver() {
        if (mReceiverRegistered) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
            this.mReceiverRegistered = false;
        }
    }

    public void setStateNotActive(boolean initial) {
//        TODO adapt colour theming
//        mHomeTextName.setTextColor(getActivity().getColor(R.color.light_grey));
//        mHomeTextSecurity.setTextColor(getActivity().getColor(R.color.light_grey));
//        mHomeTextAttacks.setTextColor(getActivity().getColor(R.color.light_grey));
//        mHomeTextProfile.setTextColor(getActivity().getColor(R.color.light_grey));
//        mHomeTextProfileHeader.setTextColor(getActivity().getColor(R.color.light_grey));

        if (!initial) {
            ThreatIndicatorGLRenderer.setThreatLevel(ThreatIndicatorGLRenderer.ThreatLevel.NOT_MONITORING);
        }

        isActive = false;
    }

    public void initAsNotActive() {
        mHomeSwitchConnection.setChecked(false);

        setStateNotActive(false);
    }

    public void initStateActive(){
        setStateActive();

        mHomeSwitchConnection.setChecked(true);
    }

    public void setStateActive() {
        mHomeTextName.setTextColor(mDefaultTextColor);
        mHomeTextProfile.setTextColor(mDefaultTextColor);
        mHomeTextProfileHeader.setTextColor(mDefaultTextColor);

        isActive = true;
    }

    public void setStateNotConnected() {
        mHomeTextSecurity.setVisibility(View.INVISIBLE);
        mHomeTextAttacks.setVisibility(View.INVISIBLE);
        mHomeTextProfile.setVisibility(View.INVISIBLE);
        mHomeTextProfileHeader.setVisibility(View.INVISIBLE);
        mHomeProfileImage.setVisibility(View.INVISIBLE);
        mHomeConnectionInfoButton.setVisibility(View.INVISIBLE);

        mHomeTextName.setText(R.string.not_connected);
        isConnected = false;
    }

    public void setStateConnected() {
        mHomeTextAttacks.setVisibility(View.VISIBLE);
        mHomeTextSecurity.setVisibility(View.VISIBLE);
        mHomeTextProfile.setVisibility(View.VISIBLE);
        mHomeTextProfileHeader.setVisibility(View.VISIBLE);
        mHomeProfileImage.setVisibility(View.VISIBLE);
        mHomeConnectionInfoButton.setVisibility(View.VISIBLE);
        isConnected = true;
    }

    public void updateUI() {
        loadCurrentProfile();
        loadConnectionInfo();

        boolean hasActiveListeners = false;
        int totalAttacks = daoHelper.getAttackRecordDAO().getNumAttacksSeenByBSSID(mConnectionInfo.getString(getActivity().getString(R.string.connection_info_bssid), null));

        if (MainActivity.getInstance().getHostageService() != null) {
            if (MainActivity.getInstance().getHostageService().hasRunningListeners()) {
                hasActiveListeners = true;
                updateThreatAnimation(totalAttacks);
            }
        }
        updateTextConnection(totalAttacks);
        if (hasActiveListeners) {
            initStateActive();
            // color text according to threat level
            changeTextColorThreat(totalAttacks);
            //updateAndroidIcon();
            setThreatLevel();
        } else {
            initAsNotActive();
        }

    }

    private void addAndroidIcon() {
        mHomeAndroidImage = (ImageView) mRootView.findViewById(R.id.imageview);
        mHomeAndroidImage.setImageResource(R.drawable.ic_android_home);
    }

    private void updateAndroidIcon() {
        if (mThreatLevel == LIVE_THREAT && !updatedImageView) {
            mHomeAndroidImage.setImageResource(R.drawable.ic_android_threat_home);
            updatedImageView = true;
        }
    }

    private void setThreatLevel() {
        ThreatIndicatorGLRenderer.setThreatLevel(mThreatLevel);
    }

    private void changeTextColorThreat(int totalAttacks) {
        switch (mThreatLevel) {
            case NO_THREAT:
                mHomeTextAttacks.setTextColor(getActivity().getColor(R.color.holo_dark_green));
                mHomeTextSecurity.setTextColor(getActivity().getColor(R.color.holo_dark_green));
                break;
            case PAST_THREAT:
                mHomeTextAttacks.setTextColor(getActivity().getColor(R.color.holo_yellow));
                mHomeTextSecurity.setTextColor(getActivity().getColor(R.color.holo_yellow));
                break;
            case LIVE_THREAT:
                mHomeTextAttacks.setText(totalAttacks
                        + (totalAttacks == 1 ? getActivity().getString(R.string.attack) : getActivity().getString(R.string.attacks))
                        + getActivity().getString(R.string.recorded));
                mHomeTextSecurity.setText(R.string.insecure);
                mHomeTextAttacks.setTextColor(getActivity().getColor(R.color.holo_red));
                mHomeTextSecurity.setTextColor(getActivity().getColor(R.color.holo_red));
                break;
        }

    }

    private static void updateThreatAnimation(int totalAttacks) {
        if (MainActivity.getInstance().getHostageService().hasActiveAttacks() && totalAttacks > 0) {
            mThreatLevel = LIVE_THREAT;
        } else if (totalAttacks > 0) {
            mThreatLevel = ThreatIndicatorGLRenderer.ThreatLevel.PAST_THREAT;
        } else {
            mThreatLevel = ThreatIndicatorGLRenderer.ThreatLevel.NO_THREAT;
        }

    }

    private void updateTextConnection(int totalAttacks) {
        if (isConnected) {
            if (totalAttacks == 0) {
                mHomeTextAttacks.setText(R.string.zero_attacks);
                mHomeTextSecurity.setText(R.string.secure);
            } else {
                mHomeTextAttacks.setText(totalAttacks
                        + (totalAttacks == 1 ? getActivity().getString(R.string.attack) : getActivity().getString(R.string.attacks))
                        + getActivity().getString(R.string.recorded));
                mHomeTextSecurity.setText(R.string.insecure);
            }
        } else {
            mHomeTextAttacks.setText("");
            mHomeTextSecurity.setText("");
        }

    }

    private void loadConnectionInfo() {
        if (HelperUtils.isNetworkAvailable(getActivity())) {
            setStateConnected();
            String ssid = mConnectionInfo.getString(getActivity().getString(R.string.connection_info_ssid), "");
            mHomeTextName.setText(ssid);
        } else {
            setStateNotConnected();
        }
    }

    private void loadCurrentProfile() {
        Profile profile = mProfileManager.getCurrentActivatedProfile();
        if (profile != null) {
            mHomeTextProfile.setText(profile.mLabel);
            mHomeProfileImage.setImageBitmap(profile.getIconBitmap());
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addThreatAnimation() {
        mRootView.findViewById(R.id.surfaceview).setOnTouchListener((v, event) -> {
            float relx = event.getX() / (float) v.getWidth();
            float rely = event.getY() / (float) v.getHeight();
            if (relx < 0.25f || relx > 0.75f) return false;
            if (rely < 0.25f || rely > 0.9f) return false;

            ThreatIndicatorGLRenderer.showSpeechBubble();

            return false;
        });
    }

    private void loadAttackListener() {
        String ssid = mConnectionInfo.getString(getContext().getString(R.string.connection_info_ssid), "");
        if (!ssid.isEmpty()) {
            ArrayList<String> ssids = new ArrayList<>();
            ssids.add(ssid);

            LogFilter filter = new LogFilter();
            filter.setESSIDs(ssids);

            RecordOverviewFragment recordOverviewFragment = new RecordOverviewFragment();
            recordOverviewFragment.setFilter(filter);
            recordOverviewFragment.setGroupKey("ESSID");

            MainActivity.getInstance().injectFragment(recordOverviewFragment);
        }
    }

    private void addConnectionInfoButton() {
        // hook up the connection info button
        mHomeConnectionInfoButton.setOnClickListener(v -> {
            final FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                ConnectionInfoDialogFragment connectionInfoDialogFragment = new ConnectionInfoDialogFragment();
                connectionInfoDialogFragment.show(fragmentManager.beginTransaction(), connectionInfoDialogFragment.getTag());
            }
        });
    }

    private void addBroadcastReceiver() {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getUserVisibleHint())
                    updateUI();
            }
        };
        registerBroadcastReceiver();

        updateUI();
    }

    private void noServicesAlertDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.information)
                .setMessage(R.string.profile_no_services_msg)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                        }).setIcon(android.R.drawable.ic_dialog_info)
                .show();

        initAsNotActive();
    }

    private void noConnectionAlertDialog() {
        new AlertDialog.Builder(getActivity()).setTitle(R.string.information).setMessage(R.string.network_not_connected_msg)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {

                }).setIcon(android.R.drawable.ic_dialog_info).show();

        initAsNotActive();
        setStateNotConnected();
    }

    @Override
    public void onStart() {
        super.onStart();
        registerBroadcastReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mRootView != null) {
            unbindDrawables(mRootView);
            mRootView = null;
        }
        if (mReceiver != null) {
            unregisterBroadcastReceiver();
        }
        updateUIThread.interrupt();
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

    /**
     * Checks if foreground and background location permissions are granted. If a permission is not
     * granted, trigger a permission request and return false.
     *
     * @return true if all permissions are already granted.
     */
    private boolean arePermissionsReady() {
        try {
            customLocationManager = CustomLocationManager.getLocationManagerInstance(getContext());
        } catch (LocationException le) {
            le.printStackTrace();
        }

        if (!customLocationManager.isLocationPermissionGranted(getContext())) {
            customLocationManager.getLocationPermission(this);
            return false;

        } else if (!customLocationManager.isBackgroundPermissionGranted(getContext())) {
            customLocationManager.getBackgroundPermission(this);
            return false;

        } else {
            return true;
        }
    }

    /**
     * Start updating location and start monitoring the selected profile.
     */
    private void startMonitoring() {
        try {
            customLocationManager.keepLocationUpdated(true);
        } catch (LocationException le) {
            le.printStackTrace();
        }

        MainActivity.getInstance().startMonitorServices(protocols);
        setStateActive();
    }

    /**
     * Handle when user has given or rejected a location permission request
     *
     * @param requestCode Request identification (foreground or background location request)
     * @param permissions
     * @param grantResults Request result (given or rejected
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

//                    Ask user about background location next
                    if (!customLocationManager.isBackgroundPermissionGranted(getContext())) {
                        customLocationManager.getBackgroundPermission(this);
                    }
                } else {
//                    Explain why wee need location
                    showReasonAfterForegroundDeny();
                }
                startMonitoring();

                break;
            }
            case LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We currently do nothing more after we have received background location permission
                } else {
                    showReasonAfterBackgroundDeny();
                }

                break;
            }
        }
    }

    /**
     * Show dialog informing the user why location permission is needed.
     */
    private void showReasonAfterForegroundDeny() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(R.string.location_permission_needed);
        dialog.setMessage(R.string.location_permission_denied);
        dialog.setNeutralButton(R.string.ok, null);

        dialog.create().show();
    }

    /**
     * Show dialog informing the user why background location is needed
     */
    private void showReasonAfterBackgroundDeny() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(R.string.background_location_needed);
        dialog.setMessage(R.string.background_permission_denied);
        dialog.setNeutralButton(R.string.ok, null);

        dialog.create().show();
    }
}





