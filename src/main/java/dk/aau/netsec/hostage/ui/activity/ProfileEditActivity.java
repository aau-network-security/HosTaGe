package dk.aau.netsec.hostage.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import dk.aau.netsec.hostage.ui.fragment.ProfileEditFragment;


/**
 * This activity manages an fragment for editing and creating a profile
 *
 * @author Alexander Brakowski
 * @created 08.02.14 23:36
 */
public class ProfileEditActivity extends AppCompatActivity {
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
		getSupportFragmentManager().beginTransaction()
				.replace(android.R.id.content,editFragment)
				.commit();
	}

	@Override
	public void on BackPressed(){
		super.onBackPressed();
		Intent intent=new
		Intent(ProfileEditActivity.this,MainActivity.class);
		startActivity(intent);
		finish();
	}


}
