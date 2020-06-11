package de.tudarmstadt.informatik.hostage.persistence.DAO;

import android.os.Build;
import android.support.annotation.RequiresApi;


import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import de.tudarmstadt.informatik.hostage.logging.AttackRecord;
import de.tudarmstadt.informatik.hostage.logging.AttackRecordDao;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.NetworkRecord;
import de.tudarmstadt.informatik.hostage.logging.NetworkRecordDao;
import de.tudarmstadt.informatik.hostage.ui.helper.ColorSequenceGenerator;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;
import de.tudarmstadt.informatik.hostage.ui.model.PlotComparisonItem;


public class NetworkRecordDAO extends DAO {
    private DaoSession daoSession;

    public NetworkRecordDAO(DaoSession daoSession){
        this.daoSession= daoSession;

    }

    public void insert(NetworkRecord record){
        NetworkRecordDao recordDao = this.daoSession.getNetworkRecordDao();
        insertElement(recordDao,record);
    }

    private ArrayList<NetworkRecord> getNetworkRecords(){
        NetworkRecordDao recordDao = this.daoSession.getNetworkRecordDao();
        ArrayList<NetworkRecord> networkRecords = (ArrayList<NetworkRecord>) selectElements(recordDao);
        return  networkRecords;

    }
    /**
     * Gets all network related data stored in the database
     * @return An ArrayList with an Network for all Entry in the network table.
     */
    public synchronized ArrayList<NetworkRecord> getNetworkInformation() {
        return this.getNetworkRecords();
    }

    /**
     * Updates the network table with the information contained in the parameter.
     * @param networkInformation ArrayList of {@link NetworkRecord NetworkRecords}
     */
    public synchronized void updateNetworkInformation(ArrayList<NetworkRecord> networkInformation) {
        NetworkRecordDao recordDao = this.daoSession.getNetworkRecordDao();
        updateElements(recordDao,networkInformation);

    }


    /**
     * Updated the network table with a new {@link NetworkRecord}.
     * If information about this BSSID are already in the database,
     * the table will only be updated if the new {@link NetworkRecord }
     * has a newer hostage.location time stamp.
     * @param record The new {@link NetworkRecord}.
     */
    public synchronized void updateNetworkInformation(NetworkRecord record) {
        NetworkRecordDao recordDao = this.daoSession.getNetworkRecordDao();
        updateElement(recordDao,record);
    }

    /**
     * Returns a String array with all BSSIDs stored in the database.
     *
     * @return ArrayList<String> of all recorded BSSIDs.
     */
    public synchronized ArrayList<String> getAllBSSIDS() {
        ArrayList<String> bssidList = new ArrayList<String>();
        ArrayList<NetworkRecord> networkRecords = this.getNetworkRecords();

        for(NetworkRecord record:networkRecords){
            String s = record.getBssid();
            bssidList.add(s);
        }

        return  bssidList;
    }

    /**
     * Returns all missing bssids.
     *
     * @return a list of missing network records.
     */
    private synchronized ArrayList<String> getMissingNetworkBssids(ArrayList<String> otherBSSIDs) {
        ArrayList<String> currentBSSIDs = getAllBSSIDS();
        ArrayList<String> notPresent = new ArrayList<String>(otherBSSIDs);
        notPresent.removeAll(currentBSSIDs);

        return notPresent;

    }

    /**
     * Returns all missing records bssids.
     *
     * @return a list of missing network records.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public synchronized ArrayList<NetworkRecord> getMissingNetworkRecords(ArrayList<String> otherBSSIDs) {
        ArrayList<String> missingBSSIDs = getMissingNetworkBssids( otherBSSIDs);
        ArrayList<NetworkRecord> networkRecords = this.getNetworkRecords();
        ArrayList<NetworkRecord> missingNetworkRecords = new ArrayList<NetworkRecord>();

        ArrayList<String> currentBssids= new ArrayList<>();
        networkRecords.stream().filter(o -> currentBssids.add(o.getBssid())).collect(Collectors.toList());

        missingBSSIDs.removeAll(currentBssids);

        for(String missingBssid:missingBSSIDs){
            NetworkRecord record = new NetworkRecord();
            record.setBssid(missingBssid);
            missingNetworkRecords.add(record);
        }


        return  missingNetworkRecords;

    }

    private synchronized NetworkRecord createNetworkRecord(NetworkRecord existingRecord){
        NetworkRecord record = new NetworkRecord();
        record.setBssid(existingRecord.getBssid());
        record.setSsid(existingRecord.getSsid());
        record.setLatitude(existingRecord.getLatitude());
        record.setLongitude(existingRecord.getLongitude());
        record.setAccuracy(existingRecord.getAccuracy());
        record.setTimestampLocation(existingRecord.getTimestampLocation());
        return record;
    }

    /**
     * Deletes networkRecords from the network table for the given filter object.
     * @param filter
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void deleteFromFilter(LogFilter filter){
        ArrayList<NetworkRecord> filterBSSIDs = selectionBSSIDFromFilter(filter);
        ArrayList<NetworkRecord> filterESSIDs = selectionESSIDFromFilter(filter);

        NetworkRecordDao recordDao = this.daoSession.getNetworkRecordDao();
        recordDao.deleteInTx(filterBSSIDs);
        recordDao.deleteInTx(filterESSIDs);

    }


    /**
     * Returns the query for the given filter.
     * @param filter (LogFilter)
     * @return QueryBuilder<AttackRecord> query
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public ArrayList<NetworkRecord> selectionBSSIDFromFilter(LogFilter filter) {
        ArrayList<String> filterBSSIDs = filter.getBSSIDs();
        ArrayList<NetworkRecord> networkRecords = this.getNetworkRecords();
        ArrayList<NetworkRecord> list = new ArrayList<>();


        if(filterBSSIDs != null && filterBSSIDs.size() > 0) {
            for (final String current : filterBSSIDs) {

                list.add(networkRecords.stream().filter(o -> o.getBssid().equals(current)).findAny().orElse(null));

            }
        }
        list.removeAll(Collections.singleton(null));

        return list;

    }

    /**
     * Returns the query for the given filter.
     * @param filter (LogFilter)
     * @return QueryBuilder<AttackRecord> query
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public ArrayList<NetworkRecord> selectionESSIDFromFilter(LogFilter filter) {
        ArrayList<String> filterESSIDs = filter.getESSIDs();
        ArrayList<NetworkRecord> networkRecords = this.getNetworkRecords();
        ArrayList<NetworkRecord> list = new ArrayList<>();


        if(filterESSIDs != null && filterESSIDs.size() > 0) {
            for (final String current : filterESSIDs) {

                list.add(networkRecords.stream().filter(o -> o.getSsid().equals(current)).findAny().orElse(null));

            }
        }

        list.removeAll(Collections.singleton(null));

        return list;

    }

    /**
     * Gets all non duplicate Records For the key ESSID.
     *
     * @return A ArrayList with received Records.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public synchronized ArrayList<String> getUniqueESSIDRecords() {
        ArrayList<NetworkRecord> networkRecords = this.getNetworkRecords();

        ArrayList<String> essids= new ArrayList<>();
        networkRecords.stream().filter(o -> essids.add(o.getSsid())).collect(Collectors.toList());
        ArrayList<String> distinctEssids = (ArrayList<String>) essids.stream().distinct().collect(Collectors.toList());

        return  distinctEssids;

    }

    /**
     * Gets all non duplicate Records For the key BSSID.
     *
     * @return A ArrayList with received Records.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public synchronized ArrayList<String> getUniqueBSSIDRecords() {
        ArrayList<NetworkRecord> networkRecords = this.getNetworkRecords();

        ArrayList<String> bssids= new ArrayList<>();
        networkRecords.stream().filter(o -> bssids.add(o.getBssid())).collect(Collectors.toList());
        ArrayList<String> distinctBssids = (ArrayList<String>) bssids.stream().distinct().collect(Collectors.toList());

        return  distinctBssids;

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public synchronized ArrayList<String> getUniqueESSIDRecordsForProtocol(String protocol) {
        AttackRecordDAO attackRecordDAO = new AttackRecordDAO(this.daoSession);
        ArrayList<AttackRecord> filteredAttackRecords = attackRecordDAO.getAttacksPerProtocol(protocol);

        ArrayList<NetworkRecord> filterNetworkRecords=  new ArrayList<>();
        filteredAttackRecords.stream().filter(o -> filterNetworkRecords.add(o.getRecord())).collect(Collectors.toList());

        ArrayList<String> essids= new ArrayList<>();
        filterNetworkRecords.stream().filter(o -> essids.add(o.getSsid())).collect(Collectors.toList());
        ArrayList<String> distinctEssids = (ArrayList<String>) essids.stream().distinct().collect(Collectors.toList());


        return  distinctEssids;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public synchronized ArrayList<String> getUniqueBSSIDRecordsForProtocol(String protocol) {
        AttackRecordDAO attackRecordDAO = new AttackRecordDAO(this.daoSession);
        ArrayList<AttackRecord> filteredAttackRecords = attackRecordDAO.getAttacksPerProtocol(protocol);


        ArrayList<NetworkRecord> filterNetworkRecords=  new ArrayList<>();
        filteredAttackRecords.stream().filter(o -> filterNetworkRecords.add(o.getRecord())).collect(Collectors.toList());

        ArrayList<String> bssids = new ArrayList<>();
        filterNetworkRecords.stream().filter(o -> bssids.add(o.getBssid())).collect(Collectors.toList());
        ArrayList<String> distinctBssids = (ArrayList<String>) bssids.stream().distinct().collect(Collectors.toList());


        return  distinctBssids;


    }

    /**
     * Returns PlotComparisionItems for attacks per essid.
     * @param filter (LogFilter) filter object
     *
     * @return ArrayList<PlotComparisonItem>
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public synchronized ArrayList<PlotComparisonItem> attacksPerESSID(LogFilter filter) {
        AttackRecordDAO attackRecordDAO = new AttackRecordDAO(this.daoSession);
        ArrayList<AttackRecord> filteredAttackRecords = attackRecordDAO.selectionQueryFromFilter(filter);

        ArrayList<NetworkRecord> filterNetworkRecords=  new ArrayList<>();
        filteredAttackRecords.stream().filter(o -> filterNetworkRecords.add(o.getRecord())).collect(Collectors.toList());


        ArrayList<PlotComparisonItem> plots = new ArrayList<PlotComparisonItem>();

        return  addPlotComparison(plots,filterNetworkRecords);
    }

    private  ArrayList<PlotComparisonItem> addPlotComparison( ArrayList<PlotComparisonItem> plots, ArrayList<NetworkRecord> filterNetworkRecords){
        int counter = 0;
        ArrayList<String> bssids = getAllBSSIDS();
        for(NetworkRecord record: filterNetworkRecords){
            String title = record.getBssid(); // COLUMN_NAME_SSID
            double value =  Collections.frequency(bssids, title);// COUNT
            if (value == 0.) continue;
            PlotComparisonItem plotItem = new PlotComparisonItem(title, this.getColor(counter), 0. , value);
            plots.add(plotItem);

            counter++;
        }

        return  plots;
    }

    /** Returns the color for the given index
     * @return int color*/
    public Integer getColor(int index) {
        return ColorSequenceGenerator.getColorForIndex(index);
    }


    }
