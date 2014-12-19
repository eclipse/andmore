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

import android.app.ListActivity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import #ManifestPackageName#.R;

public class #class_name# extends ListActivity {

	private String[] listItems = null;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		//load list
		Resources res = getResources();
		listItems = res.getStringArray(R.array.sample_simple_list_items);

        // maps an array to TextViews
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, listItems));
    }
	
	@Override
	protected void onListItemClick(ListView lv, View v, int position, long id) {
		// your action here
		Toast.makeText(lv.getContext(), getString(R.string.sample_simple_list_selected) +
				lv.getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
	}
}
