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

package com.tribal.omlet.framework;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import com.tribal.omlet.R;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import com.google.gson.Gson;
import com.tribal.mobile.AbstractFramework;
import com.tribal.mobile.Framework;
import com.tribal.mobile.api.Client;
import com.tribal.mobile.api.login.LoginCompleted;
import com.tribal.mobile.api.packages.Catalogue;
import com.tribal.mobile.api.packages.LibraryItem;
import com.tribal.mobile.api.packages.PackageCatalogueRetrieved;
import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.api.packages.PackageManager;
import com.tribal.mobile.api.tracking.TrackingHelper;
import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.model.BaseContentItem;
import com.tribal.mobile.model.MenuItem;
import com.tribal.mobile.net.AuthHttpConnection;
import com.tribal.mobile.net.HandlerCallback;
import com.tribal.mobile.net.HttpConnection.Callback;
import com.tribal.mobile.phonegap.GetCourseLocalPathRootCompleted;
import com.tribal.mobile.phonegap.InitialiseCurrentCourseLocalTempFolderCompleted;
import com.tribal.mobile.phonegap.MFSettingsKeys;
import com.tribal.mobile.phonegap.MFStoreType;
import com.tribal.mobile.phonegap.PhoneGapOperationNoResultCompleted;
import com.tribal.mobile.util.FileHelper;
import com.tribal.mobile.util.NativeSettingsHelper;
import com.tribal.mobile.util.PackageHelper;
import com.tribal.mobile.util.ServiceLayerExceptionHelper;
import com.tribal.mobile.util.database.BaseDatabaseHelper;

/**
 * Omlet implementation of {@link AbstractFramework} and {@link OmletServer}.
 * 
 * @author Jon Brasted
 */
public class OmletFramework extends AbstractFramework implements OmletServer {
	/* Fields */
	
	private Context context;
	private BaseDatabaseHelper databaseHelper;
	private String userId;
	private String username;
	private String password;

	private MenuItem currentMenuItem;
	private BaseContentItem currentResourceItem;

	/* Properties */
	
	public BaseApplication getBaseApplication() {
		return (BaseApplication) context;
	}

	/* Constructor */
	
	public OmletFramework(BaseApplication baseApplication, BaseDatabaseHelper databaseHelper) {
		this.context = baseApplication.getApplicationContext();
		this.databaseHelper = databaseHelper;
		
		// instantiate the tracking helper
		new TrackingHelper(databaseHelper);
	}
	
	/* Methods */

	// JB. 25/05/2012.
	// Workaround to keep the app running if the PhoneGap activity crashes and wipes out the Framework singleton.
	public static void reinstatiateJkoFramework(BaseApplication baseApplication, BaseDatabaseHelper databaseHelper) {
		new OmletFramework(baseApplication, databaseHelper);

		// get username
		final String username = NativeSettingsHelper.getInstance(baseApplication.getApplicationContext()).checkAndGetNativeSetting(MFSettingsKeys.LAST_LOGGED_IN_USER);
		String password = baseApplication.getString(R.string.login_hardcoded_passcode);

		try {
			// reauthenticate
			Framework.getServer().login(username, password, new LoginCompleted() {
				@Override
				public void onLoginCompleted(boolean authenticated) {
					Client client = Framework.getClient();
					
					if (client instanceof OmletFramework) {
						if (authenticated) {
							((OmletFramework)client).updateUserUsername(username);
						} else {
							((OmletFramework)client).updateUserUsername(null);
						}
					}
				}
			});
		} catch (Exception e) {
			Client client = Framework.getClient();
			
			if (client instanceof OmletFramework) {
				((OmletFramework)client).updateUserUsername(null);
			}
		}
	}

	@Override
	public void login(String username, String password,
			final LoginCompleted callback) {
		this.username = username;
		this.password = password;

		// get validate url
		String validateUrl = context.getString(R.string.authenticationUrl);

		AuthHttpConnection.post(validateUrl, "", new HandlerCallback(
				new Callback() {
					@Override
					public void onSuccess(String data) {
						try {
							final Gson json = new Gson();
							@SuppressWarnings("unchecked")
							Map<String, Object> obj = json.fromJson(data, Map.class);
							boolean authenticated = (Boolean) obj.get("success");
							if (authenticated == Boolean.TRUE) {
								Double d = (Double) obj.get("userId");
								userId = String.valueOf(d.intValue());
							}
							callback.onLoginCompleted(authenticated);
						} catch (Throwable t) {
							onError(t);
						}
					}

					@Override
					public void onStart() {
					}

					@Override
					public void onError(Throwable t) {
						// clear the username
						Client client = Framework.getClient();
						
						if (client instanceof OmletFramework) {
							((OmletFramework) Framework.getClient()).updateUserUsername(null);
						}
						
						// process exception
						ServiceLayerExceptionHelper.getInstance().processException(t, context);
						
						// invoke callback
						callback.onLoginCompleted(false);
					}
				}), true, context);
	}

	@Override
	public String getUserId() {
		return userId;
	}

	@Override
	public String getUserUsername() {
		return username;
	}

	/**
	 * Update the Client's username. This method should only be used by
	 * LoginActivity when retrieving the username from persisted storage.
	 * 
	 * @param value
	 */
	public void updateUserUsername(String value) {
		username = value;
	}

	@Override
	public String getUserPassword() {
		return password;
	}

	/**
	 * Logs out the current user and clears all cached data and downloaded
	 * courses.
	 */
	@Override
	public void logout() {
		try {
			NativeSettingsHelper.getInstance(context).removePreferenceValue(MFSettingsKeys.LAST_LOGGED_IN_USER);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Intent broadcast = new Intent();
		broadcast.setAction(BroadcastActions.PreLogout);
		broadcast.putExtra(IntentParameterConstants.SkipConfirmation, true);
		this.context.sendBroadcast(broadcast);
	}

	/**
	 * Provides the ability to track the current menu item.
	 */
	@Override
	public void setMenuItem(MenuItem menuItem) {
		currentMenuItem = menuItem;
	}

	/**
	 * Provides the ability to track the current resource item.
	 */
	@Override
	public void setResourceItem(BaseContentItem resourceItem) {
		currentResourceItem = resourceItem;
	}

	/**
	 * Gets a value for a key on the native storage. Native storage is provided
	 * by the framework. It consists in a way to store key/value pairs. Like the
	 * HTML5 DB, this kind of storage is cleaned up automatically if the user
	 * removes the application. This storage should be used specially if you
	 * want to share data between the HTML content and native side of the
	 * application, otherwise we highly recommend use HTML5 DB. There are two
	 * ways of store data on the native storage, globally and content-specific.
	 * 
	 * <p>
	 * <strong>Globally</strong> It's global on the application. The key can be
	 * retrieved from any content in any part of the system, as well as in any
	 * part of the native code. You must take care that the key can be used by
	 * other content developer. Below you can find the API calls to store and
	 * retrieve key/value pairs.
	 * <ul>
	 * <li>clientPlugin.getValue(MFStoreType.GLOBAL, key, callback);</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>Content-specific</strong> It's connected to the current
	 * resource/menu-item. Below you can find the API calls to store and
	 * retrieve key/value pairs.
	 * <ul>
	 * <li>clientPlugin.getValue(MFStoreType.SPECIFIC, key, callback);</li>
	 * </ul>
	 * </p>
	 * 
	 * @param type
	 *            The type of the native storage.
	 * @param key
	 *            The key name.
	 */
	@Override
	public String getValue(String type, String key) {
		// check the type
		if (MFStoreType.GLOBAL.equalsIgnoreCase(type)) {
			// check native settings
			String value = NativeSettingsHelper.getInstance(context).checkAndGetNativeSetting(key);

			if (value == null) {
				// get setting from the database
				value = databaseHelper.getSetting(key);
			}

			return value;

		} else if (MFStoreType.SPECIFIC.equalsIgnoreCase(type)) {
			String objectId = null;
			
			if (currentMenuItem != null) {
				objectId = currentMenuItem.getFullIdPathWithPackageId();
			} else if (currentResourceItem != null) {
				objectId = currentResourceItem.getFullIdPathWithPackageId();
			}
			
			// get setting from the database
			return databaseHelper.getSettingByObjectId(key, objectId);
		}

		return null;
	}

	/**
	 * Sets a value for a key on the native storage. Native storage is provided
	 * by the framework. It consists in a way to store key/value pairs. Like the
	 * HTML5 DB, this kind of storage is cleaned up automatically if the user
	 * removes the application. This storage should be used specially if you
	 * want to share data between the HTML content and native side of the
	 * application, otherwise we highly recommend use HTML5 DB. There are two
	 * ways of store data on the native storage, globally and content-specific.
	 * <p>
	 * <strong>Globally</strong> It's global on the application. The key can be
	 * retrieved from any content in any part of the system, as well as in any
	 * part of the native code. You must take care that the key can be used by
	 * other content developer. Below you can find the API calls to store and
	 * retrieve key/value pairs.
	 * <ul>
	 * <li>clientPlugin.setValue(MFStoreType.GLOBAL, key, value, callback);</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>Content-specific</strong> It's connected to the current
	 * resource/menu-item. Below you can find the API calls to store and
	 * retrieve key/value pairs.
	 * <ul>
	 * <li>clientPlugin.setValue(MFStoreType.SPECIFIC, key, value, callback);</li>
	 * </ul>
	 * </p>
	 * 
	 * @param type
	 *            The type of the native storage.
	 * @param key
	 *            The key name.
	 * @param value
	 *            The value.
	 */
	@Override
	public boolean setValue(String type, String key, String value) {
		boolean result = false;
		
		// check the type
		if (MFStoreType.GLOBAL.equalsIgnoreCase(type)) {
			if (getUserUsername() != null) {
				// set setting on the database
				result = databaseHelper.createOrUpdateSetting(key, value);
			}
		} else if (MFStoreType.SPECIFIC.equalsIgnoreCase(type)) {
			String objectId = null;
			
			if (currentMenuItem != null) {
				objectId = currentMenuItem.getFullIdPathWithPackageId();
			} else if (currentResourceItem != null) {
				objectId = currentResourceItem.getFullIdPathWithPackageId();
			}
			
			// set setting
			result = databaseHelper.createOrUpdateSettingWithObjectId(key, value, objectId);
		}
		
		return result;
	}

	@Override
	public void track(String sender, String additionalInfo) {
		// ensure that we are allowed to track the user's movements

		if (currentMenuItem != null) {
			if (currentResourceItem == null) {
				track(sender, currentMenuItem.getFullIdPathWithPackageId(), additionalInfo);
			} else {
				track(sender, currentResourceItem.getFullIdPathWithPackageId(), additionalInfo);
			}
		} else {
			if (currentResourceItem != null) {
				track(sender, currentResourceItem.getFullIdPathWithPackageId(), additionalInfo);
			}
		}
	}

	@Override
	public void track(String sender, String objectId, String additionalInfo) {
		// create tracking entry
		databaseHelper.createTrackingEntry(sender, objectId, additionalInfo);
	}

	@Override
	public void getPackageCatalogue(final PackageCatalogueRetrieved callback) {
		String packageCatalogueUrl = context.getString(R.string.packageCatalogueUrl);

		AuthHttpConnection.get(packageCatalogueUrl, "", new HandlerCallback(
				new Callback() {
					@Override
					public void onSuccess(String data) {
						try {
							Catalogue catalogue = null;

							if (!StringUtils.isEmpty(data)) {
								catalogue = PackageHelper.convertJSONToCatalogue(data, context);
							} else {
								catalogue = new Catalogue(new ArrayList<PackageItem>());
							}

							callback.onCatalogueRetrieved(catalogue);
						} catch (Throwable t) {
							onError(t);
						}
					}

					@Override
					public void onStart() {
					}

					@Override
					public void onError(Throwable t) {
						ServiceLayerExceptionHelper.getInstance().processException(t, context);
						callback.onCatalogueRetrieved(null);
					}
				}), context);
	}

	@Override
	public void openResource(String resourceId) {
		// fire broadcast

		Intent broadcast = new Intent();
		broadcast.setAction(BroadcastActions.OpenResource);
		broadcast.putExtra(IntentParameterConstants.ResourceItemId, resourceId);
		this.context.sendBroadcast(broadcast);
	}

	/**
	 * Retrieves the local path root for a particular course.
	 * 
	 * @param courseId
	 *            The course id.
	 * @param phoneGapCallback
	 *            The PhoneGap callback.
	 * @param callback
	 *            The callback function.
	 */
	@Override
	public void getCourseLocalPathRoot(final String courseId,
			final String phoneGapCallback,
			final GetCourseLocalPathRootCompleted callback) {
		// get the local folder for the course id
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				LibraryItem libraryItem = databaseHelper.getLibraryItemByUniqueId(courseId);
				String error = null;
				String localPathRoot = null;

				if (libraryItem == null) {
					error = String.format(context.getString(R.string.get_package_local_path_root_no_item), courseId);
				} else {
					String externalStorageCoursePath = String.format(context.getString(R.string.external_storage_packages_path_format_string), libraryItem.getLocalFolder()); 
					localPathRoot = Environment.getExternalStorageDirectory() + externalStorageCoursePath;
				}

				callback.onGetCourseLocalPathRootCompleted(courseId, localPathRoot, error, phoneGapCallback);
			}
		};

		Thread thread = new Thread(runnable);
		thread.start();
	}

	/**
	 * Retrieves the local path root for the currently opened course.
	 * 
	 * @param phoneGapCallback
	 *            The PhoneGap callback.
	 * @param callback
	 *            The callback function.
	 */
	@Override
	public void getCurrentCourseLocalPathRoot(final String phoneGapCallback,
			final GetCourseLocalPathRootCompleted callback) {
		// get the local folder for the course id
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				// get the currently opened library item
				LibraryItem libraryItem = PackageManager.getInstance(context, getBaseApplication().getDatabaseHelper()).getCurrentlyOpenedLibraryItem();
				String error = null;
				String localPathRoot = null;
				String courseId = null;

				if (libraryItem == null) {
					error = context.getString(R.string.get_opened_course_no_item);
				} else {
					courseId = libraryItem.getName();

					String externalStorageCoursePath = String.format(context.getString(R.string.external_storage_packages_path_format_string), libraryItem.getLocalFolder()); 
					localPathRoot = Environment.getExternalStorageDirectory() + externalStorageCoursePath;
				}

				callback.onGetCourseLocalPathRootCompleted(courseId,
						localPathRoot, error, phoneGapCallback);
			}
		};

		Thread thread = new Thread(runnable);
		thread.start();
	}

	/**
	 * Initialises a temporary folder for the currently opened course. If the
	 * folder already exists, the folder will be cleared. If it does not exist,
	 * it will be created.
	 * 
	 * Callback will return a JSON object with the following key and value
	 * pairs: tempFolderPath The full local path to the temporary folder in the
	 * course directory. E.g. /mnt/sdcard/.../{uniqueCourseFolderId}/temp
	 * 
	 * Error callback will return a JSON object with the following key and value
	 * pairs: error An error string to determine why the native method call
	 * failed.
	 * 
	 * @param phoneGapCallback
	 *            The PhoneGap callback.
	 * @param callback
	 *            The callback function.
	 */
	@Override
	public void initialiseCurrentCourseLocalTempFolder(
			final String phoneGapCallback,
			final InitialiseCurrentCourseLocalTempFolderCompleted callback) {
		// get the local folder for the course id
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				// get the currently opened library item
				LibraryItem libraryItem = PackageManager.getInstance(context, getBaseApplication().getDatabaseHelper())
						.getCurrentlyOpenedLibraryItem();
				String tempFolderPath = null;
				String error = null;

				if (libraryItem == null) {
					error = context.getString(R.string.get_opened_course_no_item);
				} else {
					String externalStorageCoursesTempDirectoryPathFormatString = context.getString(R.string.external_storage_courses_temp_directory_path_format_string);
					String externalStorageCoursesTempDirectoryPath = String.format(externalStorageCoursesTempDirectoryPathFormatString, Environment.getExternalStorageDirectory(),
									libraryItem.getLocalFolder());

					File tempFolder = new File(
							externalStorageCoursesTempDirectoryPath);
					tempFolderPath = externalStorageCoursesTempDirectoryPath;

					// we should create the folder
					if (!tempFolder.exists()) {
						// create the folder
						boolean result = tempFolder.mkdirs();

						if (!result) {
							String initialiseCurrentCourseLocalTempFolderErrorFormatString = context.getString(R.string.initialize_current_course_local_temp_folder_error_format_string);
							error = String.format(initialiseCurrentCourseLocalTempFolderErrorFormatString, libraryItem.getName());
						}
					} else {
						FileHelper.deleteFilesRecursive(tempFolder, false);
					}
				}

				callback.onInitialiseCurrentCourseLocalTempFolderCompleted(
						tempFolderPath, error, phoneGapCallback);
			}
		};

		Thread thread = new Thread(runnable);
		thread.start();
	}

	/**
	 * Clears the temporary folder for the currently opened course. If the
	 * folder exists, the folder will be cleared and removed. If the folder does
	 * not exist, an error will occur.
	 * 
	 * Callback will not return a value and will be invoked when the operation
	 * has finished.
	 * 
	 * Error callback will return a JSON object with the following key and value
	 * pairs: error An error string to determine why the native method call
	 * failed.
	 * 
	 * @param phoneGapCallback
	 *            The PhoneGap callback.
	 * @param callback
	 *            The callback function.
	 */
	@Override
	public void clearCurrentCourseLocalTempFolder(String phoneGapCallback,
			PhoneGapOperationNoResultCompleted callback) {
		createAndClearCourseTempFolder(false, phoneGapCallback, callback);
	}

	private void createAndClearCourseTempFolder(final boolean createFolder,
			final String phoneGapCallback,
			final PhoneGapOperationNoResultCompleted callback) {
		// get the local folder for the course id
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// get the currently opened library item
				LibraryItem libraryItem = PackageManager.getInstance(context, getBaseApplication().getDatabaseHelper())
						.getCurrentlyOpenedLibraryItem();
				String error = null;

				if (libraryItem == null) {
					error = context.getString(R.string.get_opened_course_no_item);
				} else {
					String externalStorageCoursesTempDirectoryPathFormatString = context.getString(R.string.external_storage_courses_temp_directory_path_format_string);
					String externalStorageCoursesTempDirectoryPath = String.format(externalStorageCoursesTempDirectoryPathFormatString, Environment.getExternalStorageDirectory(), libraryItem.getLocalFolder());

					File tempFolder = new File(externalStorageCoursesTempDirectoryPath);

					// check folder exists
					if (!tempFolder.exists()) {
						// temp folder does not exist, throw error
						String clearCurrentCourseLocalTempFolderNoFolderFormatString = context
								.getString(R.string.clear_current_course_local_temp_folder_no_folder_format_string);
						error = String
								.format(clearCurrentCourseLocalTempFolderNoFolderFormatString, libraryItem.getName());
					} else {
						FileHelper.deleteFilesRecursive(tempFolder);
					}
				}

				callback.onPhoneGapOperationNoResultCompleted(error, phoneGapCallback);
			}
		};

		Thread thread = new Thread(runnable);
		thread.start();
	}

	@Override
	public String getUserPasswordHash() {
		return null;
	}

	@Override
	public void setUserPasswordHash(String passwordHash) {
	}

	@Override
	public void setUserUsername(String username) {
	}

	@Override
	public void sync() {
		Intent broadcast = new Intent();
		broadcast.setAction(BroadcastActions.ManualSyncRequested);
		this.context.sendBroadcast(broadcast);
	}
}