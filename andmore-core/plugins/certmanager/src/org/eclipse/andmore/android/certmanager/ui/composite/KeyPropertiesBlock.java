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

import java.util.Date;

import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.common.utilities.ui.WidgetsFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * This class shows the properties to for Android certificates / keys.
 */
public class KeyPropertiesBlock extends CertificateBlock {
	private Label labelExpirationDate = null;

	private Text textExpirationDate = null;

	private Label labelCreationDate = null;

	private Text textCreationDate = null;

	@Override
	protected void createCustomDetailedInfoArea(Composite parent) {
		labelCreationDate = WidgetsFactory.createLabel(parent, CertificateManagerNLS.CertificateBlock_CreationDate
				+ ":"); //$NON-NLS-1$
		textCreationDate = WidgetsFactory.createText(parent);

		labelExpirationDate = WidgetsFactory.createLabel(parent, CertificateManagerNLS.CertificateBlock_ExpirationDate
				+ ":"); //$NON-NLS-1$
		textExpirationDate = WidgetsFactory.createText(parent);
	}

	public Composite createInfoBlock(Composite parent, String alias, String name, String organization,
			String organizationUnit, String country, String state, String locality, Date validity, Date creationDate) {
		Composite toReturn = super.createInfoBlock(parent, alias, name, organization, organizationUnit, country, state,
				locality);
		labelCreationDate.setText(CertificateManagerNLS.CertificateBlock_CreationDate + ":"); //$NON-NLS-1$
		labelCreationDate.setVisible(true);
		textCreationDate.setTextLimit(Text.LIMIT);
		textCreationDate.setText(creationDate.toString());
		textCreationDate.setEditable(false);

		labelExpirationDate.setText(CertificateManagerNLS.CertificateBlock_ExpirationDate + ":"); //$NON-NLS-1$
		textExpirationDate.setTextLimit(Text.LIMIT);
		textExpirationDate.setText(validity.toString());
		textExpirationDate.setEditable(false);

		return toReturn;
	}
}
