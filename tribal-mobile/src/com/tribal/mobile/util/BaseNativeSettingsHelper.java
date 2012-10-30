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

package com.tribal.mobile.util;

import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.tribal.mobile.phonegap.MFSettingsKeys;

/**
 * Utility class to retrieve native preference values, specifically the keys in {@link MFSettingsKeys}.
 *
 * @author Jon Brasted
 */
public class BaseNativeSettingsHelper {
	/* Fields */
	
    public static final String KEY_SYNC_INTERVAL = "app_sync_interval";
    public static final String KEY_LAST_LOGGED_IN_USER = "last_logged_in_user";
	
	/* Properties */
	
    private Context context;
	
	/* Constructor */
	
	protected BaseNativeSettingsHelper(Context context) {
		this.context = context;
	}
	
	
	/* Methods */
	
	/**
	 * Retrieves a native setting. The permitted values for <code>key</code> are as follows:
	 * <p>
	 * 	<code>MFSettingsKeys.SYNC_INTERVAL</code><br />
	 * 	<code>MFSettingsKeys.DATA_USE</code><br />
	 *  <code>MFSettingsKeys.VERSION</code><br />
	 *  <code>MFSettingsKeys.LAST_LOGGED_IN_USER</code>
	 * </p>
	 * 
	 * @param key	the key
	 * @return		the value or null
	 */	
	public String checkAndGetNativeSetting(String key) {
		// check the predefined list
		if (MFSettingsKeys.SYNC_INTERVAL.equalsIgnoreCase(key)) {
			return getPreferenceStringValue(KEY_SYNC_INTERVAL);
			
		} else if (MFSettingsKeys.DATA_USE.equalsIgnoreCase(key)) {
			return getPreferenceStringValue(MFSettingsKeys.DATA_USE);
			
		} else if (MFSettingsKeys.VERSION.equalsIgnoreCase(key)) {
			return ContextUtils.getVersionName(context);
			
		} else if (MFSettingsKeys.LAST_LOGGED_IN_USER.equalsIgnoreCase(key)) {
			return getPreferenceStringValue(KEY_LAST_LOGGED_IN_USER);
		
		}
		
		return null;
	}
	
	/**
	 * Retrieves a native setting by invoking {@link #checkAndGetNativeSetting(String)}. In addition to that, method will return
	 * <code>defaultValue</code> if a null or empty String is returned from {@link #checkAndGetNativeSetting(String)}.
	 * 
	 * @param key	the key
	 * @return		the value or null
	 */	
	public String checkAndGetNativeSetting(String key, String defaultValue) {
		String value = checkAndGetNativeSetting(key);
		
		if (TextUtils.isEmpty(value)) {
			return defaultValue;
		}
		
		return value;
	}
	
	/**
	 * Retrieves a string value for a given key.
	 * 
	 * @param preferenceKey		the preference key
	 * @return					the string value for the given key
	 */
	protected String getPreferenceStringValue(String preferenceKey) {
		Object value = getPreferenceValue(preferenceKey);
		
		if (value != null) {
			return value.toString();
		}
		
		return null;
	}
	
	/**
	 * Retrieves an object value for a given key.
	 * 
	 * @param preferenceKey		the preference key
	 * @return					the object value for the given key
	 */
	protected Object getPreferenceValue(String preferenceKey) {
		// get preferences
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (!sharedPreferences.contains(preferenceKey)) {
			return null;
		}
		else {
			// get and set the preference value
			Map<String, ?> preferences = sharedPreferences.getAll();
			Object kvpValue = null;
			
			for (Entry<String, ?> kvp : preferences.entrySet()) {
				if (kvp.getKey().equalsIgnoreCase(preferenceKey)) {
					kvpValue = kvp.getValue();  
				}
			}
			
			if (kvpValue != null) {
				return kvpValue;
			}
			
			return null;
		}
	}
	
	/**
	 * Set preference value for a given key.
	 * 
	 * @param preferenceKey		the preference key
	 * @param preferenceValue	the preference value
	 */
	public void setPreferenceValue(String preferenceKey, String preferenceValue) {
		// get preferences
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor editor = sharedPreferences.edit();
			
		editor.putString(preferenceKey, preferenceValue);
		editor.commit();
	}
	
	/**
	 * Remove a preference value for a given key.
	 * 
	 * @param preferenceKey		the preference key
	 */
	public void removePreferenceValue(String preferenceKey) {
		// get preferences
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (sharedPreferences.contains(preferenceKey)) {
			Editor editor = sharedPreferences.edit();
			
			editor.remove(preferenceKey);
			editor.commit();
		}
	}
}