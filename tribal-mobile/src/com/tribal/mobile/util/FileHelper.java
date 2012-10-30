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

/**
 * Utility class to help faciltiate working with files.
 * 
 * @author	Jon Brasted and Jack Kierney
 */
public class FileHelper {
    /**
     * Returns file name from specified file path.
     * 
     * @param filePath	the file path
     * @return			file name from specfied file path
     */
	public static String getFileNameFromPath(String filePath) {
    	if (filePath.contains("/")) {
    		int lastIndexOfSlash = filePath.lastIndexOf("/");
    		
    		return filePath.substring(lastIndexOfSlash + 1, filePath.length());
    	}
    	else if (filePath.contains("\\")) {
    		int lastIndexOfSlash = filePath.lastIndexOf("\\");
    		
    		return filePath.substring(lastIndexOfSlash + 1, filePath.length());
    	}
    	
    	return filePath;
    }
	
	/**
	 * Returns a file path string without the file extension.
	 * 
	 * @param filePath	the file path string
	 * @return			a file path string without the file extension
	 */
	public static String removeExtensionFromPath(String filePath) {
		if (filePath.contains(".")) {
			int lastIndexOfPeriod = filePath.lastIndexOf(".");
			
			return filePath.substring(0, lastIndexOfPeriod);
		}
		
		return filePath;
	}
	
	/**
	 * Asynchronously deletes a directory recursively.
	 * 
	 * @param dir		the directory
	 * @param payload	the payload to pass to the {@link DeleteDirectoryCompleted} callback
	 * @param callback	the {@link DeleteDirectoryCompleted} callback
	 */
	public static void deleteDirRecursiveAsync(final File dir, final Object payload, final DeleteDirectoryCompleted callback) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				boolean result = deleteFilesRecursive(dir);
				callback.onDeleteDirectoryCompleted(result, payload);
			}
		};
		
		Thread thread = new Thread(runnable);
		thread.start();
	}
	
	/**
	 * Deletes all files in a file or directory but not the file or directory itself.
	 * 
	 * @param fileOrDirectory	the file or directory
	 * @return					whether the operation was successful or not
	 */
	public static boolean deleteFilesRecursive(File fileOrDirectory) {
		return deleteFilesRecursive(fileOrDirectory, false);
	}
	
	/**
	 * Deletes all files in a file or directory and the file or directory itself. 
	 * 
	 * @param fileOrDirectory	the file or directory
	 * @param includeSelf		whether to delete file or directory itself
	 * @return					whether the operation was successful or not
	 */
	public static boolean deleteFilesRecursive(File fileOrDirectory, boolean includeSelf) {
		boolean success = false;
		
		if (fileOrDirectory.isDirectory()) {
			for (File child : fileOrDirectory.listFiles()) {
				success = success | deleteFilesRecursive(child, true);
			}
		}
		
		if (includeSelf) {
			success = success | fileOrDirectory.delete();
		}
		
		return success;
	}
}