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

package com.tribal.mobile.fragments;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.tribal.mobile.Framework;
import com.tribal.mobile.activities.BaseActivity;
import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.net.ConnectivityMode;
import com.tribal.mobile.phonegap.MFSettingsKeys;
import com.tribal.mobile.util.ConnectivityUtils;
import com.tribal.mobile.util.NativeSettingsHelper;

/**
 * Base activity for mobile framework fragments based on the {@link SherlockFragment}.
 * Provides convenience methods for working with intent broadcasts.
 * 
 * @author Jack Kierney
 */
public class BaseFragment extends SherlockFragment {
	/* Fields */

	private Map<String, BroadcastReceiver> broadcastReceivers;

	/* Properties */

	/**
	 * Returns the associated instance of {@link BaseActivity}.
	 * 
	 * @return	the associated instance of {@link BaseActivity}
	 */
	public BaseActivity getBaseActivity() {
		return (BaseActivity) getActivity();
	}

	/* Methods */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		broadcastReceivers = new HashMap<String, BroadcastReceiver>();
	}

	@Override
	public void onDestroy() {
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

		super.onDestroy();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		// unsubscribe from the ConnectivityManager.CONNECTIVITY_ACTION broadcast
		if (isBroadcastReceiverActionRegistered(ConnectivityManager.CONNECTIVITY_ACTION)) {
			removeBroadcastReceiver(ConnectivityManager.CONNECTIVITY_ACTION);
		}		
		
		// unsubscribe from the refresh connectivity broadcast
		if (isBroadcastReceiverActionRegistered(BroadcastActions.RefreshConnectivityChanged)) {
			removeBroadcastReceiver(BroadcastActions.RefreshConnectivityChanged);
		}
		
		// unsubscribe from the show toast
		if (isBroadcastReceiverActionRegistered(BroadcastActions.ShowToast)) {
			removeBroadcastReceiver(BroadcastActions.ShowToast);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// subscribe to the ConnectivityManager.CONNECTIVITY_ACTION broadcast
		if (!isBroadcastReceiverActionRegistered(ConnectivityManager.CONNECTIVITY_ACTION)) {
			addBroadcastReceiver(ConnectivityManager.CONNECTIVITY_ACTION);
		}
				
		// subscribe to the refresh connectivity broadcast
		if (!isBroadcastReceiverActionRegistered(BroadcastActions.RefreshConnectivityChanged)) {
			addBroadcastReceiver(BroadcastActions.RefreshConnectivityChanged);
		}
		
		// subscribe from the show toast
		if (!isBroadcastReceiverActionRegistered(BroadcastActions.ShowToast)) {
			addBroadcastReceiver(BroadcastActions.ShowToast);
		}
		
		// ensure that the Framework is still working
		if (Framework.getServer() == null || Framework.getClient() == null) {
			BaseActivity baseActivity = getBaseActivity();
			
			if (baseActivity != null) {
				BaseApplication baseApplication = baseActivity.getBaseApplication();
				
				if (baseApplication != null) {
					baseApplication.resetApplication();
					return;
				}
			}
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
		getActivity().getApplicationContext().registerReceiver(receiver, filter);

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
			getActivity().getApplicationContext().unregisterReceiver(receiver);

			// remove the receiver from the map
			broadcastReceivers.remove(intentAction);
		}
	}

	/**
	 * Provides functionality to send a parameterless broadcast to the supplied broadcast action.
	 * 
	 * @param intentAction	the intent action of which to fire 
	 */
	protected void sendBroadcast(String broadcastAction) {
		sendBroadcast(broadcastAction, null, null);
	}

	/**
	 * Provides functionality to asynchronously send a broadcast with the supplied intent.
	 * 
	 * @param intent	the intent of which to broadcast
	 */
	protected void sendBroadcastAsync(final Intent intent) {
		final Context context = getActivity();
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
	protected void sendBroadcast(String broadcastAction,
			Map<String, Serializable> parameters, int[] intentFlags) {
		Intent broadcast = new Intent();
		broadcast.setAction(broadcastAction);

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

		getActivity().getApplicationContext().sendBroadcast(broadcast);
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
	 *  <code>ShowToast</code><br />
	 *  <code>CONNECTIVITY_ACTION</code>
	 * </p>
	 */
	private void onBroadcastReceive(Intent intent) {
		String intentAction = intent.getAction();

		if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intentAction)) {
			// call base onConnectivityChanged method
			handleConnectivityChanged(intent);
		} else if (BroadcastActions.ShowToast.equals(intentAction)) {
			// call show toast method
			onShowToast(intent);
		} else {

			// otherwise fire override method
			onBroadcastReceiveOverride(intent);
		}
	}
	
	/**
	 * Returns a boolean to indicate whether network is available.
	 * 
	 * @return	a boolean to indicate whether network is available
	 */
	public boolean isNetworkAvailable() {
		
		boolean canConnect = false;
		
		try {
			Context context = getActivity().getApplicationContext();
			String syncConnectivityModeString = NativeSettingsHelper.getInstance(context).checkAndGetNativeSetting(MFSettingsKeys.DATA_USE, ConnectivityMode.wifiAndCellularData.toString());
			
			ConnectivityMode connectivityMode = Enum.valueOf(ConnectivityMode.class, syncConnectivityModeString);
			
			canConnect = ConnectivityUtils.canConnect(context, connectivityMode);
			
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		
		return canConnect;
	}
	
	private void handleConnectivityChanged(Intent intent){
		
		// call isNetworkAvailable
		onConnectivityChanged(isNetworkAvailable());
	}
	
	/**
	 * Provides functionality to handle a change in connectivity on the device. Will cancel all downloads if <code>isConnected</code> has a value of <code>false</code>.  
	 * 
	 * @param isConnected	whether the device has connectivity
	 */
	protected void onConnectivityChanged(boolean isConnected) {
	}

	/**
	 * Provides an override method for handling intent actions. Will be invoked by {@link #onBroadcastReceive(Intent) onBroadcastReceive} if <code>intent.getAction()</code> does not return
	 * a handled intent action.
	 */
	protected void onBroadcastReceiveOverride(Intent intent) {
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
			
			Toast.makeText(getBaseActivity(), message, duration).show();
		}
	}
}