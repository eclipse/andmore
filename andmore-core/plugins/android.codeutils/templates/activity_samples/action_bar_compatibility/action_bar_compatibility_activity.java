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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import #ManifestPackageName#.R;

public class #class_name# extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //action bar substitutes the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.#layout_name#action_bar_compatibility/ablayout.xml#);
    }
   
    public void onActionBarItemSelected(View v){

        //Example of how to handle action bar item selections. 
        //Adjust, insert and/or remove case statements for your menu items accordingly.
        switch (v.getId()){
        //when app icon in action bar or action bar title is clicked, show home/main activity
        case R.id.applicationIcon: 
        case R.id.actionBarTitle:
            Intent intent = new Intent(this, #ManifestPackageName#.#main_activity#.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            break;
        case R.id.menuItem1:
            Toast.makeText(this, getString(R.string.actionItem1), Toast.LENGTH_SHORT).show();
            break;
        case R.id.menuItem2:
            Toast.makeText(this, getString(R.string.actionItem2), Toast.LENGTH_SHORT).show();
            break;
        case R.id.menuItem3:
            Toast.makeText(this, getString(R.string.actionItem3), Toast.LENGTH_SHORT).show();
            break;
        default:
        }
            
    } 
    
}