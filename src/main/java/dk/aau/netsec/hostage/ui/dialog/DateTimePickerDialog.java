package dk.aau.netsec.hostage.ui.dialog;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.annotation.NonNull;

import java.util.Calendar;

import dk.aau.netsec.hostage.R;

/**
 * This class contains a single static method to display date and time picker.
 * <p>
 * It also defines a callback interface to pass the value back to the filter when the user selects
 * date and time.
 *
 * @author Filip Adamik
 * Created on 18/07/2021
 */
public class DateTimePickerDialog {

    /**
     * Display data and time picker flow. First, a date picker is displayed. After the user selects
     * a date and confirms, a time picker is displayed. The selected date and time is passed to the
     * implementation of the {@link DateTimeSelected} interface, which is passed as a method
     * argument.
     * <p>
     * The selected date and time is only passed on if the user selected both date and time. If the
     * user cancels the operation before selecting a time, nothing is passed on.
     *
     * @param context    Application context
     * @param filterFrom A flag that is passed back in the callback, indicating whether this was
     *                   triggered from the <i>before</i> or <i>after</i> filter.
     * @param callback   The interface implementation where the date and time value is passed.
     */
    public static void showDateTimePicker(@NonNull Context context, boolean filterFrom, @NonNull DateTimeSelected callback) {
        Calendar date;

        final Calendar currentDate = Calendar.getInstance();
        date = Calendar.getInstance();
        new DatePickerDialog(context, R.style.CustomDateTimePicker, (view, year, monthOfYear, dayOfMonth) -> {
            date.set(year, monthOfYear, dayOfMonth);
            new TimePickerDialog(context, R.style.CustomDateTimePicker, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    date.set(Calendar.MINUTE, minute);

                    callback.dateTimeSelected(date, filterFrom);
                }
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), true).show();
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();

    }

    public interface DateTimeSelected {
        void dateTimeSelected(@NonNull Calendar date, boolean filterFrom);
    }
}
