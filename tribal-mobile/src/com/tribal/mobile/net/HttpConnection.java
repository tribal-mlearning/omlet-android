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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStore;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;

import com.tribal.mobile.preferences.PrivateSettingsKeys;
import com.tribal.mobile.util.NativeSettingsHelper;

/**
 * Implementation based on http://masl.cis.gvsu.edu/2010/04/05/android-code-sample-asynchronous-http-connections/
 * 
 * @author Eduardo S. Nunes and Jon Brasted 
 */
public class HttpConnection implements Runnable {

	public interface Callback {

		void onStart();

		void onSuccess(String data);

		void onError(Throwable t);

	}

	public static HttpConnection get(String url, String data,
			Callback callback, Context context) {
		final HttpConnection result = new HttpConnection(HttpMethod.GET, url,
				data, callback, false, context);
		ConnectionManager.getInstance().push(result);
		return result;
	}

	public static HttpConnection post(String url, String data,
			Callback callback, Context context) {
		return post(url, data, callback, false, context);
	}

	public static HttpConnection post(String url, String data,
			Callback callback, boolean includeUsername, Context context) {
		final HttpConnection result = new HttpConnection(HttpMethod.POST, url,
				data, callback, includeUsername, context);
		ConnectionManager.getInstance().push(result);
		return result;
	}

	private final HttpMethod method;
	private final String url;
	private final Callback callback;
	private final String data;
	private final boolean includeUsername;
	private final Context context;

	private static final String authorisationHeaderName = "X-AUTH";

	private HttpClient httpClient;

	private ClientConnectionManager connectionManager;

	private static final BasicHttpParams httpParams = new BasicHttpParams();
	private static final SchemeRegistry supportedSchemes = new SchemeRegistry();

	public HttpConnection(HttpMethod method, String url, String data,
			Callback callback, Context context) {
		this(method, url, data, callback, false, context);
	}

	public HttpConnection(HttpMethod method, String url, String data,
			Callback callback, boolean includeUsername, Context context) {
		this.method = method;
		this.url = url;
		this.data = data;
		this.callback = callback;
		this.includeUsername = includeUsername;
		this.context = context;

		httpParams.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 1);
		httpParams.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(1));
		httpParams.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
		ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
		HttpProtocolParams.setVersion(httpParams, protocolVersion);
		HttpProtocolParams.setContentCharset(httpParams, "utf8");
		supportedSchemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	}

	protected String getData() {
		return data;
	}

	@Override
	public void run() {
		callback.onStart();

		checkSSLSelfSignedCertificatePreference();

		httpClient = new DefaultHttpClient(connectionManager, httpParams);

		HttpConnectionParams.setSoTimeout(httpClient.getParams(), 25000);
		try {
			HttpResponse response = null;
			switch (method) {
				case GET:
					HttpGet httpGet = new HttpGet(url);

					// add header
					httpGet.addHeader(authorisationHeaderName,
							getAuthorisationHeader(includeUsername));

					response = httpClient.execute(httpGet);
					break;
				case POST:
					HttpPost httpPost = new HttpPost(url);

					// add header
					httpPost.addHeader("Content-Type", "application/json");
					httpPost.addHeader(authorisationHeaderName, getAuthorisationHeader(includeUsername));
					httpPost.setEntity(new StringEntity(getData()));
					response = httpClient.execute(httpPost);
					break;
			}
			processEntity(response.getEntity());
		} catch (Throwable t) {
			callback.onError(t);
		}
	}

	private void checkSSLSelfSignedCertificatePreference() {
		final boolean acceptSelfSignedCertificates = NativeSettingsHelper.getInstance(context).checkAndGetPrivateBooleanSetting(PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS,
				false);

		if (acceptSelfSignedCertificates) {
			supportedSchemes.register(new Scheme("https", TrustAllSSLSocketFactory.getSocketFactory(), 443));
		} else {
			supportedSchemes.register(new Scheme("https", createNewTrustedMLearningSSLSocketFactory(context), 443));
		}

		connectionManager = new ThreadSafeClientConnManager(httpParams, supportedSchemes);
	}
	
	public SSLSocketFactory createNewTrustedMLearningSSLSocketFactory(Context context) {
		try {
			// Get an instance of the Bouncy Castle KeyStore format
			KeyStore trusted = KeyStore.getInstance("BKS");

			// Get the raw resource, which contains the keystore with
			// your trusted certificates (root and any intermediate certs)
			//InputStream in = context.getResources().openRawResource(R.raw.mlearningkeystore);

			/*try {
				// Initialize the keystore with the provided trusted
				// certificates
				// Also provide the password of the keystore
				trusted.load(in, "secretpassword".toCharArray());
			} finally {
				in.close();
			}*/

			// Pass the keystore to the SSLSocketFactory. The factory is
			// responsible
			// for the verification of the server certificate.
			//SSLSocketFactory sf = new MLearningSSLSocketFactory(trusted); // SSLSocketFactory(trusted);
			SSLSocketFactory sf = new SSLSocketFactory(trusted);

			// Hostname verification from certificate
			// http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
			sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
			return sf;
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void processEntity(HttpEntity entity) throws IllegalStateException, IOException {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
		final StringBuilder total = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			total.append(line);
		}
		callback.onSuccess(total.toString());
	}

	/**
	 * Override method for retrieving the authorization header. 
	 * 
	 * @param includeUsername	include the plane text username in the HTTP request message 
	 * @return					the authorization header
	 */
	protected String getAuthorisationHeader(boolean includeUsername) {
		return null;
	}
}