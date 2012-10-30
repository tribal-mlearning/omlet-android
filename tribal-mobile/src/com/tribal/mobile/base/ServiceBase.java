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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Base service class. Provides protected methods to add, remove and check intent broadcast receivers.
 * 
 * @author Jon Brasted
 */
public abstract class ServiceBase extends Service {
	/* Fields */
	
	private Map<String, BroadcastReceiver> broadcastReceivers;
	
	/* Methods */
	
	@Override
	public void onCreate() {
		super.onCreate();

		// create new broadcast receivers collection
		broadcastReceivers = new HashMap<String, BroadcastReceiver>();
	}
	
	@Override
	public void onDestroy() {
		// unregister the broadcast receivers
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
	 * Provides an override method for handling intent actions.
	 * 
	 * @param intent	the intent
	 */
	protected void onBroadcastReceive(Intent intent) {
	}
}
