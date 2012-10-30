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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.ProgressBar;

import com.tribal.mobile.R;
import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.util.FileHelper;
import com.tribal.mobile.util.PackageHelper;
import com.tribal.mobile.util.resources.ResourceHelper;
import com.tribal.mobile.util.resources.ResourceItemType;
import com.tribal.mobile.util.resources.StringResourceLookups;

/**
 * Class provides logic required to unzip and process a zip file in the background and store the information in the local SQLite database.
 * 
 * @author Jon Brasted
 */
public class UnzipFileAsync extends AsyncTask<Object, String, Boolean> {

	private static final int bufferSize = 1024;
	private UnzipFileAsyncCompleted callback = null;
	private ProgressBar progressBar;
	private String courseFolderPath;
	private String errorMessage;
	private File file;
	private Context context;
	private BaseApplication application;
	private PackageItem packageItem;

	@Override
	protected Boolean doInBackground(Object... params) {

		try {
			// first parameter should be the file
			file = (File)params[0];

			// second parameter should be the application
			application = (BaseApplication)params[1]; 
			
			context = application.getApplicationContext();
			
			// third param will be callback
			callback = (UnzipFileAsyncCompleted)params[2];
			
			// fourth param will be package
			packageItem = (PackageItem)params[3];

			final FileInputStream fin = new FileInputStream(file);
			final ZipInputStream inputStream = new ZipInputStream(fin);

			// construct a path for the unzipped folder with the same name
			// as the zip
			courseFolderPath = FileHelper.removeExtensionFromPath(file.getAbsolutePath());

			// create the folder
			File courseFolder = new File(courseFolderPath);

			if (!courseFolder.exists()) {
				courseFolder.mkdir();
			}

			// course folder

			// Loop through all the files and folders
			for (ZipEntry entry = inputStream.getNextEntry(); entry != null; entry = inputStream
					.getNextEntry()) {
				File innerFile = new File(courseFolderPath, entry.getName());

				if (innerFile.exists()) {
					innerFile.delete();
				}
				
				// Check if it is a folder
				if (entry.isDirectory()) {
					// Its a folder, create that folder
					innerFile.mkdirs();

				} else {
					// ZipEntry thinks this is a file
					// However, if this fails, we should take the parent and try and make it
					if (!innerFile.isDirectory() && !innerFile.isFile()) {
						// parent
						File parentFile = innerFile.getParentFile();
						
						if (parentFile != null) {
							parentFile.mkdirs();
						}						
					}
					
					// Create a file output stream
					FileOutputStream outputStream = new FileOutputStream(innerFile);

					// Buffer the ouput to the file
					BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, bufferSize);

					// Write the contents
					int count = 0;

					byte[] buff = new byte[bufferSize];
					
					while ((count = inputStream.read(buff, 0, bufferSize)) != -1) {

						// Back button is pressed stop reading.
						if (isCancelled()) {
							break;
						}

						bufferedOutputStream.write(buff, 0, count);
					}

					// Flush and close the buffers
					bufferedOutputStream.flush();
					bufferedOutputStream.close();
				}

				// Close the current entry
				inputStream.closeEntry();
			}
			
			inputStream.close();

			// Copy Image to SDCard
			int externalStorageImagesPathResourceId = ResourceHelper.getResourceIdByName(context, ResourceItemType.string, StringResourceLookups.ExternalStorageImagesPath); 
			String externalStorageImagesPath = context.getString(externalStorageImagesPathResourceId);
			String imagePath = Environment.getExternalStorageDirectory() + externalStorageImagesPath;

			//folder name will be packages Unique id
			String folderName = packageItem.getUniqueId();
			String courseImageFileName = folderName  +".png";
			
			PackageHelper.copyDrawableToExternalStorage(context, packageItem.getImageUrl(), imagePath, courseImageFileName);
			
			//Add package to library database table
			application.getDatabaseHelper().createMyLibraryEntry(packageItem, imagePath + "/"+ courseImageFileName, folderName);
			
			
			// find the package xml inside the folder
			File packageXml = findPackageXmlInsideFolder(courseFolder);

			if (packageXml != null) {
				// Copy js files will return true if successful
				copyJsFiles(packageXml.getParent());
			}
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	@Override
	protected void onProgressUpdate(String... progress) {
		progressBar.setProgress(Integer.parseInt(progress[0]));
	}

	@Override
	protected void onPostExecute(Boolean success) {
		if (callback != null) {
			callback.onUnzipFileAsyncCompleted(file, success, errorMessage);
		}
	}

	private File findPackageXmlInsideFolder(File folderPathDirectory) {
		// this will be recursive
		List<File> directoriesToSearch = new ArrayList<File>();

		// 1. look inside this folder
		for (File file : folderPathDirectory.listFiles()) {
			if (!file.isDirectory()) {
				if (file.getName().equals("package.xml")) {
					return file;
				}
			} else {
				directoriesToSearch.add(file);
			}
		}

		// 2. not found the package.xml
		// Search the directories

		for (File directory : directoriesToSearch) {
			File innerFile = findPackageXmlInsideFolder(directory);

			if (innerFile != null) {
				return innerFile;
			}
		}

		// 3. not found anything
		return null;
	}

	// Read a file from assets and write it out to package folder path on sdcard
	private void copyFileToSdFromPath(String path, String name,
			String localCoursePath) {
		try {
			// file we are creating
			File outfile = new File(localCoursePath, name);
			FileOutputStream f = new FileOutputStream(outfile);
			// file from assets
			InputStream in = context.getAssets().open(path);

			// create read buffer
			byte[] buffer = new byte[bufferSize];
			int len1 = 0;

			// write to a file
			while ((len1 = in.read(buffer)) > 0) {
				f.write(buffer, 0, len1);
			}

			f.close();

		} catch (Exception e) {
			// if failed			
			errorMessage = context.getString(R.string.packageFindXmlError);
		}
	}

	// Copy the phonegap files from assets to the extracted path
	private void copyJsFiles(String localCoursePath) {
		String appJSPath = context.getString(R.string.app_js_path);
		String appRealJSPath = context.getString(R.string.app_real_js_path);
		String phonegapJSPath = context.getString(R.string.phonegap_js_path);
		String phonegapAndroidJSPath = context.getString(R.string.phonegap_android_js_path);
		String frameworkJSPath = context.getString(R.string.framework_js_path);
		
		String appJSName = context.getString(R.string.app_js_name);
		String appRealJSName = context.getString(R.string.app_real_js_name);
		String phonegapJSName = context.getString(R.string.phonegap_js_name);
		String phonegapAndroidJSName = context.getString(R.string.phonegap_android_js_name);
		String frameworkJSName = context.getString(R.string.framework_js_name);
		
		copyFileToSdFromPath(appJSPath, appJSName, localCoursePath);
		copyFileToSdFromPath(appRealJSPath, appRealJSName, localCoursePath);
		copyFileToSdFromPath(phonegapJSPath, phonegapJSName, localCoursePath);
		copyFileToSdFromPath(phonegapAndroidJSPath, phonegapAndroidJSName, localCoursePath);
		copyFileToSdFromPath(frameworkJSPath, frameworkJSName, localCoursePath);
	}
}