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

package com.tribal.mobile.util.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Environment;
import android.text.TextUtils;

import com.tribal.mobile.Framework;
import com.tribal.mobile.R;
import com.tribal.mobile.api.packages.LibraryItem;
import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.api.tracking.TrackingEntry;
import com.tribal.mobile.util.FileHelper;

/**
 * Class to facilitate base database logic required by the mobile framework. 
 * 
 * @author Jon Brasted, Jack Kierney and Jack Harrison
 */
public abstract class BaseDatabaseHelper extends DatabaseHelper {
	/* Fields */
	
	private final static String libraryTableName = "mylibrary";
	private final static String settingsTableName = "settings";
	private final static String trackingTableName = "tracking";
	
	/* Methods */

	public BaseDatabaseHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	/**
	 * Return internal databases folder path.
	 * 
	 * @return	internal databases folder path
	 */
	protected String getInternalDatabasesFolderPath() {
		String internalDatabasesFolderPathFormatString = context.getString(R.string.internalDatabasesFolderPathFormatString);
		String internalDatabasesFolderPath = String.format(internalDatabasesFolderPathFormatString, Environment.getDataDirectory(), context.getPackageName());
		
		return internalDatabasesFolderPath;
	}
	
	/**
	 * Close and teardown all databases.
	 */
	public void closeAndTeardownAllDatabases() {
		// close application database
		this.close();
		
		// get internal databases folder
		String internalDatabasesFolder = getInternalDatabasesFolderPath();
		
		if (!TextUtils.isEmpty(internalDatabasesFolder)) {
			File databasesFolder = new File(internalDatabasesFolder);
			
			if (databasesFolder.exists()) {
				FileHelper.deleteFilesRecursive(databasesFolder, true);
			}
		}
	}

	/**
	 * Get setting from database (synchronous).
	 * 
	 * @param settingName
	 * @return
	 */
	public String getSetting(String settingName) {
		String value = null;

		// construct statement

		String[] columns = new String[] { "value" };

		String queryString = "key=? AND userId=? AND objectId IS NULL";
		String[] queryParameters = new String[] { settingName,
				Framework.getClient().getUserUsername() };

		Cursor settingsCursor = null;

		try {
			settingsCursor = executeSelect(settingsTableName, columns,
					queryString, queryParameters, null);

			if (settingsCursor.getCount() == 1) {
				settingsCursor.moveToFirst();

				// get the value
				value = settingsCursor.getString(settingsCursor
						.getColumnIndex("value"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (settingsCursor != null) {
				// close the cursor
				settingsCursor.close();
			}
		}

		// return the value
		return value;
	}
	
	/**
	 * Get setting from database by objectId (synchronous).
	 * 
	 * @param settingName
	 * @param objectId
	 * @return
	 */
	public String getSettingByObjectId(String settingName, String objectId) {
		String value = null;

		// construct statement

		String[] columns = new String[] { "value" };

		String queryString = "key=? AND userId=? AND objectId=?";
		String[] queryParameters = new String[] { settingName, Framework.getClient().getUserUsername(), objectId };

		Cursor settingsCursor = null;

		try {
			settingsCursor = executeSelect(settingsTableName, columns, queryString, queryParameters, null);

			if (settingsCursor.getCount() == 1) {
				settingsCursor.moveToFirst();

				// get the value
				value = settingsCursor.getString(settingsCursor.getColumnIndex("value"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (settingsCursor != null) {
				// close the cursor
				settingsCursor.close();
			}
		}

		// return the value
		return value;
	}

	/**
	 * Create or update setting.
	 * 
	 * @param settingName	the setting name
	 * @param value			the value
	 * @return				whether the operation was successful
	 */
	public boolean createOrUpdateSetting(String settingName, String value) {
		String existingValue = getSetting(settingName);

		if (existingValue == null) {
			// create
			return createSetting(settingName, value);
		} else {
			if (!existingValue.equalsIgnoreCase(value)) {
				// update
				return updateSetting(settingName, value);
			}
		}

		return true;
	}

	/**
	 * Create or update setting with object id.
	 * 
	 * @param settingName	the setting name
	 * @param value			the value
	 * @param objectId		the object id
	 * @return				whether the operation was successful
	 */
	public boolean createOrUpdateSettingWithObjectId(String settingName, String value, String objectId) {
		String existingValue = getSettingByObjectId(settingName, objectId);

		if (existingValue == null) {
			// create
			return createSettingWithObjectId(settingName, value, objectId);
		} else {
			if (!existingValue.equalsIgnoreCase(value)) {
				// update
				return updateSettingWithObjectId(settingName, value, objectId);
			}
		}

		return true;
	}
	
	/**
	 * Create setting.
	 * 
	 * @param settingName	the setting name
	 * @param value			the value
	 * @return				whether the operation was successful
	 */
	public boolean createSetting(String settingName, String value) {
		// construct parameters

		ContentValues contentValues = new ContentValues();
		contentValues.put("key", settingName);
		contentValues.put("value", value);
		contentValues.put("userId", Framework.getClient().getUserUsername());

		boolean hasCreatedSetting = false;

		try {
			hasCreatedSetting = executeCreate(settingsTableName, contentValues);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// return the result
		return hasCreatedSetting;
	}
	
	/**
	 * Create setting with object id.
	 * 
	 * @param settingName	the setting name
	 * @param value			the value
	 * @param objectId		the object id
	 * @return				whether the operation was successful
	 */
	public boolean createSettingWithObjectId(String settingName, String value, String objectId) {
		// construct parameters

		ContentValues contentValues = new ContentValues();
		contentValues.put("key", settingName);
		contentValues.put("value", value);
		contentValues.put("userId", Framework.getClient().getUserUsername());
		contentValues.put("objectId", objectId);

		boolean hasCreatedSetting = false;

		try {
			hasCreatedSetting = executeCreate(settingsTableName, contentValues);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// return the result
		return hasCreatedSetting;
	}

	/**
	 * Update setting.
	 * 
	 * @param settingName	the setting name
	 * @param value			the value
	 * @return				whether the operation was successful
	 */
	private boolean updateSetting(String settingName, String value) {
		// construct parameters

		ContentValues contentValues = new ContentValues();
		contentValues.put("value", value);

		String queryString = "key=? AND userId=? AND objectId IS NULL";
		String[] queryParameters = new String[] { settingName,
				Framework.getClient().getUserUsername() };

		boolean hasUpdatedSetting = false;

		try {
			hasUpdatedSetting = executeUpdate(settingsTableName, contentValues,
					queryString, queryParameters);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// return the result
		return hasUpdatedSetting;
	}
	
	/**
	 * Update setting with object id.
	 * 
	 * @param settingName	the setting name
	 * @param value			the value
	 * @param objectId		the object id
	 * @return				whether the operation was successful
	 */
	private boolean updateSettingWithObjectId(String settingName, String value, String objectId) {
		// construct parameters

		ContentValues contentValues = new ContentValues();
		contentValues.put("value", value);

		String queryString = "key=? AND userId=? AND objectId=?";
		String[] queryParameters = new String[] { settingName, Framework.getClient().getUserUsername(), objectId };

		boolean hasUpdatedSetting = false;

		try {
			hasUpdatedSetting = executeUpdate(settingsTableName, contentValues, queryString, queryParameters);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// return the result
		return hasUpdatedSetting;
	}
	
	/**
	 * Delete all settings with object id and current user id.
	 * 
	 * @param objectId		the object id
	 */
	public void deleteSettingWithObjectIdByCurrentUserId(String objectId) {
		String sql = context.getString(R.string.delete_setting_objectId_userId_sql);
				
		String objectIdSqlQueryPlaceholder = context.getString(R.string.objectIdSqlQueryPlaceholder);
		String userIdSqlQueryPlaceholder = context.getString(R.string.userIdSqlQueryPlaceholder);
		
		// replace object id
		sql = sql.replace(objectIdSqlQueryPlaceholder, objectId);
		
		// replace user Id		
		String userId = Framework.getClient().getUserUsername();
		sql = sql.replace(userIdSqlQueryPlaceholder, userId);
		
		// execute sql
		try {
			executeSql(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Delete all settings with current user id.
	 */
	public void deleteSettingByCurrentUserId() {
		String sql = context.getString(R.string.delete_setting_userId_sql);
				
		String userIdSqlQueryPlaceholder = context.getString(R.string.userIdSqlQueryPlaceholder);
		
		// replace user Id		
		String userId = Framework.getClient().getUserUsername();
		sql = sql.replace(userIdSqlQueryPlaceholder, userId);
		
		// execute sql
		try {
			executeSql(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get tracking entries from database (synchronous).
	 * 
	 * @param callback
	 * @return tracking entries cursor
	 */
	public Cursor getTrackingEntries() {
		// get statement

		// get username
		String username = Framework.getClient().getUserUsername();

		// get sql query
		String sqlQuery = context.getString(R.string.get_tracking_entries_sql);
		
		// execute sql query
		try {
			return executeSelectQuery(sqlQuery, new String[] { username });
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Return tracking entries list.
	 * 
	 * @return	tracking entries list
	 */
	public List<TrackingEntry> getTrackingEntriesList() {
		List<TrackingEntry> trackingEntryList = new ArrayList<TrackingEntry>();

		// get the cursor
		Cursor trackingEntriesCursor = getTrackingEntries();

		// get the column index for the entries
		int idIndex = trackingEntriesCursor.getColumnIndex("_id");
		int objectIdIndex = trackingEntriesCursor.getColumnIndex("objectId");
		int senderIndex = trackingEntriesCursor.getColumnIndex("sender");
		int deviceTimestampIndex = trackingEntriesCursor.getColumnIndex("deviceTimestamp");
		int addInfoIndex = trackingEntriesCursor.getColumnIndex("addInfo");

		TrackingEntry trackingEntry = null;
		int id = 0;
		String objectId = null;
		String sender = null;
		String deviceTimestamp = null;
		String addInfo = null;

		// iterate over the cursor
		while (trackingEntriesCursor.moveToNext()) {
			trackingEntry = new TrackingEntry();

			id = trackingEntriesCursor.getInt(idIndex);
			trackingEntry.setId(id);

			objectId = trackingEntriesCursor.getString(objectIdIndex);
			trackingEntry.setObjectId(objectId);

			sender = trackingEntriesCursor.getString(senderIndex);
			trackingEntry.setSender(sender);

			deviceTimestamp = trackingEntriesCursor.getString(deviceTimestampIndex);
			trackingEntry.setDeviceTimestamp(deviceTimestamp);

			if (!trackingEntriesCursor.isNull(addInfoIndex)) {
				addInfo = trackingEntriesCursor.getString(addInfoIndex);
				trackingEntry.setAdditionalInfo(addInfo);
			}

			trackingEntryList.add(trackingEntry);
		}

		// close the tracking entries cursor
		trackingEntriesCursor.close();

		return trackingEntryList;
	}
	
	/**
	 * Delete tracking entry.
	 * 
	 * @param id	the tracking entry id
	 */
	public void removeTrackingEntry(String id) {
		try {
			String deleteTrackingEntryWhereClauseSql = context.getString(R.string.delete_tracking_entry_where_clause_sql);
			executeDelete(trackingTableName, deleteTrackingEntryWhereClauseSql, new String[] { id });
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Clear the tracking log.
	 * 
	 * @return
	 */
	public boolean clearTrackingLog() {
		try {
			return executeDelete(trackingTableName, null, null);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Create a tracking entry.
	 * 
	 * @param sender			the sender
	 * @param objectId			the object id
	 * @param additionalInfo	the additional info
	 * @return					whether the operation was successful
	 */
	public boolean createTrackingEntry(String sender, String objectId,
			String additionalInfo) {
		// construct parameters

		ContentValues contentValues = new ContentValues();
		contentValues.put("userId", Framework.getClient().getUserUsername());
		contentValues.put("objectId", objectId);
		contentValues.put("sender", sender);
		contentValues.put("addInfo", additionalInfo);

		boolean hasCreatedTrackingEntry = false;

		try {
			hasCreatedTrackingEntry = executeCreate(trackingTableName,
					contentValues);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// return the result
		return hasCreatedTrackingEntry;
	}

	/**
	 * Return a {@link Cursor} for all entries in the MyLibrary table.
	 * 
	 * @return	a {@link Cursor} for all entries in the MyLibrary table
	 */
	public Cursor getMyLibrary() {

		// construct statement
		Cursor myLibraryCursor = null;

		// get sql query
		String sqlQuery = context.getString(R.string.get_my_library_sql);
		
		// execute sql query
		try {
			myLibraryCursor = executeSelectQuery(sqlQuery, null);
		} catch (SQLException e) {
			e.printStackTrace();

			if (myLibraryCursor != null) {
				myLibraryCursor.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// return the cursor
		return myLibraryCursor;
	}

	/**
	 * Return a {@link LibraryItem} containing the data for a particular entry in the MyLibrary table.
	 * 
	 * @param uniqueId	the unique id		
	 * @return			a {@link LibraryItem} containing the data for a particular entry in the MyLibrary table
	 */
	public LibraryItem getLibraryItemByUniqueId(String uniqueId) {
		// construct statement
		Cursor myLibraryCursor = null;
		LibraryItem libraryItem = null;

		// get sql query
		String sqlQuery = context.getString(R.string.get_my_library_single_item_sql);
		
		// execute sql query
		try {
			myLibraryCursor = executeSelectQuery(sqlQuery, new String[] { uniqueId });

			if (myLibraryCursor.moveToFirst()) {
				String itemName;
				String description;
				String organisation;
				String fileUrl;
				String imagePath;
				String publishedDate;
				String localFolder;
				String version;
				String courseCode;
				String md5sum;

				String library_column_name = "name";
				String library_column_org = "organization";
				String library_column_fileUrl = "fileUrl";
				String library_column_imagePath = "imagePath";
				String library_column_description = "description";
				String library_column_published_date = "published_date";
				String library_column_folder = "localFolder";
				String library_column_version = "version";
				String library_column_courseCode = "courseCode";
				String library_column_md5sum = "md5sum";
				
				itemName = myLibraryCursor.getString(myLibraryCursor.getColumnIndex(library_column_name));
				description = myLibraryCursor.getString(myLibraryCursor.getColumnIndex(library_column_description));
				organisation = myLibraryCursor.getString(myLibraryCursor.getColumnIndex(library_column_org));
				fileUrl = myLibraryCursor.getString(myLibraryCursor.getColumnIndex(library_column_fileUrl));
				imagePath = myLibraryCursor.getString(myLibraryCursor.getColumnIndex(library_column_imagePath));
				publishedDate = myLibraryCursor.getString(myLibraryCursor.getColumnIndex(library_column_published_date));
				localFolder = myLibraryCursor.getString(myLibraryCursor.getColumnIndex(library_column_folder));
				
				// Updated to include version and course code
				// Jack Harrison 13/06/2012
				version = myLibraryCursor.getString(myLibraryCursor.getColumnIndex(library_column_version));
				courseCode = myLibraryCursor.getString(myLibraryCursor.getColumnIndex(library_column_courseCode));
				md5sum = myLibraryCursor.getString(myLibraryCursor.getColumnIndex(library_column_md5sum));
				
				libraryItem = new LibraryItem(uniqueId, itemName, description, organisation, fileUrl, imagePath, publishedDate, localFolder, version, courseCode, md5sum);
			}
		} catch (SQLException e) {
			e.printStackTrace();

			if (myLibraryCursor != null) {
				myLibraryCursor.close();
				myLibraryCursor = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (myLibraryCursor != null) {
			myLibraryCursor.close();
		}

		return libraryItem;
	}

	/**
	 * Delete a particular entry from MyLibrary table by the unique id.
	 * 
	 * @param id	the unique id
	 * @return		a particular entry from MyLibrary table by the unique id
	 */
	public boolean removeLibraryEntry(String id) {
		String deleteLibraryEntryWhereClause = context.getString(R.string.delete_library_entry_where_clause_sql);
		
		// get where clause
		try {
			return executeDelete(libraryTableName, deleteLibraryEntryWhereClause, new String[] { id });
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}
	
	/**
	 * Update status field for a particular row in the MyLibrary table by unique id.
	 * 
	 * @param id		the unique id
	 * @param state		the state
	 * @return			whether the operation was successful
	 */
	public boolean updateLibraryEntryStatus(String id, String state) {
		// construct parameters

		ContentValues contentValues = new ContentValues();
		contentValues.put("state", state);

		String queryString = "uniqueId=?";
		String[] queryParameters = new String[] { id };

		boolean hasUpdatedStatus = false;

		try {
			hasUpdatedStatus = executeUpdate(libraryTableName, contentValues, queryString, queryParameters);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// return the result
		return hasUpdatedStatus;
	}
	
	/**
	 * Create a MyLibrary entry.
	 * 
	 * @param packageItem	the {@link PackageItem} object
	 * @param imagePath		the image path
	 * @param folderName	the folder name
	 * @return				whether the operation was successful
	 */
	public boolean createMyLibraryEntry(PackageItem packageItem,
			String imagePath, String folderName) {
		// construct parameters
		removeLibraryEntry(packageItem.getUniqueId());

		ContentValues contentValues = new ContentValues();
		contentValues.put("name", packageItem.getName());
		contentValues.put("organization", packageItem.getOrganization());
		contentValues.put("fileUrl", packageItem.getFileUrl());
		contentValues.put("imagePath", imagePath);
		contentValues.put("description", packageItem.getDescription());
		// contentValues.put("published_date", packageItem.getPublishDate());
		contentValues.put("published_date", new Date().toString());
		contentValues.put("localFolder", folderName);
		contentValues.put("courseCode", packageItem.getCourseCode());
		contentValues.put("version", packageItem.getVersion());
		contentValues.put("download_date", new Date().toString());
		contentValues.put("uniqueId", packageItem.getUniqueId());
		contentValues.put("md5sum", packageItem.getMD5sum());

		boolean hasCreatedMyLibraryEntry = false;

		try {
			hasCreatedMyLibraryEntry = executeCreate("mylibrary", contentValues);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// return the result
		return hasCreatedMyLibraryEntry;
	}
}