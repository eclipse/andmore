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
import android.os.Bundle;
import android.widget.*;
import android.view.*;

import #ManifestPackageName#.R;

public class #class_name# extends Activity {

    private ViewGroup feature_1;
    private ViewGroup feature_2;
    private ViewGroup feature_3;
    private ViewGroup feature_4;
    private ViewGroup feature_5;
    private ViewGroup feature_6;

    /**
     * @see android.app.Activity#onCreate(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.#layout_name#dashboard_pattern/dashboard_layout.xml#);
        
        this.feature_1 = (ViewGroup) findViewById(R.id.feature_1);
        this.feature_2 = (ViewGroup) findViewById(R.id.feature_2);
        this.feature_3 = (ViewGroup) findViewById(R.id.feature_3);
        this.feature_4 = (ViewGroup) findViewById(R.id.feature_4);
        this.feature_5 = (ViewGroup) findViewById(R.id.feature_5);
        this.feature_6 = (ViewGroup) findViewById(R.id.feature_6);
        
        View.OnClickListener onClickListener = new DashboardClickListener();
        
        this.feature_1.setOnClickListener(onClickListener);
        this.feature_2.setOnClickListener(onClickListener);
        this.feature_3.setOnClickListener(onClickListener);
        this.feature_4.setOnClickListener(onClickListener);
        this.feature_5.setOnClickListener(onClickListener);
        this.feature_6.setOnClickListener(onClickListener);
        
    }
    
    private class DashboardClickListener implements View.OnClickListener{

        public void onClick(View v) {
            
            /*
             * TODO: Replace the code below with your business logic
             * 
             * You will probably open an activity, which can be done using a code like below:
             * startActivity(new Intent(v.getContext(), YourActivity.class));
             */
            
            String msg = "You selected Feature ";
            
            switch(v.getId()){
                case R.id.feature_1:
                    msg += "1";
                    break;
                case R.id.feature_2:
                    msg += "2";
                    break;
                case R.id.feature_3:
                    msg += "3";
                    break;
                case R.id.feature_4:
                    msg += "4";
                    break;
                case R.id.feature_5:
                    msg += "5";
                    break;
                case R.id.feature_6:
                    msg += "6";
                    break;
                default:
                    // none
            }
        
            Toast.makeText(v.getContext(), msg, Toast.LENGTH_SHORT).show();
        }
        
    }

}
