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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.MimeTypeMap;

public class NativeFileHelper {
	public static final String MIME_TYPE_EPUB = "application/epub+zip";
	public static final String MIME_TYPE_MOBI = "application/x-mobipocket-ebook";
	
	/**
     * Returns whether the supplied context can render PDF files via some installed application that reacts to a intent
     * with the pdf mime type and viewing action.
     *
     * @param context	the context
     * @return			whether the supplied context can render PDF files via some installed application that reacts to a intent
     * 					with the pdf mime type and viewing action.
     */
    public static boolean canOpenFile(Context context, File file) {
        PackageManager packageManager = context.getPackageManager();
        Intent testIntent = getOpenFileIntent(file);
        
        if (packageManager.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Return the mime type for a file.
     * 
     * @param filePath	the file path
     * @return			the mime type
     */
    public static String getMimeTypeForFile(String filePath) {
    	MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
    	
    	// get file extension
    	String fileExtension = MimeTypeMap.getFileExtensionFromUrl(getFileExtensionFromPath(filePath));
    	
    	// get mime type from extension
    	String mimeType = mimeTypeMap.getMimeTypeFromExtension(fileExtension);
    	
    	// if the mime type is null, try one of the statically defined ones
    	if (mimeType == null) {
    		if ("epub".equalsIgnoreCase(fileExtension)) {
    			return MIME_TYPE_EPUB;
    		} else if ("mobi".equalsIgnoreCase(fileExtension)) {
    			return MIME_TYPE_MOBI;
    		}
    	}
    	
    	return mimeType;
    }
    
    /**
     * Return file extension for a file path.
     * 
     * @param filePath	the file path
     * @return			file extension for the specified file path
     */
    public static String getFileExtensionFromPath(String filePath) {
    	if (filePath.contains(".")) {    	
    		return filePath.substring(filePath.lastIndexOf("."));
    	}
    	
    	return filePath;
    }
    
    /**
     * Create and return an open file intent with the specified {@link File}.
     * 
     * @param file	the file
     * @return		the open file intent
     */
    public static Intent getOpenFileIntent(File file) {
    	Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		
		// set data type and start activity
		intent.setDataAndType(Uri.fromFile(file), NativeFileHelper.getMimeTypeForFile(file.getAbsolutePath()));
		
		return intent;
    }
}