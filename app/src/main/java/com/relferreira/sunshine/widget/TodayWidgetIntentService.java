package com.relferreira.sunshine.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.relferreira.sunshine.MainActivity;
import com.relferreira.sunshine.R;
import com.relferreira.sunshine.Utility;
import com.relferreira.sunshine.data.WeatherContract;

/**
 * Created by relferreira on 7/30/16.
 */
public class TodayWidgetIntentService extends IntentService {

    public static final String TAG = "TodayWidgetIntentService";
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_SHORT_DESC = 1;
    private static final int INDEX_MAX_TEMP = 2;
    private static final int INDEX_MIN_TEMP = 3;


    public TodayWidgetIntentService() {
        super(TAG);
    }

    public TodayWidgetIntentService(String name) {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                TodayWidgetProvider.class));

        String location = Utility.getPreferredLocation(this);
        boolean isMetric = Utility.isMetric(this);
        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location,
                System.currentTimeMillis()
        );

        Cursor data = getContentResolver().query(
                weatherUri,
                FORECAST_COLUMNS,
                null,
                null,
                WeatherContract.WeatherEntry.COLUMN_DATE + " ASC"
        );

        if(data == null)
            return;

        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        // Extract the weather data from the Cursor
        int weatherId = data.getInt(INDEX_WEATHER_ID);
        int weatherArtResourceId = Utility.getArtResourceForWeatherCondition(weatherId);
        String description = data.getString(INDEX_SHORT_DESC);
        double maxTemp = data.getDouble(INDEX_MAX_TEMP);
        double minTemp = data.getDouble(INDEX_MIN_TEMP);
        String formattedMaxTemperature = Utility.formatTemperature(this, maxTemp, isMetric);
        String formattedMinTemperature = Utility.formatTemperature(this, minTemp, isMetric);
        data.close();

        for (int appWidgetId : appWidgetIds) {
            int layout = R.layout.widget_today_small;
            Bundle options = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                options = appWidgetManager.getAppWidgetOptions(appWidgetId);
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                int width =  (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                        displayMetrics);
                int minWidth = getResources().getDimensionPixelSize(R.dimen.widget_today_default_width);
                int maxwidth = getResources().getDimensionPixelSize(R.dimen.widget_today_max_size);
                if(width < minWidth){
                    layout = R.layout.widget_today_small;
                } else if(width >= minWidth && width < maxwidth){
                    layout = R.layout.widget_today;
                } else {
                    layout = R.layout.widget_today_large;
                }
            }

            RemoteViews views = new RemoteViews(
                    getPackageName(),
                    layout);


            // Add the data to the RemoteViews
            views.setImageViewResource(R.id.widget_icon, weatherArtResourceId);
            // Content Descriptions for RemoteViews were only added in ICS MR1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, description);
            }

            views.setTextViewText(R.id.widget_description, description);
            views.setTextViewText(R.id.widget_high_temperature, formattedMaxTemperature);
            views.setTextViewText(R.id.widget_low_temperature, formattedMinTemperature);

            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, String description) {
        views.setContentDescription(R.id.widget_icon, description);
    }
}
