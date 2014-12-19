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
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import #ManifestPackageName#.R;

public class #class_name# extends Activity implements OnItemSelectedListener{
	
	private String[] listItems;
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.#layout_name#dropdown_list/ddlistlayout.xml#);

		Resources res = getResources();
		listItems = res.getStringArray(R.array.sample_ddlist_items);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, listItems);
		
		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setPrompt(getString(R.string.sample_ddlist_pick_item));
		spinner.setOnItemSelectedListener(this);
	}
	
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		
		Toast.makeText(arg0.getContext(), getString(R.string.sample_ddlist_select) +
				arg0.getItemAtPosition(arg2).toString(), Toast.LENGTH_SHORT).show();
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		//do nothing
	}
}
