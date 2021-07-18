package dk.aau.netsec.hostage.ui.dialog;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.ui.fragment.RecordOverviewFragment;

/**
 * TODO write javadoc
 *
 * @author Filip Adamik
 * Created on 18/07/2021
 */
public class DateTimePickerDialog {

    /**
     * TODO write javadoc
     *
     * @return
     */
    public static void showDateTimePicker(DateTimeSelected callback, Context context, boolean filterFrom) {
        Calendar date;

        final Calendar currentDate = Calendar.getInstance();
        date = Calendar.getInstance();
        new DatePickerDialog(context, R.style.CustomDateTimePicker, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                date.set(year, monthOfYear, dayOfMonth);
                new TimePickerDialog(context, R.style.CustomDateTimePicker, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        date.set(Calendar.MINUTE, minute);

                        callback.dateTimeSelected(date, filterFrom);
                    }
                }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), true).show();
            }
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();

    }

    public interface DateTimeSelected {
        void dateTimeSelected(Calendar date, boolean filterFrom);
    }
}
