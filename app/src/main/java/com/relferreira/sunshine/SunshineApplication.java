package com.relferreira.sunshine;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Created by relferreira on 7/30/16.
 */
public class SunshineApplication extends Application {
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
    }
}
