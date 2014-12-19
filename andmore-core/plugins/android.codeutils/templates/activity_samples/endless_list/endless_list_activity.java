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
import java.util.List;
import android.app.Activity;
import android.app.LauncherActivity.ListItem;
import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import #ManifestPackageName#.R;

/**
 * Class which holds an example of a endless list. It means a list will be
 * displayed and it will always have items to be displayed. <br>
 * New data is loaded asynchronously in order to provide a good user experience.
 */
public class #class_name# extends ListActivity { 

    /**
     * Adapter for endless list.
     */
    private EndlessListAdapter arrayAdapter = null;

    /**
     * Variable which controls when new items start being fetched. For instance
     * you may want to start get element when the list have 5 elements left to
     * be displayed.
     */
    private int totalSizeToBe = 0;

    /**
     * Variable which controls when new items are being loaded. If this variable
     * is true, it means items are being loaded, otherwise it is set to false.
     */
    private boolean isLoading = false;

    /**
     * The number of elements which are retrieved every time the service is
     * called for retrieving elements.
     */
    private static final int BLOCK_SIZE = 15;

    /**
     * The number of elements left in the list when the asynchronous service
     * will be called.
     */
    private static final int LOAD_AHEAD_SIZE = 5;
    
    /**
     * The number of items added to the <i>totalSizeToBe</i> field.
     */
    private static final int INCREMENT_TOTAL_MINIMUM_SIZE = 15;
    
    /**
     * Property to save the number of items already loaded
     */
    private static final String PROP_ITEM_COUNT = "item_count";
    
    /**
     * Property to save the top most index of the list
     */
    private static final String PROP_TOP_ITEM = "top_list_item";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        arrayAdapter = new EndlessListAdapter(this, R.layout.#layout_name#endless_list/listrow.xml#, new ArrayList<ListElement>());

        // download asynchronously initial list
        Downloaditems downloadAction = new Downloaditems();
        downloadAction.execute(new Integer[] {
                0, BLOCK_SIZE });

        setListAdapter(arrayAdapter);
        getListView().setOnScrollListener(new EndlessListScrollListener());
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(PROP_ITEM_COUNT, arrayAdapter.getCount());
        for (int i = 0; i < arrayAdapter.getCount(); i++) {
            outState.putSerializable(Integer.toString(i),
                    arrayAdapter.getItemAt(i));
        }
        outState.putInt(PROP_TOP_ITEM, getListView().getFirstVisiblePosition());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        int count = state.getInt(PROP_ITEM_COUNT);
        for (int i = 0; i < count; i++) {
            arrayAdapter.add((ListElement) state.get(Integer.toString(i)));
        }
        getListView().setSelection(state.getInt(PROP_TOP_ITEM));
    }

    @Override
    protected void onListItemClick(ListView lv, View v, int position, long id) {
        // your action here
        Toast.makeText(
                lv.getContext(),
                getString(R.string.selected_element_message,
                        arrayAdapter.getItemAt(position).text),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * This method represents a service which takes a long time to be executed.
     * To simulate it, it is inserted a lag of 1 second. <br>
     * This method basically creates a <i>cache</i> number of
     * {@link ListElement} and returns it. It creates {@link ListElement}s with
     * text higher than <i>itemNumber</i>.
     * 
     * @param itemNumber
     *            Basic number to create other elements.
     * @param numberOfItemsToBeCreated
     *            Number of items to be created.
     * 
     * @return Returns the created list of {@link ListElement}s.
     */
    private List<ListElement> retrieveItems(Integer itemNumber, int numberOfItemsToBeCreated) {
        List<ListElement> results = new ArrayList<ListElement>();
        try {
            // wait for 1 second in order to simulate the long service
            Thread.sleep(1000);
            // create items
            for (int i = 0; i <= numberOfItemsToBeCreated; i++) {
                String itemToBeAdded = getString(R.string.list_item_number,
                        (itemNumber + i));
                results.add(new ListElement(itemToBeAdded, R.drawable.listicon));
            }
        } catch (InterruptedException e) {
            // treat exception here
        }
        return results;
    }

    /**
     * Listener which handles the endless list. It is responsible for
     * determining when the long service will be called asynchronously.
     */
    class EndlessListScrollListener implements OnScrollListener {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {

            // load more elements if there are LOAD_AHEAD_SIZE left in the list to be displayed
            boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount
                    - LOAD_AHEAD_SIZE;

            /*
             * Add one more condition: only get more results in case the list achieves
             * a minimum size. This is necessary in order to avoid that this method is called
             * each time the condition above is reached and the scroll is pressed. 
             */
            if (loadMore && totalSizeToBe <= totalItemCount) {
                totalSizeToBe += INCREMENT_TOTAL_MINIMUM_SIZE;
                // call service
                Downloaditems downloadAction = new Downloaditems();
                downloadAction.execute(new Integer[] {
                        totalItemCount, BLOCK_SIZE });
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            // do nothing
        }
    }

    /**
     * Asynchronous job call. This class is responsible for calling
     * the long service and managing the <i>isLoading</i> flag.
     */
    class Downloaditems extends AsyncTask<Integer, Void, List<ListElement>> {

        // indexes constants
        private static final int ITEM_NUMBER_INDES = 0;
        private static final int NUMBER_OF_ITEMS_TO_BE_CREATED_INDEX = 1;
        
        @Override
        protected void onPreExecute() {
            // flag loading is being executed
            isLoading = true;
        }

        @Override
        protected List<ListElement> doInBackground(Integer... params) {

            // execute the long service
            return retrieveItems(params[ITEM_NUMBER_INDES], params[NUMBER_OF_ITEMS_TO_BE_CREATED_INDEX]);
        }

        @Override
        protected void onPostExecute(List<ListElement> result) {
            arrayAdapter.setNotifyOnChange(true);
            for (ListElement item : result) {
                // it is necessary to verify whether the item was already added
                // because this job is called many times asynchronously
                synchronized (arrayAdapter) {
                    if (!arrayAdapter.contains(item)) {
                        arrayAdapter.add(item);
                    }
                }
            }
            // flag the loading is finished
            isLoading = false;
        }
    }

    /**
     * Adapter which handles the list be be displayed.
     */
    class EndlessListAdapter extends ArrayAdapter<ListElement> {

        private final Activity context;
        private final List<ListElement> items;
        private final int rowViewId;

        /**
         * Instantiate the Adapter for an Endless List Activity.
         * 
         * @param context {@link Activity} which holds the endless list.
         * @param rowviewId Identifier of the View which holds each row of
         * the List.
         * @param items Initial set of items which are added to list being displayed.
         */
        public EndlessListAdapter(Activity context, int rowviewId, List<ListElement> items) {
            super(context, rowviewId, items);
            this.context = context;
            this.items = items;
            this.rowViewId = rowviewId;
        }

        /**
         * Check whether a {@link ListItem} is already in this adapter.
         * 
         * @param item Item to be verified whether it is in the adapter.
         * 
         * @return Returns <code>true</code> in case the {@link ListElement} is
         * in the adapter, <code>false</code> otherwise.
         */
        public boolean contains(ListElement item) {
            return items.contains(item);
        }
        
        /**
         * Get a {@link ListElement} at a certain position.
         * 
         * @param index Position where the {@link ListElement} is retrieved.
         * 
         * @return Returns the {@link ListElement} give a certain position.
         */
        public ListElement getItemAt(int index) {
            return items.get(index);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ImageView imageView;
            TextView textView;

            View rowView = convertView;
            
            /*
             * We inflate the row using the determined layout. Also,
             * we fill the necessary data in the text and image views. 
             */
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(rowViewId, null, true);
            textView = (TextView) rowView.findViewById(R.id.text01);
            imageView = (ImageView) rowView.findViewById(R.id.img01);
            textView.setText(items.get(position).text);
            imageView.setImageResource(items.get(position).imageId);

            /*
             * If we reached the last position of the list and the loading
             * operation is still being performed, set the loading message
             * instead the normal value.
             * 
             * Moreover, we modify the layout in order to center the loading message.
             */
            if (isLoading && position == items.size() - 1) {
                textView.setText(R.string.loading_message);
                
                // wrap content of the text view in order to center it
                LayoutParams layoutParameters = (LayoutParams) textView.getLayoutParams();
                layoutParameters.width = LayoutParams.WRAP_CONTENT;
                
                // set image to the center, the text field will go along
                imageView.setImageResource(android.R.drawable.progress_indeterminate_horizontal);
                LinearLayout linearLayout = (LinearLayout) rowView.findViewById(R.id.vw01);
                linearLayout.setGravity(Gravity.CENTER);
            }

            return rowView;
        }
    }
}