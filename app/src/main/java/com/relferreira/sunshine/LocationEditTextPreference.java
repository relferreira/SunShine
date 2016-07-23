package com.relferreira.sunshine;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by relferreira on 7/23/16.
 */
public class LocationEditTextPreference extends EditTextPreference {

    private static final int DEFAULT_LENGTH = 2;
    private final int minLength;
    private EditText editText;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.LocationEditTextPreference, 0, 0);

        try{
            minLength = a.getInteger(R.styleable.LocationEditTextPreference_minLength, DEFAULT_LENGTH);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        editText = getEditText();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                Dialog d = getDialog();
                if(d instanceof AlertDialog) {
                    Button positiveButton =  ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE);
                    positiveButton.setEnabled((editable.length() >= minLength));
                }
            }
        });
    }


}
