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
package org.eclipse.andmore.android.generateviewbylayout;

/**
 * Constants for creating code based on layout (method names, variables, types)
 */
public interface JavaViewBasedOnLayoutModifierConstants {

	/*
	 * Constants
	 */
	String ON_KEY_LISTENER = "OnKeyListener"; //$NON-NLS-1$

	String SET_ON_KEY_LISTENER = "setOnKeyListener"; //$NON-NLS-1$

	String ON_KEY = "onKey"; //$NON-NLS-1$

	String EVENT = "event"; //$NON-NLS-1$

	String KEY_EVENT = "KeyEvent"; //$NON-NLS-1$

	String KEY_CODE = "keyCode"; //$NON-NLS-1$

	String METHOD_ON_CLICK_LISTENER = "OnClickListener"; //$NON-NLS-1$

	String VIEW_VARIABLE_NAME = "target"; //$NON-NLS-1$

	String METHOD_NAME_GET_ID = "getId"; //$NON-NLS-1$

	String METHOD_NAME_ON_CLICK = "onClick"; //$NON-NLS-1$

	String IMPORT_ANDROID_VIEW_VIEW = "android.view.*"; //$NON-NLS-1$

	String IMPORT_ANDROID_WIDGET = "android.widget.*"; //$NON-NLS-1$

	String IMPORT_ANDROID_APP = "android.app.*"; //$NON-NLS-1$

	String IMPORT_ANDROID_OS = "android.os.Bundle"; //$NON-NLS-1$

	String HANDLER_ONCLICK_LISTENER = "onClickHandler"; //$NON-NLS-1$

	String SET_ON_CLICK_LISTENER = "setOnClickListener"; //$NON-NLS-1$   

	String ID = "id"; //$NON-NLS-1$

	String R = "R"; //$NON-NLS-1$

	String FIND_VIEW_BY_ID = "findViewById"; //$NON-NLS-1$

	String FIND_FRAGMENT_BY_ID = "findFragmentById"; //$NON-NLS-1$

	String ON_ITEM_SELECTED_LISTENER = "OnItemSelectedListener"; //$NON-NLS-1$

	String SET_ON_ITEM_SELECTED_LISTENER = "setOnItemSelectedListener"; //$NON-NLS-1$

	String ON_NOTHING_SELECTED = "onNothingSelected"; //$NON-NLS-1$

	String ON_ITEM_SELECTED = "onItemSelected"; //$NON-NLS-1$

	String ROW = "row"; //$NON-NLS-1$

	String POSITION = "position"; //$NON-NLS-1$

	String SELECTED_ITEM_VIEW = "selectedItemView"; //$NON-NLS-1$

	String VIEW_CLASS = "View"; //$NON-NLS-1$

	String ADAPTER_VIEW = "AdapterView"; //$NON-NLS-1$

	String PARENT_VIEW = "parentView"; //$NON-NLS-1$

	String SET_ON_ITEM_CLICK_LISTENER = "setOnItemClickListener"; //$NON-NLS-1$

	String ON_ITEM_CLICK_LISTENER = "OnItemClickListener"; //$NON-NLS-1$

	String SET_ON_RATING_BAR_CHANGE_LISTENER = "setOnRatingBarChangeListener"; //$NON-NLS-1$

	String ON_ITEM_CLICK = "onItemClick"; //$NON-NLS-1$

	String ON_SEEK_BAR_CHANGE_LISTENER = "OnSeekBarChangeListener"; //$NON-NLS-1$

	String ON_STOP_TRACKING_TOUCH = "onStopTrackingTouch"; //$NON-NLS-1$

	String ON_START_TRACKING_TOUCH = "onStartTrackingTouch"; //$NON-NLS-1$

	String ON_PROGRESS_CHANGED = "onProgressChanged"; //$NON-NLS-1$

	String ON_RATING_BAR_CHANGE_LISTENER = "OnRatingBarChangeListener"; //$NON-NLS-1$

	String SEEK_BAR = "SeekBar"; //$NON-NLS-1$

	String SET_ON_SEEK_BAR_CHANGE_LISTENER = "setOnSeekBarChangeListener"; //$NON-NLS-1$

	String ON_RATING_CHANGED = "onRatingChanged"; //$NON-NLS-1$

	String RATING_VARIABLE = "rating"; //$NON-NLS-1$

	String RATING_BAR_VARIABLE = "ratingBar"; //$NON-NLS-1$

	String FROM_USER_VARIABLE = "fromUser"; //$NON-NLS-1$

	String PROGRESS_VARIABLE = "progress"; //$NON-NLS-1$

	String SEEK_BAR_VARIABLE = "seekBar"; //$NON-NLS-1$

	String GET_FRAGMENT_MANAGER = "getFragmentManager"; //$NON-NLS-1$

	String GET_SUPPORT_FRAGMENT_MANAGER = "getSupportFragmentManager"; //$NON-NLS-1$

	String EXPRESSION_MISSING = "MISSING";
}
