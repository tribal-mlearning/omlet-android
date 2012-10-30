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

import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

/**
 * Class that encapsulates {@link SQLiteOpenHelper} and provides convenience methods for various operations.
 * 
 * @author Jon Brasted
 */
public abstract class DatabaseHelper extends SQLiteOpenHelper {
	/* Fields */

	public static String DEFAULT_DB_NAME = "database.sqlite";

	protected SQLiteDatabase database;	
	protected final Context context;

	/* Properties */
	
	@Override
	public String getDatabaseName() {
		return DEFAULT_DB_NAME;
	}
	
	/* Constructor */

	/**
	 * Constructor Takes and keeps a reference of the passed context in order to
	 * access to the application assets and resources.
	 * 
	 */	
	public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		this.context = context;
		
		// when we move away from database being stored on the SD card, we will use this method
		database = getWritableDatabase();
	}
	
	/* Methods */
	
	@Override
	public synchronized void close() {
		if (database != null) {
			database.close();
		}
		
		super.close();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String createDatabaseSql = getCreateDatabaseSql();
		
		if (!TextUtils.isEmpty(createDatabaseSql)) {
			processCreateUpdateSql(db, createDatabaseSql);
		}
		
		database = db;
		
		if (!database.isOpen()) {
			database = getWritableDatabase();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String updateDatabaseSql = getUpdateDatabaseSql(oldVersion, newVersion);
		
		if (!TextUtils.isEmpty(updateDatabaseSql)) {
			processCreateUpdateSql(db, updateDatabaseSql);
		}
	}

	/**
	 * Returns sql required for database creation.
	 * 	
	 * @return	sql required for database creation
	 */
	protected String getCreateDatabaseSql() {
		return null;
	}
	
	/**
	 * Returns sql required for database update. 
	 * 
	 * @param oldVersion	the old version number
	 * @param newVersion	the new version number
	 * @return				sql required for database update
	 */
	protected String getUpdateDatabaseSql(int oldVersion, int newVersion) {
		return null;
	}

	/**
	 * Process creation and update sql.
	 * 
	 * @param db					the database
	 * @param createDatabaseSql		the create database sql
	 */
	protected void processCreateUpdateSql(SQLiteDatabase db, String createDatabaseSql) {
		// split by semicolon
		String[] sqlStrings = createDatabaseSql.split("[;]");
		
		for (String sqlString : sqlStrings) {
			if (!TextUtils.isEmpty(sqlString)) {			
				db.execSQL(sqlString);
			}
		}
	}
	
	/**
	 * Execute a SQL SELECT.
	 * 
	 * @param tableName			the table name
	 * @param columns			the columns
	 * @param whereClause		the where clause
	 * @param whereArgs			the where arguments
	 * @param orderBy			the order by
	 * @return					a Cursor containing the returned data
	 * @throws SQLException
	 * @throws IOException
	 */
	protected Cursor executeSelect(String tableName, String[] columns, String whereClause, String[] whereArgs, String orderBy) throws SQLException, IOException {
		if (!database.isOpen()) {
			database = getWritableDatabase();
		}
		
		Cursor cursor = database.query(tableName, columns, whereClause, whereArgs, null, null, orderBy);

		return cursor;
	}
	
	/**
	 * Execute a SQL SELECT statement.
	 * 
	 * @param sql				the SQL string
	 * @param whereArgs			the where arguments
	 * @return					a Cursor containing the returned data
	 * @throws SQLException
	 * @throws IOException
	 */
	protected Cursor executeSelectQuery(String sql, String[] whereArgs) throws SQLException, IOException {
		if (!database.isOpen()) {
			database = getWritableDatabase();
		}
		
		// run the query
		Cursor cursor = database.rawQuery(sql, whereArgs);

		return cursor;
	}

	/**
	 * Execute a SQL INSERT.
	 * 
	 * @param tableName			the table name
	 * @param contentValues		the content values
	 * @return					whether the operation was successful
	 * @throws SQLException
	 * @throws IOException
	 */
	protected boolean executeCreate(String tableName, ContentValues contentValues) throws SQLException, IOException {
		if (!database.isOpen()) {
			database = getWritableDatabase();
		}
		
		long rowId = database.insert(tableName, null, contentValues);

		return (rowId > 0);
	}

	/**
	 * Execute a SQL UPDATE.
	 * 
	 * @param tableName			the table name
	 * @param contentValues		the content values
	 * @param whereClause		the where clause
	 * @param whereArgs			the where arguments
	 * @return					whether the operation was successful
	 * @throws SQLException
	 * @throws IOException
	 */
	protected boolean executeUpdate(String tableName, ContentValues contentValues, String whereClause, String[] whereArgs) throws SQLException, IOException {
		if (!database.isOpen()) {
			database = getWritableDatabase();
		}
		
		long numberOfRowsAffected = database.update(tableName, contentValues, whereClause, whereArgs);

		return (numberOfRowsAffected > 0);
	}
	
	/**
	 * Execute a SQL statement
	 * 
	 * @param sql				the SQL string
	 * @throws SQLException
	 * @throws IOException
	 */
	protected void executeSql(String sql) throws SQLException, IOException {
		if (!database.isOpen()) {
			database = getWritableDatabase();
		}
		
		database.execSQL(sql);
	}
	
	/**
	 * Execute a SQL DELETE.
	 * 
	 * @param tableName			the table name
	 * @param whereClause		the where clause
	 * @param whereArgs			the where arguments
	 * @return					whether the operation was successful
	 * @throws SQLException
	 * @throws IOException
	 */
	protected boolean executeDelete(String tableName, String whereClause, String[] whereArgs) throws SQLException, IOException {
		if (!database.isOpen()) {
			database = getWritableDatabase();
		}
		
		long numberOfRowsAffected = database.delete(tableName, whereClause, whereArgs);

		return (numberOfRowsAffected > 0);
	}
}