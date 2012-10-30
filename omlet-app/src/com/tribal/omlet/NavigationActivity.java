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

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.tribal.mobile.activities.BaseNavigationActivity;
import com.tribal.mobile.api.packages.LibraryItem;
import com.tribal.mobile.base.ClearApplicationDataCompleted;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.model.BaseContentItem;
import com.tribal.mobile.model.BookItem;
import com.tribal.mobile.model.HtmlItem;
import com.tribal.mobile.model.MenuItem;
import com.tribal.mobile.model.Package;
import com.tribal.mobile.model.VideoItem;
import com.tribal.mobile.phonegap.MFSettingsKeys;
import com.tribal.mobile.util.DialogHelper;
import com.tribal.mobile.util.NativeFileHelper;
import com.tribal.mobile.util.NativeSettingsHelper;

/**
 * Class that provides base functionality for navigating to another activity.
 * 
 * @author Jon Brasted
 */
public abstract class NavigationActivity extends BaseNavigationActivity implements ClearApplicationDataCompleted {
	/* Methods */

	@Override
	protected void onBeforePreLogout() {
		super.onBeforePreLogout();
		
		try {
			NativeSettingsHelper.getInstance(getApplicationContext()).removePreferenceValue(MFSettingsKeys.LAST_LOGGED_IN_USER);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void overridePendingTransition(int enterAnim, int exitAnim) {
		// set enter animation to be slide in left
		enterAnim = android.R.anim.slide_in_left;
		
		// set exit animation to be slide out right
		exitAnim = android.R.anim.slide_out_right;
		
		super.overridePendingTransition(enterAnim, exitAnim);
	}
	
	/**
	 * Navigate by menu item.
	 * 
	 * @param context			the context
	 * @param menuItem			the menu item
	 */
	protected void navigateByMenuItem(Context context, MenuItem menuItem) {
		navigateByMenuItem(context, menuItem, null);
	}

	/**
	 * Navigate by menu item.
	 * 
	 * @param context			the context
	 * @param menuItem			the menu item
	 * @param parameters		the parameters
	 */
	protected void navigateByMenuItem(Context context, MenuItem menuItem, Map<String, Serializable> parameters) {
		int[] intentFlags = null;
		navigateByMenuItem(context, menuItem, parameters, intentFlags, null);
	}

	/**
	 * Navigate by menu item.
	 * 
	 * @param context			the context				
	 * @param menuItem			the menu item
	 * @param parameters		the parameters
	 * @param activityClass		the activity class
	 */
	protected void navigateByMenuItem(Context context, MenuItem menuItem, Map<String, Serializable> parameters, Class<?> activityClass) {
		int[] intentFlags = null;
		navigateByMenuItem(context, menuItem, parameters, intentFlags, activityClass);
	}

	/**
	 * Navigate by menu item.
	 * 
	 * @param context			the context
	 * @param menuItem			the menu item
	 * @param parameters		the parameters
	 * @param intentFlags		the intent flags
	 * @param activityClass		the activity class
	 */
	protected void navigateByMenuItem(Context context, MenuItem menuItem, Map<String, Serializable> parameters, int[] intentFlags, Class<?> activityClass) {
		boolean isPhoneGapNavigate = false;
		
		if (activityClass == null) {

			// determine the activity class
			switch (menuItem.getType()) {
				case menu: {
					activityClass = ListMenuActivity.class;
					break;
				}
				case link: {
					BaseContentItem contentItem = menuItem.getLinkedContentItem();
					Class<? extends BaseContentItem> contentItemClass = contentItem.getClass();
					
					if (HtmlItem.class.equals(contentItemClass)) {
						activityClass = PhoneGapActivity.class;
						
						isPhoneGapNavigate = true;
						break;

					} else {
						if (BookItem.class.equals(contentItemClass) || VideoItem.class.equals(contentItemClass)) {
							navigateToNativeFileHandler(contentItem);
							return;
						}

						Log.e("NavigationActivity", "BaseContentItem type " + contentItem.getClass() + " is not currently supported in navigateByContentItem().");
						return;
					}
				}
				default: {
					Log.e("NavigationActivity", "menu item type " + menuItem.getType() + " is not currently supported in navigateByMenuItem().");
					return;
				}
			}
		}

		// create parameter map
		Map<String, Serializable> parameterMap;

		// check to see if the menu item has been supplied
		if (parameters == null) {
			parameterMap = new HashMap<String, Serializable>();
		} else {
			parameterMap = parameters;
		}

		// add menu item
		if (!parameterMap.containsKey(IntentParameterConstants.MenuItem)) {
			parameterMap.put(IntentParameterConstants.MenuItem, menuItem);
		}
		
		if (isPhoneGapNavigate && !parameterMap.containsKey("loadUrlTimeoutValue")) {
			parameterMap.put("loadUrlTimeoutValue", 60000);
		}

		// navigate
		navigate(context, activityClass, parameterMap, intentFlags);
	}

	/**
	 * Navigate by content item.
	 * 
	 * @param context			the context
	 * @param contentItem		the content item
	 */
	public void navigateByContentItem(Context context, BaseContentItem contentItem) {
		navigateByContentItem(context, contentItem, null);
	}

	/**
	 * Navigate by content item.
	 * 
	 * @param context			the context
	 * @param contentItem		the content item
	 * @param parameterMap		the parameters
	 */
	public void navigateByContentItem(Context context, BaseContentItem contentItem, Map<String, Serializable> parameterMap) {
		// check the content item
		Class<? extends BaseContentItem> contentItemClass = contentItem.getClass();
		
		if (BookItem.class.equals(contentItemClass) || VideoItem.class.equals(contentItemClass)) {
			// open book or video
			navigateToNativeFileHandler(contentItem);

		} else {
			// use standard navigation logic
		
			if (parameterMap == null) {
				parameterMap = new HashMap<String, Serializable>();
			}
	
			// check the content item
	
			Class<?> activityClass = getContentItemActivityClass(contentItem);
	
			// add content item
			if (!parameterMap.containsKey(IntentParameterConstants.ResourceItem)) {
				parameterMap.put(IntentParameterConstants.ResourceItem, contentItem);
			}
			
			if (!parameterMap.containsKey("loadUrlTimeoutValue")) {
				parameterMap.put("loadUrlTimeoutValue", 60000);
			}
	
			// navigate
			navigate(context, activityClass, parameterMap, null);
		}
	}
	
	private void navigateToNativeFileHandler(BaseContentItem baseContentItem) {
		String externalStorageFilePath = String.format(getString(R.string.external_storage_packages_path_format_string), baseContentItem.getPathWithPackage()); 
		externalStorageFilePath = Environment.getExternalStorageDirectory() + externalStorageFilePath;
		
		// create file
		File file = new File(externalStorageFilePath);

		// check that the file exists
		if (!file.exists()) {
			// show error
			String appName = getString(R.string.app_name);
			String message = getString(R.string.file_not_found_message);
			
			DialogHelper.showAlertDialog(this, appName, message);
			
			// return
			return;
		}
		
		if (NativeFileHelper.canOpenFile(this, file)) {
			// fire the intent to open the file
			Intent intent = NativeFileHelper.getOpenFileIntent(file);
			
			// parse and log experienced track
			parseUrlAndLogExperiencedTrack(file.getAbsolutePath());
			
			startActivity(intent);
		} else {
			// show alert message
			String title = getString(R.string.native_file_open_no_app_message_title);
			String messageFormatString = getString(R.string.native_file_open_no_app_message_message);

			// get the file extension
			String fileExtension = NativeFileHelper.getFileExtensionFromPath(file.getAbsolutePath());

			// get file extension without the period
			String fileExtensionWithoutPeriod = fileExtension.substring(1);

			// get formatted message
			String formattedMessage = String.format(messageFormatString, fileExtensionWithoutPeriod);

			DialogHelper.showAlertDialog(this, title, formattedMessage);
		}
	}

	private Class<?> getContentItemActivityClass(BaseContentItem contentItem) {
		// determine the activity class
		Class<?> activityClass = null;

		if (contentItem.getClass().equals(HtmlItem.class)) {
			activityClass = PhoneGapActivity.class;
		}

		return activityClass;
	}

	@Override
	protected void redirectToLogin() {
		Intent loginIntent = new Intent(this, LoginActivity.class);
		startActivity(loginIntent);		
	}
	
	/**
	 * Open a {@link Package} object.
	 * 
	 * @param packageObject		the package object			
	 * @param libraryItem		the library item
	 */
	protected void openPackage(Package packageObject, LibraryItem libraryItem) {
		// get the entry point

		String entryPoint = packageObject.getEntryPoint();

		// get the first menu item
		com.tribal.mobile.model.MenuItem menuItem = null;

		if (StringUtils.isNotEmpty(entryPoint)) {
			menuItem = packageObject.findMenuItemByPath(entryPoint);
		} else {
			menuItem = packageObject.findFirstMenuItem();
		}

		HashMap<String, Serializable> parameterMap = new HashMap<String, Serializable>();
		parameterMap.put(IntentParameterConstants.PackageName, libraryItem.getName());
		
		if (menuItem != null) {
			navigateByMenuItem(this, menuItem, parameterMap);
		} else {
			// now try and get the content item

			BaseContentItem contentItem = null;

			if (StringUtils.isNotEmpty(entryPoint)) {
				contentItem = packageObject.findContentItemById(entryPoint);
			}

			if (contentItem != null) {
				navigateByContentItem(this, contentItem, parameterMap);
			} else {
				showErrorDialog(getString(R.string.packageErrorTitle), getString(R.string.packageNoMenuError));
			}
		}
	}
}