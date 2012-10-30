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

package com.tribal.mobile.api.tracking;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.tribal.mobile.Framework;
import com.tribal.mobile.util.database.BaseDatabaseHelper;

/**
 * Utility class to help facilitate creation and logging of tracking entries.
 * 
 * @author Jon Brasted
 */
public class TrackingHelper {
	/* Fields */
	
	private static String MOBILE_FRAMEWORK_KEY = "mf";
	private static String SYNC_KEY = "sync";
	private static String EXPERIENCED_KEY = "experienced";
	private static TrackingHelper instance;
	
	/* Properties */
	
	public static TrackingHelper getInstance() {
		if (instance == null) {
			Log.e("TrackingHelper", "Not initialised.");
		}
		
		return instance;
	}
	
	/* Constructor */
	
	public TrackingHelper(BaseDatabaseHelper databaseHelper) {
		// persist the database helper
		TrackingHelper.instance = this;
	}

	/* Methods */
	
	/**
	 * Provides functionality to create and log a SCORM-like 'experienced' tracking entry.
	 * 
	 * @param url	url to track. This will be converted into a url that is relative to the current open package's root directory.
	 */
	public void trackExperiencedWithMobileFrameworkSender(String url) {
		JSONArray jsonArray = createAdditionalInfoForExperiencedTrack(url);
		
		Framework.getClient().track(MOBILE_FRAMEWORK_KEY, jsonArray.toString());
	}
	
	/**
	 * Provides functionality to create 'additional info' JSON object for a tracking entry for an 'experienced' event.
	 * 
	 * @param url	url to track		
	 * @return		'additional info' JSON object for a tracking entry for an 'experienced' event
	 */
	private JSONArray createAdditionalInfoForExperiencedTrack(String url) {
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		
		try {
			jsonObject.put(EXPERIENCED_KEY, url);
			jsonArray.put(jsonObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return jsonArray;
	}
	
	/**
	 * Provides functionality to track a synchronization event with the 'sender' of 'mf' (Mobile Framework).
	 */
	public void trackSyncWithMobileFrameworkSender() {
		JSONArray jsonArray = createAdditionalInfoForSync();
		
		Framework.getClient().track(MOBILE_FRAMEWORK_KEY, jsonArray.toString());
	}
	
	/**
	 * Provides functionality to create 'additional info' JSON object for a tracking entry for a synchronization event.
	 * 
	 * @return	'additional info' JSON object for a tracking entry for a synchronization event
	 */
	private JSONArray createAdditionalInfoForSync() {
		String deviceInfo = "android " + android.os.Build.VERSION.RELEASE;
		
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		
		try {
			jsonObject.put(SYNC_KEY, deviceInfo);
			jsonArray.put(jsonObject); 
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return jsonArray;
	}
}