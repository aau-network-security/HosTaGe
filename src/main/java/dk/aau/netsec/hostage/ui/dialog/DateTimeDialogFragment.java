package dk.aau.netsec.hostage.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import java.util.Calendar;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.ui.activity.MainActivity;

/**
 * Created by Julien on 16.02.14.
 */
@SuppressLint("ValidFragment")
public class DateTimeDialogFragment extends DialogFragment implements OnDateChangedListener, OnTimeChangedListener {
    // Define constants for date-time picker.
    public final static int DATE_PICKER = 1;
    public final static int TIME_PICKER = 2;
    public final static int DATE_TIME_PICKER = 3;

    // DatePicker reference
    public DatePicker datePicker;

    // TimePicker reference
    public TimePicker timePicker;

    // Calendar reference
    private Calendar mCalendar;

    // Define activity
    private Activity activity;

    // Define Dialog type
    private int DialogType;

    // Define Dialog view
    private View mView;

    // Constructor start
    public DateTimeDialogFragment(Activity activity) {
        this(activity, DATE_TIME_PICKER);
    }

    /**
     * Constructor
     * @param activity the activity
     * @param DialogType, what kind of dialog it is (TIME_PICKER, DATE_PICKER, DATE_TIME_PICKER)
     */
    public DateTimeDialogFragment(Activity activity, int DialogType) {

        this.activity = activity;
        this.DialogType = DialogType;

        LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.date_time_dialog, null);

        this.setupRootView(mView);

    }


    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        LayoutInflater inflater = LayoutInflater.from(this.activity);

        ViewGroup container = (ViewGroup) this.mView.getParent();

        container.removeView(this.mView);

        mView = inflater.inflate(R.layout.date_time_dialog, null);
        container.addView(mView);
        this.setupRootView(mView);
    }

    /**
     * Configure the root view in here.
     * @param mView, root view
     */
    private void setupRootView(View mView){

        mCalendar = Calendar.getInstance();

        datePicker = mView.findViewById(R.id.DatePicker);
        datePicker.init(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH), this);


        timePicker = mView.findViewById(R.id.TimePicker);

        setIs24HourView(true);
        setCalendarViewShown(false);

        switch (DialogType) {
            case DATE_PICKER:
                timePicker.setVisibility(View.GONE);
                break;
            case TIME_PICKER:
                datePicker.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Set the current date.
     * @param timeInMillis, date in milliseconds
     */
    public void setDate(long timeInMillis){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis (timeInMillis);

        int year    = calendar.get(Calendar.YEAR) ;
        int month   = calendar.get(Calendar.MONTH);
        int day     = calendar.get(Calendar.DATE);
        int hour    = calendar.get(Calendar.HOUR);
        int min     = calendar.get(Calendar.MINUTE);

        datePicker.updateDate(year, month, day);
        timePicker.setCurrentHour(hour);
        timePicker.setCurrentMinute(min);
    }

    /**
     * Returns the current selected date.
     * @return long, date in milliseconds
     */
   public long getDate(){
	   
       int day = datePicker.getDayOfMonth();
       int month = datePicker.getMonth();
       int year =  datePicker.getYear();
       
       int hourOfDay = timePicker.getCurrentHour();
       int minute = timePicker.getCurrentMinute();

       Calendar calendar = Calendar.getInstance();

       calendar.set(year, month, day, hourOfDay, minute);

       return calendar.getTime().getTime();
   }

    /**
     * The listener which will be called if the user tapped "cancel" or "ok"
     */
    public interface DateTimeDialogFragmentListener {
        /**
         * Called if the user tapped "ok"
         * @param dialog {@link DateTimeDialogFragment DateTimeDialogFragment}
         */
        void onDateTimePickerPositiveClick(DateTimeDialogFragment dialog);

        /**
         * Called if the user tapped "cancel"
         * @param dialog {@link DateTimeDialogFragment DateTimeDialogFragment}
         */
        void onDateTimePickerNegativeClick(DateTimeDialogFragment dialog);
    }
    private DateTimeDialogFragmentListener mListener;

    /**
     * Set the user interaction listener.
     * @param listener DateTimeDialogFragmentListener
     */
    public void setDateChangeListener(DateTimeDialogFragmentListener listener){
        this.mListener = listener;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            if (this.mListener == null){
                if (activity.getClass().equals(MainActivity.class)){
                    mListener = (DateTimeDialogFragmentListener) (((MainActivity)activity).getDisplayedFragment());
                } else {
                    mListener = (DateTimeDialogFragmentListener) activity;
                }
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DateTimeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Builder builder = new AlertDialog.Builder(activity);

        builder.setView(mView);

        builder.setMessage(activity.getString(R.string.rec_set_date))
                .setPositiveButton(activity.getString(R.string.rec_set),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								mListener
										.onDateTimePickerPositiveClick(DateTimeDialogFragment.this);
							}
						})
                .setNegativeButton(activity.getString(R.string.rec_cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								mListener
										.onDateTimePickerNegativeClick(DateTimeDialogFragment.this);
								DateTimeDialogFragment.this.getDialog().cancel();
							}
						}); 

        return builder.create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        timePicker.setOnTimeChangedListener(this);
    }

    /**
     * Returns the value of the given field after computing the field values by calling complete() first.
     * @param field int
     * @return int
     */
    public int get(final int field) {
        return mCalendar.get(field);
    }

    /**
     * Set the time picker style.
     * @param is24HourView
     */
    public void setIs24HourView(boolean is24HourView) {
        timePicker.setIs24HourView(is24HourView);
    }

    /**
     * Show / hide the calendar view of the DatePicker.
     * @param calendarView boolean
     */
    public void setCalendarViewShown(boolean calendarView) {
        datePicker.setCalendarViewShown(calendarView);
    }



    /**
     * Handles date change event.
     * @param view DatePicker
     * @param year changed year
     * @param monthOfYear changed month of Year
     * @param dayOfMonth changed day of Month
     */
    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        mCalendar.set(year, monthOfYear, dayOfMonth, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE));
    }

    /**
     * Handles on time changed events.
     * @param view TimePicker
     * @param hourOfDay changed hour
     * @param minute changed minute
     */
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        mCalendar.set(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH), hourOfDay, minute);
    }

    // MAYBE NEED IN FUTURE DEVELOPMENT
    // UNUSED
//    public long getDateTimeMillis() {
//        return mCalendar.getTimeInMillis();
//    }
//    public boolean CalendarViewShown() {
//        return datePicker.getCalendarViewShown();
//    }
//    public boolean is24HourView() {
//        return timePicker.is24HourView();
//    }
//    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
//        datePicker.updateDate(year, monthOfYear, dayOfMonth);
//    }
//
//    public void updateTime(int currentHour, int currentMinute) {
//        timePicker.setCurrentHour(currentHour);
//        timePicker.setCurrentMinute(currentMinute);
//    }
//
//    public String getDateTime() {
//        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(activity);
//        return dateFormat.format(mCalendar.getTime());
//    }
}