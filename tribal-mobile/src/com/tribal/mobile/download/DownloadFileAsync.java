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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.net.AuthHttpConnection;
import com.tribal.mobile.net.HttpMethod;
import com.tribal.mobile.net.URLConnectionUtils;
import com.tribal.mobile.preferences.PrivateSettingsKeys;
import com.tribal.mobile.util.NativeSettingsHelper;
import com.tribal.mobile.util.ServiceLayerExceptionHelper;

/**
 * Class provides logic required to download a file in the background.
 * 
 * @author Jon Brasted
 */
public class DownloadFileAsync extends AsyncTask<Object, Double, File> {

	private String LOG_TAG = "Downloader";
	private static final int bufferSize = 1024;
	private String fileUrl;
	private Object payload;
	private int lastPublishedPercentage = -1;
	private DownloadFileAsyncProgressUpdate progressUpdateCallback = null;
	private DownloadFileAsyncCompleted completedCallback = null;
	private DownloadFileAsyncCancelled cancelledCallback = null;
	private DownloadFileAsyncFailed failedCallback = null;
	private Context context = null;
	private BaseApplication application;
	private PackageItem packageItem = null;

	@Override
	protected File doInBackground(Object... params) {

		try {
			// first parameter should be the destination folder path
			String destinationFolderPath = (String) params[0];

			// second param will be the file url
			fileUrl = (String) params[1];

			// third param will be a data payload
			payload = params[2];

			// fourth param will be progress callback
			progressUpdateCallback = (DownloadFileAsyncProgressUpdate) params[3];

			// fifth param will be completed callback
			completedCallback = (DownloadFileAsyncCompleted) params[4];
			
			// sixth param will be the cancelled callback
			cancelledCallback = (DownloadFileAsyncCancelled) params[5];
			
			// seventh param will be the failed callback
			failedCallback = (DownloadFileAsyncFailed) params[6];

			// eighth param will be context
			context = (Context)params[7];
			
			// ninth param will be application
			application = (BaseApplication)params[8];
			
			packageItem = (PackageItem) payload;
			
			//Set Package status to updating if it already exists
			application.getDatabaseHelper().updateLibraryEntryStatus(packageItem.getUniqueId(), DownloadBroadcastActions.PackageUpdate);
			
			// making sure the download directory exists
			checkAndCreateDirectory(destinationFolderPath);

			// connecting to url
			URL url = new URL(fileUrl);
			
			boolean isUrlHttps = url.getProtocol().toLowerCase().equals("https"); 
			final boolean acceptSelfSignedCertificates = NativeSettingsHelper.getInstance(context).checkAndGetPrivateBooleanSetting(PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS, false);
			
			if (isUrlHttps) {
				if (acceptSelfSignedCertificates) {
					URLConnectionUtils.enableSelfSignedSSLCertificates();
					
				} else {
					URLConnectionUtils.enableSSLCertificateCheck();
				}
			}

			HttpURLConnection connection = null;
			
			if (isUrlHttps) {
				HttpsURLConnection https = (HttpsURLConnection)url.openConnection();
                https.setHostnameVerifier(URLConnectionUtils.DO_NOT_VERIFY);
                connection = https;
			} else {
				connection = (HttpURLConnection) url.openConnection();
			}
			
			connection.setRequestMethod("GET");
			
			final AuthHttpConnection result = new AuthHttpConnection(HttpMethod.GET, fileUrl, null, null, null);
			connection.setRequestProperty("X-AUTH", result.getAuthorisationHeader(false));
			connection.connect();

			double lengthOfFile = 0;
			
			//if we can't get file length from the connection then use the one stored on the object.
			try {
				double packageSize = Double.parseDouble(packageItem.getFileSize());
				
				// Convert from kb to bytes
				lengthOfFile = (long)(packageSize * 1024);
				
				if (lengthOfFile <= 0) {
					// lengthOfFile is used for calculating download progress
					lengthOfFile = connection.getContentLength();
				}				
			} catch (Exception e) {
				Log.e(LOG_TAG, "Error parsing fileSize", e);
			}

			String fileName = packageItem.getUniqueId() + ".zip";

			File file = new File(destinationFolderPath, fileName);

			// this is where the file will be seen after the download
			FileOutputStream f = new FileOutputStream(file);

			// file input is from the url
			InputStream in = connection.getInputStream();

			// here's the download code
			byte[] buffer = new byte[bufferSize];
			int len1 = 0;
			long total = 0;

			// publish progress of 0 to inform that the download has started
			publishProgress((double) 0, (double) 100);

			while ((len1 = in.read(buffer)) > 0) {
				// Stop loop if cancelled (e.g back button press)
				if (isCancelled()) {
					f.close();
					return null;
				}

				total += len1;

				// check that we have not already publishes this percentage
				// publish progress
				publishProgress((double) total, lengthOfFile);

				f.write(buffer, 0, len1);
			}
			
			// if file has finished downloading but the total is smaller than length of file, publish another progress update
			// so file registers as having been downloaded
			if (total < lengthOfFile) {				
				publishProgress(lengthOfFile, lengthOfFile);
			}

			f.close();

			return file;

		} catch (Exception e) {
			ServiceLayerExceptionHelper.getInstance().processException(e, context);

			if (failedCallback != null) {
				failedCallback.onDownloadFileAsyncFailed(fileUrl, payload, e);
			}
		}

		return null;
	}

	@Override
	protected void onProgressUpdate(Double... values) {
		Log.d(LOG_TAG, "" + values[0]);

		double percentageDownloaded = values[0] / values[1] * 100;
		int percentage = (int) percentageDownloaded;

		// check that we have not already publishes this percentage
		if (lastPublishedPercentage < percentage) {
			// send progress update
			if (progressUpdateCallback != null) {
				progressUpdateCallback.onDownloadFileAsyncProgressUpdate(fileUrl, payload, Double.valueOf(values[0]).longValue(), Double.valueOf(values[1]).longValue());
			}

			lastPublishedPercentage = percentage;
		}
	}

	@Override
	protected void onPostExecute(File file) {
		if (file != null && completedCallback != null) {
			completedCallback.onDownloadFileAsyncCompleted(fileUrl, payload, file);
		}
	}

	// function to verify if directory exists
	public void checkAndCreateDirectory(String dirName) {
		File newDir = new File(dirName);

		if (!newDir.exists()) {
			// make a new one
			newDir.mkdirs();
		}
	}
	
	@Override
	protected void onCancelled() {
		super.onCancelled();
		
		//Reset Package Status
		application.getDatabaseHelper().updateLibraryEntryStatus(packageItem.getUniqueId(), DownloadBroadcastActions.PackageProcessingCompleted);
		
		if (cancelledCallback != null) {
			cancelledCallback.onDownloadFileAsyncCancelled(fileUrl, payload);
		}
	}
}