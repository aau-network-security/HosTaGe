package dk.aau.netsec.hostage.ui.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.AsyncTask;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.location.CustomLocationManager;
import dk.aau.netsec.hostage.location.LocationException;
import dk.aau.netsec.hostage.persistence.ProfileManager;
import dk.aau.netsec.hostage.system.Device;
import dk.aau.netsec.hostage.system.iptablesUtils.Api;
import dk.aau.netsec.hostage.ui.adapter.DrawerListAdapter;
import dk.aau.netsec.hostage.ui.fragment.AboutFragment;
import dk.aau.netsec.hostage.ui.fragment.HomeFragment;
import dk.aau.netsec.hostage.ui.fragment.PrivacyFragment;
import dk.aau.netsec.hostage.ui.fragment.ProfileManagerFragment;
import dk.aau.netsec.hostage.ui.fragment.RecordOverviewFragment;
import dk.aau.netsec.hostage.ui.fragment.ServicesFragment;
import dk.aau.netsec.hostage.ui.fragment.SettingsFragment;
import dk.aau.netsec.hostage.ui.fragment.StatisticsFragment;
import dk.aau.netsec.hostage.ui.fragment.ThreatMapFragment;
import dk.aau.netsec.hostage.ui.fragment.UpNavigableFragment;
import dk.aau.netsec.hostage.ui.fragment.opengl.ThreatIndicatorGLRenderer;
import dk.aau.netsec.hostage.ui.model.DrawerListItem;
import dk.aau.netsec.hostage.ui.model.LogFilter;
import eu.chainfire.libsuperuser.Shell;


/**
 * Manages the whole application, and should act like a singleton.
 *
 * @author Alexander Brakowski
 * @created 12.01.14 23:24
 */
public class MainActivity extends AppCompatActivity {
    private static WeakReference<Context> context;

    private CustomLocationManager customLocationManager;

    /**
     * singleton instance of the MainActivity with WeakReference to avoid Memory leaks
     */
    private static WeakReference<MainActivity> mActivityRef;

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
    private androidx.appcompat.app.ActionBarDrawerToggle mDrawerToggle;

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
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    public static final int LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE = 101;


    /**
     * Connection to bind the background service
     *
     * @see Hostage
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
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
     * M
     *
     * @return MainActivity - the singleton instance
     */
    public static MainActivity getInstance() {
        return mActivityRef.get();
    }

    /**
     * Retrieves the context of the application
     *
     * @return the context
     */
    public static Context getContext() {
        return context.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
        super.onStart();
        // Register syncing with android
        //SyncUtils.CreateSyncAccount(this);

        if (isServiceRunning()) {
            this.bindService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if (locationManager != null)
//            locationManager.stopUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        super.onStop();
        this.unbindService();
//        if (locationManager != null)
//            locationManager.stopUpdates();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (locationManager != null) {
//            locationManager.stopUpdates();
//            locationManager = null;
//        }
        // /Unbind running service
        if (!mHoneyService.hasRunningListeners()) {
            stopAndUnbind();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // make the main activity a singleton
        mActivityRef = new WeakReference<>(this);

        // sets the static context reference to the application context
        context = new WeakReference<>(getApplicationContext());
        setContentView(R.layout.activity_drawer_main);

        addAnimation();
        // configures the action bar
        configureActionBar();
        loadDrawer();
        executeRoot();

        loadFirstRun();

        if (savedInstanceState == null) {
            // on first time display view for first nav item
            displayView(0);
        } else {
            mDisplayedFragmentIndex = savedInstanceState.getInt("mDisplayedFragmentIndex");
            mDisplayedFragment = getSupportFragmentManager().getFragment(savedInstanceState, "mDisplayedFragment");
            mRootFragment = getSupportFragmentManager().getFragment(savedInstanceState, "mRootFragment");

            mDrawerList.setItemChecked(mDisplayedFragmentIndex, true);
            mDrawerList.setSelection(mDisplayedFragmentIndex);
            setTitle(mDrawerItems.get(mDisplayedFragmentIndex).text);
            getSupportFragmentManager().popBackStack(HomeFragment.class.getName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getSupportFragmentManager().putFragment(savedInstanceState, mRootFragment.getClass().getName(), mRootFragment);

            injectFragment(mDisplayedFragment);
        }
    }

    private void executeRoot() {
        CheckRoot checkRoot = new CheckRoot();
        checkRoot.execute();
    }

    private static void checkForRoot() {
        if (Shell.SU.available()) {
            Device.checkCapabilities();
            if (Api.assertBinaries(getContext(), true)) {
                try {
                    Api.executeCommands();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //Device.executePortRedirectionScript(); //not working with Samsungs
            }
        }
    }

    private static class CheckRoot extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            checkForRoot();
            return null;
        }
    }

    private void addAnimation() {
        // init threat indicator animation
        ThreatIndicatorGLRenderer.setThreatLevel(ThreatIndicatorGLRenderer.ThreatLevel.NOT_MONITORING);

        // set background color
        TypedArray arr = getTheme().obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
        ThreatIndicatorGLRenderer.setBackgroundColor(arr.getColor(0, 0xFFFFFF));
        arr.recycle();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("mDisplayedFragmentIndex", mDisplayedFragmentIndex);
        getSupportFragmentManager().putFragment(outState, "mRootFragment", mRootFragment);
        getSupportFragmentManager().putFragment(outState, "mDisplayedFragment", mDisplayedFragment);

        super.onSaveInstanceState(outState);
    }

    /**
     * Displays the disclaimer on first run of the application
     */
    private void onFirstRun() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(Html.fromHtml(getString(R.string.hostage_disclaimer)))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.agree), (dialog, id) -> {
                    // and, if the user accept, you can execute something like this:
                    // We need an Editor object to make preference changes.
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putBoolean("isFirstRun", false);
                    editor.apply();

                    // Enabled shared preferences for 'first' time non-portbinder activation
                    SharedPreferences.Editor editor1 = mSharedPreferences.edit();
                    editor1.putBoolean("isFirstEmulation", true);
                    editor1.apply();

                    getLocationData();
                    startAndBind();
                    addProfileManager();

                })
                .setNegativeButton(getString(R.string.disagree), (dialog, id) -> {
                    getHostageService().stopListeners();
                    stopAndUnbind();
                    finish();
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void addProfileManager() {
        mProfileManager = ProfileManager.getInstance();
    }

    private void loadFirstRun() {
        mSharedPreferences = getSharedPreferences(getString(R.string.shared_preference_path), Hostage.MODE_PRIVATE);
        if (mSharedPreferences.getBoolean("isFirstRun", true)) {
            onFirstRun();
        } else {
            getLocationData();
            startAndBind();
            addProfileManager();
        }
    }

    private void configureActionBar() {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
    }

    private void loadDrawer() {
        // sets the drawer and action title to the application title
        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerList = findViewById(R.id.left_drawer);

        // propagates the navigation drawer with items
        mDrawerItems = new ArrayList<>();
        mDrawerItems.add(new DrawerListItem(R.string.drawer_overview, R.drawable.ic_menu_home));
        mDrawerItems.add(new DrawerListItem(R.string.drawer_threat_map, R.drawable.ic_menu_mapmode));
        mDrawerItems.add(new DrawerListItem(R.string.drawer_records, R.drawable.ic_menu_records));
        mDrawerItems.add(new DrawerListItem(R.string.drawer_statistics, R.drawable.ic_menu_stats));
        mDrawerItems.add(new DrawerListItem(R.string.drawer_services, R.drawable.ic_menu_set_as));
        mDrawerItems.add(new DrawerListItem(R.string.drawer_profile_manager, R.drawable.ic_menu_allfriends));
        mDrawerItems.add(new DrawerListItem(R.string.drawer_settings, R.drawable.ic_menu_preferences));
        mDrawerItems.add(new DrawerListItem(R.string.drawer_app_info, R.drawable.ic_menu_info_details));
        mDrawerItems.add(new DrawerListItem(R.string.privacy_policy, R.drawable.ic_menu_privacy));

        DrawerListAdapter listAdapter = new DrawerListAdapter(this, mDrawerItems);

        mDrawerList.setAdapter(listAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        setmDrawerToggle();
    }

    private void setmDrawerToggle() {
        // configures the navigation drawer
        mDrawerToggle = new androidx.appcompat.app.ActionBarDrawerToggle(this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                R.string.drawer_open, /* "open drawer" description for accessibility */
                R.string.drawer_close /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    /**
     * Get latest location data. If needed, this will trigger a location permission request.
     */
    private void getLocationData() {
        try {
            customLocationManager = CustomLocationManager.getLocationManagerInstance(this);
            customLocationManager.getLatestLocation();

        } catch (LocationException le) {
            le.printStackTrace();
        }
    }

    /**
     * Starts the hostage service and binds this activity to the service
     */
    public void startAndBind() {
        if (!isServiceRunning()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                getContext().startForegroundService(getServiceIntent());
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
     * Unbinds the activity from the service
     */
    public void unbindService() {
        try {
            unbindService(mConnection);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
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
    public void navigateBack() {
        if (!(this.mDisplayedFragment instanceof UpNavigableFragment)) {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            return;
        }

        UpNavigableFragment upNav = (UpNavigableFragment) this.mDisplayedFragment;

        getSupportFragmentManager().popBackStackImmediate(upNav.getUpFragment().getName(), 0);
        this.mDisplayedFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        configureFragment();

        mDrawerToggle.setDrawerIndicatorEnabled(!(this.mDisplayedFragment instanceof UpNavigableFragment) || !((UpNavigableFragment) this.mDisplayedFragment).isUpNavigable());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
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
        // Pass any configuration change to the drawer toggle
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

        Fragment fragment = null;

        try {
            if (menuItemPosition.getKlass() == ServicesFragment.class && !mHoneyService.isImplementedProtocolsReady()) {

                mDrawerLayout.closeDrawer(mDrawerList);
                Snackbar.make(mDrawerLayout, R.string.services_unavailable, Snackbar.LENGTH_LONG).show();
                return;
            }

            fragment = (Fragment) menuItemPosition.getKlass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            Log.i(menuItemPosition.getKlass().toString(), "Could not create new instance of fragment");
        }

        if (fragment != null) {
            if (position == 0 && mRootFragment == null) {
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
        if (fragment instanceof UpNavigableFragment) {
            UpNavigableFragment upFrag = (UpNavigableFragment) fragment;
            if (upFrag.getUpFragment() == null) {
                upFrag.setUpFragment(this.mDisplayedFragment.getClass());
            }
            if (upFrag.isUpNavigable()) {
                mDrawerToggle.setDrawerIndicatorEnabled(false);
            }
        }

        configureFragment(fragment);

        // exchange the existing fragment with the given one
        FragmentManager fragmentManager = getSupportFragmentManager();
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
                System.exit(0);
            } else {

                Snackbar.make(mDisplayedFragment.getView(), R.string.close_app_warning, Snackbar.LENGTH_SHORT).show();
                this.mCloseWarning = true;
            }
        } else {
            super.onBackPressed();
            this.mDisplayedFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            configureFragment();

            mDrawerToggle.setDrawerIndicatorEnabled(!(this.mDisplayedFragment instanceof UpNavigableFragment) || !((UpNavigableFragment) this.mDisplayedFragment).isUpNavigable());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        if (keycode == KeyEvent.KEYCODE_MENU) {
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
     * Create a new intent intended for binding the hostage service to the activity
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
     *
     * @return hostage service
     */
    public Hostage getHostageService() {
        return this.mHoneyService;
    }

    /**
     * Checks if the hostage service is bound to the activity
     *
     * @return true,  if bound
     * false, otherwise
     */
    public boolean isServiceBound() {
        return this.mServiceBound;
    }

    /**
     * Checks whether the hostage service is running
     *
     * @return true,  if running
     * false, otherwise
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
    public void startMonitorServices(List<String> protocols) {
        if (mHoneyService.isImplementedProtocolsReady()) {
            for (String protocol : protocols) {
                if (!getHostageService().isRunning(protocol))

                    getHostageService().startListener(protocol);
            }
        } else {
            Snackbar.make(MainActivity.getInstance().getDisplayedFragment().getView(), R.string.services_unavailable, Snackbar.LENGTH_SHORT).show();
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
        APPLICATION_INFO(7, AboutFragment.class),
        PRIVACY(8, PrivacyFragment.class);


        private final int value;
        private final Class<?> klass;

        MainMenuItem(int value, Class<?> klass) {
            this.value = value;
            this.klass = klass;
        }

        static public MainMenuItem create(int value) {
            if (value < 0 || value >= MainMenuItem.values().length)
                return MainMenuItem.HOME;
            return MainMenuItem.values()[value];
        }

        public static boolean hasClass(Class<?> klass) {
            for (MainMenuItem m : MainMenuItem.values()) {
                if (m.getKlass().equals(klass)) return true;
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

    /**
     * Callback after location permission has been requested. If foreground location permission has
     * been requested, notify {@link CustomLocationManager} if it has been granted or not.
     * <p>
     * If on API 29 and above, request also background location permission.
     *
     * @param requestCode  Request code to identify the calling request
     * @param permissions  Type of permission that was requested
     * @param grantResults Request result (granted or not)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    customLocationManager.permissionGrantedCallback();

                    //Only needed on Android API >= 29
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        customLocationManager.requestBackgroundLocation();
                    }
                } else {
                    customLocationManager.userHasDeniedLocation(true);
                }
                break;
            }
            case LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE: {
                // We currently do nothing more after we have requested background location permission
                break;
            }
        }
    }

}
