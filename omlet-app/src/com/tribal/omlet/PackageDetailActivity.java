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

import org.apache.commons.lang3.StringUtils;
import com.tribal.omlet.util.PackageCatalogueAddToLibraryClickListener;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;
import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.download.DownloadBroadcastActions;
import com.tribal.mobile.image.DrawableBackgroundDownloader;

/**
 * Package detail activity.
 * 
 * @author Jon Brasted
 */
public class PackageDetailActivity extends SystemMenuProviderActivity {
	/* Fields */
	
	private PackageItem packageItem = null;
	private Button addToLibraryButton = null;
	
	/* Methods */
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		setContentView(R.layout.package_detail);

		getPackageItem();

		configureView();
		
		super.onCreate(savedInstanceState);
		
	}

	@Override
	protected void onResume() {
		subscribeToDownloadBroadcastActions();

		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onConnectivityChanged(boolean isConnected) {
		onNetworkAvailable(isConnected);
	}
	
	private void onNetworkAvailable(boolean isNetworkAvailable) {
		if (!isNetworkAvailable) {
			// close the activity
			finish();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home: {
	            // app icon in action bar clicked; go to Package Catalogue tab on home screen
	        	// Done this way because cross package linking download will cause 'back' to not be be the same as 'up'
	            Intent intent = new Intent(this, MainTabActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            intent.putExtra(IntentParameterConstants.SelectedTab, getString(R.string.packageStoreTabId));
	            startActivity(intent);
	            return true;
	        }
	        default: {
	            return super.onOptionsItemSelected(item);
	        }
	    }
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
	}

	@Override
	protected void onBroadcastReceiveOverride(Intent intent) {
		String intentAction = intent.getAction();

		if (intent.hasExtra(IntentParameterConstants.PackageItem)) {
			PackageItem packageItem = (PackageItem) intent.getSerializableExtra(IntentParameterConstants.PackageItem);

			if (packageItem.getName().equalsIgnoreCase(this.packageItem.getName())) {
				if (DownloadBroadcastActions.PackageDownloadQueued.equalsIgnoreCase(intentAction)) {
					updateDownloadStatusButton(DownloadBroadcastActions.PackageDownloadQueued);
				} else if (DownloadBroadcastActions.PackageDownloading.equalsIgnoreCase(intentAction)) {
					updateDownloadStatusButton(DownloadBroadcastActions.PackageDownloading);
				} else if (DownloadBroadcastActions.PackageDownloadCancelled.equalsIgnoreCase(intentAction)) {
					updateDownloadStatusButton(DownloadBroadcastActions.PackageDownloadCancelled);
				} else if (DownloadBroadcastActions.PackageDownloadFailed.equalsIgnoreCase(intentAction)) {
					updateDownloadStatusButton(DownloadBroadcastActions.PackageDownloadFailed);
				} else if (DownloadBroadcastActions.PackageDownloaded.equalsIgnoreCase(intentAction)) {
					updateDownloadStatusButton(DownloadBroadcastActions.PackageDownloaded);
				} else if (DownloadBroadcastActions.PackageProcessingQueued.equalsIgnoreCase(intentAction)) {
					updateDownloadStatusButton(DownloadBroadcastActions.PackageProcessingQueued);
				} else if (DownloadBroadcastActions.PackageProcessing.equalsIgnoreCase(intentAction)) {
					updateDownloadStatusButton(DownloadBroadcastActions.PackageProcessing);
				} else if (DownloadBroadcastActions.PackageProcessingCompleted.equalsIgnoreCase(intentAction)) {
					updateDownloadStatusButton(DownloadBroadcastActions.PackageProcessingCompleted);
				} else if (DownloadBroadcastActions.PackageReset.equalsIgnoreCase(intentAction)) {
					updateDownloadStatusButton(DownloadBroadcastActions.PackageReset);
				}
			}
		}
	}

	private void getPackageItem() {
		Bundle extras = getIntent().getExtras();

		if (extras != null && extras.containsKey(IntentParameterConstants.PackageItem)) {
			// get the package
			this.packageItem = (PackageItem) extras.getSerializable(IntentParameterConstants.PackageItem);
		}
	}

	private void configureView() {

		TextView tvDescription = (TextView) findViewById(R.id.package_detail_description);
		TextView tvName = (TextView) findViewById(R.id.store_listitem_name);
		TextView tvSize = (TextView) findViewById(R.id.package_detail_size);
		ImageView ivIcon = (ImageView) findViewById(R.id.store_listitem_image);
		TextView tvCourseCode = (TextView) findViewById(R.id.package_detail_code);
		TextView tvVersion = (TextView) findViewById(R.id.package_detail_version);

		String description = packageItem.getDescription();

		if (StringUtils.isEmpty(description)) {
			description = getString(R.string.noDescriptionText);
		}

		String size = getString(R.string.unknownText);
		if (StringUtils.isNotEmpty(packageItem.getFileSize())) {
			size = packageItem.getFileSize();
			Double fileSize = Double.parseDouble(size);

			if (fileSize < 1024) {
				size = fileSize.shortValue() + " KB";
			} else {
				fileSize = fileSize / 1024;
				size = fileSize.shortValue() + " MB";
			}

		}

		size = String.format(getString(R.string.package_size_format_string), size);

		String courseCodeFormatString = getString(R.string.course_code_format_string);
		String courseCode = String.format(courseCodeFormatString, getString(R.string.unknownText));

		if (StringUtils.isNotEmpty(packageItem.getCourseCode())) {
			courseCode = String.format(courseCodeFormatString, packageItem.getCourseCode());
		}		
		
		String versionFormatString = getString(R.string.version_format_string);
		String version = String.format(versionFormatString, getString(R.string.unknownText));
		
		if (StringUtils.isNotEmpty(packageItem.getVersion())) {
			version = String.format(versionFormatString, packageItem.getVersion());
		}		

		tvDescription.setText(description);
		tvName.setText(packageItem.getName());
		tvSize.setText(size);
		tvCourseCode.setText(courseCode);
		tvVersion.setText(version);

		DrawableBackgroundDownloader drawableBackgroundDownloader = ((OmletApplication)getApplication()).getDrawableBackgroundDownloader();
		drawableBackgroundDownloader.loadDrawableWithPlaceholder(packageItem.getImageUrl(), ivIcon);

		addToLibraryButton = (Button) findViewById(R.id.store_listitem_download);

		// get download status
		Intent intent = getIntent();

		if (intent.hasExtra(IntentParameterConstants.DownloadStatus)) {
			String downloadStatus = intent.getStringExtra(IntentParameterConstants.DownloadStatus);
			updateDownloadStatusButton(downloadStatus);
		}

		addToLibraryButton.setTag(packageItem);
		addToLibraryButton.setOnClickListener(new PackageCatalogueAddToLibraryClickListener());
	}

	private void updateDownloadStatusButton(String downloadStatus) {
		Button button = (Button) findViewById(R.id.store_listitem_download);

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
}