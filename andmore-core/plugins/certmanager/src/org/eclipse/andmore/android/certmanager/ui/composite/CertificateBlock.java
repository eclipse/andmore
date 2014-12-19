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
package org.eclipse.andmore.android.certmanager.ui.composite;

import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.common.utilities.ui.Country;
import org.eclipse.andmore.android.common.utilities.ui.ToolsCountries;
import org.eclipse.andmore.android.common.utilities.ui.WidgetsFactory;
import org.eclipse.andmore.android.common.utilities.ui.WidgetsUtil;
import org.eclipse.andmore.android.wizards.elements.IBaseBlock;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This class shows the properties to for certificate.
 */
public class CertificateBlock implements IBaseBlock {
	private static final String LABEL_DECORATOR = "*"; //$NON-NLS-1$

	private static final int LONG_TEXT_SIZE = 256;

	private static final int MEDIUM_TEXT_SIZE = 128;

	private static final int SMALL_TEXT_SIZE = 64;

	/**
	 * The attribute representing the window with which the user interacts.
	 */
	private Shell shell;

	/**
	 * Store the composite.
	 */
	protected Composite parent;

	/**
	 * The key pair alias text field.
	 */
	private Text textAlias;

	/**
	 * The common name text field.
	 */
	private Text commonNameText;

	/**
	 * The organization text field.
	 */
	private Text textOrganization;

	/**
	 * The organization unit text field.
	 */
	private Text textOrganizationUnit;

	/**
	 * The locality text field.
	 */
	private Text textLocality;

	/**
	 * The state text field.
	 */
	private Text textState;

	/**
	 * The country combo.
	 */
	private Combo comboCountry;

	private Label labelAlias;

	private Control controlToFocus = null;

	/**
	 * This listener is used to save the field that has the focus.
	 * */
	protected FocusListener focusListener = new FocusListener() {

		@Override
		public void focusLost(FocusEvent e) {
			// do nothing...
		}

		@Override
		public void focusGained(FocusEvent e) {
			if (e.widget instanceof Control) {
				controlToFocus = (Control) e.widget;
			}
		}
	};

	@Override
	public Composite createContent(Composite parent) {
		Composite toReturn = WidgetsFactory.createComposite(parent, 1);

		this.parent = parent;

		createBasicInfoArea(toReturn);
		createDetailedInfoArea(toReturn);
		createCustomArea(toReturn);
		setDefaultFocus();

		return toReturn;
	}

	protected void createCustomArea(Composite parent) {
		// by default, do nothing
	}

	/**
	 * Creates Custom Fields to be appended in the properties block
	 * 
	 * @param dnFieldsArea
	 */
	protected void createCustomDetailedInfoArea(Composite dnFieldsArea) {
		// by default, do nothing
	}

	/**
	 * Create all fields of the distinguished name.
	 * 
	 * @param parent
	 *            The parent composite to add the created fields.
	 */
	private void createDetailedInfoArea(Composite parent) {
		// Composite bottomComposite = WidgetsFactory.createComposite(parent);

		Group detailsGroup = WidgetsFactory.createTitledGroup(parent,
				CertificateManagerNLS.CertificateBlock_DetailedInfoGroupTitle, 2);

		// Creating the common name field
		WidgetsFactory.createLabel(detailsGroup, CertificateManagerNLS.CertificateBlock_FirstAndLastName + ":"); //$NON-NLS-1$ 
		commonNameText = WidgetsFactory.createText(detailsGroup);
		commonNameText.setTextLimit(MEDIUM_TEXT_SIZE);
		commonNameText.addListener(SWT.Modify, this);
		commonNameText.addFocusListener(focusListener);

		// Creating the organization field
		WidgetsFactory.createLabel(detailsGroup, CertificateManagerNLS.CertificateBlock_Organization + ":"); //$NON-NLS-1$ 
		textOrganization = WidgetsFactory.createText(detailsGroup);
		textOrganization.setTextLimit(MEDIUM_TEXT_SIZE);
		textOrganization.addListener(SWT.Modify, this);
		textOrganization.addFocusListener(focusListener);

		// Creating the organization unit field
		WidgetsFactory.createLabel(detailsGroup, CertificateManagerNLS.CertificateBlock_OrganizationUnit + ":"); //$NON-NLS-1$
		textOrganizationUnit = WidgetsFactory.createText(detailsGroup);
		textOrganizationUnit.setTextLimit(MEDIUM_TEXT_SIZE);
		textOrganizationUnit.addListener(SWT.Modify, this);
		textOrganizationUnit.addFocusListener(focusListener);

		// Creating the locality field
		WidgetsFactory.createLabel(detailsGroup, CertificateManagerNLS.CertificateBlock_CityOrLocality + ":"); //$NON-NLS-1$ 
		textLocality = WidgetsFactory.createText(detailsGroup);
		textLocality.setTextLimit(LONG_TEXT_SIZE);
		textLocality.addListener(SWT.Modify, this);
		textLocality.addFocusListener(focusListener);

		// Creating the state field
		WidgetsFactory.createLabel(detailsGroup, CertificateManagerNLS.CertificateBlock_StateOrProvince + ":"); //$NON-NLS-1$
		textState = WidgetsFactory.createText(detailsGroup);
		textState.setTextLimit(LONG_TEXT_SIZE);
		textState.addListener(SWT.Modify, this);
		textState.addFocusListener(focusListener);

		// Creating the country field
		WidgetsFactory.createLabel(detailsGroup, CertificateManagerNLS.CertificateBlock_CountryCode + ":"); //$NON-NLS-1$
		comboCountry = WidgetsFactory.createCombo(detailsGroup);
		comboCountry.addListener(SWT.Selection, this);
		comboCountry.addFocusListener(focusListener);
		for (Country country : ToolsCountries.getInstance().getCountries()) {
			this.comboCountry.add(country.getCountryName());
			this.comboCountry.setData(country.getCountryName(), country.getCountryCode());
		}
		comboCountry.setVisibleItemCount(SMALL_TEXT_SIZE);

		createCustomDetailedInfoArea(detailsGroup);
	}

	/**
	 * Create the key pair alias field.
	 * 
	 * @param parent
	 *            The parent composite to add the key pair alias field.
	 */
	private void createBasicInfoArea(Composite parent) {
		Group groupBasicInfo = WidgetsFactory.createTitledGroup(parent,
				CertificateManagerNLS.CertificateBlock_BasicInfoGroupTitle, 2);

		labelAlias = WidgetsFactory.createLabel(groupBasicInfo, CertificateManagerNLS.CertificateBlock_AliasName + ":"); //$NON-NLS-1$
		textAlias = WidgetsFactory.createText(groupBasicInfo);
		textAlias.setTextLimit(SMALL_TEXT_SIZE);
		textAlias.addListener(SWT.Modify, this);
		textAlias.addFocusListener(focusListener);

		createCustomBasicInfoArea(groupBasicInfo);
	}

	/**
	 * Allow subclasses to add custom required fields to the certificate block.
	 * 
	 * @param parent
	 *            The parent composite for the custom required fields.
	 */
	protected void createCustomBasicInfoArea(Composite parent) {
		// default implementation does nothing.
	}

	/**
	 * Decorate required fields using
	 * {@link CertificateBlock#decorateText(String)}. Always call the
	 * {@code super} implementation when overriding this method. <br/>
	 * Default required fields are:
	 * <ul>
	 * <li>Alias</li>
	 * </ul>
	 * */
	protected void decorateRequiredFields() {
		labelAlias.setText(decorateText(labelAlias.getText()));
	}

	/**
	 * Decorate required fields by prefixing it with
	 * {@link CertificateBlock#LABEL_DECORATOR}.
	 * 
	 * @return The decorated text.
	 * */
	protected String decorateText(String strToDecorate) {
		return LABEL_DECORATOR.concat(strToDecorate);
	}

	/**
	 * Obtains the key pair alias defined by user.
	 * 
	 * @return The key pair alias.
	 */
	public String getAlias() {
		return textAlias.getText().toLowerCase();
	}

	/**
	 * Obtains the common name defined by user.
	 * 
	 * @return The common name.
	 */
	public String getCommonName() {
		return commonNameText.getText();
	}

	/**
	 * Obtains the organization defined by user.
	 * 
	 * @return The organization.
	 */
	public String getOrganization() {
		return textOrganization.getText();
	}

	/**
	 * Obtains the organization unit defined by user.
	 * 
	 * @return The organization unit.
	 */
	public String getOrganizationUnit() {
		return textOrganizationUnit.getText();
	}

	/**
	 * Obtains the locality defined by user.
	 * 
	 * @return The locality.
	 */
	public String getLocality() {
		return textLocality.getText();
	}

	/**
	 * Obtains the state defined by user.
	 * 
	 * @return The state.
	 */
	public String getState() {
		return textState.getText();
	}

	/**
	 * Obtains the country defined by user.
	 * 
	 * @return The country.
	 */
	public String getCountry() {
		String result = new String(); // empty string

		// ensure that there is something selected in the combo before indexing
		// its item list
		if (comboCountry.getSelectionIndex() >= 0) {
			result = (String) comboCountry.getData(comboCountry.getItem(comboCountry.getSelectionIndex()));
		}
		return result;
	}

	@Override
	public boolean isPageComplete() {
		return (getErrorMessage() == null)
				&& !WidgetsUtil.isNullOrEmpty(this.textAlias)
				&& (!WidgetsUtil.isNullOrEmpty(this.commonNameText)
						|| !WidgetsUtil.isNullOrEmpty(this.textOrganization)
						|| !WidgetsUtil.isNullOrEmpty(this.textOrganizationUnit)
						|| !WidgetsUtil.isNullOrEmpty(this.textLocality) || !WidgetsUtil.isNullOrEmpty(this.textState) || !WidgetsUtil
							.isNullOrDeselected(this.comboCountry));
	}

	@Override
	public boolean canFlipToNextPage() {
		return (getErrorMessage() == null) && isPageComplete();
	}

	@Override
	public String getErrorMessage() {
		String toReturn = null;

		if (WidgetsUtil.isNullOrEmpty(textAlias)) {
			toReturn = NLS.bind(CertificateManagerNLS.CertificateBlock_FieldIsEmpty,
					CertificateManagerNLS.CertificateBlock_AliasName);
		} else if (WidgetsUtil.isNullOrEmpty(commonNameText) && (WidgetsUtil.isNullOrEmpty(textOrganization))
				&& (WidgetsUtil.isNullOrEmpty(textOrganizationUnit)) && (WidgetsUtil.isNullOrEmpty(textLocality))
				&& (WidgetsUtil.isNullOrEmpty(textState)) && (WidgetsUtil.isNullOrDeselected(comboCountry))) {
			toReturn = CertificateManagerNLS.CertificateBlock_DetailedInfoNonEmptyFieldsRestriction;
		}

		return toReturn;
	}

	public Composite createInfoBlock(Composite parent, String alias, String name, String organization,
			String organizationUnit, String country, String state, String locality) {
		Composite toReturn = createContent(parent);
		textAlias.setText(alias);
		commonNameText.setText(name);
		textOrganization.setText(organization);
		textOrganizationUnit.setText(organizationUnit);
		textLocality.setText(locality);
		textState.setText(state);
		int i = 0;
		boolean found = false;
		while ((i < comboCountry.getItemCount()) && !found) {
			String id = (String) comboCountry.getData(comboCountry.getItem(i));
			if (id.equalsIgnoreCase(country)) {
				comboCountry.select(i);
				found = true;
			}
			i++;
		}

		textAlias.setEditable(false);
		commonNameText.setEditable(false);
		textOrganization.setEditable(false);
		textOrganizationUnit.setEditable(false);
		textLocality.setEditable(false);
		textState.setEditable(false);
		if (!found) {
			comboCountry.add(CertificateManagerNLS.CertificateInfoDialog_NotAvailableProperty, 0);
			comboCountry.select(0);
		}
		comboCountry.setEnabled(false);

		return toReturn;
	}

	@Override
	public void handleEvent(Event event) {
		if (this.parent != null) {
			this.parent.notifyListeners(event.type, event);
		}
	}

	@Override
	public void refresh() {
		// Empty
	}

	@Override
	public void setShell(Shell shell) {
		this.shell = shell;
	}

	@Override
	public Shell getShell() {
		return shell;
	}

	@Override
	public void setDefaultFocus() {
		controlToFocus = textAlias;
	}

	@Override
	public void setFocus() {
		if (controlToFocus != null) {
			controlToFocus.setFocus();
		}
	}
}
