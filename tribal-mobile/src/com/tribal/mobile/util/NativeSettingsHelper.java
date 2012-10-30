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

import android.content.Context;
import android.text.TextUtils;

import com.tribal.mobile.preferences.PrivateSettingsKeys;

/**
 * Utility class to retrieve native preference values, specifically the keys in {@link PrivateSettingsKeys}.
 *
 * @author Jon Brasted
 */
public class NativeSettingsHelper extends BaseNativeSettingsHelper {
	private static NativeSettingsHelper instance;

	public static NativeSettingsHelper getInstance(Context context) {
		if (instance == null) {		
			instance = new NativeSettingsHelper(context);
		}
		
		return instance;
	}
	
	public NativeSettingsHelper(Context context) {
		super(context);
		NativeSettingsHelper.instance = this;
	}
	
	/**
	 * Retrieves a native setting. The permitted values for <code>key</code> are as follows:
	 * <p>
	 * 	<code>PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS</code><br />
	 * 	<code>PrivateSettingsKeys.USER_PASSWORD_HASH</code>
	 * </p>
	 * 
	 * @param key	the key
	 * @return		the value or null
	 */	
	public String checkAndGetPrivateSetting(String key) {
		// check the predefined list
		if (PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS.equalsIgnoreCase(key)) {
			return getPreferenceStringValue(PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS);
		} else if (PrivateSettingsKeys.USER_PASSWORD_HASH.equalsIgnoreCase(key)) {
			return getPreferenceStringValue(PrivateSettingsKeys.USER_PASSWORD_HASH);
		}
		
		return null;
	}
	
	/**
	 * Retrieves a native setting by invoking {@link #checkAndGetPrivateSetting(String)}. In addition to that, method will return
	 * <code>defaultValue</code> if a null or empty String is returned from {@link #checkAndGetPrivateSetting(String)}.
	 * 
	 * @param key	the key
	 * @return		the value or null
	 */	
	public String checkAndGetPrivateSetting(String key, String defaultValue) {
		String value = checkAndGetPrivateSetting(key);
		
		if (TextUtils.isEmpty(value)) {
			return defaultValue;
		}
		
		return value;
	}
	
	/**
	 * Retrieves a boolean value for a given key.
	 * 
	 * @param preferenceKey		the preference key
	 * @return					the boolean value for the given key
	 */
	public boolean checkAndGetPrivateBooleanSetting(String key, boolean defaultValue) {
		String stringValue = null;
		
		if (PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS.equalsIgnoreCase(key)) {
			stringValue = getPreferenceStringValue(PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS);
		}
		
		if (stringValue != null) {
			return Boolean.parseBoolean(stringValue);
		}
		
		return defaultValue;
	}
}