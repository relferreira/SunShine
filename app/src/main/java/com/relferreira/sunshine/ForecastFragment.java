package com.relferreira.sunshine;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.relferreira.sunshine.data.WeatherContract;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String FORECAST_TAG = "forecast_tag";
    public static final String SELECTED_ITEM_POSITION = "selected_item_position";
    public static final int LOADER_ID = 1;
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    private ForecastAdapter adapter;
    private ArrayList<String> weekForecast = new ArrayList<>();
    private ListView listView;
    private int selectedPosition;
    private boolean twoPanel;

    public interface ForecastCallback {

        void onItemSelected(Uri dateUri);
    }

    public ForecastFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        listView = (ListView) view.findViewById(R.id.listview_forecast);
        adapter = new ForecastAdapter(getActivity(), null, 0);
        adapter.setTwoPanelMode(twoPanel);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if(cursor != null){
                    String location = Utility.getPreferredLocation(getActivity());
                    Uri uri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(location, cursor.getLong(COL_WEATHER_DATE));
                    ForecastCallback callback = (ForecastCallback) getActivity();
                    callback.onItemSelected(uri);
                }
            }
        });

        if(savedInstanceState != null){
            selectedPosition = savedInstanceState.getInt(SELECTED_ITEM_POSITION);
        }
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_refresh:
                updateWeather();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onLocationChanged(){
        updateWeather();
        getLoaderManager().restartLoader(LOADER_ID, null, this);

    }

    private void updateWeather(){

        FetchWeatherTask task = new FetchWeatherTask(getActivity());
        String location = Utility.getPreferredLocation(getActivity());
        task.execute(location);
    }

    public static boolean checkIfAppIsInstalled(Context context, String uri) {
        PackageManager pm = context.getPackageManager();
        boolean appInstalled = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            appInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            appInstalled = false;
        }
        return appInstalled;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String location = Utility.getPreferredLocation(getActivity());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(location, System.currentTimeMillis());
        return new CursorLoader(getActivity(), weatherLocationUri ,FORECAST_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
        listView.smoothScrollToPosition(selectedPosition);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_ITEM_POSITION, listView.getCheckedItemPosition());
        super.onSaveInstanceState(outState);
    }

    public void setAdapterMode(boolean twoPanel){
        this.twoPanel = twoPanel;
        if(adapter != null)
            adapter.setTwoPanelMode(twoPanel);
    }
}
