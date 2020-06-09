package de.tudarmstadt.informatik.hostage.ui.fragment;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;


import de.tudarmstadt.informatik.hostage.Handler;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBOpenHelper;
import de.tudarmstadt.informatik.hostage.persistence.ProfileManager;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.adapter.ServicesListAdapter;
import de.tudarmstadt.informatik.hostage.ui.model.ServicesListItem;


/**
 * @author Daniel Lazar
 * @created 05.02.14
 * Fragment that displays a switch for every protocol.
 * Also it can de-/activate every protocol by using this switch.
 */
public class ServicesFragment extends TrackerFragment {
    private Switch mServicesSwitchService;
    private TextView mServicesTextName;

    private View rootView;

    private CompoundButton.OnCheckedChangeListener switchChangeListener = null;

    private BroadcastReceiver mReceiver;

    private ServicesListAdapter adapter;

    private ArrayList<ServicesListItem> protocolList;

    private HostageDBOpenHelper dbh = new HostageDBOpenHelper(MainActivity.getContext());

    private String[] protocols;

    private SharedPreferences mConnectionInfo;

    private boolean mReceiverRegistered = false;

    private Profile mProfile;
    private Integer[] mGhostPorts;

    public ServicesFragment() {
    }

    /**
     * assign views which are not asynchronously loaded
     */
    private void assignViews() {
        mServicesSwitchService = rootView.findViewById(R.id.service_switch_connection);
        mServicesTextName = rootView.findViewById(R.id.services_text_name);

        rootView.findViewById(R.id.services_button_connection_info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectionInfoDialogFragment connectionInfoDialogFragment = new ConnectionInfoDialogFragment();
                connectionInfoDialogFragment.show(getFragmentManager().beginTransaction(), connectionInfoDialogFragment.getTag());
            }
        });

    }

    /**
     * updates the user interface
     * in detail: the main switch and the textField mServicesTextName
     */
    public void updateUI() {
        //SK: Temp bugfix
        //if (!HelperUtils.isNetworkAvailable(getActivity())) {
        if (!HelperUtils.isNetworkAvailable(getActivity())) {
            if(!MainActivity.getInstance().getHostageService().hasRunningListeners()) {
                mServicesSwitchService.setOnCheckedChangeListener(null);
                setStateNotConnected();
                setStateNotActive();
                mServicesSwitchService.setOnCheckedChangeListener(switchChangeListener);
            }
            else{
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
                                item.attacks = dbh.getNumAttacksSeenByBSSID(item.protocol,
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
     * @param inflater the inflater
     * @param container the container
     * @param savedInstanceState the saved instance state
     * @return rootView
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        rootView = inflater.inflate(R.layout.fragment_services, container, false);
        assignViews();

        protocols = getResources().getStringArray(R.array.protocols);
        mConnectionInfo = getActivity().getSharedPreferences(getString(R.string.connection_info), Context.MODE_PRIVATE);

        updateUI();

        ListView list = rootView.findViewById(R.id.services_list_view);

        protocolList = new ArrayList<ServicesListItem>();
        int i = 0;
        for (String protocol : protocols) {
            protocolList.add(new ServicesListItem(protocol));
            protocolList.get(i).attacks = dbh.getNumAttacksSeenByBSSID(protocolList.get(i).protocol,
                    mConnectionInfo.getString(getString(R.string.connection_info_bssid), null));
            i++;
        }

        mServicesSwitchService = rootView.findViewById(R.id.service_switch_connection);

        if (switchChangeListener == null) {
            switchChangeListener = new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mProfile = ProfileManager.getInstance().getCurrentActivatedProfile();
                    //SK: Temp bugfix
                        //if (!HelperUtils.isNetworkAvailable(getActivity())) {
					//replaced with if (!HelperUtils.isWifiConnected(getActivity())) {

					if (isChecked) { // switch activated
						// we need a network connection, checks both types
						if (!HelperUtils.isNetworkAvailable(getActivity())) {
							new AlertDialog.Builder(getActivity())
									.setTitle(R.string.information)
									.setMessage(R.string.wifi_not_connected_msg)
									.setPositiveButton(android.R.string.ok,
											new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int which) {

												}
											}
									)
									.setIcon(android.R.drawable.ic_dialog_info)
									.show();
							setStateNotActive();
							setStateNotConnected();
						} else { // we have a connection
							// activate all protocols
							for (String protocol : protocols) {
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
								} else {
									if (MainActivity.getInstance().getHostageService() != null
											&& !MainActivity.getInstance().getHostageService().isRunning(protocol)) {
										MainActivity.getInstance().getHostageService().startListener(protocol);
									}
								}
							}
							setStateActive();
						}
					} else { // switch deactivated
						if (MainActivity.getInstance().getHostageService() != null) {
							// why should the hostage service not be running??
							MainActivity.getInstance().getHostageService().stopListeners();
							MainActivity.getInstance().stopAndUnbind();
						}
						setStateNotActive();
					}
                }

            };
        }
        mServicesSwitchService.setOnCheckedChangeListener(switchChangeListener);

        adapter = new ServicesListAdapter(getActivity().getBaseContext(), protocolList);
        adapter.setActivity(this.getActivity(), this.mServicesSwitchService, this.switchChangeListener);
        list.setAdapter(adapter);

        registerBroadcastReceiver();

        return rootView;

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
        mServicesSwitchService.setOnCheckedChangeListener(null);
    }

    /**
     * overrides onDestroy
     * unregisters broadcast receiver, when destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceiver();
    }
}
