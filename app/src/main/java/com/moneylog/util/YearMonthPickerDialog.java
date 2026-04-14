package com.moneylog.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneylog.R;

import java.time.YearMonth;

/**
 * 년/월 선택 다이얼로그.
 */
public final class YearMonthPickerDialog {

    public interface OnYearMonthSelectedListener {
        void onSelected(String yearMonth);
    }

    private YearMonthPickerDialog() {}

    public static void show(Context context, String currentYearMonth,
                            OnYearMonthSelectedListener listener) {
        YearMonth ym = YearMonth.parse(currentYearMonth);
        int year = ym.getYear();
        int month = ym.getMonthValue();

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_year_month_picker, null);
        NumberPicker npYear = view.findViewById(R.id.npYear);
        NumberPicker npMonth = view.findViewById(R.id.npMonth);

        npYear.setMinValue(2000);
        npYear.setMaxValue(2100);
        npYear.setValue(year);
        npYear.setWrapSelectorWheel(false);

        npMonth.setMinValue(1);
        npMonth.setMaxValue(12);
        npMonth.setValue(month);
        npMonth.setWrapSelectorWheel(true);
        npMonth.setDisplayedValues(
                context.getResources().getStringArray(R.array.month_names));

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.month_picker_title))
                .setView(view)
                .setPositiveButton(context.getString(R.string.confirm), (dialog, which) -> {
                    String selected = String.format("%04d-%02d", npYear.getValue(), npMonth.getValue());
                    listener.onSelected(selected);
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }
}
