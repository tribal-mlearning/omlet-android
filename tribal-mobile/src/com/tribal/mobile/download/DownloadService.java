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

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.base.ServiceBase;
import com.tribal.mobile.util.AsyncTaskHelper;

/**
 * Class provides the logic required to queue up and download a file.
 * 
 * @author Jon Brasted
 */
public abstract class DownloadService extends ServiceBase implements DownloadFileAsyncProgressUpdate, DownloadFileAsyncCompleted, DownloadFileAsyncCancelled, DownloadFileAsyncFailed, UnzipFileAsyncCompleted {
	/* Fields */

	private final static int MAX_CONCURRENT_DOWNLOADS = 3;

	protected Queue<PackageItem> downloadQueue;

	private Map<String, PackageItem> currentDownloads;
	private Map<String, DownloadFileAsync> currentDownloadTasks;

	protected Queue<File> unzipQueue;
	protected Map<File, PackageItem> unzipFileMap;

	private int concurrentDownloads = 0;
	private File currentFileToUnzip = null;

	/* Properties */

	/**
	 * Returns the download destination path. Intended to be overriden.
	 * 
	 * @return the download destination path
	 */
	protected String getDownloadDestinationPath() {
		return null;
	}

	/**
	 * Returns a {@link java.util.Map} of the current downloads in progress.
	 * 
	 * @return a {@link java.util.Map} of the current downloads in progress
	 */
	protected Map<String, PackageItem> getCurrentDownloads() {
		return currentDownloads;
	}

	/**
	 * Returns a {@link java.util.Map} of the current download tasks.
	 * 
	 * @return a {@link java.util.Map} of the current download tasks
	 */
	protected Map<String, DownloadFileAsync> getCurrentDownloadTasks() {
		return currentDownloadTasks;
	}

	/* Methods */

	@Override
	public IBinder onBind(Intent arg) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// create downloadQueue
		downloadQueue = new LinkedList<PackageItem>();

		// create currentDownloads
		currentDownloads = new HashMap<String, PackageItem>();

		// create currentDownloadTasks
		currentDownloadTasks = new HashMap<String, DownloadFileAsync>();

		// create unzipQueue
		unzipQueue = new LinkedList<File>();

		// create unzipFileMap
		unzipFileMap = new HashMap<File, PackageItem>();

		// add broadcast receivers
		addBroadcastReceiver(DownloadBroadcastActions.AddPackageToLibrary);
		addBroadcastReceiver(DownloadBroadcastActions.PackageDownloadCancelRequest);
		addBroadcastReceiver(DownloadBroadcastActions.PackageDownloadCancelAllRequest);
	}

	/**
	 * Provides functionality to handle common intent actions and is invoked when a broadcast receiver receives 
	 * Method will invoke {@link #onBroadcastReceiveOverride(Intent) onBroadcastReceiveOverride} method if <code>intent.getAction()</code> does not return
	 * one of the following:
	 * <p>
	 * 	<code>AddPackageToLibrary</code><br />
	 * 	<code>PackageDownloadCancelRequest</code><br />
	 *  <code>PackageDownloadCancelAllRequest</code>
	 * </p>
	 */
	@Override
	protected void onBroadcastReceive(Intent intent) {
		String action = intent.getAction();

		if (DownloadBroadcastActions.AddPackageToLibrary.equalsIgnoreCase(action)) {
			addPackageToLibrary(intent);
		} else if (DownloadBroadcastActions.PackageDownloadCancelRequest.equalsIgnoreCase(action)) {
			cancelPackageDownload(intent);
		} else if (DownloadBroadcastActions.PackageDownloadCancelAllRequest.equalsIgnoreCase(action)) {
			cancelAllDownloads();
		} else {
			onBroadcastReceiveOverride(intent);
		}
	}

	/**
	 * Provides an override method for handling intent actions. Will be invoked by {@link #onBroadcastReceive(Intent) onBroadcastReceive} if <code>intent.getAction()</code> does not return
	 * a handled intent action.
	 */
	protected void onBroadcastReceiveOverride(Intent intent) {
	}

	private void addPackageToLibrary(Intent intent) {
		// get package item
		if (intent.hasExtra(IntentParameterConstants.PackageItem)) {
			Serializable serializable = intent.getSerializableExtra(IntentParameterConstants.PackageItem);

			if (serializable instanceof PackageItem) {
				PackageItem packageItem = (PackageItem) serializable;

				// queue up the package item for download
				queuePackageItemForDownload(packageItem);
			}
		}
	}

	private void queuePackageItemForDownload(PackageItem packageItem) {
		if (!downloadQueue.contains(packageItem)) {
			downloadQueue.add(packageItem);

			// send downloading queued broadcast
			sendBroadcast(DownloadBroadcastActions.PackageDownloadQueued,
					packageItem);

			if (concurrentDownloads < MAX_CONCURRENT_DOWNLOADS) {
				// start new download
				startNextDownload();
			}
		}
	}

	private void startNextDownload() {
		DownloadFileAsync downloadFileAsyncTask = null;

		if (concurrentDownloads < MAX_CONCURRENT_DOWNLOADS) {
			downloadFileAsyncTask = new DownloadFileAsync();

			concurrentDownloads++;
		} else {
			Log.d("Download service", "Too many concurrent downloads.");
			return;
		}

		// get the top item off the queue
		PackageItem packageItem = downloadQueue.poll();

		// add item to currentDownloads
		currentDownloads.put(packageItem.getFileUrl(), packageItem);

		// add item to current download tasks
		currentDownloadTasks.put(packageItem.getFileUrl(),
				downloadFileAsyncTask);

		// send downloading broadcast
		sendBroadcast(DownloadBroadcastActions.PackageDownloading, packageItem);

		// execute task
		AsyncTaskHelper.executeAsyncTask(downloadFileAsyncTask,
				getDownloadDestinationPath(), packageItem.getFileUrl(),
				packageItem, this, this, this, this, getApplicationContext(),
				getApplication());
	}

	private void cancelAllDownloads() {
		// First clear the download queue
		downloadQueue.clear();

		// Cancel all the courses that are being downloaded
		for (PackageItem packageItem : currentDownloads.values()) {
			cancelDownload(packageItem);
		}

		currentDownloads.clear();
	}

	private void cancelPackageDownload(Intent intent) {
		if (intent.hasExtra(IntentParameterConstants.PackageItem)) {
			PackageItem packageItem = (PackageItem) intent
					.getSerializableExtra(IntentParameterConstants.PackageItem);

			cancelDownload(packageItem);

			currentDownloads.remove(packageItem.getFileUrl());
		}
	}

	private void cancelDownload(PackageItem packageItem) {
		if (packageItem != null) {
			String packageFileUrl = packageItem.getFileUrl();

			if (currentDownloads.containsKey(packageFileUrl)) {
				packageItem = currentDownloads.get(packageFileUrl);

				if (currentDownloadTasks.containsKey(packageFileUrl)) {
					DownloadFileAsync task = currentDownloadTasks
							.remove(packageFileUrl);

					// set isCancelled
					task.cancel(true);

					// raise cancelled notification
					onFileDownloadCancelled(packageItem);

					// start next task
					concurrentDownloads--;

					// check to see if there is anymore items to download
					if (!downloadQueue.isEmpty()) {
						startNextDownload();
					}
				}

			} else if (downloadQueue.contains(packageItem)) {
				downloadQueue.remove(packageItem);
			}
		}
	}

	@Override
	public final void onDownloadFileAsyncProgressUpdate(String fileUrl,
			Object payload, long progress, long max) {

		onDownloadFileProgressUpdate(fileUrl, payload, progress, max);
	}

	/**
	 * Provides an override method for handling file download progress updates.
	 * 
	 * @param fileUrl	the file url
	 * @param payload	the payload
	 * @param progress	the progress value
	 * @param max		the progress max
	 */
	protected void onDownloadFileProgressUpdate(String fileUrl, Object payload,
			long progress, long max) {
	}

	@Override
	public final void onDownloadFileAsyncCompleted(String fileUrl,
			Object payload, File file) {

		// send download completed broadcast
		sendBroadcast(DownloadBroadcastActions.PackageDownloaded,
				(PackageItem) payload);

		// remove the download
		currentDownloadTasks.remove(fileUrl);

		// invoke the on download file completed method
		onDownloadFileCompleted(fileUrl, file);

		// decrement current downloads
		concurrentDownloads--;

		// check to see if there is anymore items to download
		if (!downloadQueue.isEmpty()) {
			startNextDownload();
		}

		// do the unzip
		queueFileForUnzip(file, (PackageItem) payload);
	}

	/**
	 * Provides an override method for handling file download completions.
	 * 
	 * @param fileUrl	the file url
	 * @param file		the file
	 */
	protected void onDownloadFileCompleted(String fileUrl, File file) {
	}

	@Override
	public final void onDownloadFileAsyncCancelled(String fileUrl,
			Object payload) {
		// create intent to broadcast package download cancel request
		Intent cancellationIntent = new Intent();
		cancellationIntent
				.setAction(DownloadBroadcastActions.PackageDownloadCancelRequest);
		cancellationIntent.putExtra(IntentParameterConstants.PackageItem,
				(PackageItem) payload);
		sendBroadcast(cancellationIntent);

		// reuse intent to broadcast package download cancelled
		cancellationIntent
				.setAction(DownloadBroadcastActions.PackageDownloadCancelled);
		sendBroadcast(cancellationIntent);
	}

	/**
	 * Provides an override method for handling file download failures.
	 * 
	 * @param fileUrl	the file url
	 * @param payload	the payload
	 * @param exception	the exception raised when the file download failed
	 */
	protected void onDownloadFileFailed(String fileUrl, Object payload,
			Exception exception) {
	}

	@Override
	public final void onDownloadFileAsyncFailed(String fileUrl, Object payload,
			Exception exception) {
		// create intent to broadcast package download failed
		Intent failedIntent = new Intent();
		failedIntent.setAction(DownloadBroadcastActions.PackageDownloadFailed);
		failedIntent.putExtra(IntentParameterConstants.PackageItem,
				(PackageItem) payload);
		sendBroadcast(failedIntent);

		onDownloadFileFailed(fileUrl, payload, exception);

		// decrement current downloads
		concurrentDownloads--;

		// check to see if there is anymore items to download
		if (!downloadQueue.isEmpty()) {
			startNextDownload();
		}
	}

	private void queueFileForUnzip(File file, PackageItem packageItem) {
		if (!unzipQueue.contains(file)) {
			unzipQueue.add(file);
			unzipFileMap.put(file, packageItem);

			// send queued broadcast
			sendBroadcast(DownloadBroadcastActions.PackageProcessingQueued,
					packageItem);

			if (currentFileToUnzip == null) {
				// start new unzip
				startNextUnzip();
			}
		}
	}

	private void startNextUnzip() {
		// get the top file off the queue
		File file = unzipQueue.poll();

		// set currentFileToUnzip
		currentFileToUnzip = file;

		// get package item for file
		PackageItem packageItem = unzipFileMap.get(file);

		// send processing broadcast
		sendBroadcast(DownloadBroadcastActions.PackageProcessing, packageItem);

		// call onProcessFileStarted
		onProcessFileStarted(file, packageItem);

		// execute task
		UnzipFileAsync unzipFileAsyncTask = new UnzipFileAsync();
		AsyncTaskHelper.executeAsyncTask(unzipFileAsyncTask, file,
				getApplication(), this, packageItem);
	}

	/**
	 * Provides an override method for handling when a file begins to be processed. 
	 * 
	 * @param file			the file
	 * @param packageItem	the package item
	 */
	protected void onProcessFileStarted(File file, PackageItem packageItem) {
	}

	@Override
	public void onUnzipFileAsyncCompleted(File file, Boolean successful,
			String errorMessage) {
		currentFileToUnzip = null;

		// do the other processing stuff

		// get package item for the file
		PackageItem packageItem = unzipFileMap.get(file);

		// send processing completed broadcast
		sendBroadcast(DownloadBroadcastActions.PackageProcessingCompleted,
				packageItem);

		// pretend we have done that
		onProcessFileCompleted(file);

		// check to see if there is anymore files to unzip
		if (!unzipQueue.isEmpty()) {
			startNextUnzip();
		}
	}

	/**
	 * Provides an override method for handling when a file finishes being processed.
	 * 
	 * @param file	the file
	 */
	protected void onProcessFileCompleted(File file) {
	}
	
	private void onFileDownloadCancelled(PackageItem packageItem) {
		// send download cancelled broadcast
		sendBroadcast(DownloadBroadcastActions.PackageDownloadCancelled, packageItem);

		// invoke override
		onFileDownloadCancelledOverride(packageItem);
	}

	/**
	 * Provides an override method for handling when a file download is cancelled.
	 * 
	 * @param packageItem	the package item
	 */
	protected void onFileDownloadCancelledOverride(PackageItem packageItem) {
	}

	private void sendBroadcast(String intentAction, PackageItem packageItem) {
		Intent intent = new Intent();
		intent.setAction(intentAction);

		intent.putExtra(IntentParameterConstants.PackageItem, packageItem);

		sendBroadcast(intent);
	}
}