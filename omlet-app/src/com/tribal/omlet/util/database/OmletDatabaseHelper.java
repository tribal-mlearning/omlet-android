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

package com.tribal.omlet.util.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import com.tribal.mobile.util.ContextUtils;
import com.tribal.mobile.util.database.BaseDatabaseHelper;
import com.tribal.mobile.util.database.SQLiteCursorFactory;

public class OmletDatabaseHelper extends BaseDatabaseHelper {
	/* Fields */

	private static CursorFactory cursorFactory = new SQLiteCursorFactory(false);

	/* Constructor */

	public OmletDatabaseHelper(Context context, String databaseName) {
		super(context, databaseName, cursorFactory, ContextUtils.getVersionCode(context));
	}

	/* Methods */
	
	@Override
	protected String getCreateDatabaseSql() {
		InputStream inputStream;

		try {
			inputStream = context.getAssets().open("tables_create.sql");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder stringBuilder = new StringBuilder();

			String line = bufferedReader.readLine();
			while (line != null) {
				stringBuilder.append(line);
				line = bufferedReader.readLine();
			}

			return stringBuilder.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}

	@Override
	protected String getUpdateDatabaseSql(int oldVersion, int newVersion) {
		InputStream inputStream;
		StringBuilder stringBuilder = new StringBuilder();

		try {
			// Loop through versions starting at the next version up from the
			// current to the latest
			for (int i = oldVersion + 1; i <= newVersion; i++) {

				// scripts will be named with version no. appended
				String scriptName = "tables_upgrade_" + i + ".sql";

				// Check if there is an upgrade script for this version and read
				// it
				if (Arrays.asList(context.getAssets().list("")).contains(scriptName)) {
					inputStream = context.getAssets().open(scriptName);

					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

					String line = bufferedReader.readLine();
					while (line != null) {
						stringBuilder.append(line);
						line = bufferedReader.readLine();
					}
				}
			}

			// return sql in colon separated string
			return stringBuilder.toString();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}
}