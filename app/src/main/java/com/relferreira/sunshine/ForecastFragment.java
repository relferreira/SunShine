package com.relferreira.sunshine;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;

import com.relferreira.sunshine.data.WeatherContract;
import com.relferreira.sunshine.sync.SunshineSyncAdapter;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener,
        ForecastAdapter.ForecasAdapterOnClickHandler{

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
    private RecyclerView recyclerView;
    private int selectedPosition;
    private boolean twoPanel;
    private TextView emptyView;
    private SharedPreferences pref;
    private int locationStatus;
    private int mChoiceMode;
    private boolean mAutoSelectView;
    private int scrollPosition;
    private boolean mHoldTransition;


    public interface ForecastCallback {

        void onItemSelected(Uri dateUri, ForecastAdapter.ForecastViewHolder vh);
    }

    public ForecastFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.ForecastFragment,
                0, 0);
        mChoiceMode = a.getInt(R.styleable.ForecastFragment_android_choiceMode, AbsListView.CHOICE_MODE_NONE);
        mAutoSelectView = a.getBoolean(R.styleable.ForecastFragment_autoSelectView, false);
        mHoldTransition = a.getBoolean(R.styleable.ForecastFragment_sharedElementTransition, false);
        a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_forecast);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        emptyView = (TextView) view.findViewById(R.id.empty_forecast);
        adapter = new ForecastAdapter(getActivity(), null, 0, emptyView, mChoiceMode, this);
        adapter.setTwoPanelMode(twoPanel);
        recyclerView.setAdapter(adapter);
        final Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayout appBar = ((MainActivity)getActivity()).getAppBar();
                scrollPosition += dy;
                if(appBar != null)
                    appBar.setTranslationY((float) -(scrollPosition * 0.5));
            }
        });
        //recyclerView.setEmptyView(emptyView);

        if(savedInstanceState != null){
            selectedPosition = savedInstanceState.getInt(SELECTED_ITEM_POSITION);
            adapter.onRestoreInstanceState(savedInstanceState);
        }
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(mHoldTransition){
            getActivity().supportPostponeEnterTransition();
        }

        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        pref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        pref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(null != recyclerView)
            recyclerView.clearOnScrollListeners();
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
            case R.id.action_map:
                launchMapIntent();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if(s.equals(getString(R.string.pref_location_status_key))) {
            locationStatus = sharedPreferences.getInt(s, SunshineSyncAdapter.LOCATION_STATUS_OK);
            updateEmptyView();
        }
    }

    private void launchMapIntent(){
        if(adapter != null) {
            Cursor c = adapter.getCursor();
            if (c.moveToFirst()) {
                String lat = c.getString(COL_COORD_LAT);
                String longtu = c.getString(COL_COORD_LONG);

                Uri uri = Uri.parse("geo:" + lat + "," + longtu + "0?");

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null)
                    startActivity(intent);
                else
                    Toast.makeText(getActivity(), getResources().getString(R.string.map_not_availabe), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onLocationChanged(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);

    }

    private void updateWeather(){
        SunshineSyncAdapter.syncImmediately(getActivity());
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
        recyclerView.smoothScrollToPosition(selectedPosition);
        updateEmptyView();
        if(data.getCount() == 0){
            getActivity().supportStartPostponedEnterTransition();
        } else {
            recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // Since we know we're going to get items, we keep the listener around until
                    // we see Children.
                    if (recyclerView.getChildCount() > 0) {
                        recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        int itemPosition = adapter.getSelectedItemPosition();
                        if ( RecyclerView.NO_POSITION == itemPosition ) itemPosition = 0;
                        RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(itemPosition);
                        if ( null != vh && mAutoSelectView ) {
                            adapter.selectView( vh );
                        }
                        if(mHoldTransition) {
                            getActivity().supportStartPostponedEnterTransition();
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void updateEmptyView() {
        if(locationStatus == SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN)
            emptyView.setText(getString(R.string.empty_forecast_list_server_down));
        else if(locationStatus == SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID)
            emptyView.setText(getString(R.string.empty_forecast_list_server_error));
        else if(locationStatus == SunshineSyncAdapter.LOCATION_STATUS_INVALID)
            emptyView.setText(getString(R.string.empty_forecast_list_invalid_location));
        else if(adapter.getItemCount() == 0 && !isConnected())
            emptyView.setText(getString(R.string.empty_list_internet));
        else if (adapter.getItemCount() == 0)
            emptyView.setText(getString(R.string.empy_list));
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //TODO
        outState.putInt(SELECTED_ITEM_POSITION, selectedPosition);
        adapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    public void setAdapterMode(boolean twoPanel){
        this.twoPanel = twoPanel;
        if(adapter != null)
            adapter.setTwoPanelMode(twoPanel);
    }

    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();

    }

    @Override
    public void click(Cursor cursor, ForecastAdapter.ForecastViewHolder vh) {
        if(cursor != null){
            String location = Utility.getPreferredLocation(getActivity());
            Uri uri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(location, cursor.getLong(COL_WEATHER_DATE));
            ForecastCallback callback = (ForecastCallback) getActivity();
            callback.onItemSelected(uri, vh);
            selectedPosition = vh.getAdapterPosition();
        }
    }
}
