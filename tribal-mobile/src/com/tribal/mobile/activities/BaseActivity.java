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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.tribal.mobile.Framework;
import com.tribal.mobile.R;
import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.ClearApplicationDataCompleted;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.download.DownloadBroadcastActions;
import com.tribal.mobile.net.ConnectivityMode;
import com.tribal.mobile.phonegap.MFSettingsKeys;
import com.tribal.mobile.preferences.PrivateSettingsKeys;
import com.tribal.mobile.util.ConnectivityUtils;
import com.tribal.mobile.util.DialogHelper;
import com.tribal.mobile.util.IntentUtils;
import com.tribal.mobile.util.LoadingDialogHelper;
import com.tribal.mobile.util.NativeSettingsHelper;
import com.tribal.mobile.util.ServiceLayerExceptionHelper;
import com.tribal.mobile.util.ViewHelper;
import com.tribal.mobile.util.database.BaseDatabaseHelper;

/**
 * Base activity for mobile framework activities except for {@link BasePhoneGapActivity}, as that has a different base class. 
 * BaseActivity provides convenience methods for functions such as registering intent broadcast receivers, sending intent broadcasts, handling logout, showing alert and error dialogs and handling changes to preferences. 
 */
public abstract class BaseActivity extends SherlockFragmentActivity implements ClearApplicationDataCompleted, OnSharedPreferenceChangeListener {
	/* Fields */

	private Map<String, BroadcastReceiver> broadcastReceivers;

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
			Log.e("com.tribal.mobile.activities.BaseActivity", "getApplication() does not inherit from BaseApplication, which is required.");
			return null;
		}
	}

	/* Methods */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		broadcastReceivers = new HashMap<String, BroadcastReceiver>();

		addBroadcastReceiver(BroadcastActions.Logout);

		View view = findViewById(android.R.id.content);

		if (view != null) {
			ViewHelper.setTypeFace(view);
		}

		// Make actionbar use up button by default
		ActionBar actionBar = getSupportActionBar();

		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case android.R.id.home:
				// If the up button is pressed then for now just do a back
				// by calling finish, this may change when we have more
				// complicated hierarchies though
				finish();
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onDestroy() {
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
	protected void onPause() {
		super.onPause();

		// keep track of if app is in background or not
		getBaseApplication().setRunning(false);

		// unsubscribe from the prelogout broadcast
		if (isBroadcastReceiverActionRegistered(BroadcastActions.PreLogout)) {
			removeBroadcastReceiver(BroadcastActions.PreLogout);
		}

		// unsubscribe from the ConnectivityManager.CONNECTIVITY_ACTION
		// broadcast
		if (isBroadcastReceiverActionRegistered(ConnectivityManager.CONNECTIVITY_ACTION)) {
			removeBroadcastReceiver(ConnectivityManager.CONNECTIVITY_ACTION);
		}

		// unsubscribe from the show dialog broadcast
		if (isBroadcastReceiverActionRegistered(BroadcastActions.ShowDialog)) {
			removeBroadcastReceiver(BroadcastActions.ShowDialog);
		}

		// unsubscribe from the refresh connectivity broadcast
		if (isBroadcastReceiverActionRegistered(BroadcastActions.RefreshConnectivityChanged)) {
			removeBroadcastReceiver(BroadcastActions.RefreshConnectivityChanged);
		}

		// unsubscribe to the show toast broadcast
		if (isBroadcastReceiverActionRegistered(BroadcastActions.ShowToast)) {
			removeBroadcastReceiver(BroadcastActions.ShowToast);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// keep track of if app is in background or not
		getBaseApplication().setRunning(true);

		// ensure that the Framework is still working
		if (Framework.getServer() == null || Framework.getClient() == null) {
			BaseApplication baseApplication = getBaseApplication();
			
			if (baseApplication != null) {
				baseApplication.resetApplication();
				return;
			}
		}
		
		// subscribe to the prelogout broadcast
		if (!isBroadcastReceiverActionRegistered(BroadcastActions.PreLogout)) {
			addBroadcastReceiver(BroadcastActions.PreLogout);
		}

		// subscribe to the ConnectivityManager.CONNECTIVITY_ACTION broadcast
		if (!isBroadcastReceiverActionRegistered(ConnectivityManager.CONNECTIVITY_ACTION)) {
			addBroadcastReceiver(ConnectivityManager.CONNECTIVITY_ACTION);
		}

		// subscribe to the show dialog broadcast
		if (!isBroadcastReceiverActionRegistered(BroadcastActions.ShowDialog)) {
			addBroadcastReceiver(BroadcastActions.ShowDialog);
		}

		// subscribe to the refresh connectivity broadcast
		if (!isBroadcastReceiverActionRegistered(BroadcastActions.RefreshConnectivityChanged)) {
			addBroadcastReceiver(BroadcastActions.RefreshConnectivityChanged);
		}

		// subscribe to the show toast broadcast
		if (!isBroadcastReceiverActionRegistered(BroadcastActions.ShowToast)) {
			addBroadcastReceiver(BroadcastActions.ShowToast);
		}

		// process deferred intent list
		List<Intent> deferredBroadcasts = IntentUtils.getInstance().popDeferredBroadcasts();

		for (Intent intent : deferredBroadcasts) {
			onBroadcastReceive(intent);
		}
	}

	/**
	 * Provides functionality to handle changes to shared preferences for the {@link MFSettingsKeys} keys <code>DATA_USE</code> and <code>ACCEPT_SSL_SELF_SIGNED_CERTS</code>.
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (MFSettingsKeys.DATA_USE.equalsIgnoreCase(key) || PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS.equalsIgnoreCase(key)) {
			if (PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS.equalsIgnoreCase(key)) {
				// reset ServiceLayerExceptionHelper's
				// hasShownSSLCertificateError property
				ServiceLayerExceptionHelper.getInstance().setHasShownSSLCertificateError(false);

				// reset setHasConnectedToSelfSignedCertificatesWhilstNotPermitted property
				ConnectivityUtils.setHasConnectedToSelfSignedCertificatesWhilstNotPermitted(false);
			}

			String syncConnectivityModeString = sharedPreferences.getString(MFSettingsKeys.DATA_USE, ConnectivityMode.wifiAndCellularData.toString());
			ConnectivityMode connectivityMode = Enum.valueOf(ConnectivityMode.class, syncConnectivityModeString);

			boolean canConnect = ConnectivityUtils.canConnect(this, connectivityMode);

			// broadcast the network changed message
			onConnectivityChanged(canConnect);
		}
	}

	/**
	 * Provides functionality to handle shared preference changed.
	 * Invokes {@link #onSharedPreferenceChanged(SharedPreferences, String) onSharedPreferenceChanged} if <code>intent</code> contains a parameter called <code>Key</code>. 
	 * 
	 * @param intent	an intent
	 */
	protected void onSharedPreferenceChanged(Intent intent) {
		if (intent.hasExtra(IntentParameterConstants.Key)) {
			String key = intent.getStringExtra(IntentParameterConstants.Key);

			// return shared preferences
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

			// invoke onSharedPreferences
			onSharedPreferenceChanged(sharedPreferences, key);
		}
	}

	/**
	 * Provides functionality to show a toast notification.
	 * 
	 * @param intent	an intent containing a key-value pair for <code>Message</code> and a key-value pair for <code>Duration</code>
	 */
	protected void onShowToast(Intent intent) {
		if (intent.hasExtra(IntentParameterConstants.Message) && intent.hasExtra(IntentParameterConstants.Duration)) {

			String message = intent.getStringExtra(IntentParameterConstants.Message);
			int duration = intent.getIntExtra(IntentParameterConstants.Duration, Toast.LENGTH_SHORT);

			Toast.makeText(this, message, duration).show();
		}
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
		if (broadcastReceivers != null && broadcastReceivers.containsKey(intentAction)) {
			BroadcastReceiver receiver = broadcastReceivers.get(intentAction);

			// unregister the receiver
			unregisterReceiver(receiver);

			// remove the receiver from the map
			broadcastReceivers.remove(intentAction);
		}
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
	 *  <code>ShowLoadingDialog</code><br />
	 *  <code>DismissLoadingDialog</code><br />
	 *  <code>ShowDialog</code><br />
	 *  <code>RefreshConnectivityChanged</code><br />
	 *  <code>SharedPreferenceChanged</code><br />
	 *  <code>ShowToast</code><br />
	 *  <code>CONNECTIVITY_ACTION</code>
	 * </p>
	 */
	private void onBroadcastReceive(Intent intent) {
		String intentAction = intent.getAction();

		if (BroadcastActions.PreLogout.equals(intentAction)) {
			// execute prelogout logic
			onPreLogout(intent);
		} else if (BroadcastActions.Logout.equals(intentAction)) {
			// finish the activity
			finish();
		} else if (BroadcastActions.ShowLoadingDialog.equals(intentAction)) {
			// show loading dialog
			showLoadingDialog();
		} else if (BroadcastActions.DismissLoadingDialog.equals(intentAction)) {
			// hide loading dialog
			dismissLoadingDialog();
		} else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intentAction)) {
			// call base onConnectivityChanged method
			handleConnectivityChanged(intent);
		} else if (BroadcastActions.ShowDialog.equals(intentAction)) {
			// call base show dialog method
			showDialog(intent);
		} else if (BroadcastActions.RefreshConnectivityChanged.equals(intentAction)) {
			// call base onConnectivityChanged method
			onConnectivityChanged(isNetworkAvailable());
		} else if (BroadcastActions.SharedPreferenceChanged.equals(intentAction)) {
			// call on shared preference changed
			onSharedPreferenceChanged(intent);
		} else if (BroadcastActions.ShowToast.equals(intentAction)) {
			// call on show toast
			onShowToast(intent);

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

		cancelAllDownloads();

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

	private void cancelAllDownloads() {
		// Send broadcast to cancel any downloads
		Intent cancelDownloadsIntent = new Intent();
		cancelDownloadsIntent.setAction(DownloadBroadcastActions.PackageDownloadCancelAllRequest);
		sendBroadcast(cancelDownloadsIntent);
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
	public void dismissLoadingDialog() {
		dismissLoadingDialog(false);
	}

	/**
	 * Provides functionality to dismiss the currently open loading dialog (if there is one), while using a counter to track the number of open dialogs and dialog requests (if a request to open a dialog is received when a dialog is still open).
	 * 
	 * @param useCount	whether to count the number of open dialogs (and dialog requests).
	 */
	public void dismissLoadingDialog(boolean useCount) {
		LoadingDialogHelper.getInstance().dismissLoadingDialog(this, useCount);
	}

	private void handleConnectivityChanged(Intent intent) {
		// call isNetworkAvailable
		onConnectivityChanged(isNetworkAvailable());
	}

	/**
	 * Provides functionality to handle a change in connectivity on the device. Will cancel all downloads if <code>isConnected</code> has a value of <code>false</code>.  
	 * 
	 * @param isConnected	whether the device has connectivity
	 */
	protected void onConnectivityChanged(boolean isConnected) {
		// If we can't connect anymore then cancel all downloads
		if (!isConnected) {
			cancelAllDownloads();
		}
	}

	/**
	 * Provides functionality to open a dialog. The supplied intent should provide key-value pairs for <code>Title</code> and <code>Message</code>.
	 * Method will invoke the method {@link DialogHelper#showAlertDialog(Context, String, String) showAlertDialog}.
	 * 
	 * @param intent	the intent
	 */
	protected void showDialog(Intent intent) {
		// update if we need to do anything more complex
		if (intent.hasExtra(IntentParameterConstants.Title) && intent.hasExtra(IntentParameterConstants.Message)) {
			String title = intent.getStringExtra(IntentParameterConstants.Title);
			String message = intent.getStringExtra(IntentParameterConstants.Message);

			DialogHelper.showAlertDialog(this, title, message);
		}
	}

	/**
	 * Provides functionality to show an error dialog with the supplied <code>title</code> and <code>message</code>.
	 * 
	 * @param title		the title
	 * @param message	the message
	 */
	protected void showErrorDialog(String title, String message) {

		// show error dialog
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);

		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		alertDialog.show();
	}

	/**
	 * Provides functionality to process a JSON string into a JSON object.
	 * 
	 * @param jsonString	the JSON string
	 * @return				a {@link JSONObject} containing the data inside the <code>jsonString</code> object
	 */
	protected JSONObject processStringToJSONObject(String jsonString) {
		// JSON-ify parameters
		try {
			return new JSONObject(jsonString);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Provides functionality to return whether a network connection is available to the application, given the value of the preference setting <code>DATA_USE</code> key specified by {@link MFSettingsKeys}.
	 *  
	 * @return	whether a network connection is available to the application
	 */
	// Check if connected to the internet
	public boolean isNetworkAvailable() {
		try {
			String syncConnectivityModeString = NativeSettingsHelper.getInstance(getApplicationContext()).checkAndGetNativeSetting(MFSettingsKeys.DATA_USE,
					ConnectivityMode.wifiAndCellularData.toString());

			ConnectivityMode connectivityMode = Enum.valueOf(ConnectivityMode.class, syncConnectivityModeString);

			boolean canConnect = ConnectivityUtils.canConnect(this, connectivityMode);

			return canConnect;
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		return false;
	}
}