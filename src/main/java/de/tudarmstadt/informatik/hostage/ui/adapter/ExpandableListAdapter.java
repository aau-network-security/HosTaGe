package de.tudarmstadt.informatik.hostage.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.tudarmstadt.informatik.hostage.ui.model.ExpandableListItem;


/**
 * Created by Julien on 06.02.14.
 */
public abstract class ExpandableListAdapter extends BaseExpandableListAdapter {

    private Context _context;

    // header titles
    public List<String> _sectionHeader;
    // data in format of header title, childs list
    public HashMap<String, ArrayList<ExpandableListItem>> _sectionTitleToChildData;

    /**
     * Constructor
     * @param context the context
     * @param listSectionHeaders the section title
     * @param dataMapping {@link ExpandableListItem ExpandableListItem} the data to visualise
     */
    public ExpandableListAdapter(Context context, List<String> listSectionHeaders,
                                 HashMap<String, ArrayList<ExpandableListItem>> dataMapping) {
        this._context = context;
        this._sectionHeader = listSectionHeaders;
        this._sectionTitleToChildData = dataMapping;
    }

    public void setData(HashMap<String, ArrayList<ExpandableListItem>> dataMapping){
        this._sectionTitleToChildData = dataMapping;
    }
    public HashMap<String, ArrayList<ExpandableListItem>> getData(){
        return this._sectionTitleToChildData;
    }
    public void setSectionHeader(List<String> listSectionHeaders){
        this._sectionHeader = listSectionHeaders;
    }
    public List<String> getSectionHeaders(){
        return this._sectionHeader;
    }

    @Override
    public Object getChild(int section, int row) {
        return this._sectionTitleToChildData.get(this._sectionHeader.get(section))
                .get(row);
    }

    @Override
    public long getChildId(int section, int row) {
        return row;
    }

    @Override
    public View getChildView(int section, final int row,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(this.getCellLayoutID(), null);
        }
        this.configureCellView(convertView, section, row);
        return convertView;
    }

    @Override
    public int getChildrenCount(int section) {
        if(this._sectionTitleToChildData.size() == 0) return 0;

        return this._sectionTitleToChildData.get(this._sectionHeader.get(section))
                .size();
    }

    @Override
    public Object getGroup(int section) {
        return this._sectionHeader.get(section);
    }

    @Override
    public int getGroupCount() {
        return this._sectionHeader.size();
    }

    @Override
    public long getGroupId(int section) {
        return section;
    }

    @Override
    public View getGroupView(int section, boolean isExpanded,
                             View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(this.getSectionLayoutID(), null);
        }
        this.configureSectionHeaderView(convertView, section);

        return convertView;
    }

    /**
     * Return the {@link ExpandableListItem ExpandableListItem} for the given index path
     * @param section int
     * @param row int
     * @return {@link ExpandableListItem ExpandableListItem}
     */
    public ExpandableListItem getDataForRow(int section, int row){
        return this._sectionTitleToChildData.get(this._sectionHeader.get(section)).get(row);
    }


    /**
     * Configure the items root view in here
     * @param cell View, the root view
     * @param section int
     * @param row int
     */
    public abstract void configureCellView(View cell, int section, int row);
    public abstract void configureSectionHeaderView(View sectionHeader, int section);

    /**
     * Returns the section header layout id.
    * @return R.layout.list_section
    * */
    public abstract  int getSectionLayoutID();
    /**
     * Return the  root view layout id.
     * @return R.layout.list_cell
     * */
    public abstract  int getCellLayoutID();


    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int section, int row) {
        return true;
    }
}