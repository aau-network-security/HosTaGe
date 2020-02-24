package de.tudarmstadt.informatik.hostage.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.R;

/**
 * Shows informations about the developers of the app
 *
 * Created by Fabio Arnold on 25.02.14.
 * displays credits for the app
 */
public class AboutFragment extends Fragment {
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		super.onCreateView(inflater, container, savedInstanceState);

		final Activity activity = getActivity();
		if (activity != null) {
			activity.setTitle(getResources().getString(R.string.drawer_app_info));
		}

		View rootView = inflater.inflate(R.layout.fragment_about, container, false);
        PackageManager manager = Hostage.getContext().getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(Hostage.getContext().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String versionApp;
        versionApp = info.versionName;

        TextView hostage = (TextView) rootView.findViewById(R.id.hostage);
        TextView version = (TextView) rootView.findViewById(R.id.hostageVersion);

        version.setText("ver. "+versionApp);
		hostage.setMovementMethod(LinkMovementMethod.getInstance());
        version.setMovementMethod(LinkMovementMethod.getInstance());

		return rootView;
	}
}
