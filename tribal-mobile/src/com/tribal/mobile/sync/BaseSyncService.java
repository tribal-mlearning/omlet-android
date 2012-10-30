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

package com.tribal.mobile.sync;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.tribal.mobile.Framework;
import com.tribal.mobile.api.Client;
import com.tribal.mobile.api.sync.SyncInterval;
import com.tribal.mobile.api.tracking.TrackingEntry;
import com.tribal.mobile.api.tracking.TrackingHelper;
import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.base.ServiceBase;
import com.tribal.mobile.net.AuthHttpConnection;
import com.tribal.mobile.net.ConnectivityMode;
import com.tribal.mobile.net.HttpConnection.Callback;
import com.tribal.mobile.phonegap.MFSettingsKeys;
import com.tribal.mobile.phonegap.MFStoreType;
import com.tribal.mobile.util.ConnectivityUtils;
import com.tribal.mobile.util.NativeSettingsHelper;
import com.tribal.mobile.util.ServiceLayerExceptionHelper;
import com.tribal.mobile.util.resources.ResourceHelper;
import com.tribal.mobile.util.resources.ResourceItemType;
import com.tribal.mobile.util.resources.XmlResourceLookups;

/**
 * Class provides the logic required to queue up and send course/package progress data to the backend.
 * 
 * @author Jon Brasted
 */
public abstract class BaseSyncService extends ServiceBase implements OnSharedPreferenceChangeListener {
	/* Fields */

	public final static int DEFAULT_DEFER_DURATION = 15;
	
	// get sync url
	private String syncUrl;

	protected NotificationManager notificationManager;

	private Map<String, BroadcastReceiver> broadcastReceivers;

	private boolean continueSynchronisation = true;
	private boolean hasApplicationShutdown = false;

	protected BaseApplication baseApplication;

	private SharedPreferences preferences;

	private SyncInterval syncInterval;
	private long syncIntervalDuration;

	private Timer timer;

	private boolean syncInProgress = false;
	private boolean sleepSyncProcessThread = false;
	
	private Timer deferTimer;
	private int syncRequests = 0;
	private boolean deferTimerIsRunning = false;

	/* Constructor */

	@Override
	public void onCreate() {
		super.onCreate();

		baseApplication = (BaseApplication) getApplication();

		Log.d("Sync Services", "Create Sync Service");

		// get notification manager
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// create new broadcast receivers collection
		broadcastReceivers = new HashMap<String, BroadcastReceiver>();

		// register broadcast receivers
		addBroadcastReceiver(BroadcastActions.ManualSyncRequested);
		addBroadcastReceiver(BroadcastActions.SyncCancelled);
		addBroadcastReceiver(ConnectivityManager.CONNECTIVITY_ACTION);
		addBroadcastReceiver(BroadcastActions.ApplicationIsRunningChanged);

		// check defaults set
		checkDefaultsSet();

		// register on shared preference change listener
		preferences = PreferenceManager
				.getDefaultSharedPreferences(baseApplication);

		// trigger preferences changes so the settings are retrieved
		onSharedPreferenceChanged(MFSettingsKeys.DATA_USE);
		onSharedPreferenceChanged(MFSettingsKeys.SYNC_INTERVAL);

		// register listener
		preferences.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onDestroy() {
		// unregister shared preference change listener
		if (preferences != null) {
			preferences.unregisterOnSharedPreferenceChangeListener(this);
		}

		// unregister the broadcast receivers
		if (!broadcastReceivers.isEmpty()) {
			// get the set of keys
			Set<String> broadcastReceiverKeySet = broadcastReceivers.keySet();

			// get the iterator
			Iterator<String> broadcastReceiverKeySetIterator = broadcastReceiverKeySet
					.iterator();

			// get the keys
			String[] broadcastReceiverKeys = new String[broadcastReceiverKeySet
					.size()];

			for (int index = 0, broadcastReceiverKeySetSize = broadcastReceiverKeys.length; index < broadcastReceiverKeySetSize; index++) {
				broadcastReceiverKeys[index] = broadcastReceiverKeySetIterator
						.next();
			}

			// remove all the receivers
			for (String intentAction : broadcastReceiverKeys) {
				removeBroadcastReceiver(intentAction);
			}
		}

		// stop service timer
		stopServiceTimer();

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// get the sync url
		if (intent.hasExtra(IntentParameterConstants.Url)) {
			syncUrl = intent.getStringExtra(IntentParameterConstants.Url);
		} else {
			Log.e(this.getClass().getPackage().getImplementationTitle()
					+ this.getClass().getSimpleName(),
					"No URL specified for sync service.");
		}

		return Service.START_NOT_STICKY;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		onSharedPreferenceChanged(key);
	}

	private void checkDefaultsSet() {
		// get the sync interval
		String syncIntervalString = Framework.getClient().getValue(
				MFStoreType.GLOBAL, MFSettingsKeys.SYNC_INTERVAL);

		if (TextUtils.isEmpty(syncIntervalString)) {
			// get settings resource id
			int settingsResourceId = ResourceHelper.getResourceIdByName(baseApplication, ResourceItemType.xml, XmlResourceLookups.Preferences);

			// load defaults
			PreferenceManager.setDefaultValues(baseApplication,
					settingsResourceId, true);
		}
	}

	private void onSharedPreferenceChanged(String key) {
		if (MFSettingsKeys.SYNC_INTERVAL.equalsIgnoreCase(key)) {
			stopServiceTimer();

			String syncIntervalModeString = preferences.getString(
					MFSettingsKeys.SYNC_INTERVAL,
					SyncInterval.fifteenMinutes.toString());
			syncInterval = Enum.valueOf(SyncInterval.class,
					syncIntervalModeString);

			startServiceTimer();
		}
	}

	@Override
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

	@Override
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

	@Override
	protected boolean isBroadcastReceiverActionRegistered(String intentAction) {
		return (broadcastReceivers != null && broadcastReceivers
				.containsKey(intentAction));
	}

	@Override
	protected void onBroadcastReceive(Intent intent) {
		String action = intent.getAction();

		Log.d("Sync Services", "Beginning Sync");

		try {
			if (BroadcastActions.ManualSyncRequested.equalsIgnoreCase(action)) {
				// trigger manual sync
				startSync(false, intent);
			} else if (BroadcastActions.SyncCancelled.equalsIgnoreCase(action)) {
				// cancel sync
				cancelSync();
			} else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
				// call base onConnectivityChanged method
				handleConnectivityChanged(intent);
			} else if (BroadcastActions.ApplicationIsRunningChanged.equals(action)) {
				// call base onApplicationIsRunningChanged
				onApplicationIsRunningChanged(intent);
			}
		} catch (Exception exc) {
			Log.d("Sync Error", exc.getMessage());
		}
	}
	
	private void handleConnectivityChanged(Intent intent) {
		boolean isNetworkAvailable = isNetworkAvailable();
		
		// try and start syncing
		if (isNetworkAvailable && syncRequests > 0) {
			syncRequests--;
			startSync(false, null);
		}
		
		// call onConnectivityChanged
		onConnectivityChanged(isNetworkAvailable());
	}
	
	// Check if connected to the internet
	public boolean isNetworkAvailable() {
		try {		
			String syncConnectivityModeString = NativeSettingsHelper.getInstance(getApplicationContext()).checkAndGetNativeSetting(MFSettingsKeys.DATA_USE, ConnectivityMode.wifiAndCellularData.toString());
			
			ConnectivityMode connectivityMode = Enum.valueOf(ConnectivityMode.class, syncConnectivityModeString);
			
			boolean canConnect = ConnectivityUtils.canConnect(this, connectivityMode);
			
			return canConnect;
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		
		return false;
	}
	
	protected void onConnectivityChanged(boolean isConnected) {
	}
	
	private void onApplicationIsRunningChanged(Intent intent) {
		if (intent.hasExtra(IntentParameterConstants.OldValue) && intent.hasExtra(IntentParameterConstants.NewValue)) {
			boolean oldValue = intent.getBooleanExtra(IntentParameterConstants.OldValue, true);
			boolean newValue = intent.getBooleanExtra(IntentParameterConstants.NewValue, true);
			
			if (!oldValue && newValue) {
				// check to see if there any sync requests
				if (syncRequests > 0) {
					syncRequests--;					
					startSync(false, null);
				}
			}
		}
	}

	private void startServiceTimer() {
		// get the sync interval
		String syncIntervalString = Framework.getClient().getValue(
				MFStoreType.GLOBAL, MFSettingsKeys.SYNC_INTERVAL);

		if (TextUtils.isEmpty(syncIntervalString)) {
			// get preferences resource id
			int settingsResourceId = ResourceHelper.getResourceIdByName(baseApplication, ResourceItemType.xml, XmlResourceLookups.Preferences);

			// load defaults
			PreferenceManager.setDefaultValues(baseApplication,
					settingsResourceId, false);

			syncIntervalString = preferences.getString(
					MFSettingsKeys.SYNC_INTERVAL,
					SyncInterval.fifteenMinutes.toString());
			Framework.getClient().setValue(MFStoreType.GLOBAL,
					MFSettingsKeys.SYNC_INTERVAL,
					SyncInterval.fifteenMinutes.toString());
		}

		syncInterval = Enum.valueOf(SyncInterval.class, syncIntervalString);

		switch (syncInterval) {
			case fiveMinutes: {
				syncIntervalDuration = 5 * 60 * 1000;
				break;
			}
			case tenMinutes: {
				syncIntervalDuration = 10 * 60 * 1000;
				break;
			}
			case fifteenMinutes: {
				syncIntervalDuration = 15 * 60 * 1000;
				break;
			}
			case manual: {
				break;
			}
		}

		// set up timer
		switch (syncInterval) {
			case fiveMinutes:
			case tenMinutes:
			case fifteenMinutes: {
				// create timer
				timer = new Timer(false);
				timer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						startSync(true, null);
					}
				}, syncIntervalDuration, syncIntervalDuration);
				break;
			}
			case manual: {
				break;
			}
		}
	}

	private void stopServiceTimer() {
		// disable timer
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
	}

	private void startSync(Boolean isAutomaticSync, Intent intent) {
		// check app tracking is enabled

		boolean isApplicationRunning = baseApplication.isRunning();

		// if app is in background then don't sync
		if (!isApplicationRunning) {
			// add a sync request if the request was not automatic
			if (!isAutomaticSync) {
				syncRequests++;
			}
			
			return;
		}

		Client client = Framework.getClient();

		// or if the user is not logged in
		if (client == null || client.getUserUsername() == null) {
			return;
		}
		
		boolean isDeferSyncRequest = (intent != null && intent.hasExtra(IntentParameterConstants.DeferDurationInSeconds));
		
		// if timer is running when we receive another request, look to see if we have another deferred request
		// if so, cancel the second defer request, otherwise stop the timer and carry on
		if (deferTimerIsRunning) {
			if (isDeferSyncRequest) {
				// cancel the second request, i.e. return
				return;
			} else {
				// stop the timer and carry on
				if (deferTimer != null) {
					deferTimerIsRunning = false;				
					deferTimer.cancel();
				}
			}
		} else {
			if (isDeferSyncRequest) {
				int deferDuration = intent.getIntExtra(IntentParameterConstants.DeferDurationInSeconds, BaseSyncService.DEFAULT_DEFER_DURATION) * 1000;
				
				// start the timer
				deferTimerIsRunning = true;
				
				deferTimer = new Timer();
				deferTimer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						deferTimerIsRunning = false;
						deferTimer.cancel();
						
						// start a new sync
						startSync(true, null);
					}
				}, deferDuration, deferDuration);
				
				// return
				return;
			}
		}
		
		// or if a sync is already taking place
		if (syncInProgress) {
			// add a sync to the queue and return
			syncRequests++;
			return;
		}

		Log.d("Sync Services", "startSync");

		boolean isNetworkAvailable = isNetworkAvailable();
		
		if (isNetworkAvailable) {
			if (syncRequests > 0) {
				syncRequests--;
			}
			
			// spin up a new thread to do the sync
			Thread thread = new Thread(new SyncRunnable(isAutomaticSync));
			thread.start();
		} else {
			// add a sync to the queue and return
			syncRequests++;
		}
	}

	private void cancelSync() {
		// cancel sync by setting a flag that the thread will pick up
		continueSynchronisation = false;
	}

	/**
	 * Override method to raise a sync progress notification 
	 * 
	 * @param progress			progress value
	 * @param max				max progress value
	 * @param isAutomaticSync	whether the syncronisation was triggered automatically
	 */
	protected void raiseSyncProgressNotification(int progress, int max, boolean isAutomaticSync) {
	}

	/**
	 * Override method to raise a sync cancelled notification
	 * 
	 * @param isAutomaticSync	whether the syncronisation was triggered automatically
	 */
	protected void raiseSyncCancelledNotification(boolean isAutomaticSync) {
	}

	/**
	 * Override method to raise a sync failed notification
	 * 
	 * @param isAutomaticSync	whether the syncronisation was triggered automatically
	 * @param exception			the exception
	 */
	protected void raiseSyncFailedNotification(boolean isAutomaticSync, Exception exception) {
	}
	
	/**
	 * Syncronisation worker logic.
	 * 
	 * @author Jon Brasted
	 */
	private class SyncRunnable implements Runnable {
		/* Fields */
		
		boolean isAutomaticSync = true;
		
		/* Constructor */
		
		public SyncRunnable(boolean isAutomaticSync) {
			this.isAutomaticSync = isAutomaticSync;
		}
		
		@Override
		public void run() {
			Log.d("Sync Services", "SyncRunnable");
			Looper.prepare();

			// set syncInProgress to true
			syncInProgress = true;
			
			// set continueSynchronisation to true
			continueSynchronisation = true;

			// log sync in native settings
			try {
				Framework.getClient().setValue(MFStoreType.GLOBAL,
						MFSettingsKeys.LAST_SYNCHRONISED_TIME,
						Calendar.getInstance().toString());
			} catch (Exception e) {
				e.printStackTrace();
			}

			// start the sync
			sync();
		}

		private void sync() {
			// get the list of IDs
			Log.d("Sync Services", "sync");
			List<TrackingEntry> trackingEntryList = baseApplication.getDatabaseHelper().getTrackingEntriesList();
			int trackingEntryListSize = trackingEntryList.size();

			if (trackingEntryListSize > 0) {
				// log the sync
				TrackingHelper.getInstance().trackSyncWithMobileFrameworkSender();
				
				// get the list again so we get the sync item
				trackingEntryList = baseApplication.getDatabaseHelper().getTrackingEntriesList();
				trackingEntryListSize = trackingEntryList.size();
			}
			
			// send broadcast
			Intent syncStartedIntent = new Intent();
			syncStartedIntent.setAction(BroadcastActions.SyncStarted);
			sendBroadcast(syncStartedIntent);

			// raise notification
			raiseSyncProgressNotification(0, trackingEntryListSize, isAutomaticSync);

			syncTrackingEntries(trackingEntryList);

			// syncing has finished so shut down the service
			if (hasApplicationShutdown) {
				stopSelf();
			}

			Log.d("Sync Services", "endSync");
		}

		private void syncTrackingEntries(List<TrackingEntry> trackingEntryList) {
			int trackingEntryListTotalSize = trackingEntryList.size();
			
			while (continueSynchronisation && !trackingEntryList.isEmpty()) {
				// set sleepSyncProcessThread
				sleepSyncProcessThread = true;

				// raise notification
				raiseSyncProgressNotification((trackingEntryListTotalSize - trackingEntryList.size()), trackingEntryListTotalSize, isAutomaticSync);
				
				// start next sync
				syncNextTrackingEntry(trackingEntryList);

				// sleep the thread while we wait for the sync return call
				while (sleepSyncProcessThread) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			// if there was no tracking entries, send a complete notification
			if (trackingEntryList.isEmpty()) {
				raiseSyncProgressNotification(1, 1, isAutomaticSync);
			}

			// reset continueSynchronisation
			continueSynchronisation = true;
			
			// reset syncInProgress
			syncInProgress = false;
			
			// look to see if there is more sync requests to execute
			if (syncRequests > 0) {
				syncRequests--;
				startSync(false, null);
			}
		}

		private void syncNextTrackingEntry(final List<TrackingEntry> trackingEntryList) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					// get the first entry
					final TrackingEntry trackingEntry = trackingEntryList.get(0);
					
					String jsonContentString = trackingEntry.getJsonContentString();
					
					// start the process
					AuthHttpConnection.post(syncUrl, jsonContentString,
							new Callback() {
								@Override
								public void onSuccess(String data) {
									// remove the tracking entry from the database
									baseApplication.getDatabaseHelper().removeTrackingEntry("" + trackingEntry.getId());
									
									// remove the top entry from the list
									trackingEntryList.remove(0);
									
									// set sleep sync process thread variable to false
									sleepSyncProcessThread = false;
								}
		
								@Override
								public void onStart() {
								}
		
								@Override
								public void onError(Throwable t) {
									ServiceLayerExceptionHelper.getInstance().processException(t, baseApplication.getApplicationContext());
		
									// send broadcast to inform app that sync has failed
									Intent failedSyncBroadcast = new Intent();
									failedSyncBroadcast.setAction(BroadcastActions.SyncFailed);
									sendBroadcast(failedSyncBroadcast);
									
									raiseSyncFailedNotification(isAutomaticSync, new Exception(t));
									
									// set continue to false
									continueSynchronisation = false;
									
									// set sleepSyncProcessThread to false
									sleepSyncProcessThread = false;
								}
							}, baseApplication.getApplicationContext()
					);
				}
			};
			
			Thread thread = new Thread(runnable);
			thread.start();
		}
	}
}