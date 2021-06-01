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

public class ProfileManagerRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    List<Profile> list;
    Context context;

    public ProfileManagerRecyclerAdapter(Context context, List<Profile> articlesList) {
        this.list = articlesList;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v;
        v = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_manager_list_item, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Profile entity = list.get(position);
        if (holder instanceof MyViewHolder) {
            ((MyViewHolder) holder).title.setText(entity.mLabel);
            ((MyViewHolder) holder).imageView.setImageDrawable(entity.getIconDrawable());

            if (entity.mActivated) {
                ((MyViewHolder) holder).itemActivated.setVisibility(View.VISIBLE);
            } else {
                ((MyViewHolder) holder).itemActivated.setVisibility(View.GONE);
            }
            ((MyViewHolder) holder).itemText.setText(entity.mText);


            ((MyViewHolder) holder).mBadgesContainer.removeAllViews();
            boolean hasProtocols = false;

            List<String> profiles = new LinkedList<String>(entity.getActiveProtocols());
//
            if (entity.mGhostActive) {
                profiles.add("GHOST");
            }
            for (String protocol : profiles) {
                hasProtocols = true;
                TextView textView = new TextView(new ContextThemeWrapper(((MyViewHolder) holder).container.getContext(), R.style.ProfileManagerListBadge));
                textView.setText(protocol);
                ((MyViewHolder) holder).mBadgesContainer.addView(textView);
            }

            if (!hasProtocols) {
                ((MyViewHolder) holder).mBadgesContainer.setVisibility(View.INVISIBLE);
            } else {
                ((MyViewHolder) holder).mBadgesContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public void addProfile(Profile profile) {
        list.add(profile);
    }

    public void editProfile(Context mContext, Profile profile) {
        Intent intent = new Intent(mContext, ProfileEditActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("profile_id", profile.getId());

        mContext.startActivity(intent);

        System.out.println("Hello there");

    }

    public void removeProfile(Profile profile) {
//        TODO implement stuffs here
        return;
    }

    public Profile getItem(int position) {
        return list.get(position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView itemText;
        ImageView imageView;
        ImageView itemActivated;
        RelativeLayout container;
        FlowLayout mBadgesContainer;

        public MyViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.profile_manager_item_label);
            itemText = itemView.findViewById(R.id.profile_manager_item_text);
            imageView = itemView.findViewById(R.id.profile_manager_item_image);
            itemActivated = itemView.findViewById(R.id.profile_manager_item_activated);
            container = itemView.findViewById(R.id.swipelist_frontview_filip);
            mBadgesContainer = itemView.findViewById(R.id.badges_container);
        }
    }


}

