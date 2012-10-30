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

package com.tribal.omlet.sync;

import java.text.NumberFormat;

import com.tribal.omlet.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.sync.BaseSyncService;

/**
 * Omlet implementation of {@link BaseSyncService}.
 * 
 * @author Jon Brasted
 */
public class SyncService extends BaseSyncService {
	/* Fields */
	
	private Notification synchronisingInProgressNotification;
	
	/* Methods */
	
	@Override
	protected void raiseSyncProgressNotification(int progress, int max, boolean isAutomaticSync) {
		int icon = R.drawable.ic_launcher_small;
		String contentTitle = getString(R.string.sync_full_title_string);
		String contentText = null;
		long when = System.currentTimeMillis();

		Intent syncStatusIntent = new Intent();

		// set sync status intent action so the extras work
		String syncStatusIntentAction = getString(R.string.sync_status_intent_action);
		syncStatusIntent.setAction(syncStatusIntentAction);

		// calculate progress percentage
		double progressPercentage = (double) progress / (double) max;
		String progressPercentageFormattedText = NumberFormat
				.getPercentInstance().format(progressPercentage);

		String progressIntentKey = getString(R.string.sync_progress_intent_key);
		String progressPercentageIntentKey = getString(R.string.sync_progress_percentage_intent_key);
		String maxIntentKey = getString(R.string.sync_max_intent_key);

		// put progress update information in the sync status content intent
		// as well
		syncStatusIntent.putExtra(progressIntentKey, progress);
		syncStatusIntent.putExtra(progressPercentageIntentKey,
				progressPercentage);
		syncStatusIntent.putExtra(maxIntentKey, max);

		PendingIntent contentIntent = PendingIntent.getActivity(
				baseApplication, 0, syncStatusIntent, 0);

		if (progress == 0) {
			// started synchronisation

			// clear all active notifications
			notificationManager.cancel(R.layout.notification_template);

			// get content text
			contentText = getString(R.string.sync_started_string);

			// create new notification
			Notification notification = new Notification(icon, contentText,
					when);
			notification.contentIntent = contentIntent;
			notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_AUTO_CANCEL;

			// set event info
			notification.setLatestEventInfo(getApplicationContext(),
					contentTitle, contentText, contentIntent);

			// raise notification
			notificationManager.notify(R.layout.notification_template,
					notification);
		} else if (progress < max) {
			// synchronising

			// use sync in progress notification
			if (synchronisingInProgressNotification == null) {
				// get content text
				contentText = getString(R.string.sync_synchronising_string);

				// create new notification
				synchronisingInProgressNotification = new Notification(icon,
						contentText, when);

				// set content intent
				synchronisingInProgressNotification.contentIntent = contentIntent;

				// set flags
				synchronisingInProgressNotification.flags = synchronisingInProgressNotification.flags
						| Notification.FLAG_ONGOING_EVENT
						| Notification.FLAG_ONLY_ALERT_ONCE;

				// set content view
				synchronisingInProgressNotification.contentView = new RemoteViews(
						getApplicationContext().getPackageName(),
						R.layout.notification_template);
				synchronisingInProgressNotification.contentView
						.setImageViewResource(R.id.notification_status_icon,
								icon);
				synchronisingInProgressNotification.contentView
						.setTextViewText(R.id.notification_status_text,
								contentText);
				synchronisingInProgressNotification.contentView
						.setProgressBar(R.id.notification_status_progress, max,
								progress, false);
			} else {
				// just update the text view and progress bar
				synchronisingInProgressNotification.contentView
						.setTextViewText(R.id.notification_progress_text,
								progressPercentageFormattedText);
				synchronisingInProgressNotification.contentView
						.setProgressBar(R.id.notification_status_progress, max,
								progress, false);
			}

			// raise notification
			notificationManager.notify(R.layout.notification_template,
					synchronisingInProgressNotification);
		} else {
			// synchronisation complete

			// clear all active notifications
			notificationManager.cancel(R.layout.notification_template);

			// clear the sync in progress notification
			synchronisingInProgressNotification = null;

			// get content text
			contentText = getString(R.string.sync_complete_string);

			// create new notification
			Notification notification = new Notification(icon, contentText,
					when);
			notification.contentIntent = contentIntent;
			notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_AUTO_CANCEL;

			// set event info
			notification.setLatestEventInfo(getApplicationContext(),
					contentTitle, contentText, contentIntent);

			// raise notification
			notificationManager.notify(R.layout.notification_template,
					notification);
		}

		// send progress update intent in case anyone is listening
		Intent syncProgressUpdateIntent = new Intent();
		syncProgressUpdateIntent.setAction(BroadcastActions.SyncProgressUpdate);

		syncProgressUpdateIntent.putExtra(progressIntentKey, progress);
		syncProgressUpdateIntent.putExtra(progressPercentageIntentKey, progressPercentage);
		syncProgressUpdateIntent.putExtra(maxIntentKey, max);
		syncProgressUpdateIntent.putExtra(IntentParameterConstants.ShowToast, !isAutomaticSync);
		sendBroadcast(syncProgressUpdateIntent);

		if (progress == max) {
			// send completed broadcast
			Intent syncCompletedIntent = new Intent();
			syncCompletedIntent.setAction(BroadcastActions.SyncCompleted);
			syncCompletedIntent.putExtra(IntentParameterConstants.ShowToast, !isAutomaticSync);
			sendBroadcast(syncCompletedIntent);
		}
	}
	
	@Override
	protected void raiseSyncCancelledNotification(boolean isAutomaticSync) {
		notificationManager.cancel(R.layout.notification_template);

		int icon = R.drawable.ic_launcher_small;
		long when = System.currentTimeMillis();

		String contentTitle = getString(R.string.sync_full_title_string);
		String contentText = getString(R.string.sync_cancelled_string);

		Intent syncStatusIntent = new Intent();
		String syncCancelledIntentKey = getString(R.string.sync_cancelled_intent_key);
		syncStatusIntent.putExtra(syncCancelledIntentKey, true);
		//PendingIntent contentIntent = PendingIntent.getActivity(baseApplication, 0, syncStatusIntent, 0);

		Notification notification = new Notification(icon, contentText, when);
		notification.setLatestEventInfo(getApplicationContext(), contentTitle,
				contentText, null);
		notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_AUTO_CANCEL;

		// raise notification
		notificationManager.notify(R.layout.notification_template, notification);

		// user cancelled
		Intent syncCancelledIntent = new Intent();
		syncCancelledIntent.setAction(BroadcastActions.SyncCancelled);
		syncCancelledIntent.putExtra(IntentParameterConstants.ShowToast, !isAutomaticSync);
		sendBroadcast(syncCancelledIntent);
	}
	
	@Override
	protected void raiseSyncFailedNotification(boolean isAutomaticSync, Exception exception) {
		notificationManager.cancel(R.layout.notification_template);

		int icon = R.drawable.ic_launcher_small;
		long when = System.currentTimeMillis();

		String contentTitle = getString(R.string.sync_full_title_string);
		
		String friendlyErrorMessage = getFriendlyErrorMessage(exception);
		String contentText = friendlyErrorMessage;

		Intent syncStatusIntent = new Intent();
		String syncCancelledIntentKey = getString(R.string.sync_cancelled_intent_key);
		syncStatusIntent.putExtra(syncCancelledIntentKey, true);

		Notification notification = new Notification(icon, contentText, when);
		notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, null);
		notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;

		// raise notification
		notificationManager.notify(R.layout.notification_template, notification);

		// user cancelled
		Intent syncCancelledIntent = new Intent();
		syncCancelledIntent.setAction(BroadcastActions.SyncCancelled);
		syncCancelledIntent.putExtra(IntentParameterConstants.ShowToast, !isAutomaticSync);
		syncCancelledIntent.putExtra(IntentParameterConstants.Message, friendlyErrorMessage);
		syncCancelledIntent.putExtra(IntentParameterConstants.Duration, Toast.LENGTH_SHORT);
		sendBroadcast(syncCancelledIntent);
	}

	private String getFriendlyErrorMessage(Exception exception) {
		String syncFailedFormatMessage = getString(R.string.sync_failed_message);
		return String.format(syncFailedFormatMessage, exception.getMessage());
	}
}