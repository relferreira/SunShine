package com.relferreira.sunshine.muzei;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.MuzeiArtSource;
import com.relferreira.sunshine.MainActivity;
import com.relferreira.sunshine.Utility;
import com.relferreira.sunshine.data.WeatherContract;
import com.relferreira.sunshine.sync.SunshineSyncAdapter;

/**
 * Created by relferreira on 7/31/16.
 */
public class WeatherMuzeiSource extends MuzeiArtSource {

    private static final String[] FORECAST_COLUMNS = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_SHORT_DESC = 1;

    public WeatherMuzeiSource() {
        super("WeatherMuzeiSource");
    }

    @Override
    protected void onUpdate(int reason) {
        String location = Utility.getPreferredLocation(this);

        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(location, System.currentTimeMillis());

        Cursor data = getContentResolver().query(
                weatherUri,
                FORECAST_COLUMNS,
                null,
                null,
                WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");

        if(data.moveToFirst()) {
            int weatherId = data.getInt(INDEX_WEATHER_ID);
            String desc = data.getString(INDEX_SHORT_DESC);

            String imageUrl = Utility.getImageUrlForWeatherCondition(weatherId);
            if(imageUrl != null) {
                publishArtwork(new Artwork.Builder()
                            .imageUri(Uri.parse(imageUrl))
                            .title(desc)
                            .byline(location)
                            .viewIntent(new Intent(this, MainActivity.class))
                            .build());
            }
        }

        data.close();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        boolean dataUpdated = intent != null && SunshineSyncAdapter.ACTION_UPDATE_DATA.equals(intent.getAction());
        if(dataUpdated && isEnabled()) {
            onUpdate(UPDATE_REASON_OTHER);
        }
    }
}
