package com.relferreira.sunshine;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;


import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.relferreira.sunshine.data.WeatherContract;
import com.relferreira.sunshine.sync.SunshineSyncAdapter;

/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int PLACE_PICKER_REQUEST = 1;
    private SharedPreferences pref;
    private String location;
    private ImageView mAttribution;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add 'general' preferences, defined in the XML file
        addPreferencesFromResource(R.xml.pref_general);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_unit_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_theme_key)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mAttribution = new ImageView(this);
            mAttribution.setImageResource(R.drawable.powered_by_google_light);

            if (!Utility.isLocationLatLongAvailable(this)) {
                mAttribution.setVisibility(View.GONE);
            }
            setListFooter(mAttribution);
        }

    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public void onResume() {
        super.onResume();
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        pref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        location = Utility.getPreferredLocation(this);
        if(s.equals(getString(R.string.pref_location_key))){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(getString(R.string.pref_location_latitude));
            editor.remove(getString(R.string.pref_location_longitude));
            editor.commit();

            if (mAttribution != null) {
                mAttribution.setVisibility(View.GONE);
            }

            Utility.resetLocationStatus(this);
            SunshineSyncAdapter.syncImmediately(this);
        } else if(s.equals(getString(R.string.pref_unit_key)) || s.equals(getString(R.string.pref_theme_key))){
            getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        } else if(s.equals(getString(R.string.pref_location_status_key))) {
            int status = sharedPreferences.getInt(s, SunshineSyncAdapter.LOCATION_STATUS_OK);
            Preference pref = findPreference(getString(R.string.pref_location_key));
            if(status == SunshineSyncAdapter.LOCATION_STATUS_INVALID){
                pref.setSummary(getString(R.string.pref_location_error_description, location));
            } else if (status == SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN){
                pref.setSummary(getString(R.string.pref_location_unknown_description, location));
            } else if(status == SunshineSyncAdapter.LOCATION_STATUS_OK) {
                pref.setSummary(location);
            } else {
                pref.setSummary(getString(R.string.pref_location_error_server));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PLACE_PICKER_REQUEST){
            if(resultCode == RESULT_OK){
                Place place = PlacePicker.getPlace(data, this);
                String address = place.getAddress().toString();
                LatLng latLong = place.getLatLng();

                if(TextUtils.isEmpty(address)) {
                    address = String.format("(%.2f, %.2f)", latLong.latitude, latLong.longitude);
                }
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.pref_location_key), address);
                editor.putFloat(getString(R.string.pref_location_latitude), (float)latLong.latitude);
                editor.putFloat(getString(R.string.pref_location_longitude), (float)latLong.longitude);
                editor.commit();

                if (mAttribution != null) {
                    mAttribution.setVisibility(View.VISIBLE);
                } else {
                    // For pre-Honeycomb devices, we cannot add a footer, so we will use a snackbar
                    View rootView = findViewById(android.R.id.content);
                    Snackbar.make(rootView, getString(R.string.attribution_text), Snackbar.LENGTH_LONG).show();
                }

                Preference locationPreference = findPreference(getString(R.string.pref_location_key));
                locationPreference.setSummary(address);
                Utility.resetLocationStatus(this);
                SunshineSyncAdapter.syncImmediately(this);

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}