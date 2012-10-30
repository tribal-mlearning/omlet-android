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

import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * URL connection utilities class.
 * 
 * @author Jon Brasted
 */
public class URLConnectionUtils {
	/* Fields */
	
	// always verify the host - dont check for certificate
	/**
	 * {@link HostnameVerifier} that does not verify the host.
	 */
	public static final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        @Override
		public boolean verify(String hostname, SSLSession session) {
            return true;
        }
	};
	
	/**
	 * Method for enabling connections to servers that present untrusted SSL certificates.
	 */
	public static void enableSelfSignedSSLCertificates() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                	return new java.security.cert.X509Certificate[] {};
                }

                @Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * (Re)set the default SSL socket factory.
	 */
	public static void enableSSLCertificateCheck() {
		// Create a trust manager that validates against known certificates
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[] {};
                }

                @Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                	// check to see if the chain contains an item
					if (chain != null && chain.length > 0) {
						// get first certificate
						Principal subjectDN = chain[0].getSubjectDN();
						
						String name = subjectDN.getName(); 
						
						// if Android does not understand the certificate, we might need to do a manual check
						if (!name.equals("CN=*.company.net,O=Company Name,OU=Company,L=Location,ST=State,C=COUNTRY")) {
							throw new CertificateException("Server is not trusted.");
						}
					}
                }
        } };

        // Install the trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}