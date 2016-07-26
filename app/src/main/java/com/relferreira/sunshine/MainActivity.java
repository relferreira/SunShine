package com.relferreira.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.relferreira.sunshine.data.WeatherContract;
import com.relferreira.sunshine.gcm.RegistrationIntentService;
import com.relferreira.sunshine.sync.SunshineSyncAdapter;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements ForecastFragment.ForecastCallback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";

    private String location;
    private boolean twoPanelLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        location = Utility.getPreferredLocation(this);

        if (findViewById(R.id.weather_detail_container) != null) {
            twoPanelLayout = true;

            if (savedInstanceState == null) {
                String location = Utility.getPreferredLocation(this);
                Uri uri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(location, new Date().getTime());
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, DetailActivityFragment.newInstance(uri), DETAILFRAGMENT_TAG)
                        .commit();
            }

            ForecastFragment frag = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            frag.setAdapterMode(twoPanelLayout);

        } else {
            twoPanelLayout = false;
            getSupportActionBar().setElevation(0f);
        }

        SunshineSyncAdapter.initializeSyncAdapter(this);

        if (checkPlayServices()) {
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
            boolean sentToken = sharedPreferences.getBoolean(SENT_TOKEN_TO_SERVER, false);
            if (!sentToken) {
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
        }
    }

    @Override
    protected void onResume () {
        super.onResume();
        String currentLocation = Utility.getPreferredLocation(this);
        if (!currentLocation.equals(location)) {
            ForecastFragment frag = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            if (frag != null) {
                frag.onLocationChanged();
            }

            DetailActivityFragment detailFrag = (DetailActivityFragment) getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if (detailFrag != null) {
                detailFrag.onLocationChanged(currentLocation);
            }

            location = currentLocation;
        }
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected (Uri dateUri){
        if (twoPanelLayout) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, DetailActivityFragment.newInstance(dateUri), DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(dateUri);
            startActivity(intent);
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(LOG_TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
