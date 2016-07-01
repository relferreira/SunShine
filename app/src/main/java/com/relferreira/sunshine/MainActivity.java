package com.relferreira.sunshine;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.relferreira.sunshine.data.WeatherContract;

public class MainActivity extends AppCompatActivity implements ForecastFragment.ForecastCallback {

    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    private String location;
    private boolean twoPanelLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        location = Utility.getPreferredLocation(this);

        if(findViewById(R.id.weather_detail_container) != null){
            twoPanelLayout = true;

            if(savedInstanceState == null){
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailActivityFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            twoPanelLayout = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentLocation = Utility.getPreferredLocation(this);
        if(!currentLocation.equals(location)){
            ForecastFragment frag = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            if(frag != null){
                frag.onLocationChanged();
            }

            DetailActivityFragment detailFrag = (DetailActivityFragment) getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if(detailFrag != null){
                detailFrag.onLocationChanged(currentLocation);
            }

            location = currentLocation;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if(id == R.id.action_map){
            launchMapIntent();
        }

        return super.onOptionsItemSelected(item);
    }

    private void launchMapIntent(){
        String location = Utility.getPreferredLocation(this);
        //TODO refactor
        Uri uri = Uri.parse("geo:0,0?")
                .buildUpon()
                .appendQueryParameter("q", location)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        if(intent.resolveActivity(getPackageManager()) != null)
            startActivity(intent);
        else
            Toast.makeText(this, getResources().getString(R.string.map_not_availabe), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onItemSelected(Uri dateUri) {
        if(twoPanelLayout){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, DetailActivityFragment.newInstance(dateUri), DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(dateUri);
            startActivity(intent);
        }
    }
}
