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

package com.tribal.omlet.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.tribal.omlet.DownloadManagerActivity;
import com.tribal.omlet.R;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.download.DownloadBroadcastActions;
import com.tribal.mobile.download.DownloadService;

/**
 * Omlet implementation of {@link DownloadService}.
 * 
 * @author Jon Brasted
 */
public class DownloadServiceImplementation extends DownloadService {
	/* Fields */

	private NotificationManager notificationManager;

	private Map<String, Integer> activeDownloadNotificationIds;
	private Map<Integer, Notification> activeDownloadNotifications;
	private Map<Integer, DownloadNotificationStatus> activeDownloadNotificationStatuses;
	private Map<Integer, PackageItem> activeDownloads;

	private String downloadDestinationPath;

	/* Properties */

	@Override
	protected String getDownloadDestinationPath() {
		return downloadDestinationPath;
	}

	/* Methods */

	@SuppressLint("UseSparseArrays")
	@Override
	public void onCreate() {
		super.onCreate();

		// get download destination path
		String externalStoragePackagesPath = String.format(getString(R.string.external_storage_packages_path_format_string), ""); 
		downloadDestinationPath = Environment.getExternalStorageDirectory() + externalStoragePackagesPath;

		// get notification manager
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// create notification id map
		activeDownloadNotificationIds = new HashMap<String, Integer>();

		// create notification map
		activeDownloadNotifications = new HashMap<Integer, Notification>();

		// create notification status map
		activeDownloadNotificationStatuses = new HashMap<Integer, DownloadNotificationStatus>();

		// create active downloads map
		activeDownloads = new HashMap<Integer, PackageItem>();

		addBroadcastReceiver(DownloadBroadcastActions.DownloadListRequest);
	}

	@Override
	protected void onDownloadFileProgressUpdate(String fileUrl, Object payload, long progress, long max) {
		// payload will be the package item
		PackageItem packageItem = (PackageItem) payload;

		// raise progress notification
		raiseDownloadProgressNotification(fileUrl, packageItem, progress, max);
	}

	private void raiseDownloadProgressNotification(String fileUrl, PackageItem packageItem, long progress, long max) {
		Notification notification;

		// get the notification id
		Integer notificationId = 0;

		if (activeDownloadNotificationIds.containsKey(fileUrl)) {
			notificationId = activeDownloadNotificationIds.get(fileUrl);
		} else {
			// create new id
			notificationId = new Random().nextInt(Integer.MAX_VALUE);
			activeDownloadNotificationIds.put(fileUrl, notificationId);
		}

		int icon = R.drawable.ic_launcher;
		String contentTitle = getString(R.string.library_notification_full_title);
		String contentText = null;
		long when = System.currentTimeMillis();

		Intent downloadStatusIntent = new Intent(getApplication(), DownloadManagerActivity.class);

		// set download status intent action so the extras work
		String downloadStatusIntentAction = getString(R.string.download_status_intent_action);
		downloadStatusIntent.setAction(downloadStatusIntentAction);

		// calculate progress percentage
		double progressPercentage = (double) progress / (double) max;
		String progressPercentageFormattedText = NumberFormat
				.getPercentInstance().format(progressPercentage);
		progressPercentage = (progressPercentage * 100);

		PendingIntent contentIntent = PendingIntent.getActivity(getApplication(), 0, downloadStatusIntent, 0);

		if (progress == 0) {
			// started downloading

			// get content text
			contentText = String.format(getString(R.string.download_started_text), packageItem.getName());

			// create new notification
			notification = new Notification(icon, contentText, when);
			notification.contentIntent = contentIntent;

			// set event info
			notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);

			// assign the notification
			activeDownloadNotifications.put(notificationId, notification);

			// set notification status
			activeDownloadNotificationStatuses.put(notificationId, DownloadNotificationStatus.DownloadStarted);

			// set package
			activeDownloads.put(notificationId, packageItem);

			// raise notification
			notificationManager.notify(notificationId, notification);
			
			// raise notification intent
			raiseNotificationIntent(notificationId, packageItem, DownloadNotificationStatus.DownloadStarted, contentText, (int)progressPercentage, 100);
		} else if (progress < max) {
			// get the notification status
			DownloadNotificationStatus downloadNotificationStatus = activeDownloadNotificationStatuses
					.get(notificationId);

			String downloadNotificationProgressText = getString(R.string.download_notification_downloading_format_text);

			double progressMegabytes = (double) progress / 1024 / 1024;
			BigDecimal progressMegabytesBigDecimal = new BigDecimal(progressMegabytes);
			BigDecimal progressMegabytesRoundedBigDecimal = progressMegabytesBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
			progressMegabytes = progressMegabytesRoundedBigDecimal.doubleValue();

			double maxMegabytes = (double) max / 1024 / 1024;
			BigDecimal maxMegabytesBigDecimal = new BigDecimal(maxMegabytes);
			BigDecimal maxMegabytesRoundedBigDecimal = maxMegabytesBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
			maxMegabytes = maxMegabytesRoundedBigDecimal.doubleValue();

			downloadNotificationProgressText = String.format(downloadNotificationProgressText,
					progressPercentageFormattedText, progressMegabytes,
					maxMegabytes);

			if (downloadNotificationStatus == DownloadNotificationStatus.DownloadStarted) {
				// clear the notification for this id
				notificationManager.cancel(notificationId);

				// clear old notification from the map
				activeDownloadNotifications.remove(notificationId);

				// get content text
				contentText = String.format(getString(R.string.download_notification_ticker_text), packageItem.getName());

				// create new notification
				notification = new Notification(icon, contentText, when);

				// set content intent
				notification.contentIntent = contentIntent;

				// set flags
				notification.flags = notification.flags
						| Notification.FLAG_ONGOING_EVENT
						| Notification.FLAG_ONLY_ALERT_ONCE;

				// set content view
				notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_notification_template);
				notification.contentView.setImageViewResource(R.id.download_notification_status_icon, icon);
				notification.contentView.setTextViewText(R.id.download_notification_file_title, packageItem.getName());
				notification.contentView.setTextViewText(R.id.download_notification_status_text, downloadNotificationProgressText);
				notification.contentView.setProgressBar(R.id.download_notification_progress_bar, 100, (int) progressPercentage, false);

				// persist notification
				activeDownloadNotifications.put(notificationId, notification);

				// set status to in progress
				activeDownloadNotificationStatuses.put(notificationId,
						DownloadNotificationStatus.DownloadInProgress);
			} else {
				// get the notification
				notification = activeDownloadNotifications.get(notificationId);

				if (notification == null) {
					return;
				}
				
				// just update the text view and progress bar
				notification.contentView.setTextViewText(R.id.download_notification_status_text, downloadNotificationProgressText);
				notification.contentView.setProgressBar(R.id.download_notification_progress_bar, 100, (int) progressPercentage, false);
			}

			// raise notification
			notificationManager.notify(notificationId, notification);
			
			// raise notification intent
			raiseNotificationIntent(notificationId, packageItem, downloadNotificationStatus, downloadNotificationProgressText, (int)progressPercentage, 100);
		} else {
			// download complete

			// clear the notification for this id
			notificationManager.cancel(notificationId);

			// clear old notification id from the map
			activeDownloadNotificationIds.remove(notificationId);

			// clear old notification from the map
			activeDownloadNotifications.remove(notificationId);

			// clear old notification status from the map
			activeDownloadNotificationStatuses.remove(notificationId);

			// remove the active download
			activeDownloads.remove(notificationId);
			
			// raise notification intent
			raiseNotificationIntent(notificationId, packageItem, DownloadNotificationStatus.DownloadCompleted, String.format(getString(R.string.download_completed_text), packageItem.getName()), (int)progressPercentage, 100);
		}
	}

	private void raiseNotificationIntent(Integer notificationId, PackageItem packageItem, DownloadNotificationStatus notificationStatus, String statusText, int progress, int max) {
		DownloadNotificationStatusResult result = new DownloadNotificationStatusResult(notificationId, packageItem, notificationStatus, statusText, progress, max);
		
		// send the result back
		
		Intent intent = new Intent();
		intent.setAction(DownloadBroadcastActions.DownloadStatusNotification);
		intent.putExtra(IntentParameterConstants.DownloadNotificationStatusResult, result);
		sendBroadcast(intent);
	}
	
	@Override
	protected void onDownloadFileCompleted(String fileUrl, File file) {
		Log.d("Omlet", fileUrl + " has finished downloading. " + file);
	}

	@Override
	protected void onProcessFileStarted(File file, PackageItem packageItem) {
		// raise notification
		
		Notification notification;

		int icon = R.drawable.ic_launcher;
		String contentText = null;
		long when = System.currentTimeMillis();

		Intent processingStatusIntent = new Intent(getApplication(), DownloadManagerActivity.class);

		// set processing status intent action so the extras work
		String processingStatusIntentAction = getString(R.string.processing_status_intent_action);
		processingStatusIntent.setAction(processingStatusIntentAction);

		PendingIntent contentIntent = PendingIntent.getActivity(getApplication(), 0, processingStatusIntent, 0);

		// clear the notification
		notificationManager.cancel(R.layout.processing_notification_template);

		// get content text
		contentText = String.format(getString(R.string.processing_notification_ticker_text), packageItem.getName());

		// create new notification
		notification = new Notification(icon, contentText, when);

		// set content intent
		notification.contentIntent = contentIntent;

		// set flags
		notification.flags = notification.flags
				| Notification.FLAG_ONGOING_EVENT
				| Notification.FLAG_ONLY_ALERT_ONCE;

		// set content view
		notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.processing_notification_template);
		notification.contentView.setImageViewResource(R.id.processing_notification_status_icon, icon);
		notification.contentView.setTextViewText(R.id.processing_notification_file_title, packageItem.getName());
		notification.contentView.setProgressBar(R.id.processing_notification_progress_bar, 100, 0, true);

		// raise notification
		notificationManager.notify(R.layout.processing_notification_template, notification);

		// add the notification status
		activeDownloadNotificationStatuses.put(R.layout.processing_notification_template, DownloadNotificationStatus.ProcessingInProgress);

		// add the active download
		activeDownloads.put(R.layout.processing_notification_template, packageItem);
		
		// raise notification intent
		raiseNotificationIntent(R.layout.processing_notification_template, packageItem, DownloadNotificationStatus.ProcessingInProgress, getString(R.string.processing_notification_processing_text), 0, 0);
	}

	@Override
	protected void onProcessFileCompleted(File file) {
		// clear the processing file notification
		notificationManager.cancel(R.layout.processing_notification_template);

		// get the package item
		PackageItem packageItem = activeDownloads.get(R.layout.processing_notification_template);
		
		// remove the active download
		activeDownloads.remove(R.layout.processing_notification_template);

		// remove the notification status
		activeDownloadNotificationStatuses.remove(R.layout.processing_notification_template);
		
		// raise notification intent
		raiseNotificationIntent(R.layout.processing_notification_template, packageItem, DownloadNotificationStatus.ProcessingCompleted, getString(R.string.processing_notification_processing_complete_text), 0, 0);
	}

	@Override
	protected void onBroadcastReceiveOverride(Intent intent) {
		String intentAction = intent.getAction();

		if (DownloadBroadcastActions.DownloadListRequest.equalsIgnoreCase(intentAction)) {
			onDownloadListRequest();
		}
	}

	@Override
	protected void onFileDownloadCancelledOverride(PackageItem packageItem) {
		// raise file download cancelled notification
		raiseDownloadCancelledNotification(packageItem);
	}
	
	private void raiseDownloadCancelledNotification(PackageItem packageItem) {
		// get the existing notification ID
		String packageItemId = packageItem.getFileUrl();

		if (activeDownloadNotificationIds.containsKey(packageItemId)) {
			// remove the notification id
			int notificationId = activeDownloadNotificationIds.remove(packageItemId);
			
			// remove the package item
			if (activeDownloads.containsKey(notificationId)) {
				activeDownloads.remove(notificationId);
			}
			
			// remove the notification
			if (activeDownloadNotifications.containsKey(notificationId)) {
				activeDownloadNotifications.remove(notificationId);				
			}
			
			// remove the notification status
			if (activeDownloadNotificationStatuses.containsKey(notificationId)) {
				activeDownloadNotificationStatuses.remove(notificationId);
			}
			
			// cancel the current notification
			notificationManager.cancel(notificationId);
			
			int icon = R.drawable.ic_launcher;
			String contentTitle = getString(R.string.download_cancelled_notification_full_title);
			String contentText = null;
			long when = System.currentTimeMillis();
			
			Intent downloadStatusIntent = new Intent(getApplication(), DownloadManagerActivity.class);
			
			// create notification
			PendingIntent contentIntent = PendingIntent.getActivity(getApplication(), 0, downloadStatusIntent, 0);

			// get content text
			contentText = String.format(getString(R.string.download_notification_cancelled_format_text), packageItem.getName());

			// create new notification
			Notification notification = new Notification(icon, contentText, when);
			notification.contentIntent = contentIntent;

			// set event info
			notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);

			// raise notification
			notificationManager.notify(notificationId, notification);
		}
	}
	
	@Override
	protected void onDownloadFileFailed(String fileUrl, Object payload, Exception exception) {
		if (payload instanceof PackageItem) {
			PackageItem packageItem = (PackageItem)payload;
			
			// raise file download failed notification
			raiseDownloadFailedNotification(packageItem, exception);
		}
	}
	
	private void raiseDownloadFailedNotification(PackageItem packageItem, Exception exception) {
		// get the existing notification ID
		String packageItemId = packageItem.getFileUrl();

		if (activeDownloadNotificationIds.containsKey(packageItemId)) {
			// remove the notification id
			int notificationId = activeDownloadNotificationIds.remove(packageItemId);
			
			// remove the package item
			if (activeDownloads.containsKey(notificationId)) {
				activeDownloads.remove(notificationId);
			}
			
			// remove the notification
			if (activeDownloadNotifications.containsKey(notificationId)) {
				activeDownloadNotifications.remove(notificationId);				
			}
			
			// remove the notification status
			if (activeDownloadNotificationStatuses.containsKey(notificationId)) {
				activeDownloadNotificationStatuses.remove(notificationId);
			}
			
			// cancel the current notification
			notificationManager.cancel(notificationId);
		}
		
		// show failed toast
		String message = getFriendlyMessageFromException(packageItem, exception);
		
		Intent showToast = new Intent();
		showToast.setAction(BroadcastActions.ShowToast);
		showToast.putExtra(IntentParameterConstants.Message, message);
		showToast.putExtra(IntentParameterConstants.Duration, Toast.LENGTH_SHORT);
		sendBroadcast(showToast);
	}
	
	private String getFriendlyMessageFromException(PackageItem packageItem, Exception exception) {
		if (exception instanceof FileNotFoundException) {
			String downloadFailedFileNotFoundFormatMessage = getString(R.string.download_failed_fnf_message);
			return String.format(downloadFailedFileNotFoundFormatMessage, packageItem.getName());
		} else {
			String downloadFailedFormatMessage = getString(R.string.download_failed_message);
			return String.format(downloadFailedFormatMessage, packageItem.getName());
		}
	}

	private void onDownloadListRequest() {
		// create list of download notification status objects
		DownloadNotificationStatusResultList list = new DownloadNotificationStatusResultList();

		int notificationId;
		PackageItem packageItem;
		DownloadNotificationStatus notificationStatus;

		for (Entry<Integer, PackageItem> activeDownload : activeDownloads.entrySet()) {
			// get notification id
			notificationId = activeDownload.getKey();
			
			// get package item
			packageItem = activeDownload.getValue();
			
			// get status from notification id
			notificationStatus = activeDownloadNotificationStatuses.get(notificationId);
			
			// add the item to the list
			list.add(new DownloadNotificationStatusResult(notificationId, packageItem, notificationStatus));
		}
		
		// send the result back		
		Intent intent = new Intent();
		intent.setAction(DownloadBroadcastActions.DownloadListResponse);
		intent.putExtra(IntentParameterConstants.DownloadList, list);
		sendBroadcast(intent);
	}
}