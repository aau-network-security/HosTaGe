package de.tudarmstadt.informatik.hostage.ui.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;


import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import de.tudarmstadt.informatik.hostage.HostageApplication;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.persistence.DAO.DAOHelper;
import de.tudarmstadt.informatik.hostage.persistence.ProfileManager;
import de.tudarmstadt.informatik.hostage.services.MultiStageAlarm;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.fragment.opengl.ThreatIndicatorGLRenderer;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;


/**
 * @author Alexander Brakowski
 * @created 13.01.14 19:06
 */

public class HomeFragment extends Fragment {
	private Switch mHomeSwitchConnection;
	private TextView mHomeTextName;
	private TextView mHomeTextSecurity;
	private TextView mHomeTextAttacks;
	private TextView mHomeTextProfile;
	private TextView mHomeTextProfileHeader;
	private ImageView mHomeProfileImage;
	private ImageView mHomeConnectionInfoButton;

	private View mRootView;
	private LayoutInflater inflater;
	private ViewGroup container;
	private Bundle savedInstanceState;

	private BroadcastReceiver mReceiver;

	private CompoundButton.OnCheckedChangeListener mSwitchChangeListener = null;
	private int mDefaultTextColor;
	private ProfileManager mProfileManager;
	private SharedPreferences mConnectionInfo;

	private  DaoSession dbSession;
	private  DAOHelper daoHelper;

	private boolean mReceiverRegistered;
	private boolean mRestoredFromSaved = false;
	private boolean isActive = false;
	private boolean isConnected = false;

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Context context;


	private ThreatIndicatorGLRenderer.ThreatLevel mThreatLevel = ThreatIndicatorGLRenderer.ThreatLevel.NOT_MONITORING;

	private void assignViews() {
		mHomeSwitchConnection = mRootView.findViewById(R.id.home_switch_connection);
		mHomeTextName = mRootView.findViewById(R.id.home_text_name);
		mHomeTextSecurity = mRootView.findViewById(R.id.home_text_security);
		mHomeTextAttacks = mRootView.findViewById(R.id.home_text_attacks);
		mHomeTextProfile = mRootView.findViewById(R.id.home_text_profile);
		mHomeTextProfileHeader = mRootView.findViewById(R.id.home_text_profile_header);
		mHomeProfileImage = mRootView.findViewById(R.id.home_image_profile);
		mHomeConnectionInfoButton = mRootView.findViewById(R.id.home_button_connection_info);
	}

	private void registerBroadcastReceiver() {
		if (!mReceiverRegistered) {
			LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, new IntentFilter(getString(R.string.broadcast)));
			this.mReceiverRegistered = true;
		}
	}

	private void unregisterBroadcastReceiver() {
		if (mReceiverRegistered) {
			LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
			this.mReceiverRegistered = false;
		}
	}

	public HomeFragment() {
	}

	public void setStateNotActive(boolean initial) {
		mHomeTextName.setTextColor(getResources().getColor(R.color.light_grey));
		mHomeTextSecurity.setTextColor(getResources().getColor(R.color.light_grey));
		mHomeTextAttacks.setTextColor(getResources().getColor(R.color.light_grey));
		mHomeTextProfile.setTextColor(getResources().getColor(R.color.light_grey));
		mHomeTextProfileHeader.setTextColor(getResources().getColor(R.color.light_grey));

		if (!initial) {
			ThreatIndicatorGLRenderer.setThreatLevel(ThreatIndicatorGLRenderer.ThreatLevel.NOT_MONITORING);
		}

		mHomeSwitchConnection.setChecked(false);
		isActive = false;
	}

	public void setStateNotActive() {
		setStateNotActive(false);
	}

	public void setStateActive() {
		setStateActive(false);
	}

	public void setStateActive(boolean initial) {
		mHomeTextName.setTextColor(mDefaultTextColor);
		mHomeTextProfile.setTextColor(mDefaultTextColor);
		mHomeTextProfileHeader.setTextColor(mDefaultTextColor);

		mHomeSwitchConnection.setChecked(true);
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
		Profile profile = mProfileManager.getCurrentActivatedProfile();
		if (profile != null) {
			mHomeTextProfile.setText(profile.mLabel);
			mHomeProfileImage.setImageBitmap(profile.getIconBitmap());
		}

		//new FileAlertTask().execute();

		if (HelperUtils.isNetworkAvailable(getActivity())) {
			setStateConnected();
			String ssid = mConnectionInfo.getString(getString(R.string.connection_info_ssid), "");
			mHomeTextName.setText(ssid);
		} else {
			setStateNotConnected();
		}

		boolean hasActiveListeners = false;
		int totalAttacks = daoHelper.getAttackRecordDAO().getNumAttacksSeenByBSSID(
				mConnectionInfo.getString(getString(R.string.connection_info_bssid), null));

		if (MainActivity.getInstance().getHostageService() != null) {
			if (MainActivity.getInstance().getHostageService().hasRunningListeners()) {
				hasActiveListeners = true;

				if (MainActivity.getInstance().getHostageService().hasActiveAttacks() && totalAttacks > 0) {
					mThreatLevel = ThreatIndicatorGLRenderer.ThreatLevel.LIVE_THREAT;
				} else if (totalAttacks > 0) {
					mThreatLevel = ThreatIndicatorGLRenderer.ThreatLevel.PAST_THREAT;
				} else {
					mThreatLevel = ThreatIndicatorGLRenderer.ThreatLevel.NO_THREAT;
				}
			}
		}

		if (isConnected) {
			if (totalAttacks == 0) {
				mHomeTextAttacks.setText(R.string.zero_attacks);
				mHomeTextSecurity.setText(R.string.secure);
			} else {
				mHomeTextAttacks.setText(totalAttacks
						+ (totalAttacks == 1 ? getResources().getString(R.string.attack) : getResources().getString(R.string.attacks))
						+ getResources().getString(R.string.recorded));
				mHomeTextSecurity.setText(R.string.insecure);
			}
		} else {
			mHomeTextAttacks.setText("");
			mHomeTextSecurity.setText("");
		}

		if (hasActiveListeners) {
			setStateActive(true);

			// color text according to threat level
			switch (mThreatLevel) {
				case NO_THREAT:
					mHomeTextAttacks.setTextColor(getResources().getColor(R.color.holo_dark_green));
					mHomeTextSecurity.setTextColor(getResources().getColor(R.color.holo_dark_green));
					break;
				case PAST_THREAT:
					mHomeTextAttacks.setTextColor(getResources().getColor(R.color.holo_yellow));
					mHomeTextSecurity.setTextColor(getResources().getColor(R.color.holo_yellow));
					break;
				case LIVE_THREAT:
					mHomeTextAttacks.setText(totalAttacks
							+ (totalAttacks == 1 ? getResources().getString(R.string.attack) : getResources().getString(R.string.attacks))
							+ getResources().getString(R.string.recorded));
					mHomeTextSecurity.setText(R.string.insecure);
					mHomeTextAttacks.setTextColor(getResources().getColor(R.color.holo_red));
					mHomeTextSecurity.setTextColor(getResources().getColor(R.color.holo_red));
					break;
			}

			ThreatIndicatorGLRenderer.setThreatLevel(mThreatLevel);
		} else {
			setStateNotActive();
		}

	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		final Activity activity = getActivity();
		if (activity != null) {
			activity.setTitle(getResources().getString(R.string.drawer_overview));
		}

		dbSession = HostageApplication.getInstances().getDaoSession();
		daoHelper = new DAOHelper(dbSession, getActivity());

		try {
			mProfileManager = ProfileManager.getInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mConnectionInfo = getActivity().getSharedPreferences(getString(R.string.connection_info), Context.MODE_PRIVATE);

		this.inflater = inflater;
		this.container= container;
		this.savedInstanceState = savedInstanceState;
		mRootView = inflater.inflate(R.layout.fragment_home, container, false);
		assignViews();

		mRootView.findViewById(R.id.surfaceview).setOnTouchListener((v, event) -> {
			float relx = event.getX() / (float) v.getWidth();
			float rely = event.getY() / (float) v.getHeight();
			if (relx < 0.25f || relx > 0.75f) return false;
			if (rely < 0.25f || rely > 0.9f) return false;

			ThreatIndicatorGLRenderer.showSpeechBubble();

			return false;
		});

		// hook up the connection info button
		mHomeConnectionInfoButton.setOnClickListener(v -> {
			final FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ConnectionInfoDialogFragment connectionInfoDialogFragment = new ConnectionInfoDialogFragment();
				connectionInfoDialogFragment.show(fragmentManager.beginTransaction(), connectionInfoDialogFragment.getTag());
			}
		});

		mDefaultTextColor = mHomeTextName.getCurrentTextColor();

		setStateNotActive(true);
		setStateNotConnected();

		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (getUserVisibleHint())
					updateUI();
			}
		};
		registerBroadcastReceiver();

		updateUI();

		mHomeSwitchConnection = mRootView.findViewById(R.id.home_switch_connection);
		mHomeSwitchConnection.setSaveEnabled(false);

		if (mSwitchChangeListener == null) {
			mSwitchChangeListener = (buttonView, isChecked) -> {
				if (isChecked) { // switch activated
					// we need a network connection
					// 2.5.2015 Fabio: for now we only check for wifi connections
					if (!HelperUtils.isNetworkAvailable(getActivity())) {
						new AlertDialog.Builder(getActivity()).setTitle(R.string.information).setMessage(R.string.network_not_connected_msg)
								.setPositiveButton(android.R.string.ok, (dialog, which) -> {

								}).setIcon(android.R.drawable.ic_dialog_info).show();

						setStateNotActive();
						setStateNotConnected();
					} else { // network available
						boolean protocolActivated = false;
						try {
							if (ProfileManager.getInstance().getCurrentActivatedProfile() == null) {
								MainActivity.getInstance().startMonitorServices(Arrays.asList(
										getResources().getStringArray(R.array.protocols)));
							} else {
								ProfileManager profileManager = ProfileManager.getInstance();

								if (profileManager.isRandomActive()) {
									profileManager
											.randomizeProtocols(profileManager.getRandomProfile());
								}

								Profile currentProfile = profileManager
										.getCurrentActivatedProfile();
								List<String> protocols = currentProfile.getActiveProtocols();
								if (protocols.size() > 0 || currentProfile.mGhostActive) {
									protocols.add("GHOST");
									MainActivity.getInstance().startMonitorServices(protocols);
									protocolActivated = true;
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

						if (protocolActivated) {
							setStateActive();
						} else {
							new AlertDialog.Builder(getActivity())
									.setTitle(R.string.information)
									.setMessage(R.string.profile_no_services_msg)
									.setPositiveButton(android.R.string.ok,
											(dialog, which) -> {

											}).setIcon(android.R.drawable.ic_dialog_info)
									.show();

							setStateNotActive();
						}
					}
				} else { // switch deactivated
					if (MainActivity.getInstance().getHostageService() != null) {
						MainActivity.getInstance().getHostageService().stopListeners();
						MainActivity.getInstance().stopAndUnbind();
					}
					setStateNotActive();
				}
			};
		}
		mHomeSwitchConnection.setOnCheckedChangeListener(mSwitchChangeListener);

		mRootView.findViewById(R.id.home_profile_details).setOnClickListener(v -> {
			Fragment fragment = new ProfileManagerFragment();
			MainActivity.getInstance().injectFragment(fragment);
		});

		View.OnClickListener attackClickListener = v -> {
			String ssid = mConnectionInfo.getString(getString(R.string.connection_info_ssid), "");
			if (!ssid.isEmpty()) {
				ArrayList<String> ssids = new ArrayList<String>();
				ssids.add(ssid);

				LogFilter filter = new LogFilter();
				filter.setESSIDs(ssids);

				RecordOverviewFragment recordOverviewFragment = new RecordOverviewFragment();
				recordOverviewFragment.setFilter(filter);
				recordOverviewFragment.setGroupKey("ESSID");

				MainActivity.getInstance().injectFragment(recordOverviewFragment);
			}
		};

		mHomeTextAttacks.setOnClickListener(attackClickListener);
		mHomeTextSecurity.setOnClickListener(attackClickListener);

		return mRootView;
	}

	@Override
	public void onStop() {
		super.onStop();
		if(mRootView!=null) {
			unbindDrawables(mRootView);
			mRootView=null;
		}
	}

	@Override
	public void onResume(){
		super.onResume();
		if(inflater!=null || container!=null || savedInstanceState!=null) {
			onCreateView(inflater, container, savedInstanceState);
			updateUI();
			registerBroadcastReceiver();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		registerBroadcastReceiver();
		updateUI();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mRootView!=null) {
			unbindDrawables(mRootView);
			mRootView=null;
		}
		if (mReceiver != null){
			unregisterBroadcastReceiver();
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

	public void AlertFile(String fname) {
		AlertDialog alert = new AlertDialog.Builder(getActivity()).create();
		alert.setTitle("Delete entry");
		alert.setMessage("Are you sure you want to delete this entry?:\n" + fname);
		alert.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
				(dialog, which) -> dialog.dismiss());
		alert.show();
	}


}





