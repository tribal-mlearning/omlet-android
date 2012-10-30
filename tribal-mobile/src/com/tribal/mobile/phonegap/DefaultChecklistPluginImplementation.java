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

import org.json.JSONArray;

import android.content.Intent;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;
import com.tribal.mobile.base.IntentParameterConstants;

/**
 * Default checklist PhoneGap plugin implementation.
 * 
 * @author Jon Brasted 
 */
public class DefaultChecklistPluginImplementation extends Plugin implements ChecklistPlugin {
	/* Fields */
	private static final String OPEN_NATIVE_ADD_ITEM_DIALOG = "openNativeAddItemDialog";	
	private static final String OPEN_NATIVE_INFO_DIALOG = "openNativeInfoDialog";
	public static final String OPEN_NATIVE_ADD_ITEM_DIALOG_BROADCAST_ACTION = "com.tribal.mobile.broadcastactions.openNativeChecklistAddItemDialog";
	public static final String OPEN_NATIVE_INFO_DIALOG_BROADCAST_ACTION = "com.tribal.mobile.broadcastactions.openNativeChecklistInfoDialog";
	
	/* Methods */
	
	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		if (OPEN_NATIVE_ADD_ITEM_DIALOG.equalsIgnoreCase(action)) {
			openNativeAddItemDialogFromJS(data, callbackId);
		} else if (OPEN_NATIVE_INFO_DIALOG.equalsIgnoreCase(action)) {
			openNativeInfoDialogFromJS(data, callbackId);
		}
		
		PluginResult pluginResult = new PluginResult(Status.NO_RESULT);
		
		// set keep callback to true so we can asynchronously invoke it when the value is returned
		pluginResult.setKeepCallback(true);
		return pluginResult;
	}
	
	private void openNativeAddItemDialogFromJS(JSONArray data, String callbackId) {
		openNativeAddItemDialog(callbackId);
	}

	@Override
	public void openNativeAddItemDialog(String callbackId) {
		if (this.ctx != null) {
		    Intent broadcast = new Intent();
		    
		    // set open menu item action
		    broadcast.setAction(OPEN_NATIVE_ADD_ITEM_DIALOG_BROADCAST_ACTION);
		    
		    // add parameters
		    broadcast.putExtra(IntentParameterConstants.PhoneGapCallbackId, callbackId);
		    
		    // send broadcast
	        this.ctx.sendBroadcast(broadcast);
        }
	}
	
	private void openNativeInfoDialogFromJS(JSONArray data, String callbackId) {
		openNativeInfoDialog(data, callbackId);
	}
	
	@Override
	public void openNativeInfoDialog(JSONArray parameters, String callbackId) {
		if (this.ctx != null) {
		    Intent broadcast = new Intent();
		    
		    // set open menu item action
		    broadcast.setAction(OPEN_NATIVE_INFO_DIALOG_BROADCAST_ACTION);
		    
		    // add parameters
		    broadcast.putExtra(IntentParameterConstants.JsonArrayParameters, parameters.toString());
		    
		    // send broadcast
	        this.ctx.sendBroadcast(broadcast);
        }
		
		PluginResult pluginResult = new PluginResult(Status.NO_RESULT);
		pluginResult.setKeepCallback(false);
		this.success(pluginResult, callbackId);
	}
}