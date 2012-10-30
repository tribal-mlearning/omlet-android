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

package com.tribal.omlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.tribal.omlet.util.PackageLibraryAdapter;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.tribal.mobile.api.packages.LibraryItem;
import com.tribal.mobile.api.packages.LibraryItemComparator;
import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.api.packages.PackageManager;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.download.DownloadBroadcastActions;
import com.tribal.mobile.fragments.BaseFragment;
import com.tribal.mobile.model.Package;
import com.tribal.mobile.util.AsyncTaskHelper;
import com.tribal.mobile.util.DialogHelper;
import com.tribal.mobile.util.FileHelper;
import com.tribal.mobile.util.PackageHelper;
import com.tribal.mobile.util.PackageWorkerCompleted;
import com.tribal.mobile.util.PackageXmlWorkerTask;
import com.tribal.mobile.util.database.BaseDatabaseHelper;

/**
 * Library Fragment.
 * 
 * @author Jack Kierney
 */
public class LibraryFragment extends BaseFragment implements PackageWorkerCompleted, ActionMode.Callback {

	private MainTabActivity parentActivity;
	private Cursor libraryCursor;
	private String selectedPackagePath;
	private String selectedPackageName;
	private String selectedPackageUniqueId;
	private ActionMode actionMode = null;
	private ListView libraryListView = null;
	private boolean isDeleting = false;
	private PackageLibraryAdapter libraryCursorAdapter = null;

	private List<LibraryItem> libraryItemList;
	private ArrayList<String> selectedPackagePaths = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) {
			return null;
		}
		
		return inflater.inflate(R.layout.package_store, container, false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		parentActivity = (MainTabActivity) getActivity();
		selectedPackagePaths = new ArrayList<String>();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.PackageItemSelected)) {
			addBroadcastReceiver(BroadcastActions.PackageItemSelected);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageProcessingCompleted)) {
			addBroadcastReceiver(DownloadBroadcastActions.PackageProcessingCompleted);
		}

		if (!isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageDownloadCancelled)) {
			addBroadcastReceiver(DownloadBroadcastActions.PackageDownloadCancelled);
		}

		// Resume the action mode if something is already selected.
		if (actionMode == null && selectedPackagePaths.size() >= 1) {
			actionMode = parentActivity.startActionMode(this);
		}

		loadLibrary();

	}

	@Override
	public void onPause() {
		super.onPause();

		if (isBroadcastReceiverActionRegistered(BroadcastActions.PackageItemSelected)) {
			removeBroadcastReceiver(BroadcastActions.PackageItemSelected);
		}

		if (isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageProcessingCompleted)) {
			removeBroadcastReceiver(DownloadBroadcastActions.PackageProcessingCompleted);
		}

		if (isBroadcastReceiverActionRegistered(DownloadBroadcastActions.PackageDownloadCancelled)) {
			removeBroadcastReceiver(DownloadBroadcastActions.PackageDownloadCancelled);
		}
	}

	@Override
	protected void onBroadcastReceiveOverride(Intent intent) {
		String intentAction = intent.getAction();

		if (BroadcastActions.PackageItemSelected.equalsIgnoreCase(intentAction)) {
			if (intent.hasExtra(IntentParameterConstants.PackageFolderPath)) {
				String packageFolderPath = intent.getStringExtra(IntentParameterConstants.PackageFolderPath);
				manageSelectedItems(packageFolderPath);
			}
		} else if (DownloadBroadcastActions.PackageProcessingCompleted.equalsIgnoreCase(intentAction)) {
			loadLibrary();
		} else if (DownloadBroadcastActions.PackageDownloadCancelled.equalsIgnoreCase(intentAction)) {
			loadLibrary();
		}
	}

	private void manageSelectedItems(String packageFolderPath) {
		if (selectedPackagePaths.contains(packageFolderPath)) {

			selectedPackagePaths.remove(packageFolderPath);

			if (actionMode != null && selectedPackagePaths.size() == 0) {
				actionMode.finish();
			}
		} else {
			selectedPackagePaths.add(packageFolderPath);

			if (actionMode == null && selectedPackagePaths.size() >= 1) {
				actionMode = parentActivity.startActionMode(this);
			}
		}
	}

	private void loadLibrary() {
		if (parentActivity == null || parentActivity.getDatabaseHelper() == null) {
			return;
		}

		try {
			// get database helper
			BaseDatabaseHelper databaseHelper = parentActivity.getDatabaseHelper();

			if (this.libraryCursor != null) {
				this.libraryCursor.close();
				this.libraryCursor = null;
			}

			this.libraryCursor = databaseHelper.getMyLibrary();

			// read the library cursor
			this.libraryItemList = PackageHelper.readLibraryCursorIntoLibraryList(parentActivity, this.libraryCursor);

			// sort the list
			Collections.sort(this.libraryItemList, new LibraryItemComparator());

			// persist the library item list
			PackageManager.getInstance(getActivity().getApplicationContext(), getBaseActivity().getDatabaseHelper()).setLibraryItemList(this.libraryItemList);

			// get the list view
			libraryListView = (ListView) parentActivity.findViewById(R.id.package_store_list);

			// Set tab listener that will notify the activities that the tab has
			// changed and they need to be updated.
			libraryListView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

					// Set selected package path
					libraryCursor.moveToPosition(position);

					String state = libraryCursor.getString(libraryCursor.getColumnIndex(getString(R.string.library_column_state)));
					if (StringUtils.isNotEmpty(state) && state.equalsIgnoreCase(DownloadBroadcastActions.PackageUpdate)) {
						return;
					}

					selectedPackagePath = libraryCursor.getString(libraryCursor.getColumnIndex(getString(R.string.library_column_folder)));
					selectedPackageName = libraryCursor.getString(libraryCursor.getColumnIndex(getString(R.string.library_column_name)));
					selectedPackageUniqueId = libraryCursor.getString(libraryCursor.getColumnIndex(getString(R.string.library_column_uniqueId)));
					openPackage();
				}
			});

			registerForContextMenu(libraryListView);

			// set up the cursor adapter
			libraryCursorAdapter = new PackageLibraryAdapter(libraryListView.getContext(), this.libraryCursor, selectedPackagePaths);

			// assign the adapter to the list view
			libraryListView.setAdapter(libraryCursorAdapter);

			LinearLayout noCoursesLayout = (LinearLayout) (parentActivity.findViewById(R.id.package_store_no_courses));
			if (libraryCursor.getCount() == 0) {
				noCoursesLayout.setVisibility(0);
			} else {
				noCoursesLayout.setVisibility(8);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void openPackage() {
		// fire loading dialog
		parentActivity.showLoadingDialog();

		String packagePathFormatString = getString(R.string.external_storage_packages_path_format_string);
		String packagePath = Environment.getExternalStorageDirectory() + String.format(packagePathFormatString, selectedPackagePath);

		String packageXMLPath = String.format(getString(R.string.package_path_format_string), packagePath);

		File packageXml = new File(packageXMLPath);
		if (!packageXml.exists()) {
			DialogHelper.showAlertDialog(parentActivity, getString(R.string.packageErrorTitle), getString(R.string.packageFindXmlError));
		}

		LibraryItem libraryItem = new LibraryItem(selectedPackageUniqueId, selectedPackageName);

		PackageXmlWorkerTask task = new PackageXmlWorkerTask();
		AsyncTaskHelper.executeAsyncTask(task, packageXMLPath, packagePath, libraryItem, this);
	}

	private void deletePackage(String packageFolderPath) {
		String packagePathFormatString = getString(R.string.external_storage_packages_path_format_string);
		String packagePath = Environment.getExternalStorageDirectory() + String.format(packagePathFormatString, packageFolderPath);

		// Delete package if it exists and remove preferences
		File filedir = new File(packagePath);
		if (filedir.isDirectory()) {
			// Remove directory and all nodes under it
			FileHelper.deleteFilesRecursive(filedir, true);
		}

		// Remove Image
		String imagePath = Environment.getExternalStorageDirectory() + getString(R.string.external_storage_images_path);
		imagePath += ".png";

		File imageFile = new File(imagePath);
		if (imageFile.exists()) {
			FileHelper.deleteFilesRecursive(imageFile, true);
		}

		BaseDatabaseHelper dbHelper = parentActivity.getDatabaseHelper();
		if (dbHelper != null) {
			dbHelper.removeLibraryEntry(packageFolderPath);
		}

		// Remove native stored values
		dbHelper.deleteSettingWithObjectIdByCurrentUserId(packageFolderPath);

		// Update Download list to show this package as available for download again
		// send broadcast to add the package item to the queue
		PackageItem packageItem = new PackageItem();
		packageItem.setUniqueId(packageFolderPath);
		Intent resetPackageBroadcastIntent = new Intent();

		resetPackageBroadcastIntent.setAction(DownloadBroadcastActions.PackageReset);
		resetPackageBroadcastIntent.putExtra(IntentParameterConstants.PackageItem, packageItem);

		parentActivity.sendBroadcast(resetPackageBroadcastIntent);
	}

	@Override
	public void onPackageWorkerCompleted(Package packageObject, Object payload) {
		parentActivity.dismissLoadingDialog();

		if (packageObject == null) {
			DialogHelper.showAlertDialog(parentActivity, getString(R.string.packageErrorTitle), getString(R.string.packageParseError));
		} else {
			PackageManager packageManager = PackageManager.getInstance(getActivity().getApplicationContext(), getBaseActivity().getDatabaseHelper());

			// persist the package on the package helper
			packageManager.setCurrentlyOpenedPackage(packageObject);

			LibraryItem libraryItem = (LibraryItem) payload;

			// persist the library item on the package helper
			packageManager.setCurrentlyOpenedLibraryItemByUniqueId(libraryItem.getUniqueId());

			// open package
			parentActivity.openPackage(packageObject, libraryItem);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

		libraryCursor.moveToPosition(info.position);
		String state = libraryCursor.getString(libraryCursor.getColumnIndex(getString(R.string.library_column_state)));

		selectedPackageName = libraryCursor.getString(libraryCursor.getColumnIndex(getString(R.string.library_column_name)));
		selectedPackagePath = libraryCursor.getString(libraryCursor.getColumnIndex(getString(R.string.library_column_folder)));
		menu.setHeaderTitle(selectedPackageName);
		String[] menuItems = getResources().getStringArray(R.array.context_menu_library);

		for (int i = 0; i < menuItems.length; i++) {
			menu.add(Menu.NONE, i, i, menuItems[i]);
		}

		if (StringUtils.isNotEmpty(state) && state.equalsIgnoreCase(DownloadBroadcastActions.PackageUpdate)) {
			menu.setGroupEnabled(0, false);
		} else {
			menu.setGroupEnabled(0, true);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getTitle().toString().contentEquals("Open")) {
			openPackage();
		} else if (item.getTitle().toString().contentEquals("Delete")) {
			ArrayList<String> selectedPackagePathArray = new ArrayList<String>();
			selectedPackagePathArray.add(selectedPackagePath);

			deletePackages(selectedPackagePathArray, true);
		}

		return super.onContextItemSelected(item);
	}

	private void deletePackages(final List<String> packageIds, final boolean isContextMenuDelete) {
		AsyncTaskHelper.executeAsyncTask(new DeletePackageAsync(), packageIds, isContextMenuDelete);
	}

	public class DeletePackageAsync extends AsyncTask<Object, Void, Void> {
		private Boolean isContextMenuDelete;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			parentActivity.showLoadingDialog(getString(R.string.dialog_removing_course_message));
		}

		@Override
		protected Void doInBackground(Object... params) {

			@SuppressWarnings("unchecked")
			ArrayList<String> packageFolderPathList = (ArrayList<String>) params[0];

			isContextMenuDelete = (Boolean) params[1];

			for (String packageFolderPath : packageFolderPathList) {
				deletePackage(packageFolderPath);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			parentActivity.dismissLoadingDialog();

			isDeleting = false;

			// For context menu deletes we just need to refresh but for action
			// mode multiple deletes
			// we need to end the action mode.
			if (isContextMenuDelete) {
				loadLibrary();
			} else {
				actionMode.finish();
			}
		}
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		menu.add( "Delete")
				.setIcon(R.drawable.ic_delete)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, com.actionbarsherlock.view.MenuItem item) {

		if (item.getTitle() == "Delete") {
			isDeleting = true;
			deletePackages(selectedPackagePaths, false);
			
			// mode.finish();
		}

		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {

		actionMode = null;

		if (MainTabActivity.currentTab.equalsIgnoreCase(getString(R.string.packageLibraryTabId))) {

			if (!isDeleting) {
				selectedPackagePaths.clear();
			}

			loadLibrary();
		}
	}
}