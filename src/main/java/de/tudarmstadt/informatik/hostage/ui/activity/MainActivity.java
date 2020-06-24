package de.tudarmstadt.informatik.hostage.ui.activity;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;


import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


import androidx.drawerlayout.widget.DrawerLayout;
import androidx.legacy.app.ActionBarDrawerToggle;

import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.persistence.ProfileManager;
import de.tudarmstadt.informatik.hostage.sync.android.SyncUtils;
import de.tudarmstadt.informatik.hostage.system.Device;
import de.tudarmstadt.informatik.hostage.ui.adapter.DrawerListAdapter;
import de.tudarmstadt.informatik.hostage.ui.fragment.AboutFragment;
import de.tudarmstadt.informatik.hostage.ui.fragment.HomeFragment;
import de.tudarmstadt.informatik.hostage.ui.fragment.ProfileManagerFragment;
import de.tudarmstadt.informatik.hostage.ui.fragment.RecordOverviewFragment;
import de.tudarmstadt.informatik.hostage.ui.fragment.ServicesFragment;
import de.tudarmstadt.informatik.hostage.ui.fragment.SettingsFragment;
import de.tudarmstadt.informatik.hostage.ui.fragment.StatisticsFragment;
import de.tudarmstadt.informatik.hostage.ui.fragment.ThreatMapFragment;
import de.tudarmstadt.informatik.hostage.ui.fragment.UpNavigatibleFragment;
import de.tudarmstadt.informatik.hostage.ui.fragment.opengl.ThreatIndicatorGLRenderer;
import de.tudarmstadt.informatik.hostage.ui.model.DrawerListItem;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;


/**
 * Manages the whole application, and should act like an singleton.
 *
 * @author Alexander Brakowski
 * @created 12.01.14 23:24
 */
public class MainActivity extends Activity {
	public static volatile Context context;

	/** singleton instance of the MainActivity **/
	private static MainActivity sInstance = null;

	/**
	 * The currently displayed fragment
	 */
	public Fragment mDisplayedFragment;

    /**
     * The currently displayed fragment index
     */
    public int mDisplayedFragmentIndex;

	/**
	 * Holds the Hostage Service
	 */
	public Hostage mHoneyService;

	/**
	 * Manages the navigation drawer
	 */
	private DrawerLayout mDrawerLayout;

	/**
	 * Contains the listview to be displayed in the navigation drawer
	 */
	private ListView mDrawerList;

	/**
	 * Holds the toggler for the navigation drawer in the action bar
	 */
	private ActionBarDrawerToggle mDrawerToggle;

	/**
	 * The text that should be displayed in the drawer toggle
	 */
	private CharSequence mDrawerTitle;

	/**
	 * The text that should be displayed in the action bar
	 */
	private CharSequence mTitle;

	/**
	 * Holds the list, that should be displayed in the listview of the navigation drawer
	 */
	private ArrayList<DrawerListItem> mDrawerItems;

	/**
	 * Hold the state of the Hostage service
	 */
	private boolean mServiceBound = false;

	/**
	 * Connection to bind the background service
	 *
	 * @see Hostage
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		/**
		 * After the service is bound, check which has been clicked and start
		 * it.
		 *
		 * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
		 *      android.os.IBinder)
		 */
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mHoneyService = ((Hostage.LocalBinder) service).getService();
			mServiceBound = true;
		}

		/**
		 * After the service is unbound, delete reference.
		 *
		 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
		 */
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mHoneyService = null;
			mServiceBound = false;
		}

	};

	/**
	 * Holds an profile manager instance
	 */
	private ProfileManager mProfileManager;

	/**
	 * Holds the root fragment for our hierarchical fragment navigation
	 */
    private Fragment mRootFragment;

	/**
	 * Indicates if the warning, that the application will be closed, when pressing back again
	 */
	private boolean mCloseWarning = false;

	/**
	 * Hold the shared preferences for the app
	 */
	private SharedPreferences mSharedPreferences;

	/**
	 * Retrieve the singleton latest instance of the activity
	 *
	 * @return MainActivity - the singleton instance
	 */
	public static MainActivity getInstance() {
		return sInstance;
	}

	/**
	 * Retrieves the context of the application
	 *
	 * @return the context
	 */
	public static Context getContext() {
		return MainActivity.context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStart() {
		super.onStart();

        // Register syncing with android
        SyncUtils.CreateSyncAccount(this);

		if (isServiceRunning()) {
			this.bindService();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStop() {
		this.unbindService();
		super.onStop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        // make the main activity an singleton
		sInstance = this;

		// sets the static context reference to the application context
		MainActivity.context = getApplicationContext();

		setContentView(R.layout.activity_drawer_main);
		mProfileManager = ProfileManager.getInstance();

		// check for the porthack and iptables
		//Device.checkCapabilities();
		if (Device.isPortRedirectionAvailable()) {
			// redirect all the ports!!
			Device.executePortRedirectionScript();
		}

		// init threat indicator animation
		ThreatIndicatorGLRenderer.setThreatLevel(ThreatIndicatorGLRenderer.ThreatLevel.NOT_MONITORING);

		// set background color
		TypedArray arr = getTheme().obtainStyledAttributes(new int[] { android.R.color.background_light });
		ThreatIndicatorGLRenderer.setBackgroundColor(arr.getColor(0, 0xFFFFFF));
		arr.recycle();

		// configures the action bar
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);

		// sets the drawer and action title to the application title
		mTitle = mDrawerTitle = getTitle();
		mDrawerLayout = findViewById(R.id.drawer_layout);
		mDrawerList = findViewById(R.id.left_drawer);

		// propagates the navigation drawer with items
		mDrawerItems = new ArrayList<DrawerListItem>();
		mDrawerItems.add(new DrawerListItem(R.string.drawer_overview, R.drawable.ic_menu_home));
		mDrawerItems.add(new DrawerListItem(R.string.drawer_threat_map, R.drawable.ic_menu_mapmode));
		mDrawerItems.add(new DrawerListItem(R.string.drawer_records, R.drawable.ic_menu_records));
		mDrawerItems.add(new DrawerListItem(R.string.drawer_statistics, R.drawable.ic_menu_stats));
		mDrawerItems.add(new DrawerListItem(R.string.drawer_services, R.drawable.ic_menu_set_as));
		mDrawerItems.add(new DrawerListItem(R.string.drawer_profile_manager, R.drawable.ic_menu_allfriends));
		mDrawerItems.add(new DrawerListItem(R.string.drawer_settings, R.drawable.ic_menu_preferences));
		mDrawerItems.add(new DrawerListItem(R.string.drawer_help, R.drawable.ic_menu_help));
		mDrawerItems.add(new DrawerListItem(R.string.drawer_app_info, R.drawable.ic_menu_info_details));

		DrawerListAdapter listAdapter = new DrawerListAdapter(this, mDrawerItems);

		mDrawerList.setAdapter(listAdapter);
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		// configures the navigation drawer
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
			mDrawerLayout, /* DrawerLayout object */
			R.drawable.ic_navigation_drawer, /*
											 * nav drawer image to replace 'Up'
											 * caret
											 */
			R.string.drawer_open, /* "open drawer" description for accessibility */
			R.string.drawer_close /* "close drawer" description for accessibility */
		) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		// start the hostage service
		startAndBind();

		mSharedPreferences = getSharedPreferences(getString(R.string.shared_preference_path), Hostage.MODE_PRIVATE);


		if(mSharedPreferences.getBoolean("isFirstRun", true)){

			// opens navigation drawer if first run
			mDrawerLayout.postDelayed(new Runnable() {
				@Override
				public void run() {
					mDrawerLayout.openDrawer(Gravity.LEFT);
				}
			}, 1000);

			onFirstRun();
		}


        if (savedInstanceState == null) {
            // on first time display view for first nav item
            displayView(0);
        } else {
            mDisplayedFragmentIndex = savedInstanceState.getInt("mDisplayedFragmentIndex");
            mDisplayedFragment = getFragmentManager().getFragment(savedInstanceState, "mDisplayedFragment");
            mRootFragment = getFragmentManager().getFragment(savedInstanceState, "mRootFragment");

            mDrawerList.setItemChecked(mDisplayedFragmentIndex, true);
            mDrawerList.setSelection(mDisplayedFragmentIndex);
            setTitle(mDrawerItems.get(mDisplayedFragmentIndex).text);
            getFragmentManager().popBackStack(HomeFragment.class.getName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getFragmentManager().putFragment(savedInstanceState, mRootFragment.getClass().getName(), mRootFragment);

            injectFragment(mDisplayedFragment);
        }

	}

    @Override
    protected void onSaveInstanceState(Bundle outState){
        outState.putInt("mDisplayedFragmentIndex", mDisplayedFragmentIndex);
        getFragmentManager().putFragment(outState, "mRootFragment", mRootFragment);
        getFragmentManager().putFragment(outState, "mDisplayedFragment", mDisplayedFragment);

        super.onSaveInstanceState(outState);
    }

	/**
	 * Displays the disclaimer on first run of the application
	 */
	private void onFirstRun(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(Html.fromHtml(getString(R.string.hostage_disclaimer)))
				.setCancelable(false)
				.setPositiveButton(getString(R.string.agree), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// and, if the user accept, you can execute something like this:
						// We need an Editor object to make preference changes.
						// All objects are from android.context.Context
						SharedPreferences.Editor editor = mSharedPreferences.edit();
						editor.putBoolean("isFirstRun", false);
						// Commit the edits!
						editor.commit();

                        // Enabled shared preferences for 'first' time non-portbinder activation
                        SharedPreferences.Editor editor1= mSharedPreferences.edit();
                        editor1.putBoolean("isFirstEmulation", true);
                        editor1.commit();
					}
				})
				.setNegativeButton(getString(R.string.disagree), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						getHostageService().stopListeners();
						stopAndUnbind();
						finish();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Starts the hostage service and binds this activity to the service
	 */
	public void startAndBind() {
		if (!isServiceRunning()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				context.startForegroundService(getServiceIntent());
			else
				startService(getServiceIntent());

		}

		bindService();
	}

	/**
	 * Stops the hostage service and unbinds from the service
	 */
	public void stopAndUnbind() {
		if (mHoneyService != null) {
			unbindService();
		}

		stopService(getServiceIntent());
	}

	/**
	 * Unbindes the activity from the service
	 */
	public void unbindService() {
		try {
			unbindService(mConnection);
		} catch (IllegalArgumentException ex) {
			// somehow already unbound.
		}
	}

	/**
	 * Binds the activity to the service
	 */
	public void bindService() {
		bindService(getServiceIntent(), mConnection, BIND_AUTO_CREATE);
		// mServiceBound = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Unbind running service
		if (!mHoneyService.hasRunningListeners()) {
			stopAndUnbind();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// toggle nav drawer on selecting action bar app icon/title
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		if (item.getItemId() == android.R.id.home) {
			if (!mDrawerToggle.isDrawerIndicatorEnabled()) {
				navigateBack();
			}
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Navigates up to the parent fragment of the current fragment
	 */
	public void navigateBack(){
		if (!(this.mDisplayedFragment instanceof UpNavigatibleFragment)) {
			mDrawerToggle.setDrawerIndicatorEnabled(true);
			return;
		}

		UpNavigatibleFragment upNav = (UpNavigatibleFragment) this.mDisplayedFragment;

		getFragmentManager().popBackStackImmediate(upNav.getUpFragment().getName(), 0);
		this.mDisplayedFragment = getFragmentManager().findFragmentById(R.id.content_frame);
		configureFragment();

		if (!(this.mDisplayedFragment instanceof UpNavigatibleFragment) || !((UpNavigatibleFragment) this.mDisplayedFragment).isUpNavigatible()) {
			mDrawerToggle.setDrawerIndicatorEnabled(true);
		} else {
			mDrawerToggle.setDrawerIndicatorEnabled(false);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	/**
	 * Displays the view for the given navigation index
	 *
	 * @param position the index of the navigation item
	 */
	public void displayView(int position) {
		MainMenuItem menuItemPosition = MainMenuItem.create(position);

		// close the drawer if the to be displayed fragment is already being displayed
		if (this.mDisplayedFragment != null && this.mDisplayedFragment.getClass() == menuItemPosition.getKlass()) {
			mDrawerLayout.closeDrawer(mDrawerList);
			return;
		}

		// open help video list when pressing help navigation item
		if(menuItemPosition == MainMenuItem.HELP){
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("https://www.youtube.com/playlist?list=PLJyUmtMldh3s1XtRfE4YFaQ8ME7xjf7Gx"));
			startActivity(intent);

			return;
		}

		Fragment fragment = null;

		try {
			fragment = (Fragment) menuItemPosition.getKlass().newInstance();
		} catch (InstantiationException e) {
			Log.i(menuItemPosition.getKlass().toString(), "Could not create new instance of fragment");
		} catch (IllegalAccessException e) {
			Log.i(menuItemPosition.getKlass().toString(), "Could not create new instance of fragment");
		}

		if (fragment != null) {
			if(position == 0 && mRootFragment == null){
				mRootFragment = fragment;
			}

			injectFragment(fragment);

            mDisplayedFragmentIndex = position;
			mDrawerList.setItemChecked(position, true);
			mDrawerList.setSelection(position);
			setTitle(mDrawerItems.get(position).text);
		}

		mDrawerLayout.closeDrawer(mDrawerList);
	}

	/**
	 * Injects an given fragment into the application content view
	 *
	 * @param fragment the fragment to inject
	 */
	public void injectFragment(Fragment fragment) {
		this.mCloseWarning = false;

		// set the action bar up navigation according to the nature of the given fragment
		if (fragment instanceof UpNavigatibleFragment) {
			UpNavigatibleFragment upFrag = (UpNavigatibleFragment) fragment;
			if (upFrag.getUpFragment() == null) {
				upFrag.setUpFragment(this.mDisplayedFragment.getClass());
			}
			if (upFrag.isUpNavigatible()) {
				mDrawerToggle.setDrawerIndicatorEnabled(false);
			}
		}

		configureFragment(fragment);

		// exchange the existing fragment with the given one
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.content_frame, fragment, fragment.getClass().getName());
		fragmentTransaction.addToBackStack(fragment.getClass().getName());

		fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		fragmentTransaction.commit();

		this.mDisplayedFragment = fragment;
	}


	private void configureFragment() {
		configureFragment(this.mDisplayedFragment);
	}

	/**
	 * Configures the given fragment, e.g. fixing the screen orientation
	 *
	 * @param fragment the fragment to configure
	 */
	@SuppressLint("WrongConstant")
	private void configureFragment(Fragment fragment) {
		if (fragment == null)
			return;

		if (fragment instanceof HomeFragment || fragment instanceof AboutFragment) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT | ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}

		if (fragment instanceof StatisticsFragment || fragment instanceof RecordOverviewFragment) {

			Intent intent = this.getIntent();
			intent.removeExtra(LogFilter.LOG_FILTER_INTENT_KEY);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onBackPressed() {
		if (mDisplayedFragment instanceof HomeFragment) {

			if (this.mCloseWarning) {
				MainActivity.getInstance().getHostageService().stopListeners();
				MainActivity.getInstance().stopAndUnbind();
				this.mCloseWarning = false;
				finish();
			} else {
				Toast.makeText(this, "Press the back button again to close HosTaGe", Toast.LENGTH_SHORT).show();
				this.mCloseWarning = true;
			}
			//}
		} else {
			super.onBackPressed();
			this.mDisplayedFragment = getFragmentManager().findFragmentById(R.id.content_frame);
			configureFragment();

			if (!(this.mDisplayedFragment instanceof UpNavigatibleFragment) || !((UpNavigatibleFragment) this.mDisplayedFragment).isUpNavigatible()) {
				mDrawerToggle.setDrawerIndicatorEnabled(true);
			} else {
				mDrawerToggle.setDrawerIndicatorEnabled(false);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onKeyDown(int keycode, KeyEvent e) {
		switch (keycode) {
		case KeyEvent.KEYCODE_MENU:
			if (this.mDrawerToggle.isDrawerIndicatorEnabled()) {
				if (this.mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
					this.mDrawerLayout.closeDrawer(Gravity.LEFT);
					return true;
				}
				this.mDrawerLayout.openDrawer(Gravity.LEFT);
			}
			return true;
		}

		return super.onKeyDown(keycode, e);
	}

	/**
	 * Create a new intent intented for binding the hostage service to the activity
	 *
	 * @return the new service intent
	 */
	public Intent getServiceIntent() {
		return new Intent(this, Hostage.class);
	}

	/**
	 * Retrieves the currently displayed fragment
	 *
	 * @return the current fragment
	 */
	public Fragment getDisplayedFragment() {
		return this.mDisplayedFragment;
	}

	/**
	 * Retrieves the Hostage service instance
	 * @return hostage service
	 */
	public Hostage getHostageService() {
		return this.mHoneyService;
	}

	/**
	 * Checks if the hostage service is bound to the activity
	 * @return true,  if bound
	 *         false, otherwise
	 */
	public boolean isServiceBound() {
		return this.mServiceBound;
	}

	/**
	 * Checks whether the hostage service is running
	 * @return true,  if running
	 *         false, otherwise
	 */
	public boolean isServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (service.service.getClassName().equals(Hostage.class.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Start the monitoring of the given protocols in the hostage service
	 *
	 * @param protocols the protocols to start
	 */
	public void startMonitorServices(List<String> protocols){
		for(String protocol: protocols){
			// if the given protocol is ghost start a listener for every defined port for ghost
			if(protocol.equals("GHOST")){
				if(mProfileManager.getCurrentActivatedProfile() != null){
					Profile profile = mProfileManager.getCurrentActivatedProfile();
					if(profile.mGhostActive){
						for(int port: profile.getGhostPorts()){
							if(!getHostageService().isRunning(protocol, port)) getHostageService().startListener(protocol, port);
						}
					}
				}
			} else {
				if(!getHostageService().isRunning(protocol)) getHostageService().startListener(protocol);
			}
		}
	}

	/**
	 * Holds the index of the navigation items in an enum and also a reference to an Fragment class for each item
	 */
	public enum MainMenuItem {
		HOME(0, HomeFragment.class),
		THREAT_MAP(1, ThreatMapFragment.class),
		RECORDS(2, RecordOverviewFragment.class),
		STATISTICS(3, StatisticsFragment.class),
		SERVICES(4, ServicesFragment.class),
		PROFILE_MANAGER(5, ProfileManagerFragment.class),
		SETTINGS(6, SettingsFragment.class),
		HELP(7, Class.class),
		APPLICATION_INFO(8, AboutFragment.class);

		private int value;
		private Class<?> klass;

		MainMenuItem(int value, Class<?> klass) {
			this.value = value;
			this.klass = klass;
		}

		static public MainMenuItem create(int value) {
			if (value < 0 || value >= MainMenuItem.values().length)
				return MainMenuItem.HOME;
			return MainMenuItem.values()[value];
		}

		public static boolean hasClass(Class<?> klass){
			for(MainMenuItem m: MainMenuItem.values()){
				if(m.getKlass().equals(klass)) return true;
			}

			return false;
		}

		public int getValue() {
			return value;
		}

		public Class<?> getKlass() {
			return this.klass;
		}
	}

	/**
	 * The listener for the navigation drawer items.
	 */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			displayView(position);
		}
	}




}
