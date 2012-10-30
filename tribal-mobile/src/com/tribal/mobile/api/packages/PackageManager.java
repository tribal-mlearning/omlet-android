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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.os.Looper;

import com.tribal.mobile.Framework;
import com.tribal.mobile.R;
import com.tribal.mobile.model.BaseContentItem;
import com.tribal.mobile.model.MenuItem;
import com.tribal.mobile.model.MenuItemType;
import com.tribal.mobile.model.Package;
import com.tribal.mobile.net.ConnectivityMode;
import com.tribal.mobile.phonegap.MFSettingsKeys;
import com.tribal.mobile.util.AsyncTaskHelper;
import com.tribal.mobile.util.ConnectivityUtils;
import com.tribal.mobile.util.NativeSettingsHelper;
import com.tribal.mobile.util.PackageHelper;
import com.tribal.mobile.util.PackageWorkerCompleted;
import com.tribal.mobile.util.PackageXmlWorkerTask;
import com.tribal.mobile.util.database.BaseDatabaseHelper;
import com.tribal.mobile.util.resources.ResourceHelper;
import com.tribal.mobile.util.resources.ResourceItemType;
import com.tribal.mobile.util.resources.StringResourceLookups;

/**
 * Singleton utility class that keeps track of the current open {@link Package}, opened {@link Package} objects, current open {@link LibraryItem} and list of {@link LibraryItem} objects. 
 * 
 * @author Jon Brasted
 */
public class PackageManager implements GetPackageCompleted {
	/* Static Fields */
	
	private static PackageManager instance;
	
	/* Fields */
	
	private Context context;
	private BaseDatabaseHelper databaseHelper;
	
	private Package currentlyOpenedPackage;
	private Map<String, Package> openedPackages;
	
	private LibraryItem currentlyOpenedLibraryItem;
	private List<LibraryItem> libraryItemList;
	
	/* Properties */
	
	public static PackageManager getInstance(Context context, BaseDatabaseHelper databaseHelper) {
		if (instance == null) {
			instance = new PackageManager(context, databaseHelper);
		}
		
		return instance;
	}
	
	/**
	 * Returns the current opened {@link Package}.
	 * 
	 * @return	the current opened {@link Package}.
	 */
	public Package getCurrentlyOpenedPackage() {
		return currentlyOpenedPackage;
	}

	/**
	 * Sets the current opened {@link Package}.
	 * 
	 * @param	the current opened {@link Package}
	 */
	public void setCurrentlyOpenedPackage(Package packageObject) {
		currentlyOpenedPackage = packageObject;
		
		String packageId = packageObject.getId();
		
		if (openedPackages.containsKey(packageId)) {
			openedPackages.remove(packageId);
		}

		// add to openedPackages
		openedPackages.put(packageId, packageObject);
	}
	
	/**
	 * Returns the current opened {@link LibraryItem}.
	 * 
	 * @return	the current opened {@link LibraryItem}
	 */
	public LibraryItem getCurrentlyOpenedLibraryItem() {
		return currentlyOpenedLibraryItem;
	}
	
	/**
	 * Sets the current opened {@link LibraryItem}.
	 * 
	 * @param	the current opened {@link LibraryItem}
	 */
	public void setCurrentlyOpenedLibraryItem(LibraryItem libraryItem) {
		currentlyOpenedLibraryItem = libraryItem;
	}
	
	/**
	 * Sets the current opened {@link LibraryItem} by the specified unique id.
	 * 
	 * @param	the unique id
	 */
	public void setCurrentlyOpenedLibraryItemByUniqueId(String uniqueId) {
		for (LibraryItem libraryItem : libraryItemList) {
			if (libraryItem.getUniqueId().equalsIgnoreCase(uniqueId)) {
				currentlyOpenedLibraryItem = libraryItem;
				break;
			}
		}
	}
	
	/**
	 * Returns the list of {@link LibraryItem} objects.
	 * 
	 * @return	the list of {@link LibraryItem} objects
	 */
	public List<LibraryItem> getLibraryItemList() {
		return libraryItemList;
	}
	
	/**
	 * Sets the list of {@link LibraryItem} objects.
	 * 
	 * @param libraryItemList	the list of {@link LibraryItem} objects
	 */
	public void setLibraryItemList(List<LibraryItem> libraryItemList) {
		this.libraryItemList = libraryItemList;
	}
	
	/* Constructor */
	
	public PackageManager(Context context, BaseDatabaseHelper databaseHelper) {
		this.context = context;
		this.databaseHelper = databaseHelper;
		
		openedPackages = new HashMap<String, Package>();
		libraryItemList = new ArrayList<LibraryItem>();
	}
	
	/* Methods */
	
	private void updateLibraryItemList() {
		// get cursor
		Cursor libraryCursor = databaseHelper.getMyLibrary();
		
		// read cursor
		List<LibraryItem> libraryItemList = PackageHelper.readLibraryCursorIntoLibraryList(context, libraryCursor);
		
		// close the cursor
		if (!libraryCursor.isClosed()) {
			libraryCursor.close();
		}
		
		// set the library item list
		setLibraryItemList(libraryItemList);
	}
	
	/**
	 * Returns a {@link PackageItemResult} containing the {@link MenuItem} object based on the specified item id via the specified {@link PackageItemRetrieveCompleted} callback. 
	 * 
	 * @param itemId			the item id
	 * @param callback			the {@link PackageItemRetrieveCompleted} callback
	 * @param phonegapCallback	the PhoneGap callback
	 */
	public void getMenuItem(final String itemId, final PackageItemRetrieveCompleted callback, final String phonegapCallback) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// get the package
				getPackage(MenuItemType.menu, itemId, callback, phonegapCallback);
			}
		};
		
		Thread thread = new Thread(runnable);
		thread.start();
	}
	
	/**
	 * Returns a {@link PackageItemResult} containing the {@link BaseContentItem} object based on the specified item id via the specified {@link PackageItemRetrieveCompleted} callback. 
	 * 
	 * @param itemId			the item id
	 * @param callback			the {@link PackageItemRetrieveCompleted} callback
	 * @param phonegapCallback	the PhoneGap callback
	 */
	public void getResourceItem(final String itemId, final PackageItemRetrieveCompleted callback, final String phonegapCallback) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// get the package
				getPackage(MenuItemType.link, itemId, callback, phonegapCallback);
			}
		};
		
		Thread thread = new Thread(runnable);
		thread.start();
	}
	
	/**
	 * Returns a {@link PackageItemResult} containing the {@link PackageItem} object based on the specified item id via the specified {@link PackageItemRetrieveCompleted} callback. 
	 * 
	 * @param itemId			the item id
	 * @param callback			the {@link PackageItemRetrieveCompleted} callback
	 * @param phonegapCallback	the PhoneGap callback
	 */
	private void getPackage(MenuItemType itemType, String itemId, final PackageItemRetrieveCompleted callback, final String phonegapCallback) {
		// get first index of period
		int firstPeriodIndex = itemId.indexOf(".");
		
		String uniqueId = null;
		
		if (firstPeriodIndex >= 0) {
			// split the string
			uniqueId = itemId.substring(0, firstPeriodIndex);
		} else {
			// check that there is currently opened package
			Package currentPackage = getCurrentlyOpenedPackage();
			
			if (currentPackage != null) {			
				uniqueId = currentPackage.getId();
			} else {
				// No course has been loaded yet.
				// NB: This should never happen.
				PackageItemResult result = new PackageItemResult("It appears that no course has been loaded yet.");
				
				// invoke callback
				if (callback != null) {
					callback.onPackageItemRetrieveCompleted(result, phonegapCallback);
				}
				
				return;
			}
		}
		
		// ensure that we have the latest library item list
		updateLibraryItemList();
		
		// check whether the package has already been opened
		if (openedPackages.containsKey(uniqueId)) {
			for (LibraryItem libraryItem : this.libraryItemList) {
				if (libraryItem.getUniqueId().equalsIgnoreCase(uniqueId)) {
					// get the package item
					onGetPackageCompleted(openedPackages.get(uniqueId), itemType, itemId, callback, phonegapCallback, libraryItem);
				}
			}
		} else {
			// search the library
			
			boolean foundLibraryItem = false;
			
			for (LibraryItem libraryItem : this.libraryItemList) {
				if (libraryItem.getUniqueId().equalsIgnoreCase(uniqueId)) {
					// found the library item
					foundLibraryItem = true;
					
					// open the package
					openPackageAsync(itemType, itemId, libraryItem.getLocalFolder(), this, callback, phonegapCallback, libraryItem);
					
					break;
				}
			}
			
			if (!foundLibraryItem) {
				// TODO: Put error strings into a resources file
				// Error: could not find menu item
				
				// check connectivity
				String syncConnectivityModeString = NativeSettingsHelper.getInstance(context).checkAndGetNativeSetting(MFSettingsKeys.DATA_USE);			
				ConnectivityMode connectivityMode = Enum.valueOf(ConnectivityMode.class, syncConnectivityModeString);
				
				boolean canConnect = ConnectivityUtils.canConnect(context, connectivityMode);
				
				final String tempUniqueId = uniqueId;
				
				if (canConnect) {
					Looper.prepare();
					
					Framework.getClient().getPackageCatalogue(new PackageCatalogueRetrieved() {
						
						@Override
						public void onCatalogueRetrieved(Catalogue catalogue) {
							PackageItemResult result = null;
							
							// TODO: Get the package catalogue and check whether it contains the unique id.
							if (catalogue != null) {
								List<PackageItem> packageList = catalogue.getPackages();
								
								if (packageList.size() > 0) {
									for (PackageItem packageItem : packageList) {
										if (tempUniqueId.equalsIgnoreCase(packageItem.getUniqueId())) {
											result = new PackageItemResult(packageItem);
											break;
										}
									}
								}
							}
							
							if (result == null) {
								result = new PackageItemResult("Referenced course is not on the device and has not been published to the m-Learning Suite.");
							}
							
							// invoke callback
							if (callback != null) {
								callback.onPackageItemRetrieveCompleted(result, phonegapCallback);
							}
						}
					});
					
					Looper.loop();
				} else {
					PackageItemResult result = new PackageItemResult("Referenced course has not been downloaded to the device and the device is offline.");
					
					// invoke callback
					if (callback != null) {
						callback.onPackageItemRetrieveCompleted(result, phonegapCallback);
					}
				}
			}
		}
	}
	
	/**
	 * Provides functionality to clear the current opened {@link Package} and current opened {@link LibraryItem}.
	 */
	public void clear() {
		// clear currently opened items
		currentlyOpenedPackage = null;
		currentlyOpenedLibraryItem = null;
	}
	
	private void openPackageAsync(MenuItemType itemType, String itemId, String packagePath, GetPackageCompleted getPackageCompletedCallback, PackageItemRetrieveCompleted packageItemRetrieveCompletedCallback,
									String phonegapCallback, Object payload) {
		// create worker object
		GetPackageWorker worker = new GetPackageWorker(itemType, itemId, getPackageCompletedCallback, packageItemRetrieveCompletedCallback, phonegapCallback);
		
		int externalStoragePackagesPathFormatStringResourceId = ResourceHelper.getResourceIdByName(context, ResourceItemType.string, StringResourceLookups.ExternalStoragePackagesPathFormatString);
		String externalStoragePackagesPathFormatString = context.getString(externalStoragePackagesPathFormatStringResourceId);
		String externalStoragePackagesPath = String.format(externalStoragePackagesPathFormatString, packagePath);
		String selectedPackagePath = Environment.getExternalStorageDirectory() + externalStoragePackagesPath;

		String packageXMLPath = String.format(context.getString(R.string.package_path_format_string), selectedPackagePath);

		PackageXmlWorkerTask task = new PackageXmlWorkerTask();
		AsyncTaskHelper.executeAsyncTask(task, packageXMLPath, selectedPackagePath, payload, worker);
	}
	
	/**
	 * Provides an implementation for {@link GetPackageCompleted#onGetPackageCompleted(Package, MenuItemType, String, PackageItemRetrieveCompleted, String, Object) onGetPackageCompleted}.
	 */
	@Override
	public void onGetPackageCompleted(Package packageItem, MenuItemType itemType, String itemId, PackageItemRetrieveCompleted callback, String phonegapCallback, Object payload) {
		String packageId = packageItem.getId();
		
		// persist the package item
		if (openedPackages.containsKey(packageId)) {
			openedPackages.remove(packageId);
		}
		
		openedPackages.put(packageId, packageItem);
		
		PackageItemResult result = null;
		
		if (itemType == MenuItemType.menu) {		
			// get item
			MenuItem menuItem = packageItem.findMenuItemByPath(itemId);
		
			if (menuItem != null) {
				result = new PackageMenuItemResult(menuItem, null);
			} else {
				// Error: could not find menu item
				int referencedCourseDownloadedMenuItemNotExistErrorStringResourceId = ResourceHelper.getResourceIdByName(context, ResourceItemType.string, StringResourceLookups.ReferencedCourseDownloadedMenuItemNotExist);
				String referencedCourseDownloadedMenuItemNotExistErrorString = context.getString(referencedCourseDownloadedMenuItemNotExistErrorStringResourceId);
				
				result = new PackageMenuItemResult(null, referencedCourseDownloadedMenuItemNotExistErrorString);
			}
		} else if (itemType == MenuItemType.link) {
			// get item
			BaseContentItem resourceItem = packageItem.findContentItemById(itemId);
		
			if (resourceItem != null) {
				result = new PackageResourceItemResult(resourceItem, null);
			} else {
				// Error: could not find resource item
				int referencedCourseDownloadedResourceItemNotExistErrorStringResourceId = ResourceHelper.getResourceIdByName(context, ResourceItemType.string, StringResourceLookups.ReferencedCourseDownloadedResourceItemNotExist);
				String referencedCourseDownloadedResourceItemNotExistErrorString = context.getString(referencedCourseDownloadedResourceItemNotExistErrorStringResourceId);
				
				result = new PackageResourceItemResult(null, referencedCourseDownloadedResourceItemNotExistErrorString);
			}
		}
		
		// invoke callback
		if (callback != null) {
			callback.onPackageItemRetrieveCompleted(result, phonegapCallback);
		}
	}
	
	/**
	 * Class to mediate between {@link PackageXmlWorker} and {@link GetPackageCompleted} through async callbacks.
	 * 
	 * @author Jon Brasted
	 */
	private class GetPackageWorker implements PackageWorkerCompleted {
		/* Fields */
		
		private MenuItemType itemType;
		private String itemId;
		private GetPackageCompleted getPackageCompletedCallback;
		private PackageItemRetrieveCompleted packageItemRetrieveCompletedCallback;
		private String phonegapCallback;

		/* Constructor */
		
		public GetPackageWorker(MenuItemType itemType, String itemId, GetPackageCompleted getPackageCompletedCallback, PackageItemRetrieveCompleted packageItemRetrieveCompletedCallback, String phonegapCallback) {
			this.itemType = itemType; 
			this.itemId = itemId;
			this.getPackageCompletedCallback = getPackageCompletedCallback;
			this.packageItemRetrieveCompletedCallback = packageItemRetrieveCompletedCallback; 
			this.phonegapCallback = phonegapCallback; 
		}

		/* Methods */
		
		@Override
		public void onPackageWorkerCompleted(Package packageObject, Object payload) {
			// invoke callback
			getPackageCompletedCallback.onGetPackageCompleted(packageObject, itemType, itemId, packageItemRetrieveCompletedCallback, phonegapCallback, payload);
		}
	}
}
