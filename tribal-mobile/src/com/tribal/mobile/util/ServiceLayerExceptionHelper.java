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

import android.content.Context;
import android.content.Intent;

import com.tribal.mobile.R;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;

/**
 * Utility class to facilitate the handling of web service request errors, in particular, SSL certificate errors.
 * 
 * @author Jon Brasted
 */
public class ServiceLayerExceptionHelper {
	/* Fields */
	
	private static ServiceLayerExceptionHelper instance;
	
	private boolean hasShownSSLCertificateError = false;
	
	/* Properties */
	
	public static ServiceLayerExceptionHelper getInstance() {
		if (instance == null) {
			instance = new ServiceLayerExceptionHelper();
		}
		
		return instance;
	}
	
	public boolean getHasShownSSLCertificateError() {
		return hasShownSSLCertificateError;
	}
	
	public void setHasShownSSLCertificateError(boolean value) {
		hasShownSSLCertificateError = value;
	}
	
	/* Methods */
	
	/**
	 * Process exception. Currently only handles SSL certificate errors. All other errors are logged to Logcat.
	 * 
	 * @param exception		the exception
	 * @param context		the context
	 */
	public void processException(Throwable exception, Context context) {
		String message = exception.toString();
		
		if (context != null && 
			(exception instanceof javax.net.ssl.SSLPeerUnverifiedException || 
			(message.contains("javax.security.cert"))) &&
			!hasShownSSLCertificateError) {
			setHasShownSSLCertificateError(true);
			ConnectivityUtils.setHasConnectedToSelfSignedCertificatesWhilstNotPermitted(true);
			
			// get the text required for SSL certificate error
			String dialogTitle = context.getString(R.string.sslSelfSignedCertificateErrorTitle);
			
			// get the text required for SSL certificate error
			String dialogMessage = context.getString(R.string.sslSelfSignedCertificateErrorMessage);
			
			// fire a broadcast to inform the system to recheck the connectivity
			Intent connectivityIntent = new Intent();
			connectivityIntent.setAction(BroadcastActions.RefreshConnectivityChanged);
			context.sendBroadcast(connectivityIntent);
			//IntentUtils.getInstance().addDeferredBroadcast(connectivityIntent);
			
			// fire a broadcast to show a dialog
			Intent showDialogIntent = new Intent();
			showDialogIntent.setAction(BroadcastActions.ShowDialog);
			showDialogIntent.putExtra(IntentParameterConstants.Title, dialogTitle);
			showDialogIntent.putExtra(IntentParameterConstants.Message, dialogMessage);
			context.sendBroadcast(showDialogIntent);
			
		} else {
			exception.printStackTrace();
		}
	}
}