package dk.aau.netsec.hostage.ui.model;

import java.util.ArrayList;

/**
 * Created by Julien on 16.02.14.
 */
public class PlotComparisonItem{

    private Double value1;
    private Double value2;

    private String title;

    private Integer color = 0;

    private ArrayList<PlotComparisonItem> childItems;

    /*CONSTRUCTOR*/
    public PlotComparisonItem(String title, Integer color , Double value1, Double value2){
        super();
        this.color = color;
        this.title = title;
        this.value1 = value1;
        this.value2 = value2;
    }

    public ArrayList<PlotComparisonItem> getChildItems(){
        return this.childItems;
    }
    public void setChildItems(ArrayList<PlotComparisonItem> other){
        this.childItems = other;
    }
    public String getTitle(){
        return this.title;
    }
    public Double getValue1(){
        return this.value1;
    }
    public Double getValue2(){
        return this.value2;
    }
    public void setColor(Integer color){
        this.color = color;
    }
    public Integer getColor(){
        return this.color;
    }
}
