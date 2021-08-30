package dk.aau.netsec.hostage.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dk.aau.netsec.hostage.Hostage;
import dk.aau.netsec.hostage.R;

/**
 * Shows informations about the developers of the app
 *
 * Created by Fabio Arnold on 25.02.14.
 * displays credits for the app
 *
 */
public class AboutFragment extends Fragment {
    private View rootView;
    private LayoutInflater inflater;
    private ViewGroup container;
    private Bundle savedInstanceState;

    private static final String PRIVACY_POLICY_URL = "https://aau-network-security.github.io/HosTaGe/Privacy_policy.html";
    private static final String TERMS_CONDITIONS_URL  = "https://aau-network-security.github.io/HosTaGe/terms.html";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;
        this.container= container;
        this.savedInstanceState = savedInstanceState;
		final Activity activity = getActivity();
		if (activity != null) {
			activity.setTitle(getResources().getString(R.string.drawer_app_info));
		}

		rootView = inflater.inflate(R.layout.fragment_about, container, false);
        PackageManager manager = Hostage.getContext().getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(Hostage.getContext().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String versionApp;
        assert info != null;
        versionApp = info.versionName;

        TextView hostage = rootView.findViewById(R.id.hostage);
        TextView version = rootView.findViewById(R.id.hostageVersion);

        version.setText("ver. "+versionApp);
		hostage.setMovementMethod(LinkMovementMethod.getInstance());
        version.setMovementMethod(LinkMovementMethod.getInstance());

		return rootView;
	}

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.privacy_policy_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.terms_link_open){
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_CONDITIONS_URL));
            startActivity(browserIntent);

            return true;
        } else if (item.getItemId() == R.id.privacy_link_open){
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL));
            startActivity(browserIntent);

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
    public void onResume() {
        super.onResume();
        onCreateView(inflater,container,savedInstanceState);
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
