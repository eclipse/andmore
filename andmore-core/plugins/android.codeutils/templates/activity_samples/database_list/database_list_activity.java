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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle; 
import android.view.Gravity;
import android.widget.TableLayout; 
import android.widget.TableRow; 
import android.widget.TextView;
import android.widget.Toast;

#imports#

import #ManifestPackageName#.R;

public class #class_name# extends Activity {
	/* Called when the activity is first created. */

	private final int ONE = 1;
	private TableLayout tbl = null;
	private TableRow row = null;

	private android.widget.TableRow.LayoutParams rowParams = 
		new android.widget.TableRow.LayoutParams( 
				android.widget.TableRow.LayoutParams.#FILL_PARENT_LPARAM#, 
				android.widget.TableRow.LayoutParams.WRAP_CONTENT);

	private android.widget.TableRow.LayoutParams tableParams =
		new android.widget.TableRow.LayoutParams( 
				android.widget.TableRow.LayoutParams.#FILL_PARENT_LPARAM#, 
				android.widget.TableRow.LayoutParams.WRAP_CONTENT);

	//set database location
	public String DB_PATH = "/data/data/#package_name#/databases/";
	public String DB_NAME = "#dbName#";

	//column names
	#constColumnsNames#

	//table name
	private final String SQL_TABLE_NAME = "#tableNameLowerCase#";
	private boolean DISTINCT_ROWS = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.#layout_name#database_list/databaselayout.xml#);

		tbl = (TableLayout) findViewById(R.id.myTableLayout);
		tbl.setBackgroundColor(Color.BLACK);
		
		SQLiteDatabase checkDB = null;
		#getReadableDatabase# 		

		try
		{			
			loadTable(checkDB);
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}

		if(checkDB != null)
		{
			checkDB.close();
		}
	}

	public void loadTable(SQLiteDatabase sqliteDatabase) {  
		Cursor cursor = sqliteDatabase.query(DISTINCT_ROWS, SQL_TABLE_NAME,  
				new String[] { #columsNames# } /*columns*/, COL_1 + "=" + COL_1 /*selection*/, 
				null /*selection args*/, null /*group by*/,  
				null /*having*/, null /*order by*/, null /*limit*/);  

		if (cursor != null) {  
			this.startManagingCursor(cursor);

			String[] columns = cursor.getColumnNames();
			createHeader(columns);

			while(cursor.move(ONE))
			{
				#columnGetValues#				

				TableRow row = new TableRow(getApplicationContext());
				row.setLayoutParams(rowParams);

				if(cursor.getPosition() % 2 == 1) {
					row.setBackgroundColor(Color.LTGRAY);
				}
				else {
					row.setBackgroundColor(Color.WHITE);
				}
				
				#columnAddRows#	

				tbl.addView(row, tableParams);
			}
		}  
	}

	//adds new value to row
	private void addToRow(Object obj, TableRow row)
	{
		TextView txt = new TextView(this);
		if(obj != null)
			txt.setText(obj.toString());
		else
			txt.setText("");
		txt.setLayoutParams(rowParams);
		txt.setTextColor(Color.BLACK);
		txt.setGravity(Gravity.LEFT);
		txt.setPadding(5, 1, 1, 1);
		row.addView(txt);
	}
	
	//creates table header
	private void createHeader(String[] names)
	{
		row = new TableRow(getApplicationContext());
		row.setLayoutParams(rowParams); 
		row.setBackgroundColor(Color.GRAY);

		for(String column : names)
		{
			TextView txt = new TextView(this);
			txt.setText(column.toUpperCase());
			txt.setLayoutParams(rowParams);
			txt.setWidth(80);
			txt.setTextColor(Color.BLACK);
			txt.setTypeface(Typeface.DEFAULT_BOLD);
			txt.setGravity(Gravity.LEFT);
			//adds new field to row
			row.addView(txt);
		}
		
		//adds row to table
		tbl.addView(row, tableParams);
	}		
}