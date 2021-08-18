package dk.aau.netsec.hostage.ui.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.List;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.model.Profile;
import dk.aau.netsec.hostage.persistence.ProfileManager;
import dk.aau.netsec.hostage.ui.activity.MainActivity;
import dk.aau.netsec.hostage.ui.activity.ProfileEditActivity;
import dk.aau.netsec.hostage.ui.adapter.ProfileRecyclerAdapter;
import dk.aau.netsec.hostage.ui.helper.SwipeToEditCallback;

/**
 * Displays a list of all available profiles and allows invocation of the edit activity for a profile
 *
 * @author Alexander Brakowski
 * @created 14.01.14 15:05
 */
public class ProfileManagerFragment extends TrackerFragment implements ProfileRecyclerAdapter.OnProfileClickedListener {

    public ProfileManagerFragment() {
    }

    RecyclerView recyclerView;
    ProfileRecyclerAdapter profileRecyclerAdapter;
    ProfileManager profileManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        getActivity().setTitle(getResources().getString(R.string.drawer_profile_manager));

        // show action bar menu items
        setHasOptionsMenu(true);

        // inflate the view
        View rootView = inflater.inflate(R.layout.fragment_profile_manager, container, false);
        recyclerView = rootView.findViewById(R.id.profile_manager_recycler_view);

        // Get ProfileManager instance
        profileManager = null;

        profileManager = ProfileManager.getInstance();
        profileManager.loadData();

        String sharedPreferencePath = MainActivity.getContext().getString(R.string.shared_preference_path);

//          Holds the shared preferences for the app
        SharedPreferences mSharedPreferences = MainActivity.getContext().getSharedPreferences(sharedPreferencePath, Hostage.MODE_PRIVATE);


//          Get list of profiles to be displayed in the recyclerview

        List<Profile> strList;
        assert profileManager != null;
        strList = new LinkedList<>(profileManager.getProfilesList());

//        TODO re-add hint (was disabled during RecyclerView implementation
//		// show an help item in the listview to indicate, that the items in the list are swipeable
//		assert strList != null;
//		if(!strList.isEmpty() && !mSharedPreferences.getBoolean("dismissedProfileSwipeHelp", false)){
//		    Profile tProfile = new Profile();
//		    tProfile.mShowTooltip = true;
//
//			strList.add(1, tProfile);
//	    }

        //Get a ProfileRecyclerAdapter and assign it to ProfileManager
        profileRecyclerAdapter = new ProfileRecyclerAdapter(strList, this);
        profileManager.setProfileListAdapter(profileRecyclerAdapter);

        //Initialize RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        recyclerView.setAdapter(profileRecyclerAdapter);

//         Implement Swipe-to-edit functionality (swipe left to launch a ProfileEditFragment)
        SwipeToEditCallback swipeHandler = new SwipeToEditCallback(container.getContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int itemPosition = viewHolder.getAdapterPosition();
                profileRecyclerAdapter.editProfile(container.getContext(),
                        profileRecyclerAdapter.getItem(itemPosition));
            }
        };

        // Create and attach Swipe-to-edit handler to recyclerview
        ItemTouchHelper helper = new ItemTouchHelper(swipeHandler);
        helper.attachToRecyclerView(recyclerView);

        return rootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();

        //Refresh recyclerAdapter to remove graphics possibly left after a swipe.
        profileRecyclerAdapter.notifyDataSetChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.profile_manager_actions, menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.profile_manager_action_add) {
            Intent intent = new Intent(getActivity(), ProfileEditActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
            return true;
        }

        return false;
    }

    /***
     * Activate profile if it has been clicked.
     *
     * @param position position of the clicked Profile within profileRecyclerAdapter
     */
    @Override
    public void onClicked(int position) {
        profileManager.activateProfile(profileRecyclerAdapter.getItem(position));

        //Refresh view to display checkmark next to profile
        profileRecyclerAdapter.notifyDataSetChanged();
    }

}