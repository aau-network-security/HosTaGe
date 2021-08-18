package dk.aau.netsec.hostage.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.List;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.model.Profile;
import dk.aau.netsec.hostage.ui.activity.ProfileEditActivity;
import dk.aau.netsec.hostage.ui.layouts.FlowLayout;

/**
 * RecyclerAdapter to bind list of profiles obtained from ProfileManager to UI views constructed
 * by RecyclerView
 *
 * @author Filip Adamik
 * Created on 09-06-2021
 */
public class ProfileRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final List<Profile> list;
    private final OnProfileClickedListener mOnProfileClickedListener;

    /**
     * Create ProfileRecyclerAdapter
     *
     * @param profileList             list of profiles to be displayed as rows
     * @param mOnProfileClickListener onClickListener implementation to handle clicks on individual
     *                                profiles (rows of recyclerview)
     */
    public ProfileRecyclerAdapter(List<Profile> profileList, OnProfileClickedListener mOnProfileClickListener) {
        this.list = profileList;
        this.mOnProfileClickedListener = mOnProfileClickListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        //Inflate individual row layout
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_manager_list_item, parent, false);
        return new ProfileRowViewHolder(v, this.mOnProfileClickedListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Profile entity = list.get(position);

        // Most rows use the ProfileRowViewHolder. Fill this view with details of each specific profile
        if (holder instanceof ProfileRowViewHolder) {

            //Set basic details
            ((ProfileRowViewHolder) holder).title.setText(entity.mLabel);
            ((ProfileRowViewHolder) holder).itemText.setText(entity.mText);
            ((ProfileRowViewHolder) holder).imageView.setImageDrawable(entity.getIconDrawable());

            // Show checkmark if profile is activated, hide it otherwise
            if (entity.mActivated) {
                ((ProfileRowViewHolder) holder).itemActivated.setVisibility(View.VISIBLE);
            } else {
                ((ProfileRowViewHolder) holder).itemActivated.setVisibility(View.GONE);
            }

            // Fetch protocols that are active in profile
            ((ProfileRowViewHolder) holder).mBadgesContainer.removeAllViews();
            boolean hasProtocols = false;
            List<String> profiles = new LinkedList<>(entity.getActiveProtocols());

            if (entity.mGhostActive) {
                profiles.add("GHOST");
            }

            // Create a badge for each protocol to be shown underneath the profile
            for (String protocol : profiles) {
                hasProtocols = true;
                TextView textView = new TextView(new ContextThemeWrapper(((ProfileRowViewHolder) holder).container.getContext(), R.style.ProfileManagerListBadge));
                textView.setText(protocol);
                ((ProfileRowViewHolder) holder).mBadgesContainer.addView(textView);
            }

            // Display badges under profile if it has any protocols associated with it
            if (!hasProtocols) {
                ((ProfileRowViewHolder) holder).mBadgesContainer.setVisibility(View.INVISIBLE);
            } else {
                ((ProfileRowViewHolder) holder).mBadgesContainer.setVisibility(View.VISIBLE);
            }

        } else {
            //TODO implement special layout for hint here.
        }
    }

    /**
     * Get number of profiles in the list.
     *
     * @return number of profiles
     */
    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * Add new profile to the list and display it.
     *
     * @param profile New {@link Profile} to be displayed in the UI
     */
    public void addProfile(Profile profile) {
        list.add(profile);
        notifyDataSetChanged();
    }

    /**
     * Launch a {@link ProfileEditActivity} to change properties of an existing profile.
     *
     * @param mContext application context
     * @param profile {@link Profile} to be edited
     */
    public void editProfile(Context mContext, Profile profile) {
        Intent intent = new Intent(mContext, ProfileEditActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("profile_id", profile.getId());

        mContext.startActivity(intent);
    }

    /**
     * Remove profile from the list and the UI
     *
     * @param profile {@link Profile} to be removed.
     */
    public void removeProfile(Profile profile) {
//        TODO revisit - this is not currently supported from the UI
        list.remove(profile);
        return;
    }

    /**
     * Return {@link Profile} at the given position in RecyclerView.
     *
     * @param position Position of the profile in RecyclerView
     * @return {@link Profile} to be returned
     */
    public Profile getItem(int position) {
        return list.get(position);
    }

    /**
     * ViewHolder for a single row in RecyclerView. Contains references to UI elements and binds
     * an OnClick listener to the row.
     */
    public static class ProfileRowViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView title;
        final TextView itemText;
        final ImageView imageView;
        final ImageView itemActivated;
        final RelativeLayout container;
        final FlowLayout mBadgesContainer;
        final OnProfileClickedListener onProfileClickedListener;

        /**
         * Create a reference to a single row in Profile Manager RecyclerView.
         *
         * @param itemView root view for the row
         * @param onProfileClickedListener {@link OnProfileClickedListener} implementation to handle
         *                                                                clicks from fragment
         */
        public ProfileRowViewHolder(View itemView, OnProfileClickedListener onProfileClickedListener) {
            super(itemView);

            itemView.setOnClickListener(this);

            title = itemView.findViewById(R.id.profile_row_item_label);
            itemText = itemView.findViewById(R.id.profile_row_item_text);
            imageView = itemView.findViewById(R.id.profile_row_item_image);
            itemActivated = itemView.findViewById(R.id.profile_row_item_activated);
            container = itemView.findViewById(R.id.list_item_container);
            mBadgesContainer = itemView.findViewById(R.id.profile_row_badges_container);
            this.onProfileClickedListener = onProfileClickedListener;
            this.setIsRecyclable(false);
        }

        /**
         * Call {@link OnProfileClickedListener} implementation.
         *
         * @param itemView item that was clicked
         */
        @Override
        public void onClick(View itemView) {
            onProfileClickedListener.onClicked(getAdapterPosition());
        }
    }

    /**
     * Profile click listener interface. Should be implemented by fragment or activity that
     * contains the RecyclerView
     */
    public interface OnProfileClickedListener {
        void onClicked(int adapterPosition);
    }
}

