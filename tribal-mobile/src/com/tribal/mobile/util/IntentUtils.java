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

package com.tribal.mobile.util;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;

/**
 * Utility class to assist with sending deferred <code>Intent</cod> objects.
 * 
 * @author Jon Brasted
 */
public class IntentUtils {
	/* Fields */
	
	private static IntentUtils instance;
	
	private List<Intent> deferredBroadcasts;
	
	/* Properties */
	
	public static IntentUtils getInstance() {
		if (instance == null) {
			instance = new IntentUtils();
		}
		
		return instance;
	}
	
	/* Constructor */
	
	public IntentUtils() {
		deferredBroadcasts = new ArrayList<Intent>();
	}
	
	/* Methods */
	
	public void addDeferredBroadcast(Intent deferredBroadcast) {
		this.deferredBroadcasts.add(deferredBroadcast);
	}

	public void addDeferredBroadcasts(List<Intent> deferredBroadcasts) {
		this.deferredBroadcasts.addAll(deferredBroadcasts);
	}
	
	public boolean isIntentExtraKeySet(String key) {
		Intent intent;
		
		for (int index = 0; index < deferredBroadcasts.size(); index++) {
			intent = deferredBroadcasts.get(index);
			
			if (intent.hasExtra(key)) {
				return true;
			}
		}

		return false;
	}
	
	public List<Intent> popDeferredBroadcasts() {
		List<Intent> poppedList = new ArrayList<Intent>();
		
		if (deferredBroadcasts.size() > 0) {
			for (int index = 0; index < deferredBroadcasts.size(); index++) {
				poppedList.add(deferredBroadcasts.get(index));
			}
			
			deferredBroadcasts.clear();
		}
		
		return poppedList;
	}
}
