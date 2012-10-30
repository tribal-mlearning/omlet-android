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

/**
 * Class that represents an item in the local content library. 
 * 
 * @author Jon Brasted, Jack Kierney and Jack Harrison.
 */
public class LibraryItem {
	/* Fields */

	private String uniqueId;
	private String name;
	private String description;
	private String organisation;
	private String fileUrl;
	private String imagePath;
	private String publishedDate;
	private String localFolder;
	private String version;
	private String courseCode;
	private String md5sum;

	/* Properties */

	public String getUniqueId() {
		return uniqueId;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getOrganisation() {
		return organisation;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public String getImagePath() {
		return imagePath;
	}

	public String getPublishedDate() {
		return publishedDate;
	}

	public String getLocalFolder() {
		return localFolder;
	}

	public String getVersion() {
		return version;
	}

	public String getCourseCode() {
		return courseCode;
	}

	public String getMD5sum() {
		return md5sum;
	}

	/* Constructors */

	public LibraryItem(String uniqueId, String name) {
		this.uniqueId = uniqueId;
		this.name = name;
	}

	public LibraryItem(String uniqueId, String name, String description, String organisation, String fileUrl, String imagePath, String publishedDate, String localFolder,
			String version, String courseCode, String md5sum) {
		this(uniqueId, name);
		this.description = description;
		this.organisation = organisation;
		this.fileUrl = fileUrl;
		this.imagePath = imagePath;
		this.publishedDate = publishedDate;
		this.localFolder = localFolder;
		this.version = version;
		this.courseCode = courseCode;
		this.md5sum = md5sum;
	}
}
