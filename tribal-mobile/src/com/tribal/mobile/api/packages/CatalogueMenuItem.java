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
import java.util.List;
import java.util.Map;

/**
 * Class that represents a catalogue menu item.
 * 
 * @author Jack Kierney
 */
public class CatalogueMenuItem implements Serializable {
	/* Fields */
	
	private static final long serialVersionUID = 7610053035143256789L;
	private int id;
	private Map<String, String> localisedNames;
	private List<CatalogueMenuItem> subMenuItems;
	
	/* Constructor */
	
	public CatalogueMenuItem(int id, List<CatalogueMenuItem> subMenuItems, Map<String, String> localisedNames) {
		this.id = id;
		this.localisedNames = localisedNames;
		this.subMenuItems = subMenuItems;
	}
	
	public int getId() {
		return this.id;
	}
	
	public Map<String, String> getLocalisedNames() {
		return this.localisedNames;
	}
	
	public List<CatalogueMenuItem> getSubMenuItems() {
		return this.subMenuItems;
	}

	
}
