package de.tudarmstadt.informatik.hostage.ui.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.fortysevendeg.android.swipelistview.BaseSwipeListViewListener;

import java.util.LinkedList;
import java.util.List;

import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.persistence.ProfileManager;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.activity.ProfileEditActivity;
import de.tudarmstadt.informatik.hostage.ui.adapter.ProfileManagerListAdapter;
import de.tudarmstadt.informatik.hostage.ui.swipelist.SwipeListView;

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
	private ProfileManagerListAdapter mAdapter;
	/**
	 * Holds the shared preferences for the app
	 */
	private SharedPreferences mSharedPreferences;

	public ProfileManagerFragment(){}

	/**
	 * Holds the listview for the profile list
	 */
	private SwipeListView list;

	private View rootView;
	private LayoutInflater inflater;
	private ViewGroup container;
	private Bundle savedInstanceState;

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

		this.inflater=inflater;
		this.container=container;
		this.savedInstanceState=savedInstanceState;

		// inflate the view
        rootView = inflater.inflate(R.layout.fragment_profile_manager, container, false);
	    list = rootView.findViewById(R.id.profile_manager_listview);

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

		// show an help item in the listview to indicate, that the items in the list are swipeable
		assert strList != null;
		if(!strList.isEmpty() && !mSharedPreferences.getBoolean("dismissedProfileSwipeHelp", false)){
		    Profile tProfile = new Profile();
		    tProfile.mShowTooltip = true;

			strList.add(1, tProfile);
	    }

		mAdapter = new ProfileManagerListAdapter(getActivity(), strList, list);
		pmanager.setProfileListAdapter(mAdapter);

        list.setAdapter(mAdapter);

		// add open and close actions to the items of the list view
		ProfileManager finalPmanager = pmanager;
		List<Profile> finalStrList = strList;
		list.setSwipeListViewListener(new BaseSwipeListViewListener() {
			@Override
			public void onOpened(int position, boolean toRight){
				Profile profile = mAdapter.getItem(position);
				assert profile != null;
				if(profile.mShowTooltip){
					mAdapter.remove(profile);
					finalStrList.remove(profile);
					list.dismiss(position);

					mSharedPreferences.edit().putBoolean("dismissedProfileSwipeHelp", true).apply();
				}
			}

			@Override
			public void onClickFrontView(int position) {
				// active the pressed profile
				Profile profile = mAdapter.getItem(position);
				assert profile != null;
				if(profile.mShowTooltip) return;

				try {
					finalPmanager.activateProfile(profile);
				} catch (Exception e) {
					e.printStackTrace();
				}

				mAdapter.notifyDataSetChanged();
			}
		});

        return rootView;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResume() {
		super.onResume();
		onCreateView(inflater,container,savedInstanceState);
		list.closeOpenedItems();
		mAdapter.notifyDataSetChanged();
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
		if(rootView!=null) {
			unbindDrawables(rootView);
			rootView=null;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if(rootView!=null) {
			unbindDrawables(rootView);
			rootView=null;
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if(rootView!=null) {
			unbindDrawables(rootView);
			rootView=null;
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
}