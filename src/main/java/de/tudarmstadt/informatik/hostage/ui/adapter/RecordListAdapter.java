package de.tudarmstadt.informatik.hostage.ui.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.tudarmstadt.informatik.hostage.HostageApplication;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.persistence.DAO.DAOHelper;
import de.tudarmstadt.informatik.hostage.ui.model.ExpandableListItem;

public class RecordListAdapter extends ExpandableListAdapter {
    private DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
    private DAOHelper daoHelper = new DAOHelper(dbSession);

    /**
     * Constructor
     * @param context the context
     * @param listSectionHeaders the section titles
     * @param dataMapping HashMap<String, ArrayList<{@link ExpandableListItem ExpandableListItem}>> the data to visualise
     */
    public RecordListAdapter(Context context, List<String> listSectionHeaders, HashMap<String, ArrayList<ExpandableListItem>> dataMapping) {
        super(context, listSectionHeaders, dataMapping);
    }


    /*****************************
     *
     *          Required Methods
     *
     * ***************************/

    @Override
    public void configureCellView(View cell, int section, int row) {
        ExpandableListItem object = this.getDataForRow(section, row);
        for (String key : object.getId_Mapping().keySet()){
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
        //int value = this.getChildrenCount(section);
        int value = daoHelper.getMessageRecordDAO().getRecordCount(); //shows the real number of records, not the ones that they are in section.
        tView.setText(this._sectionHeader.get(section));
        vView.setText("" + value);
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
