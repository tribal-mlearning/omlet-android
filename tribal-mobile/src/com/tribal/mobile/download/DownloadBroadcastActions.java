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

package com.tribal.mobile.download;

import com.tribal.mobile.util.BroadcastUtils;

/**
 * Class provides intent actions used in broadcast messages within the mobile framework.
 * In order to prevent cross-communication when two of more applications build with the mobile framework are running on a device at the same time,
 * the actions are updated with the application package name at runtime via reflection.
 * 
 * @see BroadcastUtils#initialiseBroadcastActions(android.content.Context) 
 * @author Jon Brasted, Jack Kierney and Jack Harrison
 */
public class DownloadBroadcastActions {

	/* 
	 * These constants should not have the operator
	 * or the compiler will inline them at compile time.
	 * They need to be updated at runtime (in BroadcastUtils.java)
	 * via reflection
	 */
	
	/* Constants */
	public static String AddPackageToLibrary = "%s.download.broadcastactions.addPackageToLibrary";
	public static String PackageDownloadQueued = "%s.download.broadcastactions.packageDownloadQueued";
	public static String PackageDownloading = "%s.download.broadcastactions.packageDownloading";
	public static String PackageDownloaded = "%s.download.broadcastactions.packageDownloaded";
	public static String PackageDownloadCancelled = "%s.download.broadcastactions.packageDownloadCancelled";
	public static String PackageDownloadCancelRequest = "%s.download.broadcastactions.packageDownloadCancelRequest";
	public static String PackageDownloadCancelAllRequest = "%s.download.broadcastactions.packageDownloadCancelAllRequest";
	public static String PackageDownloadFailed = "%s.download.broadcastactions.packageDownloadFailed";
	public static String PackageProcessingQueued = "%s.download.broadcastactions.packageProcessingQueued";
	public static String PackageProcessing = "%s.download.broadcastactions.packageProcessing";
	public static String PackageProcessingCompleted = "%s.download.broadcastactions.packageProcessingCompleted";
	public static String PackageReset = "%s.download.broadcastactions.packageReset";
	public static String PackageUpdate = "%s.download.broadcastactions.packageUpdate";
	public static String DownloadListRequest = "%s.download.broadcastactions.downloadListRequest";
	public static String DownloadListResponse = "%s.download.broadcastactions.downloadListResponse";
	public static String DownloadStatusNotification = "%s.download.broadcastactions.downloadStatusNotification";
}