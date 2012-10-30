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

package com.tribal.omlet.util;

import com.tribal.omlet.CatalogueFragment;
import com.tribal.omlet.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewParent;
import android.widget.ListView;

import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.download.DownloadBroadcastActions;

/**
 * Package Store Add to Library Click Listener. Used by {@link CatalogueFragment}.
 * 
 * @author Jon Brasted
 */
public class PackageCatalogueAddToLibraryClickListener implements OnClickListener {
	/* Methods */
	
	@Override
	public void onClick(View paramView) {
		// get the parent
		ListView listView = getParentListView(paramView);
		
		if (listView != null && !listView.isEnabled()) {
			return;
		}
		
		// get the tag
		Object tag = paramView.getTag();

		if (tag instanceof PackageItem) {

			// get the package item from the view
			final PackageItem packageItem = (PackageItem) paramView.getTag();

			String size = packageItem.getFileSize();
			Double fileSize = Double.valueOf(0);
			try {
				fileSize = Double.parseDouble(size);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}

			// Get the available storage in KB
			StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
			long availableSpaceInKB = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize() / 1024;

			final Context context = paramView.getContext();

			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

			Boolean showDialog = false;
			String dialogTitle = context.getString(R.string.downloadWarningTitle);

			StringBuilder stringBuilder = new StringBuilder();

			final String lineSeparator = System.getProperty("line.separator"); // Platform new line
			
			// Show dialog if using 3G and over 5mb
			if (activeNetworkInfo.getType() == 0 && fileSize > 5120) {

				if (showDialog) {
					stringBuilder.append(lineSeparator).append(lineSeparator);
				}
				
				stringBuilder.append(context.getString(R.string.download3GWarning));
				showDialog = true;
			}
			// If using Wifi and over 10mb
			else if (activeNetworkInfo.getType() == 1 && fileSize > 10240) {

				if (showDialog) {
					stringBuilder.append(lineSeparator).append(lineSeparator);
				}
				
				stringBuilder.append(context.getString(R.string.downloadWifiWarning));
				showDialog = true;

			}
			// If file is over 50mb
			if (fileSize > 51200) {
				if (showDialog) {
					stringBuilder.append(lineSeparator).append(lineSeparator);
				}
				
				stringBuilder.append(context.getString(R.string.downloadExceedsSizeWarning));
				showDialog = true;
			}

			// If the package is bigger than we have available then don't
			// download.
			// A future enhancement could involve checking uncompressed size instead of compressed size
			if (fileSize * 2 > availableSpaceInKB) {

				if (showDialog) {
					stringBuilder.append(lineSeparator).append(lineSeparator);
				}
				
				stringBuilder.append(context.getString(R.string.sdCardFullError));
				showDialog = true;
			}

			// If there is a warning then display a dialog
			if (showDialog) {

				AlertDialog alertDialog = new AlertDialog.Builder(context).create();
				alertDialog.setTitle(dialogTitle);
				alertDialog.setMessage(stringBuilder.toString());

				String continueString = context.getString(R.string.button_continue);
				String cancelString = context.getString(R.string.button_cancel);
				
				alertDialog.setButton(continueString, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int whichButton)
					{
						// Ignore warning and continue with download
						dialog.cancel();

						downloadPackage(packageItem, context);
					}
				});

				alertDialog.setButton2(cancelString, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						// Cancel and do not download
						dialog.cancel();

					}
				});

				alertDialog.show();
			} else {
				// If there are no warnings just download as usual
				downloadPackage(packageItem, context);
			}
		}
	}

	private void downloadPackage(PackageItem packageItem, Context context) {

		// send broadcast to add the package item to the queue
		Intent addPackageToLibraryBroadcastIntent = new Intent();

		addPackageToLibraryBroadcastIntent.setAction(DownloadBroadcastActions.AddPackageToLibrary);
		addPackageToLibraryBroadcastIntent.putExtra(IntentParameterConstants.PackageItem, packageItem);

		// pass the package item to the download service
		context.sendBroadcast(addPackageToLibraryBroadcastIntent);
	}
	
	private ListView getParentListView(View view) {
		ListView listView = null;
		ViewParent parent = view.getParent();
		
		if (parent instanceof View) {
			if (parent instanceof ListView) {
				listView = (ListView)parent;
			} else {
				listView = getParentListView((View)parent);
			}
		}
		
		return listView;
	}
}