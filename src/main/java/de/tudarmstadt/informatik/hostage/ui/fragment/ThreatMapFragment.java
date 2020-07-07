package de.tudarmstadt.informatik.hostage.ui.fragment;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.Settings;
import android.text.Html;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import de.tudarmstadt.informatik.hostage.HostageApplication;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.logging.DaoSession;
import de.tudarmstadt.informatik.hostage.logging.RecordAll;
import de.tudarmstadt.informatik.hostage.persistence.DAO.DAOHelper;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.model.LogFilter;


/**
 * ThreatMapFragment
 *
 * Created by Fabio Arnold on 10.02.14.
 */
public class ThreatMapFragment extends TrackerFragment implements GoogleMap.OnInfoWindowClickListener, OnMapReadyCallback,
		LocationListener {

	private static GoogleMap sMap;
	private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

	private static MapView mapView = null;

	private static Thread mLoader = null;

	private static HashMap<String, String> sMarkerIDToSSID = new HashMap<String, String>();

	private LocationManager mLocationManager;
	private String mLocationProvider;

	// needed for LIVE threat map
	private boolean mReceiverRegistered = false;
	private BroadcastReceiver mReceiver;
	private int offset = 0;

	/**
	 * if google play services aren't available an error notification will be displayed
	 *
	 * @return true if the google play services are available
	 */
	private boolean isGooglePlay() {
		int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext());
		boolean result = status == ConnectionResult.SUCCESS;
		if (!result) {
			GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), status, 10).show();
		}
		return result;
	}

	/**
	 * register a broadcast receiver if not already registered
	 * and also update the number of attacks per protocol
	 */
	private void registerBroadcastReceiver() {
		if (!mReceiverRegistered) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (sMap != null) {
						populateMap();
					}
				}
			};

			LocalBroadcastManager
					.getInstance(getActivity()).registerReceiver(mReceiver, new IntentFilter(getString(R.string.broadcast)));
			this.mReceiverRegistered = true;
		}
	}

	/**
	 * callback for when the info window of a marker gets clicked
	 * open the RecordOverviewFragment and display all records belonging to an SSID
	 *
	 * @param marker this info window belongs to
	 */
	@Override
	public void onInfoWindowClick(Marker marker) {
		//MainActivity.getInstance().displayView(MainActivity.MainMenuItem.RECORDS.getValue());
		//RecordOverviewFragment recordOverviewFragment = (RecordOverviewFragment)MainActivity.getInstance().getCurrentFragment();
		//if (recordOverviewFragment != null) {
		String ssid = sMarkerIDToSSID.get(marker.getId());

		ArrayList<String> ssids = new ArrayList<String>();
		ssids.add(ssid);

		LogFilter filter = new LogFilter();
		filter.setESSIDs(ssids);

		RecordOverviewFragment recordOverviewFragment = new RecordOverviewFragment();
		recordOverviewFragment.setFilter(filter);
		recordOverviewFragment.setGroupKey("ESSID");
		recordOverviewFragment.setAllowBack(true);

		MainActivity.getInstance().injectFragment(recordOverviewFragment);

	}

	/**
	 * callbacks from LocationClient
	 */

	@Override
	public void onLocationChanged(Location location) {
		sMap.animateCamera(CameraUpdateFactory.newLatLng(
				new LatLng(location.getLatitude(), location.getLongitude())));
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}


	/**
	 * helper class
	 * easier to use than LatLng
	 */
	private class Point {

		public double x, y;

		public Point(double sx, double sy) {
			x = sx;
			y = sy;
		}
	}

	/**
	 * helper class
	 * contains heuristic to split SSIDs by hostage.location
	 * see MAX_DISTANCE
	 */
	private class SSIDArea {

		private Point mMinimum, mMaximum;

		public int numPoints;

		public static final int MAX_NUM_ATTACKS = 20;

		public static final float MAX_DISTANCE = 1000.0f; // 1km

		public SSIDArea(LatLng initialLocation) {
			//mMinimum = new Point(360.0, 360.0);
			//mMaximum = new Point(-360.0, -360.0);
			mMinimum = new Point(initialLocation.latitude, initialLocation.longitude);
			mMaximum = new Point(initialLocation.latitude, initialLocation.longitude);
			numPoints = 1;
		}

		public boolean doesLocationBelongToArea(LatLng location) {
			LatLng center = calculateCenterLocation();
			float[] result = new float[1];
			Location.distanceBetween(center.latitude, center.longitude, location.latitude,
					location.longitude, result);
			return result[0] < MAX_DISTANCE;
		}

		public void addLocation(LatLng location) {
			Point point = new Point(location.latitude, location.longitude);
			if (point.x < mMinimum.x) {
				mMinimum.x = point.x;
			}
			if (point.x > mMaximum.x) {
				mMaximum.x = point.x;
			}
			if (point.y < mMinimum.y) {
				mMinimum.y = point.y;
			}
			if (point.y > mMaximum.y) {
				mMaximum.y = point.y;
			}
			numPoints++;
		}

		public LatLng calculateCenterLocation() {
			return new LatLng(0.5 * (mMinimum.x + mMaximum.x), 0.5 * (mMinimum.y + mMaximum.y));
		}

		public float calculateRadius() {
			float[] result = new float[1];
			Location.distanceBetween(mMinimum.x, mMinimum.y, mMaximum.x, mMaximum.y, result);
			return 0.5f * result[0];
		}

		public int calculateColor() {
			int threatLevel = numPoints;
			if (threatLevel > MAX_NUM_ATTACKS) {
				threatLevel = MAX_NUM_ATTACKS;
			}
			float alpha = 1.0f - (float) (threatLevel - 1) / (float) (MAX_NUM_ATTACKS - 1);
			return Color.argb(127, (int) (240.0 + 15.0 * alpha), (int) (80.0 + 175.0 * alpha), 60);
		}
	}

	/**
	 * fills the map with markers and circle representing SSIDs
	 * does it asynchronously in background thread
	 */
	private void populateMap() {
		if (mLoader != null) {
			mLoader.interrupt();
		}
		mLoader = new Thread(new Runnable() {
			private void updateUI(final HashMap<String, ArrayList<SSIDArea>> threatAreas) {
				if (mLoader.isInterrupted()) {
					return;
				}

				AppCompatActivity activity = (AppCompatActivity) getActivity();
				if (activity != null) {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							sMap.clear();

							CircleOptions circleOptions = new CircleOptions().radius(200.0)
									.fillColor(Color.argb(127, 240, 80, 60)).strokeWidth(0.0f);
							BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory
									.fromResource(R.drawable.wifi_marker);
							for (Map.Entry<String, ArrayList<SSIDArea>> entry : threatAreas.entrySet()) {
								String ssid = entry.getKey();
								ArrayList<SSIDArea> areas = entry.getValue();

								for (SSIDArea area : areas) {
									int color = area.calculateColor();
									LatLng center = area.calculateCenterLocation();
									float radius = area.calculateRadius();

									sMap.addCircle(circleOptions.center(center).radius(100.0 + radius)
											.fillColor(color));
									Marker marker = sMap.addMarker(new MarkerOptions()
											.title(ssid + ": " + area.numPoints + (area.numPoints == 1
													? getResources()
													.getString(R.string.attack)
													: getResources().getString(R.string.attacks))).position(
													center));
									marker.setIcon(bitmapDescriptor);

									sMarkerIDToSSID.put(marker.getId(), ssid);
								}
							}

						}
					});
				}
			}

			private HashMap<String, ArrayList<SSIDArea>> doInBackground() {
				DaoSession dbSession = HostageApplication.getInstances().getDaoSession();
				DAOHelper daoHelper = new DAOHelper(dbSession);
				ArrayList<RecordAll> records = daoHelper.getAttackRecordDAO().getRecordsForFilter(new LogFilter());

				HashMap<String, ArrayList<SSIDArea>> threatAreas
						= new HashMap<String, ArrayList<SSIDArea>>();

				for (RecordAll record : records) {
					LatLng location = new LatLng(record.getLatitude(), record.getLongitude());
					//Log.i("hostage.location", "lat: " + hostage.location.latitude + " long: " + hostage.location.longitude);
					ArrayList<SSIDArea> areas;
					if (threatAreas.containsKey(record.getSsid())) {
						areas = threatAreas.get(record.getSsid());
						boolean foundArea = false;
						for (SSIDArea area : areas) {
							if (area.doesLocationBelongToArea(location)) {
								area.addLocation(location);
								foundArea = true;
								break;
							}
						}
						if (!foundArea) {
							areas.add(new SSIDArea(location));
						}
					} else {
						areas = new ArrayList<SSIDArea>();
						areas.add(new SSIDArea(location));
						threatAreas.put(record.getSsid(), areas);
					}
				}

				return threatAreas;
			}

			@Override
			public void run() {
				updateUI(doInBackground());
			}
		});

		mLoader.start(); // run!
	}

	/**
	 * performs initialization
	 * checks if google play services are supported
	 * view must be removed if this object has been created once before
	 * that is why view is static
	 *
	 * @param inflater           the inflater
	 * @param container          the container
	 * @param savedInstanceState the savedInstanceState
	 * @return the view
	 */
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		final AppCompatActivity activity = (AppCompatActivity) getActivity();
		if (activity != null) {
			activity.setTitle(getResources().getString(R.string.drawer_threat_map));
		}

		View rootView = inflater.inflate(R.layout.fragment_threatmap, container, false);


		if (rootView != null) {
			ViewGroup parent = (ViewGroup) rootView.getParent();
			if (parent != null) {
				parent.removeView(rootView);
			}
		}

		try {
			if (isGooglePlay()) {

				if(rootView !=null)
					mapView  = rootView
							.findViewById(R.id.threatmapfragment);
					if (mapView != null) {
						mapView.onCreate(savedInstanceState);
						mapView.getMapAsync(this);
						mapView.onResume();

					}
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance());
				builder.setMessage(Html.fromHtml(getString(R.string.google_play_services_unavailable)))
						.setCancelable(false)
						.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// :D-|< :D-/< :D-\<
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			}
		} catch (InflateException e) {
			// map already exists
			e.printStackTrace();
		}

		if (sMap != null) {
			sMap.setOnInfoWindowClickListener(this);
			// custom info window layout
			sMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
				@Override
				public View getInfoWindow(Marker marker) {
					return null;
				}

				@Override
				public View getInfoContents(Marker marker) {
					View view = inflater.inflate(R.layout.fragment_threatmap_infowindow, null);
					if (view != null) {
						TextView titleTextView = view
								.findViewById(R.id.threatmap_infowindow_title);
						if (titleTextView != null) {
							titleTextView.setText(marker.getTitle());
						}
					}
					return view;
				}
			});

			mLocationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
			mLocationProvider = mLocationManager.getBestProvider(criteria, false);

			requestPermissionUpdates();

			sMap.setMyLocationEnabled(true);

			LatLng tudarmstadt = new LatLng(49.86923, 8.6632768); // default hostage.location
			sMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tudarmstadt, 13));

			populateMap();

			registerBroadcastReceiver();
		}

		// tell the user to enable wifi so map data can be streamed
		if (activity != null && !HelperUtils.isNetworkAvailable(activity)) {
			new AlertDialog.Builder(activity)
					.setTitle(R.string.information)
					.setMessage(R.string.no_network_connection_threatmap_msg)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
													int which) {
								}
							}
					)
					.setIcon(android.R.drawable.ic_dialog_info).show();
		}

		return rootView;
	}


	private void requestPermissionUpdates(){
		if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
				Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

			ActivityCompat.requestPermissions(MainActivity.getInstance(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);

			return;
		}
		mLocationManager.requestLocationUpdates(mLocationProvider, 0, 1000.0f, this);

	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		sMap = googleMap;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (sMap != null) {
			// repopulate
			populateMap();
		}
		if (mLocationManager != null) {
			if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
					!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
					!= PackageManager.PERMISSION_GRANTED) {

				ActivityCompat.requestPermissions(MainActivity.getInstance(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);

				return;
			}
			mLocationManager.requestLocationUpdates(mLocationProvider, 0, 1000.0f, this);
		}
	}

	/**
	 * Callback for requestPermission method. Creates an AlertDialog for the user in order to allow the permissions or not.
	 * @param requestCode
	 * @param permissions
	 * @param grantResults
	 */

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == 10) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

			} else {
				if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.getInstance(), Manifest.permission.ACCESS_FINE_LOCATION)) {
					androidx.appcompat.app.AlertDialog.Builder dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext());
					dialog.setTitle("Permission Required");
					dialog.setCancelable(false);
					dialog.setMessage("You have to Allow permission to access user location");
					dialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package",
									getContext().getPackageName(), null));
						}
					});
					androidx.appcompat.app.AlertDialog alertDialog = dialog.create();
					alertDialog.show();
				}
				//Code for deny if needed
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(this);
		}
	}
}
