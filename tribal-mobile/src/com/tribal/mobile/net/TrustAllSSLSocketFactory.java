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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

/**
 * Class that represents an all trusting SSL Socket Factory.
 * Taken from http://stackoverflow.com/a/5793865
 */
final class TrustAllSSLSocketFactory implements LayeredSocketFactory {
	/* Fields */
	
	private static final TrustAllSSLSocketFactory DEFAULT_FACTORY = new TrustAllSSLSocketFactory();

	private SSLContext sslcontext;
	private javax.net.ssl.SSLSocketFactory socketfactory;

	private String LOG_TAG = "TrustAllSSLSocketFactory";
	
	/* Properties */
	
	public static TrustAllSSLSocketFactory getSocketFactory() {
		return DEFAULT_FACTORY;
	}

	/* Constructor */
	
	private TrustAllSSLSocketFactory() {
		super();
		TrustManager[] tm = new TrustManager[] { new X509TrustManager() {

			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		} };
		try {
			this.sslcontext = SSLContext.getInstance(SSLSocketFactory.TLS);
			this.sslcontext.init(null, tm, new SecureRandom());
			this.socketfactory = this.sslcontext.getSocketFactory();
		} catch (NoSuchAlgorithmException e) {
			Log.e(LOG_TAG, "Failed to instantiate TrustAllSSLSocketFactory!", e);
		} catch (KeyManagementException e) {
			Log.e(LOG_TAG, "Failed to instantiate TrustAllSSLSocketFactory!", e);
		}
	}

	@Override
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
		SSLSocket sslSocket = (SSLSocket) this.socketfactory.createSocket(socket, host, port, autoClose);
		return sslSocket;
	}

	@Override
	public Socket connectSocket(Socket sock, String host, int port,
			InetAddress localAddress, int localPort, HttpParams params)
			throws IOException, UnknownHostException, ConnectTimeoutException {
		if (host == null) {
			throw new IllegalArgumentException("Target host may not be null.");
		}
		if (params == null) {
			throw new IllegalArgumentException("Parameters may not be null.");
		}

		SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket());

		if ((localAddress != null) || (localPort > 0)) {

			// we need to bind explicitly
			if (localPort < 0) {
				localPort = 0; // indicates "any"
			}

			InetSocketAddress isa = new InetSocketAddress(localAddress,
					localPort);
			sslsock.bind(isa);
		}

		int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
		int soTimeout = HttpConnectionParams.getSoTimeout(params);

		InetSocketAddress remoteAddress;
		remoteAddress = new InetSocketAddress(host, port);

		sslsock.connect(remoteAddress, connTimeout);

		sslsock.setSoTimeout(soTimeout);

		return sslsock;
	}

	@Override
	public Socket createSocket() throws IOException {
		// the cast makes sure that the factory is working as expected
		return this.socketfactory.createSocket();
	}

	@Override
	public boolean isSecure(Socket sock) throws IllegalArgumentException {
		return true;
	}
}