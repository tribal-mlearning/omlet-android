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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.model.LayoutType;
import com.tribal.mobile.model.MenuItem;

/**
 * Activity that encapsulates a native menu.
 * 
 * @author Jon Brasted
 */
public class ListMenuActivity extends SystemMenuProviderActivity {
	/* Fields */

	private ListView listView;
	private MenuItemAdapter menuItemAdapter;

	private int layoutResourceId = 0;
	private int layoutMenuItemResourceId = 0;

	private List<MenuItem> items;
	
	protected String packageName;

	/* Methods */

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (this.menuItem != null) {
			// get the layout type
			if (this.menuItem.getLayoutType() == LayoutType.minilist) {
				layoutResourceId = R.layout.list_menu_rounded_borders;
				layoutMenuItemResourceId = R.layout.list_menu_rounded_corners_row_template;
			} else {
				layoutResourceId = R.layout.list_menu;
				layoutMenuItemResourceId = R.layout.list_menu_row_template;
			}
		} else {
			layoutResourceId = R.layout.list_menu;
			layoutMenuItemResourceId = R.layout.list_menu_row_template;
		}

		setContentView(layoutResourceId);

		getPackageNameFromIntent();
		
		// set Header
		if (!TextUtils.isEmpty(packageName)) {
			getSupportActionBar().setTitle(packageName);
		}

		// load menu
		loadMenu();
	}

	private void getPackageNameFromIntent() {
		Intent intent = getIntent();
		if (intent.hasExtra(IntentParameterConstants.PackageName)) {
			packageName = intent.getStringExtra(IntentParameterConstants.PackageName);
		}
	}
	
	private void loadMenu() {
		// get list view
		this.listView = (ListView) findViewById(R.id.list);

		if (this.menuItem != null) {
			setOnItemClickHandler(listView);

			// get items
			items = menuItem.getChildren();

			if (items == null) {
				items = new ArrayList<MenuItem>();
			}
			
			// create new item adapter
			menuItemAdapter = new MenuItemAdapter(listView.getContext(), layoutMenuItemResourceId, items);

			listView.setAdapter(menuItemAdapter);
		}
	}

	private void setOnItemClickHandler(ListView listView) {
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// get item for the position
				MenuItem menuItem = items.get(position);

				if (menuItem.isEnabled()) {
					HashMap<String, Serializable> parameterMap = new HashMap<String, Serializable>();
					parameterMap.put(IntentParameterConstants.PackageName, packageName);
					navigateByMenuItem(view.getContext(), menuItem, parameterMap);
				}
			}
		});
	}

	private class MenuItemAdapter extends ArrayAdapter<MenuItem> {
		/* Fields */
		
		private List<MenuItem> items;
		private int rowLayoutResourceId;

		/* Constructor */
		
		public MenuItemAdapter(Context context, int layoutResourceId, List<MenuItem> items) {
			super(context, layoutResourceId, items);
			this.items = items;
			this.rowLayoutResourceId = layoutResourceId;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;

			MenuItem menuItem = items.get(position);

			if (view == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(rowLayoutResourceId, null);
			}

			// get the list view item separator
			View listItemSeparator = view.findViewById(R.id.listItemSeparator);

			if (listItemSeparator != null) {
				// check whether to hide the separator for the first item in the
				// list
				if (position == 0) {
					listItemSeparator.setVisibility(View.GONE);
				} else {
					listItemSeparator.setVisibility(View.VISIBLE);
				}
			}

			if (menuItem != null) {
				ImageView menuItemImageView = (ImageView) view.findViewById(R.id.menuItemImage);

				if (menuItemImageView != null) {
					InputStream bitmapInputStream = null;
					try {
						if (StringUtils.isNotEmpty(menuItem.getIconPath())) {
							// Get the input stream for the file path
							File file = new File(menuItem.getIconPath());

							bitmapInputStream = new FileInputStream(file);

							Bitmap bitmap = BitmapFactory.decodeStream(bitmapInputStream);
							menuItemImageView.setImageBitmap(bitmap);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				TextView menuItemNameView = (TextView) view.findViewById(R.id.menuItemName);

				if (menuItemNameView != null) {
					menuItemNameView.setText(menuItem.getTitle());
				}
			}

			return view;
		}
	}
}