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

package com.tribal.mobile.api.tracking;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to represent an entry in the tracking table in the native SQLite database.
 * 
 * @author Jon Brasted
 */
public class TrackingEntry {
	/* Fields */
	
	private int id;
	private String sender;
	private String objectId;
	private String deviceTimestamp;
	private String additionalInfo;
	
	/* Properties */
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getSender() {
		return sender;
	}
	
	public void setSender(String sender) {
		this.sender = sender;
	}
	
	public String getObjectId() {
		return objectId;
	}
	
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
	
	public String getDeviceTimestamp() {
		return deviceTimestamp;
	}
	
	public void setDeviceTimestamp(String deviceTimestamp) {
		this.deviceTimestamp = deviceTimestamp;
	}
	
	public String getAdditionalInfo() {
		return additionalInfo;
	}
	
	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
	
	/**
	 * Provides the JSON content string for the object. 
	 * 
	 * @return	the JSON content string for the object
	 */
	public String getJsonContentString() {
		JSONArray jsonArray = new JSONArray();
		
		JSONObject jsonObject = new JSONObject();
		
		try {
			jsonObject.put("objectId", objectId);
			jsonObject.put("sender", sender);
			jsonObject.put("deviceTimestamp", deviceTimestamp);
			
			
			jsonObject.put("addInfo", additionalInfo);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		jsonArray.put(jsonObject);
		
		return jsonArray.toString();
	}
}