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

package com.tribal.mobile.net;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.tribal.mobile.Framework;
import com.tribal.mobile.api.Client;

/**
 * Class to encapsulate a secure and authenticated HttpConnection.
 * Appends an authentication token to an HTTP request parameter.
 * 
 * @author Eduardo S. Nunes and Jon Brasted 
 */
public class AuthHttpConnection extends HttpConnection {

	private static final String salt = "{a0fk04383ruaf98b7a7afg76523}";
	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	/**
	 * Retrieves an instance of {@link AuthHttpConnection} for executing an HTTP GET request.
	 * 
	 * @param url		the url
	 * @param data		the data payload
	 * @param callback	the callback
	 * @param context	the context
	 * @return			an instance of {@link AuthHttpConnection}
	 */
	public static AuthHttpConnection get(String url, String data, Callback callback, Context context) {
		final AuthHttpConnection result = new AuthHttpConnection(HttpMethod.GET, url, data, callback, false, context);
		ConnectionManager.getInstance().push(result);
		return result;
	}

	/**
	 * Retrieves an instance of {@link AuthHttpConnection} for executing an HTTP POST request.
	 * 
	 * @param url				the url
	 * @param data				the data payload
	 * @param callback			the callback
	 * @param includeUsername	whether to include the username or not	
	 * @param context			the context
	 * @return					an instance of {@link AuthHttpConnection}
	 */
	public static AuthHttpConnection post(String url, String data, Callback callback, boolean includeUsername, Context context) {
		final AuthHttpConnection result = new AuthHttpConnection(HttpMethod.POST, url, data, callback, includeUsername, context);
		ConnectionManager.getInstance().push(result);
		return result;
	}

	/**
	 * Invokes {@link #post(String, String, Callback, boolean, Context)} with a value of <code>false</code> for <code>includeUsername</code>.
	 */
	public static AuthHttpConnection post(String url, String data, Callback callback, Context context) {
		return post(url, data, callback, false, context);
	}

	private final MessageDigest messageDigest;
	private final String username;
	private final String hashedUsername;
	private final String password;

	public AuthHttpConnection(HttpMethod method, String url, String data,
			Callback callback, Context context) {
		this(method, url, data, callback, false, context);
	}

	public AuthHttpConnection(HttpMethod method, String url, String data,
			Callback callback, boolean includeUsername, Context context) {
		super(method, url, data, callback, includeUsername, context);
		try {
			messageDigest = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("No Such Algorithm");
		}

		Client client = Framework.getClient();
		username = client.getUserUsername();
		hashedUsername = sha512(client.getUserUsername() + salt);
		password = sha512(client.getUserPassword() + salt);
	}

	/**
	 * Returns the <code>data</code> payload inside a JSON object with the key <code>content</code>. Data payload is inside a JSON object with the key <code>addInfo</code>.
	 */
	@Override
	public String getData() {
		Map<String, Object> map = new TreeMap<String, Object>();

		String data = super.getData();

		try {
			if (!TextUtils.isEmpty(data)) {
				// recreate the data JSON array
				JSONArray dataJSONArray = new JSONArray(data);

				JSONObject tempJSONObject;

				// iterate over the JSON array to find the JSON objects
				for (int index = 0; index < dataJSONArray.length(); index++) {
					tempJSONObject = dataJSONArray.getJSONObject(index);

					if (tempJSONObject != null) {
						if (tempJSONObject.has("addInfo")) {
							Object addInfo = tempJSONObject.get("addInfo");

							JSONArray jsonArray = new JSONArray(
									addInfo.toString());

							tempJSONObject.remove("addInfo");
							tempJSONObject.put("addInfo", jsonArray.toString());
						}
					}
				}
				map.put("\"content\"", dataJSONArray);
			} else {
				map.put("\"content\"", "[]");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		String payload = map.toString();

		// cleanse string of the extra characters the map introduces
		payload = payload.replace("=[", ":[");
		payload = payload.replace("={", ":{");
		payload = payload.replace("}], ", "}],");
		payload = payload.replace("\\\\\\/", "/");

		Log.d("Syncing", payload);

		return payload;
	}

	private String createNonce() {
		String nonce = "";

		for (int index = 0; index < 128; index++) {
			nonce += (int) Math.floor(Math.random() * 10);
		}

		return nonce;
	}

	private int createCreated() {
		return Math.round((System.currentTimeMillis() - Framework.getServer()
				.getServerDelta() / 1000));
	}

	private String createAccessToken(String nonce, int created) {
		return Base64.encodeToString(sha512(nonce + created + password)
				.getBytes(), Base64.NO_WRAP);
	}

	private String sha512(String data) {
		byte[] bytes = messageDigest.digest(data.getBytes());
		return toHex(bytes);
	}

	private String toHex(byte[] buf) {
		char[] chars = new char[2 * buf.length];
		for (int i = 0; i < buf.length; ++i) {
			chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
			chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
		}
		return new String(chars);
	}

	/**
	 * Creates and returns an authorisation header token.
	 */
	@Override
	public String getAuthorisationHeader(boolean includeUsername) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("AuthToken ");

		final String nonce = createNonce();
		final String encodedNonce = Base64.encodeToString(nonce.getBytes(),
				Base64.NO_WRAP | Base64.NO_PADDING);
		final int created = createCreated();
		final String accessToken = createAccessToken(nonce, created);

		if (includeUsername) {
			stringBuilder.append(String.format("UserId=\"%s\", ", username));
		}

		stringBuilder.append(String.format("HashId=\"%s\", ", hashedUsername));
		stringBuilder
				.append(String.format("AccessToken=\"%s\", ", accessToken));
		stringBuilder.append(String.format("Nonce=\"%s\", ", encodedNonce));
		stringBuilder.append(String.format("Created=\"%s\"", created));

		return stringBuilder.toString();
	}
}