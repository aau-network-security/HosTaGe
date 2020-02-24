package de.tudarmstadt.informatik.hostage.ui.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;

import de.tudarmstadt.informatik.hostage.ui.fragment.ProfileEditFragment;

/**
 * This activity manages an fragment for editing and creating a profile
 *
 * @author Alexander Brakowski
 * @created 08.02.14 23:36
 */
public class ProfileEditActivity extends PreferenceActivity {
	ProfileEditFragment editFragment;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		// initializes the profile edit fragment
		editFragment = new ProfileEditFragment();

		// injects the fragment into the view
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, editFragment)
				.commit();
	}
}
