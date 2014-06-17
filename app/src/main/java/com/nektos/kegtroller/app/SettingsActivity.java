package com.nektos.kegtroller.app;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by casele on 6/16/14.
 */
public class SettingsActivity extends PreferenceActivity
{

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // add the xml resource
        addPreferencesFromResource(R.layout.user_settings);
    }

}