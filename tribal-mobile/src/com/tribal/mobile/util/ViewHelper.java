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

package com.tribal.mobile.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.tribal.mobile.util.resources.ResourceHelper;
import com.tribal.mobile.util.resources.ResourceItemType;
import com.tribal.mobile.util.resources.StringResourceLookups;

/**
 * Utility class to set typeface on all {@link TextView} objects in a {@link View}.
 * 
 * @author Jack Kierney and Jon Brasted
 */
public class ViewHelper {
	private static String fontPath = null;
	
	/**
	 * Set type face on all {@link TextView} objects in the specified {@link View}. 
	 * 
	 * @param view	the view
	 */
	public static void setTypeFace(View view) {
		Context context = view.getContext();
		
		if (TextUtils.isEmpty(fontPath)) {
			int fontPathResourceId = ResourceHelper.getResourceIdByName(context, ResourceItemType.string, StringResourceLookups.FontPath);
			fontPath = context.getString(fontPathResourceId);
			
			if (TextUtils.isEmpty(fontPath)) {
				return;
			}
		}
		
		Typeface typeface = Typefaces.get(context, fontPath);
		
		if (view instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup)view;
			setTypeFaceRecursively(viewGroup, typeface);

		} else if (view instanceof TextView) {
			setTypeface((TextView)view, typeface);
		}
	}

	/**
	 * Set the type face recursively in the specified {@link ViewGroup}.
	 * 
	 * @param viewGroup		the view group		
	 * @param typeface		the typeface
	 */
	private static void setTypeFaceRecursively(ViewGroup viewGroup, Typeface typeface) {
		for (int i = 0; i < viewGroup.getChildCount(); i++) {

			View childView = viewGroup.getChildAt(i);

			if (childView instanceof ViewGroup) {
				setTypeFaceRecursively((ViewGroup)childView, typeface);
			
			} else if (childView instanceof TextView) {
				TextView textView = (TextView) childView;
				
				setTypeface(textView, typeface);
			}
		}
	}
	
	/**
	 * Set the type face to a specific {@link Typeface} in the specified {@link TextView}.
	 * 
	 * @param textView		the text view
	 * @param typeface		the typeface
	 */
	private static void setTypeface(TextView textView, Typeface typeface) {
		Typeface currentTypeFace = textView.getTypeface();
		
		if (currentTypeFace != null) {				
			int currentTypeFaceStyle = currentTypeFace.getStyle();
			textView.setTypeface(typeface, currentTypeFaceStyle);
		} else {
			textView.setTypeface(typeface);
		}
		
		textView.setTypeface(typeface);
	}
	
	/**
	 * Update a view's opacity to look faded or not faded. 
	 * 
	 * @param view		the view
	 * @param enabled	whether the view should look faded or not. If set to true, the view will have an opacity of 1. If not, it will have an opacity of 0.45.
	 */
	public static void setAlphaFadeEnabled(View view, boolean enabled) {
		if (!enabled) {
			AlphaAnimation anim = new AlphaAnimation(1, (float) 0.45);
			anim.setDuration(1);
			anim.setFillAfter(true);
			view.startAnimation(anim);
		} else {
			AlphaAnimation anim = new AlphaAnimation((float)0.45, 1);
			anim.setDuration(1);
			anim.setFillAfter(true);
			view.startAnimation(anim);
		}
	}
}