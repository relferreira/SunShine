package com.relferreira.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.util.Util;
import com.relferreira.sunshine.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int DETAIL_LOADER = 0;

    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_HUMIDITY = 5;
    static final int COL_WIND_SPEED = 6;
    static final int COL_DEGREE = 7;
    static final int COL_PRESSURE = 8;
    static final int COL_WEATHER_CONDITION_ID = 9;

    public static final String ARG_WEATHER = "arg_weather";
    private ShareActionProvider shareActionProvider;
    private String forecast;
    private ImageView iconView;
    private TextView friendlyDateView;
    private TextView dateView;
    private TextView descriptionView;
    private TextView highTempView;
    private TextView lowTempView;
    private TextView humidityView;
    private TextView pressureView;
    private TextView windView;
    private Uri uri;

    public static DetailActivityFragment newInstance(Uri dateUri){
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_WEATHER, dateUri);
        DetailActivityFragment frag = new DetailActivityFragment();
        frag.setArguments(bundle);
        return frag;
    }

    public DetailActivityFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.detail, menu);
        MenuItem item = menu.findItem(R.id.action_share);

        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        if(forecast != null)
            setShareIntent();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_settings){
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        iconView = (ImageView) view.findViewById(R.id.detail_icon);
        friendlyDateView = (TextView) view.findViewById(R.id.detail_friendly_date_textview);
        dateView = (TextView) view.findViewById(R.id.detail_date_textview);
        descriptionView = (TextView) view.findViewById(R.id.detail_forecast_textview);
        highTempView = (TextView) view.findViewById(R.id.detail_high_textview);
        lowTempView = (TextView) view.findViewById(R.id.detail_low_textview);
        humidityView = (TextView) view.findViewById(R.id.detail_humidity_textview);
        windView = (TextView) view.findViewById(R.id.detail_wind_textview);
        pressureView = (TextView) view.findViewById(R.id.detail_pressure_textview);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        uri = null;
        if(getArguments() != null)
            uri = getArguments().getParcelable(ARG_WEATHER);
        else
            return null;
        return new CursorLoader(getActivity(), uri, FORECAST_COLUMNS, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        data.moveToFirst();

        int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);

        long weatherDate = data.getLong(COL_WEATHER_DATE);
        friendlyDateView.setText(Utility.getDayName(getContext(), weatherDate));
        dateView.setText(Utility.getFormattedMonthDay(getContext(), weatherDate));

        String weatherForecast = data.getString(COL_WEATHER_DESC);
        descriptionView.setText(weatherForecast);

        Glide.with(getContext())
                .load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherId))
                .error(Utility.getArtResourceForWeatherCondition(weatherId))
                .into(iconView);
        iconView.setContentDescription(getString(R.string.a11y_forecast_icon, weatherForecast));

        boolean isMetric = Utility.isMetric(getContext());

        double high = data.getDouble(COL_WEATHER_MAX_TEMP);
        String highString = Utility.formatTemperature(getContext(), high, isMetric);
        highTempView.setText(highString);
        highTempView.setContentDescription(getString(R.string.a11y_high_temp, highString));

        double low = data.getDouble(COL_WEATHER_MIN_TEMP);
        String lowString = Utility.formatTemperature(getContext(), low, isMetric);
        lowTempView.setText(lowString);
        lowTempView.setContentDescription(getString(R.string.a11y_low_temp, lowString));

        float humidity = data.getFloat(COL_HUMIDITY);
        humidityView.setText(Utility.getFormatedHumidity(getContext(), humidity));
        humidityView.setContentDescription(humidityView.getText());

        float wind = data.getFloat(COL_WIND_SPEED);
        float degree = data.getFloat(COL_DEGREE);
        windView.setText(Utility.getFormattedWind(getContext(), wind, degree));
        windView.setContentDescription(windView.getText());

        float pressure = data.getFloat(COL_PRESSURE);
        pressureView.setText(Utility.getFormatedPressure(getContext(), pressure));
        pressureView.setContentDescription(pressureView.getText());

        forecast = String.format("%s - %s - %s/%s", weatherDate, weatherForecast, high, low);

        if(shareActionProvider != null)
            setShareIntent();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void onLocationChanged(String newLocation) {
        if(uri != null){
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            uri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    private void setShareIntent() {
        if (shareActionProvider != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, forecast);
            shareActionProvider.setShareIntent(shareIntent);
        }
    }
}
