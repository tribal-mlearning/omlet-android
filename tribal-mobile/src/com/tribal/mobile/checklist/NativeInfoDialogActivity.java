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

package com.tribal.mobile.checklist;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tribal.mobile.R;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.util.MeasurementUtils;

/**
 * Class to represent a native 'info' dialog activity.
 * Class shows information based on the content of the <code>JsonArrayParameters</code> intent parameter.
 * 	* Title text
 * 	* Description text
 * 	* One of more images
 * 	* One or more text links
 * 
 * @author Jon Brasted
 */
public class NativeInfoDialogActivity extends Activity implements View.OnClickListener {
	/* Methods */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set content view
		setContentView(R.layout.checklist_info_dialog);

		// get phonegap callback id
		Intent intent = getIntent();

		if (intent.hasExtra(IntentParameterConstants.JsonArrayParameters)) {
			String jsonArrayParameters = intent.getStringExtra(IntentParameterConstants.JsonArrayParameters);
			processParameters(jsonArrayParameters);
		}
		
		// get close button
		Button closeButton = (Button)findViewById(R.id.close_button);
		
		if (closeButton != null) {
			closeButton.setOnClickListener(this);
		}
	}
	
	@Override
	public void onClick(View view) {
		int id = view.getId();
		
		if (R.id.close_button == id) {
			onCloseButtonClick();
		}
	}
	
	private void onCloseButtonClick() {
		finish();
	}
	
	private void processParameters(String jsonArrayParameters) {
		// try and create a JSON array
		try {
			JSONArray jsonArray = new JSONArray(jsonArrayParameters);

			if (jsonArray.length() == 1) {
				JSONObject jsonObject = jsonArray.getJSONObject(0);

				// get title
				String title = jsonObject.getString("title");
				String description = jsonObject.getString("description");

				// set title and description
				TextView titleTextView = (TextView)findViewById(R.id.checklist_info_dialog_title);

				if (titleTextView != null) {
					titleTextView.setText(title);
				}

				TextView descriptionTextView = (TextView) findViewById(R.id.checklist_info_dialog_description);

				if (descriptionTextView != null) {
					descriptionTextView.setText(description);
				}

				if (jsonObject.has("images")) {
					JSONArray images = jsonObject.getJSONArray("images");
					addImages(images);
				}

				if (jsonObject.has("links")) {
					JSONArray links = jsonObject.getJSONArray("links");
					addLinks(links);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/*
	 * A future enhancement could be that the images do not come from Android's assets folder but instead come from external storage.
	 */
	private void addImages(JSONArray images) {
		try {
			if (images != null) {
				// get container
				LinearLayout checklistInfoDialogContentPlaceholder = (LinearLayout)findViewById(R.id.checklist_info_dialog_content_placeholder);

				int topMarginInPixels = MeasurementUtils.convertDipToPixels(getResources(), 10);
				
				// get the android content format string
				String assetPathFormatString = "file:///android_asset/%s";

				if (checklistInfoDialogContentPlaceholder != null) {
					String imagePath;

					for (int index = 0, imagesSize = images.length(); index < imagesSize; index++) {
						try {
							imagePath = images.getString(index);

							// create image view
							ImageView image = new ImageView(this);

							// get the image path
							imagePath = String.format(assetPathFormatString, imagePath);

							// open up a bitmap stream
							InputStream bitmapInputStream = this.getResources().getAssets().open(imagePath);
							Bitmap bitmap = BitmapFactory.decodeStream(bitmapInputStream);
							image.setImageBitmap(bitmap);

							// add image view to the container
							LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
							layoutParams.topMargin = topMarginInPixels;
							checklistInfoDialogContentPlaceholder.addView(image, layoutParams);
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void addLinks(JSONArray links) {
		if (links != null) {
			// get container
			LinearLayout checklistInfoDialogContentPlaceholder = (LinearLayout) findViewById(R.id.checklist_info_dialog_content_placeholder);

			int topMarginInPixels = MeasurementUtils.convertDipToPixels(getResources(), 10);

			String title;
			String url;

			for (int index = 0, linksSize = links.length(); index < linksSize; index++) {
				try {
					Object obj = links.get(index);

					if (obj instanceof JSONObject) {
						JSONObject jsonObject = (JSONObject) obj;

						if (jsonObject.has("title") && jsonObject.has("url")) {
							title = jsonObject.getString("title");
							url = jsonObject.getString("url");

							TextView linkTextView = new TextView(this, null, R.style.checklist_info_dialog_text_style);

							SpannableString titleSpannableString = new SpannableString(title);
							titleSpannableString.setSpan(new UnderlineSpan(), 0, titleSpannableString.length(), 0);
							linkTextView.setText(titleSpannableString);

							linkTextView.setOnClickListener(new TextLinkClickListener(url));

							// add link view to the container
							LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
							layoutParams.topMargin = topMarginInPixels;
							checklistInfoDialogContentPlaceholder.addView(linkTextView, layoutParams);
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class TextLinkClickListener implements OnClickListener {
		/* Fields */

		private String url;

		/* Constructor */

		public TextLinkClickListener(String url) {
			this.url = url;
		}

		/* Methods */

		@Override
		public void onClick(View paramView) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			startActivity(intent);
		}
	}
}