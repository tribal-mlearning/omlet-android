package com.tribal.mobile.image;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import com.tribal.mobile.net.URLConnectionUtils;
import com.tribal.mobile.preferences.PrivateSettingsKeys;
import com.tribal.mobile.util.NativeSettingsHelper;
import com.tribal.mobile.util.ServiceLayerExceptionHelper;
import com.tribal.mobile.util.resources.DrawableResourceLookups;
import com.tribal.mobile.util.resources.ResourceHelper;
import com.tribal.mobile.util.resources.ResourceItemType;

/**
 * Class to facilitate downloading drawables on a background thread and facilitate caching.
 * Taken from http://stackoverflow.com/a/7861011, which is based on Ben's solution found at http://negativeprobability.blogspot.co.uk/2011/08/lazy-loading-of-images-in-listview.html
 * Not sure what license the code is under but I assume it is Apache 2.
 */
public class DrawableBackgroundDownloader {

	private final Map<String, SoftReference<Drawable>> mCache = new HashMap<String, SoftReference<Drawable>>();
	private final LinkedList<Drawable> mChacheController = new LinkedList<Drawable>();
	private ExecutorService mThreadPool;
	private final Map<ImageView, String> mImageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());

	public static int MAX_CACHE_SIZE = 80;
	public int THREAD_POOL_SIZE = 3;

	private Context context;

	/**
	 * Constructor
	 */
	public DrawableBackgroundDownloader(Context context) {
		mThreadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		this.context = context;
	}

	/**
	 * Clears all instance data and stops running threads
	 */
	public void Reset() {
		ExecutorService oldThreadPool = mThreadPool;
		mThreadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		oldThreadPool.shutdownNow();

		mChacheController.clear();
		mCache.clear();
		mImageViews.clear();
	}

	public void loadDrawable(final String url, final ImageView imageView, Drawable placeholder) {
		mImageViews.put(imageView, url);
		Drawable drawable = getDrawableFromCache(url);

		// check in UI thread, so no concurrency issues
		if (drawable != null) {
			// Log.d(null, "Item loaded from mCache: " + url);
			if (drawable instanceof BitmapDrawable) {
				Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

				imageView.setImageBitmap(bitmap);
			}
		} else {
			imageView.setImageDrawable(placeholder);
			queueJob(url, imageView, placeholder);
		}
	}

	public void loadDrawableWithPlaceholder(final String url, final ImageView imageView) {
		int placeholderResourceId = ResourceHelper.getResourceIdByName(imageView.getContext(), ResourceItemType.drawable, DrawableResourceLookups.Placeholder);

		Drawable placeholder = null;
		
		if (placeholderResourceId > 0) {		
			placeholder = imageView.getContext().getResources().getDrawable(placeholderResourceId);
		}
		
		loadDrawable(url, imageView, placeholder);
	}

	public Drawable fetchDrawable(String url) {
		return downloadDrawable(url);
	}

	private Drawable getDrawableFromCache(String url) {
		if (mCache.containsKey(url)) {
			return mCache.get(url).get();
		}

		return null;
	}

	private synchronized void putDrawableInCache(String url, Drawable drawable) {
		int chacheControllerSize = mChacheController.size();
		if (chacheControllerSize > MAX_CACHE_SIZE) {
			mChacheController.subList(0, MAX_CACHE_SIZE / 2).clear();
		}

		mChacheController.addLast(drawable);
		mCache.put(url, new SoftReference<Drawable>(drawable));

	}

	@SuppressLint("HandlerLeak")
	private void queueJob(final String url, final ImageView imageView,
			final Drawable placeholder) {
		/* Create handler in UI thread. */
		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String tag = mImageViews.get(imageView);
				if (tag != null && tag.equals(url)) {
					if (imageView.isShown()) {
						if (msg.obj != null) {
							imageView.setImageDrawable((Drawable) msg.obj);
						} else {
							imageView.setImageDrawable(placeholder);
							// Log.d(null, "fail " + url);
						}
					}
				}
			}
		};

		mThreadPool.submit(new Runnable() {
			@Override
			public void run() {
				final Drawable bmp = downloadDrawable(url);
				// if the view is not visible anymore, the image will be ready
				// for next time in cache
				if (imageView.isShown()) {
					Message message = Message.obtain();
					message.obj = bmp;
					// Log.d(null, "Item downloaded: " + url);

					handler.sendMessage(message);
				}
			}
		});
	}

	private Drawable downloadDrawable(String url) {
		try {
			InputStream is = getInputStream(url);

			Drawable drawable = Drawable.createFromStream(is, url);
			putDrawableInCache(url, drawable);
			return drawable;

		} catch (Exception e) {
			ServiceLayerExceptionHelper.getInstance().processException(e, context);
		}

		return null;
	}

	private InputStream getInputStream(String urlString) throws Exception {
		URL url = new URL(urlString);
		URLConnection connection;

		boolean isUrlHttps = url.getProtocol().toLowerCase().equals("https"); 
		final boolean acceptSelfSignedCertificates = NativeSettingsHelper.getInstance(context).checkAndGetPrivateBooleanSetting(PrivateSettingsKeys.ACCEPT_SSL_SELF_SIGNED_CERTS, false);
		
		if (isUrlHttps) {
			if (acceptSelfSignedCertificates) {
				URLConnectionUtils.enableSelfSignedSSLCertificates();
				
			} else {
				URLConnectionUtils.enableSSLCertificateCheck();
			}
		}

		if (isUrlHttps) {
			HttpsURLConnection https = (HttpsURLConnection)url.openConnection();
            https.setHostnameVerifier(URLConnectionUtils.DO_NOT_VERIFY);
            connection = https;
		} else {
			connection = url.openConnection();
		}

		connection.setUseCaches(true);
		connection.connect();
		InputStream response = connection.getInputStream();

		return response;
	}
}