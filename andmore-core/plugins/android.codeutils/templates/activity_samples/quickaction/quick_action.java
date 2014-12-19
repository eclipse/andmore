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
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import #ManifestPackageName#.R;

public class #class_name# extends Activity {
	
    /*
     * Pixels added to QuickAction window growing window area to avoid fading edges
     */
    private static final int MARGIN_ADJUSTMENT = 8;
    
    /*
     * Pixels added to QuickAction window growing window area to avoid fading edges
     */
    private static final int ACTION_ITEM_SIZE = 64;
    
    /*
     * The current quick action window (to dismiss on application pause/rotation)
     */
    private PopupWindow currentQuickActionWindow = null;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.#layout_name#quickaction/quick_action_activity.xml#);
	}
	
	/**
	 * Create an action item
	 * @param name the action name
	 * @param imageResId the image ID to be set in the action item
	 * @return
	 */
	private View createAction (final String name, int imageResId) {
		View action = getLayoutInflater().inflate(R.layout.#layout_name#quickaction/quick_action_item.xml#, null);
		ImageView image = (ImageView) action.findViewById(R.id.action_image);
		TextView text = (TextView) action.findViewById(R.id.action_name);
		text.setText(name);
		
		/*
		 * Set image dimensions (Density Independent Pixels)
		 */
		action.setMinimumHeight((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ACTION_ITEM_SIZE, getResources().getDisplayMetrics()));
		action.setMinimumWidth((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ACTION_ITEM_SIZE, getResources().getDisplayMetrics()));
		image.setImageResource(imageResId);
		
		/*
		 *	Set action being performed when item action is clicked 
		 */
		action.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
			    /*
			     * TODO: Place your action code here
			     */
				Toast.makeText(#class_name#.this, "You have selected " + name , Toast.LENGTH_SHORT).show();
			}
		});
		return action;
	}
	
	public void openQuickActionWindow(View target) {
		/*
		 * Get the layout of quick action window
		 */
		ViewGroup quickActionLayout = (ViewGroup) getLayoutInflater()
				.inflate(R.layout.#layout_name#quickaction/quick_action_grid.xml#, null);
		
		/*
		 * Get the quick action bar
		 */
		ViewGroup quickActionBar = (ViewGroup)quickActionLayout.findViewById(R.id.quick_action_bar);
		
		/*
		 * Add the actions (change to your own needs)
		 */
		quickActionBar.addView(createAction("Action 1", R.drawable.#drawable_name#quickaction/icon.png#));
		quickActionBar.addView(createAction("Action 2", R.drawable.#drawable_name#quickaction/icon.png#));
		quickActionBar.addView(createAction("Action 3", R.drawable.#drawable_name#quickaction/icon.png#));
		quickActionBar.addView(createAction("Action n", R.drawable.#drawable_name#quickaction/icon.png#));
		
		/*
		 * Create the window and set the content
		 * It is important to set the right context due to touch events
		 */
		currentQuickActionWindow = new PopupWindow(getApplicationContext());
		currentQuickActionWindow.setContentView(quickActionLayout);

		/*
		 * The popup must be touchable (to touch actions),
		 * focusable (if you have more actions than space) and,
		 * outside touchable (to be able to close the window)
		 */
		currentQuickActionWindow.setTouchable(true);
		currentQuickActionWindow.setFocusable(true);
		currentQuickActionWindow.setOutsideTouchable(true);
		
		/*
		 * Make background transparent
		 */
		currentQuickActionWindow.setBackgroundDrawable(new BitmapDrawable());
		
		/*
		 * Close Quick Action window when clicked outside
		 */
		currentQuickActionWindow.setTouchInterceptor(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
					currentQuickActionWindow.dismiss();
					return true;
				}
				return false;
			}
		});
		
		/*
		 * Measure the Quick Action window.
		 * Add MARGIN_ADJUSTMENT density independent pixels to the size in order to not show scrolls and left some edge spaces
		 * Check layout files to check
		 */
		quickActionLayout.measure(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		currentQuickActionWindow.setWidth(quickActionLayout.getMeasuredWidth() + (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MARGIN_ADJUSTMENT, getResources().getDisplayMetrics()));
		currentQuickActionWindow.setHeight(quickActionLayout.getMeasuredHeight() + (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MARGIN_ADJUSTMENT, getResources().getDisplayMetrics()));
		
		/*
		 * Open the Quick Action window
		 */
		currentQuickActionWindow.showAsDropDown(target);
		
		/*
		 * Animate action list
		 * This must be manually called
		 */
		Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.#anim_name#quickaction/quickaction_list_anim.xml#);
		quickActionBar.startAnimation(animation);
	}
	
	@Override
    protected void onPause() {
	    /*
	     * Dismiss quick action if still opened
	     */
        if (currentQuickActionWindow != null) {
            currentQuickActionWindow.dismiss();
        }
        super.onPause();
    }
}