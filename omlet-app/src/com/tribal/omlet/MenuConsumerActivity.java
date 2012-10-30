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

import android.os.Bundle;

import com.tribal.mobile.Framework;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.model.BaseContentItem;
import com.tribal.mobile.model.MenuItem;

/**
 * Class that provides base functionality for getting and setting {@link MenuItem} and {@link BaseContentItem}. 
 * 
 * @author Jon Brasted
 */
public abstract class MenuConsumerActivity extends NavigationActivity {
    /* Fields */
	
	protected MenuItem menuItem;
	protected BaseContentItem contentItem;

	/* Methods */

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // get the menu item or resource item
        getMenuItemOrResourceItem();
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		
		if (this.menuItem != null) {
			// persist the menu item onto the client
			Framework.getClient().setMenuItem(this.menuItem);
		}
		
		if (this.contentItem != null) {
			// persist the resource item onto the client
			Framework.getClient().setResourceItem(this.contentItem); 
		}
	}
    
	private void getMenuItemOrResourceItem() {
		// get navigation menu
		
    	Bundle extras = getIntent().getExtras();
    	
		if (extras != null && extras.containsKey(IntentParameterConstants.MenuItem)) {
			// get the menu
			this.menuItem = (MenuItem)extras.getSerializable(IntentParameterConstants.MenuItem);
			
			// persist the menu item onto the client
			Framework.getClient().setMenuItem(this.menuItem);
			
			// persist the content item
			if (this.menuItem.hasLinkedContentItem()) {
				// persist the resource item
				this.contentItem = this.menuItem.getLinkedContentItem();
				
				// persist the resource item onto the client
				Framework.getClient().setResourceItem(this.contentItem);
			} else {
				// persist null as the resource item
				Framework.getClient().setResourceItem(null);
			}
		} else {
			// try and get the content item
			if (extras != null && extras.containsKey(IntentParameterConstants.ResourceItem)) {
				// get the content item
				this.contentItem = (BaseContentItem)extras.getSerializable(IntentParameterConstants.ResourceItem);
				
				if (this.contentItem.hasMenuItemParent()) {
					this.menuItem = this.contentItem.getMenuItemParent(); 
				}
				
				Framework.getClient().setMenuItem(this.menuItem);
				Framework.getClient().setResourceItem(this.contentItem);
			}
		}
	}
}