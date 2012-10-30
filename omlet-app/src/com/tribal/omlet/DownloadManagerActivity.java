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

package com.tribal.omlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.tribal.omlet.download.DownloadNotificationStatus;
import com.tribal.omlet.download.DownloadNotificationStatusResult;
import com.tribal.omlet.download.DownloadNotificationStatusResultList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.download.DownloadBroadcastActions;

/**
 * Download Manager activity.
 * 
 * @author Jon Brasted
 */
public class DownloadManagerActivity extends SystemMenuProviderActivity {
	/* Fields */

	private DownloadNotificationStatusResultList list;
	private Map<String, View> notifications;

	private DownloadManagerListAdapter listAdapter;

	private ListView listView;

	/* Methods */

	@Override
	public void onCreate(Bundle savedInstanceState) {

		notifications = new HashMap<String, View>();

		setContentView(R.layout.download_manager);

		listView = (ListView) findViewById(R.id.download_manager_downloads_list);
		listView.setOnItemClickListener(new DownloadManagerListItemCancelClickListener());

		// Do super at the end as it overrides the fonts
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.DownloadListResponse)) {
			addBroadcastReceiver(DownloadBroadcastActions.DownloadListResponse);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.DownloadStatusNotification)) {
			addBroadcastReceiver(DownloadBroadcastActions.DownloadStatusNotification);
		}

		// get the list
		getDownloadList();
	}

	@Override
	protected void onPause() {
		super.onPause();

		// just call finish to kill the instance of the activity
		finish();
	}

	@Override
	protected void onBroadcastReceiveOverride(Intent intent) {
		String intentAction = intent.getAction();

		if (DownloadBroadcastActions.DownloadListResponse.equalsIgnoreCase(intentAction)) {
			onDownloadListResponseReceive(intent);
		} else if (DownloadBroadcastActions.DownloadStatusNotification.equalsIgnoreCase(intentAction)) {
			onDownloadNotificationStatusUpdate(intent);
		}
	}

	private void getDownloadList() {
		showLoadingDialog();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// fire a broadcast to retrieve the list
				Intent downloadListRetrieveIntent = new Intent();
				downloadListRetrieveIntent.setAction(DownloadBroadcastActions.DownloadListRequest);
				sendBroadcastAsync(downloadListRetrieveIntent);
			}
		}).start();
	}

	private void onDownloadListResponseReceive(Intent intent) {
		if (intent.hasExtra(IntentParameterConstants.DownloadList)) {
			dismissLoadingDialog();

			ArrayList<?> arrayList = (ArrayList<?>) intent.getSerializableExtra(IntentParameterConstants.DownloadList);

			DownloadNotificationStatusResultList list = new DownloadNotificationStatusResultList();

			for (Object obj : arrayList) {
				if (obj instanceof DownloadNotificationStatusResult) {
					list.add((DownloadNotificationStatusResult) obj);
				}
			}

			this.list = list;
			loadList(list);
		}
	}

	private void onDownloadNotificationStatusUpdate(Intent intent) {
		if (listAdapter != null) {
			if (intent.hasExtra(IntentParameterConstants.DownloadNotificationStatusResult)) {
				DownloadNotificationStatusResult result = (DownloadNotificationStatusResult) intent.getSerializableExtra(IntentParameterConstants.DownloadNotificationStatusResult);
				DownloadNotificationStatus status = result.getNotificationStatus();

				if (status == DownloadNotificationStatus.DownloadStarted) {
					// add the result to the list
					this.list.add(result);

					// add the item to the list adapter
					listAdapter.addItem(result);
				} else if (status == DownloadNotificationStatus.DownloadInProgress) {
					// synchronize the item
					listAdapter.synchroniseItem(result);
				} else if (status == DownloadNotificationStatus.DownloadCompleted) {
					// remove item
					listAdapter.removeItem(result);
				} else if (status == DownloadNotificationStatus.ProcessingInProgress) {
					// add the item to the list adapter
					listAdapter.addItem(result);
				} else if (status == DownloadNotificationStatus.ProcessingCompleted) {
					// remove item
					listAdapter.removeItem(result);
				}

				// display message if there are no downloads
				toggleNoDownloadMessage();
			}
		}
	}

	private void toggleNoDownloadMessage() {

		// If there are no pending downloads display a message to reflect that.
		LinearLayout noDownloadMessage = (LinearLayout) findViewById(R.id.download_manager_no_download);

		TextView activeDownloadsMessage = (TextView)findViewById(R.id.download_manager_active_downloads);
		
		if (listAdapter.isEmpty()) {
			noDownloadMessage.setVisibility(View.VISIBLE);
			
			if (activeDownloadsMessage != null) {
				activeDownloadsMessage.setVisibility(View.GONE);
			}			
		} else {
			noDownloadMessage.setVisibility(View.GONE);
			
			if (activeDownloadsMessage != null) {
				activeDownloadsMessage.setVisibility(View.VISIBLE);
			}
		}
	}

	private void loadList(DownloadNotificationStatusResultList list) {
		ListView listView = (ListView) findViewById(R.id.download_manager_downloads_list);

		listAdapter = new DownloadManagerListAdapter(this, list);
		listView.setAdapter(listAdapter);
	}

	private class DownloadManagerListAdapter extends BaseAdapter {
		private Context context;
		private DownloadNotificationStatusResultList objects;
		private Map<String, DownloadNotificationStatusResult> objectMap;

		public DownloadManagerListAdapter(Context context, DownloadNotificationStatusResultList objects) {
			this.context = context;
			this.objects = objects;
			this.objectMap = new HashMap<String, DownloadNotificationStatusResult>();

			for (DownloadNotificationStatusResult result : objects) {
				objectMap.put(result.getPackageItem().getUniqueId(), result);
			}
		}

		@Override
		public int getCount() {
			return objects.size();
		}

		@Override
		public DownloadNotificationStatusResult getItem(int position) {
			return objects.get(position);
		}

		@Override
		public long getItemId(int id) {
			return id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			DownloadNotificationStatusResult downloadItem = objects.get(position);
			PackageItem packageItem = downloadItem.getPackageItem();
			String packageItemKey = packageItem.getUniqueId();

			if (downloadItem.getNotificationStatus() == DownloadNotificationStatus.DownloadStarted ||
				downloadItem.getNotificationStatus() == DownloadNotificationStatus.DownloadInProgress ||
				downloadItem.getNotificationStatus() == DownloadNotificationStatus.DownloadCompleted) {

				convertView = LayoutInflater.from(context).inflate(R.layout.download_manager_notification_template, parent, false);

				configureDownloadView(convertView, downloadItem);

			} else {
				convertView = LayoutInflater.from(context).inflate(R.layout.processing_notification_template, parent, false);

				configureProcessingView(convertView, downloadItem);
			}

			if (!notifications.containsKey(packageItemKey)) {
				notifications.put(packageItemKey, convertView);
			}

			return convertView;
		}

		private void configureDownloadView(View view, DownloadNotificationStatusResult downloadItem) {
			TextView title = (TextView) view.findViewById(R.id.download_notification_file_title);

			if (title != null) {
				title.setText(downloadItem.getPackageItem().getName());
			}

			TextView statusText = (TextView) view.findViewById(R.id.download_notification_status_text);

			if (statusText != null) {
				statusText.setText(downloadItem.getStatusText());
			}

			ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.download_notification_progress_bar);

			if (progressBar != null) {
				if (downloadItem.getNotificationStatus() == DownloadNotificationStatus.DownloadStarted) {
					progressBar.setIndeterminate(true);
				} else {
					progressBar.setIndeterminate(false);
					progressBar.setProgress(downloadItem.getProgress());
					progressBar.setMax(downloadItem.getMax());
				}
			}
		}

		private void configureProcessingView(View view, DownloadNotificationStatusResult downloadItem) {
			TextView title = (TextView) view.findViewById(R.id.processing_notification_file_title);

			if (title != null) {
				title.setText(downloadItem.getPackageItem().getName());
			}

			TextView statusText = (TextView) view.findViewById(R.id.processing_notification_status_text);

			if (statusText != null) {
				statusText.setText(getString(R.string.processing_notification_processing_text));
			}
		}

		public void addItem(DownloadNotificationStatusResult newItem) {
			objects.add(newItem);
			objectMap.put(newItem.getPackageItem().getUniqueId(), newItem);
			notifyDataSetChanged();
		}

		public void removeItem(DownloadNotificationStatusResult itemToRemove) {
			String packageItemUniqueId = itemToRemove.getPackageItem().getUniqueId();

			if (objectMap.containsKey(packageItemUniqueId)) {
				// get the item
				DownloadNotificationStatusResult obj = objectMap.get(packageItemUniqueId);

				objects.remove(obj);
				objectMap.remove(obj.getPackageItem().getUniqueId());
				notifyDataSetChanged();
			}
		}

		public void synchroniseItem(DownloadNotificationStatusResult updatedItem) {
			String packageItemUniqueId = updatedItem.getPackageItem().getUniqueId();

			if (objectMap.containsKey(packageItemUniqueId)) {
				DownloadNotificationStatusResult tempObject;

				// get the view for this object
				for (int index = 0; index <= (listView.getLastVisiblePosition() - listView.getFirstVisiblePosition()); index++) {
					// get package item for that index
					tempObject = objects.get(listView.getFirstVisiblePosition() + index);

					// check the unique ids are the same
					if (tempObject.getPackageItem().getUniqueId().equalsIgnoreCase(packageItemUniqueId)) {
						// get the view
						View view = listView.getChildAt(index);

						if (view != null) {
							configureDownloadView(view, updatedItem);
						}
					}
				}
			}
		}
	}

	private class DownloadManagerListItemCancelClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// get the item for the position
			Object dataObject = parent.getItemAtPosition(position);

			if (dataObject instanceof DownloadNotificationStatusResult) {
				DownloadNotificationStatusResult result = (DownloadNotificationStatusResult) dataObject;

				// send broadcast to cancel the item
				Intent cancelPackageItemDownloadIntent = new Intent();
				cancelPackageItemDownloadIntent.setAction(DownloadBroadcastActions.PackageDownloadCancelRequest);
				cancelPackageItemDownloadIntent.putExtra(IntentParameterConstants.PackageItem, result.getPackageItem());

				DownloadManagerActivity.this.sendBroadcast(cancelPackageItemDownloadIntent);

				// remove the object from the list
				listAdapter.removeItem(result);

				// display message if there are no downloads
				toggleNoDownloadMessage();
			}
		}
	}
}