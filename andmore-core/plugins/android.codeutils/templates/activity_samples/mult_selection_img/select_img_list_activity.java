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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import #ManifestPackageName#.R;

public class #class_name# extends ListActivity implements OnClickListener
{ 
	private ArrayList<Integer> selectedItems = new ArrayList<Integer>(); 
	private final String SELECTED_ITEM_KEY = "selected_items";
	public final String TEXT_KEY_1 = "title";
	public final String TEXT_KEY_2 = "description";
	public final String ITEM_ID = "id";
	public final String IMG_KEY = "img";
	
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.#layout_name#mult_selection_img/listviewmult.xml#);
        
        findViewById(R.id.button).setOnClickListener(this);
        
		// list data
        List<Map<String, Object>> resourceNames = new ArrayList<Map<String, Object>>();
        generateData(resourceNames);
        
        MyAdapter notes = new MyAdapter(
            this,
            resourceNames,
            R.layout.#layout_name#mult_selection_img/listrowmult.xml#,
            new String[] { TEXT_KEY_1,TEXT_KEY_2, IMG_KEY, ITEM_ID },
            new int[] { R.id.text1, R.id.text2, R.id.img},
            selectedItems);
        
        
        setListAdapter(notes);
    }

    private void generateData(List<Map<String, Object>> resourceNames)
    {
        //TODO here you will fill resourceNames with your own data
        
    	Map<String, Object> data;
    	int NUM_ITEMS = 50;
    	
    	for ( int i = 0; i <= NUM_ITEMS; i++ )
    	{
    		data = new HashMap<String, Object>();
    		data.put(ITEM_ID, i);
    		data.put(TEXT_KEY_1, getString(R.string.list_item) + " " + Integer.toString(i) );
    		data.put(TEXT_KEY_2, getString(R.string.description));
    		data.put(IMG_KEY, R.drawable.#drawable_name#mult_selection_img/listicon.png# );
    		resourceNames.add(data);
    	}
    }
   
    /*
     * Restores list selection
     */
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		selectedItems.addAll(state.getIntegerArrayList(SELECTED_ITEM_KEY));
	}

	/* 
	 * When the device is rotated, this activity is killed
	 * This method is called when activity is about to be killed and saves
	 * the current list selection
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putIntegerArrayList(SELECTED_ITEM_KEY, selectedItems);
	}

	/*
	 * Prints on screen the currently selected items
	 */
	public void onClick(View target)
    {
	    //TODO execute your action here
	    
    	StringBuilder strText = new StringBuilder();
    	strText.append(getString(R.string.selected));
    	
    	Collections.sort(selectedItems);
    	
    	boolean first = true;
    	for(Integer cur : selectedItems)
    	{
    		if(first)
    		{
    			strText.append(cur);
    			first = false;
    		}
    		else
    		{
    			strText.append(", " + cur);
    		}
    	}
    	
    	Toast.makeText(getApplicationContext(), strText.toString(), Toast.LENGTH_LONG).show();
    }
	
	class MyAdapter extends SimpleAdapter
	{
	    List<? extends Map<String, ?>> resourceNames;
	    OnItemClickListener listener = null;
	    ArrayList<Integer> selectedItems;
	    String[] strKeys;
	    int[] ids;
	    
	    public MyAdapter(Context context, List<? extends Map<String, ?>> data,
	            int resource, String[] from, int[] to, ArrayList<Integer> selectedItems) {
	        super(context, data, resource, from, to);
	        this.selectedItems = selectedItems;
	        resourceNames = data;
	        strKeys = from;
	        ids = to;
	    }

	    /* Returns a view to be added on the list
	    *  When we scroll the list, some items leave the screen becoming invisible to the user.
	    *  Since creating views is an expensive task, we'd rather recycle these not visible views, 
	    *  that are referenced by convertView, updating its fields values.*/
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        
	        // used to improve performance, since we call findViewById 
	        // only once for each created, but not recycled, view
	        ViewHolder holder;
	        
	        if(listener == null)
	            listener = new OnItemClickListener(selectedItems);
	        
	        //view to be recycled
	        if(convertView == null)
	        {
	            holder = new ViewHolder();
	            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.#layout_name#mult_selection_img/listrowmult.xml#, null);
	            
	            holder.tv1 = (TextView) convertView.findViewById(R.id.text1);
	            holder.tv2 = (TextView) convertView.findViewById(R.id.text2);
	            holder.img = (ImageView) convertView.findViewById(R.id.img);
	            holder.ckb = (CheckBox) convertView.findViewById(R.id.ckb);
	            
	            convertView.setTag(holder);
	        }
	        else
	        {
	            holder = (ViewHolder) convertView.getTag();
	        }
	        Map<String, ?> currentData = resourceNames.get(position);
	        
	        //updates list items values
	        holder.tv1.setText(currentData.get(strKeys[0]).toString());
	        holder.tv2.setText(currentData.get(strKeys[1]).toString());
	        holder.img.setImageResource((Integer) currentData.get(strKeys[2]));
	        holder.ckb.setChecked(selectedItems.contains((Integer) currentData.get(strKeys[3])));
	        
	        convertView.setId((Integer) currentData.get(strKeys[3]));
	        convertView.setOnClickListener(listener);
	        
	        return convertView;
	    }
	}

	/*
	 * Holds references to list items
	 */
	class ViewHolder 
	{
	    TextView tv1, tv2;
	    ImageView img;
	    CheckBox ckb;
	}

	/*
	 * Called when a list item is clicked
	 */
	class OnItemClickListener implements OnClickListener
	{
	    ArrayList<Integer> selectedItems;
	    
	    public OnItemClickListener(ArrayList<Integer> selectedItems)
	    {
	        this.selectedItems = selectedItems;
	    }
	    
	    public void onClick(View v) {
	        //handles list item click
	        CheckBox ckb = (CheckBox) v.findViewById(R.id.ckb);
	        boolean checked = ckb.isChecked();
	        //updates selected list
	        if(checked)
	        {
	            selectedItems.remove(new Integer(v.getId()));
	        }
	        else
	        {
	            selectedItems.add(v.getId());
	        }
	        
	        //update check box value
	        ckb.setChecked(!checked);
	    }   
	}
}

