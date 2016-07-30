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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.ui.PlacePicker;

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

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(getContext());
        if(resultCode == ConnectionResult.SUCCESS) {
            setWidgetLayoutResource(R.layout.pref_current_location);
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        View currentLocation = view.findViewById(R.id.current_location);
        if(currentLocation != null) {
            currentLocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SettingsActivity activity = (SettingsActivity) getContext();
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                    try {
                        activity.startActivityForResult(builder.build(activity), SettingsActivity.PLACE_PICKER_REQUEST);
                    } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return view;

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
