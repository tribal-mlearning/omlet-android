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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.tribal.mobile.R;

/**
 * Utility class to help facilitate the creation and showing of standard alert dialogs.
 * 
 * @author Jon Brasted
 */
public class DialogHelper {
	
	/**
	 * Creates and shows an alert dialog. Dialog will be created with the given <code>context</code>, <code>title</code> and <code>message</code>.
	 * 
	 * @param context	the context
	 * @param title		the title
	 * @param message	the message
	 */
	public static void showAlertDialog(Context context, String title, String message){
		
		//show error dialog
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);

		String okText = context.getString(R.string.button_ok);
		
		alertDialog.setButton(okText, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		alertDialog.show();
	}
	
	/**
	 * Creates and shows an alert dialog. Dialog will be created with the given <code>context</code>, <code>title</code> and <code>message</code>.
	 * <code>DialogInterface.OnClickListener</code> can be given and will be attached to the OK button.
	 * 
	 * @param context	the context
	 * @param title		the title
	 * @param message	the message
	 * @param callback	the callback
	 */
	public static void showAlertDialog(Context context, String title, String message, DialogInterface.OnClickListener callback) {
		
		//show error dialog
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);

		String okText = context.getString(R.string.button_ok);
		
		alertDialog.setButton(okText, callback);

		alertDialog.show();
	}
}