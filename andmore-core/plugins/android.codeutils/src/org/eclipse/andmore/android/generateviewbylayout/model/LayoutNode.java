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
package org.eclipse.andmore.android.generateviewbylayout.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Model representing an Android GUI item that can be inserted into activity /
 * fragment
 */
public class LayoutNode {
	/**
	 * Methods available for setting View state (eg.: RadioButton, Spinner,
	 * EditText)
	 */
	protected enum ViewSetMethods {
		setChecked, setSelected, setText, setSelection, setProgress
	}

	/**
	 * Methods available for getting View state (eg.: RadioButton, Spinner,
	 * EditText)
	 */
	protected enum ViewGetMethods {
		isChecked, isSelected, getText, getSelectedItemPosition, getProgress
	}

	/**
	 * Methods available for Preference putters
	 */
	protected enum PreferenceSetMethods {
		putInt, putBoolean, putString
	}

	/**
	 * Methods available for Preference getters
	 */
	protected enum PreferenceGetMethods {
		getInt, getBoolean, getString
	}

	/**
	 * Available options for save/restore operations
	 */
	public enum ViewProperties {
		ViewStateSetMethod, ViewStateGetMethod, PreferenceSetMethod, PreferenceGetMethod, ViewStateValueType
	}

	/**
	 * Available items to generate code for
	 */
	public static enum LayoutNodeViewType {
		Button, ToggleButton, ImageButton, CheckBox, RadioButton, EditText, Spinner, Gallery, RatingBar, SeekBar
	}

	protected final Map<ViewProperties, String> properties = new HashMap<LayoutNode.ViewProperties, String>();

	private boolean isFragmentPlaceholder = false;

	private boolean isLayout = false;

	private boolean isGUIItem = false;

	private String nodeType;

	private String nodeId;

	private String onClick;

	private boolean insertCode = false;

	private boolean saveState = false;

	private boolean alreadySaved = false;

	private boolean alreadyDeclaredInCode = false;

	private String clazzName; // used for fragments only

	private boolean alreadyRestored;

	/**
	 * @return the clazzName (given by android:name attribute)
	 */
	public String getClazzName() {
		return clazzName;
	}

	/**
	 * @param clazzName
	 *            the clazzName to set
	 */
	public void setClazzName(String clazzName) {
		this.clazzName = clazzName;
	}

	/**
	 * @return true if it is a &lt;fragment&gt; element, false otherwise
	 */
	public boolean isFragmentPlaceholder() {
		return isFragmentPlaceholder;
	}

	/**
	 * @param isFragmentPlaceholder
	 *            the isFragmentPlaceholder to set
	 */
	public void setFragmentPlaceholder(boolean isFragmentPlaceholder) {
		this.isFragmentPlaceholder = isFragmentPlaceholder;
	}

	/**
	 * @return true if element is a layout, false otherwise (if it is a GUI item
	 *         or fragment)
	 */
	public boolean isLayout() {
		return isLayout;
	}

	public void setLayout(boolean isLayout) {
		this.isLayout = isLayout;
	}

	/**
	 * @return true if it is a GUI Item (Button, EditText, etc) element
	 */
	public boolean isGUIItem() {
		return isGUIItem;
	}

	public void setGUIItem(boolean isGUIItem) {
		this.isGUIItem = isGUIItem;
	}

	/**
	 * Type depends on XML element (eg.: &lt;button&gt;)
	 */
	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	/**
	 * @return @return value available on android:id attribute
	 */
	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @return value available on android:onClick attribute
	 */
	public String getOnClick() {
		return onClick;
	}

	public void setOnClick(String onClick) {
		this.onClick = onClick;
	}

	/**
	 * @return true if user marked in the dialog/wizard to insert the code for
	 *         this layout node
	 */
	public boolean shouldInsertCode() {
		return insertCode;
	}

	public void setInsertCode(boolean insertCode) {
		this.insertCode = insertCode;
	}

	/**
	 * @return true if the element is already declared on code (according to
	 *         code visitors)
	 */
	public boolean isAlreadyDeclaredInCode() {
		return alreadyDeclaredInCode;
	}

	/**
	 * @param alreadyDeclaredInCode
	 *            the alreadyDeclaredInCode to set
	 */
	public void setAlreadyDeclaredInCode(boolean alreadyDeclaredInCode) {
		this.alreadyDeclaredInCode = alreadyDeclaredInCode;
	}

	/**
	 * @return true if need to save/restore state, false otherwise
	 */
	public boolean getSaveState() {
		return saveState;
	}

	/**
	 * @param saveState
	 *            the saveState to set
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

	/**
	 * @return true if already has code to save state
	 */
	public boolean isAlreadySaved() {
		return alreadySaved;
	}

	public void setAlreadySaved(boolean alreadySaved) {
		this.alreadySaved = alreadySaved;
	}

	/**
	 * @return true if already has code to restore state
	 */
	public boolean isAlreadyRestored() {
		return alreadyRestored;
	}

	public void setAlreadyRestored(boolean alreadyRestored) {
		this.alreadyRestored = alreadyRestored;
	}

	public String getProperty(ViewProperties property) {
		return properties.get(property);
	}

}
