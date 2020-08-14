package de.tudarmstadt.informatik.hostage.ui.model;

import java.util.HashMap;


/**
 * Created by Julien on 06.02.14.
 */
public class ExpandableListItem {

    /*Mapping Data Key To ViewID*/
    public HashMap<String, Integer> id_Mapping;

    /*Data Key To Textual Information*/
    public HashMap<String, String> data;

    public long getTag() {
        return tag;
    }
    public void setTag(long tag) {
        this.tag = tag;
    }

    private long tag;



    public HashMap<String, Integer> getId_Mapping() {
        return id_Mapping;
    }
    public void setId_Mapping(HashMap<String, Integer> id_Mapping) {
        this.id_Mapping = id_Mapping;
    }

    public HashMap<String, String> getData(){
        return this.data;
    }
    public void setData(HashMap<String, String> _data){
        this.data = _data;
    }
}
