package dk.aau.netsec.hostage.ui.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
    private LayoutInflater inflater;
    private ViewGroup container;
    private Bundle savedInstanceState;
    ProfileManagerRecyclerAdapter myAdapter;

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

        this.inflater = inflater;
        this.container = container;
        this.savedInstanceState = savedInstanceState;

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


        ItemTouchHelper.SimpleCallback touchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(getResources().getColor(R.color.green));


            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {

                container.post(new Runnable() {

                    @Override
                    public void run() {
                        myAdapter.showMenu(viewHolder.getAdapterPosition());
                    }
                });

            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;

                if (dX > 0) {
                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX), itemView.getBottom());
                } else if (dX < 0) {
                    background.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
                } else {
                    background.setBounds(0, 0, 0, 0);
                }

                background.draw(c);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.setOnScrollChangeListener(

                new View.OnScrollChangeListener() {
                    @Override
                    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                        v.post(new Runnable() {
                            @Override
                            public void run() {
                                myAdapter.closeMenu();
                            }
                        });

                    }
                });

//		mAdapter = new ProfileManagerListAdapter(getActivity(), strList, list);
//		pmanager.setProfileListAdapter(mAdapter);
//
//        list.setAdapter(mAdapter);
//
//		// add open and close actions to the items of the list view
//		ProfileManager finalPmanager = pmanager;
//		List<Profile> finalStrList = strList;
//		list.setSwipeListViewListener(new BaseSwipeListViewListener() {
//			@Override
//			public void onOpened(int position, boolean toRight){
//				Profile profile = mAdapter.getItem(position);
//				assert profile != null;
//				if(profile.mShowTooltip){
//					mAdapter.remove(profile);
//					finalStrList.remove(profile);
//					list.dismiss(position);
//
//					mSharedPreferences.edit().putBoolean("dismissedProfileSwipeHelp", true).apply();
//				}
//			}
//
//			@Override
//			public void onClickFrontView(int position) {
//				// active the pressed profile
//				Profile profile = mAdapter.getItem(position);
//				assert profile != null;
//				if(profile.mShowTooltip) return;
//
//				try {
//					finalPmanager.activateProfile(profile);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//
//				mAdapter.notifyDataSetChanged();
//			}
//		});

        return rootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        onCreateView(inflater, container, savedInstanceState);
//		list.closeOpenedItems();
//		mAdapter.notifyDataSetChanged();
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
        if (rootView != null) {
            unbindDrawables(rootView);
            rootView = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (rootView != null) {
            unbindDrawables(rootView);
            rootView = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (rootView != null) {
            unbindDrawables(rootView);
            rootView = null;
        }
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
    }

//	@Override
//	public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
////		recyclerView.post(new Runnable() {
////			@Override
////			public void run() {
////				myAdapter.closeMenu();
////			}
////		});
////	}


}