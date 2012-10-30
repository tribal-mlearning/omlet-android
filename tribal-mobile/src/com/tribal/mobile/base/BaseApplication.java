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

package com.tribal.mobile.base;

import java.io.File;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import android.webkit.WebStorage;

import com.tribal.mobile.api.packages.PackageManager;
import com.tribal.mobile.image.DrawableBackgroundDownloader;
import com.tribal.mobile.util.BroadcastUtils;
import com.tribal.mobile.util.FileHelper;
import com.tribal.mobile.util.LoadingDialogHelper;
import com.tribal.mobile.util.database.BaseDatabaseHelper;

/**
 * Base application class. Provides public methods to retrieve a {@link BaseDatabaseHelper} singleton object and a {@link DrawableBackgroundDownloader} singleton object,
 * keep track of whether the application is still running in the foreground, the cache is cleared, create the {@link LoadingDialogHelper} singleton object, initialize the broadcast actions,
 * and asynchronously clear the application data.
 * 
 * @author Jon Brasted
 */
public abstract class BaseApplication extends Application {
	/* Fields */

	private BaseDatabaseHelper databaseHelper;
	private DrawableBackgroundDownloader drawableBackgroundDownloader;

	private boolean isRunning = false;
	private boolean isCacheCleared = false;

	/* Properties */

	/**
	 * Returns singleton database helper object. Creates the object if necessary.
	 * 
	 * @return	singleton instance of {@link BaseDatabaseHelper}
	 */
	public BaseDatabaseHelper getDatabaseHelper() {
		if (databaseHelper == null) {
			databaseHelper = createDatabaseHelper();
		}

		return databaseHelper;
	}

	/**
	 * Returns singleton drawable background downloader object. Creates the object if necessary.
	 * 
	 * @return	singleton instance of {@link DrawableBackgroundDownloader}
	 */
	public DrawableBackgroundDownloader getDrawableBackgroundDownloader() {
		if (drawableBackgroundDownloader == null) {
			drawableBackgroundDownloader = new DrawableBackgroundDownloader(getApplicationContext());
		}

		return drawableBackgroundDownloader;
	}

	/**
	 * Returns the <code>isRunning</code> boolean flag. Used to determine whether the application is running in the foreground.
	 *  
	 * @return	the isRunning boolean flag
	 */
	public boolean isRunning() {
		return isRunning;
	}
	
	/**
	 * Provides functionality to set the <code>isRunning</code> flag. Also fires an intent with the intent action <code>ApplicationIsRunningChanged</code>.
	 * 
	 * @param value	the specified value for <code>isRunning</code>
	 */
	public void setRunning(boolean value) {
		if (isRunning != value) {
			boolean oldIsRunning = isRunning;

			isRunning = value;

			// fire broadcast
			Intent intent = new Intent();
			intent.setAction(BroadcastActions.ApplicationIsRunningChanged);
			intent.putExtra(IntentParameterConstants.OldValue, oldIsRunning);
			intent.putExtra(IntentParameterConstants.NewValue, value);
			sendBroadcast(intent);
		}
	}

	/**
	 * Returns the <code>isCacheCleared</code> boolean flag. 
	 * 
	 * @return the <code>isCacheCleared</code> boolean flag
	 */
	public boolean isCacheCleared() {
		return isCacheCleared;
	}

	/**
	 * Provides functionality to set the <code>isCacheCleared</code> boolean flag.
	 * 
	 * @param value	the <code>isCacheCleared</code> boolean flag
	 */
	public void setIsCacheCleared(boolean value) {
		// set to false in the login activity
		isCacheCleared = value;
	}

	/* Constructor */

	public BaseApplication() {
		new LoadingDialogHelper();
	}

	/* Methods */

	@Override
	public void onCreate() {
		super.onCreate();
		BroadcastUtils.initialiseBroadcastActions(getApplicationContext());
	}

	/**
	 * Provides an override method to create an instance of {@link BaseDatabaseHelper}. Intended to be overriden. 
	 * 
	 * @return	null value. Method is intended to be overriden.
	 */
	protected BaseDatabaseHelper createDatabaseHelper() {
		Log.e("com.tribal.mobile.base.BaseApplication", "createDatabaseHelper() must be overriden in a subclass to have an effect.");
		return null;
	}
	
	/**
	 * Provides an override method to reset the application if it recovers from a crash situation.
	 */
	public void resetApplication() {
	}

	/**
	 * Provides functionality to clear the cached data for the application. Could take a while so users should be notified.
	 */
	public void clearApplicationDataAsync(final ClearApplicationDataCompleted callback) {
		if (!isCacheCleared) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					File cache = getCacheDir();
					File appDir = new File(cache.getParent());

					// clear the webkit databases
					WebStorage webStorage = WebStorage.getInstance();
					webStorage.deleteAllData();
					
					// close and teardown databases
					databaseHelper.close();
					databaseHelper = null;
					
					// remove the caches and databases
					if (appDir.exists()) {
						String[] children = appDir.list();

						for (String s : children) {
							if ((!s.equals("lib")) && (!s.equals("shared_prefs"))) {
								FileHelper.deleteFilesRecursive(new File(appDir, s), true);
							}
						}
					}
					
					onClearingApplicationData(callback);
				};
			};

			Thread thread = new Thread(runnable);
			thread.start();

			isCacheCleared = true;
		} else {
			onClearingApplicationData(callback);
		}
	}

	private void onClearingApplicationData(ClearApplicationDataCompleted callback) {
		// clear the package manager
		PackageManager.getInstance(getApplicationContext(), getDatabaseHelper()).clear();

		// invoke the override
		onClearingApplicationDataOverride(callback);
	}

	/**
	 * Provides an override method that will be invoked when the application data is cleared. 
	 * 
	 * @param callback	the {@link ClearApplicationDataCompleted} callback
	 */
	protected void onClearingApplicationDataOverride(ClearApplicationDataCompleted callback) {
	}
}