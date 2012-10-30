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

package com.tribal.mobile.phonegap;

/**
 * Represents the PhoneGap client plugin.
 * 
 * @author Eduardo S. Nunes and Jon Brasted
 */
public interface ClientPlugin {

	/**
	 * Opens a native menu on the application.
	 *
	 * @param menuItemId The full path to the menu item, starting with the package id. For example: shell.root.1.2
	 */
	void openMenuItem(String menuItemId, String callback);
	
	/**
	 * Opens a resource on the application.
	 * The resource called can be contained in the current package or another package, but is referenced with its full name in both cases.
	 *
	 * @param menuItemId The full path to the resource, starting with the package id. For example: shell.root.1.2
	 */		
	void openResource(String resourceId, String callback);
	
	/**
	 * Sets a value for a key on the native storage. Native storage is provided by the framework. It consists in a way to store key/value pairs. Like the HTML5 DB, this kind of storage is cleaned up automatically if the user removes the application. This storage should be used specially if you want to share data between the HTML content and native side of the application, otherwise we highly recommend use HTML5 DB. There are two ways of store data on the native storage, globally and content-specific.
	 * <p>
	 *   <strong>Globally</strong>
	 *   It's global on the application. The key can be retrieved from any content in any part of the system, as well as in any part of the native code. You must take care that the key can be used by other content developer. Below you can find the API calls to store and retrieve key/value pairs.
	 *   <ul>
	 *     <li>clientPlugin.setValue(MFStoreType.GLOBAL, key, value, callback);</li>
	 *   </ul>
	 * </p>
	 * <p>
	 *   <strong>Content-specific</strong>
	 *   It's connected to the current resource/menu-item. Below you can find the API calls to store and retrieve key/value pairs.
	 *   <ul>
	 *     <li>clientPlugin.setValue(MFStoreType.SPECIFIC, key, value, callback);</li>
	 *   </ul>
	 * </p>
	 * 
	 * @param type		The type of the native storage.
	 * @param key		The key name.
	 * @param value		The value.
	 * @param callback	The callback function to confirm the operation.
	 */
	void setValue(String type, String key, String value, String callback);
	
	/**
	 * Gets a value for a key on the native storage. Native storage is provided by the framework. It consists in a way to store key/value pairs. Like the HTML5 DB, this kind of storage is cleaned up automatically if the user removes the application. This storage should be used specially if you want to share data between the HTML content and native side of the application, otherwise we highly recommend use HTML5 DB. There are two ways of store data on the native storage, globally and content-specific.
	 *
	 * <p>
	 *   <strong>Globally</strong>
	 * It's global on the application. The key can be retrieved from any content in any part of the system, as well as in any part of the native code. You must take care that the key can be used by other content developer. Below you can find the API calls to store and retrieve key/value pairs.
	 *   <ul>
	 *     <li>clientPlugin.getValue(MFStoreType.GLOBAL, key, callback);</li>
	 *   </ul>
	 * </p>
	 * <p>
	 *   <strong>Content-specific</strong>
	 * It's connected to the current resource/menu-item. Below you can find the API calls to store and retrieve key/value pairs.
	 *   <ul>
	 *     <li>clientPlugin.getValue(MFStoreType.SPECIFIC, key, callback);</li>
	 *   </ul>
	 * </p>
	 * 
	 * @param type		The type of the native storage.
	 * @param key		The key name.
	 * @param callback	The callback function. It will receive an object with the value.
	 */		
	void getValue(String type, String key, String callback);
	
	/**
	 * Retrieves the current user username.
	 *
	 * @param callback	The callback function. It will receive an object with the username.
	 */
	void getUserUsername(String callback);

	/**
	 * Forces sync.
	 *
	 * @param callback	The callback function.
	 */
	void sync(String callback);

	/**
	 * Tracks a content-specific thing. This method must be called by content developers to track anything they want. Everything tracked by this method will be connected to the current object id (resource or menu-item).
	 *
	 * @param sender			The sender. This will be 'mf' for internal mobile framework calls. It's a string value.
	 * @param additionalInfo	The track information. It's a string value, can contain anything you want.
	 * @param callback			The callback function. It will receive an object with the device information.
	 */
	void track(String sender, String additionalinfo, String callback);
	
	/**
	 * Log out the current user and redirect the user to the login screen.
	 * 
	 * @param callback	The callback function.
	 */
	void logout(String callback);
	
	/**
	 * Retrieves the local path root for a particular course.
	 * 
	 * A success PhoneGap callback will return a JSON object with the following key and value pairs:
	 * 	courseId		The course id specified.
	 *  localPathRoot	The full local path to the course directory root. E.g. /mnt/sdcard/.../{uniqueCourseFolderId}
	 * 
	 * An error PhoneGap callback will return a JSON object with the following key and value pairs:
	 *  courseId		The course id specified.
	 *  error			An error string to determine why the native method call failed. 
	 * 
	 * @param courseId		The course id.
	 * @param callback		The callback function.
	 */
	void getCourseLocalPathRoot(String courseId, String callback);
	
	/**
	 * Retrieves the local path root for the currently opened course.
	 * 
	 * Callback will return a JSON object with the following key and value pairs:
	 *  localPathRoot	The full local path to the course directory root. E.g. /mnt/sdcard/.../{uniqueCourseFolderId}
	 * 
	 * Error callback will return a JSON object with the following key and value pairs:
	 *  error			An error string to determine why the native method call failed.
	 * 
	 * @param callback		The callback function.
	 */
	void getCurrentCourseLocalPathRoot(String callback);
	
	/**
	 * Initialises a temporary folder for the currently opened course.
	 * If the folder already exists, the folder will be cleared. If it does not exist, it will be created.
	 * 
	 * Callback will return a JSON object with the following key and value pairs:
	 * 	tempFolderPath	The full local path to the temporary folder in the course directory. E.g. /mnt/sdcard/.../{uniqueCourseFolderId}/temp
	 * 
	 * Error callback will return a JSON object with the following key and value pairs:
	 *  error			An error string to determine why the native method call failed.
	 * 
	 * @param callback		The callback function.
	 * @param errorCallback	The error callback function.
	 */
	void initialiseCurrentCourseLocalTempFolder(String callback);
	
	/**
	 * Clears the temporary folder for the currently opened course.
	 * If the folder exists, the folder will be cleared and removed. If the folder does not exist, an error will occur.
	 * 
	 * Callback will not return a value and will be invoked when the operation has finished.
	 * 
	 * Error callback will return a JSON object with the following key and value pairs:
	 *  error			An error string to determine why the native method call failed.
	 * 
	 * @param callback		The callback function.
	 * @param errorCallback	The error callback function.
	 */
	void clearCurrentCourseLocalTempFolder(String callback);
}