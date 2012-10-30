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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tribal.omlet.download.DownloadNotificationStatus;
import com.tribal.omlet.download.DownloadNotificationStatusResult;
import com.tribal.omlet.util.PackageCatalogueAddToLibraryClickListener;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.tribal.mobile.Framework;
import com.tribal.mobile.api.packages.Catalogue;
import com.tribal.mobile.api.packages.PackageCatalogueRetrieved;
import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.download.DownloadBroadcastActions;
import com.tribal.mobile.fragments.BaseFragment;
import com.tribal.mobile.image.DrawableBackgroundDownloader;
import com.tribal.mobile.util.CustomBaseAdapter;
import com.tribal.mobile.util.ViewHelper;
import com.tribal.mobile.util.database.BaseDatabaseHelper;

/**
 * Catalogue fragment.
 * 
 * @author Jack Kierney
 */
public class CatalogueFragment extends BaseFragment implements PackageCatalogueRetrieved {
	/* Fields */

	private Catalogue packageCatalogueList = null;
	private ListView listView = null;
	private Cursor libraryCursor = null;

	private SystemMenuProviderActivity parentActivity;

	private Map<String, String> packageItemDownloadStatuses;

	private boolean isRetrievingCatalogue = false;

	/* Methods */

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) {
			return null;
		}
		
		return inflater.inflate(R.layout.package_list, container, false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		packageItemDownloadStatuses = new HashMap<String, String>();
		parentActivity = (SystemMenuProviderActivity) getActivity();
	}

	@Override
	public void onResume() {
		super.onResume();

		subscribeToDownloadBroadcastActions();

		loadCatalogue();
	}

	private void subscribeToDownloadBroadcastActions() {
		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageDownloadQueued)) {
			addBroadcastReceiver(DownloadBroadcastActions.PackageDownloadQueued);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageDownloading)) {
			addBroadcastReceiver(DownloadBroadcastActions.PackageDownloading);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageDownloadCancelled)) {
			addBroadcastReceiver(DownloadBroadcastActions.PackageDownloadCancelled);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageDownloadFailed)) {
			addBroadcastReceiver(DownloadBroadcastActions.PackageDownloadFailed);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageDownloaded)) {
			addBroadcastReceiver(DownloadBroadcastActions.PackageDownloaded);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageProcessingQueued)) {
			addBroadcastReceiver(DownloadBroadcastActions.PackageProcessingQueued);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageProcessing)) {
			addBroadcastReceiver(DownloadBroadcastActions.PackageProcessing);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageProcessingCompleted)) {
			addBroadcastReceiver(DownloadBroadcastActions.PackageProcessingCompleted);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageReset)) {
			addBroadcastReceiver(DownloadBroadcastActions.PackageReset);
		}

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.CatalogueTabChanged)) {
			addBroadcastReceiver(BroadcastActions.CatalogueTabChanged);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.DownloadListResponse)) {
			addBroadcastReceiver(DownloadBroadcastActions.DownloadListResponse);
		}
	}

	private void loadCatalogue() {
		if (parentActivity == null || Framework.getServer() == null) {
			return;
		}

		// Check for connectivity return if none
		if (!parentActivity.isNetworkAvailable() || isRetrievingCatalogue) {
			return;
		}

		isRetrievingCatalogue = true;

		// show loading dialog
		parentActivity.showLoadingDialog();

		// Call package service
		Framework.getServer().getPackageCatalogue(this);

		listView = (ListView) parentActivity.findViewById(R.id.lst_packages);

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				HashMap<String, Serializable> parameterMap = new HashMap<String, Serializable>();

				// check the content item

				// use standard navigation logic

				Class<?> activityClass = PackageDetailActivity.class;

				// add content item

				if (packageCatalogueList != null) {
					List<PackageItem> packageList = packageCatalogueList.getPackages();
					PackageItem selectedPackage = packageList.get(position);

					if (!parameterMap.containsKey(IntentParameterConstants.PackageItem)) {
						parameterMap.put(IntentParameterConstants.PackageItem, selectedPackage);
					}

					// get download status
					if (packageItemDownloadStatuses.containsKey(selectedPackage.getUniqueId())) {
						String downloadStatus = packageItemDownloadStatuses.get(selectedPackage.getUniqueId());
						parameterMap.put(IntentParameterConstants.DownloadStatus, downloadStatus);
					}
				}

				// navigate
				parentActivity.navigate(getActivity().getBaseContext(), activityClass, parameterMap, null);
			}
		});
	}

	@Override
	protected void onBroadcastReceiveOverride(Intent intent) {
		String intentAction = intent.getAction();

		if (intent.hasExtra(IntentParameterConstants.PackageItem)) {
			PackageItem packageItem = (PackageItem)intent.getSerializableExtra(IntentParameterConstants.PackageItem);

			if (DownloadBroadcastActions.PackageDownloadQueued.equalsIgnoreCase(intentAction)) {
				updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageDownloadQueued);
			} else if (DownloadBroadcastActions.PackageDownloading.equalsIgnoreCase(intentAction)) {
				updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageDownloading);
			} else if (DownloadBroadcastActions.PackageDownloadCancelled.equalsIgnoreCase(intentAction)) {
				updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageDownloadCancelled);
			} else if (DownloadBroadcastActions.PackageDownloadFailed .equalsIgnoreCase(intentAction)) {
				updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageDownloadFailed);
			} else if (DownloadBroadcastActions.PackageDownloaded.equalsIgnoreCase(intentAction)) {
				updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageDownloaded);
			} else if (DownloadBroadcastActions.PackageProcessingQueued.equalsIgnoreCase(intentAction)) {
				updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageProcessingQueued);
			} else if (DownloadBroadcastActions.PackageProcessing.equalsIgnoreCase(intentAction)) {
				updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageProcessing);
			} else if (DownloadBroadcastActions.PackageProcessingCompleted.equalsIgnoreCase(intentAction)) {
				updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageProcessingCompleted);
			} else if (DownloadBroadcastActions.PackageReset.equalsIgnoreCase(intentAction)) {
				updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageReset);
			}
		} else if (DownloadBroadcastActions.DownloadListResponse.equalsIgnoreCase(intentAction)) {
			onDownloadListResponseReceive(intent);
		}
	}

	private void updatePackageItemDownloadStatus(PackageItem packageItem, String downloadStatus) {
		// get package item unique id
		String packageItemId = packageItem.getUniqueId();

		// put the package item into the map
		if (packageItemDownloadStatuses.containsKey(packageItemId)) {
			packageItemDownloadStatuses.remove(packageItemId);
		}

		packageItemDownloadStatuses.put(packageItemId, downloadStatus);
		PackageItem tempPackageItem;

		// check that the package item is visible

		if (packageCatalogueList == null) {
			return;
		}

		List<PackageItem> packageItems = this.packageCatalogueList.getPackages();

		for (int index = 0; index <= (listView.getLastVisiblePosition() - listView.getFirstVisiblePosition()); index++) {
			// get package item for that index
			tempPackageItem = packageItems.get(listView.getFirstVisiblePosition() + index);

			// check the names are the same
			if (tempPackageItem.getUniqueId().equalsIgnoreCase(packageItemId)) {
				// get the view
				View view = listView.getChildAt(index);

				if (view != null) {
					// get the download button
					Button button = (Button)view.findViewById(R.id.store_listitem_download);

					if (button != null) {
						// Update the button
						updateDownloadStatusButton(button, downloadStatus);
					}
				}
			}
		}
	}

	private void updateDownloadStatusButton(Button button, String downloadStatus) {
		if (DownloadBroadcastActions.PackageDownloadQueued.equalsIgnoreCase(downloadStatus)) {
			// disable the button
			button.setEnabled(false);

			// set the text
			button.setText(getString(R.string.downloadQueued));
			
		} else if (DownloadBroadcastActions.PackageDownloading.equalsIgnoreCase(downloadStatus)) {
			// disable the button
			button.setEnabled(false);

			// set the text
			button.setText(getString(R.string.downloading));

		} else if (DownloadBroadcastActions.PackageDownloaded.equalsIgnoreCase(downloadStatus)) {
			// disable the button
			button.setEnabled(false);

			// set the text
			button.setText(getString(R.string.downloaded));

		} else if (DownloadBroadcastActions.PackageDownloadFailed.equalsIgnoreCase(downloadStatus)) {
			// disable the button
			button.setEnabled(false);

			// set the text
			button.setText(getString(R.string.downloadFailed));

		} else if (DownloadBroadcastActions.PackageProcessingQueued.equalsIgnoreCase(downloadStatus)) {
			// disable the button
			button.setEnabled(false);

			// set the text
			button.setText(getString(R.string.processingQueued));

		} else if (DownloadBroadcastActions.PackageProcessing.equalsIgnoreCase(downloadStatus)) {
			// disable the button
			button.setEnabled(false);

			// set the text
			button.setText(getString(R.string.processing));

		} else if (DownloadBroadcastActions.PackageProcessingCompleted.equalsIgnoreCase(downloadStatus)) {
			// disable the button
			button.setEnabled(false);

			// set the text
			button.setText(getString(R.string.installed));

		} else if (DownloadBroadcastActions.PackageUpdate.equalsIgnoreCase(downloadStatus)) {
			// disable the button
			button.setEnabled(true);

			// set the text
			button.setText(getString(R.string.update));

		} else {
			// reset the button
			button.setEnabled(true);
			button.setText(getString(R.string.addToLibrary));
		}
	}

	@Override
	public void onCatalogueRetrieved(Catalogue catalogue) {
		isRetrievingCatalogue = false;

		parentActivity.dismissLoadingDialog();

		// add packages to a list and display them on screen
		if (catalogue != null) {

			OmletApplication application = (OmletApplication) getActivity().getApplication();
			
			// get image cache object
			DrawableBackgroundDownloader drawableBackgroundDownloader = application.getDrawableBackgroundDownloader();
			packageCatalogueList = catalogue;

			BaseDatabaseHelper databaseHelper = parentActivity.getDatabaseHelper();

			// get library
			if (this.libraryCursor != null) {
				this.libraryCursor.close();
				this.libraryCursor = null;
			}

			this.libraryCursor = databaseHelper.getMyLibrary();
			String packageIdColumn = getString(R.string.library_column_uniqueId);
			String libraryMD5sumColumn = getString(R.string.library_column_md5sum);

			try {
				// Loop through the packages that have been retrieved from the
				// service
				for (PackageItem packageItem : packageCatalogueList.getPackages()) {

					// Loop through the packages that are saved in the database
					// comparing them to see
					// if the package is already downloaded.
					boolean hasNext = libraryCursor.moveToFirst();

					while (hasNext) {
						String packageId = libraryCursor.getString(libraryCursor.getColumnIndex(packageIdColumn));

						if (packageItem.getUniqueId().equalsIgnoreCase(packageId)) {

							String md5sum = libraryCursor.getString(libraryCursor.getColumnIndex(libraryMD5sumColumn));

							String packageMD5Sum = packageItem.getMD5sum();

							if (!TextUtils.isEmpty(packageMD5Sum) && !packageMD5Sum.equalsIgnoreCase(md5sum)) {
								updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageUpdate);
							} else {
								updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageProcessingCompleted);
							}
						}

						hasNext = libraryCursor.moveToNext();
					}
				}
			} catch (Exception e) {
				Log.e(this.getClass().getSimpleName(), e.getMessage(), e);
			}

			// Set Adapter
			PackageStoreAdapter adapter = new PackageStoreAdapter(parentActivity.getBaseContext(), 
																packageCatalogueList.getPackages(),
																drawableBackgroundDownloader);

			listView.setAdapter(adapter);

		}

		// Get the courses that are currently being downloaded/processed and
		// show their status
		getDownloadList();

	}

	private void getDownloadList() {

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

			ArrayList<?> arrayList = (ArrayList<?>) intent.getSerializableExtra(IntentParameterConstants.DownloadList);

			// Loop through the current downloads and display their progress in
			// the buttons on screen
			for (Object obj : arrayList) {
				if (obj instanceof DownloadNotificationStatusResult) {
					DownloadNotificationStatusResult result = ((DownloadNotificationStatusResult) obj);

					DownloadNotificationStatus status = result.getNotificationStatus();
					PackageItem packageItem = result.getPackageItem();

					if (status == DownloadNotificationStatus.DownloadStarted) {
						updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageDownloading);
					} else if (status == DownloadNotificationStatus.DownloadInProgress) {
						updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageDownloading);
					} else if (status == DownloadNotificationStatus.DownloadCompleted) {
						updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageDownloaded);
					} else if (status == DownloadNotificationStatus.ProcessingInProgress) {
						updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageProcessing);
					} else if (status == DownloadNotificationStatus.ProcessingCompleted) {
						updatePackageItemDownloadStatus(packageItem, DownloadBroadcastActions.PackageProcessingCompleted);
					}
				}
			}
		}
	}

	@Override
	protected void onConnectivityChanged(boolean isConnected) {
		super.onConnectivityChanged(isConnected);

		listView = (ListView) parentActivity.findViewById(R.id.lst_packages);

		LinearLayout noConnectionTextContainer = (LinearLayout) parentActivity.findViewById(R.id.package_list_no_connection);

		// re-fetch the catalogue
		if (isConnected) {
			// enable the listView
			listView.setVisibility(View.VISIBLE);
			noConnectionTextContainer.setVisibility(View.GONE);

			loadCatalogue();
		} else {
			// disable the listView
			listView.setVisibility(View.GONE);
			noConnectionTextContainer.setVisibility(View.VISIBLE);
		}

		View view;

		for (int index = listView.getFirstVisiblePosition(); 
				index <= (listView.getLastVisiblePosition() - listView.getFirstVisiblePosition()); index++) {
			// get item for that index
			view = listView.getChildAt(index);

			// enable or disable the items
			if (view != null) {
				view.setEnabled(isConnected);
				ViewHelper.setAlphaFadeEnabled(view, isConnected);

				Button downloadButton = (Button)view.findViewById(R.id.store_listitem_download);
				downloadButton.setClickable(isConnected);
			}
		}
	}

	private class PackageStoreAdapter extends CustomBaseAdapter {
		private List<PackageItem> elements;
		private Context context;
		private DrawableBackgroundDownloader drawableBackgroundDownloader;

		public PackageStoreAdapter(Context context, List<PackageItem> packageCatalogue, DrawableBackgroundDownloader drawableBackgroundDownloader) {
			this.elements = packageCatalogue;
			this.context = context;
			this.drawableBackgroundDownloader = drawableBackgroundDownloader;
		}

		@Override
		public int getCount() {
			return elements.size();
		}

		@Override
		public Object getItem(int position) {
			return elements.get(position);
		}

		@Override
		public long getItemId(int id) {
			return id;
		}

		@Override
		protected View getViewOverride(int position, View convertView, ViewGroup parent) {
			PackageItem packageItem = elements.get(position);

			if (convertView == null) {
				convertView = LayoutInflater.from(context).inflate(R.layout.store_listitem, parent, false);
			}

			boolean listViewEnabled = listView.isEnabled();

			convertView.setEnabled(listViewEnabled);
			ViewHelper.setAlphaFadeEnabled(convertView, listViewEnabled);

			TextView tv = (TextView)convertView.findViewById(R.id.store_listitem_name);
			tv.setText(packageItem.getName());

			tv = (TextView)convertView.findViewById(R.id.store_listitem_code);
			tv.setText(packageItem.getCourseCode());

			ImageView imView = (ImageView)convertView.findViewById(R.id.store_listitem_image);

			Button downloadButton = (Button)convertView.findViewById(R.id.store_listitem_download);
			downloadButton.setTag(packageItem);
			downloadButton.setClickable(listViewEnabled);
			downloadButton.setOnClickListener(new PackageCatalogueAddToLibraryClickListener());

			if (packageItem.isDownloaded()) {
				updateDownloadStatusButton(downloadButton, DownloadBroadcastActions.PackageProcessingCompleted);
			} else {
				String downloadStatus = null;

				if (packageItemDownloadStatuses.containsKey(packageItem.getUniqueId())) {
					downloadStatus = packageItemDownloadStatuses.get(packageItem.getUniqueId());
				}

				updateDownloadStatusButton(downloadButton, downloadStatus);
			}

			drawableBackgroundDownloader.loadDrawableWithPlaceholder(packageItem.getImageUrl(), imView);

			return convertView;
		}
	}
}