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

package com.tribal.omlet;

import com.tribal.omlet.download.DownloadServiceImplementation;
import com.tribal.omlet.framework.OmletFramework;
import com.tribal.omlet.framework.OmletServer;
import com.tribal.omlet.sync.SyncService;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.tribal.mobile.Framework;
import com.tribal.mobile.api.Client;
import com.tribal.mobile.api.login.LoginCompleted;
import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.phonegap.MFSettingsKeys;
import com.tribal.mobile.preferences.PrivateSettingsKeys;
import com.tribal.mobile.util.ContextUtils;
import com.tribal.mobile.util.NativeSettingsHelper;
import com.tribal.mobile.util.ServiceLayerExceptionHelper;

/**
 * Login activity.
 * 
 * @author Jon Brasted
 */
public class LoginActivity extends SystemMenuProviderActivity implements LoginCompleted, View.OnClickListener {
	/* Fields */

	private TextView loginTextView = null;
	private EditText loginAccessCodeEntry = null;
	private Button loginAccessCodeEntryButton = null;
	private ProgressBar progressSpinner = null;
	private TextView buildVersionTextView = null;

	/* Methods */

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// load default preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// set base application isCacheCleared property
		getBaseApplication().setIsCacheCleared(false);

		// create framework
		new OmletFramework((BaseApplication) getApplication(), getDatabaseHelper());

		// instantiate the NativeSettingsHelper
		new NativeSettingsHelper(getApplication());

		Intent downloadServiceIntent = new Intent(this.getApplication(), DownloadServiceImplementation.class);

		// start the sync service
		downloadServiceIntent.setFlags(Service.START_NOT_STICKY);
		startService(downloadServiceIntent);

		// stop the service if it is running
		Intent syncServiceIntent = new Intent(this.getApplication(), SyncService.class);
		stopService(syncServiceIntent);

		// start the sync service
		syncServiceIntent.setFlags(Service.START_NOT_STICKY);
		syncServiceIntent.putExtra(IntentParameterConstants.Url, getString(R.string.syncUrl));
		startService(syncServiceIntent);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		// get UI elements
		loginTextView = (TextView) findViewById(R.id.login_login_text);
		loginAccessCodeEntry = (EditText) findViewById(R.id.login_access_code_entry);
		loginAccessCodeEntryButton = (Button) findViewById(R.id.login_access_code_entry_button);
		progressSpinner = (ProgressBar) findViewById(R.id.login_progress_spinner);
		buildVersionTextView = (TextView) findViewById(R.id.login_build_version_text);

		if (loginAccessCodeEntryButton != null) {
			loginAccessCodeEntryButton.setOnClickListener(this);
		}
		
		updateBuildVersionText();

		checkLoggedInState();

		// Disable Up button for this page
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		getSupportActionBar().setHomeButtonEnabled(false);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Don't display sync icon on login screen
		menu.removeItem(R.id.menu_item_sync);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// unsubscribe to the prelogout broadcast
		if (isBroadcastReceiverActionRegistered(BroadcastActions.PreLogout)) {
			removeBroadcastReceiver(BroadcastActions.PreLogout);
		}
	}
	
	@Override
	public void onClick(View view) {
		int id = view.getId();
		
		switch (id) {
			case R.id.login_access_code_entry_button: {
				login_OnClick();
				break;
			}
		}
	}

	private void updateBuildVersionText() {
		if (buildVersionTextView != null) {
			// get the build version
			String versionName = ContextUtils.getVersionName(getApplicationContext());

			String buildVersionShortFormatString = getString(R.string.buildVersionShortFormatString);
			String buildVersionShort = String.format(buildVersionShortFormatString, versionName);

			buildVersionTextView.setText(buildVersionShort);
		}
	}

	private void checkLoggedInState() {
		// check to see if the user has logged in before
		Object currentUserId;
		boolean skipLoginCheck = false;

		try {
			currentUserId = NativeSettingsHelper.getInstance(getApplicationContext()).checkAndGetNativeSetting(MFSettingsKeys.LAST_LOGGED_IN_USER);

			if (currentUserId != null) {
				Client client = Framework.getClient();

				if (client instanceof OmletFramework) {
					((OmletFramework) client).updateUserUsername(currentUserId.toString());
				}

				if (isNetworkAvailable()) {
					// trigger auto login
					attemptAutoLogin();
				} else {				
					skipLoginCheck = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (skipLoginCheck) {
			goToHome();
		} else {
			boolean hasShownSSLCertificateError = ServiceLayerExceptionHelper.getInstance().getHasShownSSLCertificateError();

			// check if the phone is offline or if there has been an SSL error
			if (!isNetworkAvailable() || hasShownSSLCertificateError) {
				if (hasShownSSLCertificateError) {
					// check to see whether the user has enabled unsigned SSL
					// certificates
					boolean isAcceptSelfSignedCertificatesEnabled = NativeSettingsHelper.getInstance(this).checkAndGetPrivateBooleanSetting(
							PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS, false);

					if (isAcceptSelfSignedCertificatesEnabled) {
						// initialise login
						initialiseLogin();

						return;
					}
				}

				// show an error message
				showLoginUnavailableMessage();
			} else {
				// initialise login
				initialiseLogin();
			}
		}
	}

	private void attemptAutoLogin() {
		initialiseLogin();
		
		// user id
		String userId = Framework.getClient().getUserUsername();
		
		loginAccessCodeEntry.setText(userId);
		
		login_OnClick();
	}
	
	private void showLoginUnavailableMessage() {
		loginTextView.setText(R.string.login_text_no_connection);

		loginAccessCodeEntryButton.setEnabled(false);

		loginAccessCodeEntry.setVisibility(View.INVISIBLE);
		loginAccessCodeEntryButton.setVisibility(View.INVISIBLE);
	}

	private void initialiseLogin() {
		// set the login access code entry field to password
		loginAccessCodeEntry.setTransformationMethod(PasswordTransformationMethod.getInstance());

		loginAccessCodeEntry.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (loginAccessCodeEntry.length() > 0
						&& !loginAccessCodeEntryButton.isEnabled()) {
					loginAccessCodeEntryButton.setEnabled(true);
				} else if (loginAccessCodeEntry.length() == 0) {
					loginAccessCodeEntryButton.setEnabled(false);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});

		loginTextView.setText(R.string.login_text);
		loginAccessCodeEntryButton.setEnabled(true);

		loginAccessCodeEntry.setVisibility(View.VISIBLE);
		loginAccessCodeEntryButton.setVisibility(View.VISIBLE);
	}

	public void login_OnClick() {
		// get the user name
		try {
			// show loading spinner
			progressSpinner.setVisibility(View.VISIBLE);

			// disable the login entry field
			loginAccessCodeEntry.setEnabled(false);

			// disable the login button
			loginAccessCodeEntryButton.setEnabled(false);

			final String pinCode = loginAccessCodeEntry.getText().toString();

			// get hardcoded password
			String passcode = getString(R.string.login_hardcoded_passcode);

			// call authenticate method
			OmletServer server = (OmletServer) Framework.getServer();

			server.login(pinCode, passcode, this);

		} catch (Exception e) {
			// hide loading spinner
			progressSpinner.setVisibility(View.INVISIBLE);

			// re-enable the login entry field
			loginAccessCodeEntry.setEnabled(true);

			// re-enable the login button
			loginAccessCodeEntryButton.setEnabled(true);

			loginTextView.setText(R.string.login_text_error_with_connection);
		}
	}

	@Override
	public void onLoginCompleted(boolean authenticated) {
		// reenable the login button
		loginAccessCodeEntryButton.setEnabled(true);

		// hide loading spinner
		progressSpinner.setVisibility(View.INVISIBLE);

		// do something with the results
		if (authenticated) {
			// update persisted user ID
			try {
				NativeSettingsHelper.getInstance(getApplicationContext()).setPreferenceValue(MFSettingsKeys.LAST_LOGGED_IN_USER, Framework.getClient().getUserUsername());
			} catch (Exception e) {
				e.printStackTrace();
			}

			// start the rest of the loading
			goToHome();
		} else {
			// re-enable the login entry field
			loginAccessCodeEntry.setEnabled(true);

			// set the text
			loginTextView.setText(R.string.login_text_error);
		}
	}

	private void goToHome() {
		// navigate to the home activity
		navigate(this, MainTabActivity.class, null, null);
	}

	@Override
	protected void onBroadcastReceiveOverride(Intent intent) {
		String intentAction = intent.getAction();

		if (BroadcastActions.SharedPreferenceChanged.equalsIgnoreCase(intentAction)) {
			if (intent.hasExtra(IntentParameterConstants.Key) && intent.hasExtra(IntentParameterConstants.Value)) {
				String sharedPreferenceKey = intent.getStringExtra(IntentParameterConstants.Key);
				String sharedPreferenceValue = intent.getStringExtra(IntentParameterConstants.Value);

				if (PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS.equalsIgnoreCase(sharedPreferenceKey)) {
					// get boolean value
					boolean booleanValue = Boolean.parseBoolean(sharedPreferenceValue);

					if (ServiceLayerExceptionHelper.getInstance().getHasShownSSLCertificateError()) {
						if (booleanValue) {
							// initialise login
							initialiseLogin();
						} else {
							// show the error message
							showLoginUnavailableMessage();
						}
					}
				}
			}
		}
	}
}