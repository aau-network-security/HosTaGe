package de.tudarmstadt.informatik.hostage.ui.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.persistence.ProfileManager;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.activity.ProfileEditActivity;
import de.tudarmstadt.informatik.hostage.ui.layouts.FlowLayout;
import de.tudarmstadt.informatik.hostage.ui.swipelist.SwipeListView;


/**
 * This adapter creates the item views for the profile manager by making use the viewholder pattern
 *
 * @author Alexander Brakowski
 * @created 14.01.14 18:00
 */
public class ProfileManagerListAdapter extends ArrayAdapter<Profile> {

	/**
	 * Holds our views, to reduce the number of view lookups immensly
	 */
	private class ViewHolder {
		public TextView mLabelView;
		public TextView mTextView;
		public ImageView mImageSelected;
		public ImageView mItemIcon;
		public ImageButton mButtonEdit;
		public ImageButton mButtonDelete;
		public View mSeperator;
		public FlowLayout mBadgesContainer;
	}

	/**
	 * The context nedded for resource lookups
	 */
    private final Context mContext;

	/**
	 * The profiles to display in the list
	 */
    private final List<Profile> mValues;

	/**
	 * A reference to the list view itself
	 */
	private SwipeListView mList;

	/**
	 * A simple constructor to propagate this object with neccessary references to needed objects
	 *
	 * @param context needed for resource lookups
	 * @param objects the profiles to display
	 * @param list a reference to the list view
	 */
    public ProfileManagerListAdapter(Context context, List<Profile> objects, SwipeListView list) {
        super(context, R.layout.profile_manager_list_item, objects);
        this.mContext = context;
        this.mValues = objects;
	    this.mList = list;
    }


	/**
	 * {@inheritDoc}
	 */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) mContext
			    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = convertView;
	    ViewHolder holder = null;

	    final Profile item = mValues.get(position);

	    // if the current item has the show tooltip flag set, render this item as an tooltip view
	    if(item.mShowTooltip){
		    rowView = inflater.inflate(R.layout.profile_manager_list_item_help, parent, false);
		    rowView.findViewById(R.id.profile_manager_help_dismiss).setOnClickListener(new View.OnClickListener() {
			    @Override
			    public void onClick(View v) {
				    ProfileManagerListAdapter.this.remove(item);
				    ProfileManagerListAdapter.this.notifyDataSetChanged();
				    mList.dismiss(position);

				    // just show the tooltip as long as it was not dismissed
				    MainActivity.getContext().getSharedPreferences(
						    MainActivity.getContext().getString(R.string.shared_preference_path), Hostage.MODE_PRIVATE
				    ).edit().putBoolean("dismissedProfileSwipeHelp", true).commit();
			    }
		    });
	    } else {
		    // put our views into an view holder, if it is new
		    if (rowView == null || rowView.getTag() == null) {
			    rowView = inflater.inflate(R.layout.profile_manager_list_item, parent, false);

			    holder = new ViewHolder();
			    holder.mLabelView = rowView.findViewById(R.id.profile_manager_item_label);
			    holder.mTextView = rowView.findViewById(R.id.profile_manager_item_text);
			    holder.mImageSelected = rowView.findViewById(R.id.profile_manager_item_activated);
			    holder.mItemIcon = rowView.findViewById(R.id.profile_manager_item_image);
			    holder.mButtonEdit = rowView.findViewById(R.id.profile_manager_item_button_edit);
			    holder.mButtonDelete = rowView.findViewById(R.id.profile_manager_item_button_delete);
			    holder.mSeperator = rowView.findViewById(R.id.profile_manager_item_seperator);
			    holder.mBadgesContainer = rowView.findViewById(R.id.badges_container);

			    rowView.setTag(holder);
		    } else {
			    // save the viewholder to the tag of the view, so we can reuse it later
			    holder = (ViewHolder) rowView.getTag();
		    }

		    // swipe listview needs some cleanup
		    ((SwipeListView) parent).recycle(rowView, position);

		    // fill the item view with the correct data
		    holder.mTextView.setText(item.mText);
		    holder.mLabelView.setText(item.mLabel);

		    if (item.getIconBitmap() != null) {
			    //Bitmap bitmap = Bitmap.createScaledBitmap(item.getIconBitmap(), 32, 32, true);
			    holder.mItemIcon.setImageBitmap(item.getIconBitmap());
		    } else {
			    holder.mItemIcon.setImageBitmap(BitmapFactory.decodeResource(MainActivity.context.getResources(), R.drawable.ic_launcher));
		    }

		    // open the profile edit activity, if the edit button was pressed
		    holder.mButtonEdit.setOnClickListener(new View.OnClickListener() {
			    @Override
			    public void onClick(View v) {
				    Intent intent = new Intent(mContext, ProfileEditActivity.class);
				    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				    intent.putExtra("profile_id", item.mId);

				    mContext.startActivity(intent);
			    }
		    });

		    // delete the profile, if the delete button was pressed. But shows an confirm dialog first.
		    holder.mButtonDelete.setOnClickListener(new View.OnClickListener() {
			    @Override
			    public void onClick(View v) {
				    new AlertDialog.Builder(mContext)
						    .setTitle(R.string.delete_profile)
						    .setMessage(R.string.really_want_delete_profiel)
						    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
							    @Override
							    public void onClick(DialogInterface dialog, int which) {

							    }
						    })
						    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							    public void onClick(DialogInterface dialog, int which) {
								    ProfileManager profileManager = ProfileManager.getInstance();

								    profileManager.deleteProfile(item);
								    profileManager.getProfileListAdapter().notifyDataSetChanged();
								    mList.closeOpenedItems();
							    }
						    })
						    .setIcon(android.R.drawable.ic_dialog_alert)
						    .show();
			    }
		    });

		    // show all the active protocols of an profile in form of badges at the bottom of an profile list item
		    holder.mBadgesContainer.removeAllViews();
		    boolean hasProtocols = false;

		    List<String> profiles = new LinkedList<String>(item.getActiveProtocols());

		    if (item.mGhostActive) {
			    profiles.add("GHOST");
		    }

		    for (String protocol : profiles) {
			    hasProtocols = true;
			    TextView textView = new TextView(new ContextThemeWrapper(mContext, R.style.ProfileManagerListBadge));
			    textView.setText(protocol);
			    holder.mBadgesContainer.addView(textView);
		    }

		    if (!hasProtocols) {
			    holder.mBadgesContainer.setVisibility(View.INVISIBLE);
		    } else {
			    holder.mBadgesContainer.setVisibility(View.VISIBLE);
		    }

		    // do some styling when an profile is flagged as active
		    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.mTextView.getLayoutParams();

		    if (!item.mActivated) {
			    lp.setMargins(0, 0, 0, 0);

			    holder.mTextView.setLayoutParams(lp);

			    holder.mImageSelected.setVisibility(View.GONE);
		    } else {
			    holder.mImageSelected.setVisibility(View.VISIBLE);
		    }

		    if (!item.isEditable()) {
			    holder.mButtonDelete.setVisibility(View.GONE);
			    holder.mSeperator.setVisibility(View.GONE);
		    } else {
			    holder.mButtonDelete.setVisibility(View.VISIBLE);
			    holder.mSeperator.setVisibility(View.VISIBLE);
		    }

	    }

        return rowView;
    }


}
