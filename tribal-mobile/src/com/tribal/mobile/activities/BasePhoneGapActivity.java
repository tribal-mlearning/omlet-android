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

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.phonegap.DroidGap;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;
import com.tribal.mobile.Framework;
import com.tribal.mobile.R;
import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.api.packages.PackageManager;
import com.tribal.mobile.api.tracking.TrackingHelper;
import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.ClearApplicationDataCompleted;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.checklist.NativeAddItemDialogActivity;
import com.tribal.mobile.checklist.NativeInfoDialogActivity;
import com.tribal.mobile.model.BaseContentItem;
import com.tribal.mobile.model.MenuItem;
import com.tribal.mobile.model.Package;
import com.tribal.mobile.phonegap.DefaultChecklistPluginImplementation;
import com.tribal.mobile.util.DeletionUtils;
import com.tribal.mobile.util.DialogHelper;
import com.tribal.mobile.util.EnvironmentUtils;
import com.tribal.mobile.util.LoadingDialogHelper;
import com.tribal.mobile.util.NativeFileHelper;
import com.tribal.mobile.util.database.BaseDatabaseHelper;
import com.tribal.mobile.util.resources.ResourceHelper;
import com.tribal.mobile.util.resources.ResourceItemType;
import com.tribal.mobile.util.resources.StringResourceLookups;

/**
 * Base activity for PhoneGap-based activities that provides functionality used by inheriting classes. 
 */
public abstract class BasePhoneGapActivity extends DroidGapActionBar implements ClearApplicationDataCompleted {
	/* Fields */
	
	private ProgressDialog loadingDialog = null;

	protected MenuItem menuItem;
	protected BaseContentItem resourceItem;
	protected String packageName;

	protected Map<String, BroadcastReceiver> broadcastReceivers = null;

	protected String loadedUrl;
	
	protected Timer sessionTimer = null;
	protected int sessionElapsedTime = 0;
	
	/* Properties */

	/**
	 * Returns singleton database helper object.
	 * 
	 * @return	singleton instance of {@link BaseDatabaseHelper}
	 */
	public BaseDatabaseHelper getDatabaseHelper() {
		return getBaseApplication().getDatabaseHelper();
	}

	/**
	 * Returns singleton base application object.
	 * 
	 * @return	singleton instance of {@link BaseApplication}
	 */
	public BaseApplication getBaseApplication() {
		Application application = getApplication();

		if (application instanceof BaseApplication) {
			return (BaseApplication) this.getApplication();
		} else {
			Log.e("com.tribal.mobile.activities.BasePhoneGapActivity", "getApplication() does not inherit from BaseApplication, which is required.");
			return null;
		}
	}
	
	private void setMenuItem(MenuItem menuItem) {
		this.menuItem = menuItem;
		
		// persist the menu item onto the client
		Framework.getClient().setMenuItem(this.menuItem);
	}
	
	private void setResourceItem(BaseContentItem resourceItem) {
		this.resourceItem = resourceItem;
		
		// persist the resource item onto the client
		Framework.getClient().setResourceItem(this.resourceItem);
	}
	
	private String getPackageId() {
		if (this.resourceItem != null) {
			return this.resourceItem.getPackageId();
		} else if (this.menuItem != null) {
			return this.menuItem.getPackageId();
		} else {
			return null;
		}
	}

	/* Methods */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		broadcastReceivers = new HashMap<String, BroadcastReceiver>();
		
		addBroadcastReceiver(BroadcastActions.Logout);

		onNewIntent(getIntent());
	}
	
	@Override
	public void onDestroy() {
		this.appView.clearCache(false);
		
		if (!broadcastReceivers.isEmpty()) {
			// get the set of keys
			Set<String> broadcastReceiverKeySet = broadcastReceivers.keySet();

			// get the iterator
			Iterator<String> broadcastReceiverKeySetIterator = broadcastReceiverKeySet.iterator();

			// get the keys
			String[] broadcastReceiverKeys = new String[broadcastReceiverKeySet.size()];

			for (int index = 0, broadcastReceiverKeySetSize = broadcastReceiverKeys.length; index < broadcastReceiverKeySetSize; index++) {
				broadcastReceiverKeys[index] = broadcastReceiverKeySetIterator.next();
			}

			// remove all the receivers
			for (String intentAction : broadcastReceiverKeys) {
				removeBroadcastReceiver(intentAction);
			}
		}

		super.onDestroy();
	}

	@Override
	public void init() {
		super.init();

		loadView();
	}

	@Override
	protected void onResume() {
		super.onResume();

		getBaseApplication().setRunning(true);
		
		// ensure that the Framework is still working
		if (Framework.getServer() == null || Framework.getClient() == null) {
			BaseApplication baseApplication = getBaseApplication();
				
			if (baseApplication != null) {
				baseApplication.resetApplication();
				return;
			}
		}

		if (this.resourceItem != null) {
			// persist the resource item onto the client
			Framework.getClient().setResourceItem(this.resourceItem); 
		}

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.PreLogout)) {
			addBroadcastReceiver(BroadcastActions.PreLogout);
		}
		
		if (!isBroadcastReceiverActionRegistered(BroadcastActions.OpenMenuItem)) {
			addBroadcastReceiver(BroadcastActions.OpenMenuItem);
		}

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.OpenResource)) {
			addBroadcastReceiver(BroadcastActions.OpenResource);
		}

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.ShowLoadingDialog)) {
			addBroadcastReceiver(BroadcastActions.ShowLoadingDialog);
		}

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.DismissLoadingDialog)) {
			addBroadcastReceiver(BroadcastActions.DismissLoadingDialog);
		}
		
		if (!isBroadcastReceiverActionRegistered(DefaultChecklistPluginImplementation.OPEN_NATIVE_ADD_ITEM_DIALOG_BROADCAST_ACTION)) {
			addBroadcastReceiver(DefaultChecklistPluginImplementation.OPEN_NATIVE_ADD_ITEM_DIALOG_BROADCAST_ACTION);
		}

		if (!isBroadcastReceiverActionRegistered(DefaultChecklistPluginImplementation.OPEN_NATIVE_INFO_DIALOG_BROADCAST_ACTION)) {
			addBroadcastReceiver(DefaultChecklistPluginImplementation.OPEN_NATIVE_INFO_DIALOG_BROADCAST_ACTION);
		}
		
		// subscribe to the show toast broadcast
		if (!isBroadcastReceiverActionRegistered(BroadcastActions.ShowToast)) {
			addBroadcastReceiver(BroadcastActions.ShowToast);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		getBaseApplication().setRunning(false);

		if (isBroadcastReceiverActionRegistered(BroadcastActions.OpenMenuItem)) {
			removeBroadcastReceiver(BroadcastActions.OpenMenuItem);
		}

		if (isBroadcastReceiverActionRegistered(BroadcastActions.OpenResource)) {
			removeBroadcastReceiver(BroadcastActions.OpenResource);
		}

		if (isBroadcastReceiverActionRegistered(BroadcastActions.ShowLoadingDialog)) {
			removeBroadcastReceiver(BroadcastActions.ShowLoadingDialog);
		}

		if (isBroadcastReceiverActionRegistered(BroadcastActions.DismissLoadingDialog)) {
			removeBroadcastReceiver(BroadcastActions.DismissLoadingDialog);
		}
		
		if (isBroadcastReceiverActionRegistered(DefaultChecklistPluginImplementation.OPEN_NATIVE_ADD_ITEM_DIALOG_BROADCAST_ACTION)) {
			removeBroadcastReceiver(DefaultChecklistPluginImplementation.OPEN_NATIVE_ADD_ITEM_DIALOG_BROADCAST_ACTION);
		}

		if (isBroadcastReceiverActionRegistered(DefaultChecklistPluginImplementation.OPEN_NATIVE_INFO_DIALOG_BROADCAST_ACTION)) {
			removeBroadcastReceiver(DefaultChecklistPluginImplementation.OPEN_NATIVE_INFO_DIALOG_BROADCAST_ACTION);
		}
		
		if (isBroadcastReceiverActionRegistered(BroadcastActions.ShowToast)) {
			removeBroadcastReceiver(BroadcastActions.ShowToast);
		}

		if (loadingDialog != null) {
			dismissLoadingDialog();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// get the extras
		Bundle extras = intent.getExtras();

		// update package name
		if (intent.hasExtra(IntentParameterConstants.PackageName)) {
			String packageName = intent
					.getStringExtra(IntentParameterConstants.PackageName);

			if (!TextUtils.isEmpty(packageName)
					&& (!TextUtils.isEmpty(this.packageName) || (!packageName
							.equalsIgnoreCase(this.packageName)))) {
				this.packageName = packageName;

				// invoke onPackageNameUpdate
				onPackageNameUpdate();
			}
		}

		// get the menu item
		if (extras != null && extras.containsKey(IntentParameterConstants.MenuItem)) {
			MenuItem menuItem = (MenuItem) extras.getSerializable(IntentParameterConstants.MenuItem);

			if (this.menuItem != menuItem) {
				// get the menu				
				setMenuItem(menuItem);
			}
		}

		// get the menu item
		if (extras != null && extras.containsKey(IntentParameterConstants.ResourceItem)) {
			// get the content item
			BaseContentItem contentItem = (BaseContentItem) extras.getSerializable(IntentParameterConstants.ResourceItem);

			if (this.resourceItem != contentItem) {
				setResourceItem(contentItem);

				if (this.menuItem == null && this.resourceItem != null && this.resourceItem.hasMenuItemParent()) {
					setMenuItem(this.resourceItem.getMenuItemParent());
				}
			}
		} else {
			if (this.menuItem != null) {
				// get the resource item
				BaseContentItem resourceItem = this.menuItem.getLinkedContentItem();

				if (this.resourceItem != resourceItem) {
					setResourceItem(resourceItem);
				}
			}
		}

		// load url
		loadUrl();
	}
	
	/**
	 * Provides functionality to retrieve the database path to the HTML 5 database(s) for the currently open package.
	 * 
	 * @return	the database path to the HTML 5 database(s) for the currently open package
	 */
	@Override
	protected String getHtml5DatabasePath() {
		// get the path
		return DeletionUtils.getHtml5DatabasePath(getApplicationContext(), getPackageId());
	}
	
	/**
	 * Provides an override method for handling package name updates.
	 */
	protected void onPackageNameUpdate() {
	}

	/**
	 * Provides functionality to add a broadcast receiver to the given intent action.
	 * 
	 * @param intentAction	the intent action on which to place an intent broadcast receiver upon
	 */
	protected void addBroadcastReceiver(String intentAction) {
		BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				onBroadcastReceive(intent);
			}
		};

		IntentFilter filter = new IntentFilter();
		filter.addAction(intentAction);
		registerReceiver(receiver, filter);

		// add receiver to the collection
		broadcastReceivers.put(intentAction, receiver);
	}

	/**
	 * Provides functionality to remove a broadcast receiver from the given intent action.
	 * 
	 * @param intentAction	the intent action of which to remove the intent broadcast receiver from
	 */
	protected void removeBroadcastReceiver(String intentAction) {
		if (broadcastReceivers != null
				&& broadcastReceivers.containsKey(intentAction)) {
			BroadcastReceiver receiver = broadcastReceivers.get(intentAction);

			// unregister the receiver
			unregisterReceiver(receiver);

			// remove the receiver from the map
			broadcastReceivers.remove(intentAction);
		}
	}

	/**
	 * Provides functionality to return whether a broadcast receiver has been registered with the supplied intent action.  
	 * 
	 * @param intentAction	the intent action
	 * @return				a boolean result determining whether a broadcast receiver has been registered with the supplied intent action 
	 */
	protected boolean isBroadcastReceiverActionRegistered(String intentAction) {
		return (broadcastReceivers != null && broadcastReceivers.containsKey(intentAction));
	}

	/**
	 * Provides functionality to handle common intent actions and is invoked when a broadcast receiver receives 
	 * Method will invoke {@link #onBroadcastReceiveOverride(Intent) onBroadcastReceiveOverride} method if <code>intent.getAction()</code> does not return
	 * one of the following:
	 * <p>
	 * 	<code>PreLogout</code><br />
	 * 	<code>Logout</code><br />
	 *  <code>OpenMenuItem</code><br />
	 *  <code>OpenResource</code><br />
	 *  <code>ShowLoadingDialog</code><br />
	 *  <code>DismissLoadingDialog</code><br />
	 *  <code>ShowToast</code><br />
	 *  <code>OPEN_NATIVE_ADD_ITEM_DIALOG_BROADCAST_ACTION</code><br />
	 *  <code>OPEN_NATIVE_INFO_DIALOG_BROADCAST_ACTION</code>
	 * </p>
	 */
	private void onBroadcastReceive(Intent intent) {
		String intentAction = intent.getAction();
		
		if (BroadcastActions.PreLogout.equals(intentAction)) {
			// invoke onPreLogout
			onPreLogout(intent);
		} else if (BroadcastActions.Logout.equals(intentAction)) {
			// finish the activity
			finish();
		} else if (BroadcastActions.OpenMenuItem.equals(intentAction)) {
			// open menu item
			openMenuItem(intent);
		} else if (BroadcastActions.OpenResource.equals(intentAction)) {
			// open resource
			openResource(intent);
		} else if (BroadcastActions.ShowLoadingDialog.equals(intentAction)) {
			// show loading dialog
			showLoadingDialog();
		} else if (BroadcastActions.DismissLoadingDialog.equals(intentAction)) {
			// hide loading dialog
			dismissLoadingDialog();
		} else if (BroadcastActions.ShowToast.equals(intentAction)) {
			// show toast
			onShowToast(intent);
		} else if (DefaultChecklistPluginImplementation.OPEN_NATIVE_ADD_ITEM_DIALOG_BROADCAST_ACTION.equals(intentAction)) {
			// open checklist native add item dialog
			onOpenChecklistNativeAddItemDialog(intent);
		} else if (DefaultChecklistPluginImplementation.OPEN_NATIVE_INFO_DIALOG_BROADCAST_ACTION.equals(intentAction)) {
			// open checklist native info dialog
			onOpenChecklistNativeInfoDialog(intent);
		} else {
			// otherwise fire override method
			onBroadcastReceiveOverride(intent);
		}
	}

	/**
	 * Provides an override method for handling intent actions. Will be invoked by {@link #onBroadcastReceive(Intent) onBroadcastReceive} if <code>intent.getAction()</code> does not return
	 * a handled intent action.
	 */
	protected void onBroadcastReceiveOverride(Intent intent) {
	}
	
	/**
	 * Provides an override method for handling intents with the action <code>PreLogout</code>. Will invoke {@link #onPreLogout(Intent) onPreLogout} with a null value <code>intent</code> if not
	 * overriden.
	 */
	protected void requestLogout() {
		onPreLogout(null);
	}
	
	private void onPreLogout(Intent intent) {
		boolean skipConfirmation = false;
		
		if (intent != null && intent.hasExtra(IntentParameterConstants.SkipConfirmation)) {
			skipConfirmation = intent.getBooleanExtra(IntentParameterConstants.SkipConfirmation, false); 
		}
		
		if (!skipConfirmation) {		
			// trigger confirmation dialog
			showLogoutConfirmationDialog();
		} else {
			onPreLogoutConfirmed();
		}
	}
	
	private void showLogoutConfirmationDialog() {
		String dialogTitle = getString(R.string.dialog_logoutConfirmDialogTitle);

		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setCancelable(false);
		alertDialog.setTitle(dialogTitle);
		alertDialog.setMessage(getString(R.string.dialog_logoutConfirmDialogMessage));

		String continueString = getString(R.string.button_continue);
		String cancelString = getString(R.string.button_cancel);
		
		alertDialog.setButton(continueString, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// Ignore warning and continue with logout
				dialog.dismiss();

				// invoke onPreLogoutConfirmed
				onPreLogoutConfirmed();
			}
		});

		alertDialog.setButton2(cancelString, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Cancel and do not logout
				dialog.cancel();
			}
		});

		alertDialog.show();
	}
	
	private void onPreLogoutConfirmed() {
		onBeforePreLogout();
		
		// if cache is cleared, just logout
		if (getBaseApplication().isCacheCleared()) {
			doLogout();
		} else {
			// if not, trigger cache clearing process
			getBaseApplication().clearApplicationDataAsync(this);
		}
	}
	
	/**
	 * Provides functionality to show loading dialog and close {@link BaseDatabaseHelper} singleton instance after logout has been confirmed by the user
	 * but the prelogout process has not yet taken place.
	 */
	protected void onBeforePreLogout() {
		// show loading dialog
		showLoadingDialog(getString(R.string.dialog_removing_message));
		
		// close the database
		getDatabaseHelper().close();
	}
	
	/**
	 * Provides functionality to dismiss the loading dialog after the prelogout process has taken place.
	 */
	protected void onAfterPreLogout() {
		// dismiss loading dialog
		dismissLoadingDialog();
	}
	
	/**
	 * Provides implementation of {@link ClearApplicationDataCompleted#onClearApplicationDataCompleted() onClearApplicationDataCompleted} callback. 
	 * Will be invoked as part of the logout process.
	 */
	@Override
	public void onClearApplicationDataCompleted() {
		// do logout
		doLogout();
	}
	
	/**
	 * Provides an override method for handling a redirect to the login screen.
	 */
	protected void redirectToLogin() {
	}
	
	private void doLogout() {
		onAfterPreLogout();
		
		// redirect to login
		redirectToLogin();
		
		// send broadcasts to log out all the other activities
		sendBroadcast(BroadcastActions.Logout);
	}
	
	/**
	 * Provides functionality to show a loading dialog with the message as specified in the <code>dialog_loading_message</code> string resource.
	 */
	public void showLoadingDialog() {
		showLoadingDialog(false, getString(R.string.dialog_loading_message));
	}
	
	/**
	 * Provides functionality to show a loading dialog, while using a counter to track the number of open dialogs and dialog requests (if a request to open a dialog is received when a dialog is still open).
	 * 
	 * @param useCount	whether to count the number of open dialogs (and dialog requests).
	 */
	public void showLoadingDialog(boolean useCount) {
		showLoadingDialog(useCount, getString(R.string.dialog_loading_message));
	}

	/**
	 * Provides functionality to show a loading dialog with the supplied message.
	 * 
	 * @param message	the message
	 */
	public void showLoadingDialog(String message) {
		showLoadingDialog(false, message);
	}
	
	/**
	 * Provides functionality to show a loading dialog with the supplied message, while using a counter to track the number of open dialogs and dialog requests (if a request to open a dialog is received when a dialog is still open).
	 * 
	 * @param useCount	whether to count the number of open dialogs (and dialog requests).
	 * @param message	the message
	 */
	public void showLoadingDialog(boolean useCount, String message) {
		LoadingDialogHelper.getInstance().showLoadingDialog(this, useCount, message);
	}

	/**
	 * Provides functionality to dismiss the currently open loading dialog (if there is one).
	 */
	protected void dismissLoadingDialog() {
		dismissLoadingDialog(false);
	}
	
	/**
	 * Provides functionality to dismiss the currently open loading dialog (if there is one), while using a counter to track the number of open dialogs and dialog requests (if a request to open a dialog is received when a dialog is still open).
	 * 
	 * @param useCount	whether to count the number of open dialogs (and dialog requests).
	 */
	protected void dismissLoadingDialog(boolean useCount) {
		LoadingDialogHelper.getInstance().dismissLoadingDialog(this, useCount);
	}

	/**
	 * Provides functionality to send a parameterless broadcast to the supplied broadcast action.
	 * 
	 * @param intentAction	the intent action of which to fire 
	 */
	protected void sendBroadcast(String intentAction) {
		sendBroadcast(intentAction, null, null);
	}

	/**
	 * Provides functionality to asynchronously send a broadcast with the supplied intent.
	 * 
	 * @param intent	the intent of which to broadcast
	 */
	protected void sendBroadcastAsync(final Intent intent) {
		final Context context = this;
		new Thread(new Runnable() {
			@Override
			public void run() {
				context.sendBroadcast(intent);
			}
		}).start();
	}

	/**
	 * Provides functionality to send a broadcast with the supplied intent action, parameters and intent flags.
	 * 
	 * @param intentAction	the intent action
	 * @param parameters	the parameters
	 * @param intentFlags	the intent flags
	 */
	protected void sendBroadcast(String intentAction, Map<String, Serializable> parameters, int[] intentFlags) {
		Intent broadcast = new Intent();
		broadcast.setAction(intentAction);

		if (parameters != null && !parameters.isEmpty()) {
			for (Map.Entry<String, Serializable> entry : parameters.entrySet()) {
				broadcast.putExtra(entry.getKey(), entry.getValue());
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

			broadcast.setFlags(flags);
		}

		sendBroadcast(broadcast);
	}

	/**
	 * Provides an override method that will be invoked when the activity is initialized.
	 */
	protected void loadView() {
	}

	/**
	 * Provides an override method that will be invoked when the menu item, resource item and package name have been set on the activity.
	 */	
	protected void loadUrl() {
		String url = null;

		if (this.resourceItem != null) {
			String htmlPath = resourceItem.getPath();

			// load the url
			url = htmlPath;
		} else {
			// look to see if the path has been given in an extra
			Intent intent = getIntent();

			if (intent.hasExtra(IntentParameterConstants.Url)) {
				url = intent.getStringExtra(IntentParameterConstants.Url);
			}
		}

		if (url != null
				&& (TextUtils.isEmpty(this.loadedUrl) || !loadedUrl
						.contains(url))) {
			this.loadUrl(url);
		}
	}

	private void openMenuItem(Intent intent) {
		dismissLoadingDialog();

		// get resource id
		if (intent.hasExtra(IntentParameterConstants.MenuItemId)) {
			String menuItemId = intent
					.getStringExtra(IntentParameterConstants.MenuItemId);

			if (!TextUtils.isEmpty(menuItemId)) {
				// get package
				Package currentPackage = PackageManager.getInstance(getApplicationContext(), getDatabaseHelper()).getCurrentlyOpenedPackage();

				if (currentPackage != null) {
					MenuItem menuItem = currentPackage
							.findMenuItemByPath(menuItemId);

					if (menuItem != null) {
						// navigate to menu item
						navigateToMenuItem(menuItem);
					}
				}
			}
		} else if (intent.hasExtra(IntentParameterConstants.MenuItem)) {
			Object menuItemObject = intent
					.getSerializableExtra(IntentParameterConstants.MenuItem);

			if (menuItemObject instanceof MenuItem) {
				MenuItem menuItem = (MenuItem) menuItemObject;
				navigateToMenuItem(menuItem);
			}
		}
	}

	/**
	 * Provides an override method to navigate to a class based on a specified {@link MenuItem}.
	 * 
	 * @param menuItem	the menu item
	 */
	protected void navigateToMenuItem(MenuItem menuItem) {
	}

	private void openResource(Intent intent) {
		dismissLoadingDialog();

		// get resource id
		if (intent.hasExtra(IntentParameterConstants.ResourceItemId)) {
			String resourceItemId = intent
					.getStringExtra(IntentParameterConstants.ResourceItemId);

			if (!TextUtils.isEmpty(resourceItemId)) {
				// TODO: Add cross-package loading logic
				// get package
				Package currentPackage = PackageManager.getInstance(getApplicationContext(), getDatabaseHelper()).getCurrentlyOpenedPackage();

				if (currentPackage != null) {
					BaseContentItem resourceItem = currentPackage
							.findContentItemById(resourceItemId);

					if (resourceItem != null) {
						// navigate to resource item
						navigateToResourceItem(resourceItem);
					}
				}
			}
		} else if (intent.hasExtra(IntentParameterConstants.ResourceItem)) {
			Object resourceItemObject = intent
					.getSerializableExtra(IntentParameterConstants.ResourceItem);

			if (resourceItemObject instanceof BaseContentItem) {
				BaseContentItem resourceItem = (BaseContentItem) resourceItemObject;
				navigateToResourceItem(resourceItem);
			}
		}
	}

	/**
	 * Provides an override method to navigate to a class based on a specified {@link BaseContentItem}.
	 * 
	 * @param resourceItem	the resource item
	 */
	protected void navigateToResourceItem(BaseContentItem resourceItem) {
	}

	/**
	 * Provides an override method to navigate to the package detail screen based on a specified {@link PackageItem}.
	 * 
	 * @param packageItem	the package item
	 */
	public void navigateToPackageDetails(PackageItem packageItem) {
	}
	
	/**
	 * Provides functionality to override {@link DroidGap#setWebViewClient(WebView, WebViewClient) setWebViewClient} and set the {@link WebViewClient} instance to an instance of
	 * {@link PhoneGapViewClient}.
	 */
	@Override
	protected void setWebViewClient(WebView appView, WebViewClient client) {
		PhoneGapViewClient phoneGapViewClient = new PhoneGapViewClient(this);

		// override the web view client so we can use our own
		super.setWebViewClient(appView, phoneGapViewClient);
	}

	private void onOpenChecklistNativeAddItemDialog(Intent intent) {
		// create intent
		Intent openChecklistNativeAddItemDialogIntent = new Intent(this,
				NativeAddItemDialogActivity.class);

		if (intent.hasExtra(IntentParameterConstants.PhoneGapCallbackId)) {
			String phoneGapCallbackId = intent
					.getStringExtra(IntentParameterConstants.PhoneGapCallbackId);
			openChecklistNativeAddItemDialogIntent.putExtra(
					IntentParameterConstants.PhoneGapCallbackId,
					phoneGapCallbackId);

			startActivityForResult(
					openChecklistNativeAddItemDialogIntent,
					NativeAddItemDialogActivity.NativeAddItemDialogActivity_OpenChecklistNativeAddItemDialog_ReturnIdentifier);
		}
	}

	private void onOpenChecklistNativeInfoDialog(Intent intent) {
		// create intent
		Intent openChecklistNativeInfoDialogIntent = new Intent(this,
				NativeInfoDialogActivity.class);

		if (intent.hasExtra(IntentParameterConstants.JsonArrayParameters)) {
			String jsonArrayParameters = intent
					.getStringExtra(IntentParameterConstants.JsonArrayParameters);
			openChecklistNativeInfoDialogIntent.putExtra(
					IntentParameterConstants.JsonArrayParameters,
					jsonArrayParameters);

			startActivity(openChecklistNativeInfoDialogIntent);
		}
	}
	
	private void parseUrlAndLogExperiencedTrack(String url) {
		// get the path to the courses folder
		int coursesFolderPathResourceId = ResourceHelper.getResourceIdByName(this, ResourceItemType.string, StringResourceLookups.ExternalStoragePackagesPathFormatString);
		String coursesFolderPath = getString(coursesFolderPathResourceId);
		String fullCoursesFolderPath = Environment.getExternalStorageDirectory() + String.format(coursesFolderPath, "");
		
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
	
	/**
	 * Provides functionality to show a toast notification.
	 * 
	 * @param intent	an intent containing a key-value pair for <code>Message</code> and a key-value pair for <code>Duration</code>
	 */
	protected void onShowToast(Intent intent) {
		if (intent.hasExtra(IntentParameterConstants.Message) &&
			intent.hasExtra(IntentParameterConstants.Duration)) {
			
			String message = intent.getStringExtra(IntentParameterConstants.Message);
			int duration = intent.getIntExtra(IntentParameterConstants.Duration, Toast.LENGTH_SHORT);
			
			Toast.makeText(this, message, duration).show();
		}
	}
	
	/**
	 * Provides functionality to send a {@link PluginResult} object with a {@link PluginResult.Status} object and no payload back to the PhoneGap framework.
	 * 
	 * @param callbackId	the PhoneGap callback id
	 * @param status		the PhoneGap {@link PluginResult.Status} object
	 */
	protected void sendPluginResult(String callbackId, Status status) {
		sendPluginResult(callbackId, status, null);
	}
	
	/**
	 * Provides functionality to send a {@link PluginResult} object with a {@link PluginResult.Status} object and string payload back to the PhoneGap framework.
	 * 
	 * @param callbackId	the PhoneGap callback id
	 * @param status		the PhoneGap {@link PluginResult.Status} object
	 * @param payload		the payload
	 */
	protected void sendPluginResult(String callbackId, Status status, String payload) {
		PluginResult pluginResult = null;
		
		if (payload != null) {
			pluginResult = new PluginResult(status, payload);
		} else {
			pluginResult = new PluginResult(status);
		}
		
		pluginResult.setKeepCallback(false);
		sendJavascript(pluginResult.toSuccessCallbackString(callbackId));
	}

	/**
	 * Provides functionality to navigate to the specified activity class, supplying the specifying parameters and intent flags in the intent.
	 * 
	 * @param context			the navigation context
	 * @param activityClass		the activity class			
	 * @param parameters		the parameters
	 * @param intentFlags		the intent flags
	 */
	protected void navigate(Context context, Class<?> activityClass, Map<String, Serializable> parameters, int[] intentFlags) {
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
	 * Provides functionality to handle creation of options menu. Will invoke {@link #onPreCreateOptionsMenu(Menu)} to determine whether to permit the creation of the options menu
	 * to continue.
	 *
	 * @param menu	the menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (onPreCreateOptionsMenu(menu)) {
			onCreateOptionsMenuOverride(menu);

			return true;
		}

		return false;
	}

	/**
	 * Provides an override method to allow for a subclass to prevent the options menu from being created.
	 * 
	 * @param menu	the menu
	 */
	protected boolean onPreCreateOptionsMenu(Menu menu) {
		return true;
	}

	/**
	 * Provides an override method to allow for creation of options menu while enforcing the
	 * invocation of {@link #onPreCreateOptionsMenu}.
	 * 
	 * @param menu	the menu
	 */
	protected void onCreateOptionsMenuOverride(Menu menu) {
	}
	
	/**
	 * Provides the functionality to override the window flags.
	 */
	protected void setWindowFlags() {
		getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
	}

	/**
	 * PhoneGapViewClient extends {@link GapViewClient}, the default PhoneGap implementation of {@link WebViewClient}.
	 * This implementation shows a loading dialog when a page is loading, logs SCORM-like 'experienced' tracking entries and has a better implementation for handling navigating to non-web files through anchor links.
	 */
	private class PhoneGapViewClient extends GapViewClient {
		/* Fields */
		
		private DroidGapActionBar context = null;
		private ProgressDialog progressDialog = null;
		private boolean suppressFileNotFoundDialogs = false;
		private String fileNotFoundUrl = null;

		/* Constructor */
		
		public PhoneGapViewClient(DroidGapActionBar ctx) {
			super(ctx);
			this.context = ctx;
		}
		
		/* Methods */
		
		/**
		 * Provides additional functionality to show a loading dialog and better handle navigating to non-web files through anchor links.
		 */
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			if (suppressFileNotFoundDialogs) {
				return;
			}
			
			// Fix for videos not playing from courses under Android 2
			if (EnvironmentUtils.isRunningAndroid2() && shouldHandleNativeFile(url)) {
				// pass false value as trackExperienced because onPageFinished() will automatically track the resource access
				boolean result = navigateToNativeFile(view.getContext(), url, false);
				
				if (!result) {
					view.stopLoading();
				}
				
				return;
			}
			
			super.onPageStarted(view, url, favicon);

			// check that the activity is not exiting
			if (!isFinishing()) {
				if (progressDialog != null) {
					progressDialog.dismiss();
					progressDialog = null;
				}

				String progressDialogMessage = context.getString(R.string.dialog_loading_message);
				progressDialog = ProgressDialog.show(context, null, progressDialogMessage, true, false);
			}
		}

		/**
		 * Provides additional functionality to close the loading dialog and parse and log SCORM-like 'experienced' tracking entries.
		 */
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);

			loadedUrl = url;

			// check that the activity is not exiting
			if (!isFinishing()) {

				if (progressDialog != null) {
					progressDialog.dismiss();
					progressDialog = null;
				}
				
				// parse url
				parseUrlAndLogExperiencedTrack(url);
			}
		}
		
		/**
		 * Provides additional functionality to close the loading dialog and better handle invalid file not found errors. 
		 */
		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

			// check to see if the failing url has already been noted
			// use default logic for Gingerbread 
			if (TextUtils.isEmpty(fileNotFoundUrl) || !fileNotFoundUrl.equalsIgnoreCase(failingUrl) || EnvironmentUtils.isRunningGingerbread()) {
				super.onReceivedError(view, errorCode, description, failingUrl);
			} else {
				// otherwise ignore the error and reset fileNotFoundUrl
				fileNotFoundUrl = null;
				
				view.stopLoading();
			}

			if (progressDialog != null) {
				progressDialog.dismiss();
				progressDialog = null;
			}
		}
		
		/**
		 * Provides additional functionality to better handle navigating to non-web files through anchor links.
		 */
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// If the file is a video file, open it without going through the standard logic
			if (shouldHandleNativeFile(url)) {
				// pass true value as trackExperienced because onPageStarted() and onPageFinished() will be automatically fired
				navigateToNativeFile(view.getContext(), url, true);
				
				// return true to override
				return true;
			} else {			
				return super.shouldOverrideUrlLoading(view, url);
			}
		}
		
		private boolean shouldHandleNativeFile(String url) {
			return (!TextUtils.isEmpty(url) && url.endsWith("mp4") || url.endsWith("epub") || url.endsWith("mobi") || url.endsWith("pdf"));
		}
		
		private boolean navigateToNativeFile(Context context, String url, boolean trackExperienced) {
			Uri data = Uri.parse(url);
			
			// persist the data uri so we can track it
			url = data.getPath();
			
			File file = new File(url);
			
			// check that the file exists
			if (!file.exists()) {
				if (!suppressFileNotFoundDialogs) {				
					// show error
					int appNameResourceId = ResourceHelper.getResourceIdByName(context, ResourceItemType.string, "app_name");
					String appName = null;
					String message = context.getString(R.string.file_not_found_message);
					
					if (appNameResourceId > 0) {
						appName = context.getString(appNameResourceId);
					}
						
					// set suppressFileNotFoundDialogs
					suppressFileNotFoundDialogs = true;
					
					// show alert dialog
					DialogHelper.showAlertDialog(context, appName, message, new DialogInterface.OnClickListener() {
	
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// disable FNF dialog suppression
							suppressFileNotFoundDialogs = false;
	
							// dismiss dialog
							dialog.dismiss();
						}					
					});
								
					// log the url
					fileNotFoundUrl = data.toString();
				}
				
				// return
				return false;
			}
			
			if (NativeFileHelper.canOpenFile(context, file)) {
				if (trackExperienced) {				
					// parse and track url
					parseUrlAndLogExperiencedTrack(url);
				}

				// fire the intent to open the file
				Intent intent = NativeFileHelper.getOpenFileIntent(file);
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

				DialogHelper.showAlertDialog(BasePhoneGapActivity.this, title, formattedMessage);
			}
			
			return true;
		}
	}
}