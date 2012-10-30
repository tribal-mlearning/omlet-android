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

package com.tribal.mobile.api.packages;

import java.io.Serializable;

/**
 * Class that represents some metadata fields that relate to a content package.
 * 
 * @author Jack Kierney
 */
public class MetaData implements Serializable {
	/* Fields */
	
	private static final long serialVersionUID = 6085999094252874028L;
		
	private String deviceWidth;
	private String deviceMinWidth;
	private String deviceMinHeight;
	private String deviceMaxWidth;
	private String deviceMaxHeight;
	private String deviceOS;
	private String deviceOSMinVersion;
	private String deviceOSMaxVersion;
	private String deviceHeight;
	
	public String getDeviceHeight() {
		return deviceHeight;
	}
	
	public void setDeviceHeight(String deviceHeight) {
		this.deviceHeight = deviceHeight;
	}
	
	public String getDeviceWidth() {
		return deviceWidth;
	}
	
	public void setDeviceWidth(String deviceWidth) {
		this.deviceWidth = deviceWidth;
	}
	
	public String getDeviceMinWidth() {
		return deviceMinWidth;
	}
	
	public void setDeviceMinWidth(String deviceMinWidth) {
		this.deviceMinWidth = deviceMinWidth;
	}
	
	public String getDeviceMinHeight() {
		return deviceMinHeight;
	}
	
	public void setDeviceMinHeight(String deviceMinHeight) {
		this.deviceMinHeight = deviceMinHeight;
	}
	
	public String getDeviceMaxWidth() {
		return deviceMaxWidth;
	}
	
	public void setDeviceMaxWidth(String deviceMaxWidth) {
		this.deviceMaxWidth = deviceMaxWidth;
	}
	
	public String getDeviceMaxHeight() {
		return deviceMaxHeight;
	}
	
	public void setDeviceMaxHeight(String deviceMaxHeight) {
		this.deviceMaxHeight = deviceMaxHeight;
	}
	
	public String getDeviceOS() {
		return deviceOS;
	}
	
	public void setDeviceOS(String deviceOS) {
		this.deviceOS = deviceOS;
	}
	
	public String getDeviceOSMinVersion() {
		return deviceOSMinVersion;
	}
	
	public void setDeviceOSMinVersion(String deviceOSMinVersion) {
		this.deviceOSMinVersion = deviceOSMinVersion;
	}
	
	public String getDeviceOSMaxVersion() {
		return deviceOSMaxVersion;
	}
	
	public void setDeviceOSMaxVersion(String deviceOSMaxVersion) {
		this.deviceOSMaxVersion = deviceOSMaxVersion;
	}
}