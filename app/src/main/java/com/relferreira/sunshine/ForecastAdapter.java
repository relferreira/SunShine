package com.relferreira.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.relferreira.sunshine.data.WeatherContract;

import org.w3c.dom.Text;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    private boolean twoPanelMode;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    public void setTwoPanelMode(boolean twoPanel) {
        this.twoPanelMode = twoPanel;
    }
    @Override
    public int getItemViewType(int position) {
        return (position == 0 && !twoPanelMode) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /*
            Remember that these views are reused as needed.
         */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int layoutId = (getItemViewType(cursor.getPosition()) == VIEW_TYPE_TODAY) ? R.layout.list_item_forecast_today : R.layout.list_item_forecast;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);

        int weatherIcon;
        if(getItemViewType(cursor.getPosition()) == VIEW_TYPE_TODAY)
            weatherIcon = Utility.getArtResourceForWeatherCondition(weatherId);
        else
            weatherIcon = Utility.getIconResourceForWeatherCondition(weatherId);
        viewHolder.iconView.setImageResource(weatherIcon);

        long weatherDate = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        viewHolder.dateView.setText(Utility.getDayName(context, weatherDate));

        String weatherForecast = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        viewHolder.descriptionView.setText(weatherForecast);

        boolean isMetric = Utility.isMetric(context);

        double high = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        viewHolder.highTempView.setText(Utility.formatTemperature(context, high, isMetric));

        double low = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        viewHolder.lowTempView.setText(Utility.formatTemperature(context, low, isMetric));
    }

    public static class ViewHolder {

        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ViewHolder(View view){
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
        }
    }
}