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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import #ManifestPackageName#.R;

public class #class_name# extends ListActivity implements OnClickListener
{ 
	private final String SELECTED_ITEM_KEY = "selected_items";
	public final String TEXT_KEY_1 = "title";
	public final String TEXT_KEY_2 = "description";
	public final String ITEM_ID = "id";
	public final String IMG_KEY = "img";
	public final String RADIO_KEY = "radio";
	private Integer selectedItem = -1; 
	private RadioButton selectedRadio;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.#layout_name#single_selection_img/listviewsingle.xml#);
		
		// list data
		List<Map<String, Object>> resourceNames =
			new ArrayList<Map<String, Object>>();

		generateData(resourceNames);
		
		MySingleAdapter notes = new MySingleAdapter(
				this,
				resourceNames,
				R.layout.#layout_name#single_selection_img/listrowsingle.xml#,
				new String[] { TEXT_KEY_1,TEXT_KEY_2, IMG_KEY, RADIO_KEY, ITEM_ID },
				new int[] { R.id.text1, R.id.text2, R.id.img1, R.id.radio});

		setListAdapter(notes);
	}

	/*
	 * Populate list
	 */
	private void generateData(List<Map<String, Object>> resourceNames)
	{
	    //TODO here you will fill resourceNames with your own data
		HashMap<String, Object> data;
		int NUM_ITEMS = 50;
		
		for (int i = 0; i <= NUM_ITEMS; i++)
		{
			data = new HashMap<String, Object>();
			data.put(ITEM_ID, i);
			data.put(TEXT_KEY_1, getString(R.string.list_item) + " " + Integer.toString(i));
			data.put(TEXT_KEY_2, getString(R.string.description));
			data.put(IMG_KEY, R.drawable.#drawable_name#single_selection_img/listiconsingle.png#);
			data.put(RADIO_KEY, false);
			resourceNames.add(data);
		}
	}

	/*
	 * Prints on screen the currently selected item
	 */
	public void printMessage()
	{
	    //TODO execute your action here
	    
		StringBuilder strText = new StringBuilder();
		if(!selectedItem.equals(-1))
		{
			strText.append(getString(R.string.selected) + " ");
			strText.append(selectedItem.toString());
		}
		else
		{
			strText.append(getString(R.string.noselection));
		}

		Toast.makeText(getApplicationContext(), strText.toString(), 
				Toast.LENGTH_SHORT).show();
	}

	public void onClick(View v) {

		//handles list item click
		RadioButton radio = (RadioButton) v.findViewById(R.id.radio);
		boolean checked = radio.isChecked();
		//updates selected list
		if(checked)
		{
			selectedRadio = null;
			selectedItem = -1;
		}
		else
		{
			if(!selectedItem.equals(-1))
			{
				selectedRadio.setChecked(false);
			}
			selectedItem = v.getId();
			selectedRadio = radio;
		}

		//update check box value
		radio.setChecked(!checked);
		printMessage();
	}

	public void setSelectedRadio(RadioButton selectedRadio)
	{
		this.selectedRadio = selectedRadio;
	}

	/*
	 * Restores list selection
	 */
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		selectedItem = state.getInt(SELECTED_ITEM_KEY, -1);
	}

	/* when the device is rotated, this activity is killed
	 * this method is called when activity is about to be shut down and saves
	 * the current list selection
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_ITEM_KEY, selectedItem);
	}

	public Integer getSelectedItem()
	{
		return selectedItem;
	}
	
	class MySingleAdapter extends SimpleAdapter
	{
	    List<? extends Map<String, ?>> resourceNames;
	    #class_name# context;   
	    String[] strKeys;
	    
	    public MySingleAdapter(#class_name# context, List<? extends Map<String, ?>> data,
	            int resource, String[] from, int[] to) {
	        super(context, data, resource, from, to);
	        this.context = context;
	        resourceNames = data;
	        strKeys = from;
	    }

	    /* Return a view to be added on the list
	    *  When we scroll the list, some items leave the screen becoming invisible to the user.
	    *  Since creating views is an expensive task, we'd  rather recycle these not visible views, 
	    *  that are referenced by convertView, updating its fields values.*/
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {

	        // used to improve performance, since we call findViewById 
	        // only once for each created, but not recycled, view 
	        ViewHolder holder;

	        //view to be recycled
	        if(convertView == null)
	        {
	            holder = new ViewHolder();
	            
	            convertView = LayoutInflater.from(parent.getContext()).inflate(
	                    R.layout.#layout_name#single_selection_img/listrowsingle.xml#, null);

	            holder.textView1 = (TextView) convertView.findViewById(R.id.text1);
	            holder.textView2 = (TextView) convertView.findViewById(R.id.text2);
	            holder.imgView = (ImageView) convertView.findViewById(R.id.img1);
	            holder.radioButton = (RadioButton) convertView.findViewById(R.id.radio);

	            convertView.setTag(holder);
	        }
	        else
	        {
	            holder = (ViewHolder) convertView.getTag();
	        }
	        Map<String, ?> currentData = resourceNames.get(position);

	        //updates list items values
	        holder.textView1.setText(currentData.get(context.TEXT_KEY_1).toString());
	        holder.textView2.setText(currentData.get(context.TEXT_KEY_2).toString());
	        holder.imgView.setImageResource((Integer) currentData.get(context.IMG_KEY));
	        holder.radioButton.setChecked(context.getSelectedItem().equals(
	                (Integer) currentData.get(context.ITEM_ID)));

	        if(holder.radioButton.isChecked())
	        {
	            context.setSelectedRadio(holder.radioButton);
	        }

	        convertView.setId((Integer) currentData.get(context.ITEM_ID));
	        convertView.setOnClickListener(context);

	        return convertView;
	    }
	}

	/*
	 * Holds references to list items
	 */
	class ViewHolder
	{
	    TextView textView1, textView2;
	    ImageView imgView;
	    RadioButton radioButton;
	}
}

