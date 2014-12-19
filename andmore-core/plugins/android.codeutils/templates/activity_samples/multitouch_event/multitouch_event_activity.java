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
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import #ManifestPackageName#.R;

public class #class_name# extends Activity {
	private ImageView imgView;

	//available operations
	private final int MOVE = 1;
	private final int ZOOM = 2;
	private int action = 0;

	PointF startPoint = new PointF();
	PointF centerPoint = new PointF();
	
	//holds previous state information
	private double prevDist = 0;
	private Matrix curMatrix = new Matrix();
	private Matrix auxMatrix = new Matrix();

	//Minimum distance between fingers 
	private Double MIN_DISTANCE; 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.#layout_name#multitouch_event/multitouchlayout.xml#);
		
		// convert dip measurements to pixels. Screen independent value.
		final float scale = getResources().getDisplayMetrics().density;
		MIN_DISTANCE = (double) ( 15.0f * scale + 0.5f );
		
		//loads image view
		imgView = (ImageView) findViewById(R.id.img01);
		imgView.setImageMatrix(curMatrix);
		imgView.setImageResource(R.drawable.#drawable_name#multitouch_event/multitouch.jpg#);
		imgView.setVisibility(View.VISIBLE);
		imgView.setOnTouchListener(new OnTouchListener(){

			public boolean onTouch(View v, MotionEvent event) {
				ImageView view = (ImageView)v;

				//get the 8 bits that represents the action itself 
				int eventType = event.getAction() & MotionEvent.ACTION_MASK;
				
				switch (eventType) {
				//first touch
				case MotionEvent.ACTION_DOWN:
					action = MOVE;
					auxMatrix.set(curMatrix);
					startPoint.set(event.getX(), event.getY());
					break;
				//second touch
				case MotionEvent.ACTION_POINTER_DOWN:
					prevDist = distance(event);
					if (prevDist > MIN_DISTANCE) {
						action = ZOOM;
						auxMatrix.set(curMatrix);
						//used to center the image
						centerPoint = mean(event);
					}
					break;
				//movement event
				case MotionEvent.ACTION_MOVE:
					//using one finger we drag the image
					if (action == MOVE) {
						curMatrix.set(auxMatrix);
						//moves the image
						curMatrix.postTranslate(event.getX() - startPoint.x,
								event.getY() - startPoint.y);
					}
					//using two fingers, zoom in or out and rotate the image
					else if (action == ZOOM) 
					{
						double curDist = distance(event);
						if (curDist > MIN_DISTANCE) {
							curMatrix.set(auxMatrix);
							//relation between fingers distance
							Double scale = curDist / prevDist;
							//resize image keeping its center position
							curMatrix.postScale(scale.floatValue(), scale.floatValue(), 
									centerPoint.x, centerPoint.y);
						}
					}
					break;
				}
				//apply changes
				view.setImageMatrix(curMatrix);
				return true; 
			}});
	}

	/*distance between the two fingers of dual touch event*/
	private double distance(MotionEvent event) {
		float dy = event.getY(1) - event.getY(0);
		float dx = event.getX(1) - event.getX(0);
		dx *= dx;
		dy *= dy;
		return Math.sqrt(dx + dy);
	}
	
	/*evaluates the center point*/
	private PointF mean(MotionEvent event) {
		PointF point = new PointF();
		float dy = event.getY(1) + event.getY(0);
		float dx = event.getX(1) + event.getX(0);
		point.set(dx / 2, dy / 2);
		return point;
	}
}