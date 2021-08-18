package dk.aau.netsec.hostage.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import java.util.List;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.ui.model.PlotComparisonItem;

/**
 * Created by Julien on 22.02.14.
 */
public class StatisticListAdapter extends ArrayAdapter<PlotComparisonItem> {

    /**
     * Holds all necessary subviews in the rootview.
     */
    private class ViewHolder {
        public TextView titleView;
        public TextView valueView;
        public TextView colorView;
    }

    /**
     * A ValueFormatter converts information containing in the
     * {@link dk.aau.netsec.hostage.ui.model.PlotComparisonItem PlotComparisonItems}
     * in a readable format.
     */
    public interface ValueFormatter {
        String convertValueForItemToString(PlotComparisonItem item);
    }

    private ValueFormatter formatter;
    private final Context context;
    private List<PlotComparisonItem> values;

    /**
     * Set the value formatter.
     * @param formatter ValueFormatter
     */
    public void setValueFormatter(ValueFormatter formatter){
        this.formatter = formatter;
    }

    /**
     * Constructor
     * @param context the context
     * @param objects the representing {@link dk.aau.netsec.hostage.ui.model.PlotComparisonItem PlotComparisonItems}
     */
    public StatisticListAdapter(Context context, List<PlotComparisonItem> objects) {
        super(context, getLayoutID(), objects);
        List<PlotComparisonItem> list = objects == null ? new ArrayList<PlotComparisonItem>() : objects;
        this.context = context;
        this.values = list;
    }

    /**
     * Set the representing {@link dk.aau.netsec.hostage.ui.model.PlotComparisonItem PlotComparisonItems}.
     * @param list List<PlotComparisonItem>
     */
    public void setValues(List<PlotComparisonItem> list){
        this.values = list;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = convertView;
        ViewHolder holder = null;

        final PlotComparisonItem item = values.get(position);

        if(rowView == null){
            rowView = inflater.inflate(getLayoutID() , parent, false);
            holder = new ViewHolder();
            holder.titleView = rowView.findViewById(R.id.title_text_view);
            holder.valueView = rowView.findViewById(R.id.value_text_view);
            holder.colorView = rowView.findViewById(R.id.color_view);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }


        this.configureView(holder, item);

        return rowView;
    }

    /**
     * Returns the items layout ID.
     * @return int layoutID
     */
    static public int getLayoutID(){
        return R.layout.plot_list_item;
    }

    /**
     * Configure the items rootview in here.
     * @param holder ViewHolder
     * @param item {@link dk.aau.netsec.hostage.ui.model.PlotComparisonItem PlotComparisonItem}
     */
    private void configureView(ViewHolder holder, PlotComparisonItem item){
        holder.colorView.setBackgroundColor(item.getColor());
        holder.titleView.setText(item.getTitle());

        if (this.formatter == null){
            holder.valueView.setText("" + item.getValue2());
        } else {
            holder.valueView.setText(this.formatter.convertValueForItemToString(item));
        }
    }
}
