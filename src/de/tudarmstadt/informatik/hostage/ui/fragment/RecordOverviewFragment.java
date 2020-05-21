package de.tudarmstadt.informatik.hostage.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

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
import java.util.Random;

import de.tudarmstadt.informatik.hostage.Handler;
import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.LogExport;
import de.tudarmstadt.informatik.hostage.logging.MessageRecord;
import de.tudarmstadt.informatik.hostage.logging.NetworkRecord;
import de.tudarmstadt.informatik.hostage.logging.Record;
import de.tudarmstadt.informatik.hostage.logging.SyncData;
import de.tudarmstadt.informatik.hostage.logging.SyncInfo;
import de.tudarmstadt.informatik.hostage.persistence.HostageDBOpenHelper;
import de.tudarmstadt.informatik.hostage.sync.android.SyncUtils;
import de.tudarmstadt.informatik.hostage.sync.bluetooth.BluetoothSyncActivity;
import de.tudarmstadt.informatik.hostage.sync.nfc.NFCSyncActivity;
import de.tudarmstadt.informatik.hostage.sync.tracing.TracingSyncActivity;
import de.tudarmstadt.informatik.hostage.sync.wifi_direct.ui.WiFiP2pSyncActivity;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.adapter.RecordListAdapter;
import de.tudarmstadt.informatik.hostage.ui.dialog.ChecklistDialog;
import de.tudarmstadt.informatik.hostage.ui.dialog.DateTimeDialogFragment;
import de.tudarmstadt.informatik.hostage.ui.model.ExpandableListItem;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter.SortType;
import de.tudarmstadt.informatik.hostage.ui.model.ServicesListItem;
import de.tudarmstadt.informatik.hostage.ui.popup.AbstractPopup;
import de.tudarmstadt.informatik.hostage.ui.popup.AbstractPopupItem;
import de.tudarmstadt.informatik.hostage.ui.popup.SimplePopupItem;
import de.tudarmstadt.informatik.hostage.ui.popup.SimplePopupTable;
import de.tudarmstadt.informatik.hostage.ui.popup.SplitPopupItem;

public class RecordOverviewFragment extends UpNavigatibleFragment implements ChecklistDialog.ChecklistDialogListener, DateTimeDialogFragment.DateTimeDialogFragmentListener {

	static final String FILTER_MENU_TITLE_BSSID = MainActivity.getContext().getString(R.string.BSSID);
	static final String FILTER_MENU_TITLE_ESSID = MainActivity.getContext().getString(R.string.ESSID);
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

    private int mListPosition = -1;
    private int mItemPosition = -1;

    public String groupingKey;

    private ExpandableListView expListView;
    private ProgressBar spinner;

    private Toast noDataNotificationToast;

    HostageDBOpenHelper dbh;

    private String sectionToOpen = "";
    private ArrayList<Integer> openSections;

	private SharedPreferences pref;

    Thread loader;

    private boolean mReceiverRegistered = false;
    private BroadcastReceiver mReceiver;



    /* DATE CONVERSION STUFF*/
    static final DateFormat localisedDateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    // DATE WHICH PATTERN
    static final String localDatePattern  = ((SimpleDateFormat)localisedDateFormatter).toLocalizedPattern();
    static final String groupingDatePattern  = "MMMM yyyy";

    // INSERT HERE YOUR DATE PATERN
    static final SimpleDateFormat groupingDateFormatter = new SimpleDateFormat(groupingDatePattern);
    static final Calendar calendar = Calendar.getInstance();

    // DATE STRINGS
    static final String TODAY = MainActivity.getInstance().getResources().getString( R.string.TODAY);
    static final String YESTERDAY = MainActivity.getInstance().getResources().getString( R.string.YESTERDAY);


    private SyncInfo si ;// = s.getSyncInfo();
    private SyncData sd ;//= s.getSyncData(si);

    /**
     * Constructor
     */
    public RecordOverviewFragment(){}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);
		getActivity().setTitle(getResources().getString(R.string.drawer_records));

		dbh = new HostageDBOpenHelper(this.getActivity().getBaseContext());
	    pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

	    // Get the message from the intent
        //this.addRecordToDB(4,4,4);
        /*
         Synchronizer s = new Synchronizer(this.dbh);
         si = s.getSyncInfo();
        HashMap<String, Long> map = new HashMap<String, Long>();
        map.put(SyncDevice.currentDevice().getDeviceID(), new Long(-1));
        si.deviceMap = map;
         sd = s.getSyncData(si);

         s.updateFromSyncData(sd);
         */
        if (this.filter == null){
            Intent intent = this.getActivity().getIntent();
            LogFilter filter = intent.getParcelableExtra(LogFilter.LOG_FILTER_INTENT_KEY);

            if(filter == null){
                this.clearFilter();
            } else {
                this.filter = filter;
            }
        }

        if (this.groupingKey == null) this.groupingKey = this.groupingTitles().get(DEFAULT_GROUPING_KEY_INDEX);

	    this.setShowFilterButton(!this.filter.isNotEditable());

		View rootView = inflater.inflate(this.getLayoutId(), container, false);
        this.rootView = rootView;
		ExpandableListView mylist = rootView.findViewById(R.id.loglistview);

        this.spinner = rootView.findViewById(R.id.progressBar1);
        this.spinner.setVisibility(View.GONE);

		this.expListView = mylist;

        this.initialiseListView();

        ImageButton deleteButton = rootView.findViewById(R.id.DeleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RecordOverviewFragment.this.openDeleteFilteredAttacksDialog();
            }
        });
        deleteButton.setVisibility(this.showFilterButton? View.VISIBLE : View.INVISIBLE);

        ImageButton filterButton = rootView.findViewById(R.id.FilterButton);
        filterButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	RecordOverviewFragment.this.openFilterPopupMenuOnView(v);
            }
        });
        filterButton.setVisibility(this.showFilterButton? View.VISIBLE : View.INVISIBLE);


        ImageButton sortButton = rootView.findViewById(R.id.SortButton);
        sortButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Open SortMenu
                RecordOverviewFragment.this.openSortingDialog();
            }
        });

        ImageButton groupButton = rootView.findViewById(R.id.GroupButton);
        groupButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Open SortMenu
                RecordOverviewFragment.this.openGroupingDialog();
            }
        });

        this.registerBroadcastReceiver();

		return rootView;
	 }


    /**Initialises the expandable list view in a backgorund thread*/
    private void initialiseListView(){
        if (loader != null) loader.interrupt();
        if (this.openSections == null) this.openSections = new ArrayList<Integer>();

        this.spinner.setVisibility(View.VISIBLE);

        loader = new Thread(new Runnable(){

            private void updateUI(final RecordListAdapter currentAdapter)
            {
                if(loader.isInterrupted()){
                    return;
                }
                //checks null before the initialization of the Activity.
                if (getActivity() != null){
                    Activity activity = RecordOverviewFragment.this.getActivity();

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            RecordOverviewFragment.this.expListView.setAdapter(currentAdapter);
                            // Update view and remove loading spinner etc...
                            RecordListAdapter adapter = (RecordListAdapter) RecordOverviewFragment.this.expListView.getExpandableListAdapter();

                            if (adapter != null){
                                adapter.notifyDataSetChanged();

                                if (adapter.getGroupCount() >= 1){
                                    RecordOverviewFragment.this.expListView.expandGroup(DEFAULT_GROUPING_KEY_INDEX);
                                    if (!RecordOverviewFragment.this.openSections.contains(DEFAULT_GROUPING_KEY_INDEX)){
                                        RecordOverviewFragment.this.openSections.add(DEFAULT_GROUPING_KEY_INDEX);
                                    }
                                } else {
                                    RecordOverviewFragment.this.setSectionToOpen(RecordOverviewFragment.this.sectionToOpen);
                                }
                            }

                            if (RecordOverviewFragment.this.openSections != null && RecordOverviewFragment.this.openSections.size() != 0){
                                for (int i = 0; i < RecordOverviewFragment.this.openSections.size(); i++){
                                    int index = RecordOverviewFragment.this.openSections.get(i);
                                    RecordOverviewFragment.this.expListView.expandGroup(index);
                                }
                            } else {
                                RecordOverviewFragment.this.openSections = new ArrayList<Integer>();
                            }

                            if (mListPosition != -1 && mItemPosition != -1)
                                RecordOverviewFragment.this.expListView.setSelectedChild(mListPosition, mItemPosition, true);

                            mListPosition = -1;
                            mItemPosition = -1;
                            registerListClickCallback(RecordOverviewFragment.this.expListView);
                            RecordOverviewFragment.this.spinner.setVisibility(View.GONE);
                            RecordOverviewFragment.this.actualiseFilterButton();
                            RecordOverviewFragment.this.showEmptyDataNotification();
                        }
                    });
                }
            }

            private RecordListAdapter doInBackground()
            {
                return populateListViewFromDB(RecordOverviewFragment.this.expListView);
            }

            @Override
            public void run()
            {
                //RecordOverviewFragment.this.addRecordToDB(40, 10, 4);
                updateUI(doInBackground());
            }

        });

        loader.start();

        this.actualiseFilterButton();
    }


    /**
    *  Returns the Fragment layout ID
    *  @return int The fragment layout ID
    * */
    public int getLayoutId(){
        return R.layout.fragment_record_list;
    }

    /**
    * Gets called if the user clicks on item in the filter menu.
    *
    * @param  item {@link AbstractPopupItem AbstractPopupItem }
    * */
	public void onFilterMenuItemSelected(AbstractPopupItem item) {
		String title = item.getTitle();

        if (item instanceof SplitPopupItem){
            SplitPopupItem splitItem = (SplitPopupItem)item;
            if (splitItem.wasRightTouch){
                this.openTimestampToFilterDialog();
            } else {
                this.openTimestampFromFilterDialog();
            }
            return;
        }

        if (title != null){
            if(title.equals(FILTER_MENU_TITLE_BSSID)){
                this.openBSSIDFilterDialog();
            }
            if(title.equals(FILTER_MENU_TITLE_ESSID)){
                this.openESSIDFilterDialog();
            }
            if(title.equals(FILTER_MENU_TITLE_PROTOCOLS)){
                this.openProtocolsFilterDialog();
            }
            if(title.equals(FILTER_MENU_TITLE_SORTING)){
                this.openSortingDialog();
            }
            if(title.equals(FILTER_MENU_TITLE_REMOVE)){
                this.clearFilter();
                this.actualiseListViewInBackground();
            }
            if(title.equals(FILTER_MENU_TITLE_TIMESTAMP_BELOW)){
                this.openTimestampToFilterDialog();
            }
            if(title.equals(FILTER_MENU_TITLE_TIMESTAMP_ABOVE)){
                this.openTimestampFromFilterDialog();
            }
        }
		//return super.onOptionsItemSelected(item);
	}


    @Override
    public void onStart() {
        super.onStart();
        if (this.expListView.getExpandableListAdapter() != null){
            if (this.expListView.getExpandableListAdapter().getGroupCount() == 1){
                this.expListView.expandGroup(0);
            } else {
                this.setSectionToOpen(this.sectionToOpen);
            }
        }

    }

    @Override
    public void onDestroy(){
        if (mReceiver != null){
        }
        super.onDestroy();
    }


    @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu items for use in the action bar
		inflater.inflate(R.menu.records_overview_actions, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.records_action_synchronize:

				AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
				builder.setTitle(MainActivity.getInstance().getString(R.string.rec_sync_rec));
				builder.setItems(new String[]{
						MainActivity.getInstance().getString(R.string.rec_via_bt),
						"With TraCINg",
                        "Via WifiDirect"
				}, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int position) {
						switch(position){
							case 0:

								startActivityForResult(new Intent(getBaseContext(), BluetoothSyncActivity.class), 0);
								break;
							/*case 1:
								getActivity().startActivity(new Intent(getActivity(), NFCSyncActivity.class));
								break;*/

							case 1:
                                startActivityForResult(new Intent(getActivity(), TracingSyncActivity.class), 0);
								break;
                            case 2:
                                startActivityForResult(new Intent(getActivity(), WiFiP2pSyncActivity.class), 0);
                                break;
						}
					}
				});
				builder.create();
				builder.show();


				return true;
			case R.id.records_action_export:
				AlertDialog.Builder builderExport = new AlertDialog.Builder(getActivity());
				builderExport.setTitle(MainActivity.getInstance().getString(R.string.rec_choose_export_format));
				builderExport.setItems(R.array.format, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int position) {
						//RecordOverviewFragment.this.exportDatabase(position);
						Intent intent = new Intent(getActivity(), LogExport.class);
						intent.setAction(LogExport.ACTION_EXPORT_DATABASE);
						intent.putExtra(LogExport.FORMAT_EXPORT_DATABASE, position);

						RecordOverviewFragment.this.getActivity().startService(intent);
					}
				});
				builderExport.create();
				builderExport.show();

				return true;
		}

		return false;
	}


    public void openDeleteFilteredAttacksDialog() {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String deleteFILTEREDAttacksTitle = MainActivity.getInstance().getString(R.string.deleteFILTEREDAttacksTitle);
        String deleteALLAttacksTitle = MainActivity.getInstance().getString(R.string.deleteALLAttacksTitle);

        String cancelTitle = MainActivity.getInstance().getString(R.string.cancel);
        String deleteTitle = MainActivity.getInstance().getString(R.string.delete);

        String text = this.filter.isSet()? deleteFILTEREDAttacksTitle : deleteALLAttacksTitle;

        builder.setMessage(text)
                .setPositiveButton(deleteTitle, new DialogInterface.OnClickListener() {
                    private RecordOverviewFragment recordOverviewFragment = null;
                    public void onClick(DialogInterface dialog, int id) {
                        recordOverviewFragment.deleteFilteredAttacks();
                        MainActivity.getInstance().getHostageService().notifyUI(Handler.class.toString(), null);
                    }
                    public DialogInterface.OnClickListener init(RecordOverviewFragment rf){
                        this.recordOverviewFragment = rf;
                        return this;
                    }
                }.init(this))
                .setNegativeButton(cancelTitle, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 0){
            if(resultCode == SyncUtils.SYNC_SUCCESSFUL){
                actualiseListViewInBackground();
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
	public void showDetailsForSSID(Context context,  String SSID) {
		Log.e("RecordOverviewFragment", "Implement showDetailsForSSID!!");
        this.clearFilter();
        int ESSID_INDEX = 2;
        ArrayList<String> ssids = new ArrayList<String>();
        this.sectionToOpen = SSID;
        this.groupingKey = this.groupingTitles().get(ESSID_INDEX);
  	}


	/*****************************
	 *
	 *          ListView Stuff
	 *
	 * ***************************/

    /**
    *  Reloads the data in the ExpandableListView for the given filter object.
    *  @param  mylist {@link ExpandableListView ExpandableListView}
    * */
	private RecordListAdapter populateListViewFromDB(ExpandableListView mylist) {
        ArrayList<String> groupTitle = new ArrayList<String>();

        HashMap<String, ArrayList<ExpandableListItem>> sectionData = this.fetchDataForFilter(this.filter, groupTitle);

        RecordListAdapter adapter = null;
        if (mylist.getAdapter() != null && mylist.getAdapter() instanceof RecordListAdapter){
            adapter = (RecordListAdapter) mylist.getAdapter();
            adapter.setData(sectionData);
            adapter.setSectionHeader(groupTitle);
        } else {
            adapter = new RecordListAdapter( RecordOverviewFragment.this.getApplicationContext(), groupTitle, sectionData);
        }

        return adapter;
	}

    private HashMap<String, ArrayList<ExpandableListItem>> fetchDataForFilter(LogFilter filter, ArrayList<String> groupTitle){
        HashMap<String, ArrayList<ExpandableListItem>> sectionData = new HashMap<String, ArrayList<ExpandableListItem>>();

        ArrayList<Record> data = dbh.getRecordsForFilter(filter == null ? this.filter : filter);

        // Adding Items to ListView
        String[] keys = new String[] { RecordOverviewFragment.this.getString(R.string.RecordBSSID), RecordOverviewFragment.this.getString(R.string.RecordSSID), RecordOverviewFragment.this.getString(R.string.RecordProtocol), RecordOverviewFragment.this.getString(R.string.RecordTimestamp)};
        int[] ids = new int[] {R.id.RecordTextFieldBSSID, R.id.RecordTextFieldSSID, R.id.RecordTextFieldProtocol, R.id.RecordTextFieldTimestamp };

        HashMap<String, Integer> mapping = new HashMap<String, Integer>();
        int i = 0;
        for(String key : keys){
            mapping.put(key, ids[i]);
            i++;
        }

        if (groupTitle == null){
            groupTitle = new ArrayList<String>();
        } else {
            groupTitle.clear();
        }


        for (Record val : data) {
            // DO GROUPING IN HERE
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(RecordOverviewFragment.this.getString(R.string.RecordBSSID), val.getBssid());
            map.put(RecordOverviewFragment.this.getString(R.string.RecordSSID), val.getSsid());
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
                items = new ArrayList<ExpandableListItem>();
                sectionData.put(groupID, items);
                groupTitle.add(groupID);
            }


            items.add(item);
        }

        if (this.groupingKey.equals(this.groupingTitles().get(DEFAULT_GROUPING_KEY_INDEX))){
            Collections.sort(groupTitle,new DateStringComparator());
        } else {
            Collections.sort(groupTitle, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return s1.compareToIgnoreCase(s2);
                }
            });
        }

        return sectionData;
    }

    /**
     * The DateStringComparator compares formatted date strings by converting the into date.
     * This class  is mainly used for grouping the records by their timestamp.
     */
    class DateStringComparator implements Comparator<String>
    {
        public int compare(String lhs, String rhs)
        {
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

    /**
     * Actualises the list in a background thread
     */
    private void actualiseListViewInBackground(){
        if (loader != null && loader.isAlive()) loader.interrupt();

        loader = null;

        this.spinner.setVisibility(View.VISIBLE);
        this.actualiseFilterButton();

        loader = new Thread(new Runnable() {
            @Override
            public void run() {
                this.runOnUiThread(this.doInBackground());
            }

            private RecordListAdapter doInBackground(){
                return RecordOverviewFragment.this.populateListViewFromDB(RecordOverviewFragment.this.expListView);
            }

            private void runOnUiThread(final RecordListAdapter adapter){
                Activity actv = RecordOverviewFragment.this.getActivity();
                if (actv != null){
                    actv.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            this.actualiseUI();
                        }
                        private void actualiseUI(){
                            RecordOverviewFragment self = RecordOverviewFragment.this;
                            if (adapter != null){
                                self.expListView.setAdapter(adapter);
                                adapter.notifyDataSetChanged();
                                self.spinner.setVisibility(View.GONE);
                            }
                            self.showEmptyDataNotification();
                            if (self.openSections != null && self.expListView != null){
                                for (int i = 0; i < self.openSections.size(); i++){
                                    int index = self.openSections.get(i);
                                    self.expListView.expandGroup(index);
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
    private void showEmptyDataNotification(){
        if (RecordOverviewFragment.this.noDataNotificationToast == null){
            RecordOverviewFragment.this.noDataNotificationToast =  Toast.makeText(getApplicationContext(), R.string.no_data_notification, Toast.LENGTH_SHORT);
        }
        RecordListAdapter adapter = (RecordListAdapter) RecordOverviewFragment.this.expListView.getExpandableListAdapter();

        if (this.getFilterButton().getVisibility() == View.VISIBLE && this.filter.isSet()){
            this.noDataNotificationToast.setText(R.string.no_data_notification);
        } else {
            this.noDataNotificationToast.setText(R.string.no_data_notification_no_filter);
        }
        if (adapter == null || adapter.getData().isEmpty())
            RecordOverviewFragment.this.noDataNotificationToast.show();

    }

    /**This will open a section in the ExpandableListView with the same title as the parameter s.
    *
    * @param s String (the section title to open)
    *
    * */
    private void setSectionToOpen(String s){
        this.sectionToOpen = s;
        if (this.sectionToOpen != null && this.sectionToOpen.length() != 0){
            if (this.getGroupTitles().contains(this.sectionToOpen)){
                int section = this.getGroupTitles().indexOf(this.sectionToOpen);
                this.expListView.expandGroup(section);
                this.sectionToOpen = "";
                if (!this.openSections.contains(section)){
                    RecordOverviewFragment.this.openSections.add(section);
                }
            }
        }
    }

    /**
    * Returns the base context.
    * @return Context baseContext
    * */
	private Context getBaseContext(){
		return this.getActivity().getBaseContext();
	}

    /**Returns the application context.
    * @return Context application context
    * */
	private Context getApplicationContext(){
		return this.getActivity().getApplicationContext();
	}

    /**Sets the list view listener on the given ExpandableListView.
    *
    * @param mylist  {@link ExpandableListView ExpandableListView }
    * */
	private void registerListClickCallback(ExpandableListView mylist) {
        mylist.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long l) {
                RecordListAdapter adapter = (RecordListAdapter)expandableListView.getExpandableListAdapter();

                ExpandableListItem item = (ExpandableListItem)adapter.getChild(i,i2);

                mListPosition = i;
                mItemPosition = i2;
	            HostageDBOpenHelper dbh = new HostageDBOpenHelper(getBaseContext());
                Record rec = dbh.getRecordOfAttackId((int) item.getTag());
                RecordOverviewFragment.this.pushRecordDetailViewForRecord(rec);
                return true;
            }
        });
        mylist.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int i) {
                if (!RecordOverviewFragment.this.openSections.contains(i)){
                    RecordOverviewFragment.this.openSections.add(i);
                }
            }
        });
        mylist.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int i) {
                RecordOverviewFragment.this.openSections.remove(i);
            }
        });
	}



	/*****************************
	 *
	 *          Date Transformation / Conversion
	 *
	 * ***************************/


    /**Returns the localised date format for the given timestamp
    * @param timeStamp long */
	@SuppressLint("SimpleDateFormat")
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
     *
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
        String date = this.getDateAsMonthString(timestamp);
        // DAY FORMAT
        //String date = this.getDateAsDayString(timestamp);

        return date;
    }

    /**Returns a date as a formated string
     * @param timestamp date
     * @return String date format is localised*/
    @SuppressLint("SimpleDateFormat")
    private String getDateAsDayString(long timestamp) {
        try {
            Date netDate = (new Date(timestamp));
            String dateString;

            long date = this.dayMilliseconds(timestamp);

            if(this.todayMilliseconds() == date ){
                dateString = TODAY;
            }else if(this.yesterdayMilliseconds() == date ){
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
     * @param dateString String
     * @return Date
     */
    private Date convertStringToDate(String dateString){
        if (dateString != null && dateString.length() != 0){
            SimpleDateFormat dateFormat = groupingDateFormatter; //new SimpleDateFormat(localDatePattern);
            Date date;
            try {
                if (dateString.equals(TODAY)){
                    long millisec = RecordOverviewFragment.this.todayMilliseconds();
                    date = new Date(millisec);
                } else if (dateString.equals(YESTERDAY)){
                    long millisec = RecordOverviewFragment.this.yesterdayMilliseconds();
                    date = new Date(millisec);
                } else {
                    date = dateFormat.parse(dateString);
                }
                return date;

            } catch (java.text.ParseException e ) {
                date = new Date(0);
                return date;
            }
        } else {
            return new Date(0);
        }
    }

    /**
     * Returns the milliseconds for the day today (not the time).
     * @return long
     */
    private long todayMilliseconds(){
        Date current = new Date();
        calendar.setTimeInMillis(current.getTime());
        int day = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.set(year, month, day, 0,0,0);

        long milli = calendar.getTimeInMillis();

        Date today = new Date(milli);

        return (milli / (long) 1000) * (long) 1000;
    }

    /**
     * Returns the milliseconds for the day yesterday (not the time).
     * @return long
     */
    private long yesterdayMilliseconds(){
        Date current = new Date();
        calendar.setTimeInMillis(current.getTime());
        int day = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.set(year, month, day, 0,0,0);

        calendar.add(Calendar.DATE, -1);

        long milli = calendar.getTimeInMillis();

        Date today = new Date(milli);

        return (milli / (long) 1000) * (long) 1000;
    }

    /**
     * returns just the date not the time of a date.
     * @param date Date
     * @return long
     */
    private long dayMilliseconds(long date){
        //Date current = new Date();
        calendar.setTimeInMillis(date);
        int day = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.set(year, month, day, 0,0,0);

        long milli = calendar.getTimeInMillis();

        Date test = new Date(milli);

        return (milli / (long) 1000) * (long) 1000;
    }

    /**Returns a date as a formated string
     * @param timeStamp date
     * @return String date format is localised*/
    @SuppressLint("SimpleDateFormat")
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
     * @param key String
     */
    public void setGroupKey(String key){
        this.groupingKey = key;
    }

    public void setFilter(LogFilter filter){
        this.filter = filter;
    }


	/*****************************
	 *
	 *          Open Dialog Methods
	 *
	 * ***************************/

    /**Opens the grouping dialog*/
    private void openGroupingDialog(){
        ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_GROUP, this.groupingTitles(), this.selectedGroup(), false , this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_GROUP);
    }

    /**opens the bssid filter dialog*/
	private void openBSSIDFilterDialog(){
		ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_BSSID,this.bssids(), this.selectedBSSIDs(), true , this);
	    newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_BSSID);
	}

    /**opens the essid filter dialog*/
	private void openESSIDFilterDialog(){
		ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_ESSID,this.essids(), this.selectedESSIDs(), true , this);
	    newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_ESSID);
	}

    /**opens the protocol filter dialog*/
	private void openProtocolsFilterDialog(){
		ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_PROTOCOLS,this.protocolTitles(), this.selectedProtocols(), true , this);
	    newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_PROTOCOLS);
	}

    /**opens the timestamp filter dialog (minimal timestamp required)*/
	private void openTimestampFromFilterDialog(){
		this.wasBelowTimePicker = false;
		DateTimeDialogFragment newFragment = new DateTimeDialogFragment(this.getActivity());
	    newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_SORTING);
        if (this.filter.aboveTimestamp != Long.MIN_VALUE)newFragment.setDate(this.filter.aboveTimestamp);
	}

    /**opens time timestamp filter dialog (maximal timestamp required)*/
	private void openTimestampToFilterDialog(){
		this.wasBelowTimePicker = true;
		DateTimeDialogFragment newFragment = new DateTimeDialogFragment(this.getActivity());
	    newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_SORTING);
        if (this.filter.belowTimestamp != Long.MAX_VALUE) newFragment.setDate(this.filter.belowTimestamp);
    }

    /**opens the sorting dialog*/
	private void openSortingDialog(){
		ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_SORTING,this.sortTypeTiles(), this.selectedSorttype(), false , this);
	    newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_SORTING);
	}

    /*****************************
     *
     *          Grouping Stuff
     *
     * ***************************/

    /**returns the group title for the given record. Uses the groupingKey to decied which value of the record should be used.
    * @param  rec {@link Record Record }
    * @return String grouptitle*/
    public String getGroupValue(Record rec){
        int index = this.groupingTitles().indexOf(this.groupingKey);
        switch (index){
            case 1:
                return rec.getProtocol();
            case 2:
                return rec.getSsid();
            case 3:
                return rec.getBssid();
            case 0:
                return this.getFormattedDateForGrouping(rec.getTimestamp());
            default:
                return this.getFormattedDateForGrouping(rec.getTimestamp());
        }
    }

    /**Returns the Group titles for the specified grouping key. e.g. groupingKey is "ESSID" it returns all available essids.
    * @return ArrayList<String> grouptitles*/
    public List<String> getGroupTitles(){
        int index = this.groupingTitles().indexOf(this.groupingKey);
        switch (index){
            case 1:
                return this.protocolTitles();
            case 2:
                return this.essids();
            case 3:
                return this.bssids();
            case 0:
            default:
                RecordListAdapter adapter = (RecordListAdapter) this.expListView.getExpandableListAdapter();
                if (adapter != null){
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

    /**Returns the FilterButton.
     * @return ImageButton filterButton*/
    private ImageButton getFilterButton(){
        return (ImageButton) this.rootView.findViewById(R.id.FilterButton);
    }

    /**Opens the filter menu on a anchor view. The filter menu will always be on top of the anchor.
    * @param  v View the anchorView*/
	private void openFilterPopupMenuOnView(View v){

        SimplePopupTable filterMenu = new SimplePopupTable(this.getActivity(), new AbstractPopup.OnPopupItemClickListener() {
            public void onItemClick(Object ob) {
                if (ob instanceof  AbstractPopupItem){
                    AbstractPopupItem item = (AbstractPopupItem) ob;
                    RecordOverviewFragment.this.onFilterMenuItemSelected(item);
                }
            }
        });
        filterMenu.setTitle(FILTER_MENU_POPUP_TITLE);
		for(String title : RecordOverviewFragment.this.filterMenuTitles()){
            AbstractPopupItem item = null;
            if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_BELOW)) continue;
            if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_ABOVE)){
                item = new SplitPopupItem(this.getActivity());
                item.setValue(SplitPopupItem.RIGHT_TITLE, FILTER_MENU_TITLE_TIMESTAMP_BELOW);
                item.setValue(SplitPopupItem.LEFT_TITLE, FILTER_MENU_TITLE_TIMESTAMP_ABOVE);
                if (this.filter.hasBelowTimestamp()){
                    item.setValue(SplitPopupItem.RIGHT_SUBTITLE, this.getDateAsString(this.filter.belowTimestamp));
                }
                if (this.filter.hasAboveTimestamp()){
                    item.setValue(SplitPopupItem.LEFT_SUBTITLE, this.getDateAsString(this.filter.aboveTimestamp));
                }
            } else {
                item = new SimplePopupItem(this.getActivity());
                item.setTitle(title);
                ((SimplePopupItem)item).setSelected(this.isFilterSetForTitle(title));
            }

            filterMenu.addItem(item);
		}
		filterMenu.showOnView(v);
	}

    /**Returns true  if the filter object is set for the given title otherwise false. e.g. the filter object has protocols,
    * so the method will return for the title FILTER_MENU_TITLE_PROTOCOLS TRUE.
    * @param  title String
    * @return boolean value
    * */
    private boolean isFilterSetForTitle(String title){
        if (title.equals(FILTER_MENU_TITLE_BSSID)){
            return this.filter.hasBSSIDs();
        }
        if (title.equals(FILTER_MENU_TITLE_ESSID)){
            return this.filter.hasESSIDs();
        }
        if (title.equals(FILTER_MENU_TITLE_PROTOCOLS)){
            return this.filter.hasProtocols();
        }
        if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_BELOW)){
            return this.filter.hasBelowTimestamp();
        }
        if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_ABOVE)){
            return this.filter.hasAboveTimestamp();
        }
        return false;
    }

    /**clears the filter. Does not invoke populatelistview!*/
	private void clearFilter(){
    	if(filter == null) this.filter = new LogFilter();
    	this.filter.clear();
	}

    /**Returns all grouping titles.
    * @return ArrayList<String> tiles*/
    public ArrayList<String> groupingTitles(){
        ArrayList<String> titles = new ArrayList<String>();
        titles.add(MainActivity.getContext().getString(R.string.date));
        titles.add(MainActivity.getContext().getString(R.string.rec_protocol));
        titles.add(MainActivity.getContext().getString(R.string.ESSID));
        titles.add(MainActivity.getContext().getString(R.string.BSSID));
        return titles;
    }
    /**
     * Returns a bool array. This array is true at the index of the groupingKey in groupingTitles(), otherwise false.
    * @return boolean[] selection
    * */
    public boolean[] selectedGroup(){
        ArrayList<String> groups = this.groupingTitles();
        boolean[] selected = new boolean[groups.size()];
        int i = 0;
        for(String group : groups){
            selected[i] =(group.equals(this.groupingKey));
            i++;
        }
        return selected;
    }

    /**Returns all protocol titles / names.
    * @return ArrayList<String> protocolTitles
    * */
	public ArrayList<String> protocolTitles(){
		ArrayList<String> titles = new ArrayList<String>();
		for (String protocol : this.getResources().getStringArray(
				R.array.protocols)) {
			titles.add(protocol);
		}

		titles.add("PORTSCAN");
        titles.add("FILE INJECTION");
        titles.add("MULTISTAGE ATTACK");
		return titles;
	}
    /**Return a boolean array of the selected / filtered protocols. If the filter object has
    * an protocol from the protocolTitles() array, the index of it will be true, otherwise false.
    * @return boolean[] protocol selection
    * */
	public boolean[] selectedProtocols(){
		ArrayList<String> protocols = this.protocolTitles();
		boolean[] selected = new boolean[protocols.size()];

		int i = 0;
		for(String protocol : protocols){
			selected[i] =(this.filter.protocols.contains(protocol));
			i++;
		}
		return selected;
	}

    /**
    * Returns the Sorttype Titles
    * @return ArayList<String> Sort type titles
    * */
	public ArrayList<String> sortTypeTiles(){
		ArrayList<String> titles = new ArrayList<String>();
		titles.add(MainActivity.getContext().getString(R.string.rec_time));
		titles.add(MainActivity.getContext().getString(R.string.rec_protocol));
        titles.add(MainActivity.getContext().getString(R.string.ESSID));
        titles.add(MainActivity.getContext().getString(R.string.BSSID));
		return titles;
	}
    /**
    * Returns an boolean array. The array is true at the index of the selected sort type..
    * The index of the selected sort type is the same index in the sortTypeTiles array.
    * @return boolean array, length == sortTypeTiles().length
    * */
	public boolean[] selectedSorttype(){
		ArrayList<String> types = this.sortTypeTiles();
		boolean[] selected = new boolean[types.size()];
		int i = 0;
		for(String sorttype : types){
			selected[i] =(this.filter.sorttype.toString().equals(sorttype));
			i++;
		}
		return selected;
	}

    /**
    * Returns all unique bssids.
    * @return ArrayList<String>
    * */
	public ArrayList<String> bssids(){
		ArrayList<String> records = dbh.getUniqueBSSIDRecords();
		return records;
	}
    /**
    * Returns an boolean array. The array is true at the indices of the selected bssids.
    * The index of the selected bssid is the same index in the bssids() array.
    * @return boolean array, length == bssids().length
    * */
	public boolean[] selectedBSSIDs(){
		ArrayList<String> bssids = this.bssids();
		boolean[] selected = new boolean[bssids.size()];

		int i = 0;
		for(String bssid : bssids){
			selected[i] =(this.filter.BSSIDs.contains(bssid));
			i++;
		}
		return selected;
	}

    /**
    * Returns all unique essids.
    * @return ArrayList<String>
    * */
	public ArrayList<String> essids(){
		ArrayList<String> records = dbh.getUniqueESSIDRecords();
		return records;
	}
    /**
    * Returns an boolean array. The array is true at the indices of the selected essids.
    * The index of the selected essid is the same index in the essids() array.
    * @return boolean array, length == essids().length
    * */
	public boolean[] selectedESSIDs(){
		ArrayList<String> essids = this.essids();
		boolean[] selected = new boolean[essids.size()];

		int i = 0;
		for(String essid : essids){
			selected[i] =(this.filter.ESSIDs.contains(essid));
			i++;
		}
		return selected;
	}

    /**
     * Returns all filter menu titles.
     * @return ArrayList<String>
     * */
	private ArrayList<String> filterMenuTitles(){
		ArrayList<String> titles = new ArrayList<String>();
		titles.add(FILTER_MENU_TITLE_BSSID);
		titles.add(FILTER_MENU_TITLE_ESSID);
		titles.add(FILTER_MENU_TITLE_PROTOCOLS);
		titles.add(FILTER_MENU_TITLE_TIMESTAMP_ABOVE);
		titles.add(FILTER_MENU_TITLE_TIMESTAMP_BELOW);
        if (this.filter.isSet())titles.add(FILTER_MENU_TITLE_REMOVE);
		return titles;
	}


	/*****************************
	 *
	 *          Listener Actions
	 *
	 * ***************************/

    /**
     * Will be called if the users selects a timestamp.
     * @param  dialog {@link DateTimeDialogFragment DateTimeDialogFragment }
     * */
	public void onDateTimePickerPositiveClick(DateTimeDialogFragment dialog) {
		if(this.wasBelowTimePicker){
			this.filter.setBelowTimestamp(dialog.getDate());
		} else {
			this.filter.setAboveTimestamp(dialog.getDate());
		}
        this.actualiseListViewInBackground();
        this.actualiseFilterButton();
    }
    /**
     * Will be called if the users cancels a timestamp selection.
     * @param dialog  {@link DateTimeDialogFragment DateTimeDialogFragment }
     * */
	public void onDateTimePickerNegativeClick(DateTimeDialogFragment dialog) {
		if(this.wasBelowTimePicker){
			this.filter.setBelowTimestamp(Long.MAX_VALUE);
		} else {
			this.filter.setAboveTimestamp(Long.MIN_VALUE);
		}
        this.actualiseListViewInBackground();
        this.actualiseFilterButton();
    }

    /**
     * Will be called if the users clicks the positiv button on a ChechlistDialog.
     * @param  dialog  {@link ChecklistDialog ChecklistDialog }
     */
	public void onDialogPositiveClick(ChecklistDialog dialog) {
		String title = dialog.getTitle();
		if(title.equals(FILTER_MENU_TITLE_BSSID)){
            ArrayList<String> titles =dialog.getSelectedItemTitles();
            if (titles.size() == this.bssids().size()){
                this.filter.setBSSIDs(new ArrayList<String>());
            } else {
                this.filter.setBSSIDs(titles);
            }
		}
		if(title.equals(FILTER_MENU_TITLE_ESSID)){
            ArrayList<String> titles =dialog.getSelectedItemTitles();
            if (titles.size() == this.essids().size()){
                this.filter.setESSIDs(new ArrayList<String>());
            } else {
                this.filter.setESSIDs(titles);
            }
		}
		if(title.equals(FILTER_MENU_TITLE_PROTOCOLS)){
            ArrayList<String> protocols = dialog.getSelectedItemTitles();
            if (protocols.size() == this.protocolTitles().size()){
                this.filter.setProtocols(new ArrayList<String>());
            } else {
			    this.filter.setProtocols(dialog.getSelectedItemTitles());
            }
		}
		if(title.equals(FILTER_MENU_TITLE_SORTING)){
			ArrayList<String> titles = dialog.getSelectedItemTitles();
            if (titles.size() == 0) return;
            // ALWAYS GET THE FIRST ELEMENT (SHOULD BE ALWAYS ONE)
            String t = titles.get(0);
			int sortType = this.sortTypeTiles().indexOf(t);
			this.filter.setSorttype(SortType.values()[sortType]);
		}
        if (title.equals(FILTER_MENU_TITLE_GROUP)){
            ArrayList<String> titles = dialog.getSelectedItemTitles();
            if (titles.size() == 0) return;
            // ALWAYS GET THE FIRST ELEMENT (SHOULD BE ALWAYS ONE)
            this.groupingKey =  titles.get(0);
        }
        this.actualiseListViewInBackground();

        this.actualiseFilterButton();
	}

    /**Paints the filter button if the current filter object is set.*/
    private void actualiseFilterButton(){
        if (this.filter.isSet() ){
            ImageButton filterButton = this.getFilterButton();
            if (filterButton != null){
                filterButton.setImageResource(R.drawable.ic_filter_pressed);
                filterButton.invalidate();
            }
        } else {
            ImageButton filterButton = this.getFilterButton();
            if (filterButton != null){
                filterButton.setImageResource(R.drawable.ic_filter);
                filterButton.invalidate();
            }
        }
    }

    /**
     * Deletes the current displayed attacks.
     */
    public void deleteFilteredAttacks(){
        LogFilter filter = this.filter;
        dbh.deleteAttacksByFilter(filter);
        this.actualiseListViewInBackground();
    }

    /**
     * Will be called if the users clicks the negativ button on a ChechlistDialog.
     * @param  dialog  {@link ChecklistDialog ChecklistDialog }
     */
	public void onDialogNegativeClick(ChecklistDialog dialog) {}


	/*****************************
	 *
	 *          TEST
	 *
	 * ***************************/

    /**
    * This will clear the database at first and than add new attacks.
    * @param createNetworks number of networks to create
    * @param attacksPerNetwork maximal number of attack per network
    * @param maxMessagePerAttack maximal number of messages per attack
    * */
	private void addRecordToDB( int createNetworks, int attacksPerNetwork, int maxMessagePerAttack) {
        if ((dbh.getRecordCount() > 0)) dbh.clearData();

		Calendar cal = Calendar.getInstance();

		int maxProtocolsIndex = this.getResources().getStringArray(
				R.array.protocols).length;

		Random random = new Random();

		LatLng tudarmstadtLoc = new LatLng(49.86923, 8.6632768);


		final double ssidRadius = 0.1;
		final double bssidRadius = 0.004;

        int attackId = 0;


        ArrayList<NetworkRecord> networkRecords = new ArrayList<NetworkRecord>();
        ArrayList<AttackRecord> attackRecords = new ArrayList<AttackRecord>();
        ArrayList<MessageRecord> messageRecords = new ArrayList<MessageRecord>();


        for (int numOfNetworks = 0; numOfNetworks < createNetworks; numOfNetworks++){
            String ssidName = "WiFi" + ((numOfNetworks) + 1);
            String bssidName = "127.0.0." + ((numOfNetworks) + 1);

            int protocolIndex = numOfNetworks % maxProtocolsIndex;
            String protocolName = this.getResources().getStringArray(
                    R.array.protocols)[protocolIndex];

            int numOfAttackPerNetwork = (Math.abs(random.nextInt()) % Math.max(1, attacksPerNetwork + 1));

            NetworkRecord network = new NetworkRecord();
            network.setBssid(bssidName);
            network.setSsid(ssidName);

            LatLng ssidLocation = new LatLng(tudarmstadtLoc.latitude - ssidRadius + 2.0 * ssidRadius * Math.random(), tudarmstadtLoc.longitude - ssidRadius + 2.0 * ssidRadius * Math.random());
            double latitude = ssidLocation.latitude - bssidRadius + 2.0 * bssidRadius * Math.random();
            double longitude = ssidLocation.longitude - bssidRadius + 2.0 * bssidRadius * Math.random();

            long timestamp = cal.getTimeInMillis();
            network.setTimestampLocation(timestamp);
            network.setLongitude(longitude);
            network.setLatitude(latitude);
            network.setAccuracy(0.f);

            //dbh.updateNetworkInformation(network);
            networkRecords.add(network);

            // ATTACKS PER NETWORK
            for (int attackNumber = 0; attackNumber < numOfAttackPerNetwork; attackNumber++) {

                int numRecordsPerAttack = (Math.abs(random.nextInt()) % (Math.max( maxMessagePerAttack, 1))) + 1;

                if (maxMessagePerAttack <= 0) numRecordsPerAttack = 0;

                /* ADD A ATTACK*/
                AttackRecord attack = new AttackRecord(true);

                attack.setBssid(bssidName);

                attack.setProtocol(protocolName);
                attack.setLocalIP(bssidName);


                //dbh.addAttackRecord(attack);
                attackRecords.add(attack);

                // MESSAGE PER ATTACK
                for (int messageID = attackId; messageID < attackId + numRecordsPerAttack; messageID++) {
                    MessageRecord message = new MessageRecord(true);
                    //message.setId(messageID);
                    message.setAttack_id(attack.getAttack_id());

                    // GO BACK IN TIME
                    message.setTimestamp(cal.getTimeInMillis()
                            - ((messageID * 60 * 60 * 24) * 1000) + (1000 * ((messageID - attackId) + 1)));

                    if ((messageID - attackId) % 2 == 0){
                        message.setType(MessageRecord.TYPE.RECEIVE);
                    } else {
                        message.setType(MessageRecord.TYPE.SEND);
                    }
                    message.setPacket("");

                    //dbh.addMessageRecord(message);
                    messageRecords.add(message);
                }

                attackId+=numRecordsPerAttack;
            }

        }

        dbh.updateNetworkInformation(networkRecords);
        dbh.insertAttackRecords(attackRecords);
        dbh.insertMessageRecords(messageRecords);
//        int countAllLogs = dbh.getAllRecords().size();
//        int countRecords = dbh.getRecordCount();
//        int countAttacks = dbh.getAttackCount();
//
//        if ((countRecords == 0)) {
//            Record rec = dbh.getRecordOfAttackId(0);
//            Record rec2 = dbh.getRecord(0);
//
//            System.out.println("" + "Could not create logs!");
//        }

    }


    /**Navigation. Shows the record detail view for the given record
    * @param  record  {@link Record Record } to show
    * */
    private void pushRecordDetailViewForRecord(Record record){

        FragmentManager fm = this.getActivity().getFragmentManager();

        if (fm != null){
            RecordDetailFragment newFragment = new RecordDetailFragment();
            newFragment.setRecord(record);

            newFragment.setUpNavigatible(true);

            MainActivity.getInstance().injectFragment(newFragment);

        }

    }
}