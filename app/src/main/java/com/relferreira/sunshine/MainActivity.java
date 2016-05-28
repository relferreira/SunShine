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

public class MainActivity extends AppCompatActivity {

    private String location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        location = Utility.getPreferredLocation(this);

        if(savedInstanceState == null){
            ForecastFragment frag = new ForecastFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, frag, ForecastFragment.FORECAST_TAG)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentLocation = Utility.getPreferredLocation(this);
        if(!currentLocation.equals(location)){
            ForecastFragment frag = (ForecastFragment) getSupportFragmentManager().findFragmentByTag(ForecastFragment.FORECAST_TAG);
            if(frag != null){
                frag.onLocationChanged();
                location = currentLocation;
            }
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
}
