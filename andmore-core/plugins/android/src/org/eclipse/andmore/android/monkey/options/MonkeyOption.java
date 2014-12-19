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
package org.eclipse.andmore.android.monkey.options;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/**
 * Bean that represents an monkey option
 */
public class MonkeyOption {

	// Checked status (whether the monkey options is being used or not)
	private boolean checked;

	// Widget that represents the checked status in the UI
	private Widget checkedWidget;

	// monkey option name
	private String name;

	// monkey option user-friendly name
	private String userFriendlyName;

	// monkey option description (user-friendly description)
	private String description;

	// monkey option type (which type of values that the monkey option accepts)
	private int type;

	// monkey option type details (details of the values that the monkey option
	// accepts)
	private String typeDetails;

	// monkey option value (monkey option configured value)
	private String value;

	// Widget that represents the monkey option value in the UI
	private Widget valueWidget;

	// monkey option predefined values (list of values the monkey option
	// accepts)
	private List<String> preDefinedValues;

	/**
	 * Constructor
	 * 
	 * @param name
	 * @param type
	 */
	public MonkeyOption(String name, int type) {
		this.checked = false;
		this.name = name;
		this.type = type;
		this.value = "";
		this.preDefinedValues = new ArrayList<String>();
	}

	/**
	 * Get monkey option name
	 * 
	 * @return monkey option name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set monkey option name
	 * 
	 * @param name
	 *            monkey option name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get monkey option user-friendly name
	 * 
	 * @return
	 */
	public String getUserFriendlyName() {
		return userFriendlyName;
	}

	/**
	 * Set monkey option user-friendly name
	 * 
	 * @param userFriendlyName
	 */
	public void setUserFriendlyName(String userFriendlyName) {
		this.userFriendlyName = userFriendlyName;
	}

	/**
	 * Get monkey option type
	 * 
	 * @return monkey option type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Set monkey option type
	 * 
	 * @param type
	 *            monkey option type
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Get monkey option value
	 * 
	 * @return monkey option value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set monkey option value
	 * 
	 * @param value
	 *            monkey option value
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Get monkey option pre-defined values
	 * 
	 * @return monkey option pre-defined values
	 */
	public List<String> getPreDefinedValues() {
		return preDefinedValues;
	}

	/**
	 * Set monkey option pre-defined values
	 * 
	 * @param preDefinedValues
	 *            monkey option pre-defined values
	 */
	public void setPreDefinedValues(List<String> preDefinedValues) {
		this.preDefinedValues = preDefinedValues;
	}

	/**
	 * Check if the monkey option is being used
	 * 
	 * @return true if the monkey option is being used, false otherwise
	 */
	public boolean isChecked() {
		return checked;
	}

	/**
	 * Set that the monkey option is being used or not
	 * 
	 * @param checked
	 *            true if the monkey option is being used, false otherwise
	 */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/**
	 * Get monkey option description
	 * 
	 * @return monkey option description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set monkey option description
	 * 
	 * @param description
	 *            monkey option description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Get monkey option type details
	 * 
	 * @return monkey option type details
	 */
	public String getTypeDetails() {
		return typeDetails;
	}

	/**
	 * Get monkey option type details
	 * 
	 * @param typeDetails
	 *            valuable information for validating if the value assigned is
	 *            correct
	 */
	public void setTypeDetails(String typeDetails) {
		this.typeDetails = typeDetails;
	}

	/**
	 * Get the widget that represents the checked status in the UI
	 * 
	 * @return widget that represents the checked status in the UI
	 */
	public Widget getCheckedWidget() {
		return checkedWidget;
	}

	/**
	 * Set the widget that represents the checked status in the UI
	 * 
	 * @param checkedWidget
	 *            widget that represents the checked status in the UI
	 */
	public void setCheckedWidget(Widget checkedWidget) {
		this.checkedWidget = checkedWidget;
	}

	/**
	 * Get the widget that represents the monkey option value in the UI
	 * 
	 * @return widget that represents the monkey option value in the UI
	 */
	public Widget getValueWidget() {
		return valueWidget;
	}

	/**
	 * Set the widget that represents the monkey option value in the UI
	 * 
	 * @param valueWidget
	 *            widget that represents the monkey option value in the UI
	 */
	public void setValueWidget(Widget valueWidget) {
		this.valueWidget = valueWidget;
	}

	/**
	 * Update the widgets that represent this monkey options in the UI by
	 * changing their state to match the current values for checked and value
	 */
	public void updateUI() {
		if ((checkedWidget != null) && !checkedWidget.isDisposed()) {
			((Button) this.checkedWidget).setSelection(this.checked);
		}
		if ((valueWidget != null) && !checkedWidget.isDisposed()) {
			if (this.valueWidget instanceof Text) {
				((Text) this.valueWidget).setText(this.value);
			} else if (this.valueWidget instanceof Combo) {
				if ((this.value == null) || (this.value.equals(""))) {
					((Combo) this.valueWidget).deselectAll();
				} else {
					((Combo) this.valueWidget).select(getPreDefinedValues().indexOf(this.value));
				}
			}
		}
	}
}
