package dk.aau.netsec.hostage.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.ui.model.DrawerListItem;

/**
 * Creates the item view for the navigation drawer listview
 *
 * @author Alexander Brakowski
 * @created 13.01.14 16:35
 */
public class DrawerListAdapter extends ArrayAdapter<DrawerListItem> {

	/**
	 * The context the adapter needs to retrieve resources
	 */
    private final Context mContext;

	/**
	 * The list items
	 */
    private final List<DrawerListItem> mValues;

	/**
	 * Create the list adapter
	 *
	 * @param context the context needed for resource retrieval
	 * @param objects all the items that should be displayed in the list
	 */
    public DrawerListAdapter(Context context, List<DrawerListItem> objects) {
        super(context, R.layout.drawer_list_item, objects);
        this.mContext = context;
        this.mValues  = objects;
    }

	/**
	 * {@inheritDoc}
	 */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.drawer_list_item, parent, false);
        TextView textView = rowView.findViewById(R.id.drawer_listitem_text);
        ImageView imageView = rowView.findViewById(R.id.drawer_listitem_icon);

        DrawerListItem item = mValues.get(position);
        textView.setText(item.text);
        imageView.setImageResource(item.icon);

        return rowView;
    }
}
