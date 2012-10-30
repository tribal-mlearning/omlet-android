/*
 * Copyright (c) 2012, TATRC and Tribal
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * * Neither the name of TATRC or TRIBAL nor the
 *   names of its contributors may be used to endorse or promote products
 *   derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TATRC OR TRIBAL BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.tribal.omlet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.View;

import com.tribal.mobile.Framework;
import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.phonegap.MFSettingsKeys;
import com.tribal.mobile.preferences.PrivateSettingsKeys;
import com.tribal.mobile.util.IntentUtils;
import com.tribal.mobile.util.NativeSettingsHelper;
import com.tribal.mobile.util.ViewHelper;

/**
 * Preferences activity.
 * 
 * @author Jon Brasted
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    /* Fields */
	
	private ListPreference dataSyncFrequencyPreference;
    private ListPreference dataSyncConnectivityPreference;
    
	/* Properties */
    
	protected BaseApplication getBaseApplication() {
		return (BaseApplication) this.getApplication();
	}
	
	/* Methods */
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // set the content view
        setContentView(R.layout.preferences);
        
        // set up view helper
		View view = findViewById(android.R.id.content);
		
		if (view != null) {		
			ViewHelper.setTypeFace(view);
		}
		
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();
        
        // Get a reference to the preferences
        dataSyncFrequencyPreference = (ListPreference)preferenceScreen.findPreference(NativeSettingsHelper.KEY_SYNC_INTERVAL);
        dataSyncConnectivityPreference = (ListPreference)preferenceScreen.findPreference(MFSettingsKeys.DATA_USE);
        
        String value = null;
        
        value = sharedPreferences.getString(NativeSettingsHelper.KEY_SYNC_INTERVAL, null);
        	
    	if (value != null) {        	
    		dataSyncFrequencyPreference.setValue(value);
    		onSharedPreferenceChanged(sharedPreferences, NativeSettingsHelper.KEY_SYNC_INTERVAL);
    	} else {
        	// set the summary to the default
            dataSyncFrequencyPreference.setSummary(dataSyncFrequencyPreference.getEntry());
        }
        
        value = sharedPreferences.getString(MFSettingsKeys.DATA_USE, null);
        	
    	if (value != null) {        	
    		dataSyncConnectivityPreference.setValue(value);
    		onSharedPreferenceChanged(sharedPreferences, MFSettingsKeys.DATA_USE);
    	} else {
        	// set the summary to the default
            dataSyncConnectivityPreference.setSummary(dataSyncConnectivityPreference.getEntry());
        }
    }
	
	@Override
    protected void onResume() {
        super.onResume();

        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onBackPressed() {
    	// If the user has not logged in, this means that the user is on the login screen and we need to ensure that the
        // user is redirected back to the login screen otherwise the application will be closed
    	if (Framework.getClient().getUserUsername() == null) {
    		// navigate to the login screen
    		Intent loginActivity = new Intent(this, LoginActivity.class);
    		startActivity(loginActivity);
    	}
    	
    	// finish the activity
    	finish();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if (NativeSettingsHelper.KEY_SYNC_INTERVAL.equalsIgnoreCase(key)) {
    		dataSyncFrequencyPreference.setSummary(dataSyncFrequencyPreference.getEntry());
    	}
    	else if (MFSettingsKeys.DATA_USE.equalsIgnoreCase(key)) {
    		dataSyncConnectivityPreference.setSummary(dataSyncConnectivityPreference.getEntry());
    	}
    	
    	if (PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS.equalsIgnoreCase(key)) {    	
	    	IntentUtils intentUtils = IntentUtils.getInstance();
	    	
	    	if (!intentUtils.isIntentExtraKeySet(key)) {
	    		Intent deferredBroadcast = new Intent();
	    		deferredBroadcast.setAction(BroadcastActions.SharedPreferenceChanged);
	    		deferredBroadcast.putExtra(IntentParameterConstants.Key, key);
	    		
	    		String value = NativeSettingsHelper.getInstance(this).checkAndGetPrivateSetting(key);
	    		deferredBroadcast.putExtra(IntentParameterConstants.Value, value);
	    		
	    		intentUtils.addDeferredBroadcast(deferredBroadcast);
	    	}
    	}
    }
}