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

import com.tribal.mobile.util.BroadcastUtils;

/**
 * Class provides intent actions used in broadcast messages within the mobile framework.
 * In order to prevent cross-communication when two of more applications build with the mobile framework are running on a device at the same time,
 * the actions are updated with the application package name at runtime via reflection.
 * 
 * @see BroadcastUtils#initialiseBroadcastActions(android.content.Context) 
 * @author Jon Brasted, Jack Kierney and Jack Harrison
 */
public class BroadcastActions {
	/* 
	 * These constants should not have the final operator
	 * or the compiler will inline them at compile time.
	 * They need to be updated at runtime (in BroadcastUtils.java)
	 * via reflection
	 */
	
	/* Constants */
	public static String PreLogout = "%s.broadcastactions.prelogout";
	public static String Logout = "%s.broadcastactions.logout";
	public static String ManualSyncRequested = "%s.broadcastactions.manualSyncRequested";
	
	public static String OpenMenuItem = "%s.broadcastactions.openMenuItem";
	public static String OpenResource = "%s.broadcastactions.openResource";
	
	public static String SyncStarted = "%s.broadcastactions.syncStarted";
	public static String SyncProgressUpdate = "%s.broadcastactions.syncProgressUpdate";
	public static String SyncCancelled = "%s.broadcastactions.syncCancelled";
	public static String SyncCompleted = "%s.broadcastactions.syncCompleted";
	public static String SyncFailed = "%s.broadcastactions.syncFailed";
	public static String SyncTrackingError = "%s.broadcastactions.syncTrackingError";
	
	public static String OpenHtmlDialog = "%s.broadcastactions.openHtmlDialog";
	
	public static String ShowLoadingDialog = "%s.broadcastactions.showLoadingDialog";
	public static String DismissLoadingDialog = "%s.broadcastactions.dismissLoadingDialog";
	
	public static String CatalogueTabChanged = "%s.download.broadcastactions.catalogueTabChanged";

	public static String PackageItemSelected = "%s.broadcastactions.packageItemSelected";

	public static String ShowDialog = "%s.broadcastactions.showDialog";
	public static String RefreshConnectivityChanged = "%s.broadcastactions.refreshConnectivityChanged";
	
	public static String SharedPreferenceChanged = "%s.broadcastactions.sharedPreferenceChanged";
	
	public static String ApplicationIsRunningChanged = "%s.broadcastactions.applicationIsRunningChanged";
	
	public static String ShowToast = "%s.broadcastactions.showToast";
}