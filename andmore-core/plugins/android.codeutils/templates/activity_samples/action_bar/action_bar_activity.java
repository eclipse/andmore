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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import #ManifestPackageName#.R;

public class #class_name# extends Activity {

    //reference to manipulate the action bar
    ActionBar actionBar = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.#layout_name#action_bar/ablayout.xml#);

        //get reference to the action bar
        actionBar = getActionBar();

        //set title and subtitle of the action bar
        actionBar.setTitle(getString(R.string.actionBarTitle));
        actionBar.setSubtitle(getString(R.string.actionBarSubtitle));
    }

    /**
     * Inflate the menu into the action bar.
     * */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate the xml menu into java code
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.#menu_name#action_bar/abmenu.xml#, menu);

        return true;
    }

    /**
     * Handle menu/action bar item selection.
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //Variable to check whether the menu item was handled.
        //If this method return false, then the system proceeds with normal menu processing. 
        boolean eventHandled = false;

        //Example of how to handle menu/action bar item selections. 
        //Adjust, insert and/or remove case statements for your menu items accordingly.
        switch (item.getItemId()){
            case android.R.id.home:
                //when app icon in action bar is clicked, show home/main activity
                Intent intent = new Intent(this, #ManifestPackageName#.#main_activity#.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                eventHandled = true;
                break;

            case R.id.menuItem1:
                Toast.makeText(this, getString(R.string.menuItem1),
                        Toast.LENGTH_SHORT).show();
                eventHandled = true;
                break;

            case R.id.menuItem2:
                Toast.makeText(this, getString(R.string.menuItem2),
                        Toast.LENGTH_SHORT).show();
                eventHandled = true;
                break;

            case R.id.menuItem3:
                Toast.makeText(this, getString(R.string.menuItem3),
                        Toast.LENGTH_SHORT).show();
                eventHandled = true;
                break;

            case R.id.menuItem4:
                Toast.makeText(this, getString(R.string.menuItem4),
                        Toast.LENGTH_SHORT).show();
                eventHandled = true;
                break;

            case R.id.menuItem5:
                Toast.makeText(this, getString(R.string.menuItem5),
                        Toast.LENGTH_SHORT).show();
                eventHandled = true;
                break;

            default:
                eventHandled = super.onOptionsItemSelected(item);
        }

        return eventHandled;
    }

}