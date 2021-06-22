package dk.aau.netsec.hostage.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import dk.aau.netsec.hostage.Handler;
import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.logging.LogExport;
import dk.aau.netsec.hostage.logging.LogSaveWorker;
import dk.aau.netsec.hostage.logging.RecordAll;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.sync.android.SyncUtils;
import dk.aau.netsec.hostage.ui.activity.MainActivity;
import dk.aau.netsec.hostage.ui.adapter.RecordListAdapter;
import dk.aau.netsec.hostage.ui.dialog.ChecklistDialog;
import dk.aau.netsec.hostage.ui.dialog.DateTimeDialogFragment;
import dk.aau.netsec.hostage.ui.model.ExpandableListItem;
import dk.aau.netsec.hostage.ui.model.LogFilter;
import dk.aau.netsec.hostage.ui.popup.AbstractPopupItem;
import dk.aau.netsec.hostage.ui.popup.SimplePopupItem;
import dk.aau.netsec.hostage.ui.popup.SimplePopupTable;
import dk.aau.netsec.hostage.ui.popup.SplitPopupItem;


public class RecordOverviewFragment extends UpNavigatibleFragment implements ChecklistDialog.ChecklistDialogListener, DateTimeDialogFragment.DateTimeDialogFragmentListener {
    static final String FILTER_MENU_TITLE_BSSID = MainActivity.getContext().getString(R.string.BSSID);
    static final String FILTER_MENU_TITLE_ESSID = MainActivity.getContext().getString(R.string.ESSID);
    static final String FILTER_MENU_TITLE_IPS = MainActivity.getContext().getString(R.string.RecordIP);
    static final String FILTER_MENU_TITLE_PROTOCOLS = MainActivity.getContext().getString(R.string.rec_protocol);
    static final String FILTER_MENU_TITLE_TIMESTAMP_BELOW = MainActivity.getContext().getString(
            R.string.rec_latest);
    static final String FILTER_MENU_TITLE_TIMESTAMP_ABOVE = MainActivity.getContext().getString(
            R.string.rec_earliest);
    static final String FILTER_MENU_TITLE_SORTING = MainActivity.getContext().getString(R.string.rec_sortby);
    static final String FILTER_MENU_TITLE_REMOVE = MainActivity.getContext().getString(R.string.rec_reset_filter);
    static final String FILTER_MENU_TITLE_GROUP = MainActivity.getContext().getString(
            R.string.rec_group_by);
    static final String FILTER_MENU_POPUP_TITLE = MainActivity.getContext().getString(
            R.string.rec_filter_by);
    static final int DEFAULT_GROUPING_KEY_INDEX = 0;

    private boolean wasBelowTimePicker;
    private LogFilter filter;
    private boolean showFilterButton;
    private View rootView;
    private View footer;
    private int mListPosition = -1;
    private int mItemPosition = -1;
    public String groupingKey;
    private ExpandableListView expListView;
    private ProgressBar spinner;
    private Toast noDataNotificationToast;

    private DaoSession dbSession;
    private DAOHelper daoHelper;

    private int offset = 0;
    private int limit = 20;
    private int attackRecordOffset = 0;
    private int attackRecordLimit = 999;//needs Different limit because the attackRecords are smaller than messageRecords.
    private final int realLimit = 20;
    private String sectionToOpen = "";
    private ArrayList<Integer> openSections;
    private ProgressBar progressBar;
    private SharedPreferences pref;
    Thread loader;
    private boolean mReceiverRegistered = false;
    private BroadcastReceiver mReceiver;
    private ExpandableListView mylist;
    ArrayList<RecordAll> data = new ArrayList<>();
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 3;

    /* DATE CONVERSION STUFF*/
    static final DateFormat localisedDateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    // DATE WHICH PATTERN
    static final String localDatePattern = ((SimpleDateFormat) localisedDateFormatter).toLocalizedPattern();
    static final String groupingDatePattern = "MMMM yyyy";
    // INSERT HERE YOUR DATE PATERN
    static final SimpleDateFormat groupingDateFormatter = new SimpleDateFormat(groupingDatePattern);
    static final Calendar calendar = Calendar.getInstance();
    // DATE STRINGS
    static final String TODAY = MainActivity.getInstance().getResources().getString(R.string.TODAY);
    static final String YESTERDAY = MainActivity.getInstance().getResources().getString(R.string.YESTERDAY);

    private LayoutInflater inflater;
    private ViewGroup container;
    private Bundle savedInstanceState;


    public static final int filipsRequestCode = 101;

    public static final int POSITION_EXPORT_FORMAT_PLAINTEXT = 0;
    public static final int POSITION_EXPORT_FORMAT_JSON = 1;
    public static final String LOG_EXPORT_FORMAT = "dk.aau.netsec.hostage.logging.LOG_EXPORT_FORMAT";

    /**
     * Constructor
     */
    public RecordOverviewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;
        this.container = container;
        this.savedInstanceState = savedInstanceState;

        setHasOptionsMenu(true);
        getActivity().setTitle(getResources().getString(R.string.drawer_records));
        setUpDatabase();
        getFilter();
        initializeViews(inflater, container, savedInstanceState);
        addButtons();
        this.registerBroadcastReceiver();

        return rootView;
    }


    private void setUpDatabase() {
        dbSession = HostageApplication.getInstances().getDaoSession();
        daoHelper = new DAOHelper(dbSession, getActivity());
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

    }

    private void getFilter() {
        if (this.filter == null) {
            Intent intent = this.getActivity().getIntent();
            LogFilter filter = intent.getParcelableExtra(LogFilter.LOG_FILTER_INTENT_KEY);

            if (filter == null) {
                this.clearFilter();
            } else {
                this.filter = filter;
            }
        }

    }

    private void addButtons() {
        addDeleteButton();
        addFilterButton();
        addSortButton();
        addGroupButton();
    }

    private void addDeleteButton() {
        ImageButton deleteButton = rootView.findViewById(R.id.DeleteButton);
        deleteButton.setOnClickListener(v -> RecordOverviewFragment.this.openDeleteFilteredAttacksDialog());
        deleteButton.setVisibility(this.showFilterButton ? View.VISIBLE : View.INVISIBLE);
    }

    private void addFilterButton() {
        ImageButton filterButton = rootView.findViewById(R.id.FilterButton);
        filterButton.setOnClickListener(RecordOverviewFragment.this::openFilterPopupMenuOnView);
        filterButton.setVisibility(this.showFilterButton ? View.VISIBLE : View.INVISIBLE);
    }

    private void addSortButton() {
        ImageButton sortButton = rootView.findViewById(R.id.SortButton);
        sortButton.setOnClickListener(v -> {
            // Open SortMenu
            RecordOverviewFragment.this.openSortingDialog();
        });
    }

    private void addGroupButton() {
        ImageButton groupButton = rootView.findViewById(R.id.GroupButton);
        groupButton.setOnClickListener(v -> {
            // Open SortMenu
            RecordOverviewFragment.this.openGroupingDialog();
        });
    }

    private void initializeViews(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (this.groupingKey == null)
            this.groupingKey = this.groupingTitles().get(DEFAULT_GROUPING_KEY_INDEX);

        this.setShowFilterButton(!this.filter.isNotEditable());
        View rootView = inflater.inflate(this.getLayoutId(), container, false);
        this.rootView = rootView;

        mylist = rootView.findViewById(R.id.loglistview);
        this.footer = LayoutInflater.from(getApplicationContext()).inflate(R.layout.footer_listview_progressbar, null);
        this.progressBar = footer.findViewById(R.id.progressBar);
        this.spinner = rootView.findViewById(R.id.progressBar1);
        this.spinner.setVisibility(View.GONE);

        this.expListView = mylist;
        this.initialiseListView();
        setListOnScrollListener();
    }

    /**
     * Loads the data when the user scrolls the list.
     */
    private void setListOnScrollListener() {
        expListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (!view.canScrollList(View.SCROLL_AXIS_VERTICAL) && scrollState == SCROLL_STATE_IDLE) {
                    addData();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }

        });

    }

    private void addData() {
        populateListGradually();
        expListView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);
        actualiseListViewInBackground();
        scrollOnTheBottom();
    }


    /**
     * Goes to the bottom of the list when reloads.
     */
    private void scrollOnTheBottom() {
        expListView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        expListView.setStackFromBottom(true);
    }

    private void setListViewFooter() {
        expListView.addFooterView(progressBar);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void removeListViewFooter() {
        progressBar.setVisibility(View.GONE);
        expListView.removeFooterView(progressBar);
    }

    /**
     * Initialises the expandable list view in a background thread
     */
    private void initialiseListView() {
        if (loader != null) loader.interrupt();
        if (this.openSections == null) this.openSections = new ArrayList<>();

        this.spinner.setVisibility(View.VISIBLE);
        loader = new Thread(new Runnable() {

            private void updateUI(final RecordListAdapter currentAdapter) {
                if (loader.isInterrupted()) {
                    return;
                }
                //checks null before the initialization of the Activity.
                if (getActivity() != null) {
                    Activity activity = RecordOverviewFragment.this.getActivity();

                    activity.runOnUiThread(() -> {
                        RecordOverviewFragment.this.expListView.setAdapter(currentAdapter);
                        // Update view and remove loading spinner etc...
                        getExpandableListGroups();
                        expandGroupSection();


                        if (mListPosition != -1 && mItemPosition != -1)
                            RecordOverviewFragment.this.expListView.setSelectedChild(mListPosition, mItemPosition, true);

                        mListPosition = -1;
                        mItemPosition = -1;
                        registerListClickCallback(RecordOverviewFragment.this.expListView);
                        RecordOverviewFragment.this.spinner.setVisibility(View.GONE);
                        RecordOverviewFragment.this.actualiseFilterButton();
                        RecordOverviewFragment.this.showEmptyDataNotification();
                    });
                }
            }

            private RecordListAdapter doInBackground() {
                return populateListViewFromDB(RecordOverviewFragment.this.expListView);
            }

            @Override
            public void run() {
                updateUI(doInBackground());
            }

        });

        loader.start();

        this.actualiseFilterButton();
    }

    private void getExpandableListGroups() {
        RecordListAdapter adapter = (RecordListAdapter) RecordOverviewFragment.this.expListView.getExpandableListAdapter();

        if (adapter != null) {
            adapter.notifyDataSetChanged();

            if (adapter.getGroupCount() >= 1) {
                RecordOverviewFragment.this.expListView.expandGroup(DEFAULT_GROUPING_KEY_INDEX);
                if (!RecordOverviewFragment.this.openSections.contains(DEFAULT_GROUPING_KEY_INDEX)) {
                    RecordOverviewFragment.this.openSections.add(DEFAULT_GROUPING_KEY_INDEX);
                }
            } else {
                RecordOverviewFragment.this.setSectionToOpen(RecordOverviewFragment.this.sectionToOpen);
            }
        }

    }

    private void expandGroupSection() {
        if (RecordOverviewFragment.this.openSections != null && RecordOverviewFragment.this.openSections.size() != 0) {
            for (int i = 0; i < RecordOverviewFragment.this.openSections.size(); i++) {
                int index = RecordOverviewFragment.this.openSections.get(i);
                RecordOverviewFragment.this.expListView.expandGroup(index);
            }
        } else {
            RecordOverviewFragment.this.openSections = new ArrayList<>();
        }

    }

    /**
     * Returns the Fragment layout ID
     *
     * @return int The fragment layout ID
     */
    public int getLayoutId() {
        return R.layout.fragment_record_list;
    }

    /**
     * Gets called if the user clicks on item in the filter menu.
     *
     * @param item {@link AbstractPopupItem AbstractPopupItem }
     */
    public void onFilterMenuItemSelected(AbstractPopupItem item) {
        String title = item.getTitle();

        if (item instanceof SplitPopupItem) {
            SplitPopupItem splitItem = (SplitPopupItem) item;
            if (splitItem.wasRightTouch) {
                this.openTimestampToFilterDialog();
            } else {
                this.openTimestampFromFilterDialog();
            }
            return;
        }

        if (title != null) {
            if (title.equals(FILTER_MENU_TITLE_BSSID)) {
                this.openBSSIDFilterDialog();
            }
            if (title.equals(FILTER_MENU_TITLE_ESSID)) {
                this.openESSIDFilterDialog();
            }
            if (title.equals(FILTER_MENU_TITLE_IPS)) {
                this.openIpsFilterDialog();
            }
            if (title.equals(FILTER_MENU_TITLE_PROTOCOLS)) {
                this.openProtocolsFilterDialog();
            }
            if (title.equals(FILTER_MENU_TITLE_SORTING)) {
                this.openSortingDialog();
            }
            if (title.equals(FILTER_MENU_TITLE_REMOVE)) {
                this.clearFilter();
                this.actualiseListViewInBackground();
            }
            if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_BELOW)) {
                this.openTimestampToFilterDialog();
            }
            if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_ABOVE)) {
                this.openTimestampFromFilterDialog();
            }
        }
        //return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.expListView.getExpandableListAdapter() != null) {
            if (this.expListView.getExpandableListAdapter().getGroupCount() == 1) {
                this.expListView.expandGroup(0);
            } else {
                this.setSectionToOpen(this.sectionToOpen);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (expListView != null) {
            expListView = null;
        }
        if (rootView != null) {
            unbindDrawables(rootView);
            rootView = null;
        }
        if (mReceiver != null)
            unregisterBroadcastReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mReceiver == null)
            registerBroadcastReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (expListView != null) {
            expListView = null;
        }
        if (rootView != null) {
            unbindDrawables(rootView);
            rootView = null;
        }
        if (mReceiver != null)
            unregisterBroadcastReceiver();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.records_overview_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//			case R.id.records_action_synchronize:
//				return synchronizeMenu(item);
            case R.id.records_action_export:
                AlertDialog.Builder builderExport = new AlertDialog.Builder(getActivity());
                builderExport.setTitle(MainActivity.getInstance().getString(R.string.rec_choose_export_format));
                builderExport.setItems(R.array.format, (dialog, position) -> {


                    Intent filipsIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

                    filipsIntent.setType("application/json");

                    filipsIntent.putExtra(Intent.EXTRA_TITLE, LogExport.getFileName("file", ".json"));
                    filipsIntent.putExtra(LOG_EXPORT_FORMAT, position);

//                    filipsIntent.putExtra()
                    startActivityForResult(filipsIntent, filipsRequestCode);


//				    Intent intent = new Intent(getActivity(), LogExport.class);
//                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
//
//                    intent.setAction(LogExport.ACTION_EXPORT_DATABASE);
//                    intent.putExtra(LogExport.FORMAT_EXPORT_DATABASE, position);
//
//                    RecordOverviewFragment.this.getActivity().startService(intent);
                });
                builderExport.create();
                builderExport.show();

                return true;
        }

        return false;
    }


    //Disabled for release.
    @Deprecated
    private boolean synchronizeMenu(MenuItem item) {
//        if (item.getItemId() == R.id.records_action_synchronize) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
//            builder.setTitle(MainActivity.getInstance().getString(R.string.rec_sync_rec));
//            builder.setItems(new String[]{
//                    MainActivity.getInstance().getString(R.string.rec_via_bt),
//                    "With TraCINg",
//                    "Via WifiDirect"
//            }, (dialog, position) -> {
//                switch (position) {
//                    case 0:
//                        startActivityForResult(new Intent(getBaseContext(), BluetoothSyncActivity.class), 0);
//                        break;
//                        /*case 1:
//                            getActivity().startActivity(new Intent(getActivity(), NFCSyncActivity.class));
//                            break;*/
//                    //TODO Temporary removed TracingMonitor
//                    case 1:
//                        //startActivityForResult(new Intent(getActivity(), TracingSyncActivity.class), 0);
//                        // break;
//                    case 2:
//                        startActivityForResult(new Intent(getActivity(), WiFiP2pSyncActivity.class), 0);
//                        break;
//                }
//            });
//            builder.create();
//            builder.show();
//
//            return true;
//        }
        return false;
    }

    public void openDeleteFilteredAttacksDialog() {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String deleteFILTEREDAttacksTitle = MainActivity.getInstance().getString(R.string.deleteFILTEREDAttacksTitle);
        String deleteALLAttacksTitle = MainActivity.getInstance().getString(R.string.deleteALLAttacksTitle);

        String cancelTitle = MainActivity.getInstance().getString(R.string.cancel);
        String deleteTitle = MainActivity.getInstance().getString(R.string.delete);

        String text = this.filter.isSet() ? deleteFILTEREDAttacksTitle : deleteALLAttacksTitle;

        builder.setMessage(text)
                .setPositiveButton(deleteTitle, new DialogInterface.OnClickListener() {
                    private RecordOverviewFragment recordOverviewFragment = null;

                    public void onClick(DialogInterface dialog, int id) {
                        recordOverviewFragment.deleteFilteredAttacks();
                        MainActivity.getInstance().getHostageService().notifyUI(Handler.class.toString(), null);
                    }

                    public DialogInterface.OnClickListener init(RecordOverviewFragment rf) {
                        this.recordOverviewFragment = rf;
                        return this;
                    }
                }.init(this))
                .setNegativeButton(cancelTitle, (dialog, id) -> {
                    // User cancelled the dialog
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0) {
            if (resultCode == SyncUtils.SYNC_SUCCESSFUL) {
                actualiseListViewInBackground();
            }
        } else if (requestCode == filipsRequestCode) {
            switch (resultCode) {
                case AppCompatActivity.RESULT_OK:
                    if (data != null
                            && data.getData() != null) {

                        int export_format = data.getIntExtra(LOG_EXPORT_FORMAT, POSITION_EXPORT_FORMAT_PLAINTEXT);

                        WorkRequest createLogWorkRequest = new OneTimeWorkRequest.Builder(LogSaveWorker.class)
                                .setInputData(new Data.Builder()
                                        .putString("filipsKey", "filips Awesomeeee data")
                                        .putString("filipsHorribleUri", data.getData().toString())
                                        .putInt(LOG_EXPORT_FORMAT, export_format)
                                        .build())
                                .build();
                        WorkManager.getInstance(getContext()).enqueue(createLogWorkRequest);
                    }
                    break;
                case AppCompatActivity.RESULT_CANCELED:
                    break;
            }
        }
    }


    /*****************************
     *
     * 			Public API
     *
     * ***************************/

    /**
     * Group records by SSID and expand given SSID
     *
     * @param SSID the SSID
     */
    public void showDetailsForSSID(String SSID) {
        Log.e("RecordOverviewFragment", "Implement showDetailsForSSID!!");
        this.clearFilter();
        int ESSID_INDEX = 2;
        this.sectionToOpen = SSID;
        this.groupingKey = this.groupingTitles().get(ESSID_INDEX);
    }


    /*****************************
     *
     *          ListView Stuff
     *
     * ***************************/

    /**
     * Reloads the data in the ExpandableListView for the given filter object.
     *
     * @param mylist {@link ExpandableListView ExpandableListView}
     */
    private RecordListAdapter populateListViewFromDB(ExpandableListView mylist) {
        ArrayList<String> groupTitle = new ArrayList<>();

        HashMap<String, ArrayList<ExpandableListItem>> sectionData = this.fetchDataForFilter(this.filter, groupTitle);
        RecordListAdapter adapter = null;
        if (mylist.getAdapter() != null && mylist.getAdapter() instanceof RecordListAdapter) {
            adapter = (RecordListAdapter) mylist.getAdapter();
            adapter.setData(sectionData);
            adapter.setSectionHeader(groupTitle);
        } else {
            adapter = new RecordListAdapter(groupTitle, sectionData);
        }

        return adapter;
    }

    /**
     * Offset(int): Sets the offset for query results in combination with limit(int).
     * The first offset results are skipped and the total number of results will be limited by limit(int)
     * <p>
     * limit(int): Limits the number of results returned by the query.
     * The recordSize is the number of message records.
     * <p>
     * In this method we increase the limit and the offset every time the user scrolls the list.
     */
    private void populateListGradually() {
        int recordsSize = daoHelper.getMessageRecordDAO().getRecordCount();
        long attackRecordSize = daoHelper.getAttackRecordDAO().getRecordsCount();
        setattackRecordLimits(attackRecordSize);
        changeLimitOffset(recordsSize);
    }

    private void setattackRecordLimits(long attackRecordSize) {
        changeAttackLimitOffset(attackRecordSize);
    }

    private void changeLimitOffset(long recordsSize) {
        if (offset + limit < recordsSize - 1) {
            limit += realLimit;
            //offset+=realLimit; //temporary removed because of missing records on group sidefect.
        }
    }

    private void changeAttackLimitOffset(long recordsSize) {
        if (recordsSize > 1000) {
            if (attackRecordOffset + attackRecordLimit < recordsSize - 1) {
                attackRecordLimit += realLimit;
                //attackRecordOffset += realLimit; //temporary removed because of missing records on group sidefect.
            }
        }
    }

    private HashMap<String, ArrayList<ExpandableListItem>> fetchDataForFilter(LogFilter filter, ArrayList<String> groupTitle) {
        HashMap<String, ArrayList<ExpandableListItem>> sectionData = new HashMap<String, ArrayList<ExpandableListItem>>();
        // Adding Items to ListView
        String[] keys = new String[]{RecordOverviewFragment.this.getString(R.string.RecordIP), RecordOverviewFragment.this.getString(R.string.RecordSSID), RecordOverviewFragment.this.getString(R.string.RecordProtocol), RecordOverviewFragment.this.getString(R.string.RecordTimestamp)};
        int[] ids = new int[]{R.id.RecordTextFieldBSSID, R.id.RecordTextFieldIP, R.id.RecordTextFieldProtocol, R.id.RecordTextFieldTimestamp};

        if (filter != null && !filter.protocols.isEmpty()) {
            int maxLimit = 20000;
            //The offset is always 0, so it used to set the maxLimit for the filter, to avoid missing records.
            data = daoHelper.getAttackRecordDAO().getRecordsForFilter(this.filter, limit, maxLimit, attackRecordOffset, attackRecordLimit);
        } else {
            data = daoHelper.getAttackRecordDAO().getRecordsForFilter(filter, offset, limit, attackRecordOffset, attackRecordLimit);

        }

        HashMap<String, Integer> mapping = new HashMap<>();
        int i = 0;
        for (String key : keys) {
            mapping.put(key, ids[i]);
            i++;
        }

        if (groupTitle == null) {
            groupTitle = new ArrayList<>();
        } else {
            groupTitle.clear();
        }

        for (RecordAll val : data) {
            // DO GROUPING IN HERE
            HashMap<String, String> map = new HashMap<>();
            map.put(RecordOverviewFragment.this.getString(R.string.RecordBSSID), val.getBssid());
            map.put(RecordOverviewFragment.this.getString(R.string.RecordIP), val.getRemoteIP());
            map.put(RecordOverviewFragment.this.getString(R.string.RecordProtocol), val.getProtocol());
            map.put(RecordOverviewFragment.this.getString(R.string.RecordTimestamp),
                    RecordOverviewFragment.this.getDateAsString(val.getTimestamp()));

            ExpandableListItem item = new ExpandableListItem();
            item.setData(map);

            item.setId_Mapping(mapping);

            item.setTag(val.getAttack_id());

            String groupID = RecordOverviewFragment.this.getGroupValue(val);

            ArrayList<ExpandableListItem> items = sectionData.get(groupID);
            if (items == null) {
                items = new ArrayList<>();
                sectionData.put(groupID, items);
                groupTitle.add(groupID);
            }

            items.add(item);
        }


        if (this.groupingKey.equals(this.groupingTitles().get(DEFAULT_GROUPING_KEY_INDEX))) {
            Collections.sort(groupTitle, new DateStringComparator());
        } else {
            Collections.sort(groupTitle, String::compareToIgnoreCase);
        }

        return sectionData;
    }


    /**
     * The DateStringComparator compares formatted date strings by converting the into date.
     * This class  is mainly used for grouping the records by their timestamp.
     */
    class DateStringComparator implements Comparator<String> {
        public int compare(String lhs, String rhs) {
            Date date1 = RecordOverviewFragment.this.convertStringToDate(lhs);
            Date date2 = RecordOverviewFragment.this.convertStringToDate(rhs);

            return date2.compareTo(date1);
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
                    RecordOverviewFragment.this.actualiseListViewInBackground();
                }
            };

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

    /**
     * Actualises the list in a background thread
     */
    private void actualiseListViewInBackground() {
        if (loader != null && loader.isAlive()) loader.interrupt();
        loader = null;
        setListViewFooter();
        this.actualiseFilterButton();

        loader = new Thread(new Runnable() {
            @Override
            public void run() {
                this.runOnUiThread(this.doInBackground());
            }

            private RecordListAdapter doInBackground() {
                return RecordOverviewFragment.this.populateListViewFromDB(RecordOverviewFragment.this.expListView);
            }

            private void runOnUiThread(final RecordListAdapter adapter) {
                Activity actv = RecordOverviewFragment.this.getActivity();
                if (actv != null) {
                    actv.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            this.actualiseUI();
                        }

                        private void actualiseUI() {
                            RecordOverviewFragment self = RecordOverviewFragment.this;
                            if (adapter != null) {
                                self.expListView.setAdapter(adapter);
                                adapter.notifyDataSetChanged();
                                removeListViewFooter();
                            }
                            self.showEmptyDataNotification();
                            if (self.openSections != null && self.expListView != null) {
                                for (int i = 0; i < self.openSections.size(); i++) {
                                    int index = self.openSections.get(i);
                                    try {
                                        self.expListView.expandGroup(index);
                                    } catch (IndexOutOfBoundsException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
        loader.start();
    }

    /**
     * Shows a small toast if the data to show is empty (no records).
     */
    private void showEmptyDataNotification() {
        if (RecordOverviewFragment.this.noDataNotificationToast == null) {
            RecordOverviewFragment.this.noDataNotificationToast = Toast.makeText(getApplicationContext(), R.string.no_data_notification, Toast.LENGTH_SHORT);
        }
        RecordListAdapter adapter = (RecordListAdapter) RecordOverviewFragment.this.expListView.getExpandableListAdapter();

        if (this.getFilterButton().getVisibility() == View.VISIBLE && this.filter.isSet()) {
            this.noDataNotificationToast.setText(R.string.no_data_notification);
        } else {
            this.noDataNotificationToast.setText(R.string.no_data_notification_no_filter);
        }
        if (adapter == null || adapter.getData().isEmpty())
            RecordOverviewFragment.this.noDataNotificationToast.show();

    }

    /**
     * This will open a section in the ExpandableListView with the same title as the parameter s.
     *
     * @param s String (the section title to open)
     */
    private void setSectionToOpen(String s) {
        this.sectionToOpen = s;
        if (this.sectionToOpen != null && this.sectionToOpen.length() != 0) {
            if (this.getGroupTitles().contains(this.sectionToOpen)) {
                int section = this.getGroupTitles().indexOf(this.sectionToOpen);
                this.expListView.expandGroup(section);
                this.sectionToOpen = "";
                if (!this.openSections.contains(section)) {
                    RecordOverviewFragment.this.openSections.add(section);
                }
            }
        }
    }

    /**
     * Returns the base context.
     *
     * @return Context baseContext
     */
    private Context getBaseContext() {
        return this.getActivity().getBaseContext();
    }

    /**
     * Returns the application context.
     *
     * @return Context application context
     */
    private Context getApplicationContext() {
        return this.getActivity().getApplicationContext();
    }

    /**
     * Sets the list view listener on the given ExpandableListView.
     *
     * @param mylist {@link ExpandableListView ExpandableListView }
     */
    private void registerListClickCallback(ExpandableListView mylist) {
        mylist.setOnChildClickListener((expandableListView, view, i, i2, l) -> {
            RecordListAdapter adapter = (RecordListAdapter) expandableListView.getExpandableListAdapter();

            ExpandableListItem item = (ExpandableListItem) adapter.getChild(i, i2);

            mListPosition = i;
            mItemPosition = i2;
            DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
            DAOHelper daoHelper = new DAOHelper(dbSession, getActivity());
            RecordAll rec = daoHelper.getAttackRecordDAO().getRecordOfAttackId((int) item.getTag());
            RecordOverviewFragment.this.pushRecordDetailViewForRecord(rec);
            return true;
        });
        mylist.setOnGroupExpandListener(i -> {
            if (!RecordOverviewFragment.this.openSections.contains(i)) {
                RecordOverviewFragment.this.openSections.add(i);
            }
        });
        mylist.setOnGroupCollapseListener(i -> {
            try {
                RecordOverviewFragment.this.openSections.remove(i);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        });
    }


    /*****************************
     *
     *          Date Transformation / Conversion
     *
     * ***************************/


    /**
     * Returns the localised date format for the given timestamp
     *
     * @param timeStamp long
     */
    private String getDateAsString(long timeStamp) {
        Date date = (new Date(timeStamp));
        try {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
            return formatter.format(date);
        } catch (Exception ex) {
            return "---";
        }
    }

    /**
     * Returns the timestamp in a own format.
     * Depending on the returned format the grouping by timestamp will change.
     * <p>
     * e.g.
     * If you return a DateAsMonth formatted Date the records will be mapped by their month and year.
     * If you return the a DateAsDay formatted Date the records will be mapped by their day, month and year.
     * and so on...
     *
     * @param timestamp long
     * @return formatted date String
     */
    public String getFormattedDateForGrouping(long timestamp) {
        // DECIDE WHICH KIND OF FORMAT SHOULD BE USED
        // MONTH FORMAT
        // DAY FORMAT
        //String date = this.getDateAsDayString(timestamp);

        return this.getDateAsMonthString(timestamp);
    }

    /**
     * Returns a date as a formated string
     *
     * @param timestamp date
     * @return String date format is localised
     */
    private String getDateAsDayString(long timestamp) {
        try {
            Date netDate = (new Date(timestamp));
            String dateString;

            long date = this.dayMilliseconds(timestamp);

            if (this.todayMilliseconds() == date) {
                dateString = TODAY;
            } else if (this.yesterdayMilliseconds() == date) {
                dateString = YESTERDAY;
            } else {
                dateString = localisedDateFormatter.format(netDate);
            }
            return dateString;

        } catch (Exception ex) {
            return "---";
        }
    }

    /**
     * Converts a formatted DateString into a date.
     *
     * @param dateString String
     * @return Date
     */
    private Date convertStringToDate(String dateString) {
        if (dateString != null && dateString.length() != 0) {
            SimpleDateFormat dateFormat = groupingDateFormatter; //new SimpleDateFormat(localDatePattern);
            Date date;
            try {
                if (dateString.equals(TODAY)) {
                    long millisec = RecordOverviewFragment.this.todayMilliseconds();
                    date = new Date(millisec);
                } else if (dateString.equals(YESTERDAY)) {
                    long millisec = RecordOverviewFragment.this.yesterdayMilliseconds();
                    date = new Date(millisec);
                } else {
                    date = dateFormat.parse(dateString);
                }
                return date;

            } catch (java.text.ParseException e) {
                date = new Date(0);
                return date;
            }
        } else {
            return new Date(0);
        }
    }

    /**
     * Returns the milliseconds for the day today (not the time).
     *
     * @return long
     */
    private long todayMilliseconds() {
        Date current = new Date();
        calendar.setTimeInMillis(current.getTime());
        int day = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.set(year, month, day, 0, 0, 0);

        long milli = calendar.getTimeInMillis();

        Date today = new Date(milli);

        return (milli / (long) 1000) * (long) 1000;
    }

    /**
     * Returns the milliseconds for the day yesterday (not the time).
     *
     * @return long
     */
    private long yesterdayMilliseconds() {
        Date current = new Date();
        calendar.setTimeInMillis(current.getTime());
        int day = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.set(year, month, day, 0, 0, 0);

        calendar.add(Calendar.DATE, -1);

        long milli = calendar.getTimeInMillis();

        Date today = new Date(milli);

        return (milli / (long) 1000) * (long) 1000;
    }

    /**
     * returns just the date not the time of a date.
     *
     * @param date Date
     * @return long
     */
    private long dayMilliseconds(long date) {
        //Date current = new Date();
        calendar.setTimeInMillis(date);
        int day = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.set(year, month, day, 0, 0, 0);

        long milli = calendar.getTimeInMillis();

        return (milli / (long) 1000) * (long) 1000;
    }

    /**
     * Returns a date as a formated string
     *
     * @param timeStamp date
     * @return String date format is localised
     */
    private String getDateAsMonthString(long timeStamp) {
        try {
            Date netDate = (new Date(timeStamp));
            return groupingDateFormatter.format(netDate);
        } catch (Exception ex) {
            return "xx";
        }
    }


    /*****************************
     *
     *          Getter / Setter
     *
     * ***************************/

    public boolean isShowFilterButton() {
        return showFilterButton;
    }

    public void setShowFilterButton(boolean showFilterButton) {
        this.showFilterButton = showFilterButton;
    }

    /**
     * Set the group key for grouping the records.
     * All possible grouping keys are:
     * R.string.date,
     * R.string.rec_protocol,
     * R.string.ESSID,
     * R.string.BSSID
     *
     * @param key String
     */
    public void setGroupKey(String key) {
        this.groupingKey = key;
    }

    public void setFilter(LogFilter filter) {
        this.filter = filter;
    }


    /****************************

     Open Dialog Methods
     ***************************/

    /**
     * Opens the grouping dialog
     */
    private void openGroupingDialog() {
        ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_GROUP, this.groupingTitles(), this.selectedGroup(), false, this);
        expListView.setStackFromBottom(false);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_GROUP);
    }

    /**
     * opens the bssid filter dialog
     */
    private void openBSSIDFilterDialog() {
        ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_BSSID, this.bssids(), this.selectedBSSIDs(), true, this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_BSSID);
    }

    /**
     * opens the essid filter dialog
     */
    private void openESSIDFilterDialog() {
        ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_ESSID, this.essids(), this.selectedESSIDs(), true, this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_ESSID);
    }

    /**
     * opens the ips filter dialog
     */
    private void openIpsFilterDialog() {
        ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_IPS, this.ips(), this.selectedIps(), true, this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_IPS);
    }

    /**
     * opens the protocol filter dialog
     */
    private void openProtocolsFilterDialog() {
        ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_PROTOCOLS, this.protocolTitles(), this.selectedProtocols(), true, this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_PROTOCOLS);
    }

    /**
     * opens the timestamp filter dialog (minimal timestamp required)
     */
    private void openTimestampFromFilterDialog() {
        this.wasBelowTimePicker = false;
        DateTimeDialogFragment newFragment = new DateTimeDialogFragment(this.getActivity());
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_SORTING);
        if (this.filter.aboveTimestamp != Long.MIN_VALUE)
            newFragment.setDate(this.filter.aboveTimestamp);
    }

    /**
     * opens time timestamp filter dialog (maximal timestamp required)
     */
    private void openTimestampToFilterDialog() {
        this.wasBelowTimePicker = true;
        DateTimeDialogFragment newFragment = new DateTimeDialogFragment(this.getActivity());
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_SORTING);
        if (this.filter.belowTimestamp != Long.MAX_VALUE)
            newFragment.setDate(this.filter.belowTimestamp);
    }

    /**
     * opens the sorting dialog
     */
    private void openSortingDialog() {
        ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_SORTING, this.sortTypeTiles(), this.selectedSorttype(), false, this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_SORTING);
    }

    /*****************************
     *
     *          Grouping Stuff
     *
     * ***************************/

    /**
     * returns the group title for the given record. Uses the groupingKey to decided which value of the record should be used.
     *
     * @param rec {@link RecordAll Record }
     * @return String grouptitle
     */
    public String getGroupValue(RecordAll rec) {
        int index = this.groupingTitles().indexOf(this.groupingKey);
        switch (index) {
            case 1:
                return rec.getProtocol();
            case 2:
                return rec.getRemoteIP();
            case 3:
                return rec.getSsid();
            case 4:
                return rec.getBssid();
            default:
                return this.getFormattedDateForGrouping(rec.getTimestamp());
        }
    }

    /**
     * Returns the Group titles for the specified grouping key. e.g. groupingKey is "ESSID" it returns all available essids.
     *
     * @return ArrayList<String> grouptitles
     */
    public List<String> getGroupTitles() {
        int index = this.groupingTitles().indexOf(this.groupingKey);
        switch (index) {
            case 1:
                return this.protocolTitles();
            case 2:
                return this.ips();
            case 3:
                return this.essids();
            case 4:
                return this.bssids();
            case 0:
            default:
                RecordListAdapter adapter = (RecordListAdapter) this.expListView.getExpandableListAdapter();
                if (adapter != null) {
                    return adapter.getSectionHeaders();
                }
                return new ArrayList<String>();
        }
    }


    /*****************************
     *
     *          Filter Stuff
     *
     * ***************************/

    /**
     * Returns the FilterButton.
     *
     * @return ImageButton filterButton
     */
    private ImageButton getFilterButton() {
        return (ImageButton) this.rootView.findViewById(R.id.FilterButton);
    }

    /**
     * Opens the filter menu on a anchor view. The filter menu will always be on top of the anchor.
     *
     * @param v View the anchorView
     */
    private void openFilterPopupMenuOnView(View v) {
        SimplePopupTable filterMenu = new SimplePopupTable(this.getActivity(), ob -> {
            if (ob instanceof AbstractPopupItem) {
                AbstractPopupItem item = (AbstractPopupItem) ob;
                RecordOverviewFragment.this.onFilterMenuItemSelected(item);
            }
        });
        filterMenu.setTitle(FILTER_MENU_POPUP_TITLE);
        for (String title : RecordOverviewFragment.this.filterMenuTitles()) {
            AbstractPopupItem item = null;
            if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_BELOW)) continue;
            if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_ABOVE)) {
                item = new SplitPopupItem(this.getActivity());
                item.setValue(SplitPopupItem.RIGHT_TITLE, FILTER_MENU_TITLE_TIMESTAMP_BELOW);
                item.setValue(SplitPopupItem.LEFT_TITLE, FILTER_MENU_TITLE_TIMESTAMP_ABOVE);
                if (this.filter.hasBelowTimestamp()) {
                    item.setValue(SplitPopupItem.RIGHT_SUBTITLE, this.getDateAsString(this.filter.belowTimestamp));
                }
                if (this.filter.hasAboveTimestamp()) {
                    item.setValue(SplitPopupItem.LEFT_SUBTITLE, this.getDateAsString(this.filter.aboveTimestamp));
                }
            } else {
                item = new SimplePopupItem(this.getActivity());
                item.setTitle(title);
                ((SimplePopupItem) item).setSelected(this.isFilterSetForTitle(title));
            }

            filterMenu.addItem(item);
        }
        filterMenu.showOnView(v);
    }

    /**
     * Returns true  if the filter object is set for the given title otherwise false. e.g. the filter object has protocols,
     * so the method will return for the title FILTER_MENU_TITLE_PROTOCOLS TRUE.
     *
     * @param title String
     * @return boolean value
     */
    private boolean isFilterSetForTitle(String title) {
        if (title.equals(FILTER_MENU_TITLE_BSSID)) {
            return this.filter.hasBSSIDs();
        }
        if (title.equals(FILTER_MENU_TITLE_ESSID)) {
            return this.filter.hasESSIDs();
        }
        if (title.equals(FILTER_MENU_TITLE_IPS)) {
            return this.filter.hasIps();
        }
        if (title.equals(FILTER_MENU_TITLE_PROTOCOLS)) {
            return this.filter.hasProtocols();
        }
        if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_BELOW)) {
            return this.filter.hasBelowTimestamp();
        }
        if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_ABOVE)) {
            return this.filter.hasAboveTimestamp();
        }
        return false;
    }

    /**
     * clears the filter. Does not invoke populatelistview!
     */
    private void clearFilter() {
        if (filter == null) this.filter = new LogFilter();
        this.filter.clear();
    }

    /**
     * Returns all grouping titles.
     *
     * @return ArrayList<String> tiles
     */
    public ArrayList<String> groupingTitles() {
        ArrayList<String> titles = new ArrayList<String>();
        titles.add(MainActivity.getContext().getString(R.string.date));
        titles.add(MainActivity.getContext().getString(R.string.rec_protocol));
        titles.add(MainActivity.getContext().getString(R.string.IP));
        titles.add(MainActivity.getContext().getString(R.string.ESSID));
        titles.add(MainActivity.getContext().getString(R.string.BSSID));
        return titles;
    }

    /**
     * Returns a bool array. This array is true at the index of the groupingKey in groupingTitles(), otherwise false.
     *
     * @return boolean[] selection
     */
    public boolean[] selectedGroup() {
        ArrayList<String> groups = this.groupingTitles();
        boolean[] selected = new boolean[groups.size()];
        int i = 0;
        for (String group : groups) {
            selected[i] = (group.equals(this.groupingKey));
            i++;
        }
        return selected;
    }

    /**
     * Returns all protocol titles / names.
     *
     * @return ArrayList<String> protocolTitles
     */
    public ArrayList<String> protocolTitles() {
        ArrayList<String> titles = new ArrayList<>();
        for (String protocol : this.getResources().getStringArray(
                R.array.protocols)) {
            titles.add(protocol);
        }

        titles.add("PORTSCAN");
        titles.add("FILE INJECTION");
        titles.add("MULTISTAGE ATTACK");
        return titles;
    }

    /**
     * Return a boolean array of the selected / filtered protocols. If the filter object has
     * an protocol from the protocolTitles() array, the index of it will be true, otherwise false.
     *
     * @return boolean[] protocol selection
     */
    public boolean[] selectedProtocols() {
        ArrayList<String> protocols = this.protocolTitles();
        boolean[] selected = new boolean[protocols.size()];

        int i = 0;
        for (String protocol : protocols) {
            selected[i] = (this.filter.protocols.contains(protocol));
            i++;
        }
        return selected;
    }

    /**
     * Returns the Sorttype Titles
     *
     * @return ArayList<String> Sort type titles
     */
    public ArrayList<String> sortTypeTiles() {
        ArrayList<String> titles = new ArrayList<>();
        titles.add(MainActivity.getContext().getString(R.string.rec_time));
        titles.add(MainActivity.getContext().getString(R.string.rec_protocol));
        titles.add(MainActivity.getContext().getString(R.string.IP));
        titles.add(MainActivity.getContext().getString(R.string.ESSID));
        titles.add(MainActivity.getContext().getString(R.string.BSSID));
        return titles;
    }

    /**
     * Returns an boolean array. The array is true at the index of the selected sort type..
     * The index of the selected sort type is the same index in the sortTypeTiles array.
     *
     * @return boolean array, length == sortTypeTiles().length
     */
    public boolean[] selectedSorttype() {
        ArrayList<String> types = this.sortTypeTiles();
        boolean[] selected = new boolean[types.size()];
        int i = 0;
        for (String sorttype : types) {
            selected[i] = (this.filter.sorttype.toString().equals(sorttype));
            i++;
        }
        return selected;
    }

    /**
     * Returns all unique bssids.
     *
     * @return ArrayList<String>
     */
    public ArrayList<String> bssids() {
        return daoHelper.getNetworkRecordDAO().getUniqueBSSIDRecords();
    }

    /**
     * Returns an boolean array. The array is true at the indices of the selected bssids.
     * The index of the selected bssid is the same index in the bssids() array.
     *
     * @return boolean array, length == bssids().length
     */
    public boolean[] selectedBSSIDs() {
        ArrayList<String> bssids = this.bssids();
        boolean[] selected = new boolean[bssids.size()];

        int i = 0;
        for (String bssid : bssids) {
            selected[i] = (this.filter.BSSIDs.contains(bssid));
            i++;
        }
        return selected;
    }

    /**
     * Returns all unique essids.
     *
     * @return ArrayList<String>
     */
    public ArrayList<String> essids() {
        return daoHelper.getNetworkRecordDAO().getUniqueESSIDRecords();
    }

    /**
     * Returns all unique ips.
     *
     * @return ArrayList<String>
     */
    public ArrayList<String> ips() {
        return daoHelper.getAttackRecordDAO().getUniqueIPRecords();
    }

    /**
     * Returns an boolean array. The array is true at the indices of the selected essids.
     * The index of the selected essid is the same index in the essids() array.
     *
     * @return boolean array, length == essids().length
     */
    public boolean[] selectedESSIDs() {
        ArrayList<String> essids = this.essids();
        boolean[] selected = new boolean[essids.size()];

        int i = 0;
        for (String essid : essids) {
            selected[i] = (this.filter.ESSIDs.contains(essid));
            i++;
        }
        return selected;
    }

    /**
     * Returns an boolean array. The array is true at the indices of the selected ips.
     * The index of the selected ip is the same index in the ipss() array.
     *
     * @return boolean array, length == ips().length
     */
    public boolean[] selectedIps() {
        ArrayList<String> ips = this.ips();
        boolean[] selected = new boolean[ips.size()];

        int i = 0;
        for (String ip : ips) {
            selected[i] = (this.filter.IPs.contains(ip));
            i++;
        }
        return selected;
    }

    /**
     * Returns all filter menu titles.
     *
     * @return ArrayList<String>
     */
    private ArrayList<String> filterMenuTitles() {
        ArrayList<String> titles = new ArrayList<>();
        titles.add(FILTER_MENU_TITLE_BSSID);
        titles.add(FILTER_MENU_TITLE_ESSID);
        titles.add(FILTER_MENU_TITLE_IPS);
        titles.add(FILTER_MENU_TITLE_PROTOCOLS);
        titles.add(FILTER_MENU_TITLE_TIMESTAMP_ABOVE);
        titles.add(FILTER_MENU_TITLE_TIMESTAMP_BELOW);
        if (this.filter.isSet()) titles.add(FILTER_MENU_TITLE_REMOVE);
        return titles;
    }

    /*****************************
     *
     *          Listener Actions
     *
     * ***************************/

    /**
     * Will be called if the users selects a timestamp.
     *
     * @param dialog {@link DateTimeDialogFragment DateTimeDialogFragment }
     */
    public void onDateTimePickerPositiveClick(DateTimeDialogFragment dialog) {
        if (this.wasBelowTimePicker) {
            this.filter.setBelowTimestamp(dialog.getDate());
        } else {
            this.filter.setAboveTimestamp(dialog.getDate());
        }
        this.actualiseListViewInBackground();
        this.actualiseFilterButton();
    }

    /**
     * Will be called if the users cancels a timestamp selection.
     *
     * @param dialog {@link DateTimeDialogFragment DateTimeDialogFragment }
     */
    public void onDateTimePickerNegativeClick(DateTimeDialogFragment dialog) {
        if (this.wasBelowTimePicker) {
            this.filter.setBelowTimestamp(Long.MAX_VALUE);
        } else {
            this.filter.setAboveTimestamp(Long.MIN_VALUE);
        }
        this.actualiseListViewInBackground();
        this.actualiseFilterButton();
    }

    /**
     * Will be called if the users clicks the positiv button on a ChechlistDialog.
     *
     * @param dialog {@link ChecklistDialog ChecklistDialog }
     */
    public void onDialogPositiveClick(ChecklistDialog dialog) {
        String title = dialog.getTitle();
        if (title.equals(FILTER_MENU_TITLE_BSSID)) {
            ArrayList<String> titles = dialog.getSelectedItemTitles();
            if (titles.size() == this.bssids().size()) {
                this.filter.setBSSIDs(new ArrayList<>());
            } else {
                this.filter.setBSSIDs(titles);
            }
        }
        if (title.equals(FILTER_MENU_TITLE_ESSID)) {
            ArrayList<String> titles = dialog.getSelectedItemTitles();
            if (titles.size() == this.essids().size()) {
                this.filter.setESSIDs(new ArrayList<>());
            } else {
                this.filter.setESSIDs(titles);
            }
        }

        if (title.equals(FILTER_MENU_TITLE_IPS)) {
            ArrayList<String> titles = dialog.getSelectedItemTitles();
            if (titles.size() == this.ips().size()) {
                this.filter.setIps(new ArrayList<>());
            } else {
                this.filter.setIps(titles);
            }
        }
        if (title.equals(FILTER_MENU_TITLE_PROTOCOLS)) {
            ArrayList<String> protocols = dialog.getSelectedItemTitles();
            if (protocols.size() == this.protocolTitles().size()) {
                this.filter.setProtocols(new ArrayList<>());
            } else {
                this.filter.setProtocols(dialog.getSelectedItemTitles());
            }
        }
        if (title.equals(FILTER_MENU_TITLE_SORTING)) {
            ArrayList<String> titles = dialog.getSelectedItemTitles();
            if (titles.size() == 0) return;
            // ALWAYS GET THE FIRST ELEMENT (SHOULD BE ALWAYS ONE)
            String t = titles.get(0);
            int sortType = this.sortTypeTiles().indexOf(t);
            this.filter.setSorttype(LogFilter.SortType.values()[sortType]);
        }
        if (title.equals(FILTER_MENU_TITLE_GROUP)) {
            ArrayList<String> titles = dialog.getSelectedItemTitles();
            if (titles.size() == 0) return;
            // ALWAYS GET THE FIRST ELEMENT (SHOULD BE ALWAYS ONE)
            this.groupingKey = titles.get(0);
        }
        this.actualiseListViewInBackground();

        this.actualiseFilterButton();
    }

    /**
     * Paints the filter button if the current filter object is set.
     */
    private void actualiseFilterButton() {
        if (this.filter.isSet()) {
            ImageButton filterButton = this.getFilterButton();
            if (filterButton != null) {
                filterButton.setImageResource(R.drawable.ic_filter_pressed);
                filterButton.invalidate();
            }
        } else {
            ImageButton filterButton = this.getFilterButton();
            if (filterButton != null) {
                filterButton.setImageResource(R.drawable.ic_filter);
                filterButton.invalidate();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LogExport.exportDatabase(LogExport.formatter);

            } else {
                androidx.appcompat.app.AlertDialog.Builder dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                dialog.setTitle("Permission Required");
                dialog.setMessage("If you don't allow the permission to access External Storage you won't be able to extract any records.");
                dialog.setPositiveButton("Settings", (dialog1, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getApplicationContext().getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                });
                dialog.setNegativeButton("No, thanks", (dialog1, which) -> {
                });
                androidx.appcompat.app.AlertDialog alertDialog = dialog.create();
                alertDialog.show();
            }
        }
    }

    /**
     * Deletes the current displayed attacks.
     */
    public void deleteFilteredAttacks() {
        LogFilter filter = this.filter;
        daoHelper.getAttackRecordDAO().deleteAttacksByFilter(filter);
        this.actualiseListViewInBackground();
    }

    /**
     * Will be called if the users clicks the negativ button on a ChechlistDialog.
     *
     * @param dialog {@link ChecklistDialog ChecklistDialog }
     */
    public void onDialogNegativeClick(ChecklistDialog dialog) {
    }


    /**
     * Navigation. Shows the record detail view for the given record
     *
     * @param record {@link RecordAll Record } to show
     */
    private void pushRecordDetailViewForRecord(RecordAll record) {
        FragmentManager fm = this.getActivity().getFragmentManager();

        if (fm != null) {
            RecordDetailFragment newFragment = new RecordDetailFragment();
            newFragment.setRecord(record);
            newFragment.setUpNavigatible(true);
            MainActivity.getInstance().injectFragment(newFragment);
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