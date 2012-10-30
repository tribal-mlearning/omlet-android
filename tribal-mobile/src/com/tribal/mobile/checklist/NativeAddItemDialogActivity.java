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

package com.tribal.mobile.checklist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.tribal.mobile.R;
import com.tribal.mobile.base.IntentParameterConstants;

/**
 * Class to represent a native 'add item' dialog activity. Provides a single {@link EditText} input field.
 * 
 * @author Jon Brasted
 */
public class NativeAddItemDialogActivity extends Activity implements View.OnClickListener {
	/* Fields */
	public final static int NativeAddItemDialogActivity_OpenChecklistNativeAddItemDialog_ReturnIdentifier = 36278;
	
	private String phoneGapCallbackId = null;
	
	/* Methods */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// set content view
		setContentView(R.layout.checklist_add_item_dialog);
		
		// get phonegap callback id
		Intent intent = getIntent();
		
		if (intent.hasExtra(IntentParameterConstants.PhoneGapCallbackId)) {
			phoneGapCallbackId = intent.getStringExtra(IntentParameterConstants.PhoneGapCallbackId);
		}
		
		// get done button
		Button doneButton = (Button)findViewById(R.id.done_button);
		
		if (doneButton != null) {
			doneButton.setOnClickListener(this);
		}
		
		// get cancel button
		Button cancelButton = (Button)findViewById(R.id.cancel_button);
		
		if (cancelButton != null) {
			cancelButton.setOnClickListener(this);
		}
	}
	
	@Override
	public void onClick(View view) {
		int id = view.getId();
		
		if (R.id.done_button == id) {
			onDoneButtonClick();
		} else if (R.id.cancel_button == id) {
			onCancelButtonClick();
		}
	}
	
	private void onDoneButtonClick() {
		String checklistEntry = null;
		
		// get edit text field
		EditText editTextField = (EditText)findViewById(R.id.checklist_add_item_entry);
		
		if (editTextField != null) {
			checklistEntry = editTextField.getText().toString();
		}

		Intent intent = new Intent();
		intent.putExtra(IntentParameterConstants.PhoneGapCallbackId, phoneGapCallbackId);
		
		if (!TextUtils.isEmpty(checklistEntry)) {		
			intent.putExtra(IntentParameterConstants.StringData, checklistEntry);
			setResultForCallback(Activity.RESULT_OK, intent);
		} else {
			setResultForCallback(Activity.RESULT_CANCELED, intent);
		}
		
		finish();
	}
	
	private void onCancelButtonClick() {
		Intent intent = new Intent();
		intent.putExtra(IntentParameterConstants.PhoneGapCallbackId, phoneGapCallbackId);
		
		setResultForCallback(Activity.RESULT_CANCELED, intent);
		
		finish();
	}
	
	private void setResultForCallback(int resultCode, Intent data) {
		Activity parent = getParent(); 
		
		if (parent == null) {
			setResult(resultCode, data);
		} else {
			parent.setResult(resultCode, data);
		}
	}
}