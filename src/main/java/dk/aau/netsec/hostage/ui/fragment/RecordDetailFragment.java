package dk.aau.netsec.hostage.ui.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import dk.aau.netsec.hostage.HostageApplication;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.logging.DaoSession;
import dk.aau.netsec.hostage.logging.MessageRecord;
import dk.aau.netsec.hostage.logging.RecordAll;
import dk.aau.netsec.hostage.persistence.DAO.DAOHelper;
import dk.aau.netsec.hostage.ui.activity.MainActivity;


/**
 * Displays detailed informations about an record.
 *
 * @author Fabio Arnold
 * @author Alexander Brakowski
 * @author Julien Clauter
 */

/**
 * Created by Shreyas Srinivasa on 01.10.15.
 */
public class RecordDetailFragment extends UpNavigatibleFragment {
    /**
     * Hold the record of which the detail informations should be shown
     */
    private RecordAll mRecord;
    /**
     * The database helper to retrieve data from the database
     */
    private DaoSession dbSession;
    private DAOHelper daoHelper;
    /**
     * The layout inflater
     */
    private LayoutInflater mInflater;
    /*
     * References to the views in the layout
     */
    private View mRootView;
    private LinearLayout mRecordOverviewConversation;
    private TextView mRecordDetailsTextAttackType;
    private TextView mRecordDetailsTextSsid;
    private TextView mRecordDetailsTextBssid;
    private TextView mRecordDetailsTextRemoteip;
    private TextView mRecordDetailsTextProtocol;
    private Button textButton;
    private Button hexButton;
    private LayoutInflater inflater;
    private ViewGroup container;
    private Bundle savedInstanceState;
    public SharedPreferences pref;
    public int port;

    /**
     * Sets the record of which the details should be displayed
     *
     * @param rec the record to be used
     */
    public void setRecord(RecordAll rec) {
        this.mRecord = rec;
    }

    /**
     * Retrieves the record which is used for the display of the detail information
     *
     * @return the record
     */
    public RecordAll getRecord() {
        return this.mRecord;
    }

    /**
     * Retrieves the id of the layout
     *
     * @return the id of the layout
     */
    public int getLayoutId() {
        return R.layout.fragment_record_detail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        pref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;
        this.container = container;
        this.savedInstanceState = savedInstanceState;

        mInflater = inflater;
        if (mRecord != null)
            getActivity().setTitle(mRecord.getSsid());

        dbSession = HostageApplication.getInstances().getDaoSession();
        daoHelper = new DAOHelper(dbSession, getActivity());

        this.mRootView = inflater.inflate(this.getLayoutId(), container, false);
        this.assignViews(mRootView);
        this.configurateRootView(mRootView);

        return mRootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
        super.onStart();

    }

    /**
     * Retrieves all the views from the given view
     *
     * @param view the layout view
     */
    private void assignViews(View view) {
        mRecordOverviewConversation = view.findViewById(R.id.record_overview_conversation);
        mRecordDetailsTextAttackType = view.findViewById(R.id.record_details_text_attack_type);
        mRecordDetailsTextSsid = view.findViewById(R.id.record_details_text_ssid);
        mRecordDetailsTextBssid = view.findViewById(R.id.record_details_text_bssid);
        mRecordDetailsTextRemoteip = view.findViewById(R.id.record_details_text_remoteip);
        mRecordDetailsTextProtocol = view.findViewById(R.id.record_details_text_protocol);
    }


    /**
     * Configures the given view and fills it with the detail information
     *
     * @param rootView the view to use to display the information
     */
    private void configurateRootView(View rootView) {
        mRecordDetailsTextAttackType.setText(mRecord.getWasInternalAttack() ? R.string.RecordInternalAttack : R.string.RecordExternalAttack);
        mRecordDetailsTextBssid.setText(mRecord.getBssid());
        mRecordDetailsTextSsid.setText(mRecord.getSsid());
        if (mRecord.getRemoteIP() != null)
            mRecordDetailsTextRemoteip.setText(mRecord.getRemoteIP() + ":" + mRecord.getRemotePort());

        mRecordDetailsTextProtocol.setText(mRecord.getProtocol());

        ArrayList<RecordAll> conversation = this.daoHelper.getAttackRecordDAO().getConversationForAttackID(mRecord.getAttack_id());

        // display the conversation of the attack
        for (RecordAll r : conversation) {
            View row;

            String from = r.getLocalIP() == null ? "-" : r.getLocalIP() + ":" + r.getLocalPort();
            String to = r.getRemoteIP() == null ? "-" : r.getRemoteIP() + ":" + r.getRemotePort();

            if (r.getStringMessageType() != null && r.getStringMessageType().equals(MessageRecord.TYPE.SEND.name())) {
                row = mInflater.inflate(R.layout.fragment_record_conversation_sent, null);
                textButton = row.findViewById(R.id.text_id_sent);
                hexButton = row.findViewById(R.id.hex_id_sent);
            } else {
                row = mInflater.inflate(R.layout.fragment_record_conversation_received, null);
                textButton = row.findViewById(R.id.text_id_received);
                hexButton = row.findViewById(R.id.hex_id_received);

                String tmp = from;
                from = to;
                to = tmp;
            }

            addConversationText(row, from, to, r);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addConversationText(View row, String from, String to, RecordAll r) {
        TextView conversationInfo = row.findViewById(R.id.record_conversation_info);
        TextView conversationContent = row.findViewById(R.id.record_conversation_content);


        conversationContent.setOnTouchListener((v, motionEvent) -> {
            if (v.getId() == R.id.record_conversation_content) {
                if (v.canScrollVertically(1) || v.canScrollVertically(-1)) { // if the view is scrollable
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    if ((motionEvent.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
            }
            return false;
        });

        changeHexToText(textButton, r, conversationContent);
        changeTextToHex(hexButton, r, conversationContent);

        Date date = new Date(r.getTimestamp());
        conversationInfo.setText(String.format(getString(R.string.record_details_info), from, to, getDateAsString(date), getTimeAsString(date)));
        if (r.getPacket() != null)
            conversationContent.setText(r.getPacket());

        mRecordOverviewConversation.addView(row);
    }

    /**
     * Click button listener that changes the packet content from hex to text form
     *
     * @param button              text button
     * @param record              record containing the packet
     * @param conversationContent the conversion content view
     */
    private void changeHexToText(Button button, RecordAll record, TextView conversationContent) {
        button.setOnClickListener(view -> {
            conversationContent.setText(record.convertPacketFromHex(record.getPacket()));
        });
    }

    /**
     * Click button listener that changes packet content from text to hex form
     *
     * @param button              hex button
     * @param record              record containing the packet
     * @param conversationContent the conversion content view
     */
    private void changeTextToHex(Button button, RecordAll record, TextView conversationContent) {
        button.setOnClickListener(view -> {
            conversationContent.setText(record.convertPacketFromText(record.getPacket()));
        });
    }

    private void deleteDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(R.string.record_details_confirm_delete)
                .setPositiveButton(R.string.yes,
                        (dialog, which) -> {
                            daoHelper.getAttackRecordDAO().deleteByAttackID(mRecord.getAttack_id());
                            MainActivity.getInstance().navigateBack();
                        }
                ).setNegativeButton(R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert).show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.records_detail_actions, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.records_detail_discard) {
            deleteDialog();

            return true;
        }
//		if (item.getItemId() == R.id.bro_sig) {
//
//            //TODO Disabled for PlayStoreRelease
//			AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
//			builder.setTitle(MainActivity.getInstance().getString(R.string.bro_signature));
//			builder.setMessage(MainActivity.getInstance().getString(R.string.bro_message));
//
//			builder.setPositiveButton(R.string.generate,
//					(dialog, which) -> {
//						try {
//							getConversation();
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//						MainActivity.getInstance().navigateBack();
//					}
//			).setNegativeButton(R.string.cancel, null);
//
//			builder.create();
//			builder.show();
//
//			return true;
//		}
        return false;
    }


    public int protocol2Port(String protocol) {
        if (protocol.contains("HTTP")) {
            port = 80;
        } else if (protocol.contains("MODBUS")) {
            port = 502;
        } else if (protocol.contains("TELNET")) {
            port = 23;
        } else if (protocol.contains("SMB")) {
            port = 1025;
        } else if (protocol.contains("MQTT")) {
            port = 1883;
        } else if (protocol.contains("HTTPS")) {
            port = 443;
        } else if (protocol.contains("ECHO")) {
            port = 7;
        } else if (protocol.contains("FTP")) {
            port = 21;
        } else if (protocol.contains("MySQL")) {
            port = 3306;
        } else if (protocol.contains("S7COMM")) {
            port = 102;
        } else if (protocol.contains("SIP")) {
            port = 1025;
        } else if (protocol.contains("SMTP")) {
            port = 25;
        } else if (protocol.contains("SNMP")) {
            port = 161;
        } else if (protocol.contains("SSH")) {
            port = 22;
        } else if (protocol.contains("PORTSCAN")) {
            port = 0;
        } else if (protocol.contains("FILE INJECTION")) {
            port = 1025;
        }
        return port;
    }

    //Signature generation
    private void getConversation() throws IOException {
        ArrayList<RecordAll> conversation = this.daoHelper.getAttackRecordDAO().getConversationForAttackID(mRecord.getAttack_id());
        for (RecordAll r : conversation) {

            String mydata = r.getPacket();
            ArrayList<String> myTokensList = new ArrayList<String>();
            String[] tokens = mydata.split("\n");
            for (String tok : tokens) {
                if (tok.contains("Protocol:")) {
                    myTokensList.add(tok.split(":")[1]);
                }
            }

            ArrayList<Integer> myPortList = new ArrayList<Integer>();
            for (String tok : myTokensList) {
                if (tok.contentEquals("PORTSCAN")) {

                }
                myPortList.add(protocol2Port(tok));
            }
            System.out.print(myPortList);

            // Generates a Bro signature policy for Multistage attack
            if (mRecord.getProtocol().contentEquals("MULTISTAGE")) {

                String signature = createMultistageSignature(r.getRemoteIP(), myPortList); // generates signature
                createPolicyFile(signature, mRecord.getProtocol()); // creates a signature file
            } else if (mRecord.getProtocol().contentEquals("HTTP")) {
                String protocol = r.getProtocol();
                String signature = createSignature(r.getPacket(), protocol);
                createSignatureFile(signature, protocol);
            } else if (mRecord.getProtocol().contentEquals("S7COMM")) {
                String protocol = r.getProtocol();
                String signature = createSignature(r.getPacket(), protocol);
                createSignatureFile(signature, protocol);
            } else if (mRecord.getProtocol().contentEquals("MODBUS")) {
                String protocol = r.getProtocol();
                String signature = createSignature(r.getPacket(), protocol);
                createSignatureFile(signature, protocol);
            } else if (mRecord.getProtocol().contentEquals("SMB")) {
                String protocol = r.getProtocol();
                String signature = createSignature(r.getPacket(), protocol);
                createSignatureFile(signature, protocol);
            }
        }

    }

    private String createSignature(String packet, String protocol) {

        int port = protocol2Port(protocol);
        String sigPort = String.valueOf(port);
        String sigmatch = packet;
        String modbusSignature = "signature-" + protocol + "-sig {\n" +
                "    ip-proto == tcp\n" +
                "    dst-port ==" + sigPort + "\n" +
                "    payload /" + sigmatch + "/" + "\n" +
                "    event \"" + protocol + " Attack!!\"\n" +
                "}";

        return modbusSignature;
    }


    private String createMultistageSignature(String ip, ArrayList portList) {

        int portListSize = 0;
        StringBuilder portArray = new StringBuilder();

        for (Object tok : portList) {

            portArray.append(tok + "/tcp");
            portListSize++;
            if (portListSize != portList.size()) {
                portArray.append(",");
            }

        }

        String MultiStageSignature = "@load base/frameworks/notice\n" +
                "\n" +
                "\n" +
                "\n" +
                "export{\n" +
                "\tredef enum Notice::Type += {\n" +
                "\t\tMultistage\n" +
                "\t};\n" +
                "}\n" +
                "global attack_ip =" + ip + ";\n" +
                "global attack_port = set(" + portArray + ");\n" +
                "global attack_count = 0;\n" +
                "\n" +
                "\n" +
                "\n" +
                "event connection_established(c: connection)\n" +
                "{\n" +
                "\n" +
                "print fmt (\"Initiating.............\");\n" +
                "print c$id$orig_h;\n" +
                "print c$id$resp_p;\n" +
                "\n" +
                "for (i in attack_port){\n" +
                "\n" +
                "\tif(count==0){\n" +
                "\t\t\n" +
                "\tif ((c$id$orig_h==attack_ip) && (c$id$resp_p==attack_prot1))\n" +
                "        {\n" +
                "\tprint fmt(\"Inside the loop\");\n" +
                "        ++attack_count;\n" +
                "\tprint attack_count;\n" +
                "       }\n" +
                "\n" +
                "\telse{break;}\n" +
                "\t}\n" +
                "\n" +
                "  \n" +
                "\n" +
                "}\n" +
                "\n" +
                " else {\n" +
                "\n" +
                "        if ((c$id$orig_h==attack_ip) && (c$id$resp_p == attack_prot2)){\n" +
                "\t\n" +
                "\tprint fmt (\"MULTISTAGE ATTACK!!!\");\n" +
                "        NOTICE([$note = Multistage,\n" +
                "                $conn = c,\n" +
                "                $msg = fmt(\"Multistage Attack! from %s\",c$id$orig_h)]);\n" +
                "\tattack_count = 0;\n" +
                "\t\n" +
                "        }\n" +
                "\n" +
                " }\n" +
                "\n" +
                "\n" +
                "}\n";

        return MultiStageSignature;
    }

    private void createPolicyFile(String signature, String protocol) throws IOException {
        FileOutputStream sig;
        long tsLong = System.currentTimeMillis() / 1000;
        String ts = Long.toString(tsLong);
        String fileName = protocol + "Bro_Policy" + ts + ".bro";
//		TODO adjust this since storage setting has been removed
        String externalLocation = pref.getString("pref_external_location", "");
        String root = Environment.getExternalStorageDirectory().toString();


        if (root != null && isExternalStorageWritable()) {
            File dir = new File(root + externalLocation);
            dir.mkdirs();
            File file = new File(dir, fileName);
            sig = new FileOutputStream(file);
            sig.write(signature.getBytes());
            sig.write(System.getProperty("line.separator").getBytes());
            sig.flush();
            sig.close();
            Snackbar.make(mRootView, "Policy file:" + fileName + "created", Snackbar.LENGTH_LONG).show();

        } else {
//TODO extract string
            Snackbar.make(mRootView, "Could not write to SD Card", Snackbar.LENGTH_SHORT).show();
            return;
        }

    }

    //write to file and store in SD Card
    private void createSignatureFile(String signature, String protocol) throws IOException {
        FileOutputStream sig;
        Long tsLong = System.currentTimeMillis() / 1000;
        String ts = tsLong.toString();
        String fileName = protocol + "Bro_Sig" + ts + ".sig";
//		TODO adjust this, since storage setting has been removed
        String externalLocation = pref.getString("pref_external_location", "");
        String root = Environment.getExternalStorageDirectory().toString();

        if (root != null && isExternalStorageWritable()) {
            File dir = new File(root + externalLocation);
            dir.mkdirs();
            File file = new File(dir, fileName);
            sig = new FileOutputStream(file);
            sig.write(signature.getBytes());
            sig.write(System.getProperty("line.separator").getBytes());
            sig.flush();
            sig.close();
            Snackbar.make(mRootView, "Signature file:" + fileName + "created", Snackbar.LENGTH_LONG).show();

        } else {
//            TODO extract string
            Snackbar.make(mRootView, "Could not write to SD Card", Snackbar.LENGTH_SHORT).show();
            return;
        }

    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(state);
    }


    /**
     * Converts the given data to an localized string
     *
     * @param date the date to convert
     * @return the converted date as an string
     */
    private String getDateAsString(Date date) {
        return DateFormat.getDateFormat(getActivity()).format(date);
    }

    /**
     * Converts the given date to an localized time
     *
     * @param date the date to convert
     * @return the converted time as an string
     */
    private String getTimeAsString(Date date) {
        return DateFormat.getTimeFormat(getActivity()).format(date);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mRootView != null) {
            unbindDrawables(mRootView);
            mRootView = null;
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
