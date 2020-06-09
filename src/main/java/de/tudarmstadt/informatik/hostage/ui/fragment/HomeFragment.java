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

import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;



import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBOpenHelper;
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

	private BroadcastReceiver mReceiver;

	private CompoundButton.OnCheckedChangeListener mSwitchChangeListener = null;

	private int mDefaultTextColor;

	private ProfileManager mProfileManager;

	private SharedPreferences mConnectionInfo;

	private HostageDBOpenHelper mDbHelper;

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
		int totalAttacks = mDbHelper.getNumAttacksSeenByBSSID(
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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final MultiStageAlarm alarm = new MultiStageAlarm();

		final Activity activity = getActivity();
		if (activity != null) {
			activity.setTitle(getResources().getString(R.string.drawer_overview));
		}

		mDbHelper = new HostageDBOpenHelper(getActivity());

		mProfileManager = ProfileManager.getInstance();
		mConnectionInfo = getActivity().getSharedPreferences(getString(R.string.connection_info), Context.MODE_PRIVATE);

		mRootView = inflater.inflate(R.layout.fragment_home, container, false);
		assignViews();

		mRootView.findViewById(R.id.surfaceview).setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				float relx = event.getX() / (float) v.getWidth();
				float rely = event.getY() / (float) v.getHeight();
				if (relx < 0.25f || relx > 0.75f) return false;
				if (rely < 0.25f || rely > 0.9f) return false;

				ThreatIndicatorGLRenderer.showSpeechBubble();

				return false;
			}
		});

		// hook up the connection info button
		mHomeConnectionInfoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final FragmentManager fragmentManager = getFragmentManager();
				if (fragmentManager != null) {
					ConnectionInfoDialogFragment connectionInfoDialogFragment = new ConnectionInfoDialogFragment();
					connectionInfoDialogFragment.show(fragmentManager.beginTransaction(), connectionInfoDialogFragment.getTag());
				}
			}
		});

		mDefaultTextColor = mHomeTextName.getCurrentTextColor();

		setStateNotActive(true);
		setStateNotConnected();

		mReceiver = new BroadcastReceiver() {
			@SuppressLint("NewApi")
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
			mSwitchChangeListener = new CompoundButton.OnCheckedChangeListener() {
				@RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) { // switch activated
						// we need a network connection
						// 2.5.2015 Fabio: for now we only check for wifi connections
						if (!HelperUtils.isNetworkAvailable(getActivity())) {
							new AlertDialog.Builder(getActivity()).setTitle(R.string.information).setMessage(R.string.network_not_connected_msg)
									.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {

										}
									}).setIcon(android.R.drawable.ic_dialog_info).show();

							setStateNotActive();
							setStateNotConnected();
						} else { // network available
							boolean protocolActivated = false;
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

							if (protocolActivated) {
								setStateActive();
							} else {
								new AlertDialog.Builder(getActivity())
										.setTitle(R.string.information)
										.setMessage(R.string.profile_no_services_msg)
										.setPositiveButton(android.R.string.ok,
												new DialogInterface.OnClickListener() {
													public void onClick(DialogInterface dialog,
																		int which) {

													}
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
				}

			};
		}
		mHomeSwitchConnection.setOnCheckedChangeListener(mSwitchChangeListener);

		mRootView.findViewById(R.id.home_profile_details).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment fragment = new ProfileManagerFragment();
				MainActivity.getInstance().injectFragment(fragment);
			}
		});

		View.OnClickListener attackClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
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
			}
		};

		mHomeTextAttacks.setOnClickListener(attackClickListener);
		mHomeTextSecurity.setOnClickListener(attackClickListener);

		return mRootView;
	}

	@Override
	public void onStop() {
		super.onStop();
		unregisterBroadcastReceiver();
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
		unregisterBroadcastReceiver();
	}


	public void AlertFile(String fname) {


		AlertDialog alert = new AlertDialog.Builder(getActivity()).create();
		alert.setTitle("Delete entry");
		alert.setMessage("Are you sure you want to delete this entry?:\n" + fname);
		alert.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		alert.show();


	}


}





