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
 * Object to hold the available user settings keys.
 */
public class MFSettingsKeys {
	/**
	 * User settings: is tracking enabled.
	 * @const
	 * @type {string}
	 */
	public final static String TRACKING_ENABLED = "app_tracking_enabled";
	
	/**
	 * User settings: the tracking interval related to gps information.
	 * @const
	 * @type {string}
	 */
	public final static String GPS_INTERVAL = "app_gps_interval";
	
	/**
	 * User settings: the sync interval.
	 * @const
	 * @type {string}
	 */
	public final static String SYNC_INTERVAL = "app_sync_interval";
	
	/**
	 * User settings: the enabled data use setting (connectivity).
	 * @const
	 * @type {string}
	 */
	public final static String DATA_USE = "app_data_use";
	
	/**
	 * User settings: the application version.
	 * @const
	 * @type {string}
	 */
	public final static String VERSION = "app_version";
	
	/**
	 * Last logged in user.
	 * NOTE: Only to be used by login process. Not to be manually retrieved or updated.
	 * @const
	 * @type {string}
	 */
	public final static String LAST_LOGGED_IN_USER = "last_logged_in_user";
	
	/**
	 * Last synchronised time.
	 * NOTE: Only to be used by sync process. Not to be manually retrieved or updated.
	 * @const
	 * @type {string}
	 */
	public final static String LAST_SYNCHRONISED_TIME = "last_sychronised_time";
	
	/**
	 * User settings: is tracking enabled.
	 * @const
	 * @type {string}
	 */
	public final static String TRANSLATION_ENABLED = "app_translation_enabled";
}