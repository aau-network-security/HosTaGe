package dk.aau.netsec.hostage.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.echo.holographlibrary.Bar;
import com.echo.holographlibrary.BarGraph;
import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LineGraph;
import com.echo.holographlibrary.LinePoint;
import com.echo.holographlibrary.PieGraph;
import com.echo.holographlibrary.PieSlice;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import dk.aau.netsec.hostage.ui.dialog.DateTimePickerDialog;
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
public class StatisticsFragment extends TrackerFragment implements ChecklistDialog.ChecklistDialogListener, DateTimePickerDialog.DateTimeSelected {

    // MINIMAL 2
    static final int MAX_NUMBER_OF_CHART_OBJECTS = 6;
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

    private DAOHelper daoHelper;

    private ListView legendListView;

    private Snackbar noDataNotificationSnackbar;
    private String selectedCompareData;

    private Menu optionsMenu;

    /**
     * The Charttype.
     * PIE_CHART = 0
     * BAR_CHART = 1
     * LINE_CHART = 2
     */
    public enum ChartType {
        PIE_CHART(0),
        BAR_CHART(1),
        LINE_CHART(2);

        ChartType(int value) {
        }

        static public ChartType create(int value) {
            if (value < 0 || value >= ChartType.values().length) return ChartType.PIE_CHART;
            return ChartType.values()[value];
        }
    }

    /**
     * Returns the layout ID
     *
     * @return int layoutID
     */
    public int getLayoutID() {
        return R.layout.fragment_statistics;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        getActivity().setTitle(getResources().getString(R.string.drawer_statistics));

        DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
        daoHelper = new DAOHelper(dbSession, getActivity());

        // Get the message from the intent
        if (filter == null) {
            Intent intent = getActivity().getIntent();
            LogFilter filter = intent.getParcelableExtra(LogFilter.LOG_FILTER_INTENT_KEY);
            if (filter == null) {
                clearFilter();
            } else {
                this.filter = filter;
            }
        }

        selectedCompareData = getString(R.string.stats_attacks_protocol);

        rootView = inflater.inflate(getLayoutID(), container, false);
        configureRootView(rootView);

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.statistics_actions, menu);

        optionsMenu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.statistics_action_export) {
            openBarSelectionMenuOnView(rootView);

            return true;
        } else if (item.getItemId() == R.id.statistics_action_filter) {
            openFilterMenuOnView(rootView);

            return true;
        }

        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        ViewGroup container = (ViewGroup) getView();
        assert container != null;
        container.removeAllViewsInLayout();
        rootView = inflater.inflate(getLayoutID(), container, false);
        container.addView(rootView);

        configureRootView(rootView);
    }

    /**
     * Returns the base context.
     *
     * @return Context the base context
     */
    private Context getBaseContext() {
        return getActivity().getBaseContext();
    }

    /**
     * Returns the application context.
     *
     * @return Context the application context
     */
    private Context getApplicationContext() {
        return getActivity().getApplicationContext();
    }

    /**
     * Configures the given rootview.
     * Sets the Spinner, the list and all requiered buttons.
     * It also actualises the current plot type.
     *
     * @param rootView View
     */
    public void configureRootView(View rootView) {
        LinearLayout plotLayout = rootView.findViewById(R.id.plot_layout);
        plotLayout.removeAllViews();
        plotLayout.setWillNotDraw(false);

        ProgressBar spinner = rootView.findViewById(R.id.progressBar1);
        if (spinner != null) {
            this.spinner = spinner;
            this.spinner.setVisibility(View.GONE);
        } else {
            RelativeLayout parent = (RelativeLayout) this.spinner.getParent();
            parent.removeView(this.spinner);
            RelativeLayout newParent = rootView.findViewById(R.id.plot_parent_layout);
            if (newParent != null) {
                newParent.addView(this.spinner);
            }
        }

        legendListView = rootView.findViewById(R.id.legend_list_view);
        legendListView.setOnItemClickListener((adapterView, view, i, l) -> userTappedOnLegendItem(i));
        rootView.setWillNotDraw(false);

//        ImageButton visualButton = rootView.findViewById(R.id.plot_data_button);
//        visualButton.setOnClickListener(StatisticsFragment.this::openBarSelectionMenuOnView);
//
//        ImageButton filterButton = this.getFilterButton();
//        filterButton.setOnClickListener(StatisticsFragment.this::openFilterMenuOnView);

        actualiseCurrentPlot();

        if (currentPlotView instanceof BarGraph) {
            setTitle("" + getCurrentSelectedProtocol() + ": " + selectedCompareData);
        } else {
            setTitle(selectedCompareData);
        }
    }

    /**
     * Sets the title over the plot view.
     *
     * @param title String
     */
    public void setTitle(String title) {
        TextView titleView = rootView.findViewById(R.id.title_text_view);
        if (title != null && titleView != null) {
            titleView.setText(title);
            titleView.invalidate();
        }
    }

    /**
     * Returns the title over the plot view.
     *
     * @return String title
     */
    public String getTitle() {
        TextView titleView = rootView.findViewById(R.id.title_text_view);
        if (titleView != null) {
            return "" + titleView.getText();
        }
        return "";
    }

    @Override
    public void onStart() {
        super.onStart();
        actualiseCurrentPlot();
        currentPlotView.invalidate();

        if (currentPlotView instanceof BarGraph) {
            setTitle("" + getCurrentSelectedProtocol() + ": " + selectedCompareData);
        } else {
            setTitle(selectedCompareData);
        }
    }

    /**
     * Sets the current chart to the given type and actualises it.
     *
     * @param type {@link StatisticsFragment.ChartType ChartType}
     */
    public void setChartType(ChartType type) {
        boolean shouldChange = true;
        clearFilter();
        if (currentPlotView != null) {
            if (type == ChartType.PIE_CHART) {
                shouldChange = !(currentPlotView instanceof PieGraph);
                // SET FILTER BUTTON HIDDEN
//                ImageButton filterButton = getFilterButton();
//                if (filterButton != null) filterButton.setVisibility(View.GONE);
            } else {
                if (pieGraph != null)
                    pieGraph.setVisibility(View.GONE);
                // SHOW FILTER BUTTON
//                ImageButton filterButton = getFilterButton();
//                if (filterButton != null) filterButton.setVisibility(View.VISIBLE);
            }
            if (type == ChartType.LINE_CHART) {
                shouldChange = !(currentPlotView instanceof LineGraph);
            } else {
                if (lineGraph != null)
                    lineGraph.setVisibility(View.GONE);
            }
            if (type == ChartType.BAR_CHART) {
                shouldChange = !(currentPlotView instanceof BarGraph);

            } else {
                if (barGraph != null)
                    barGraph.setVisibility(View.GONE);
            }
        }
        if (shouldChange) {
            currentPlotView = getPlotViewForType(type);
            currentPlotView.setVisibility(View.VISIBLE);
        }
        actualiseCurrentPlot();
    }

    /**
     * Returns the plot view for a given type.
     *
     * @param type {@link StatisticsFragment.ChartType ChartType}
     */
    public View getPlotViewForType(ChartType type) {
        switch (type) {
            case PIE_CHART:
                return getPieGraphView();
            case LINE_CHART:
                return getLineGraphView();
            default:
                return getBarGraphView();
        }
    }

    /**
     * Actualises the list view. Therefore it requires the "currentData".
     */
    public void actualiseLegendList() {
        StatisticListAdapter adapter = new StatisticListAdapter(getApplicationContext(), currentData);
        if (currentPlotView instanceof LineGraph) {
            adapter.setValueFormatter(item -> String.format("%.02f", item.getValue2()) + " %" + " " + "(" + (item.getValue1().intValue()) + ")");
        } else {
            adapter.setValueFormatter(item -> {
                int v = item.getValue2().intValue();
                return "" + v;
            });
        }
        legendListView.setAdapter(adapter);

        TextView tableHeaderTitleView = rootView.findViewById(R.id.table_header_title_textview);
        TextView tableHeaderValueView = rootView.findViewById(R.id.table_header_value_textview);
        if (currentPlotView instanceof LineGraph) {
            tableHeaderTitleView.setText(R.string.essid);
            tableHeaderValueView.setText(R.string.stats_per_cent_all);
        }
        if (currentPlotView instanceof PieGraph) {
            tableHeaderTitleView.setText(R.string.rec_protocol);
            tableHeaderValueView.setText(R.string.stats_attacks_count);
        }
        if (currentPlotView instanceof BarGraph) {
            tableHeaderValueView.setText(R.string.stats_attacks_count);
            if (selectedCompareData.equals(getString(R.string.stats_attacks_bssid))) {
                tableHeaderTitleView.setText(R.string.bssid);
            } else {
                tableHeaderTitleView.setText(R.string.essid);
            }
        }
        if (currentData == null || currentData.isEmpty()) {
            tableHeaderTitleView.setText("");
            tableHeaderValueView.setText("");
        }
    }

    /*
     * MENU
     * */

    /**
     * Opens the Bar Option Menu above the given anchor view.
     *
     * @param anchorView View
     */
    private void openBarSelectionMenuOnView(View anchorView) {
        SimplePopupTable visualiseMenu = new SimplePopupTable(getActivity(), ob -> {
            if (ob instanceof AbstractPopupItem) {
                AbstractPopupItem item = (AbstractPopupItem) ob;
                userSelectMenuItem(item);
            }
        });
        visualiseMenu.setTitle(getString(R.string.stats_visualize));
        int id = 0;
        for (String title : getMenuTitles()) {
            SimplePopupItem item = new SimplePopupItem(getActivity());
            item.setTitle(title);
            item.setItemId(id);
            item.setSelected(false);
            visualiseMenu.addItem(item);
            id++;
        }
        visualiseMenu.showOnView(anchorView);
    }

    /**
     * Will be called when the users selected an menu item (visualise menu / plot menu).
     * If the user selected "Protocols" this method sets the current plot type to piegraph.
     * Otherwise it will open a new dialog to select the comparison type.
     */
    private void userSelectMenuItem(AbstractPopupItem item) {
        // OPEN A DIALOG TO SPECIFY THE VISUALISE DATA
        if (item.getTitle().equals(getString(R.string.stats_protocols))) {
            ChartType chartType = ChartType.PIE_CHART;
            setChartType(chartType);
            setTitle(getString(R.string.stats_attacks_protocol));
        }
        if (item.getTitle().equals(getString(R.string.stats_networks))) {
            openNetworkDataDialog();
        }
        if (item.getTitle().equals(getString(R.string.stats_attacks))) {
            openAttackDataDialog();
        }
    }

    /**
     * Returns the menu titles (visualise menu / plot menu)
     */
    private ArrayList<String> getMenuTitles() {
        ArrayList<String> titles = new ArrayList<>();
        titles.add(getString(R.string.stats_protocols));
        titles.add(getString(R.string.stats_networks));
        titles.add(getString(R.string.stats_attacks));
        return titles;
    }

    /*
     * PLOT DATA DIALOGS
     * */
//     private void openProtocolDataDialog(){
//        ArrayList<String> titles = getDialogProtocolDataTitle();
//        ChecklistDialog newFragment = new ChecklistDialog(DIALOG_PROTOCOLS_TITLE, titles, selectedData(titles), false , this);
//        newFragment.show(getActivity().getFragmentManager(), DIALOG_PROTOCOLS_TITLE);
//    }

    /**
     * Opens the network comparison dialog
     */
    private void openNetworkDataDialog() {
        ArrayList<String> titles = getDialogNetworkDataTitle();
        ChecklistDialog newFragment = new ChecklistDialog(getString(R.string.stats_select_network_data), titles, selectedData(titles), false, this);
        newFragment.show(getActivity().getFragmentManager(), getString(R.string.stats_select_network_data));
    }

    /**
     * Opens the attack comparison dialog
     */
    private void openAttackDataDialog() {
        ArrayList<String> titles = getDialogAttackDataTitle();
        ChecklistDialog newFragment = new ChecklistDialog(getString(R.string.stats_select_attack_data), titles, selectedData(titles), false, this);
        newFragment.show(getActivity().getFragmentManager(), getString(R.string.stats_select_attack_data));
    }

    /*
     *
     * DIALOG ACTION METHODS
     *
     * */

    /**
     * Will be called if the user selects the positive button on an checklist dialog.
     *
     * @param dialog {@link ChecklistDialog ChecklistDialog}
     */
    public void onDialogPositiveClick(ChecklistDialog dialog) {
        String title = dialog.getTitle();
        ArrayList<String> titles = dialog.getSelectedItemTitles();

        if (title.equals(getString(R.string.stats_protocols))) {
            //titles = titles.size() == 0 ? protocolTitles() : titles;
            filter.setProtocols(titles);
            actualiseCurrentPlot();
            return;
        }
        if (title.equals(getString(R.string.stats_select_protocol))) {
            if (titles.size() == 0) {
                titles = new ArrayList<>();
                titles.add(protocolTitles().get(0));
            }
            filter.setProtocols(titles);

            actualiseCurrentPlot();
            String fragTitle = "" + getCurrentSelectedProtocol() + ": " + selectedCompareData;
            setTitle(fragTitle);

            return;
        }
        if (title.equals(getString(R.string.essid))) {
            filter.setESSIDs(titles);
            actualiseCurrentPlot();

            return;
        }
        if (title.equals(getString(R.string.bssid))) {
            filter.setBSSIDs(titles);
            actualiseCurrentPlot();

            return;
        }

        if (titles.size() != 0) {
            String data = titles.get(0);
            setTitle(data);

            actualiseFilterButton();

            if (data.equals(getString(R.string.stats_attacks_time)) || data.equals(getString(R.string.stats_attacks_date))) {
                ChartType chartType = ChartType.LINE_CHART;
                selectedCompareData = data;
                setChartType(chartType);
                return;
            }
            if (data.equals(getString(R.string.stats_attacks_bssid)) || data.equals(getString(R.string.stats_attacks_essid))) {
                ChartType chartType = ChartType.BAR_CHART;
                selectedCompareData = data;
                setChartType(chartType);

                String fragTitle = "" + getCurrentSelectedProtocol() + ": " + selectedCompareData;
                setTitle(fragTitle);
            }
        }

    }

    /**
     * Will be called if the user selects the negative button on an checklist dialog.
     *
     * @param dialog {@link ChecklistDialog ChecklistDialog}
     */
    public void onDialogNegativeClick(ChecklistDialog dialog) {

    }
    /*
     *
     * DIALOG DATA
     *
     * */


//     private ArrayList<String> getDialogProtocolDataTitle(){
//        ArrayList<String> data = new ArrayList<String>();
//        data.add(getString(R.string.stats_attacks_protocol));
//        data.add(COMPARE_TITLE_UsesPerProtocol);
//        return data;
//    }

    /**
     * Returns the Attacks comparison titles.
     *
     * @return ArrayList<String> the titles
     */
    private ArrayList<String> getDialogAttackDataTitle() {
        ArrayList<String> data = new ArrayList<>();
        data.add(getString(R.string.stats_attacks_date));
        data.add(getString(R.string.stats_attacks_time));
        return data;
    }

    /**
     * Returns the network comparison titles.
     *
     * @return ArrayList<String> the titles
     */
    private ArrayList<String> getDialogNetworkDataTitle() {
        ArrayList<String> data = new ArrayList<>();
        data.add(getString(R.string.stats_attacks_essid));
        data.add(getString(R.string.stats_attacks_bssid));
        return data;
    }

    /**
     * DEFAULT
     * Returns an boolean array with a default selection. Just the first object is true.
     *
     * @return boolean[] selected array
     */
    private boolean[] selectedData(ArrayList<String> data) {
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

    /**
     * Paints the filter button if the current filter object is set.
     */
    private void actualiseFilterButton() {
        if (optionsMenu != null) {
            MenuItem filterItem = optionsMenu.findItem(R.id.statistics_action_filter);


            if (filterItem.getIcon() != null) {

                if (filter.isSet()) {

                    Drawable filterIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_filter);
                    filterIcon.setTint(getResources().getColor(R.color.colorPrimaryVariant));
                    filterItem.setIcon(filterIcon);

                } else {
                    Drawable filterIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_filter);

                    filterIcon.setTintList(null);
                    filterItem.setIcon(R.drawable.ic_filter);

                }

                onPrepareOptionsMenu(optionsMenu);
            }
        }
    }

    /**
     * Opens the filter menu above an given anchor view.
     *
     * @param anchor View
     */
    private void openFilterMenuOnView(View anchor) {
        SimplePopupTable filterMenu = new SimplePopupTable(getActivity(), ob -> {
            if (ob instanceof AbstractPopupItem) {
                AbstractPopupItem item = (AbstractPopupItem) ob;
                onFilterMenuItemSelected(item);
            }
        });

        filterMenu.setTitle(getString(R.string.rec_filter_by));
        for (String title : filterMenuTitles()) {
            AbstractPopupItem item;
            if (title.equals(getString(R.string.rec_latest))) continue;
            if (title.equals(getString(R.string.rec_earliest))) {

                item = new SplitPopupItem(getActivity());
                item.setValue(SplitPopupItem.RIGHT_TITLE, getString(R.string.rec_latest));
                item.setValue(SplitPopupItem.LEFT_TITLE, getString(R.string.rec_earliest));
                if (filter.hasBelowTimestamp()) {
                    item.setValue(SplitPopupItem.RIGHT_SUBTITLE, getDateAsString(filter.belowTimestamp));
                }
                if (filter.hasAboveTimestamp()) {
                    item.setValue(SplitPopupItem.LEFT_SUBTITLE, getDateAsString(filter.aboveTimestamp));
                }
            } else {
                item = new SimplePopupItem(getActivity());
                item.setTitle(title);
                ((SimplePopupItem) item).setSelected(isFilterSetForTitle(title));
            }

            filterMenu.addItem(item);
        }
        filterMenu.showOnView(anchor);
    }

    /**
     * Will be called if the user selected an filter item.
     *
     * @param item {@link AbstractPopupItem AbstractPopupItem}
     */
    private void onFilterMenuItemSelected(AbstractPopupItem item) {
        if (item instanceof SplitPopupItem) {
            SplitPopupItem sItem = (SplitPopupItem) item;
            boolean wasBelowTimePicker = sItem.wasRightTouch;
            DateTimePickerDialog.showDateTimePicker(getContext(), !wasBelowTimePicker, this);
            return;
        }
        String title = item.getTitle();
        if (title.equals(getString(R.string.essid))) {
            openESSIDFilterDialog();
        }
        if (title.equals(getString(R.string.bssid))) {
            openBSSIDFilterDialog();
        }
        if (title.equals(getString(R.string.rec_protocol))) {
            openFilterDialogSelectProtocol();
        }
        if (title.equals(getString(R.string.stats_protocols))) {
            openProtocolsFilterDialog();
        }
        if (title.equals(getString(R.string.rec_reset_filter))) {
            clearFilter();
            actualiseCurrentPlot();
        }
    }

    /**
     * Return the menu titles of the filter menu.
     *
     * @return ArrayList<String> filter menu title
     */
    private ArrayList<String> filterMenuTitles() {
        ArrayList<String> titles = new ArrayList<>();
        if (currentPlotView instanceof LineGraph) {
            titles.add(getString(R.string.essid));
            titles.add(getString(R.string.stats_protocols));
            titles.add(getString(R.string.rec_earliest));
            if (filter.hasESSIDs() || filter.hasATimestamp() || (filter.getProtocols() != null && filter.hasProtocols() && filter.getProtocols().size() != protocolTitles().size())) {
                titles.add(getString(R.string.rec_reset_filter));
            }
        } else {
            titles.add(getString(R.string.rec_protocol));
            String protocol = getCurrentSelectedProtocol();
            if (protocol.length() > 0) {
                if (selectedCompareData.equals(getString(R.string.stats_attacks_bssid))) {
                    titles.add(getString(R.string.bssid));
                } else {
                    // DEFAULT
                    titles.add(getString(R.string.essid));
                }
            }
            titles.add(getString(R.string.rec_earliest));
            if (filter.hasATimestamp() || filter.hasESSIDs() || filter.hasBSSIDs()
                    || (currentPlotView instanceof LineGraph && filter.hasProtocols())) {
                titles.add(getString(R.string.rec_reset_filter));
            }
        }
        return titles;
    }

    /**
     * Opens a multiple protocol checklist dialog
     */
    private void openProtocolsFilterDialog() {
        ChecklistDialog newFragment = new ChecklistDialog(getString(R.string.stats_protocols),
                protocolTitles(),
                selectedProtocols(),
                true,
                this);
        newFragment.show(getActivity().getFragmentManager(), getString(R.string.stats_protocols));
    }

    /**
     * Opens a single protocol checklist dialog
     */
    private void openFilterDialogSelectProtocol() {
        ArrayList<String> titles = protocolTitles();
        boolean[] selected = new boolean[titles.size()];
        int i = 0;
        for (String title : titles) {
            selected[i] = title.equals(getCurrentSelectedProtocol());
            i++;
        }
        ChecklistDialog newFragment = new ChecklistDialog(getString(R.string.stats_select_protocol), titles, selected, false, this);
        newFragment.show(getActivity().getFragmentManager(), getString(R.string.stats_select_protocol));
    }

    /**
     * Opens a multiple essid checklist dialog
     */
    private void openESSIDFilterDialog() {
        ChecklistDialog newFragment = new ChecklistDialog(getString(R.string.essid), essids(), selectedESSIDs(), true, this);
        newFragment.show(getActivity().getFragmentManager(), getString(R.string.essid));
    }

    /**
     * Opens a multiple bssid checlist dialog.
     */
    private void openBSSIDFilterDialog() {
        ChecklistDialog newFragment = new ChecklistDialog(getString(R.string.bssid), bssids(), selectedBSSIDs(), true, this);
        newFragment.show(getActivity().getFragmentManager(), getString(R.string.bssid));
    }

    /**
     * Returns all essids
     * If the current plot is a bar graph, it just return all possible essids for the selected protocol
     *
     * @return ArrayList<String> essids
     */
    public ArrayList<String> essids() {
        ArrayList<String> essids;
        if (currentPlotView instanceof BarGraph) {
            essids = daoHelper.getNetworkRecordDAO().getUniqueESSIDRecordsForProtocol(getCurrentSelectedProtocol());
        } else {
            essids = daoHelper.getNetworkRecordDAO().getUniqueESSIDRecords();
        }
        return essids;
    }

    /**
     * Returns a boolean array. The position in the array will be true, if the essid is selected in the filter.
     *
     * @return boolean[] selected essids
     */
    public boolean[] selectedESSIDs() {
        ArrayList<String> essids = essids();
        boolean[] selected = new boolean[essids.size()];

        int i = 0;
        for (String essid : essids) {
            selected[i] = (filter.getESSIDs().contains(essid));
            i++;
        }
        return selected;
    }

    /**
     * Returns all bssids
     * If the current plot is a bar graph, it just return all possible bssids for the selected protocol
     *
     * @return ArrayList<String> bssids
     */
    public ArrayList<String> bssids() {
        ArrayList<String> bssids;
        if (currentPlotView instanceof BarGraph) {
            bssids = daoHelper.getNetworkRecordDAO().getUniqueBSSIDRecordsForProtocol(getCurrentSelectedProtocol());
        } else {
            bssids = daoHelper.getNetworkRecordDAO().getUniqueBSSIDRecords();
        }
        return bssids;
    }

    /**
     * Returns a boolean array. The position in the array will be true, if the bssid is selected in the filter.
     *
     * @return boolean[] selected bssids
     */
    public boolean[] selectedBSSIDs() {
        ArrayList<String> bssids = bssids();

        boolean[] selected = new boolean[bssids.size()];

        int i = 0;
        for (String bssid : bssids) {
            selected[i] = (filter.getBSSIDs().contains(bssid));
            i++;
        }
        return selected;
    }

    /*
     *
     *  PLOT TYPES
     *
     * **/

    /**
     * Returns the current pie graph.
     *
     * @return PieGraph current piegraph
     */
    public PieGraph getPieGraphView() {
        if (pieGraph == null) {
            pieGraph = new PieGraph(getApplicationContext());
            LinearLayout plotLayout = rootView.findViewById(R.id.plot_layout);
            plotLayout.addView(pieGraph);
            pieGraph.setOnSliceClickedListener(StatisticsFragment.this::onSliceClick);
        }
        return pieGraph;
    }

    /**
     * Returns the current  {@link LineGraph Linegraph} .
     *
     * @return LineGraph current line graph
     */
    public LineGraph getLineGraphView() {
        if (lineGraph == null) {
            lineGraph = new LineGraph(getActivity());
            LinearLayout plotLayout = rootView.findViewById(R.id.plot_layout);
            plotLayout.addView(lineGraph);
            lineGraph.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
        return lineGraph;
    }

    /**
     * Returns the current  {@link BarGraph BarGraph} .
     *
     * @return BarGraph the current bar graph
     */
    public BarGraph getBarGraphView() {
        if (barGraph == null) {
            barGraph = new BarGraph(getActivity());
            LinearLayout plotLayout = rootView.findViewById(R.id.plot_layout);
            barGraph.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            plotLayout.addView(barGraph);
            barGraph.setShowBarText(false);
            barGraph.setPopupImageID(R.drawable.popup_black);
            barGraph.setOnBarClickedListener(StatisticsFragment.this::onBarClick);
        }
        return barGraph;
    }

    /*
     *  FEED PLOTS WITH DATA
     * */

    /**
     * Sets the data for the given PieGraph
     *
     * @param piegraph {@link PieGraph PieGraph}
     */
    public void setPieGraphData(PieGraph piegraph) {
        currentData = getPieData();
        if (currentData == null) {
            currentData = new ArrayList<>();
        }

        piegraph.removeSlices();

        for (PlotComparisonItem item : currentData) {
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
     *
     * @param linegraph {@link LineGraph Linegraph}
     */
    public void setLineGraphData(LineGraph linegraph) {
        currentData = getLineData();
        if (currentData == null) {
            currentData = new ArrayList<>();
        }

        linegraph.removeAllLines();
        double rangeMax_Y = 0;
        double rangeMin_Y = 0;

        double rangeMax_X = 0;
        double rangeMin_X = 0;

        int count = 0;
        for (PlotComparisonItem lineItem : currentData) {
            ArrayList<PlotComparisonItem> data = lineItem.getChildItems();
            //int index = 0;
            Line l = new Line();
            int lineColor = lineItem.getColor();
            l.setColor(lineColor);

            for (PlotComparisonItem pointItem : data) {
                LinePoint p = new LinePoint();
                p.setX(pointItem.getValue1());
                Double value2 = pointItem.getValue2();
                p.setY(value2);
                p.setColor(lineColor);
                l.addPoint(p);
                rangeMax_Y = Math.max(pointItem.getValue2(), rangeMax_Y);
                rangeMax_X = Math.max(pointItem.getValue1(), rangeMax_X);

                if (count != 0) {
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

        boolean shouldUseDate = selectedCompareData.equals(getString(R.string.stats_attacks_date));
        if (shouldUseDate) {
            linegraph.resetXLimits();

            if (filter.hasBelowTimestamp()) {
                rangeMax_X = Math.max(filter.belowTimestamp, rangeMax_X);
            }
            if (filter.hasAboveTimestamp()) {
                rangeMin_X = Math.min(filter.aboveTimestamp, rangeMin_X);
            }

            if (rangeMax_X == rangeMin_X) {
                double aDay = 60 * 60 * 24 * 1000;
                rangeMax_X += aDay;
                rangeMin_X -= aDay;
            }

            double stepRange = (rangeMax_X - rangeMin_X) / (60 * 60 * 24 * 1000);
            linegraph.setxAxisStep(Math.max(1, (float) Math.min(stepRange, 4)));

            linegraph.setRangeX(rangeMin_X, rangeMax_X);

            linegraph.setConverter(new LineGraph.AxisDataConverter() {
                @Override
                public String convertDataForX_Position(double x) {
                    return getDateAsDayString((long) x);
                }

                @Override
                public String convertDataForY_Position(double y) {
                    return "" + (long) y;
                }
            });
        } else {
            linegraph.setxAxisStep(12.f);
            linegraph.setRangeX(0, 24);
            linegraph.setConverter(null);
        }

        int maxY = (int) (rangeMax_Y - rangeMin_Y);
        linegraph.setYAxisStep(Math.min(maxY, 5));
        int yStep = (int) linegraph.getyAxisStep();
        if ((maxY % yStep) != 0) {
            maxY = maxY + (yStep - (maxY % yStep));
        }
        rangeMax_Y = rangeMin_Y + maxY;
        linegraph.setRangeY(rangeMin_Y, rangeMax_Y);
        linegraph.setLineToFill(0);
    }

    /**
     * Set the graph data to the given graph
     *
     * @param bargraph {@link BarGraph BarGraph}
     */
    public void setBarGraphData(BarGraph bargraph) {
        currentData = getBarData();
        if (currentData == null) {
            currentData = new ArrayList<>();
        }

        ArrayList<Bar> bars = new ArrayList<>();

        for (PlotComparisonItem item : currentData) {
            Bar d = new Bar();
            d.setColor(item.getColor());
            long value2 = item.getValue2().longValue();
            d.setName("");
            d.setValue((float) value2);
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
     *
     * @return records {@link java.util.ArrayList}, {@link RecordAll Record}
     */
    public ArrayList<RecordAll> getFetchedRecords() {
        if (filter == null) clearFilter();
        return daoHelper.getAttackRecordDAO().getRecordsForFilter(filter);
    }

    /**
     * Actualises the current plot in a background thread.
     */
    public void actualiseCurrentPlot() {
        spinner.setVisibility(View.VISIBLE);

        actualiseFilterButton();

        LinearLayout plotLayout = rootView.findViewById(R.id.plot_layout);

        if (barGraph != null)
            barGraph.setVisibility(View.GONE);
        if (lineGraph != null)
            lineGraph.setVisibility(View.GONE);
        if (pieGraph != null)
            pieGraph.setVisibility(View.GONE);

        View plot = currentPlotView;
        if (plot == null) {
            currentPlotView = getPieGraphView();
            plot = currentPlotView;
        }
        if (plot.getParent() != null && !plot.getParent().equals(plotLayout)) {
            LinearLayout linLayout = (LinearLayout) plot.getParent();
            linLayout.removeView(plot);
            plot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            plotLayout.addView(plot);
        }

        currentPlotView = plot;

        final LinearLayout thePlotlayout = plotLayout;

        if (loader != null && loader.isAlive()) loader.interrupt();

        loader = null;

        loader = new Thread(new Runnable() {
            @Override
            public void run() {
                loadDataInBackground();
                actualiseUI();
            }

            private void loadDataInBackground() {
                View plot = currentPlotView;
                if (plot instanceof PieGraph) {
                    PieGraph pie = (PieGraph) plot;
                    setPieGraphData(pie);
                }
                if (plot instanceof BarGraph) {
                    BarGraph bar = (BarGraph) plot;
                    setBarGraphData(bar);
                }
                if (plot instanceof LineGraph) {
                    LineGraph line = (LineGraph) plot;
                    setLineGraphData(line);
                }
            }

            private void actualiseUI() {

                Activity actv = getActivity();

                if (actv != null) {
                    actv.runOnUiThread(() -> {
                        // SET VISIBILITY
                        View plot1 = currentPlotView;

                        if (plot1 instanceof PieGraph) {
                            // HIDE FILTER BUTTON
//                            ImageButton filterButton = StatisticsFragment.this.getFilterButton();
//                            if (filterButton != null) filterButton.setVisibility(View.GONE);
                        } else {
                            if (pieGraph != null) {
                                pieGraph.setVisibility(View.GONE);
                                if (pieGraph.getParent() != null) {
                                    thePlotlayout.removeView(pieGraph);
                                }
                            }
                            // SHOW FILTER BUTTON
//                            ImageButton filterButton = StatisticsFragment.this.getFilterButton();
//                            if (filterButton != null) filterButton.setVisibility(View.VISIBLE);
                        }
                        if (!(plot1 instanceof BarGraph)) {
                            if (barGraph != null) {
                                barGraph.setVisibility(View.GONE);
                                if (barGraph.getParent() != null) {
                                    thePlotlayout.removeView(barGraph);
                                }
                            }
                        }
                        if (!(plot1 instanceof LineGraph)) {
                            if (lineGraph != null) {
                                lineGraph.setVisibility(View.GONE);
                                if (lineGraph.getParent() != null) {
                                    thePlotlayout.removeView(lineGraph);
                                }
                            }
                        }

                        plot1.setVisibility(View.VISIBLE);

                        if (plot1.getParent() == null) {
                            thePlotlayout.addView(plot1);
                        }
                        actualiseLegendList();
                        currentPlotView.bringToFront();
                        currentPlotView.invalidate();

                        spinner.setVisibility(View.GONE);

                        showEmptyDataNotification();
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
        if (noDataNotificationSnackbar == null) {
            noDataNotificationSnackbar = Snackbar.make(rootView, R.string.no_data_notification, Snackbar.LENGTH_LONG);
        }
        if (filter.isSet()) {
            noDataNotificationSnackbar.setText(R.string.no_data_notification);
        } else {
            noDataNotificationSnackbar.setText(R.string.no_data_notification_no_filter);
        }
        if (currentData == null || currentData.isEmpty()) {
            noDataNotificationSnackbar.show();
        }
    }


    /**
     * Calculates and returns the data for the piegraph
     *
     * @return ArrayList<PlotComparisonItem> data
     */
    public ArrayList<PlotComparisonItem> getPieData() {
        // DEFAULT
        return attacksPerProtocols();
    }

    /**
     * Calculates and returns the data for the bargraph.
     *
     * @return ArrayList<PlotComparisonItem> data
     */
    public ArrayList<PlotComparisonItem> getBarData() {
        String protocol = getCurrentSelectedProtocol();

        if (protocol.length() > 0) {
            if (selectedCompareData.equals(getString(R.string.stats_attacks_essid))) {
                return attacksPerESSID(protocol);
            }
            // DEFAULT
            return attacksPerBSSID(protocol);
        }
        // Nothing available
        return new ArrayList<>();
    }

    /**
     * Calculates and returns the data for the linegraph
     *
     * @return ArrayList<PlotComparisonItem> data
     */
    public ArrayList<PlotComparisonItem> getLineData() {
        return attacksPerTime();
    }

    /*
     *  DATA SOURCE
     * */

    /*PROTOCOLS OVERVIEW*/

    /**
     * Returns the attacks per protocols comparison result.
     * The returned data is resized to the specified limit.
     *
     * @return ArrayList<PlotComparisonItem>
     */
    public synchronized ArrayList<PlotComparisonItem> attacksPerProtocols() {
        ArrayList<PlotComparisonItem> plotItems = new ArrayList<>();
        int index = 0;
        for (String title : getSelectedProtocolTitles()) {
            int attacksCount = daoHelper.getAttackRecordDAO().getAttackPerProtocolCount(title);
            if (attacksCount == 0) continue;
            PlotComparisonItem item = new PlotComparisonItem(title, getColor(index), 0., (double) attacksCount);
            plotItems.add(item);
            index++;
        }
        plotItems.sort((s1, s2) -> s2.getValue2().compareTo(s1.getValue2()));
        return resizeData(plotItems);
    }


    /*
     *  LINE PLOT DATA
     */

    /**
     * Returns the line graph data responding to the selectedCompareData key.
     * The returned data is resized to the specified limit.
     *
     * @return plotItems {@link PlotComparisonItem PlotComparisonItems}
     */
    public ArrayList<PlotComparisonItem> attacksPerTime() {
        HashMap<String, HashMap<Long, ArrayList<RecordAll>>> lineMap = new HashMap<>();

        boolean shouldUseDate = selectedCompareData.equals(getString(R.string.stats_attacks_date));

        ArrayList<RecordAll> records = getFetchedRecords();
        for (RecordAll record : records) {
            long timestamp = record.getTimestamp();
            long time;
            if (shouldUseDate) {
                time = getDateFromMilliseconds(timestamp);
            } else {
                time = getDayHourFromDate(timestamp);
            }

            // GET CORRECT MAP
            HashMap<Long, ArrayList<RecordAll>> recordMap;
            String groupKey = record.getSsid();
            if (lineMap.containsKey(groupKey)) {
                recordMap = lineMap.get(record.getSsid());
            } else {
                recordMap = new HashMap<>();
                lineMap.put(groupKey, recordMap);
            }

            // GET LIST OF RECORDS
            ArrayList<RecordAll> list;
            if (recordMap.containsKey(time)) {
                list = recordMap.get(time);
            } else {
                list = new ArrayList<>();
                recordMap.put(time, list);
            }
            list.add(record);
        }

        ArrayList<PlotComparisonItem> plotItems = new ArrayList<>();

        int index = 0;
        for (String groupKey : lineMap.keySet()) {
            HashMap<Long, ArrayList<RecordAll>> recordMap = lineMap.get(groupKey);
            ArrayList<PlotComparisonItem> singleLineItems = new ArrayList<>();

            int numbOfAttacks = 0;
            for (long time : recordMap.keySet()) {
                ArrayList<RecordAll> list = recordMap.get(time);
                if (list.size() == 0) continue;
                PlotComparisonItem item = new PlotComparisonItem(getHourAsTimeString(time), 0, (double) time, (double) list.size());
                singleLineItems.add(item);
                numbOfAttacks += list.size();
            }

            singleLineItems.sort(Comparator.comparing(PlotComparisonItem::getValue1));

            double itemValue = (((double) numbOfAttacks / (double) records.size()) * 100.);
            PlotComparisonItem item = new PlotComparisonItem(groupKey, getColor(index), (double) numbOfAttacks, itemValue);
            item.setChildItems(singleLineItems);
            plotItems.add(item);
            index++;
        }
        plotItems.sort((s1, s2) -> s2.getValue2().compareTo(s1.getValue2()));
        return plotItems;
    }

    /*
     *  BAR PLOT DATA
     */

    /**
     * Returns plotitems for the comparison "attacks per bssid"
     * The returned data is resized to the specified limit.
     *
     * @return ArrayList of  {@link PlotComparisonItem PlotComparisonItems}
     */
    public ArrayList<PlotComparisonItem> attacksPerBSSID(String protocol) {
        LogFilter filter = new LogFilter();
        ArrayList<String> protocollist = new ArrayList<>();
        filter.setAboveTimestamp(this.filter.getAboveTimestamp());
        filter.setBelowTimestamp(this.filter.getBelowTimestamp());
        filter.setBSSIDs(this.filter.getBSSIDs());
        protocollist.add(protocol);
        filter.setProtocols(protocollist);

        ArrayList<PlotComparisonItem> plotItems = daoHelper.getNetworkRecordDAO().attacksPerBSSID(filter);

        plotItems.sort((s1, s2) -> s2.getValue2().compareTo(s1.getValue2()));
        return resizeData(plotItems);
    }

    /**
     * Returns plotitems for the comparison "attacks per essid"
     *
     * @return ArrayList of  {@link PlotComparisonItem PlotComparisonItems}
     */
    public ArrayList<PlotComparisonItem> attacksPerESSID(String protocol) {
        LogFilter filter = new LogFilter();
        filter.setAboveTimestamp(this.filter.getAboveTimestamp());
        filter.setBelowTimestamp(this.filter.getBelowTimestamp());
        filter.setESSIDs(this.filter.getESSIDs());
        ArrayList<String> protocollist = new ArrayList<>();
        protocollist.add(protocol);
        filter.setProtocols(protocollist);

        ArrayList<PlotComparisonItem> plotItems = daoHelper.getNetworkRecordDAO().attacksPerESSID(filter); //new ArrayList<PlotComparisonItem>();

        plotItems.sort((s1, s2) -> s2.getValue2().compareTo(s1.getValue2()));

        return resizeData(plotItems);
    }

    /**
     * This will normalize the given list of plot items to the specified length of MAX_NUMBER_OF_CHART_OBJECTS.
     * Creates an "others" group, containing all objects after the (MAX_NUMBER_OF_CHART_OBJECTS - 1)th object from the given list.
     * If the given list does contain MAX_NUMBER_OF_CHART_OBJECTS+1 or more objects, nothing will change.
     *
     * @param plotItems to normalize  {@link PlotComparisonItem PlotComparisonItems}
     * @return the normalized ArrayList of  {@link PlotComparisonItem PlotComparisonItems}
     */
    private ArrayList<PlotComparisonItem> resizeData(ArrayList<PlotComparisonItem> plotItems) {
        if (plotItems != null) {
            if (plotItems.size() > MAX_NUMBER_OF_CHART_OBJECTS && MAX_NUMBER_OF_CHART_OBJECTS > 1) {
                ArrayList<PlotComparisonItem> copy = new ArrayList<>();
                ArrayList<PlotComparisonItem> others = new ArrayList<>();
                double valueOfOthers = 0;

                for (int i = 0; i < plotItems.size(); i++) {
                    if (i < MAX_NUMBER_OF_CHART_OBJECTS - 1) {
                        PlotComparisonItem item = plotItems.get(i);
                        item.setColor(getColor(i));
                        copy.add(plotItems.get(i));
                    } else {
                        PlotComparisonItem item = plotItems.get(i);
                        others.add(item);
                        valueOfOthers += item.getValue2();
                    }
                }
                PlotComparisonItem otherItem = new PlotComparisonItem(getString(R.string.stats_other), getOtherColor(), 0., valueOfOthers);
                otherItem.setChildItems(others);
                copy.add(otherItem);

                copy.sort((s1, s2) -> s2.getValue2().compareTo(s1.getValue2()));

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
     *
     * @return String protocolName
     */
    private String getCurrentSelectedProtocol() {
        ArrayList<String> protocolTitles = getSelectedProtocolTitles();
        if (protocolTitles != null && protocolTitles.size() != 0) {
            return protocolTitles.get(0);
        }
        return protocolTitles().get(0);
    }

    /**
     * Return all Protocols
     *
     * @return ArrayList<String> protocolNames
     */
    public ArrayList<String> protocolTitles() {
        ArrayList<String> titles = new ArrayList<>(Arrays.asList(getResources().getStringArray(
                R.array.protocols)));

        titles.add("PORTSCAN");

        return titles;
    }

    /**
     * Returns a boolean array containing a bool value for each protocol. If the value is true, the filter object contains the protocol.
     * The array sequence equates to the protocolTitles() list.
     *
     * @return boolean[] selection array
     */
    public boolean[] selectedProtocols() {
        ArrayList<String> protocols = protocolTitles();
        boolean[] selected = new boolean[protocols.size()];

        int i = 0;
        for (String protocol : protocols) {
            selected[i] = (filter.protocols.contains(protocol));
            i++;
        }
        return selected;
    }

    public ArrayList<String> getSelectedProtocolTitles() {
        ArrayList<String> knownProtocols = protocolTitles();
        if (filter.hasProtocols()) {
            ArrayList<String> titles = new ArrayList<>();
            int i = 0;
            for (boolean b : selectedProtocols()) {
                if (b) {
                    String title = knownProtocols.get(i);
                    titles.add(title);
                }
                i++;
            }
            return titles;
        }
        return protocolTitles();
    }
    /*
     *
     * COLOR STUFF
     *
     * */

    /**
     * Returns the color for the other group
     *
     * @return int color
     */
    public int getOtherColor() {
        return Color.argb(255, 80, 80, 80); // grey
    }

    /**
     * Returns the color for the given index
     *
     * @return int color
     */
    public Integer getColor(int index) {
        return ColorSequenceGenerator.getColorForIndex(index);
    }

    /**
     * Returns the Plot layout.
     *
     * @return LinearLayout plot layout
     */
    public LinearLayout getPlotLayout() {
        if (rootView != null) {
            return (LinearLayout) rootView.findViewById(R.id.plot_layout);
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
     *
     * @param title of the filter menu item {@link String String}
     * @return boolean b
     */
    private boolean isFilterSetForTitle(String title) {
        if (title.equals(getString(R.string.bssid))) {
            return filter.hasBSSIDs();
        }
        if (title.equals(getString(R.string.essid))) {
            return filter.hasESSIDs();
        }
        if (title.equals(getString(R.string.stats_protocols))) {
            return (filter.getProtocols() != null && filter.hasProtocols() && filter.getProtocols().size() != protocolTitles().size());
        }
        if (title.equals(getString(R.string.rec_latest))) {
            return filter.hasBelowTimestamp();
        }
        if (title.equals(getString(R.string.rec_earliest))) {
            return filter.hasAboveTimestamp();
        }
        return false;
    }

    /**
     * Clears the current filter.
     */
    private void clearFilter() {
        if (filter == null) filter = new LogFilter();
        filter.clear();
    }

    /*
     *
     *  DATE TRANSFORMATION
     *
     */

    /**
     * Returns the current hour from a date.
     *
     * @param timeInMillis long
     * @return milliseconds in long
     */
    public long getDayHourFromDate(long timeInMillis) {

        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(timeInMillis);
        //int min     = calendar.get(Calendar.MINUTE);

        return calendar.get(Calendar.HOUR_OF_DAY);

    }

    /**
     * Returns the current date without the seconds, minutes, hours.
     *
     * @param timeInMillis long
     * @return long date with time 00:00:00
     */
    public long getDateFromMilliseconds(long timeInMillis) {
        long millisInDay = 60 * 60 * 24 * 1000;
        return (timeInMillis / millisInDay) * millisInDay;

    }

    /**
     * Returns the given hour as a formatted string.
     * Format: "HH:00"
     */
    private String getHourAsTimeString(long hour) {
        return "" + hour + ":00";
    }

    //static final DateFormat dateFormat = new SimpleDateFormat("d.M.yy");

    /**
     * Returns a date as a formatted string
     *
     * @param timeStamp date
     * @return String date format is localised
     */
    @SuppressLint("SimpleDateFormat")
    private String getDateAsDayString(long timeStamp) {
        Date netDate = (new Date(timeStamp));
        DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(getActivity());
        return dateFormat.format(netDate);
    }

    /**
     * Returns a date as a formatted string
     *
     * @param timeStamp date
     * @return String date format (H:mm  dd/MM/yyyy)
     */
    private String getDateAsString(long timeStamp) {
        DateFormat sdf = new SimpleDateFormat("H:mm  dd.MM.yyyy");
        Date netDate = (new Date(timeStamp));
        return sdf.format(netDate);
    }

    /**
     * Will be called if the users taps on a list row.
     *
     * @param index int
     */
    private void userTappedOnLegendItem(int index) {
        if (index < currentData.size()) {
            PlotComparisonItem item = currentData.get(index);
            ArrayList<String> selectedData;
            String groupingKey = null;
            selectedData = new ArrayList<>();

            if (item.getChildItems() == null) {
                selectedData.add(item.getTitle());
            } else {
                for (PlotComparisonItem other : item.getChildItems()) {
                    selectedData.add(other.getTitle());
                }
            }
            LogFilter filter = new LogFilter();
            if (currentPlotView instanceof PieGraph) {
                filter.setProtocols(selectedData);
                if (selectedData != null && selectedData.size() > 1) {
                    groupingKey = MainActivity.getInstance().getResources().getString(R.string.rec_protocol);
                }
            }
            if (currentPlotView instanceof BarGraph) {

                if (selectedCompareData.equals(getString(R.string.stats_attacks_essid))) {
                    filter.setESSIDs(selectedData);
                    groupingKey = getString(R.string.essid);
                } else {
                    filter.setBSSIDs(selectedData);
                    groupingKey = getString(R.string.bssid);

                }
                ArrayList<String> currentSelectedProtocol = new ArrayList<>();
                currentSelectedProtocol.add(getCurrentSelectedProtocol());
                filter.setProtocols(currentSelectedProtocol);
            }
            if (currentPlotView instanceof LineGraph) {
                selectedData = new ArrayList<>();
                selectedData.add(item.getTitle());
                filter.setESSIDs(selectedData);
                filter.setProtocols(this.filter.getProtocols());
                groupingKey = getString(R.string.essid);
            }

            if (this.filter.hasATimestamp()) {
                filter.setAboveTimestamp(this.filter.getAboveTimestamp());
                filter.setBelowTimestamp(this.filter.getBelowTimestamp());
            }

            pushRecordOverviewForFilter(filter, groupingKey);
        }
    }

    /**
     * Will be called if the user clicked on a slice
     *
     * @param index of the slice (int)
     */
    public void onSliceClick(int index) {
    }

    /**
     * Will be called if the user clicked on a bar
     *
     * @param index of the bar (int)
     */
    public void onBarClick(int index) {
        userTappedOnLegendItem(index);
    }

    /**
     * Displays a record over view fragment.
     *
     * @param filter      {@link LogFilter LogFilter}
     * @param groupingKey String, key to group the attack list in the RecordOverview
     */
    private void pushRecordOverviewForFilter(LogFilter filter, String groupingKey) {
        // Get the FragmentManager from the MainActivity.
        // The FragmentManager handles every pushing / popping action of a Fragment.
        FragmentManager fm = getActivity().getFragmentManager();

        if (fm != null) {
            // Create a new instance of your Fragment.
            RecordOverviewFragment newFragment = new RecordOverviewFragment();
            // Set true, if the user can navigate backwards from the new pushed fragment.
            newFragment.setUpNavigable(true);

            // Set a Filter Object if needed.
            // Otherwise the RecordOverviewFragment will display all possible records / attacks.
            // With the filter object you also can change the sorting, by changing the sorttype of it.
            if (filter != null) newFragment.setFilter(filter);

            // Set a grouping key, if you want a other grouping behaviour than the default (timestamp).
            if (groupingKey != null && groupingKey.length() != 0)
                newFragment.setGroupKey(groupingKey);

            // Push new Fragment
            MainActivity.getInstance().injectFragment(newFragment);
        }
    }

    /**
     * Callback to filter data after the user has selected filtering date and time.
     *
     * @param date       Date and time value the user has selected in the Date/Time picker dialog.
     * @param filterFrom Flag indicating whether this represents a <i>before</i> or <i>after</i>
     *                   filter
     */
    @Override
    public void dateTimeSelected(Calendar date, boolean filterFrom) {
        if (filterFrom) {
            filter.setAboveTimestamp(date.getTimeInMillis());
        } else {
            filter.setBelowTimestamp(date.getTimeInMillis());
        }
        actualiseCurrentPlot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rootView != null) {
            rootView = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rootView != null) {
            unbindDrawables(rootView);
            rootView = null;
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
