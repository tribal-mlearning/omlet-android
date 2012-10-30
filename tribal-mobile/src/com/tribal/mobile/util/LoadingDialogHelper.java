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

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Looper;

/**
 * Utility class for managing, creating and showing loading dialogs.
 * 
 * @author Jon Brasted
 */
public class LoadingDialogHelper {
	/* Fields */
	
	private static LoadingDialogHelper instance;
	
	private Map<Activity, ProgressDialog> loadingDialogMap = null;
	
	private int loadingCount = 0;
	
	/* Properties */
	
	public static LoadingDialogHelper getInstance() {
		return instance;
	}
	
	private boolean isCurrentThreadUIThread() {
		return (Looper.getMainLooper().getThread() == Thread.currentThread());
	}
	
	/* Constructor */
	
	public LoadingDialogHelper() {
		instance = this;
		loadingDialogMap = new HashMap<Activity, ProgressDialog>();
	}
		
	/* Methods */
	
	private void createAndShowLoadingDialog(Activity activity, String message) {
		// TODO: Pass in loading message
		
		if (!loadingDialogMap.containsKey(activity)) {		
			// get the title
			String progressDialogMessage = message;
			ProgressDialog loadingDialog = ProgressDialog.show(activity, null, progressDialogMessage, true, false);
			loadingDialogMap.put(activity, loadingDialog);
		}
	}
	
	private void dismissCurrentLoadingDialog(Activity activity) {
		ProgressDialog loadingDialog = null;
		
		if (loadingDialogMap.containsKey(activity)) {
			loadingDialog = loadingDialogMap.remove(activity);
		}
		
		if (loadingDialog != null) {
			loadingDialog.dismiss();
			loadingDialog = null;
		}
	}
	
	/**
	 * Create and show a loading dialog.
	 * 
	 * @param activity	the parent activity
	 * @param message	the message
	 */
	public void showLoadingDialog(final Activity activity, final String message) {
		// check current thread is ui thread
		if (isCurrentThreadUIThread()) {
			createAndShowLoadingDialog(activity, message);
		} else {
			// otherwise dispatch onto the UI thread
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					createAndShowLoadingDialog(activity, message);;
				}
			});
		}
	}
	
	/**
	 * Create and show a loading dialog.
	 * 
	 * @param activity	the parent activity
	 * @param useCount	track the number of dialogs open so only one is created
	 * @param message	the message
	 */
	public void showLoadingDialog(Activity activity, boolean useCount, String message) {
		if (!useCount) {
			showLoadingDialog(activity, message);
		} else {
			if (loadingCount == 0) {
				loadingCount++;
				showLoadingDialog(activity, message);
			} else {
				loadingCount++;
			}
		}
	}
	
	/**
	 * Dismiss loading dialog.
	 * 
	 * @param activity	the parent activity
	 */
	public void dismissLoadingDialog(final Activity activity) {
		// check current thread is ui thread
		if (isCurrentThreadUIThread()) {
			dismissCurrentLoadingDialog(activity);
		} else {
			// otherwise dispatch onto the UI thread
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					dismissCurrentLoadingDialog(activity);
				}
			});
		}
	}

	/**
	 * Dismiss loading dialog.
	 * 
	 * @param activity	the parent activity
	 * @param useCount	track the number of dialogs open so only one is created and shown at once
	 */
	public void dismissLoadingDialog(Activity activity, boolean useCount) {
		if (!useCount) {
			dismissLoadingDialog(activity);
		} else {
			if (loadingCount > 0) {
				loadingCount--;
			}

			if (loadingCount == 0) {
				dismissLoadingDialog(activity);
			}
		}
	}
	
	/**
	 * Show a loading dialog.
	 * 
	 * @param dialog		the specified loading dialog
	 * @param activity		the parent activity
	 */
	public void showLoadingDialog(final ProgressDialog dialog, final Activity activity) {
		if (isCurrentThreadUIThread()) {
			if (!loadingDialogMap.containsKey(activity)) {		
				loadingDialogMap.put(activity, dialog);
			}
			
			dialog.show();
		} else {
			// otherwise dispatch onto the UI thread
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (!loadingDialogMap.containsKey(activity)) {		
						loadingDialogMap.put(activity, dialog);
					}
					
					dialog.show();
				}
			});
		}
	}
}