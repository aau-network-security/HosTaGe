package dk.aau.netsec.hostage.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.echo.holographlibrary.Bar;
import com.echo.holographlibrary.BarGraph;
import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LineGraph;
import com.echo.holographlibrary.LinePoint;
import com.echo.holographlibrary.PieGraph;
import com.echo.holographlibrary.PieSlice;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.logging.RecordAll;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.ui.activity.MainActivity;
import dk.aau.netsec.hostage.ui.adapter.StatisticListAdapter;
import dk.aau.netsec.hostage.ui.dialog.ChecklistDialog;
import dk.aau.netsec.hostage.ui.dialog.DateTimeDialogFragment;
import dk.aau.netsec.hostage.ui.helper.ColorSequenceGenerator;
import dk.aau.netsec.hostage.ui.model.LogFilter;
import dk.aau.netsec.hostage.ui.model.PlotComparisonItem;
import dk.aau.netsec.hostage.ui.popup.AbstractPopupItem;
import dk.aau.netsec.hostage.ui.popup.SimplePopupItem;
import dk.aau.netsec.hostage.ui.popup.SimplePopupTable;
import dk.aau.netsec.hostage.ui.popup.SplitPopupItem;

/**
 * Created by Julien on 16.02.14.
 */
public class StatisticsFragment extends TrackerFragment implements ChecklistDialog.ChecklistDialogListener, DateTimeDialogFragment.DateTimeDialogFragmentListener {
    static final String FILTER_MENU_TITLE_BSSID = "BSSID";
    static final String FILTER_MENU_TITLE_ESSID = "ESSID";
    static final String FILTER_MENU_TITLE_PROTOCOLS = MainActivity.getContext().getString(R.string.stats_protocols);
    static final String FILTER_MENU_TITLE_PROTOCOL = MainActivity.getContext().getString(R.string.rec_protocol);
    static final String FILTER_MENU_TITLE_TIMESTAMP_BELOW = MainActivity.getContext().getString(R.string.rec_latest);
    static final String FILTER_MENU_TITLE_TIMESTAMP_ABOVE = MainActivity.getContext().getString(R.string.rec_earliest);
    static final String FILTER_MENU_TITLE_REMOVE = MainActivity.getContext().getString(R.string.rec_reset_filter);
    static final String FILTER_MENU_POPUP_TITLE = MainActivity.getContext().getString(R.string.rec_filter_by);

    static final String MENU_TITLE_PROTOCOLS = MainActivity.getContext().getString(
			R.string.stats_protocols);
    static final String MENU_TITLE_NETWORK = MainActivity.getContext().getString(
			R.string.stats_networks);
    static final String MENU_TITLE_ATTACKS = MainActivity.getContext().getString(
			R.string.stats_attacks);
    static final String MENU_POPUP_TITLE = MainActivity.getContext().getString(
			R.string.stats_visualize);

    static final String CHART_TYPE_TITLE_BAR = MainActivity.getContext().getString(
			R.string.stats_bar_plot);
    static final String CHART_TYPE_TITLE_PIE = MainActivity.getContext().getString(R.string.stats_pie_plot);
    static final String CHART_TYPE_TITLE_LINE = MainActivity.getContext().getString(R.string.stats_line_plot);

    //static final String DIALOG_PROTOCOLS_TITLE = MainActivity.getContext().getString(R.string.stats_select_protocol_data);
    static final String DIALOG_NETWORK_TITLE = MainActivity.getContext().getString(R.string.stats_select_network_data);
    static final String DIALOG_ATTACK_TITLE = MainActivity.getContext().getString(R.string.stats_select_attack_data);

    static  final String COMPARE_TITLE_AttacksPerProtocol   = MainActivity.getContext().getString(R.string.stats_attacks_protocol);
    //static  final String COMPARE_TITLE_UsesPerProtocol      = MainActivity.getContext().getString(R.string.stats_uses_protocol);
    static  final String COMPARE_TITLE_AttacksPerDate       = MainActivity.getContext().getString(R.string.stats_attacks_date);
    static  final String COMPARE_TITLE_AttacksPerTime       = MainActivity.getContext().getString(R.string.stats_attacks_time);
    static  final String COMPARE_TITLE_AttacksPerBSSID      = MainActivity.getContext().getString(R.string.stats_attacks_bssid);
    static  final String COMPARE_TITLE_AttacksPerESSID      = MainActivity.getContext().getString(R.string.stats_attacks_essid);
    static final String FILTER_MENU_PROTOCOL_SINGLE_CHOICE_TITLE = MainActivity.getContext().getString(R.string.stats_select_protocol);

    static final String TABLE_HEADER_VALUE_TITLE_ATTACKS_COUNT = MainActivity.getContext().getString(R.string.stats_attacks_count);
    static final String TABLE_HEADER_VALUE_TITLE_ATTACKS_PERCENTAGE = MainActivity.getContext().getString(R.string.stats_per_cent_all);

    static final String OTHER_CHART_TITLE = MainActivity.getContext().getString(R.string.stats_other);

    // MINIMAL 2
    static int MAX_NUMBER_OF_CHART_OBJECTS = 6;
    private boolean wasBelowTimePicker;
    private LogFilter filter;

    /*Maybe used in the future if the users doesn't need a filterbutton in every situation*/
    private boolean showFilterButton;

    private PieGraph pieGraph;
    private LineGraph lineGraph;
    private BarGraph barGraph;

    private View rootView;
    private View currentPlotView;
    private Thread loader;
    private ProgressBar spinner;

    private ArrayList<PlotComparisonItem> currentData;

    private DaoSession dbSession;
    private DAOHelper daoHelper;

    private ListView legendListView;

    private Toast noDataNotificationToast;
    private String selectedCompareData = COMPARE_TITLE_AttacksPerProtocol;

    private LayoutInflater inflater;
    private ViewGroup container;
    private Bundle savedInstanceState;

    /**The Charttype.
     * PIE_CHART = 0
     * BAR_CHART = 1
     * LINE_CHART = 2
     */
    public enum ChartType {
        PIE_CHART(0),
        BAR_CHART(1),
        LINE_CHART(2);

        private int value;

        ChartType(int value) {
            this.value = value;
        }
        static public ChartType create(int value){
            if (value < 0 || value  >= ChartType.values().length) return ChartType.PIE_CHART;
            return  ChartType.values()[value];
        }

        public String toString(){
            if (this.equals(ChartType.create(0))){
                return CHART_TYPE_TITLE_PIE;
            }
            if (this.equals(ChartType.create(1))){
                return CHART_TYPE_TITLE_BAR;
            }
            return CHART_TYPE_TITLE_LINE;
        }

    }

    /**Returns the FilterButton.
    * @return ImageButton filterButton*/
    private ImageButton getFilterButton(){
        return (ImageButton) this.rootView.findViewById(R.id.FilterButton);
    }

    /**
    * Returns the layout ID
    * @Return int layoutID
    * */
    public int getLayoutID(){
        return R.layout.fragment_statistics;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

	    super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;
        this.container= container;
        this.savedInstanceState = savedInstanceState;
	    getActivity().setTitle(getResources().getString(R.string.drawer_statistics));

        dbSession = HostageApplication.getInstances().getDaoSession();
        daoHelper = new DAOHelper(dbSession,getActivity());

        // Get the message from the intent
        if (this.filter == null){
            Intent intent = this.getActivity().getIntent();
            LogFilter filter = intent.getParcelableExtra(LogFilter.LOG_FILTER_INTENT_KEY);
            if(filter == null){
                this.clearFilter();
            } else {
                this.filter = filter;
            }
        }

        this.rootView = inflater.inflate(this.getLayoutID(), container, false);
        this.configureRootView(this.rootView);

        return this.rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        ViewGroup container = (ViewGroup) this.getView();
        assert container != null;
        container.removeAllViewsInLayout();
        this.rootView = inflater.inflate(this.getLayoutID(), container, false);
        container.addView(this.rootView);

        this.configureRootView(this.rootView);

    }

    /**Returns the base context.
    * @return Context the base context
    * */
    private Context getBaseContext(){
        return this.getActivity().getBaseContext();
    }

    /**Returns the application context.
    * @return Context the application context*/
    private Context getApplicationContext(){
        return this.getActivity().getApplicationContext();
    }

    /**Configures the given rootview.
    * Sets the Spinner, the list and all requiered buttons.
    * It also actualises the current plot type.
    * @param  rootView View
    * */
    public void configureRootView(View rootView){
        LinearLayout plotLayout = rootView.findViewById(R.id.plot_layout);
        plotLayout.removeAllViews();
        plotLayout.setWillNotDraw(false);

        ProgressBar spinner = rootView.findViewById(R.id.progressBar1);
        if(spinner != null){
            this.spinner = spinner;
            this.spinner.setVisibility(View.GONE);
        } else {
            RelativeLayout parent = (RelativeLayout) this.spinner.getParent();
            parent.removeView(this.spinner);
            RelativeLayout newParent = rootView.findViewById(R.id.plot_parent_layout);
            if (newParent != null){
                newParent.addView(this.spinner);
            }
        }

        this.legendListView = rootView.findViewById(R.id.legend_list_view);
        this.legendListView.setOnItemClickListener((adapterView, view, i, l) -> StatisticsFragment.this.userTappedOnLegendItem(i));
        rootView.setWillNotDraw(false);

        ImageButton visualButton = rootView.findViewById(R.id.plot_data_button);
        visualButton.setOnClickListener(StatisticsFragment.this::openBarSelectionMenuOnView);

        ImageButton filterButton = this.getFilterButton();
        filterButton.setOnClickListener(StatisticsFragment.this::openFilterMenuOnView);

        this.actualiseCurrentPlot();

        if (this.currentPlotView instanceof BarGraph){
            this.setTitle("" + this.getCurrentSelectedProtocol() + ": " +this.selectedCompareData);
        } else {
            this.setTitle(this.selectedCompareData);
        }
    }

    /**Sets the title over the plot view.
    * @param  title String
    * */
    public void setTitle(String title){
        TextView titleView = this.rootView.findViewById(R.id.title_text_view);
        if (title != null && titleView != null){
            titleView.setText(title);
            titleView.invalidate();
        }
    }

    /**Returns the title over the plot view.
    * @return String title
    * */
    public String getTitle(){
        TextView titleView = this.rootView.findViewById(R.id.title_text_view);
        if (titleView != null){
            return "" + titleView.getText();
        }
        return "";
    }

    @Override
    public void onStart() {
        super.onStart();
        this.actualiseCurrentPlot();
        this.currentPlotView.invalidate();

        if (this.currentPlotView instanceof BarGraph){
            this.setTitle("" + this.getCurrentSelectedProtocol() + ": " +this.selectedCompareData);
        } else {
            this.setTitle(this.selectedCompareData);
        }
    }

    /**Sets the current chart to the given type and acualises it.
    * @param  type  {@link StatisticsFragment.ChartType ChartType}
    * */
    public void setChartType(ChartType type){
        boolean shouldChange = true;
        this.clearFilter();
        if (this.currentPlotView != null){
            if (type == ChartType.PIE_CHART){
                shouldChange = ! (this.currentPlotView instanceof PieGraph);
                // SET FILTER BUTTON HIDDEN
                ImageButton filterButton = this.getFilterButton();
                if (filterButton != null) filterButton.setVisibility(View.GONE);
            } else {
                if (this.pieGraph != null)
                     this.pieGraph.setVisibility(View.GONE);
                // SHOW FILTER BUTTON
                ImageButton filterButton = this.getFilterButton();
                if (filterButton != null) filterButton.setVisibility(View.VISIBLE);
            }
            if (type == ChartType.LINE_CHART){
                shouldChange = ! (this.currentPlotView instanceof LineGraph);
            } else {
                if (this.lineGraph != null)
                     this.lineGraph.setVisibility(View.GONE);
            }
            if (type == ChartType.BAR_CHART){
                shouldChange = ! (this.currentPlotView instanceof BarGraph);

            } else {
                if (this.barGraph != null)
                    this.barGraph.setVisibility(View.GONE);
            }
        }
        if (shouldChange){
            this.currentPlotView = this.getPlotViewForType(type);
            this.currentPlotView.setVisibility(View.VISIBLE);
        }
        this.actualiseCurrentPlot();
    }

    /**Returns the plot view for a given type.
    * @param type  {@link StatisticsFragment.ChartType ChartType}
    * */
    public View getPlotViewForType(ChartType type){
        switch (type){
            case PIE_CHART:
                return this.getPieGraphView();
            case LINE_CHART:
                return this.getLineGraphView();
            default:
                return this.getBarGraphView();
        }
    }

    /**Actualises the list view. Therefore it requiers the "currentData".*/
    public void actualiseLegendList(){
        StatisticListAdapter adapter = new StatisticListAdapter(this.getApplicationContext(), this.currentData);
        if (this.currentPlotView instanceof LineGraph){
            adapter.setValueFormatter(item -> String.format("%.02f", item.getValue2()) + " %" + " " + "("+ (item.getValue1().intValue())  +")");
        } else {
            adapter.setValueFormatter(item -> {
                int v = item.getValue2().intValue();
                return "" + v;
            });
        }
        this.legendListView.setAdapter(adapter);

        TextView tableHeaderTitleView = this.rootView.findViewById(R.id.table_header_title_textview);
        TextView tableHeaderValueView = this.rootView.findViewById(R.id.table_header_value_textview);
        if (this.currentPlotView instanceof LineGraph){
            tableHeaderTitleView.setText(FILTER_MENU_TITLE_ESSID);
            tableHeaderValueView.setText(TABLE_HEADER_VALUE_TITLE_ATTACKS_PERCENTAGE);
        }
        if (this.currentPlotView instanceof PieGraph){
            tableHeaderTitleView.setText(FILTER_MENU_TITLE_PROTOCOL);
            tableHeaderValueView.setText(TABLE_HEADER_VALUE_TITLE_ATTACKS_COUNT);
        }
        if (this.currentPlotView instanceof BarGraph){
            tableHeaderValueView.setText(TABLE_HEADER_VALUE_TITLE_ATTACKS_COUNT);
            if (this.selectedCompareData.equals(COMPARE_TITLE_AttacksPerBSSID)){
                tableHeaderTitleView.setText(FILTER_MENU_TITLE_BSSID);
            } else {
                tableHeaderTitleView.setText(FILTER_MENU_TITLE_ESSID);
            }
        }
        if (this.currentData == null || this.currentData.isEmpty()){
            tableHeaderTitleView.setText("");
            tableHeaderValueView.setText("");
        }
    }

    /*
    * MENU
    * */
    /**Opens the Bar Option Menu above the given anchor view.
    * @param anchorView View*/
     private void openBarSelectionMenuOnView(View anchorView){
        SimplePopupTable visualiseMenu = new SimplePopupTable(this.getActivity(), ob -> {
            if (ob instanceof AbstractPopupItem){
                AbstractPopupItem item = (AbstractPopupItem) ob;
                StatisticsFragment.this.userSelectMenuItem(item);
            }
        });
        visualiseMenu.setTitle(MENU_POPUP_TITLE);
        int id = 0;
        for(String title : StatisticsFragment.this.getMenuTitles()){
            SimplePopupItem item = new SimplePopupItem(this.getActivity());
            item.setTitle(title);
            item.setItemId(id);
            item.setSelected(false);
            visualiseMenu.addItem(item);
            id++;
        }
        visualiseMenu.showOnView(anchorView);
    }

    /**Will be called when the users selected an menu item (visualise menu / plot menu).
    * If the user selected "Protocols" this method sets the current plot type to piegraph.
    * Otherwise it will open a new dialog to select the comparison type.
    * */
    private void userSelectMenuItem(AbstractPopupItem item){
        // OPEN A DIALOG TO SPECIFY THE VISUALISE DATA
        if (item.getTitle().equals(MENU_TITLE_PROTOCOLS)){
            ChartType chartType = ChartType.PIE_CHART;
            this.selectedCompareData = COMPARE_TITLE_AttacksPerProtocol;
            this.setChartType(chartType);
            this.setTitle(COMPARE_TITLE_AttacksPerProtocol);
        }
        if (item.getTitle().equals(MENU_TITLE_NETWORK)){
            this.openNetworkDataDialog();
        }
        if (item.getTitle().equals(MENU_TITLE_ATTACKS)){
            this.openAttackDataDialog();
        }
    }

    /**Returns the menu titles (visualise menu / plot menu)*/
    private ArrayList<String> getMenuTitles(){
        ArrayList<String> titles = new ArrayList<>();
        titles.add(MENU_TITLE_PROTOCOLS);
        titles.add(MENU_TITLE_NETWORK);
        titles.add(MENU_TITLE_ATTACKS);
        return titles;
    }

    /*
    * PLOT DATA DIALOGS
    * */
//     private void openProtocolDataDialog(){
//        ArrayList<String> titles = this.getDialogProtocolDataTitle();
//        ChecklistDialog newFragment = new ChecklistDialog(DIALOG_PROTOCOLS_TITLE, titles, this.selectedData(titles), false , this);
//        newFragment.show(this.getActivity().getFragmentManager(), DIALOG_PROTOCOLS_TITLE);
//    }
    /**Opens the network comparison dialog*/
    private void openNetworkDataDialog(){
        ArrayList<String> titles = this.getDialogNetworkDataTitle();
        ChecklistDialog newFragment = new ChecklistDialog(DIALOG_NETWORK_TITLE, titles, this.selectedData(titles), false , this);
        newFragment.show(this.getActivity().getFragmentManager(), DIALOG_NETWORK_TITLE);
    }
    /**Opens the attack comparison dialog*/
    private void openAttackDataDialog(){
        ArrayList<String> titles = this.getDialogAttackDataTitle();
        ChecklistDialog newFragment = new ChecklistDialog(DIALOG_ATTACK_TITLE, titles, this.selectedData(titles), false , this);
        newFragment.show(this.getActivity().getFragmentManager(), DIALOG_ATTACK_TITLE);
    }

    /*
    *
    * DIALOG ACTION METHODS
    *
    * */
    /**
     * Will be called if the user selects the positiv button on an checklist dialog.
     * @param dialog  {@link ChecklistDialog ChecklistDialog}
     * */
     public void onDialogPositiveClick(ChecklistDialog dialog) {
        String title = dialog.getTitle();
        ArrayList<String> titles =dialog.getSelectedItemTitles();

        if (title.equals(FILTER_MENU_TITLE_PROTOCOLS)){
            //titles = titles.size() == 0 ? this.protocolTitles() : titles;
            this.filter.setProtocols(titles);
            this.actualiseCurrentPlot();
            return;
        }
        if (title.equals(FILTER_MENU_PROTOCOL_SINGLE_CHOICE_TITLE)){
            if (titles.size() == 0){
                titles = new ArrayList<>();
                titles.add(this.protocolTitles().get(0));
            }
            this.filter.setProtocols(titles);

            this.actualiseCurrentPlot();
            String fragTitle = "" + this.getCurrentSelectedProtocol() + ": " + this.selectedCompareData;
            this.setTitle(fragTitle);

            return;
        }
        if (title.equals(FILTER_MENU_TITLE_ESSID)){
            this.filter.setESSIDs(titles);
            this.actualiseCurrentPlot();

            return;
        }
        if (title.equals(FILTER_MENU_TITLE_BSSID)){
            this.filter.setBSSIDs(titles);
            this.actualiseCurrentPlot();

            return;
        }

        if (titles.size() != 0){
            String data = titles.get(0);
            this.setTitle(data);

            this.actualiseFilterButton();

            if (data.equals(COMPARE_TITLE_AttacksPerTime) || data.equals(COMPARE_TITLE_AttacksPerDate)){
                ChartType chartType = ChartType.LINE_CHART;
                this.selectedCompareData = data;
                this.setChartType(chartType);
                return;
            }
            if (data.equals(COMPARE_TITLE_AttacksPerBSSID) || data.equals(COMPARE_TITLE_AttacksPerESSID)){
                ChartType chartType = ChartType.BAR_CHART;
                this.selectedCompareData = data;
                this.setChartType(chartType);

                String fragTitle = "" + this.getCurrentSelectedProtocol() + ": " + this.selectedCompareData;
                this.setTitle(fragTitle);
                return;
            }
        }

    }

    /**
     * Will be called if the user selects the negativ button on an checklist dialog.
     * @param dialog  {@link ChecklistDialog ChecklistDialog}
     * */
    public void onDialogNegativeClick(ChecklistDialog dialog) {

    }
    /*
    *
    * DIALOG DATA
    *
    * */


//     private ArrayList<String> getDialogProtocolDataTitle(){
//        ArrayList<String> data = new ArrayList<String>();
//        data.add(COMPARE_TITLE_AttacksPerProtocol);
//        data.add(COMPARE_TITLE_UsesPerProtocol);
//        return data;
//    }
    /**
     *  Returns the Attacks comparison titles.
     * @return  ArrayList<String> the titles
     */
    private ArrayList<String> getDialogAttackDataTitle(){
        ArrayList<String> data = new ArrayList<>();
        data.add(COMPARE_TITLE_AttacksPerDate);
        data.add(COMPARE_TITLE_AttacksPerTime);
        return data;
    }
    /**
     *  Returns the network comparison titles.
     * @return  ArrayList<String> the titles
     */
    private ArrayList<String> getDialogNetworkDataTitle(){
        ArrayList<String> data = new ArrayList<>();
        data.add(COMPARE_TITLE_AttacksPerESSID);
        data.add(COMPARE_TITLE_AttacksPerBSSID);
        return data;
    }
    /**
     * DEFAULT
     *  Returns an boolean array with a default selection. Just the first object is true.
     * @return  boolean[] selected array
     */
    private boolean[] selectedData(ArrayList<String> data){
        boolean[] selected = new boolean[data.size()];
         // SET DEFAULT
        selected[0] = true;
        return selected;
    }

    /*
    *
    *  FILTER BUTTON
    *
    * */

    /**Paints the filter button if the current filter object is set.*/
    private void actualiseFilterButton(){
        if ((this.filter.isSet() && (!(this.currentPlotView instanceof BarGraph))|| (this.filter.hasATimestamp() || this.filter.hasBSSIDs() || this.filter.hasESSIDs()))){
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
     * Opens the filter menu above an given anchor view.
     * @param  anchor View
     */
     private void openFilterMenuOnView(View anchor){
        SimplePopupTable filterMenu = new SimplePopupTable(this.getActivity(), ob -> {
            if (ob instanceof  AbstractPopupItem){
                AbstractPopupItem item = (AbstractPopupItem) ob;
                StatisticsFragment.this.onFilterMenuItemSelected(item);
            }
        });

        filterMenu.setTitle(FILTER_MENU_POPUP_TITLE);
        for(String title : StatisticsFragment.this.filterMenuTitles()){
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
        filterMenu.showOnView(anchor);
    }

    /**
     * Will be called if the user selected an filter item.
     * @param  item  {@link AbstractPopupItem AbstractPopupItem}
     */
    private void onFilterMenuItemSelected(AbstractPopupItem item){
        if (item instanceof SplitPopupItem){
            SplitPopupItem sItem = (SplitPopupItem) item;
            this.wasBelowTimePicker = sItem.wasRightTouch;
            if (this.wasBelowTimePicker){
                this.openTimestampToFilterDialog();
            } else {
                this.openTimestampFromFilterDialog();
            }
            return;
        }
        String title = item.getTitle();
        if (title.equals(FILTER_MENU_TITLE_ESSID)){
            this.openESSIDFilterDialog();
        }
        if (title.equals(FILTER_MENU_TITLE_BSSID)){
            this.openBSSIDFilterDialog();
        }
        if (title.equals(FILTER_MENU_TITLE_PROTOCOL)){
            this.openFilterDialogSelectProtocol();
        }
        if (title.equals(FILTER_MENU_TITLE_PROTOCOLS)){
            this.openProtocolsFilterDialog();
        }
        if (title.equals(FILTER_MENU_TITLE_REMOVE)){
            this.clearFilter();
            this.actualiseCurrentPlot();
        }
    }

    /**
     * Return the menu titles of the filter menu.
     * @return ArrayList<String> filter menu title
     * */
    private ArrayList<String> filterMenuTitles(){
        ArrayList<String> titles = new ArrayList<>();
        if (this.currentPlotView instanceof LineGraph){
            titles.add(FILTER_MENU_TITLE_ESSID);
            titles.add(FILTER_MENU_TITLE_PROTOCOLS);
            titles.add(FILTER_MENU_TITLE_TIMESTAMP_ABOVE);
            if (this.filter.hasESSIDs() || this.filter.hasATimestamp() || (this.filter.getProtocols() != null  && this.filter.hasProtocols() && this.filter.getProtocols().size() != this.protocolTitles().size())){
                titles.add(FILTER_MENU_TITLE_REMOVE);
            }
        } else {
            titles.add(FILTER_MENU_TITLE_PROTOCOL);
            String protocol = this.getCurrentSelectedProtocol();
            if (protocol.length() > 0){
                if (this.selectedCompareData.equals(COMPARE_TITLE_AttacksPerBSSID)){
                    titles.add(FILTER_MENU_TITLE_BSSID);
                } else {
                    // DEFAULT
                    titles.add(FILTER_MENU_TITLE_ESSID);
                }
            }
            titles.add(FILTER_MENU_TITLE_TIMESTAMP_ABOVE);
            if (this.filter.hasATimestamp() || this.filter.hasESSIDs() || this.filter.hasBSSIDs()
                    || (this.currentPlotView instanceof LineGraph && this.filter.hasProtocols())){
                titles.add(FILTER_MENU_TITLE_REMOVE);
            }
        }
        return titles;
    }

    /**
     * Opens a multiple protocol checklist dialog
     */
    private void openProtocolsFilterDialog(){
        ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_PROTOCOLS,
                                                    this.protocolTitles(),
                                                    this.selectedProtocols(),
                                                    true ,
                                                    this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_PROTOCOLS);
    }

    /**
     * Opens a single protocol checklist dialog
     */
    private void openFilterDialogSelectProtocol(){
        ArrayList<String> titles = this.protocolTitles();
        boolean[] selected = new boolean[titles.size()];
        int i = 0;
        for (String title : titles){
            selected[i] = title.equals(this.getCurrentSelectedProtocol());
            i++;
        }
        ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_PROTOCOL_SINGLE_CHOICE_TITLE, titles, selected, false , this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_PROTOCOL_SINGLE_CHOICE_TITLE);
    }

    /**
     * Opens a multiple essid checklist dialog
     */
    private void openESSIDFilterDialog(){
        ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_ESSID, this.essids(), this.selectedESSIDs(), true , this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_ESSID);
    }

    /**Opens a multiple bssid checlist dialog.*/
    private void openBSSIDFilterDialog(){
        ChecklistDialog newFragment = new ChecklistDialog(FILTER_MENU_TITLE_BSSID, this.bssids(), this.selectedBSSIDs(), true , this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_BSSID);
    }

    /** Opens a minimal timestamp dialog.**/
    private void openTimestampFromFilterDialog(){
        this.wasBelowTimePicker = false;
        DateTimeDialogFragment newFragment = new DateTimeDialogFragment(this.getActivity());
        newFragment.setDateChangeListener(this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_TIMESTAMP_ABOVE);

        if (this.filter.aboveTimestamp != Long.MIN_VALUE)newFragment.setDate(this.filter.aboveTimestamp);
    }

    /** Opens the maximal timestamp dialog.*/
    private void openTimestampToFilterDialog(){
        this.wasBelowTimePicker = true;
        DateTimeDialogFragment newFragment = new DateTimeDialogFragment(this.getActivity());
        newFragment.setDateChangeListener(this);
        newFragment.show(this.getActivity().getFragmentManager(), FILTER_MENU_TITLE_TIMESTAMP_BELOW);
        if (this.filter.belowTimestamp != Long.MAX_VALUE) newFragment.setDate(this.filter.belowTimestamp);
    }

    /** Returns all essids
     * If the current plot is a bar graph, it just return all possible essids for the selected protocol
     * @return ArrayList<String> essids
     * */
    public ArrayList<String> essids(){
        ArrayList<String> essids;
        if (this.currentPlotView instanceof BarGraph){
            essids = daoHelper.getNetworkRecordDAO().getUniqueESSIDRecordsForProtocol(this.getCurrentSelectedProtocol());
        } else {
            essids = daoHelper.getNetworkRecordDAO().getUniqueESSIDRecords();
        }
        return essids;
    }
    /** Returns a boolean array. The position in the array will be true, if the essid is selected in the filter.
     * @return boolean[] selected essids*/
    public boolean[] selectedESSIDs(){
        ArrayList<String> essids = this.essids();
        boolean[] selected = new boolean[essids.size()];

        int i = 0;
        for(String essid : essids){
            selected[i] =(this.filter.getESSIDs().contains(essid));
            i++;
        }
        return selected;
    }

    /** Returns all bssids
     * If the current plot is a bar graph, it just return all possible bssids for the selected protocol
     * @return ArrayList<String> bssids
     * */
    public ArrayList<String> bssids(){
        ArrayList<String> bssids ;
        if (this.currentPlotView instanceof BarGraph){
            bssids = daoHelper.getNetworkRecordDAO().getUniqueBSSIDRecordsForProtocol(this.getCurrentSelectedProtocol());
        } else {
            bssids = daoHelper.getNetworkRecordDAO().getUniqueBSSIDRecords();
        }
        return bssids;
    }
    /** Returns a boolean array. The position in the array will be true, if the bssid is selected in the filter.
     * @return boolean[] selected bssids*/
    public boolean[] selectedBSSIDs(){
        ArrayList<String> bssids = this.bssids();

        boolean[] selected = new boolean[bssids.size()];

        int i = 0;
        for(String bssid : bssids){
            selected[i] =(this.filter.getBSSIDs().contains(bssid));
            i++;
        }
        return selected;
    }

    /**Will be called if the user selects an date on the timestamp dialog*/
    public void onDateTimePickerPositiveClick(DateTimeDialogFragment dialog) {
        if(this.wasBelowTimePicker){
            this.filter.setBelowTimestamp(dialog.getDate());
        } else {
            this.filter.setAboveTimestamp(dialog.getDate());
        }
        this.actualiseCurrentPlot();
    }

    /**Will be called if the user cancels an date selection on the timestamp dialog*/
    public void onDateTimePickerNegativeClick(DateTimeDialogFragment dialog) {
        if(this.wasBelowTimePicker){
            this.filter.setBelowTimestamp(Long.MAX_VALUE);
        } else {
            this.filter.setAboveTimestamp(Long.MIN_VALUE);
        }
        this.actualiseCurrentPlot();
    }

    /*
    *
    *  PLOT TYPES
    *
    * **/
    /**Returns the current pie graph.
     * @return PieGraph current piegraph*/
     public PieGraph getPieGraphView(){
        if (this.pieGraph == null) {
            this.pieGraph = new PieGraph(this.getApplicationContext());
            LinearLayout plotLayout = this.rootView.findViewById(R.id.plot_layout);
            plotLayout.addView(this.pieGraph);
            this.pieGraph.setOnSliceClickedListener(StatisticsFragment.this::onSliceClick);
        }
         return this.pieGraph;
    }

    /**
     * Returns the current  {@link LineGraph Linegraph} .
     * @return LineGraph current line graph
     */
    public LineGraph getLineGraphView(){
        if (this.lineGraph == null) {
            this.lineGraph = new LineGraph(this.getActivity());
            LinearLayout plotLayout = this.rootView.findViewById(R.id.plot_layout);
            plotLayout.addView(this.lineGraph);
            this.lineGraph.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
        return this.lineGraph;
    }

    /**
     * Returns the current  {@link BarGraph BarGraph} .
     * @return BarGraph the current bar graph
     */
    public BarGraph getBarGraphView(){
        if (this.barGraph == null) {
            this.barGraph = new BarGraph(this.getActivity());
            LinearLayout plotLayout = this.rootView.findViewById(R.id.plot_layout);
            this.barGraph.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT ));
            plotLayout.addView(this.barGraph);
            this.barGraph.setShowBarText(false);
            this.barGraph.setPopupImageID(R.drawable.popup_black);
            this.barGraph.setOnBarClickedListener(StatisticsFragment.this::onBarClick);
        }
        return this.barGraph;
    }

    /*
    *  FEED PLOTS WITH DATA
    * */

    /**
     * Sets the data for the given PieGraph
     * @param piegraph  {@link PieGraph PieGraph}
     */
     public void setPieGraphData(PieGraph piegraph){
        this.currentData = this.getPieData();
        if (this.currentData == null){
            this.currentData = new ArrayList<>();
        }

        piegraph.removeSlices();

        for (PlotComparisonItem item : this.currentData){
            PieSlice slice = new PieSlice();
            slice.setColor(item.getColor());
            Double value2 = item.getValue2();
            float v = value2.floatValue();
            slice.setValue(v);
            slice.setTitle(item.getTitle());
            piegraph.addSlice(slice);
        }
        //piegraph.invalidate();
    }


    /**
     * Sets the data for the given LineGraph
     * @param linegraph {@link LineGraph Linegraph}
     */
    public void setLineGraphData(LineGraph linegraph){
        this.currentData = this.getLineData();
        if (this.currentData == null){
            this.currentData = new ArrayList<>();
        }

        linegraph.removeAllLines();
        double rangeMax_Y = 0;
        double rangeMin_Y = 0;

        double rangeMax_X = 0;
        double rangeMin_X = 0;

        int count = 0;
        for (PlotComparisonItem lineItem : this.currentData){
            ArrayList<PlotComparisonItem> data = lineItem.getChildItems();
            //int index = 0;
            Line l = new Line();
            int lineColor = lineItem.getColor();
            l.setColor(lineColor);

            for (PlotComparisonItem pointItem : data){
                LinePoint p = new LinePoint();
                p.setX(pointItem.getValue1());
                Double value2 = pointItem.getValue2();
                p.setY(value2);
                p.setColor(lineColor);
                l.addPoint(p);
                rangeMax_Y = Math.max(pointItem.getValue2(), rangeMax_Y);
                rangeMax_X = Math.max(pointItem.getValue1(), rangeMax_X);

                if (count != 0){
                    rangeMin_Y = Math.min(pointItem.getValue2(), rangeMin_Y);
                    rangeMin_X = Math.min(pointItem.getValue1(), rangeMin_X);
                } else {
                    rangeMin_Y = pointItem.getValue2();
                    rangeMin_X = pointItem.getValue1();
                }
                //index++;
                count++;
            }
            linegraph.addLine(l);
        }
        // add a bit more space
        rangeMax_Y++;
        rangeMin_Y--;

        boolean shouldUseDate = this.selectedCompareData.equals(COMPARE_TITLE_AttacksPerDate);
        if (shouldUseDate){
            linegraph.resetXLimits();

            if (this.filter.hasBelowTimestamp()){
                rangeMax_X = Math.max(this.filter.belowTimestamp, rangeMax_X);
            }
            if (this.filter.hasAboveTimestamp()){
                rangeMin_X = Math.min(this.filter.aboveTimestamp, rangeMin_X);
            }

            if (rangeMax_X == rangeMin_X){
                double aDay = 60*60*24*1000;
                rangeMax_X+= aDay;
                rangeMin_X-= aDay;
            }

            double stepRange = (rangeMax_X - rangeMin_X)/(60*60*24*1000);
            linegraph.setxAxisStep(Math.max(1, (float) Math.min(stepRange, 4)));

            linegraph.setRangeX(rangeMin_X  , rangeMax_X);

            linegraph.setConverter(new LineGraph.AxisDataConverter() {
                @Override
                public String convertDataForX_Position(double x) {
                    return StatisticsFragment.this.getDateAsDayString((long)x);
                }
                @Override
                public  String convertDataForY_Position(double y){
                    return "" + (long)y;
                }
            });
        } else {
            linegraph.setxAxisStep(12.f);
            linegraph.setRangeX(0, 24);
            linegraph.setConverter(null);
        }

        int maxY = (int)(rangeMax_Y - rangeMin_Y);
        linegraph.setYAxisStep(Math.min(maxY, 5));
        int yStep = (int)linegraph.getyAxisStep();
        if ((maxY % yStep) != 0) {
            maxY = maxY + (yStep - (maxY % yStep));
        }
        rangeMax_Y = rangeMin_Y + maxY;
        linegraph.setRangeY(rangeMin_Y, rangeMax_Y);
        linegraph.setLineToFill(0);
    }

    /**
    * Set the graph data to the given graph
    * @param bargraph {@link BarGraph BarGraph}
    * */
    public void setBarGraphData(BarGraph bargraph){
        this.currentData = this.getBarData();
        if (this.currentData == null){
            this.currentData = new ArrayList<>();
        }

        ArrayList<Bar> bars = new ArrayList<>();

        for (PlotComparisonItem item : this.currentData){
            Bar d = new Bar();
            d.setColor(item.getColor());
            Long value2 =  item.getValue2().longValue();
            d.setName("");
            d.setValue(value2.floatValue());
            bars.add(d);
        }

        barGraph.setBars(bars);
    }

    /*
    *
    *  FETCH & ACTUALISE RECORD DATA
    *
    * */
    /**
     * Returns the DataBaseHandler result for the current filter.
     * @return records {@link java.util.ArrayList}, {@link RecordAll Record}
     */
     public ArrayList<RecordAll> getFetchedRecords(){
        if (this.filter == null) this.clearFilter();
        return this.daoHelper.getAttackRecordDAO().getRecordsForFilter(this.filter);
    }

    /**Actualises the current plot in a background thread.*/
     public void actualiseCurrentPlot(){
         this.spinner.setVisibility(View.VISIBLE);

         this.actualiseFilterButton();

         LinearLayout plotLayout = this.rootView.findViewById(R.id.plot_layout);

         if (this.barGraph != null)
            this.barGraph.setVisibility(View.GONE);
         if (this.lineGraph != null)
            this.lineGraph.setVisibility(View.GONE);
         if (this.pieGraph != null)
            this.pieGraph.setVisibility(View.GONE);

        View plot = this.currentPlotView;
         if (plot == null){
             this.currentPlotView = this.getPieGraphView();
             plot = this.currentPlotView;
         }
         if (plot.getParent() != null && !plot.getParent().equals(plotLayout)){
             LinearLayout linLayout = (LinearLayout)plot.getParent();
             linLayout.removeView(plot);
             plot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT ));
             plotLayout.addView(plot);
         }

         this.currentPlotView = plot;

         final LinearLayout thePlotlayout = plotLayout;

         if (this.loader != null && this.loader.isAlive()) this.loader.interrupt();

         this.loader = null;

         this.loader = new Thread(new Runnable() {
             @Override
             public void run() {
                 this.loadDataInBackground();
                 this.actualiseUI();
             }

             private void loadDataInBackground(){
                 View plot = StatisticsFragment.this.currentPlotView;
                 if (plot instanceof PieGraph){
                     PieGraph pie = (PieGraph) plot;
                     StatisticsFragment.this.setPieGraphData(pie);
                 }
                 if (plot instanceof BarGraph){
                     BarGraph bar = (BarGraph) plot;
                     StatisticsFragment.this.setBarGraphData(bar);
                 }
                 if (plot instanceof LineGraph){
                     LineGraph line = (LineGraph)plot;
                     StatisticsFragment.this.setLineGraphData(line);
                 }
             }

             private void actualiseUI(){

                 Activity actv = StatisticsFragment.this.getActivity();

                 if (actv != null){
                     actv.runOnUiThread(() -> {
                         // SET VISIBILITY
                         View plot1 = StatisticsFragment.this.currentPlotView;

                         if (plot1 instanceof PieGraph){
                             // HIDE FILTER BUTTON
                             ImageButton filterButton = StatisticsFragment.this.getFilterButton();
                             if (filterButton != null) filterButton.setVisibility(View.GONE);
                         } else {
                             if (StatisticsFragment.this.pieGraph != null){
                                 StatisticsFragment.this.pieGraph.setVisibility(View.GONE);
                                 if (StatisticsFragment.this.pieGraph.getParent() != null){
                                     thePlotlayout.removeView(StatisticsFragment.this.pieGraph);
                                 }
                             }
                             // SHOW FILTER BUTTON
                             ImageButton filterButton = StatisticsFragment.this.getFilterButton();
                             if (filterButton != null) filterButton.setVisibility(View.VISIBLE);
                         }
                         if (! (plot1 instanceof BarGraph)){
                             if (StatisticsFragment.this.barGraph != null){
                                 StatisticsFragment.this.barGraph.setVisibility(View.GONE);
                                 if (StatisticsFragment.this.barGraph.getParent() != null){
                                     thePlotlayout.removeView(StatisticsFragment.this.barGraph);
                                 }
                             }
                         }
                         if (!(plot1 instanceof LineGraph)){
                             if (StatisticsFragment.this.lineGraph != null){
                                 StatisticsFragment.this.lineGraph.setVisibility(View.GONE);
                                 if (StatisticsFragment.this.lineGraph.getParent() != null){
                                     thePlotlayout.removeView(StatisticsFragment.this.lineGraph);
                                 }
                             }
                         }

                         plot1.setVisibility(View.VISIBLE);

                         if (plot1.getParent() == null){
                             thePlotlayout.addView(plot1);
                         }
                         StatisticsFragment.this.actualiseLegendList();
                         StatisticsFragment.this.currentPlotView.bringToFront();
                         StatisticsFragment.this.currentPlotView.invalidate();

                         StatisticsFragment.this.spinner.setVisibility(View.GONE);

                         StatisticsFragment.this.showEmptyDataNotification();
                     });
                 }
             }
         });

        this.loader.start();
     }

    /**
     * Shows a small toast if the data to show is empty (no records).
     */
    private void showEmptyDataNotification(){
        if (this.noDataNotificationToast == null){
            this.noDataNotificationToast =  Toast.makeText(getApplicationContext(), R.string.no_data_notification, Toast.LENGTH_SHORT);
        }
        if (this.getFilterButton().getVisibility() == View.VISIBLE){
            this.noDataNotificationToast.setText(R.string.no_data_notification);
        } else {
            this.noDataNotificationToast.setText(R.string.no_data_notification_no_filter);
        }
        if (this.currentData == null || this.currentData.isEmpty()){
            this.noDataNotificationToast.show();
        }
    }


    /** Calculates and returns the data for the piegraph
     * @return ArrayList<PlotComparisonItem> data */
    public ArrayList<PlotComparisonItem> getPieData(){
        // DEFAULT
        return this.attacksPerProtocols();
    }
    /** Calculates and returns the data for the bargraph.
     * @return ArrayList<PlotComparisonItem> data */
    public ArrayList<PlotComparisonItem> getBarData(){
        String protocol = this.getCurrentSelectedProtocol();

        if (protocol.length() > 0){
            if (this.selectedCompareData.equals(COMPARE_TITLE_AttacksPerESSID)){
                return this.attacksPerESSID(protocol);
            }
            // DEFAULT
            return this.attacksPerBSSID(protocol);
        }
        // Nothing available
        return new ArrayList<PlotComparisonItem>();
    }
    /** Calculates and returns the data for the linegraph
     * @return ArrayList<PlotComparisonItem> data */
    public ArrayList<PlotComparisonItem> getLineData(){
        return this.attacksPerTime();
    }

    /*
    *  DATA SOURCE
    * */

    /*PROTOCOLS OVERVIEW*/

    /**
     * Returns the attacks per protocols comparison result.
     * The returned data is resized to the specified limit.
     * @return ArrayList<PlotComparisonItem>
     */
     public synchronized ArrayList<PlotComparisonItem> attacksPerProtocols(){
        ArrayList<PlotComparisonItem> plotItems = new ArrayList<PlotComparisonItem>();
         int index = 0;
        for (String title : this.getSelectedProtocolTitles()){
            int attacksCount = this.daoHelper.getAttackRecordDAO().getAttackPerProtocolCount(title);
            if (attacksCount == 0) continue;
            PlotComparisonItem item = new PlotComparisonItem(title,this.getColor(index), 0., (double) attacksCount);
            plotItems.add(item);
            index++;
        }
         Collections.sort(plotItems, (s1, s2) -> s2.getValue2().compareTo(s1.getValue2()));
         return this.resizeData(plotItems);
    }


    /*
    *  LINE PLOT DATA
    */

    /**
     * Returns the line graph data responding to the selectedCompareData key.
     * The returned data is resized to the specified limit.
     * @return plotItems {@link PlotComparisonItem PlotComparisonItems}
     */
    public ArrayList<PlotComparisonItem> attacksPerTime(){
        HashMap<String,HashMap<Long, ArrayList<RecordAll> > > lineMap = new HashMap<>();

        boolean shouldUseDate = this.selectedCompareData.equals(COMPARE_TITLE_AttacksPerDate);

        ArrayList<RecordAll> records = this.getFetchedRecords();
        for (RecordAll record : records){
            long timestamp = record.getTimestamp();
            long time = 0;
            if (shouldUseDate){
                time = this.getDateFromMilliseconds(timestamp);
            } else {
                time = this.getDayHourFromDate(timestamp);
            }

            // GET CORRECT MAP
            HashMap<Long, ArrayList<RecordAll> > recordMap;
            String groupKey = record.getSsid();
            if (lineMap.containsKey(groupKey)){
                recordMap = lineMap.get(record.getSsid());
            } else {
                recordMap = new HashMap<>();
                lineMap.put(groupKey, recordMap);
            }

            // GET LIST OF RECORDS
            ArrayList<RecordAll> list;
            if (recordMap.containsKey(time)){
                list = recordMap.get(time);
            } else {
                list = new ArrayList<>();
                recordMap.put(time, list);
            }
            list.add(record);
        }

        ArrayList<PlotComparisonItem> plotItems = new ArrayList<>();

        int index = 0;
        for (String groupKey : lineMap.keySet()){
            HashMap<Long, ArrayList<RecordAll> > recordMap = lineMap.get(groupKey);
            ArrayList<PlotComparisonItem> singleLineItems = new ArrayList<>();

            int numbOfAttacks = 0;
            for (long time : recordMap.keySet()){
                ArrayList<RecordAll>list = recordMap.get(time);
                if (list.size() == 0) continue;
                PlotComparisonItem item = new PlotComparisonItem(this.getHourAsTimeString(time), 0 , (double)time, (double) list.size());
                singleLineItems.add(item);
                numbOfAttacks +=list.size();
            }

            Collections.sort(singleLineItems, (s1, s2) -> s1.getValue1().compareTo(s2.getValue1()));

            double itemValue = (((double)numbOfAttacks / (double)records.size())*100.);
            PlotComparisonItem item = new PlotComparisonItem(groupKey, this.getColor(index), (double) numbOfAttacks, itemValue);
            item.setChildItems(singleLineItems);
            plotItems.add(item);
            index++;
        }
        Collections.sort(plotItems, (s1, s2) -> s2.getValue2().compareTo(s1.getValue2()));
        return plotItems;
    }

    /*
    *  BAR PLOT DATA
    */
    /**Returns plotitems for the comparison "attacks per bssid"
     * The returned data is resized to the specified limit.
     * @return ArrayList of  {@link PlotComparisonItem PlotComparisonItems}
    */
    public ArrayList<PlotComparisonItem> attacksPerBSSID(String protocol){
        LogFilter filter = new LogFilter();
        ArrayList<String> protocollist = new ArrayList<>();
        filter.setAboveTimestamp(this.filter.getAboveTimestamp());
        filter.setBelowTimestamp(this.filter.getBelowTimestamp());
        filter.setBSSIDs(this.filter.getBSSIDs());
        protocollist.add(protocol);
        filter.setProtocols(protocollist);

        ArrayList<PlotComparisonItem> plotItems = this.daoHelper.getNetworkRecordDAO().attacksPerBSSID(filter);

        Collections.sort(plotItems, new Comparator<PlotComparisonItem>() {
            @Override
            public int compare(PlotComparisonItem s1, PlotComparisonItem s2) {
                return s2.getValue2().compareTo(s1.getValue2());
            }
        });
        return this.resizeData(plotItems);
    }
    /**Returns plotitems for the comparison "attacks per essid"
    * @return ArrayList of  {@link PlotComparisonItem PlotComparisonItems}
    */
    public ArrayList<PlotComparisonItem> attacksPerESSID(String protocol){
        LogFilter filter = new LogFilter();
        filter.setAboveTimestamp(this.filter.getAboveTimestamp());
        filter.setBelowTimestamp(this.filter.getBelowTimestamp());
        filter.setESSIDs(this.filter.getESSIDs());
        ArrayList<String> protocollist = new ArrayList<String>();
        protocollist.add(protocol);
        filter.setProtocols(protocollist);

        ArrayList<PlotComparisonItem> plotItems = this.daoHelper.getNetworkRecordDAO().attacksPerESSID(filter); //new ArrayList<PlotComparisonItem>();

        Collections.sort(plotItems, (s1, s2) -> s2.getValue2().compareTo(s1.getValue2()));

        return this.resizeData(plotItems);
    }

    /**
    * This will normalize the given list of plot items to the specified length of MAX_NUMBER_OF_CHART_OBJECTS.
    * Creates an "others" group, containing all objects after the (MAX_NUMBER_OF_CHART_OBJECTS - 1)th object from the given list.
    * If the given list does contain MAX_NUMBER_OF_CHART_OBJECTS+1 or more objects, nothing will change.
    *
    * @param plotItems to normalize  {@link PlotComparisonItem PlotComparisonItems}
    * @return the normalized ArrayList of  {@link PlotComparisonItem PlotComparisonItems}
    */
    private ArrayList<PlotComparisonItem> resizeData(ArrayList<PlotComparisonItem> plotItems){
        if (plotItems != null){
            if (plotItems.size() > MAX_NUMBER_OF_CHART_OBJECTS && MAX_NUMBER_OF_CHART_OBJECTS > 1){
                ArrayList<PlotComparisonItem> copy = new ArrayList<>();
                ArrayList<PlotComparisonItem> others = new ArrayList<>();
                double valueOfOthers = 0;

                for (int i = 0; i < plotItems.size(); i++){
                    if (i < MAX_NUMBER_OF_CHART_OBJECTS - 1){
                        PlotComparisonItem item = plotItems.get(i);
                        item.setColor(this.getColor(i));
                        copy.add(plotItems.get(i));
                    } else {
                        PlotComparisonItem item = plotItems.get(i);
                        others.add(item);
                        valueOfOthers+=item.getValue2();
                    }
                }
                PlotComparisonItem otherItem = new PlotComparisonItem(OTHER_CHART_TITLE, this.getOtherColor(), 0., valueOfOthers);
                otherItem.setChildItems(others);
                copy.add(otherItem);

                Collections.sort(copy, (s1, s2) -> s2.getValue2().compareTo(s1.getValue2()));

                return copy;
            }
        }
        return plotItems;
    }

    /*
    * FILTER STUFF
    * */

    /**
    * Returns the first selected protocol from all selected protocols.
    * If no protocol is selected, it return the first protocol in the protocolTitles() list.
    * @return String protocolName
    * */
    private String getCurrentSelectedProtocol(){
         ArrayList<String> protocolTitles = this.getSelectedProtocolTitles();
         if (protocolTitles != null && protocolTitles.size() != 0){
             return  protocolTitles.get(0);
         }
         return this.protocolTitles().get(0);
     }

    /**
     * Return all Protocols
    *
    * @return ArrayList<String> protocolNames
    * */
     public ArrayList<String> protocolTitles(){
        ArrayList<String> titles = new ArrayList<>();
        for (String protocol : this.getResources().getStringArray(
                R.array.protocols)) {
            titles.add(protocol);
        }

	    titles.add("PORTSCAN");

	    return titles;
    }
    /**
    * Returns a boolean array containing a bool value for each protocol. If the value is true, the filter object contains the protocol.
    * The array sequence equates to the protocolTitles() list.
    * @return boolean[] selection array
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
    public ArrayList<String> getSelectedProtocolTitles(){
        ArrayList<String> knownProtocols = this.protocolTitles();
        if (this.filter.hasProtocols()){
            ArrayList<String> titles = new ArrayList<>();
            int i =0;
            for (boolean b : this.selectedProtocols()){
                if (b){
                    String title = knownProtocols.get(i);
                    titles.add(title);
                }
                i++;
            }
            return titles;
        }
        return this.protocolTitles();
    }
    /*
    *
    * COLOR STUFF
    *
    * */

    /** Returns the color for the other group
     * @return int color*/
    public int getOtherColor(){
        return Color.argb(255, 80, 80, 80); // grey
	}
    /** Returns the color for the given index
     * @return int color*/
    public Integer getColor(int index) {
		return ColorSequenceGenerator.getColorForIndex(index);
    }

    /** Returns the Plot layout.
     *
     * @return LinearLayout plot layout
     */
    public LinearLayout getPlotLayout(){
        if (this.rootView != null){
            return (LinearLayout) this.rootView.findViewById(R.id.plot_layout);
        } else {
            return null;
        }
    }

    /*
     *
     *  FILTER STUFF
     *
     * */
    /**
     * Returns true if the current filter is set for a given filter menu title.
     * @param  title of the filter menu item {@link String String}
     * @return boolean b
     */
     private boolean isFilterSetForTitle(String title){
        if (title.equals(FILTER_MENU_TITLE_BSSID)){
            return this.filter.hasBSSIDs();
        }
        if (title.equals(FILTER_MENU_TITLE_ESSID)){
            return this.filter.hasESSIDs();
        }
        if (title.equals(FILTER_MENU_TITLE_PROTOCOLS)){
            return (this.filter.getProtocols() != null  && this.filter.hasProtocols() && this.filter.getProtocols().size() != this.protocolTitles().size());
        }
        if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_BELOW)){
            return this.filter.hasBelowTimestamp();
        }
        if (title.equals(FILTER_MENU_TITLE_TIMESTAMP_ABOVE)){
            return this.filter.hasAboveTimestamp();
        }
        return false;
    }

    /**
     * Clears the current filter.
     */
    private void clearFilter(){
        if(filter == null) this.filter = new LogFilter();
        this.filter.clear();
    }

    /*
    *
    *  DATE TRANSFORMATION
    *
    */

    /**
     * Returns the current hour from a date.
     * @param timeInMillis long
     * @return milliseconds in long
     */
    public long getDayHourFromDate(long timeInMillis){

        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis (timeInMillis);
        int hour    = calendar.get(Calendar.HOUR_OF_DAY);
        //int min     = calendar.get(Calendar.MINUTE);

        return hour;

    }
    /**
     * Returns the current date without the seconds, minutes, hours.
     * @param timeInMillis long
     * @return long date with time 00:00:00
     * */
    public long getDateFromMilliseconds(long timeInMillis){
        long millisInDay = 60 * 60 * 24 * 1000;
        return (timeInMillis / millisInDay) * millisInDay;

    }

    /**
    *  Returns the given hour as a formated string.
     * Format: "HH:00"
    * */
    private String getHourAsTimeString(long hour) {
        return "" + hour + ":00";
    }

    //static final DateFormat dateFormat = new SimpleDateFormat("d.M.yy");

    /**Returns a date as a formated string
     * @param timeStamp date
     * @return String date format is localised*/
    @SuppressLint("SimpleDateFormat")
    private String getDateAsDayString(long timeStamp) {
        try {
            Date netDate = (new Date(timeStamp));
            DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(this.getActivity());
            return dateFormat.format(netDate);
            //return dateFormat.format(netDate);
        } catch (Exception ex) {
            return "xx";
        }
    }

    /**Returns a date as a formated string
     * @param timeStamp date
     * @return String date format (H:mm  dd/MM/yyyy)*/
    private String getDateAsString(long timeStamp) {
        try {
            DateFormat sdf = new SimpleDateFormat("H:mm  dd.MM.yyyy");
            Date netDate = (new Date(timeStamp));
            return sdf.format(netDate);
        } catch (Exception ex) {
            return "xx";
        }
    }

    /**
     * USERINTERACTION
     */
    /**
     * Will be called if the users taps on a list row.
     * @param index int
     */
    private void userTappedOnLegendItem(int index){
        if (index < this.currentData.size()){
            PlotComparisonItem item = this.currentData.get(index);
            ArrayList<String> selectedData;
            String groupingKey = null;
            selectedData = new ArrayList<>();

            if (item.getChildItems() == null){
                selectedData.add(item.getTitle());
            } else {
                for (PlotComparisonItem other : item.getChildItems()){
                    selectedData.add(other.getTitle());
                }
            }
            LogFilter filter = new LogFilter();
            if (this.currentPlotView instanceof PieGraph){
                filter.setProtocols(selectedData);
                if(selectedData != null && selectedData.size() > 1){
                    groupingKey = MainActivity.getInstance().getResources().getString(R.string.rec_protocol);
                }
            }
            if (this.currentPlotView instanceof BarGraph){

                if (this.selectedCompareData.equals(COMPARE_TITLE_AttacksPerESSID)){
                    filter.setESSIDs(selectedData);
                    groupingKey = MainActivity.getInstance().getResources().getString(R.string.ESSID);
                } else {
                    filter.setBSSIDs(selectedData);
                    groupingKey = MainActivity.getInstance().getResources().getString(R.string.BSSID);

                }
                ArrayList<String> currentSelectedProtocol = new ArrayList<>();
                currentSelectedProtocol.add(this.getCurrentSelectedProtocol());
                filter.setProtocols(currentSelectedProtocol);
            }
            if (this.currentPlotView instanceof  LineGraph){
                selectedData = new ArrayList<>();
                selectedData.add(item.getTitle());
                filter.setESSIDs(selectedData);
                filter.setProtocols(this.filter.getProtocols());
                groupingKey = MainActivity.getInstance().getResources().getString(R.string.ESSID);
            }

            if (this.filter.hasATimestamp()){
                filter.setAboveTimestamp(this.filter.getAboveTimestamp());
                filter.setBelowTimestamp(this.filter.getBelowTimestamp());
            }

            this.pushRecordOverviewForFilter(filter, groupingKey);
        }
    }

    /**Will be called if the user clicked on a slice
     * @param index of the slice (int)*/
    public void onSliceClick(int index){
    }
    /**Will be called if the user clicked on a bar
     * @param index of the bar (int)*/
    public void onBarClick(int index){
        this.userTappedOnLegendItem(index);
    }

    /**
     * Displays a record over view fragment.
     * @param filter  {@link LogFilter LogFilter}
     * @param groupingKey String, key to group the attack list in the RecordOverview
     */
    private void pushRecordOverviewForFilter(LogFilter filter, String groupingKey){
        // Get the FragmentManager from the MainActivity.
        // The FragmentManager handles every pushing / popping action of a Fragment.
        FragmentManager fm = this.getActivity().getFragmentManager();

        if (fm != null){
            // Create a new instance of your Fragment.
            RecordOverviewFragment newFragment = new RecordOverviewFragment();
            // Set true, if the user can navigate backwards from the new pushed fragment.
	        newFragment.setUpNavigatible(true);

            // Set a Filter Object if needed.
            // Otherwise the RecordOverviewFragment will display all possible records / attacks.
            // With the filter object you also can change the sorting, by changing the sorttype of it.
            if (filter != null) newFragment.setFilter(filter);

            // Set a grouping key, if you want a other grouping behaviour than the default (timestamp).
            if (groupingKey != null && groupingKey.length() != 0) newFragment.setGroupKey(groupingKey);

            // Push new Fragment
            MainActivity.getInstance().injectFragment(newFragment);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(rootView!=null) {
            rootView=null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(rootView!=null) {
            unbindDrawables(rootView);
            rootView=null;
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
