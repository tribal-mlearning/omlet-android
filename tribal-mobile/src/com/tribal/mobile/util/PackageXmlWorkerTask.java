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

package com.tribal.mobile.util;

import java.util.Date;

import android.os.AsyncTask;
import android.util.Log;

import com.tribal.mobile.model.Package;

/**
 * Package XML Worker asynchronous task to deserialise a package XML file into an instance of a {@link Package} object.
 * 
 * @author Jon Brasted
 */
public class PackageXmlWorkerTask extends AsyncTask<Object, Void, Package> {
	PackageWorkerCompleted callback = null;
	private String packageFilePath;
	private String packageDirectory;
	private Object payload;

	// automatically done on worker thread (separate from UI thread)
	@Override
	protected Package doInBackground(Object... params) {
		Package packageObject = loadContent(params);

		//If the parsing failed then return null and an error will be raised from this
		if (packageObject == null) {
			return null;
		}
		
		// second parameter should be the xml file path
		packageDirectory = (String) params[1];
		packageObject.setExternalRootPath(packageDirectory);
		
		// second parameter should be the payload
		if (params.length > 2) {
			payload = params[2];
		}

		// process the package
		packageObject.processItems();

		return packageObject;
	}

	private Package loadContent(Object[] params) {

		// first parameter should be the xml file path
		packageFilePath = (String) params[0];

		// second parameter should be a callback
		this.callback = (PackageWorkerCompleted) params[3];

		Package packageObject = null;

		try {
			packageObject = XmlHelper.readXmlFileFromExternalStorage(packageFilePath, Package.class);
		} catch (Exception e) {
			//e.printStackTrace();
			//Log.d("jko > load package > " + packageFilePath, e.getMessage());
		}

		return packageObject;
	}

	// can use UI thread here
	@Override
	protected void onPostExecute(Package result) {
		// update menu progress
		Log.d("jko > loaded package", new Date().toString());

		if (callback != null) {
			callback.onPackageWorkerCompleted(result, payload);
		}
	}
}