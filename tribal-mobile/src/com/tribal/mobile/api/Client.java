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

package com.tribal.mobile.api;

import com.tribal.mobile.api.packages.PackageCatalogueRetrieved;
import com.tribal.mobile.model.BaseContentItem;
import com.tribal.mobile.model.MenuItem;
import com.tribal.mobile.phonegap.GetCourseLocalPathRootCompleted;
import com.tribal.mobile.phonegap.InitialiseCurrentCourseLocalTempFolderCompleted;
import com.tribal.mobile.phonegap.PhoneGapOperationNoResultCompleted;

/**
 * Native client API to deal with the mobile framework.
 * 
 * @author Eduardo S. Nunes and Jon Brasted
 * 
 */
public interface Client {

	/**
	 * Provides the ID of the current user. In case the application doesn't have
	 * authentication, a static ID must be defined.
	 * 
	 * @return the ID of the current user.
	 */
	String getUserId();

	/**
	 * Provides the username of the current user. In case the application
	 * doesn't have authentication, a static ID must be defined.
	 * 
	 * @return the username of the current user.
	 */
	String getUserUsername();

	/**
	 * Provides the password of the current user. In case the application
	 * doesn't have authentication, a static password must be defined.
	 * 
	 * @return the password of the current user.
	 */
	String getUserPassword();
	
	/**
	 * Provides the password hash of the current user.
	 * 
	 * @return the password hash of the current user.
	 */
	String getUserPasswordHash();
	
	/**
	 * Sets the password hash of the current user
	 * 
	 * @param passwordHash the password hash of the current user.
	 */
	void setUserPasswordHash(String passwordHash);

	/**
	 * Sets the username of the current user
	 * 
	 * @param username the username of the current user.
	 */
	void setUserUsername(String username);

	/**
	 * Provides the ability to clear the user ID, username and user password.
	 */
	void logout();
	
	/**
	 * Provides the ability to track the current menu item.
	 */
	void setMenuItem(MenuItem menuItem);
	
	/**
	 * Provides the ability to track the current resource item.
	 */
	void setResourceItem(BaseContentItem resourceItem);
	
	/**
	 * Provides the ability to open a resource.
	 */
	void openResource(String resourcePath);
	
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
	 */
	String getValue(String type, String key);
	
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
	 */
	boolean setValue(String type, String key, String value);

	/**
	 * Forces a sync. This method should be only used when user has set
	 * {@link SyncType} to MANUAL.
	 * 
	 */
	void sync();

	/**
	 * Tracks the access to the current object. The time spent on this object is
	 * calculated between calls to this method.
	 * 
	 * @param sender			The sender. This will be 'mf' for internal mobile framework calls. It's a string value.
	 * @param additionalInfo 	Optional additional information. This information must be used by content developers to track internal pages.
	 */
	void track(String sender, String additionalInfo);
	
	/**
	 * Tracks some information.
	 * 
	 * @param sender			The sender. This will be 'mf' for internal mobile framework calls. It's a string value.
	 * @param objectId			ObjectId.
	 * @param additionalInfo 	Optional additional information. This information must be used by content developers to track internal pages.
	 */
	void track(String sender, String objectId, String additionalInfo);

	/**
	 * Provides the list of all available packages, installed or not.
	 * 
	 * @param callback
	 *            the list of all available packages.
	 */
	void getPackageCatalogue(PackageCatalogueRetrieved callback);
	
	/**
	 * Retrieves the local path root for a particular course.
	 * 
	 * @param courseId			The course id.
	 * @param phoneGapCallback	The PhoneGap callback.
	 * @param callback			The callback function. 
	 */
	void getCourseLocalPathRoot(String courseId, String phoneGapCallback, GetCourseLocalPathRootCompleted callback);
	
	/**
	 * Retrieves the local path root for the currently opened course.
	 * 
	 * @param phoneGapCallback	The PhoneGap callback.
	 * @param callback			The callback function. 
	 */
	void getCurrentCourseLocalPathRoot(String phoneGapCallback, GetCourseLocalPathRootCompleted callback);
	
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
	 * @param phoneGapCallback	The PhoneGap callback.
	 * @param callback			The callback function. 
	 */
	void initialiseCurrentCourseLocalTempFolder(String phoneGapCallback, InitialiseCurrentCourseLocalTempFolderCompleted callback);
	
	/**
	 * Clears the temporary folder for the currently opened course.
	 * If the folder exists, the folder will be cleared and removed. If the folder does not exist, an error will occur.
	 * 
	 * Callback will not return a value and will be invoked when the operation has finished.
	 * 
	 * Error callback will return a JSON object with the following key and value pairs:
	 *  error			An error string to determine why the native method call failed.
	 * 
	 * @param phoneGapCallback	The PhoneGap callback.
	 * @param callback			The callback function.
	 */
	void clearCurrentCourseLocalTempFolder(String phoneGapCallback, PhoneGapOperationNoResultCompleted callback);
}