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

package com.tribal.mobile.util.resources;

import android.content.Context;

/**
 * Utility class that provides functionality for the Mobile Framework to look up resources that it does not have access to until runtime.
 * 
 * @author Jon Brasted
 */
public class ResourceHelper {
	/* Methods */
	
	public static int getResourceIdByName(Context context, String resourceItemType, String name) {
		Class<?> r = null;
		int id = 0;
		
		try {
			r = Class.forName(context.getPackageName() + ".R");
			
			Class<?>[] classes = r.getClasses();
			Class<?> desiredClass = null;
			
			for (int index = 0; index < classes.length; index++) {
				if (classes[index].getName().split("\\$")[1].equals(resourceItemType)) {
		            desiredClass = classes[index];
		            break;
		        }
			}
			
			if (desiredClass != null) {
				id = desiredClass.getField(name).getInt(desiredClass);
			}
		} catch (ClassNotFoundException e) {
		    e.printStackTrace();
		} catch (IllegalArgumentException e) {
		    e.printStackTrace();
		} catch (SecurityException e) {
		    e.printStackTrace();
		} catch (IllegalAccessException e) {
		    e.printStackTrace();
		} catch (NoSuchFieldException e) {
		    e.printStackTrace();
		}

		return id;
	}
}
