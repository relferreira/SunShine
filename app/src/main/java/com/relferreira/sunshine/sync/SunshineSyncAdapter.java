package com.relferreira.sunshine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.relferreira.sunshine.DetailActivity;
import com.relferreira.sunshine.MainActivity;
import com.relferreira.sunshine.R;
import com.relferreira.sunshine.Utility;
import com.relferreira.sunshine.data.WeatherContract;
import com.relferreira.sunshine.muzei.WeatherMuzeiSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Vector;
import java.util.concurrent.ExecutionException;


public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String ACTION_UPDATE_DATA = "com.relferreira.sunshine.ACTION_DATA_UPDATED";
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOCATION_STATUS_OK, LOCATION_STATUS_SERVER_DOWN, LOCATION_STATUS_SERVER_INVALID, LOCATION_STATUS_UNKNOWN})
    public @interface LocationStatus {}
    public static final int LOCATION_STATUS_OK = 0;
    public static final int LOCATION_STATUS_SERVER_DOWN = 1;
    public static final int LOCATION_STATUS_SERVER_INVALID = 2;
    public static final int LOCATION_STATUS_UNKNOWN = 3;
    public static final int LOCATION_STATUS_INVALID = 4;

    public final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();
    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute)  180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;
    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "onPerformSync Called.");
        syncForecast();
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    private void syncForecast() {
        String locationQuery = Utility.getPreferredLocation(getContext());
        String locationLatitude = String.valueOf(Utility.getLocationLatitude(getContext()));
        String locationLongitude = String.valueOf(Utility.getLocationLongitude(getContext()));

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "appid";
            final String LAT_PARAM = "lat";
            final String LON_PARAM = "lon";

            String appId = "05b955346a87c70892273d30f7c0a85f";

            Uri.Builder builder = Uri.parse(FORECAST_BASE_URL).buildUpon();

            if(Utility.isLocationLatLongAvailable(getContext())){
                builder.appendQueryParameter(LAT_PARAM, locationLatitude);
                builder.appendQueryParameter(LON_PARAM, locationLongitude);
            } else {
                builder.appendQueryParameter(QUERY_PARAM, locationQuery);
            }
            builder.appendQueryParameter(FORMAT_PARAM, format);
            builder.appendQueryParameter(UNITS_PARAM, units);
            builder.appendQueryParameter(DAYS_PARAM, Integer.toString(numDays));
            builder.appendQueryParameter(APPID_PARAM, appId);

            Uri builtUri = builder.build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();

            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                setLocationStatus(LOCATION_STATUS_SERVER_DOWN);
                return;
            }
            forecastJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            setLocationStatus(LOCATION_STATUS_SERVER_DOWN);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            getWeatherDataFromJson(forecastJsonStr, locationQuery);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            setLocationStatus(LOCATION_STATUS_SERVER_INVALID);
            e.printStackTrace();
        }
        // This will only happen if there was an error getting or parsing the forecast.
        return;
    }

    private void setLocationStatus(int status) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(getContext().getString(R.string.pref_location_status_key), status);
        editor.commit();
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr,
                                            String locationSetting)
            throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        final String OWM_MESSAGE_CODE = "cod";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";


        JSONObject forecastJson = new JSONObject(forecastJsonStr);

        if(forecastJson.has(OWM_MESSAGE_CODE)){
            int statusCode = forecastJson.getInt(OWM_MESSAGE_CODE);
            if (statusCode == HttpURLConnection.HTTP_OK){
                setLocationStatus(LOCATION_STATUS_OK);
            } else if(statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                setLocationStatus(LOCATION_STATUS_INVALID);
                return null;
            } else {
                setLocationStatus(LOCATION_STATUS_SERVER_DOWN);
                return null;
            }
        }


        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
        String cityName = cityJson.getString(OWM_CITY_NAME);

        JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
        double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

        long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

        // Insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();

        for(int i = 0; i < weatherArray.length(); i++) {
            // These are the values that will be collected.
            long dateTime;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;

            double high;
            double low;

            String description;
            int weatherId;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // Cheating to convert this to UTC time, which is what we want anyhow
            dateTime = dayTime.setJulianDay(julianStartDay+i);

            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

            // Description is in a child array called "weather", which is 1 element long.
            // That element also contains a weather code.
            JSONObject weatherObject =
                    dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            high = temperatureObject.getDouble(OWM_MAX);
            low = temperatureObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

            cVVector.add(weatherValues);
        }

        // add to database
        int inserted = 0;
        if ( cVVector.size() > 0 ) {

            // Student: call bulkInsert to add the weatherEntries to the database here
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            inserted = getContext().getContentResolver().bulkInsert(
                    WeatherContract.WeatherEntry.CONTENT_URI, cvArray);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);

            getContext().getContentResolver().delete(
                    WeatherContract.WeatherEntry.CONTENT_URI,
                    WeatherContract.WeatherEntry.COLUMN_DATE + " <= ?",
                    new String[] { String.valueOf(calendar.getTimeInMillis()) });

            Intent dataUpdatedIntent = new Intent(ACTION_UPDATE_DATA);
            getContext().sendBroadcast(dataUpdatedIntent);

            getContext().startService(new Intent(ACTION_UPDATE_DATA).setClass(getContext(), WeatherMuzeiSource.class));

            notifyWeather();
        }

        Log.d(LOG_TAG, "Fetch weather task complete. " + inserted + " Inserted");


        return null;

    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName A human-readable city name, e.g "Mountain View"
     * @param lat the latitude of the city
     * @param lon the longitude of the city
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // Students: First, check if the location with this city name exists in the db
        Uri uri = WeatherContract.LocationEntry.CONTENT_URI;
        String selection = WeatherContract.LocationEntry.COLUMN_CITY_NAME + "= ? AND " +
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?";

        Cursor c = getContext().getContentResolver().query(uri, null, selection, new String[]{cityName, locationSetting}, null);
        if(c != null && c.moveToFirst()) {
            return c.getLong(c.getColumnIndex(WeatherContract.LocationEntry._ID));
        }

        ContentValues values = new ContentValues();
        values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
        values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
        values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
        values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
        Uri locationUri = getContext().getContentResolver().insert(uri, values);

        if(locationUri != null)
            return Long.valueOf(locationUri.getLastPathSegment());
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        return -1;
    }

    private void notifyWeather() {
        Context context = getContext();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showNotification = pref.getBoolean(context.getString(R.string.pref_enable_notifications_key), Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));
        long lastNotification = pref.getLong(context.getString(R.string.pref_last_notification), 0);

        if(!showNotification)
            return;

        if(System.currentTimeMillis() - lastNotification >= DAY_IN_MILLIS) {
            String locationQuery = Utility.getPreferredLocation(context);
            boolean isMetric = Utility.isMetric(context);
            Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

            Cursor c = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

            if (c.moveToFirst()) {
                int id = c.getInt(INDEX_WEATHER_ID);
                double maxTemp  = c.getDouble(INDEX_MAX_TEMP);
                double minTemp  = c.getDouble(INDEX_MIN_TEMP);
                String des = c.getString(INDEX_SHORT_DESC);

                String contentText = String.format(
                        context.getString(R.string.format_notification),
                        des, Utility.formatTemperature(context, maxTemp, isMetric),
                        Utility.formatTemperature(context, minTemp, isMetric));

                Intent intent = new Intent(context, DetailActivity.class);
                intent.setData(weatherUri);

                TaskStackBuilder taskBuilder = TaskStackBuilder.create(context);
                taskBuilder.addNextIntent(intent);

                int iconId = Utility.getIconResourceForWeatherCondition(id);
                Resources resources = context.getResources();
                int artResourceId = Utility.getArtResourceForWeatherCondition(id);
                String artUrl = Utility.getArtUrlForWeatherCondition(context,id);

                int largeIconWidth = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ?
                        context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width) :
                        context.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_default);

                int largeIconHeight = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ?
                        context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height) :
                        context.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_default);

                try {
                    Bitmap bitmap = Glide.with(getContext())
                            .load(artUrl)
                            .asBitmap()
                            .error(artResourceId)
                            .into(largeIconWidth, largeIconHeight)
                            .get();

                    PendingIntent pendingIntent = taskBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT);
                    Notification notification = new NotificationCompat.Builder(context)
                            .setContentTitle(context.getString(R.string.notification_title))
                            .setLargeIcon(bitmap)
                            .setSmallIcon(iconId)
                            .setContentText(contentText)
                            .setContentIntent(pendingIntent)
                            .build();

                    NotificationManagerCompat.from(context).notify(WEATHER_NOTIFICATION_ID, notification);

                    SharedPreferences.Editor editor = pref.edit();
                    editor.putLong(context.getString(R.string.pref_last_notification), System.currentTimeMillis());
                    editor.apply();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }


            }
        }
    }

}