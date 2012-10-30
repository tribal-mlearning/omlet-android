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

import org.apache.commons.lang3.StringUtils;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.tribal.mobile.Framework;
import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.net.ConnectivityMode;
import com.tribal.mobile.phonegap.MFSettingsKeys;
import com.tribal.mobile.phonegap.MFStoreType;
import com.tribal.mobile.util.ConnectivityUtils;
import com.tribal.mobile.util.ContextUtils;
import com.tribal.mobile.util.ViewHelper;

/**
 * Class that provides base functionality to handle creating and handling application options menu.
 * 
 * @author Jon Brasted
 */
public abstract class SystemMenuProviderActivity extends MenuConsumerActivity {
	/* Fields */

	private MenuItem syncMenuItem = null;	
	private boolean isSyncing = false;
	
	/* Properties */
	
	private void setIsSyncing(boolean value) {
		isSyncing = value;
		
		if (syncMenuItem != null) {
			syncMenuItem.setEnabled(!isSyncing);
		}
	}

	/* Methods */

	@Override
	protected void onResume() {
		super.onResume();

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.SyncStarted)) {
			addBroadcastReceiver(BroadcastActions.SyncStarted);
		}

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.SyncProgressUpdate)) {
			addBroadcastReceiver(BroadcastActions.SyncProgressUpdate);
		}

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.SyncCancelled)) {
			addBroadcastReceiver(BroadcastActions.SyncCancelled);
		}

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.SyncCompleted)) {
			addBroadcastReceiver(BroadcastActions.SyncCompleted);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (isBroadcastReceiverActionRegistered(BroadcastActions.SyncStarted)) {
			removeBroadcastReceiver(BroadcastActions.SyncStarted);
		}

		if (isBroadcastReceiverActionRegistered(BroadcastActions.SyncProgressUpdate)) {
			removeBroadcastReceiver(BroadcastActions.SyncProgressUpdate);
		}

		if (isBroadcastReceiverActionRegistered(BroadcastActions.SyncCancelled)) {
			removeBroadcastReceiver(BroadcastActions.SyncCancelled);
		}

		if (isBroadcastReceiverActionRegistered(BroadcastActions.SyncCompleted)) {
			removeBroadcastReceiver(BroadcastActions.SyncCompleted);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (onPreCreateOptionsMenu()) {
			MenuInflater inflater = getSupportMenuInflater();
			inflater.inflate(R.menu.system_menu, menu);

			return true;
		}

		return false;
	}

	/**
	 * Override method to allow for a subclass to prevent the options menu from
	 * being created.
	 */
	protected boolean onPreCreateOptionsMenu() {
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// ensure that the Framework is still working
		if (Framework.getServer() == null || Framework.getClient() == null) {
			BaseApplication baseApplication = getBaseApplication();
				
			if (baseApplication != null) {
				baseApplication.resetApplication();
				return false;
			}
		}
		
		if (this.syncMenuItem == null) {
			this.syncMenuItem = menu.findItem(R.id.menu_item_sync);
		}
		
		if (Framework.getClient().getUserUsername() == null) {
			MenuItem logoutMenuItem = menu.findItem(R.id.menu_item_logout);

			// disable log out option if the user is not logged in
			if (logoutMenuItem != null) {
				logoutMenuItem.setEnabled(false);
			}

			// disable sync option if the user is not logged in
			if (syncMenuItem != null) {
				syncMenuItem.setEnabled(false);
			}
		} else {
			if (syncMenuItem != null) {
				boolean syncMenuItemEnabled = false;

				if (!isSyncing) {
					// check the connection and whether the user can sync data
					String syncConnectivityModeString = Framework.getClient().getValue(MFStoreType.GLOBAL, MFSettingsKeys.DATA_USE);

					//if null use default
					if (StringUtils.isEmpty(syncConnectivityModeString)) {
						syncConnectivityModeString = ConnectivityMode.wifiAndCellularData.toString();
					}
					ConnectivityMode connectivityMode = Enum.valueOf(ConnectivityMode.class, syncConnectivityModeString);

					if (connectivityMode != null) {
						boolean canConnect = ConnectivityUtils.canConnect(this, connectivityMode);
						syncMenuItemEnabled = canConnect;
					}
				}

				syncMenuItem.setEnabled(syncMenuItemEnabled);
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_item_info: {
				showInfoDialog();
				break;
			}
			case R.id.menu_item_sync: {
				if (syncMenuItem == null) {
					syncMenuItem = item;
				}
				
				syncMenuItem.setEnabled(false);
				sync();
				break;
			}
			case R.id.menu_item_settings: {
				showSettings();
				break;
			}
			case R.id.menu_item_logout: {
				requestLogout();
				break;
			}
			default: {
				return super.onOptionsItemSelected(item);
			}
		}

		return true;
	}

	@Override
	protected void onBroadcastReceiveOverride(Intent intent) {
		String action = intent.getAction();

		if (BroadcastActions.SyncStarted.equalsIgnoreCase(action)) {
			// sync started
			onSyncStarted(intent);
		} else if (BroadcastActions.SyncProgressUpdate.equalsIgnoreCase(action)) {
			// sync progress update
			onSyncProgressUpdate();
		} else if (BroadcastActions.SyncCancelled.equalsIgnoreCase(action)) {
			// sync cancelled
			onSyncCancelled(intent);
		} else if (BroadcastActions.SyncCompleted.equalsIgnoreCase(action)) {
			// sync started
			onSyncCompleted(intent);
		} else {
			super.onBroadcastReceiveOverride(intent);
		}
	}

	private void showInfoDialog() {
		// create view
		View view = LayoutInflater.from(this).inflate(R.layout.info_dialog, null);

		// set typeface on the view
		ViewHelper.setTypeFace(view);

		// create dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(view);
		final AlertDialog dialog = builder.show();

		// populate the version number
		TextView buildVersionTextView = (TextView) dialog.findViewById(R.id.info_dialog_build_version_number);

		if (buildVersionTextView != null) {
			// get the build version
			String versionName = ContextUtils.getVersionName(getApplicationContext());

			String buildVersionLongFormatString = getString(R.string.buildVersionLongFormatString);
			String buildVersionLong = String.format(buildVersionLongFormatString, versionName);

			buildVersionTextView.setText(buildVersionLong);
		}

		// attach on click handler to the close button
		Button closeButton = (Button) dialog.findViewById(R.id.info_dialog_close);

		if (closeButton != null) {
			closeButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
		}
	}

	private void showSettings() {
		// When the menu option is selected, launch an activity through this
		// intent
		Intent launchOptionsIntent = new Intent(this, SettingsActivity.class);

		startActivity(launchOptionsIntent);
	}

	private void sync() {
		Framework.getClient().sync();
	}

	private void onSyncStarted(Intent intent) {
		// set isSyncing
		setIsSyncing(true);

		if (intent.hasExtra(IntentParameterConstants.ShowToast) && 
			intent.getBooleanExtra(IntentParameterConstants.ShowToast, false)) {		
			
			// create a toast
			Toast toast = Toast.makeText(getApplicationContext(), R.string.sync_started_string, Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	private void onSyncProgressUpdate() {
		if (!isSyncing) {
			setIsSyncing(true);
		}
	}

	private void onSyncCancelled(Intent intent) {
		// set isSyncing
		setIsSyncing(false);

		if (intent.hasExtra(IntentParameterConstants.ShowToast) && 
			intent.getBooleanExtra(IntentParameterConstants.ShowToast, false)) {
			
			String message = getString(R.string.sync_cancelled_string);
			
			if (intent.hasExtra(IntentParameterConstants.Message)) {
				message = intent.getStringExtra(IntentParameterConstants.Message);
			}
			
			// create a toast
			Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
			toast.show();
		}
				
		// close the notification
		// get notification manager
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		// cancel the notification
		notificationManager.cancel(R.layout.notification_template);
	}

	private void onSyncCompleted(Intent intent) {
		// set isSyncing
		setIsSyncing(false);

		if (intent.hasExtra(IntentParameterConstants.ShowToast) && 
			intent.getBooleanExtra(IntentParameterConstants.ShowToast, false)) {
			
			// create a toast
			Toast toast = Toast.makeText(getApplicationContext(), R.string.sync_complete_string, Toast.LENGTH_SHORT);
			toast.show();
		}
		
		// close the notification
		// get notification manager
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		// cancel the notification
		notificationManager.cancel(R.layout.notification_template);
	}
}