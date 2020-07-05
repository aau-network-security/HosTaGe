package de.tudarmstadt.informatik.hostage.ui.adapter;

import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import de.tudarmstadt.informatik.hostage.Listener;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.model.Profile;
import de.tudarmstadt.informatik.hostage.persistence.ProfileManager;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.ui.model.ServicesListItem;


/**
 * @author Daniel Lazar
 * @created 06.02.14.
 * a list adapter for loading switches and service information asynchronously
 */
public class ServicesListAdapter extends ArrayAdapter<ServicesListItem> {
    private final Context context;
    private final List<ServicesListItem> values;
    private Context mActivity;
    private Switch mServicesSwitch;
    private CompoundButton.OnCheckedChangeListener mListener;
    private Profile mProfile;
    private Integer[] mGhostPorts;

    /**
     * constructor
     *
     * @param context Context of the current activity
     * @param objects List of ServicesListItem which contains all the protocols
     */
    public ServicesListAdapter(Context context, List<ServicesListItem> objects) {
        super(context, R.layout.services_list_item, objects);

        this.context = context;
        this.values = objects;
    }

    /**
     * method to save important information from parent fragment
     *
     * @param activity       activicty from parent fragment
     * @param servicesSwitch the switch from parent fragment
     * @param mainListener   Listener from parent fragment
     */
    public void setActivity(Context activity, Switch servicesSwitch, CompoundButton.OnCheckedChangeListener mainListener) {
        mActivity = activity;
        mServicesSwitch = servicesSwitch;
        mListener = mainListener;
    }

    /**
     * main method of ServicesListAdapter which initializes the holder if null
     * Also activates protocols and switches
     *
     * @param position    current position in list
     * @param convertView convert view
     * @param parent the parent view group
     * @return
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView;

        ViewHolder holder;

        final ServicesListItem item = values.get(position);

        if (rowView == null) {
            rowView = inflater.inflate(R.layout.services_list_item, parent, false);

            holder = new ViewHolder();
            assert rowView != null;
            holder.protocolName = rowView.findViewById(R.id.services_item_name);
            holder.recordedAttacks = rowView.findViewById(R.id.services_item_rec_attacks);
            holder.port = rowView.findViewById(R.id.services_item_port);
            holder.activated = rowView.findViewById(R.id.services_item_switch);
            holder.circle = rowView.findViewById(R.id.services_circle);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        holder.protocolName.setText(item.protocol);
        setRealPortListening(holder,item);
        holder.activated.setTag(item);

        try {
            this.updateStatus(item, holder);
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.activated.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        ServicesListItem item = (ServicesListItem) buttonView.getTag();
                        try {
                            mProfile = ProfileManager.getInstance().getCurrentActivatedProfile();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //SK: Temp bugfix
                        //if (!HelperUtils.isNetworkAvailable(mActivity)) {
                        if (isChecked && !HelperUtils.isWifiConnected(mActivity)) {
                            if(!MainActivity.getInstance().getHostageService().hasRunningListeners()) {
                                new AlertDialog.Builder(mActivity)
                                        .setTitle(R.string.information)
                                        .setMessage(R.string.wifi_not_connected_msg)
                                        .setPositiveButton(android.R.string.ok,
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog,
                                                                        int which) {
                                                    }
                                                }
                                        )
                                        .setIcon(android.R.drawable.ic_dialog_info).show();
                                if (buttonView.isChecked()) {
                                    buttonView.setChecked(false);
                                }
                            }
                        } else {
                            //check if switch is set to ON and start the concrete listener for the protocol
                            if (isChecked) {
                                if(item.protocol.equals("GHOST")){
                                    if(mProfile.mGhostActive){
                                        mGhostPorts = mProfile.getGhostPorts();

                                        if(mGhostPorts.length != 0) {
                                            for(Integer port: mGhostPorts){
                                                if(!MainActivity.getInstance().getHostageService().isRunning(item.protocol, port)) {
                                                    MainActivity.getInstance().getHostageService().startListener(item.protocol, port);
                                                }
                                            }
                                            //set the main switch to null, so that he won't react and starts all protocols
                                            mServicesSwitch.setOnCheckedChangeListener(null);
                                            mServicesSwitch.setChecked(true);
                                            mServicesSwitch.setOnCheckedChangeListener(mListener);

                                            if(!buttonView.isChecked()) {
                                                buttonView.setChecked(true);
                                            }
                                        }
                                    }
                                    else {
                                        if(buttonView.isChecked()) {
                                            buttonView.setChecked(false);
                                        }
                                    }
                                }
                                else if (!MainActivity.getInstance().getHostageService().isRunning(item.protocol)) {
                                    boolean success = MainActivity.getInstance().getHostageService().startListener(item.protocol);

									if (success) {
										//set the main switch to null, so that he won't react and starts all protocols
										mServicesSwitch.setOnCheckedChangeListener(null);
										mServicesSwitch.setChecked(true);
										mServicesSwitch.setOnCheckedChangeListener(mListener);
										if (!buttonView.isChecked()) {
											buttonView.setChecked(true);
										}
									} else {
										buttonView.setChecked(false);
									}
                                } else {
                                    if(!buttonView.isChecked()) {
                                        buttonView.setChecked(true);
                                    }
                                }
                            } else {
                                   if(item.protocol.equals("GHOST")) {
                                       mGhostPorts = mProfile.getGhostPorts();
                                       for(Integer port: mGhostPorts){
                                           if(port != null) {
                                               if(MainActivity.getInstance().getHostageService().isRunning("GHOST",port)){
                                                   MainActivity.getInstance().getHostageService().stopListener("GHOST", port);
                                               }
                                           }
                                       }
                                       if(buttonView.isChecked()) {
                                           buttonView.setChecked(false);
                                       }
                                   }
                                   else if (MainActivity.getInstance().getHostageService().isRunning(item.protocol)) {
                                        MainActivity.getInstance().getHostageService().stopListener(item.protocol);
                                    }
                                    if(buttonView.isChecked()) {
                                        buttonView.setChecked(false);
                                    }
                            }
                        }
                    }
                }
        );
        return rowView;
    }

    /**
     * method to update the current status, which includes changing the attack indication circle and the number of attacks
     *
     * @param item   ServiceListItem which has information about current item, e.g. protocol, activated, attacks
     * @param holder ViewHolder which represents the item in the View
     */
    private void updateStatus(ServicesListItem item, ViewHolder holder) throws Exception {
		boolean serviceIsActive = false;
		// determine if service is active
        if(item.protocol.equals("GHOST")) {
			mProfile = ProfileManager.getInstance().getCurrentActivatedProfile();
			mGhostPorts = mProfile.getGhostPorts();

			for (Integer port : mGhostPorts) {
				if (port != null && MainActivity.getInstance().getHostageService()
						.isRunning("GHOST", port)) {
					serviceIsActive = true;
					break;
				}
			}
		} else if (MainActivity.getInstance().getHostageService().isRunning(item.protocol)) {
			serviceIsActive = true;
		}

		if (serviceIsActive){
			if(!holder.activated.isChecked()) {
				holder.activated.setChecked(true);
			}

			if (item.attacks == 0) {
				setBackground(holder, R.drawable.services_circle_green);
			} else { // attacks > 0 (will never be negative)
				if (MainActivity.getInstance().getHostageService().hasProtocolActiveAttacks(item.protocol)) {
					setBackground(holder, R.drawable.services_circle_red);
				} else {
					setBackground(holder, R.drawable.services_circle_yellow);
				}
			}
		} else {
			if(holder.activated.isChecked()) {
				holder.activated.setChecked(false);
			}

			if (item.attacks > 0) {
				setBackground(holder, R.drawable.services_circle_yellow);
			} else {
				setBackground(holder, R.drawable.services_circle);
			}
		}

        holder.recordedAttacks
                .setText(String.format(MainActivity.getContext().getResources().getString(R.string.recorded_attacks) + "  %d", Integer.valueOf(item.attacks)));
    }

    /**
     * changes the indicator circle of a service
     *
     * @param holder   ViewHolder which represents the item in the View
     * @param drawable int which represents the ID of the drawable we want to display, e.g. on a present attack it should be R.drawable.services_circle_red
     */
	private void setBackground(ViewHolder holder, int drawable) {

	    holder.circle.setBackground(MainActivity.getInstance().getResources().getDrawable(drawable));

    }

    /**
     * Adds the real port number in every item of the list
     */
    private void setRealPortListening(ViewHolder holder,ServicesListItem item){
        Map<String,Integer> ports = Listener.getRealPorts();
        String protocol = item.protocol;
        if(ports.containsKey(protocol)){
            int realPort = ports.entrySet().stream()
                    .filter(e -> e.getKey().equals(protocol))
                    .map(Map.Entry::getValue)
                    .findFirst().get();

            holder.port.setText(String.format(MainActivity.getContext().getResources().getString(R.string.open_ports) + "  %d", realPort));
        }else {
            holder.port.setText(String.format(MainActivity.getContext().getResources().getString(R.string.open_ports) + "  %d", item.port));
        }
    }

    /**
     * ViewHolder stands for a row in the view
     */
    private class ViewHolder {

        public TextView protocolName;

        public TextView recordedAttacks;

        public TextView port;

        public Switch activated;

        public View circle;
    }
}
