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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.content.Context;
import android.util.Log;

/**
 * Utility class to read XML file from assets or external storage.
 * 
 * @author Jon Brasted
 */
public class XmlHelper {
	
	public static <T> T readXmlFileFromAssets(Context context, String fileName, Class<? extends T> className) throws Exception {
		// get the input stream for the file
		InputStream inputStream = context.getAssets().open(fileName);
		
		Serializer serializer = new Persister();
		
		// read the stream into an object
		T result = serializer.read(className, inputStream);
		
		return result;
	}
	
	public static <T> T readXmlFileFromExternalStorage(String fullFilePath, Class<? extends T> className) throws Exception {
		// get the input stream for the file
		
		File file = new File(fullFilePath);
		
		FileInputStream inputStream = new FileInputStream(file);

		Serializer serializer = new Persister();
		
		// read the stream into an object
		T result = null;
		try {
			result = serializer.read(className, inputStream);
		} catch (Exception e) {		
			e.printStackTrace();
			Log.d("jko > load package > ", e.getMessage());
		}
		
		return result;
	}
}