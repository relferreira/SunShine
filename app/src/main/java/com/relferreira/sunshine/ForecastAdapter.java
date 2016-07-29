package com.relferreira.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.relferreira.sunshine.data.WeatherContract;

import org.w3c.dom.Text;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    private final int flags;
    private final ItemChoiceManager mICM;
    private View emptyView;
    private final ForecasAdapterOnClickHandler handler;
    private Cursor cursor;
    private Context context;
    private boolean twoPanelMode;

    interface ForecasAdapterOnClickHandler {
        void click(Cursor cursor, ForecastViewHolder vh);
    }

    public ForecastAdapter(Context context, Cursor c, int flags, View emptyView, int choiceMode, ForecasAdapterOnClickHandler handler) {
        this.context = context;
        this.cursor = c;
        this.flags = flags;
        this.emptyView = emptyView;
        this.handler = handler;
        mICM = new ItemChoiceManager(this);
        mICM.setChoiceMode(choiceMode);

    }

    public void setTwoPanelMode(boolean twoPanel) {
        this.twoPanelMode = twoPanel;
    }

    @Override
    public ForecastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId = (viewType == VIEW_TYPE_TODAY) ? R.layout.list_item_forecast_today : R.layout.list_item_forecast;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ForecastViewHolder viewHolder, int position) {

        cursor.moveToPosition(position);

        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);

        int weatherIcon;
        if(getItemViewType(cursor.getPosition()) == VIEW_TYPE_TODAY)
            weatherIcon = Utility.getArtResourceForWeatherCondition(weatherId);
        else
            weatherIcon = Utility.getIconResourceForWeatherCondition(weatherId);

        //viewHolder.iconView.setImageResource(weatherIcon);
        Glide.with(context)
                .load(Utility.getArtUrlForWeatherCondition(context, weatherId))
                .placeholder(weatherIcon)
                .error(weatherIcon)
                .crossFade()
                .into(viewHolder.iconView);

        long weatherDate = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        viewHolder.dateView.setText(Utility.getDayName(context, weatherDate));

        String weatherForecast = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        viewHolder.descriptionView.setText(weatherForecast);
        viewHolder.descriptionView.setContentDescription(context.getString(R.string.a11y_forecast, weatherForecast));

        boolean isMetric = Utility.isMetric(context);

        String high = Utility.formatTemperature(context,
                cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP),
                isMetric);
        viewHolder.highTempView.setText(high);
        viewHolder.highTempView.setContentDescription(context.getString(R.string.a11y_high_temp, high));

        String low = Utility.formatTemperature(context,
                cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP),
                isMetric);
        viewHolder.lowTempView.setText(low);
        viewHolder.lowTempView.setContentDescription(context.getString(R.string.a11y_low_temp, low));

        ViewCompat.setTransitionName(viewHolder.iconView, "iconView" + position);
        mICM.onBindViewHolder(viewHolder, position);
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && !twoPanelMode) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getItemCount() {
        return (cursor != null) ? cursor.getCount() : 0;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public void swapCursor(Cursor cursor) {
        this.cursor = cursor;
        emptyView.setVisibility((cursor == null || cursor.getCount() == 0) ? View.VISIBLE : View.GONE);
        notifyDataSetChanged();
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mICM.onRestoreInstanceState(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
        mICM.onSaveInstanceState(outState);
    }

//    public void setUseTodayLayout(boolean useTodayLayout) {
//        mUseTodayLayout = useTodayLayout;
//    }

    public int getSelectedItemPosition() {
        return mICM.getSelectedItemPosition();
    }

    public void selectView(RecyclerView.ViewHolder viewHolder) {
        if ( viewHolder instanceof ForecastViewHolder ) {
            ForecastViewHolder vfh = (ForecastViewHolder)viewHolder;
            vfh.onClick(vfh.itemView);
        }
    }

    public class ForecastViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ForecastViewHolder(View view){
            super(view);
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);


            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            cursor.moveToPosition(getAdapterPosition());
            handler.click(cursor, this);
        }
    }
}