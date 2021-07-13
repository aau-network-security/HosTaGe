package dk.aau.netsec.hostage.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Objects;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.model.Profile;
import dk.aau.netsec.hostage.persistence.ProfileManager;

/**
 * Creates an preference screen to edit an profile
 *
 * @author Alexander Brakowski
 * @created 08.02.14 23:39
 */
public class ProfileEditFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Holds the shared preference editor for out preference screen
     */
    private SharedPreferences.Editor mPrefs;

    /**
     * A map which mirrors the state protocols in the preferencescreen
     */
    private HashMap<String, Boolean> mProfileProtocols;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // remove default action bar and replace it with an "done"/"discard" action bar
        View actionBarButtons = inflater.inflate(R.layout.actionbar_donebar, new LinearLayout(getActivity()), false);
        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setCustomView(actionBarButtons);

        View doneButton = actionBarButtons.findViewById(R.id.action_done);
        View cancelButton = actionBarButtons.findViewById(R.id.action_cancel);

        // add click listener for the save button
        doneButton.setOnClickListener(v -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            ProfileManager pmanager = null;

            pmanager = ProfileManager.getInstance();

            Profile profile = null;

            profile = getProfile();

            boolean createNew = false;

            // no profile was given to the fragment, which means this is a new profile
            if (profile == null) {
                profile = new Profile();
                createNew = true;
            } else {
                // profile was given, if profile is not editable, clone the profile and make it editable
                if (!profile.isEditable()) {
                    profile = profile.cloneProfile();
                    profile.mEditable = true;
                    createNew = true;
                }
            }

            // update the profile object data with data from the preferences
            profile.mLabel = prefs.getString("pref_profile_general_name", profile.mLabel);
            profile.mIconPath = prefs.getString("pref_profile_general_image", profile.mIconPath);
            profile.mText = prefs.getString("pref_profile_general_description", profile.mText);
            profile.mGhostActive = prefs.getBoolean("pref_profile_protocols_ghost_active", profile.mGhostActive);
            profile.mGhostPorts = prefs.getString("pref_profile_protocols_ghost_text", "");

            if (profile.mLabel == null || profile.mLabel.isEmpty()) {
                new AlertDialog.Builder(getActivity()).setTitle(R.string.information).setMessage(R.string.profile_needs_name)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {

                        }).setIcon(android.R.drawable.ic_dialog_info).show();
                return;
            }

            if (profile.mGhostPorts.isEmpty()) {
                profile.mGhostActive = false;
            }

            profile.mActiveProtocols = new HashMap<String, Boolean>(mProfileProtocols);

            // persist the changes of the profile
            if (createNew) {
                profile.mId = -1;
                profile.mIconId = 0;
                profile.mIconName = "";
                profile.mIsRandom = false;
                profile.mIcon = null;
                pmanager.addProfile(profile);
            } else {
                pmanager.persistProfile(profile);
            }

            getActivity().finish();
        });

        cancelButton.setOnClickListener(v -> getActivity().finish());
    }

    /**
     * Called during {@link #onCreate(Bundle)} to supply the preferences for this fragment.
     * Subclasses are expected to call {@link #} either
     * directly or via helper methods such as {@link #addPreferencesFromResource(int)}.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     *                           this is the state.
     * @param rootKey            If non-null, this preference fragment should be rooted at the
     *                           {@link } with this key.
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Profile profile = null;
        profile = getProfile();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity()).edit();

        String pname = "",
                pimage = null,
                pdesc = "",
                pghost = "";

        boolean pbghost = false;

        if (profile != null) {
            pname = profile.mLabel;
            pimage = profile.mIconPath;
            pdesc = profile.mText;
            pghost = profile.mGhostPorts;
            pbghost = profile.mGhostActive;
        }

        // fill the preferences of the preference screen with data from the profile object
        mPrefs.putString("pref_profile_general_name", pname);
        mPrefs.putString("pref_profile_general_image", pimage);
        mPrefs.putString("pref_profile_general_description", pdesc);
        mPrefs.putString("pref_profile_protocols_ghost_text", pghost);
        mPrefs.putBoolean("pref_profile_protocols_ghost_active", pbghost);

        mPrefs.apply();

        // create the preference view
        addPreferencesFromResource(R.xml.profile_preferences);

        Preference pref = findPreference("pref_profile_general_image");

        assert pref != null;

        if (profile != null) {
            pref.setIcon(profile.getIconDrawable());
            mProfileProtocols = new HashMap<>(profile.mActiveProtocols);
        } else {
            mProfileProtocols = new HashMap<>();
        }

        // show an image chooser dialog when pressing the image preference
        pref.setOnPreferenceClickListener(
                preference -> {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    int PICK_IMAGE = 1;
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.select_icon)), PICK_IMAGE);
                    return true;
                }
        );

        if (profile != null) {
            findPreference("pref_profile_general_name").setSummary(profile.mLabel);
            findPreference("pref_profile_general_description").setSummary(profile.mText);

            if (!profile.mGhostPorts.isEmpty())
                findPreference("pref_profile_protocols_ghost_text").setSummary(profile.mGhostPorts);
        }

        if (profile == null || profile.isEditable()) {
            getPreferenceScreen().removePreference(findPreference("pref_profile_warning"));
        }

        PreferenceCategory protocolsCategory = (PreferenceCategory) findPreference("pref_profile_protocols_settings");
        String[] protocols = getResources().getStringArray(R.array.protocols);
        String[] protocols_summary = getResources().getStringArray(R.array.protocols_description);

        // add all available protocols to the preference screen with an checkbox
        for (int i = 0; i < protocols.length; i++) {
            mPrefs.putBoolean("pref_profile_protocol_" + protocols[i], profile != null && profile.isProtocolActive(protocols[i]));
            mPrefs.commit();

            CheckBoxPreference check = new CheckBoxPreference(getActivity());
            check.setTitle(protocols[i]);
            check.setKey("pref_profile_protocol_" + protocols[i]);
            check.setSummary(protocols_summary[i]);

            protocolsCategory.addPreference(check);
        }

    }

    /**
     * Retrieve the given profile from the intent
     *
     * @return the profile
     */
    public Profile getProfile() {
        ProfileManager pmanager = ProfileManager.getInstance();

        Intent intent = getActivity().getIntent();
        int profile_id = intent.getIntExtra("profile_id", -1);

        if (profile_id != -1) {
            return pmanager.getProfile(profile_id);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Called when a shared preference is changed, added, or removed. This
     * may be called even if a preference is set to its existing value.
     *
     * <p>This callback will be run on your main thread.
     *
     * @param sharedPreferences The {@link android.content.SharedPreferences} that received
     *                          the change.
     * @param key               The key of the preference that was changed, added, or
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference p = findPreference(key);

        if (p instanceof EditTextPreference) {
            p.setSummary(sharedPreferences.getString(key, ""));
        } else if (p instanceof CheckBoxPreference && !p.getKey().equals("pref_profile_protocols_ghost_active")) {
            mProfileProtocols.put(p.getTitle().toString(), ((CheckBoxPreference) p).isChecked());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        if (resultCode == AppCompatActivity.RESULT_OK) {
            Cursor cursor = getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{
                            MediaStore.Images.Media.DATA,
                            MediaStore.Images.Media.DATE_ADDED,
                            MediaStore.Images.ImageColumns.ORIENTATION
                    },
                    MediaStore.Images.Media.DATE_ADDED, null, "date_added ASC");

            String filePath = "";
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    filePath = Uri.parse(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))).toString();
                } while (cursor.moveToNext());
                cursor.close();
            }

            Preference pref = findPreference("pref_profile_general_image");

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

            pref.setIcon(new BitmapDrawable(getResources(), bitmap));

            mPrefs.putString("pref_profile_general_image", filePath);
            mPrefs.commit();
        }
    }
}
