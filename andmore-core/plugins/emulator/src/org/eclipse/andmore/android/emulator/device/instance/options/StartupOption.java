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
package org.eclipse.andmore.android.emulator.device.instance.options;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/**
 * Bean that represents an startup option
 * 
 */
public class StartupOption {

	// Checked status (whether the startup options is being used or not)
	private boolean checked;

	// Widget that represents the checked status in the UI
	private Widget checkedWidget;

	// Startup option name
	private String name;

	// Startup option user-friendly name
	private String userFriendlyName;

	// Startup option description (user-friendly description)
	private String description;

	// Startup option type (which type of values that the startup option
	// accepts)
	private int type;

	// Startup option type details (details of the values that the startup
	// option accepts)
	private String typeDetails;

	// Startup option value (startup option configured value)
	private String value;

	// Widget that represents the startup option value in the UI
	private Widget valueWidget;

	// Startup option predefined values (list of values the startup option
	// accepts)
	private List<String> preDefinedValues;

	/**
	 * Constructor
	 * 
	 * @param name
	 * @param type
	 */
	public StartupOption(String name, int type) {
		this.checked = false;
		this.name = name;
		this.type = type;
		this.value = "";
		this.preDefinedValues = new ArrayList<String>();
	}

	/**
	 * Get startup option name
	 * 
	 * @return startup option name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set startup option name
	 * 
	 * @param name
	 *            startup option name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get startup option user-friendly name
	 * 
	 * @return
	 */
	public String getUserFriendlyName() {
		return userFriendlyName;
	}

	/**
	 * Set startup option user-friendly name
	 * 
	 * @param userFriendlyName
	 */
	public void setUserFriendlyName(String userFriendlyName) {
		this.userFriendlyName = userFriendlyName;
	}

	/**
	 * Get startup option type
	 * 
	 * @return startup option type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Set startup option type
	 * 
	 * @param type
	 *            startup option type
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Get startup option value
	 * 
	 * @return startup option value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set startup option value
	 * 
	 * @param value
	 *            startup option value
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Get startup option pre-defined values
	 * 
	 * @return startup option pre-defined values
	 */
	public List<String> getPreDefinedValues() {
		return preDefinedValues;
	}

	/**
	 * Set startup option pre-defined values
	 * 
	 * @param preDefinedValues
	 *            startup option pre-defined values
	 */
	public void setPreDefinedValues(List<String> preDefinedValues) {
		this.preDefinedValues = preDefinedValues;
	}

	/**
	 * Check if the startup option is being used
	 * 
	 * @return true if the startup option is being used, false otherwise
	 */
	public boolean isChecked() {
		return checked;
	}

	/**
	 * Set that the startup option is being used or not
	 * 
	 * @param checked
	 *            true if the startup option is being used, false otherwise
	 */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/**
	 * Get startup option description
	 * 
	 * @return startup option description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set startup option description
	 * 
	 * @param description
	 *            startup option description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Get startup option type details
	 * 
	 * @return startup option type details
	 */
	public String getTypeDetails() {
		return typeDetails;
	}

	/**
	 * Get startup option type details
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
	 * Get the widget that represents the startup option value in the UI
	 * 
	 * @return widget that represents the startup option value in the UI
	 */
	public Widget getValueWidget() {
		return valueWidget;
	}

	/**
	 * Set the widget that represents the startup option value in the UI
	 * 
	 * @param valueWidget
	 *            widget that represents the startup option value in the UI
	 */
	public void setValueWidget(Widget valueWidget) {
		this.valueWidget = valueWidget;
	}

	/**
	 * Update the widgets that represent this startup options in the UI by
	 * changing their state to match the current values for checked and value
	 */
	public void updateUI() {
		if (checkedWidget != null && !checkedWidget.isDisposed()) {
			((Button) this.checkedWidget).setSelection(this.checked);
		}
		if (valueWidget != null && !checkedWidget.isDisposed()) {
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
