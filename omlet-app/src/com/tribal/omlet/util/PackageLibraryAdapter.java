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

package com.tribal.omlet.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import com.tribal.omlet.R;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.tribal.mobile.download.DownloadBroadcastActions;
import com.tribal.mobile.util.CustomCursorAdapter;
import com.tribal.mobile.util.ViewHelper;

/**
 * Package Library Adapter.
 * 
 * @author Jon Brasted
 */
public class PackageLibraryAdapter extends CustomCursorAdapter {
	/* Fields */
	
	private ArrayList<String> selectedPackagePaths = null;

	/* Constructor */
	
	public PackageLibraryAdapter(Context context, Cursor cursor, ArrayList<String> selectedPackagePaths) {
		super(context, cursor);
		this.selectedPackagePaths = selectedPackagePaths;
	}
	
	/* Methods */
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView nameTextView = (TextView) view.findViewById(R.id.lib_listitem_name);
		nameTextView.setText(cursor.getString(cursor.getColumnIndex(context.getString(R.string.library_column_name))));

		TextView codeTextView = (TextView) view.findViewById(R.id.lib_listitem_code);
		codeTextView.setText(cursor.getString(cursor.getColumnIndex(context.getString(R.string.library_column_course_code))));

		ImageView iconImageView = (ImageView) view.findViewById(R.id.lib_listitem_image);

		String iconPath = cursor.getString(cursor.getColumnIndex(context.getString(R.string.library_column_imagePath)));

		String folderPath = cursor.getString(cursor.getColumnIndex(context.getString(R.string.library_column_folder)));

		CheckBox checkbox = (CheckBox) view.findViewById(R.id.lib_listitem_checkbox);

		if (checkbox != null) {

			checkbox.setOnCheckedChangeListener(null);

			// Read in the packages that are already previously selected and set
			// them to checked.
			if (selectedPackagePaths != null && selectedPackagePaths.contains(folderPath)) {
				checkbox.setChecked(true);
			} else {
				checkbox.setChecked(false);
			}

			checkbox.setTag(folderPath);

			checkbox.setOnCheckedChangeListener(new SelectPackageCheckboxChangeListener());
		}

		if (StringUtils.isNotBlank(iconPath)) {
			try {
				// get the image
				InputStream bitmapInputStream = new FileInputStream(iconPath);

				Bitmap bitmap = BitmapFactory.decodeStream(bitmapInputStream);
				iconImageView.setImageBitmap(bitmap);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		String status = cursor.getString(cursor.getColumnIndex(context.getString(R.string.library_column_state)));

		if (StringUtils.isNotEmpty(status) && status.equalsIgnoreCase(DownloadBroadcastActions.PackageUpdate)) {
			ViewHelper.setAlphaFadeEnabled(view, false);
			checkbox.setEnabled(false);
		} else {
			ViewHelper.setAlphaFadeEnabled(view, true);
			checkbox.setEnabled(true);
		}

		super.bindView(view, context, cursor);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.library_listitem, parent, false);
		bindView(view, context, cursor);
		return view;
	}
}