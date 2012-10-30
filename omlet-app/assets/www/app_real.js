/**
 * Android user agents.
 */
var IS_ANDROID_21 = navigator.appVersion.indexOf("Android 2.1") != -1;
var IS_ANDROID_22 = navigator.appVersion.indexOf("Android 2.2") != -1;
var IS_ANDROID_2P = navigator.appVersion.indexOf("Android 2.") != -1;

/**
 * Workaround method to correct some horizontal sizing issues that can occur inside the Android web browser.
 * Tribal code will automatically try and call this function once the web page has loaded.
 */
function checkContentSize() {
	if (!IS_ANDROID) return;
	
	var bodyElement = document.getElementsByTagName('body')[0];
	
	if (bodyElement != null) {
		if (bodyElement.offsetWidth != window.innerWidth) {
			bodyElement.style.width = window.innerWidth + "px";
		}
		if (bodyElement.offsetHeight != window.innerHeight) {
			bodyElement.style.height = window.innerHeight + "px";
		}
	}
}

// invoke checkContentSize
checkContentSize();

/**
 * @fileOverview
 * ClientPlugin class.
 */

/**
 * Object to hold the list of native store types.
 */
var MFStoreType = {};
/**
 * Global native store type
 * @const
 * @type {string}
 */
MFStoreType.GLOBAL = 'global';
/**
 * Content-specific native store type
 * @const
 * @type {string}
 */
MFStoreType.SPECIFIC = 'specific';

/**
 * Object to hold the available user settings keys.
 */
var MFSettingsKeys = {};
/**
 * User settings: is tracking enabled.
 * @const
 * @type {string}
 */
MFSettingsKeys.TRACKING_ENABLED = 'app_tracking_enabled';
/**
 * User settings: the tracking interval related to gps information.
 * @const
 * @type {string}
 */
MFSettingsKeys.GPS_INTERVAL = 'app_gps_interval';
/**
 * User settings: the sync interval.
 * @const
 * @type {string}
 */
MFSettingsKeys.SYNC_INTERVAL = 'app_sync_interval';
/**
 * User settings: the enabled network to sync.
 * @const
 * @type {string}
 */
MFSettingsKeys.SYNC_NETWORK = 'app_sync_network';
/**
 * User settings: the enabled data use setting (connectivity).
 * @const
 * @type {string}
 */
MFSettingsKeys.DATA_USE = "app_data_use";
/**
 * User settings: the application version.
 * @const
 * @type {string}
 */
MFSettingsKeys.VERSION = 'app_version';

/**
 * Mobile Framework Plugin 
 * @constructor
 */
var ClientPlugin = function() {
};

/**
 * Opens a native menu on the application.
 *
 * @param menuItemId The full path to the menu item, starting with the package id. For example: shell.root.1.2
 */		
ClientPlugin.prototype.openMenuItem = function(menuItemId, callback, errorCallback) {
	PhoneGap.exec(callback, errorCallback, 'ClientPlugin', 'openMenuItem', [menuItemId]);
};

/**
 * Opens a resource on the application.
 * The resource called can be contained in the current package or another package, but is referenced with its full name in both cases.
 *
 * @param menuItemId The full path to the resource, starting with the package id. For example: shell.root.1.2
 */		
ClientPlugin.prototype.openResource = function(resourceId, callback, errorCallback) {
	PhoneGap.exec(callback, errorCallback, 'ClientPlugin', 'openResource', [resourceId]);
};

ClientPlugin.prototype.initialize = function(callback) {
	PhoneGap.exec(callback, null, 'ClientPlugin', 'initialize', []);
};

ClientPlugin.prototype.terminate = function(callback) {
	PhoneGap.exec(callback, null, 'ClientPlugin', 'terminate', []);
};

/**
 * Sets a value for a key on the native storage. Native storage is provided by the framework. It consists in a way to store key/value pairs. Like the HTML5 DB, this kind of storage is cleaned up automatically if the user removes the application. This storage should be used specially if you want to share data between the HTML content and native side of the application, otherwise we highly recommend use HTML5 DB. There are two ways of store data on the native storage, globally and content-specific.
 * <p>
 *   <strong>Globally</strong>
 *   It's global on the application. The key can be retrieved from any content in any part of the system, as well as in any part of the native code. You must take care that the key can be used by other content developer. Below you can find the API calls to store and retrieve key/value pairs.
 *   <ul>
 *     <li>clientPlugin.setValue(MFStoreType.GLOBAL, key, value, callback);</li>
 *   </ul>
 * </p>
 * <p>
 *   <strong>Content-specific</strong>
 *   It's connected to the current resource/menu-item. Below you can find the API calls to store and retrieve key/value pairs.
 *   <ul>
 *     <li>clientPlugin.setValue(MFStoreType.SPECIFIC, key, value, callback);</li>
 *   </ul>
 * </p>
 * 
 * @param type		The type of the native storage.
 * @param key		The key name.
 * @param value		The value.
 * @param callback	The callback function to confirm the operation.
 */		
ClientPlugin.prototype.setValue = function(type, key, value, callback) {
	PhoneGap.exec(callback, null, 'ClientPlugin', 'setValue', [type, key, value]);
}

/**
 * Gets a value for a key from the native storage. Native storage is provided by the framework. It consists in a way to store key/value pairs. Like the HTML5 DB, this kind of storage is cleaned up automatically if the user removes the application. This storage should be used specially if you want to share data between the HTML content and native side of the application, otherwise we highly recommend use HTML5 DB. There are two ways of store data on the native storage, globally and content-specific.
 *
 * <p>
 *   <strong>Globally</strong>
 * It's global on the application. The key can be retrieved from any content in any part of the system, as well as in any part of the native code. You must take care that the key can be used by other content developer. Below you can find the API calls to store and retrieve key/value pairs.
 *   <ul>
 *     <li>clientPlugin.getValue(MFStoreType.GLOBAL, key, callback);</li>
 *   </ul>
 * </p>
 * <p>
 *   <strong>Content-specific</strong>
 * It's connected to the current resource/menu-item. Below you can find the API calls to store and retrieve key/value pairs.
 *   <ul>
 *     <li>clientPlugin.getValue(MFStoreType.SPECIFIC, key, callback);</li>
 *   </ul>
 * </p>
 * 
 * @param type		The type of the native storage.
 * @param key		The key name.
 * @param callback	The callback function. It will receive an object with the value.
 */		
ClientPlugin.prototype.getValue = function(type, key, callback) {
	PhoneGap.exec(callback, null, 'ClientPlugin', 'getValue', [type, key]);
}

/**
 * Retrieves the current user username.
 *
 * @param callback	The callback function. It will receive an object with the username.
 */
ClientPlugin.prototype.getUserUsername = function(callback) {
	PhoneGap.exec(callback, null, 'ClientPlugin', 'getUserUsername', []);
}

/**
 * Retrieves the device information.
 *
 * @param callback	The callback function. It will receive an object with the device information.
 */
ClientPlugin.prototype.getDeviceInfo = function(callback) {
	PhoneGap.exec(callback, null, 'ClientPlugin', 'getDeviceInfo', []);
}

/**
 * Forces sync.
 *
 * @param callback	The callback function. It will receive an object with the device information.
 */
ClientPlugin.prototype.sync = function(callback) {
	PhoneGap.exec(callback, null, 'ClientPlugin', 'sync', []);
}

/**
 * Tracks a content-specific thing. This method must be called by content developers to track anything they want. Everything tracked by this method will be connected to the current object id (resource or menu-item).
 *
 * @param info		The track information. It's a string value, can contain anything you want.
 * @param callback	The callback function. It will receive an object with the device information.
 */
ClientPlugin.prototype.track = function(sender, info, callback) {
	PhoneGap.exec(callback, null, 'ClientPlugin', 'track', [sender, info]);
}

/**
 * Log out the current user and redirect the user to the login screen.
 * 
 * @param callback	The callback function.
 */
ClientPlugin.prototype.logout = function(callback) {
	PhoneGap.exec(callback, null, 'ClientPlugin', 'logout', []);
}

/**
 * Retrieves the local path root for the currently opened course.
 * 
 * Callback will return a JSON object with the following key and value pairs:
 *  courseId		The course id for the currently opened course.
 *  localPathRoot	The full local path to the course directory root. E.g. /mnt/sdcard/.../{uniqueCourseFolderId}
 * 
 * Error callback will return a JSON object with the following key and value pairs:
 *  error			An error string to determine why the native method call failed.
 * 
 * @param callback		The callback function.
 * @param errorCallback	The error callback function.
 */
ClientPlugin.prototype.getCurrentCourseLocalPathRoot = function(callback, errorCallback) {
	PhoneGap.exec(callback, errorCallback, 'ClientPlugin', 'getCurrentCourseLocalPathRoot', []);
}

/**
 * Initializes a temporary folder for the currently opened course.
 * If the folder already exists, the folder will be cleared. If it does not exist, it will be created.
 * 
 * Callback will return a JSON object with the following key and value pairs:
 * 	tempFolderPath	The full local path to the temporary folder in the course directory. E.g. /mnt/sdcard/.../{uniqueCourseFolderId}/temp
 * 
 * Error callback will return a JSON object with the following key and value pairs:
 *  error			An error string to determine why the native method call failed.
 * 
 * @param callback		The callback function.
 * @param errorCallback	The error callback function.
 */
ClientPlugin.prototype.initializeCurrentCourseLocalTempFolder = function(callback, errorCallback) {
	PhoneGap.exec(callback, errorCallback, 'ClientPlugin', 'initializeCurrentCourseLocalTempFolder', []);
}

/**
 * Clears the temporary folder for the currently opened course.
 * If the folder already exists, the folder will be cleared and removed. If the folder does not exist, an error will occur.
 * 
 * Callback will not return a value and will be invoked when the operation has finished.
 * 
 * Error callback will return a JSON object with the following key and value pairs:
 *  error			An error string to determine why the native method call failed.
 * 
 * @param callback		The callback function.
 * @param errorCallback	The error callback function.
 */
ClientPlugin.prototype.clearCurrentCourseLocalTempFolder = function(callback, errorCallback) {
	PhoneGap.exec(callback, errorCallback, 'ClientPlugin', 'clearCurrentCourseLocalTempFolder', []);
}

if (typeof(PhoneGap) != 'undefined') {
	PhoneGap.addConstructor(function() {
		if (!window.plugins) {
			window.plugins = {};
		}
		
		clientPlugin = new ClientPlugin();
		window.plugins.clientPlugin = clientPlugin;
	});
}