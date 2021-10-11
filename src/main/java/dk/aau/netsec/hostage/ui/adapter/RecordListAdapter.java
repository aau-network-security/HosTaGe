package dk.aau.netsec.hostage.ui.adapter;

import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.ui.model.ExpandableListItem;

public class RecordListAdapter extends ExpandableListAdapter {
    private final DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
    private final DAOHelper daoHelper = new DAOHelper(dbSession);

    /**
     * Constructor
     *
     * @param listSectionHeaders the section titles
     * @param dataMapping        HashMap<String, ArrayList<{@link ExpandableListItem ExpandableListItem}>> the data to visualise
     */
    public RecordListAdapter(List<String> listSectionHeaders, HashMap<String, ArrayList<ExpandableListItem>> dataMapping) {
        super(listSectionHeaders, dataMapping);
    }


    /*****************************
     *
     *          Required Methods
     *
     * ***************************/

    @Override
    public void configureCellView(View cell, int section, int row) {
        ExpandableListItem object = this.getDataForRow(section, row);
        for (String key : object.getId_Mapping().keySet()) {
            int viewID = object.getId_Mapping().get(key);
            String textualInfo = object.getData().get(key);
            TextView tView = cell.findViewById(viewID);
            tView.setText(textualInfo);
        }
    }

    @Override
    public void configureSectionHeaderView(View sectionHeader, int section) {
        int headerLabelID = R.id.sectionHeaderTitle;
        int valueLabelID = R.id.sectionHeaderValue;
        TextView tView = sectionHeader.findViewById(headerLabelID);
        TextView vView = sectionHeader.findViewById(valueLabelID);
        int nowValue = this.getChildrenCount(section);
        int value = daoHelper.getMessageRecordDAO().getRecordCount(); //shows the real number of records, not the ones that they are in section.
        tView.setText(this._sectionHeader.get(section));
        vView.setText(nowValue + "/" + value);
    }

    @Override
    public int getSectionLayoutID() {
        return R.layout.expandable_section_header;
    }

    @Override
    public int getCellLayoutID() {
        return R.layout.record_list_item;
    }
}
