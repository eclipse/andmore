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

import android.app.TabActivity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;

import #ManifestPackageName#.R;

/* 
 * NOTES:
 *  - TabActivity class is deprecated since API level 13.
 *  - New applications are recommended to use fragments.
 *    
 * */

public class #class_name# extends TabActivity implements TabContentFactory{
	
	private static int numTabs = 4;	
	
	private static final String TAB_TITLE = "Tab";
	private static final String TAB_CONTENT = "content.";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TabHost host = getTabHost();
		
		for(int i = 0; i < numTabs; i++)
		{
			TabSpec spec = host.newTabSpec(TAB_TITLE + i);
			Resources res = getResources();                          
            Drawable icon = res.getDrawable(R.drawable.tabicon);
            //set tab text and icon
            spec.setIndicator(TAB_TITLE + " " + i, icon);
			spec.setContent(this);
			host.addTab(spec);
		}
	}

	public View createTabContent(String tag) {
		TextView tv = new TextView(this);
		tv.setText(tag + " " + TAB_CONTENT);
		return tv;
	}
}
