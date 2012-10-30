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

package com.tribal.mobile.phonegap;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;
import com.tribal.mobile.Framework;
import com.tribal.mobile.R;
import com.tribal.mobile.activities.BasePhoneGapActivity;
import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.api.packages.PackageItemResult;
import com.tribal.mobile.api.packages.PackageItemRetrieveCompleted;
import com.tribal.mobile.api.packages.PackageManager;
import com.tribal.mobile.api.packages.PackageMenuItemResult;
import com.tribal.mobile.api.packages.PackageResourceItemResult;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.util.AsyncTaskHelper;

/**
 * Default client PhoneGap plugin implementation.
 * 
 * @author Jon Brasted 
 */
public class DefaultClientPluginImplementation extends Plugin implements ClientPlugin, PackageItemRetrieveCompleted, GetCourseLocalPathRootCompleted, InitialiseCurrentCourseLocalTempFolderCompleted, PhoneGapOperationNoResultCompleted {
	/* Fields */
	
	private static final String OPEN_MENU_ITEM = "openMenuItem";
	private static final String OPEN_RESOURCE = "openResource";
	private static final String SET_VALUE = "setValue";
	private static final String GET_VALUE = "getValue";
	private static final String GET_USER_USERNAME = "getUserUsername";
	private static final String TRACK = "track";
	private static final String SYNC = "sync";
	private static final String LOGOUT = "logout";
	private static final String GET_COURSE_LOCAL_PATH_ROOT = "getCourseLocalPathRoot";
	private static final String GET_CURRENT_COURSE_LOCAL_PATH_ROOT = "getCurrentCourseLocalPathRoot";
	private static final String INITIALISE_CURRENT_COURSE_LOCAL_TEMP_FOLDER = "initializeCurrentCourseLocalTempFolder";
	private static final String CLEAR_CURRENT_COURSE_LOCAL_TEMP_FOLDER = "clearCurrentCourseLocalTempFolder";
	
	/* Methods */
	
	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		if (OPEN_MENU_ITEM.equalsIgnoreCase(action)) {
			openMenuItemFromJS(data, callbackId);
		
		} else if (OPEN_RESOURCE.equalsIgnoreCase(action)) {
			openResourceFromJS(data, callbackId);
		
		} else if (SET_VALUE.equalsIgnoreCase(action)) {
			setValueFromJS(data, callbackId);
			
		} else if (GET_VALUE.equalsIgnoreCase(action)) {
			getValueFromJS(data, callbackId);
			
		} else if (GET_USER_USERNAME.equalsIgnoreCase(action)) {
			getUserUsernameFromJS(data, callbackId);
			
		} else if (TRACK.equalsIgnoreCase(action)) {
			trackFromJS(data, callbackId);
			
		} else if (SYNC.equalsIgnoreCase(action)) {
			syncFromJS(data, callbackId);
			
		}
		else if (LOGOUT.equalsIgnoreCase(action)) {
			logoutFromJS(data, callbackId);
		
		}
		else if (GET_COURSE_LOCAL_PATH_ROOT.equalsIgnoreCase(action)) {
			getCourseLocalPathRootFromJS(data, callbackId);
		
		}
		else if (GET_CURRENT_COURSE_LOCAL_PATH_ROOT.equalsIgnoreCase(action)) {
			getCurrentCourseLocalPathRootFromJS(data, callbackId);
		
		}
		else if (INITIALISE_CURRENT_COURSE_LOCAL_TEMP_FOLDER.equalsIgnoreCase(action)) {
			initialiseCurrentCourseLocalTempFolderFromJS(data, callbackId);
			
		}
		else if (CLEAR_CURRENT_COURSE_LOCAL_TEMP_FOLDER.equalsIgnoreCase(action)) {
			clearCurrentCourseLocalTempFolderFromJS(data, callbackId);
			
		}
		
		PluginResult pluginResult = new PluginResult(Status.NO_RESULT);
		
		// set keep callback to true so we can asynchronously invoke it when the value is returned
		pluginResult.setKeepCallback(true);
		return pluginResult;
	}
	
	/**
	 * Opens a native menu on the application.
	 *
	 * @param menuItemId The full path to the menu item, starting with the package id. For example: shell.root.1.2
	 */
	@Override
	public void openMenuItem(String menuItemId, String callback) {
		if (this.ctx != null) {
		    // fire loading broadcast
			Intent showLoadingDialogBroadcast = new Intent();
			showLoadingDialogBroadcast.setAction(BroadcastActions.ShowLoadingDialog);
			this.ctx.sendBroadcast(showLoadingDialogBroadcast);
			
			BasePhoneGapActivity phoneGapActivity = (BasePhoneGapActivity)this.ctx;
			
			// get the package item
			PackageManager.getInstance(phoneGapActivity.getApplicationContext(), phoneGapActivity.getDatabaseHelper()).getMenuItem(menuItemId, this, callback);
        }
	}
	
	/* Open a native menu on the application. */
	public void openMenuItemFromJS(JSONArray data, String callback) {
		if (data != null && data.length() == 1 && !data.isNull(0)) {
			// get the menu item id
			try {
				String menuItemId = data.getString(0);
				
				if (menuItemId != null && StringUtils.isNotEmpty(menuItemId)) {
					openMenuItem(menuItemId, callback);
				}				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Opens a resource on the application.
	 * The resource called can be contained in the current package or another package, but is referenced with its full name in both cases.
	 *
	 * @param menuItemId The full path to the resource, starting with the package id. For example: shell.root.1.2
	 */
	@Override
	public void openResource(String resourceId, String callback) {
		if (this.ctx != null) {
		    // fire loading broadcast
			Intent showLoadingDialogBroadcast = new Intent();
			showLoadingDialogBroadcast.setAction(BroadcastActions.ShowLoadingDialog);
			this.ctx.sendBroadcast(showLoadingDialogBroadcast);
			
			BasePhoneGapActivity phoneGapActivity = (BasePhoneGapActivity)this.ctx;
			
			// get the package item
			PackageManager.getInstance(phoneGapActivity.getApplicationContext(), phoneGapActivity.getDatabaseHelper()).getResourceItem(resourceId, this, callback);
        }
	}
	
	/* Open a resource on the application. */
	public void openResourceFromJS(JSONArray data, String callback) {
		if (data != null && data.length() == 1 && !data.isNull(0)) {
			// get the resource id
			try {
				String resourceId = data.getString(0);
				
				if (resourceId != null && StringUtils.isNotEmpty(resourceId)) {
					openResource(resourceId, callback);
				}				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Sets a value for a key on the native storage. Native storage is provided by the framework. It consists in a way to store key/value pairs. Like the HTML5 DB, this kind of storage is cleaned up automatically if the user removes the application. This storage should be used specially if you want to share data between the HTML content and native side of the application, otherwise we highly recommend use HTML5 DB. There are two ways of store data on the native storage, globally and content-specific.
	 * <p>
	 *   <strong>Globally</strong>
	 *   It's global on the application. The key can be retrieved from any content in any part of the system, as well as in any part of the native code. You must take care that the key can be used by other content developer. Below you can find the API calls to store and retrieve key/value pairs.
	 *   <ul>
	 *     <li>clientPlugin.setValue(MFStoreType.GLOBAL, key, value, callback);</li>
	 *   </ul>
	 * </p>
	 * <p>
	 *   <strong>Content-specific</strong>
	 *   It's connected to the current resource/menu-item. Below you can find the API calls to store and retrieve key/value pairs.
	 *   <ul>
	 *     <li>clientPlugin.setValue(MFStoreType.SPECIFIC, key, value, callback);</li>
	 *   </ul>
	 * </p>
	 * 
	 * @param type		The type of the native storage.
	 * @param key		The key name.
	 * @param value		The value.
	 * @param callback	The callback function to confirm the operation.
	 */
	@Override
	public void setValue(String type, String key, String value, final String callback) {
		AsyncTask<Object, Void, Boolean> setValueWorkerTask = new AsyncTask<Object, Void, Boolean>() {
			String key = null;
			
			@Override
			protected Boolean doInBackground(Object... params) {
				if (params.length == 3) {				
					// get the type
					String type = (String)params[0];
					
					// get the key
					key = (String)params[1];
					
					// get the value
					String value = (String)params[2];
					
					// call client to set value
					return Framework.getClient().setValue(type, key, value);
				}
				
				return false;
			}
			
			@Override
			protected void onPostExecute(Boolean value) {
				// invoke callback
				
				if (callback != null) {
					// create plugin result
					PluginResult pluginResult = new PluginResult(com.phonegap.api.PluginResult.Status.OK, value);
					pluginResult.setKeepCallback(false);
					
					// fire callback
					success(pluginResult, callback);
		        }
			}
		};
		
		AsyncTaskHelper.executeAsyncTask(setValueWorkerTask, type, key, value);
	}
	
	/* Sets a value for a key on the native storage. */
	public void setValueFromJS(JSONArray data, String callback) {
		if (data != null && data.length() == 3 && !data.isNull(0) && !data.isNull(1) && !data.isNull(2)) {
			// get the item
			try {
				String type = data.getString(0);
				String key = data.getString(1);
				String value = data.getString(2);
				
				if (type != null &&
					StringUtils.isNotEmpty(type) &&
					StringUtils.isNotEmpty(key) &&
					StringUtils.isNotEmpty(value)) {
					setValue(type, key, value, callback);
				}			
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Gets a value for a key on the native storage. Native storage is provided by the framework. It consists in a way to store key/value pairs. Like the HTML5 DB, this kind of storage is cleaned up automatically if the user removes the application. This storage should be used specially if you want to share data between the HTML content and native side of the application, otherwise we highly recommend use HTML5 DB. There are two ways of store data on the native storage, globally and content-specific.
	 *
	 * <p>
	 *   <strong>Globally</strong>
	 * It's global on the application. The key can be retrieved from any content in any part of the system, as well as in any part of the native code. You must take care that the key can be used by other content developer. Below you can find the API calls to store and retrieve key/value pairs.
	 *   <ul>
	 *     <li>clientPlugin.getValue(MFStoreType.GLOBAL, key, callback);</li>
	 *   </ul>
	 * </p>
	 * <p>
	 *   <strong>Content-specific</strong>
	 * It's connected to the current resource/menu-item. Below you can find the API calls to store and retrieve key/value pairs.
	 *   <ul>
	 *     <li>clientPlugin.getValue(MFStoreType.SPECIFIC, key, callback);</li>
	 *   </ul>
	 * </p>
	 * 
	 * @param type		The type of the native storage.
	 * @param key		The key name.
	 * @param callback	The callback function. It will receive an object with the value.
	 */		
	@Override
	public void getValue(String type, String key, final String callback) {
		AsyncTask<Object, Void, String> getValueWorkerTask = new AsyncTask<Object, Void, String>() {

			String key = null;
			
			@Override
			protected String doInBackground(Object... params) {
				if (params.length == 2) {				
					// get the type
					String type = (String)params[0];
					
					// get the key
					key = (String)params[1];
					
					// call client to get value
					return Framework.getClient().getValue(type, key);
				}
				
				return null;
			}
			
			@Override
			protected void onPostExecute(String value) {
				if (callback != null) {
					// create plugin result
					PluginResult pluginResult = new PluginResult(com.phonegap.api.PluginResult.Status.OK, value);
					pluginResult.setKeepCallback(false);
					
					// fire callback
					success(pluginResult, callback);
		        }
			}
		};
		
		AsyncTaskHelper.executeAsyncTask(getValueWorkerTask, type, key);
	}
	
	/* Gets a value for a key from the native storage. */
	public void getValueFromJS(JSONArray data, String callback) {
		if (data != null && data.length() == 2 && !data.isNull(0) && !data.isNull(1)) {
			// get the item
			try {
				String type = data.getString(0);
				String key = data.getString(1);
				
				if (type != null &&
					StringUtils.isNotEmpty(type) &&
					StringUtils.isNotEmpty(key)) {
					getValue(type, key, callback);
				}			
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Retrieves the current user username.
	 *
	 * @param callback	The callback function. It will receive an object with the username.
	 */
	@Override
	public void getUserUsername(String callback) {
		String username = Framework.getClient().getUserUsername();
		
		if (callback != null) {
			// create plugin result
			PluginResult pluginResult = new PluginResult(Status.OK, username);
			pluginResult.setKeepCallback(false);
			
			// fire callback
			this.success(pluginResult, callback);
        }
	}
	
	/* Retrieves the current user username. */
	public void getUserUsernameFromJS(JSONArray data, String callback) {
		getUserUsername(callback);
	}
	
	/**
	 * Forces sync.
	 *
	 * @param callback	The callback function. It will receive an object with the device information.
	 */
	@Override
	public void sync(String callback) {
		// call sync on the client plugin.
		Framework.getClient().sync();
		
		if (callback != null) {
			// create plugin result
			PluginResult pluginResult = new PluginResult(Status.OK);
			pluginResult.setKeepCallback(false);
			
			// fire callback
			this.success(pluginResult, callback);
        }
	}
	
	/* Forces sync. */
	public void syncFromJS(JSONArray data, String callback) {
		sync(callback);
	}

	/**
	 * Tracks a content-specific thing. This method must be called by content developers to track anything they want. Everything tracked by this method will be connected to the current object id (resource or menu-item).
	 *
	 * @param sender			The sender. This will be 'mf' for internal mobile framework calls. It's a string value.
	 * @param additionalInfo	The track information. It's a string value, can contain anything you want.
	 * @param callback			The callback function. It will receive an object with the device information.
	 */
	@Override
	public void track(String sender, String additionalInfo, String callback) {
		// call into client tracking method
		Framework.getClient().track(sender, additionalInfo);
		
		if (callback != null) {
			// create plugin result
			PluginResult pluginResult = new PluginResult(Status.OK);
			pluginResult.setKeepCallback(false);
			
			// fire callback
			this.success(pluginResult, callback);
        }
	}
	
	/* Tracks a content-specific thing. This method must be called by content developers to track anything they want. Everything tracked by this method will be connected to the current object id (resource or menu-item). */
	public void trackFromJS(JSONArray data, String callback) {
		if (data != null && data.length() == 2) {
			// get the sender and additional info
			try {
				String sender = !data.isNull(0) ? data.getString(0) : null;
				String additionalInfo = !data.isNull(1) ? data.getString(1) : null;
				
				track(sender, additionalInfo, callback);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Log out the current user and redirect the user to the login screen.
	 * 
	 * @param callback	The callback function.
	 */
	@Override
	public void logout(String callback) {
		// call logout on the client plugin
		Framework.getClient().logout();
		
		if (callback != null) {
			// create plugin result
			PluginResult pluginResult = new PluginResult(Status.OK);
			pluginResult.setKeepCallback(false);
			
			// fire callback
			this.success(pluginResult, callback);
        }
	}
	
	/* Log out the current user and redirects the user to the login screen. */
	public void logoutFromJS(JSONArray data, String callback) {
		logout(callback);
	}
	
	/**
	 * Retrieves the local path root for a particular course.
	 * 
	 * @param courseId		The course id.
	 * @param callback		The callback function. If successful, it will receive the local path. If it is unsuccessful, it will receive an error message. 
	 */
	@Override
	public void getCourseLocalPathRoot(String courseId, String callback) {
		Framework.getClient().getCourseLocalPathRoot(courseId, callback, this);
	}

	/* Retrieves the local path root for a particular course. */
	private void getCourseLocalPathRootFromJS(JSONArray data, String callback) {
		if (data != null && data.length() == 1) {
			// get the course id
			try {
				String courseId = !data.isNull(0) ? data.getString(0) : null;
				
				getCourseLocalPathRoot(courseId, callback);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Retrieves the local path root for the currently opened course.
	 * 
	 * Callback will return a JSON object with the following key and value pairs:
	 *  courseId		The course id for the currently opened course.
	 *  localPathRoot	The full local path to the course directory root. E.g. /mnt/sdcard/.../{uniqueCourseFolderId}
	 * 
	 * Error callback will return a JSON object with the following key and value pairs:
	 *  error			An error string to determine why the native method call failed.
	 * 
	 * @param callback		The callback function.
	 */
	@Override
	public void getCurrentCourseLocalPathRoot(String callback) {
		Framework.getClient().getCurrentCourseLocalPathRoot(callback, this);
	}
	
	/* Retrieves the local path root for the currently opened course. */
	private void getCurrentCourseLocalPathRootFromJS(JSONArray data, String callback) {
		getCurrentCourseLocalPathRoot(callback);
	}
	
	/**
	 * Initialises a temporary folder for the currently opened course.
	 * If the folder already exists, the folder will be cleared. If it does not exist, it will be created.
	 * 
	 * Callback will return a JSON object with the following key and value pairs:
	 * 	tempFolderPath	The full local path to the temporary folder in the course directory. E.g. /mnt/sdcard/.../{uniqueCourseFolderId}/temp
	 * 
	 * Error callback will return a JSON object with the following key and value pairs:
	 *  error			An error string to determine why the native method call failed.
	 * 
	 * @param callback		The callback function.
	 * @param errorCallback	The error callback function.
	 */
	@Override
	public void initialiseCurrentCourseLocalTempFolder(String callback) {
		Framework.getClient().initialiseCurrentCourseLocalTempFolder(callback, this);
	}
	
	/* Initialises a temporary folder for the currently opened course. */
	private void initialiseCurrentCourseLocalTempFolderFromJS(JSONArray data, String callback) {
		initialiseCurrentCourseLocalTempFolder(callback);
	}
	
	/**
	 * Clears the temporary folder for the currently opened course.
	 * If the folder exists, the folder will be cleared and removed. If the folder does not exist, an error will occur.
	 * 
	 * Callback will not return a value and will be invoked when the operation has finished.
	 * 
	 * Error callback will return a JSON object with the following key and value pairs:
	 *  error			An error string to determine why the native method call failed.
	 * 
	 * @param callback		The callback function.
	 * @param errorCallback	The error callback function.
	 */
	@Override
	public void clearCurrentCourseLocalTempFolder(String callback) {
		Framework.getClient().clearCurrentCourseLocalTempFolder(callback, this);
	}
	
	/* Clears the temporary folder for the currently opened course. */
	private void clearCurrentCourseLocalTempFolderFromJS(JSONArray data, String callback) {
		clearCurrentCourseLocalTempFolder(callback);
	}
	
	@Override
	public void onPackageItemRetrieveCompleted(final PackageItemResult result, String callback) {
		// hide loading dialog
		Intent dismissLoadingDialogIntent = new Intent();
		dismissLoadingDialogIntent.setAction(BroadcastActions.DismissLoadingDialog);
		this.ctx.sendBroadcast(dismissLoadingDialogIntent);
		
		if (!result.hasError()) {
			if (result.hasPackageToDownload()) {
				// show a dialog instead
				showDownloadConfirmationDialog(result, callback);
				return;
			}
			
			if (callback != null) {
				// create plugin result
				PluginResult pluginResult = new PluginResult(Status.OK);
				pluginResult.setKeepCallback(false);
				
				// fire callback
				this.success(pluginResult, callback);
	        }
			
			Intent broadcast = new Intent();
		    
			if (result instanceof PackageMenuItemResult) {			
			    // set open menu action
			    broadcast.setAction(BroadcastActions.OpenMenuItem);
			    
			    PackageMenuItemResult packageMenuItemResult = (PackageMenuItemResult)result;
			    
			    // add menu item parameter
			    broadcast.putExtra(IntentParameterConstants.MenuItem, packageMenuItemResult.getMenuItem());
			} else if (result instanceof PackageResourceItemResult) {
			    // set open menu action
			    broadcast.setAction(BroadcastActions.OpenResource);
			    
		    	PackageResourceItemResult packageResourceItemResult = (PackageResourceItemResult)result;
			    
			    // add menu item parameter
			    broadcast.putExtra(IntentParameterConstants.ResourceItem, packageResourceItemResult.getResourceItem());
			}
					    
			// send broadcast
			this.ctx.sendBroadcast(broadcast);
		} else if (result.hasError()) {		
			if (callback != null) {
				// create plugin result
				PluginResult pluginResult = new PluginResult(Status.ERROR, result.getError());
				pluginResult.setKeepCallback(false);
				
				// fire callback
				this.error(pluginResult, callback);
	        }
		}
	}

	private void showDownloadConfirmationDialog(final PackageItemResult result, final String callback) {
		final PackageItem packageItem = result.getPackageToDownload();
		
		String downloadConfirmationTitle = this.ctx.getString(R.string.dialog_downloadCourseConfirmDialogTitle);
		
		String downloadConfirmationMessage = this.ctx.getString(R.string.dialog_downloadCourseReferencedItemConfirmDialogTitle);
		downloadConfirmationMessage = String.format(downloadConfirmationMessage, packageItem.getName());
		
		String continueString = this.ctx.getString(R.string.button_continue);		
		String cancelString = this.ctx.getString(R.string.button_cancel);
		
		AlertDialog alertDialog = new AlertDialog.Builder(this.ctx).create();
		alertDialog.setCancelable(false);
		alertDialog.setTitle(downloadConfirmationTitle);
		alertDialog.setMessage(downloadConfirmationMessage);

		alertDialog.setButton(continueString, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();

				// fire OK callback
				PluginResult pluginResult = new PluginResult(Status.OK);
				pluginResult.setKeepCallback(false);
				success(pluginResult, callback);
				
				// redirect to the package details screen
				if (ctx instanceof BasePhoneGapActivity) {
					BasePhoneGapActivity pgActivity = (BasePhoneGapActivity)ctx;
					pgActivity.navigateToPackageDetails(packageItem);
				}
			}
		});

		alertDialog.setButton2(cancelString, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Cancel and do not logout
				dialog.cancel();
				
				// fire OK callback
				PluginResult pluginResult = new PluginResult(Status.OK);
				pluginResult.setKeepCallback(false);
				success(pluginResult, callback);
			}
		});

		alertDialog.show();
	}

	@Override
	public void onGetCourseLocalPathRootCompleted(String courseId, String localPathRoot, String error, String callback) {
		if (!StringUtils.isEmpty(callback)) {		
			PluginResult pluginResult = null;
			
			if (StringUtils.isEmpty(error)) {
				// send success callback
				JSONObject jsonObject = new JSONObject();
				
				try {
					jsonObject.put("courseId", courseId);
					jsonObject.put("localPathRoot", localPathRoot);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				// fire success callback
				pluginResult = new PluginResult(Status.OK, jsonObject);
				pluginResult.setKeepCallback(false);
				
				this.success(pluginResult, callback);
				
			} else {
				// send error callback
				JSONObject jsonObject = new JSONObject();
				
				try {
					jsonObject.put("courseId", courseId);
					jsonObject.put("error", error);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				// fire error callback
				pluginResult = new PluginResult(Status.ERROR, jsonObject);
				pluginResult.setKeepCallback(false);
				
				this.error(pluginResult, callback);
			}
		}
	}

	@Override
	public void onInitialiseCurrentCourseLocalTempFolderCompleted(String tempFolderPath, String error, String callback) {
		if (!StringUtils.isEmpty(callback)) {		
			PluginResult pluginResult = null;
			
			if (StringUtils.isEmpty(error)) {
				// fire success callback
				JSONObject jsonObject = new JSONObject();
				
				try {
					jsonObject.put("tempFolderPath", tempFolderPath);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				pluginResult = new PluginResult(Status.OK, jsonObject);
				pluginResult.setKeepCallback(false);
				
				this.success(pluginResult, callback);
				
			} else {
				// send error callback
				JSONObject jsonObject = new JSONObject();
				
				try {
					jsonObject.put("error", error);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				// fire error callback
				pluginResult = new PluginResult(Status.ERROR, jsonObject);
				pluginResult.setKeepCallback(false);
				
				this.error(pluginResult, callback);
			}
		}
		
	}
	
	@Override
	public void onPhoneGapOperationNoResultCompleted(String error, String callback) {
		if (!StringUtils.isEmpty(callback)) {		
			PluginResult pluginResult = null;
			
			if (StringUtils.isEmpty(error)) {
				// fire success callback
				pluginResult = new PluginResult(Status.OK);
				pluginResult.setKeepCallback(false);
				
				this.success(pluginResult, callback);
				
			} else {
				// send error callback
				JSONObject jsonObject = new JSONObject();
				
				try {
					jsonObject.put("error", error);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				// fire error callback
				pluginResult = new PluginResult(Status.ERROR, jsonObject);
				pluginResult.setKeepCallback(false);
				
				this.error(pluginResult, callback);
			}
		}
	}
}