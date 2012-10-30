package com.tribal.mobile.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.tribal.mobile.R;
import com.tribal.mobile.api.packages.Catalogue;
import com.tribal.mobile.api.packages.LibraryItem;
import com.tribal.mobile.api.packages.MetaData;
import com.tribal.mobile.api.packages.PackageComparator;
import com.tribal.mobile.api.packages.PackageFile;
import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.net.AuthHttpConnection;
import com.tribal.mobile.net.HttpMethod;
import com.tribal.mobile.net.URLConnectionUtils;
import com.tribal.mobile.preferences.PrivateSettingsKeys;

/**
 * Utility class to facilitate working with {@link PackageItem} objects.
 * 
 * @author Jon Brasted, Jack Kierney, Jack Harrison
 */
public class PackageHelper {
	/**
	 * Convert an {@link InputStream} to a {@link String}.
	 * 
	 * @param is	the input stream
	 * @return		the input stream as a <code>String</code>
	 */
	public static String convertStreamToString(InputStream is) {
		try {
			return new java.util.Scanner(is).useDelimiter("\\A").next();
		} catch (java.util.NoSuchElementException e) {
			return "";
		}
	}
	
	/**
	 * Convert a JSON string to a {@link Catalogue} object.
	 * 
	 * @param json		the JSON string
	 * @param context	the context
	 * @return			a {@link Catalogue} object
	 */
	public static Catalogue convertJSONToCatalogue(String json, Context context) {
		JsonParser parser = new JsonParser();
		JsonElement jsonRoot = parser.parse(json);
		JsonArray jsonPackages; // packages array
		if (jsonRoot.isJsonObject()) {
			JsonObject jsonRootObject = jsonRoot.getAsJsonObject();
			jsonPackages = jsonRootObject.getAsJsonArray("packages");
		} else if (jsonRoot.isJsonArray()) {
			jsonPackages = jsonRoot.getAsJsonArray();
		} else {
			jsonPackages = new JsonArray();
		}

		ArrayList<PackageItem> packages = convertJsonArrayToPackages(jsonPackages, context);
		
		// JB. 28/06/2012.
		// Sort in alphabetical order.
		Collections.sort(packages, new PackageComparator());

		Catalogue catalogue = new Catalogue(packages);
		
		return catalogue;
	}

	private static String getJsonString(JsonElement element) {
		if (element == null) {
			return null;
		}
		if (element.isJsonNull()) {
			return null;
		}
		return element.getAsString();
	}
	
	private static ArrayList<PackageItem> convertJsonArrayToPackages(JsonArray jsonArray, Context context) {
		ArrayList<PackageItem> packages = new ArrayList<PackageItem>();
		JsonObject jsonObject;
		for (int index = 0, jsonArraySize = jsonArray.size(); index < jsonArraySize; index++) {
			try {
				jsonObject = jsonArray.get(index).getAsJsonObject();// .getJSONObject(index);

				if (!jsonObject.has("title") || !jsonObject.has("files")) {
					continue;
				}

				PackageItem packageItem = new PackageItem();

				packageItem.setName(getJsonString(jsonObject.get("title")));
				
				if (jsonObject.has("uniqueId")) {
					packageItem.setUniqueId(getJsonString(jsonObject.get("uniqueId")));
				}

				if (jsonObject.has("description")) {
					packageItem.setDescription(getJsonString(jsonObject.get("description")));
				}

				if (jsonObject.has("organisation")) {
					packageItem.setOrganization(getJsonString(jsonObject.get("organisation")));
				}

				if (jsonObject.has("username")) {
					packageItem.setUsername(getJsonString(jsonObject.get("organisation")));
				}

				if (jsonObject.has("fileUrl")) {
					packageItem.setFileUrl(getJsonString(jsonObject.get("fileUrl")));
				}

				if (jsonObject.has("thumbnailUrl")) {
					packageItem.setImageUrl(getJsonString(jsonObject.get("thumbnailUrl")));
				}

				if (jsonObject.has("size")) {
					packageItem.setFileSize(getJsonString(jsonObject.get("size")));
				}

				// TODO: This will probably change when it's actually coming
				// back from the service
				if (jsonObject.has("version")) {
					packageItem.setVersion(getJsonString(jsonObject.get("version")));
				}
				
				if (jsonObject.has("categories")) {
					JsonArray jsonCategories = jsonObject.getAsJsonArray("categories");
					int numCategories = jsonCategories.size();
					int[] categories = new int[numCategories];
					for (int i = 0; i < numCategories; i++) {
						categories[i] = jsonCategories.get(i).getAsInt();
					}
					packageItem.setCategories(categories);
				} else {
					packageItem.setCategories(new int[] {});
				}

				if (jsonObject.has("uniqueId")) {
					packageItem.setUniqueId(getJsonString(jsonObject.get("uniqueId")));
				} else {

					String nameFromUrl = getJsonString(jsonObject.get("fileUrl"));
					if (nameFromUrl != null) {
						nameFromUrl = FileHelper.removeExtensionFromPath(nameFromUrl);
						nameFromUrl = FileHelper.getFileNameFromPath(nameFromUrl);
						packageItem.setUniqueId(nameFromUrl);
					}
				}
				
				if (jsonObject.has("metadata")) {
					Map<String, String> metaData = new HashMap<String, String>();
					JsonObject metaDataObject = jsonObject.getAsJsonObject("metadata");
					for (Entry<String, JsonElement> entry : metaDataObject.entrySet()) {
						metaData.put(entry.getKey(), getJsonString(entry.getValue()));
					}
					
					if (metaData.containsKey("courseCode")) {
						packageItem.setCourseCode(metaData.get("courseCode"));
					}
					
					packageItem.setMetaData(metaData);
				}

				if (jsonObject.has("files")) {
					JsonArray jsonfileArray = jsonObject.getAsJsonArray("files");

					// Parse Files JSON into a list of file classes
					ArrayList<PackageFile> files = convertJSONArrayToPackageFiles(jsonfileArray);

					// Get Devices Metrics
					DisplayMetrics dm = DisplayUtils.getDisplayMetrics(context);

					// Default to the first file
					PackageFile selectedFile = files.get(0);

					int previousWidth = 0;
					int previousHeight = 0;

					// Loop through files for this package
					for (PackageFile file : files) {
						int minWidth = 0;
						int minHeight = 0;
						try {
							minWidth = Integer.parseInt(file.getMetaData().getDeviceMinWidth());
							minHeight = Integer.parseInt(file.getMetaData().getDeviceMinHeight());

						} catch (Exception e) {}

						// If the file is a closer match size wise then use it
						// TODO: Will need to cater to all metadata in future.
						if (minWidth <= dm.widthPixels && minWidth > previousWidth && minHeight <= dm.heightPixels && minHeight > previousHeight) {
							previousWidth = minWidth;
							previousHeight = minHeight;
							selectedFile = file;
						}
					}

					// Set file info on the package
					packageItem.setFileUrl(selectedFile.getUrl());
					packageItem.setVersion(selectedFile.getVersion());
					packageItem.setFileSize(selectedFile.getSize());
					packageItem.setMD5sum(selectedFile.getMD5sum());
				}

				packages.add(packageItem);

			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return packages;
	}

	private static ArrayList<PackageFile> convertJSONArrayToPackageFiles(JsonArray jsonArray) {

		ArrayList<PackageFile> files = new ArrayList<PackageFile>();
		JsonObject jsonObject;

		for (int index = 0, jsonArraySize = jsonArray.size(); index < jsonArraySize; index++) {
			try {
				jsonObject = jsonArray.get(index).getAsJsonObject();

				PackageFile file = new PackageFile();

				if (jsonObject.has("version")) {
					file.setVersion(getJsonString(jsonObject.get("version")));
				}

				if (jsonObject.has("size")) {
					file.setSize(getJsonString(jsonObject.get("size")));
				}

				if (jsonObject.has("url")) {
					file.setUrl(getJsonString(jsonObject.get("url")));
				}

				if (jsonObject.has("md5sum")) {
					file.setMD5sum(getJsonString(jsonObject.get("md5sum")));
				}

				if (jsonObject.has("metadata")) {
					JsonObject metaJSON = jsonObject.getAsJsonObject("metadata");
					MetaData metaData = convertJSONObjectToMetaData(metaJSON);
					file.setMetaData(metaData);
				}

				files.add(file);

			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return files;
	}

	private static MetaData convertJSONObjectToMetaData(JsonObject jsonObject) {

		MetaData metaData = new MetaData();

		try {

			if (jsonObject.has("deviceHeight")) {
				metaData.setDeviceHeight(getJsonString(jsonObject.get("deviceHeight")));
			}

			if (jsonObject.has("deviceWidth")) {
				metaData.setDeviceWidth(getJsonString(jsonObject.get("deviceWidth")));
			}

			if (jsonObject.has("deviceMinWidth")) {
				metaData.setDeviceMinWidth(getJsonString(jsonObject.get("deviceMinWidth")));
			}

			if (jsonObject.has("deviceMinHeight")) {
				metaData.setDeviceMinHeight(getJsonString(jsonObject.get("deviceMinHeight")));
			}

			if (jsonObject.has("deviceMaxWidth")) {
				metaData.setDeviceMaxWidth(getJsonString(jsonObject.get("deviceMaxWidth")));
			}

			if (jsonObject.has("deviceMaxHeight")) {
				metaData.setDeviceMaxHeight(getJsonString(jsonObject.get("deviceMaxHeight")));
			}

			if (jsonObject.has("deviceOS")) {
				metaData.setDeviceOS(getJsonString(jsonObject.get("deviceOS")));
			}

			if (jsonObject.has("deviceOSMinVersion")) {
				metaData.setDeviceOSMinVersion(getJsonString(jsonObject.get("deviceOSMinVersion")));
			}

			if (jsonObject.has("deviceOSMaxVersion")) {
				metaData.setDeviceOSMaxVersion(getJsonString(jsonObject.get("deviceOSMaxVersion")));
			}

		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return metaData;

	}

	/**
	 * Copy a {@link Drawable} object to external storage.
	 * 
	 * @param context		the context
	 * @param fileUrl		the file URL
	 * @param path			the file path
	 * @param fileName		the local storage file name
	 */
	public static void copyDrawableToExternalStorage(Context context, String fileUrl, String path, String fileName) {
		try {
			URL url = new URL(fileUrl);

			boolean isUrlHttps = url.getProtocol().toLowerCase().equals("https");
			final boolean acceptSelfSignedCertificates = NativeSettingsHelper.getInstance(context).checkAndGetPrivateBooleanSetting(PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS, false);

			if (isUrlHttps) {
				if (acceptSelfSignedCertificates) {
					URLConnectionUtils.enableSelfSignedSSLCertificates();

				} else {
					URLConnectionUtils.enableSSLCertificateCheck();
				}
			}

			HttpURLConnection connection = null;

			if (isUrlHttps) {
				HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
				https.setHostnameVerifier(URLConnectionUtils.DO_NOT_VERIFY);
				connection = https;
			} else {
				connection = (HttpURLConnection) url.openConnection();
			}

			connection.setRequestMethod("GET");

			final AuthHttpConnection result = new AuthHttpConnection(HttpMethod.GET, fileUrl, null, null, null);
			connection.setRequestProperty("X-AUTH", result.getAuthorisationHeader(false));
			connection.connect();

			File imageDir = new File(path);
			// Create image directory
			if (!imageDir.exists()) {
				imageDir.mkdirs();
			}

			File file = new File(imageDir, fileName);

			// this is where the file will be seen after the download
			FileOutputStream f = new FileOutputStream(file);

			// file input is from the url
			InputStream in = connection.getInputStream();

			// here's the download code
			byte[] buffer = new byte[1024];
			int len1 = 0;

			while ((len1 = in.read(buffer)) > 0) {

				f.write(buffer, 0, len1);
			}

			in.close();
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		};
	}

	/**
	 * Read a library {@link Cursor} into a {@link List}{@link LibraryItem &lt;LibraryItem&gt;} object.
	 * 
	 * @param context			the context
	 * @param libraryCursor		the library cursor
	 * @return					a {@link List}{@link LibraryItem &lt;LibraryItem&gt;} object
	 */
	public static List<LibraryItem> readLibraryCursorIntoLibraryList(Context context, Cursor libraryCursor) {
		List<LibraryItem> libraryItemList = new ArrayList<LibraryItem>();

		if (libraryCursor != null) {
			boolean hasNext = libraryCursor.moveToFirst();
			LibraryItem libraryItem;

			String uniqueId;
			String name;
			String description;
			String organisation;
			String fileUrl;
			String imagePath;
			String publishedDate;
			String localFolder;
			String version;
			String courseCode;
			String md5sum;

			String libraryColumnUniqueId = context.getString(R.string.library_column_uniqueId);
			String libraryColumnName = context.getString(R.string.library_column_name);
			String libraryColumnDescription = context.getString(R.string.library_column_description);
			String libraryColumnOrg = context.getString(R.string.library_column_org);
			String libraryColumnFileUrl = context.getString(R.string.library_column_fileUrl);
			String libraryColumnImagePath = context.getString(R.string.library_column_imagePath);
			String libraryColumnPublishedDate = context.getString(R.string.library_column_published_date);
			String libraryColumnFolder = context.getString(R.string.library_column_folder);
			String libraryColumnVersion = context.getString(R.string.library_column_version);
			String libraryColumnCourseCode = context.getString(R.string.library_column_course_code);
			String libraryColumnMD5sum = context.getString(R.string.library_column_md5sum);

			while (hasNext) {
				uniqueId = libraryCursor.getString(libraryCursor.getColumnIndex(libraryColumnUniqueId));
				name = libraryCursor.getString(libraryCursor.getColumnIndex(libraryColumnName));
				description = libraryCursor.getString(libraryCursor.getColumnIndex(libraryColumnDescription));
				organisation = libraryCursor.getString(libraryCursor.getColumnIndex(libraryColumnOrg));
				fileUrl = libraryCursor.getString(libraryCursor.getColumnIndex(libraryColumnFileUrl));
				imagePath = libraryCursor.getString(libraryCursor.getColumnIndex(libraryColumnImagePath));
				publishedDate = libraryCursor.getString(libraryCursor.getColumnIndex(libraryColumnPublishedDate));
				localFolder = libraryCursor.getString(libraryCursor.getColumnIndex(libraryColumnFolder));
				version = libraryCursor.getString(libraryCursor.getColumnIndex(libraryColumnVersion));
				courseCode = libraryCursor.getString(libraryCursor.getColumnIndex(libraryColumnCourseCode));
				md5sum = libraryCursor.getString(libraryCursor.getColumnIndex(libraryColumnMD5sum));

				libraryItem = new LibraryItem(uniqueId, name, description, organisation, fileUrl, imagePath, publishedDate, localFolder, version, courseCode, md5sum);
				libraryItemList.add(libraryItem);

				hasNext = libraryCursor.moveToNext();
			}
		}

		return libraryItemList;
	}
}