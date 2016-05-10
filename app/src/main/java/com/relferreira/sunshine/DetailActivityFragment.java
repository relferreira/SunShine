package com.relferreira.sunshine;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    public static final String ARG_WEATHER = "arg_weather";

    public static DetailActivityFragment newInstance(String weather){
        Bundle bundle = new Bundle();
        bundle.putString(ARG_WEATHER, weather);
        DetailActivityFragment frag = new DetailActivityFragment();
        frag.setArguments(bundle);
        return frag;
    }

    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        TextView weatherText = (TextView) view.findViewById(R.id.detail_weather);
        if(getArguments() != null)
            weatherText.setText((String) getArguments().get(ARG_WEATHER));

        return view;
    }
}
