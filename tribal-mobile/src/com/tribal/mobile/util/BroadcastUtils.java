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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IllegalFormatException;

import android.content.Context;

import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.download.DownloadBroadcastActions;

/**
 * Utility class to prefix broadcast actions with the application package to prevent cross-communication between apps that use the same mobile framework.
 * 
 * @author Jack Harrison
 */
public class BroadcastUtils {

	// These classes shouldn't contain any other fields than broadcast actions
	private static final Class<?>[] broadcastActionClasses = new Class<?>[] { DownloadBroadcastActions.class, BroadcastActions.class };

	/*
	 * Using reflection to loop through all the fields in the broadcast action constant classes
	 * and set their values to correspond to the applications package name. This is so two
	 * different apps using the tribal mobile api do not broadcast conflicting broadcast intents.
	 * Reflection is being used to prevent having to hard code all the actions into here, and
	 * they can still be static final fields.
	 */
	public static void initialiseBroadcastActions(final Context applicationContext) {

		// Loop through the classes that contain broadcast actions
		for (Class<?> clz : broadcastActionClasses) {

			// Loop through all the fields in that class
			for (Field field : clz.getFields()) {
				try {
					// Store the current accessibility state and make sure we can access the field
					boolean accessible = field.isAccessible();
					field.setAccessible(true);

					// Make sure we are modifying a static field or we will get an NPE
					if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
						// Make sure it is a String field we are modifying
						if (field.getType() == String.class) {
							// Update the value
							String oldValue = (String) field.get(null);
							String newValue = formatBroadcastString(applicationContext, oldValue);
							field.set(null, newValue);
						}
					}

					// Restore the old field accessibility state
					field.setAccessible(accessible);

				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static String formatBroadcastString(Context context, String broadcastString) {
		try {
			return String.format(broadcastString, context.getPackageName());
		} catch (IllegalFormatException e) {
			return broadcastString;
		}
	}
}
