package com.relferreira.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DetailActivity extends AppCompatActivity {

    public final static String ARG_WEATHER = "arg_weather";
    public final String FORECAST_SHARE_HASHTAG = "#SunshineApp";
    private String weather;
    private ShareActionProvider shareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        weather = getIntent().getDataString();

        if(savedInstanceState == null){
            DetailActivityFragment frag = DetailActivityFragment.newInstance(weather);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, frag)
                    .commit();
        }
    }

}
