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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import #ManifestPackageName#.R;

public class #class_name# extends ListActivity
{ 
	private final String TEXT_KEY = "text";
	private final String IMG_KEY = "img";

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.#layout_name#img_text_list/listview.xml#);

		//adds listener to list view
		ListView listView = getListView();
        listView.setOnItemClickListener(new OnItemClickListener(){
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Toast.makeText(arg1.getContext(), getString(R.string.selected) + " " + arg2 , Toast.LENGTH_SHORT).show();
            }
        });
		
		// list data
		List<Map<String, Object>> resourceNames =
			new ArrayList<Map<String, Object>>();

		generateData(resourceNames);

		//adapter that will build the list items
		SimpleAdapter adapter = new SimpleAdapter(
				this,
				resourceNames,
				R.layout.#layout_name#img_text_list/listrow.xml#,
				new String[] { TEXT_KEY, IMG_KEY },
				new int[] { R.id.text01, R.id.img01 });

		setListAdapter(adapter);
	}
	
	private void generateData(List<Map<String, Object>> resourceNames)
	{
		// number of list items
	    int NUM_ITEMS = 50;
		Map<String, Object> data;

		for ( int i = 0; i <= NUM_ITEMS; i++ )
		{
			data = new HashMap<String, Object>();
			data.put(TEXT_KEY, getString(R.string.list_item) + " " + Integer.toString(i));
			data.put(IMG_KEY, R.drawable.#drawable_name#img_text_list/listicon.png# );
			resourceNames.add(data);
		}
	}
}