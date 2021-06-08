package dk.aau.netsec.hostage.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

//import com.fortysevendeg.android.swipelistview.BaseSwipeListViewListener;

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
//import dk.aau.netsec.hostage.ui.adapter.ProfileManagerListAdapter;
import dk.aau.netsec.hostage.ui.adapter.ProfileManagerRecyclerAdapter;

/**
 * Displays a list of all available profiles and allows invocation of the edit activity for an profile
 *
 * @author Alexander Brakowski
 * @created 14.01.14 15:05
 */
public class ProfileManagerFragment extends TrackerFragment {
    /**
     * The adapter for the profile list
     */
    private ProfileManagerRecyclerAdapter mAdapter;
    /**
     * Holds the shared preferences for the app
     */
    private SharedPreferences mSharedPreferences;

    public ProfileManagerFragment() {
    }


    /**
     * Holds the listview for the profile list
     */
    RecyclerView recyclerView;

    private View rootView;
    //    private LayoutInflater inflater;
//    private ViewGroup container;
//    private Bundle savedInstanceState;
    int posSwiped;
    ProfileManagerRecyclerAdapter myAdapter;

    boolean skipBS = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        Log.d("filipko", "This part?");
        super.onCreateView(inflater, container, savedInstanceState);


        getActivity().setTitle(getResources().getString(R.string.drawer_profile_manager));

        // show action bar menu items
        setHasOptionsMenu(true);

//            this.inflater = inflater;
//            this.container = container;
//            this.savedInstanceState = savedInstanceState;


        // inflate the view
        rootView = inflater.inflate(R.layout.fragment_profile_manager, container, false);
        recyclerView = rootView.findViewById(R.id.filips_recycler_view);

        ProfileManager pmanager = null;
        try {
            pmanager = ProfileManager.getInstance();
            pmanager.loadData();

        } catch (Exception e) {
            e.printStackTrace();
        }

        String sharedPreferencePath = MainActivity.getContext().getString(R.string.shared_preference_path);
        mSharedPreferences = MainActivity.getContext().getSharedPreferences(sharedPreferencePath, Hostage.MODE_PRIVATE);

        List<Profile> strList = null;
        try {
            strList = new LinkedList<>(pmanager.getProfilesList());
        } catch (Exception e) {
            e.printStackTrace();
        }

//		// show an help item in the listview to indicate, that the items in the list are swipeable
//		assert strList != null;
//		if(!strList.isEmpty() && !mSharedPreferences.getBoolean("dismissedProfileSwipeHelp", false)){
//		    Profile tProfile = new Profile();
//		    tProfile.mShowTooltip = true;
//
//			strList.add(1, tProfile);
//	    }


        myAdapter = new ProfileManagerRecyclerAdapter(container.getContext(), strList);
        recyclerView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        recyclerView.setAdapter(myAdapter);

        SwipeToEditCallback swipeHandler = new SwipeToEditCallback(container.getContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int itemPosition = viewHolder.getAdapterPosition();
                myAdapter.editProfile(container.getContext(), myAdapter.getItem(itemPosition));
                posSwiped = itemPosition;
            }
        };

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

        myAdapter.notifyItemChanged(posSwiped);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
//        if (rootView != null) {
//            unbindDrawables(rootView);
//            rootView = null;
//        }
    }

    @Override
    public void onPause() {

        getView().invalidate();
        getView().postInvalidate();
//        if (rootView != null) {
//            unbindDrawables(rootView);
//            rootView = null;
//        }

        super.onPause();

    }


    @Override
    public void onStop() {
        super.onStop();
//        if (rootView != null) {
//            unbindDrawables(rootView);
//            rootView = null;
//        }
    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
//        if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
//            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
//                unbindDrawables(((ViewGroup) view).getChildAt(i));
//            }
//            ((ViewGroup) view).removeAllViews();
//        }
    }




}