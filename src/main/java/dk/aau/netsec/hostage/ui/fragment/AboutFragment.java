package dk.aau.netsec.hostage.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

	@SuppressLint("SetTextI18n")
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

        ImageView img_1 = (ImageView) rootView.findViewById(R.id.img_1);
        ImageView img_2 = (ImageView) rootView.findViewById(R.id.img_2);
        ImageView img_3 = (ImageView) rootView.findViewById(R.id.img_3);
        ImageView img_4 = (ImageView) rootView.findViewById(R.id.img_4);

        img_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tu-darmstadt.de/"));
                startActivity(browserIntent);
            }
        });

        img_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.en.aau.dk/"));
                startActivity(browserIntent);
            }
        });

        img_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://northsearegion.eu/"));
                startActivity(browserIntent);
            }
        });

        img_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://keep.eu/projects/22182/Building-COMpetencies-for-C-EN/"));
                startActivity(browserIntent);
            }
        });

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
