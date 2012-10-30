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

package com.tribal.mobile.activities;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import com.tribal.mobile.R;
import com.tribal.mobile.api.tracking.TrackingHelper;
import com.tribal.mobile.util.resources.ResourceHelper;
import com.tribal.mobile.util.resources.ResourceItemType;

/**
 * Base class for activities that start other activities. Provides convenience methods for parsing urls and logging SCORM-like experienced tracking entries and starting new activities. 
 * 
 * @author Jon Brasted
 */
public abstract class BaseNavigationActivity extends BaseActivity {
	/* Methods */

	/**
	 * Provides functionality to navigate to the specified activity class, supplying the specifying parameters and intent flags in the intent.
	 * 
	 * @param context			the navigation context
	 * @param activityClass		the activity class			
	 * @param parameters		the parameters
	 * @param intentFlags		the intent flags
	 */
	public void navigate(Context context, Class<?> activityClass, Map<String, Serializable> parameters, int[] intentFlags) {
		// navigate
		Intent navigateIntent = new Intent(context, activityClass);

		if (parameters != null && !parameters.isEmpty()) {
			for (Map.Entry<String, Serializable> entry : parameters.entrySet()) {
				navigateIntent.putExtra(entry.getKey(), entry.getValue());
			}
		}

		if (intentFlags != null && intentFlags.length > 0) {
			int flags = -1;

			for (int flag : intentFlags) {
				if (flags == -1) {
					flags = flag;
				} else {
					flags = flags | flag;
				}
			}

			navigateIntent.setFlags(flags);
		}

		startActivity(navigateIntent);
	}
	
	/**
	 * Provides functionality to parse a web browser URL to determine the path relative to the package root and log a SCORM-like 'experienced' tracking entry in the native SQLite database. 
	 * 
	 * @param url	the url
	 */
	protected void parseUrlAndLogExperiencedTrack(String url) {
		// get the path to the courses folder
		int coursesFolderPathResourceId = ResourceHelper.getResourceIdByName(this, ResourceItemType.string, "externalStorageContentDirectoryPathFormatString");
		String coursesFolderPath = getString(coursesFolderPathResourceId);
		String fullCoursesFolderPath = String.format(coursesFolderPath, Environment.getExternalStorageDirectory());
		
		// remove the path string from the url
		url = url.replace(fullCoursesFolderPath, "");
		
		// get the file prefix
		String fileRegExPrefix = getString(R.string.fileRegExPrefix);

		Pattern fileRegExPrefixPattern = Pattern.compile(fileRegExPrefix);
		Matcher fileRegExPrefixPatternMatcher = fileRegExPrefixPattern.matcher(url);
		
		if (fileRegExPrefixPatternMatcher.lookingAt()) {		
			// remove the file prefix
			url = url.replaceFirst(fileRegExPrefix, "");
		} else {
			// otherwise just remove the first character from the url as that will be a slash
			url = url.substring(1);
		}
		
		// substring the url from the first slash to remove the course folder
		int indexOfSlash = url.indexOf("/");
		
		if (indexOfSlash > -1) {		
			url = url.substring(indexOfSlash);
		}
		
		// log this url
		TrackingHelper.getInstance().trackExperiencedWithMobileFrameworkSender(url);
	}
}