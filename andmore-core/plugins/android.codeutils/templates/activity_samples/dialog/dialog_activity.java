/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package #package_name#;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import #ManifestPackageName#.R;

public class #class_name# extends Activity implements OnClickListener{
	
	private int dialogId = 1;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.#layout_name#dialog/dialoglayout.xml#);
        
        Button btn = (Button) findViewById(R.id.dialog_btn);
        btn.setOnClickListener(this);
        showDialog(dialogId);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	// Items to be displayed
    	Resources res = getResources();
    	final CharSequence[] items = res.getStringArray(R.array.sample_dialog_items);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.sample_pick_item));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
            	// Add your action here
                Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_LONG).show();
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }
	
	public void onClick(View arg0) {
		showDialog(dialogId);
	}
}