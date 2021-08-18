package dk.aau.netsec.hostage.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

import dk.aau.netsec.hostage.Handler;
import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.model.Profile;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.persistence.ProfileManager;
import dk.aau.netsec.hostage.ui.activity.MainActivity;
import dk.aau.netsec.hostage.ui.adapter.ServicesListAdapter;
import dk.aau.netsec.hostage.ui.model.ServicesListItem;


/**
 * @author Daniel Lazar
 * @created 05.02.14
 * Fragment that displays a switch for every protocol.
 * Also it can de-/activate every protocol by using this switch.
 */
public class ServicesFragment extends TrackerFragment {
    private SwitchMaterial mServicesSwitchService;
    private TextView mServicesTextName;

    private View rootView;
    private CompoundButton.OnCheckedChangeListener switchChangeListener = null;
    private BroadcastReceiver mReceiver;
    private ServicesListAdapter adapter;
    private ArrayList<ServicesListItem> protocolList;

    private final DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
    private final DAOHelper daoHelper = new DAOHelper(dbSession, getActivity());

    private String[] protocols;
    private SharedPreferences mConnectionInfo;
    private boolean mReceiverRegistered = false;
    private Profile mProfile;
    private Integer[] mGhostPorts;

    private LayoutInflater inflater;
    private ViewGroup container;
    private Bundle savedInstanceState;

    public ServicesFragment() {
    }

    /**
     * assign views which are not asynchronously loaded
     */
    private void assignViews() {
        mServicesSwitchService = rootView.findViewById(R.id.service_switch_connection);
        mServicesTextName = rootView.findViewById(R.id.services_text_name);

        rootView.findViewById(R.id.services_button_connection_info).setOnClickListener(v -> {
            ConnectionInfoDialogFragment connectionInfoDialogFragment = new ConnectionInfoDialogFragment();
            connectionInfoDialogFragment.show(getFragmentManager().beginTransaction(), connectionInfoDialogFragment.getTag());
        });

    }

    /**
     * updates the user interface
     * in detail: the main switch and the textField mServicesTextName
     */
    public void updateUI() {
        if (!HelperUtils.isNetworkAvailable(getActivity())) {
            if (!MainActivity.getInstance().getHostageService().hasRunningListeners()) {
                mServicesSwitchService.setOnCheckedChangeListener(null);
                setStateNotConnected();
                setStateNotActive();
                mServicesSwitchService.setOnCheckedChangeListener(switchChangeListener);
            } else {
                mServicesSwitchService.setOnCheckedChangeListener(null);
                setStateNotConnected();
                mServicesSwitchService.setChecked(true);
                mServicesSwitchService.setOnCheckedChangeListener(switchChangeListener);
            }
        } else {
            if (MainActivity.getInstance().getHostageService().hasRunningListeners()) {
                setStateActive();
            }
            String ssid = mConnectionInfo.getString(getString(R.string.connection_info_ssid), "");

            mServicesTextName.setText(ssid);
        }

    }

    /**
     * register a broadcast receiver if not already registered
     * and also update the number of attacks per protocol
     */
    private void registerBroadcastReceiver() {
        if (!mReceiverRegistered) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String sender = intent.getStringExtra("SENDER");
                    String[] values = intent.getStringArrayExtra("VALUES");

                    if (sender.equals(Handler.class.getName()) && values[0].equals(getString(R.string.broadcast_started))) {
                        for (ServicesListItem item : protocolList) {
                            if (item.protocol.equals(values[1])) {
                                item.attacks = daoHelper.getAttackRecordDAO().getNumAttacksSeenByBSSID(item.protocol,
                                        mConnectionInfo.getString(
                                                getString(R.string.connection_info_bssid), null));
                            }
                        }
                    }

                    if (!MainActivity.getInstance().getHostageService().hasRunningListeners()) {
                        setStateNotActive();
                    } else {
                        setStateActive();
                    }
                    updateUI();
                    adapter.notifyDataSetChanged();
                }
            };

            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, new IntentFilter(getString(R.string.broadcast)));
            this.mReceiverRegistered = true;
        }
    }


    /**
     * most important method of this class
     *
     * @param inflater           the inflater
     * @param container          the container
     * @param savedInstanceState the saved instance state
     * @return rootView
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;
        this.container = container;
        this.savedInstanceState = savedInstanceState;
        rootView = inflater.inflate(R.layout.fragment_services, container, false);
        assignViews();

        protocols = getResources().getStringArray(R.array.protocols);
        int[] originalPorts = getResources().getIntArray(R.array.ports);
        mConnectionInfo = getActivity().getSharedPreferences(getString(R.string.connection_info), Context.MODE_PRIVATE);
        updateUI();
        ListView list = rootView.findViewById(R.id.services_list_view);
        protocolList = new ArrayList<>();
        int i = 0;
        for (String protocol : protocols) {
            protocolList.add(new ServicesListItem(protocol, originalPorts[i]));
            protocolList.get(i).attacks = daoHelper.getAttackRecordDAO().getNumAttacksSeenByBSSID(protocolList.get(i).protocol,
                    mConnectionInfo.getString(getString(R.string.connection_info_bssid), null));
            i++;
        }
        mServicesSwitchService = rootView.findViewById(R.id.service_switch_connection);

        if (switchChangeListener == null) {
            switchChangeListener = (buttonView, isChecked) -> {

                mProfile = ProfileManager.getInstance().getCurrentActivatedProfile();

                if (isChecked) { // switch activated
                    // we need a network connection, checks both types
                    if (!HelperUtils.isNetworkAvailable(getActivity())) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.information)
                                .setMessage(R.string.wifi_not_connected_msg)
                                .setPositiveButton(android.R.string.ok,
                                        (dialog, which) -> {
                                        }
                                )
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .show();
                        setStateNotActive();
                        setStateNotConnected();
                    } else { // we have a connection
                        // activate all protocols
                        try {
                            for (String protocol : protocols) {
                                if (MainActivity.getInstance().getHostageService() != null
                                        && !MainActivity.getInstance().getHostageService().isRunning(protocol)) {
                                    MainActivity.getInstance().getHostageService().startListener(protocol);
                                }
                            }

                            setStateActive();
                        } catch (NullPointerException ne) {
                            Snackbar.make(rootView, R.string.error_activating_services, Snackbar.LENGTH_SHORT).show();
                            setStateNotActive();
                        }

                    }
                } else { // switch deactivated
                    if (MainActivity.getInstance().getHostageService() != null) {
                        // why should the hostage service not be running??
                        MainActivity.getInstance().getHostageService().stopListeners();
                        MainActivity.getInstance().stopAndUnbind();
                    }
                }
            };
        }
        mServicesSwitchService.setOnCheckedChangeListener(switchChangeListener);

        adapter = new ServicesListAdapter(getActivity().getBaseContext(), protocolList);
        adapter.setActivity(this.mServicesSwitchService, this.switchChangeListener);
        list.setAdapter(adapter);

        registerBroadcastReceiver();

        return rootView;

    }

    @Deprecated
    private void checkGhost(String protocol) {
        if (protocol.equals("GHOST") && mProfile.mGhostActive) {
            mGhostPorts = mProfile.getGhostPorts();
            if (mGhostPorts.length != 0) {
                for (Integer port : mGhostPorts) {
                    if (MainActivity.getInstance().getHostageService() != null
                            && !MainActivity.getInstance().getHostageService().isRunning("GHOST", port)) {
                        MainActivity.getInstance().getHostageService().startListener("GHOST", port);
                    }
                }
            }
        }
    }


    /**
     * called on start of this fragment.
     * registers broadcast receiver and binds change listener to main switch
     */
    @Override
    public void onStart() {
        super.onStart();
        registerBroadcastReceiver();
        mServicesSwitchService.setOnCheckedChangeListener(switchChangeListener);
    }

    /**
     * unregister the broadcast receiver if a receiver is already registered
     */
    private void unregisterBroadcastReceiver() {
        if (mReceiverRegistered) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
            this.mReceiverRegistered = false;
        }
    }

    /**
     * sets main switch to true
     */
    private void setStateActive() {
        mServicesSwitchService.setChecked(true);
    }

    /**
     * sets text of text field to not connected, if the device is not connected to a network
     */
    private void setStateNotConnected() {
        mServicesTextName.setText(R.string.not_connected);
    }

    /**
     * sets main switch to false
     */
    private void setStateNotActive() {
        if (mServicesSwitchService.isChecked()) {
            mServicesSwitchService.setChecked(false);
        }
    }

    /**
     * overrides onStop
     * unloads the ChangeListener
     */
    @Override
    public void onStop() {
        super.onStop();
        if (rootView != null) {
            mServicesSwitchService.setOnCheckedChangeListener(null);
//            unbindDrawables(rootView);
//            rootView=null;
        }

        if (mReceiver != null)
            unregisterBroadcastReceiver();

    }

//    @Override
//    public void onResume(){
//        super.onResume();
//        onCreateView(inflater,container,savedInstanceState);
//        registerBroadcastReceiver();
//    }

    /**
     * overrides onDestroy
     * unregisters broadcast receiver, when destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rootView != null) {
            mServicesSwitchService.setOnCheckedChangeListener(null);
            unbindDrawables(rootView);
            rootView = null;
        }
        if (mReceiver != null)
            unregisterBroadcastReceiver();
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
