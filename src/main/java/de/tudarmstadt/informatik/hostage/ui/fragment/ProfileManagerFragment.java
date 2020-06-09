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
	    list = rootView.findViewById(R.id.profile_manager_listview);

		final ProfileManager pmanager = ProfileManager.getInstance();
		pmanager.loadData();

	    String sharedPreferencePath = MainActivity.getContext().getString(R.string.shared_preference_path);
	    mSharedPreferences = MainActivity.getContext().getSharedPreferences(sharedPreferencePath, Hostage.MODE_PRIVATE);

        final List<Profile> strList = new LinkedList<Profile>(pmanager.getProfilesList());

		// show an help item in the listview to indicate, that the items in the list are swipeable
	    if(strList.size() > 0 && !mSharedPreferences.getBoolean("dismissedProfileSwipeHelp", false)){
		    Profile tProfile = new Profile();
		    tProfile.mShowTooltip = true;

			strList.add(1, tProfile);
	    }

		mAdapter = new ProfileManagerListAdapter(getActivity(), strList, list);
		pmanager.setProfileListAdapter(mAdapter);

        list.setAdapter(mAdapter);

		// add open and close actions to the items of the list view
		list.setSwipeListViewListener(new BaseSwipeListViewListener() {
			@Override
			public void onOpened(int position, boolean toRight){
				Profile profile = mAdapter.getItem(position);
				if(profile.mShowTooltip){
					mAdapter.remove(profile);
					strList.remove(profile);
					list.dismiss(position);

					mSharedPreferences.edit().putBoolean("dismissedProfileSwipeHelp", true).commit();
				}
			}

			@Override
			public void onClickFrontView(int position) {
				// active the pressed profile
				Profile profile = mAdapter.getItem(position);
				if(profile.mShowTooltip) return;

				pmanager.activateProfile(profile);

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
		switch(item.getItemId()){
			case R.id.profile_manager_action_add:
				Intent intent = new Intent(getActivity(), ProfileEditActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getActivity().startActivity(intent);
				return true;
		}

		return false;
	}
}