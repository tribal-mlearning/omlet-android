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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.tribal.mobile.base.IntentParameterConstants;

/**
 * Main activity that hosts the library and catalogue tabs.
 * 
 * @author Jack Kierney
 */
@SuppressLint("NewApi")
public class MainTabActivity extends SystemMenuProviderActivity {
	/* Fields */
	
	private static ActionMode currentActionMode = null;
	private static android.view.ActionMode currentActionModeHC = null;
	
	private static Context context = null;
	public static String currentTab = null;
	
	/* Methods */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		
		context = getBaseContext();
		
		//default to home
		int selectedTab = 0;
		
		// if Tab has been changed back to this activity we need to refresh the library
		if (intent.hasExtra(IntentParameterConstants.SelectedTab)) {

			String selectedTabId = (String) intent.getSerializableExtra(IntentParameterConstants.SelectedTab);			
			
			if (selectedTabId.equalsIgnoreCase(getString(R.string.packageLibraryTabId))){			
				selectedTab = 0;
			} else if (selectedTabId.equalsIgnoreCase(getString(R.string.packageStoreTabId))){			
				selectedTab = 1;
			}		
		}
		
		// Disable Up button for this page.
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		getSupportActionBar().setHomeButtonEnabled(false);
		
		setContentView(R.layout.main_tab);
		  
		final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        bar.addTab(bar.newTab()
                .setText(getString(R.string.packageLibraryTabText))
                .setTabListener(new TabListener<LibraryFragment>(this, getString(R.string.packageLibraryTabId), LibraryFragment.class)));
        
        bar.addTab(bar.newTab()
                .setText(getString(R.string.packageStoreTabText))
                .setTabListener(new TabListener<CatalogueFragment>(this, getString(R.string.packageStoreTabId), CatalogueFragment.class)));
	        
	    bar.setSelectedNavigationItem(selectedTab);
	}	
	
	@Override
	public void onActionModeStarted(ActionMode mode) {
		super.onActionModeStarted(mode);
		
		// Keep track of action mode
		currentActionMode = mode;
	}	
	
	@Override
	public void onActionModeStarted(android.view.ActionMode mode) {
		super.onActionModeStarted(mode);
		
		// Keep track of action mode
		currentActionModeHC = mode;
	}
	
	public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
		/* Fields */
		
		private final Activity activity;
	    private final String tag;
	    private final Class<?> tabClass;
	    private final Bundle args;
        private Fragment fragment;

        /* Constructor */
        
        public TabListener(Activity activity, String tag, Class<T> tabClass) {
            this(activity, tag, tabClass, null);
        }

        public TabListener(Activity activity, String tag, Class<T> tabClass, Bundle args) {
            this.activity = activity;
            this.tag = tag;
            this.tabClass = tabClass;
            this.args = args;           
        }        
        
        @Override
		@SuppressLint("NewApi")
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
	        	
        	// Fix for compatibility library
    		FragmentManager fragMgr = ((SherlockFragmentActivity)activity).getSupportFragmentManager();
    	    ft = fragMgr.beginTransaction();
    	    ft.commit();
	        
    	    if (fragment == null) {
    	    	fragment = Fragment.instantiate(activity, tabClass.getName(), args);
                ft.add(android.R.id.content, fragment, tag);
            } else {
                ft.attach(fragment);
            }
	            
	        currentTab = tag;
	            
            // If the tab is not the 'On Device' Tab then don't display the action mode.
            if (tag.equalsIgnoreCase(context.getString(R.string.packageStoreTabId))) {
            	
        		if (currentActionMode != null) {
        			currentActionMode.finish();
        		} else if (currentActionModeHC != null) {
        			currentActionModeHC.finish();
        		}
	        }
	    }

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (fragment != null) {
				ft.detach(fragment);
			}
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {			
		}
	}
}