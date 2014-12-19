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
package org.eclipse.andmore.android.handset.ui;

import java.util.Properties;

import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.handset.i18n.AndroidHandsetNLS;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * 
 * 
 * @author xrgc84
 */
public class AndroidPropertiesPage extends PropertyPage implements IWorkbenchPropertyPage {

	// the Android Handset Instance to which this Property Page applies
	private ISerialNumbered androidIntance;

	@Override
	public void setElement(IAdaptable element) {

		this.androidIntance = (ISerialNumbered) element;

		super.setElement(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		((PreferenceDialog) this.getContainer()).getTreeViewer().expandAll();

		final Composite parentComposite = new Composite(parent, SWT.NONE);
		parentComposite.setLayout(new GridLayout(2, false));
		parentComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label serialNumberLabel = new Label(parentComposite, SWT.NONE);
		serialNumberLabel.setText(AndroidHandsetNLS.AndroidPropertiesPage_SerialNumberLabel);
		Label serialNumberValue = new Label(parentComposite, SWT.NONE);
		serialNumberValue.setText(this.androidIntance.getSerialNumber());

		Label targetLabel = new Label(parentComposite, SWT.NONE);
		targetLabel.setText(AndroidHandsetNLS.AndroidPropertiesPage_AndroidVersionLabel);
		Label targetValue = new Label(parentComposite, SWT.NONE);

		Label apiLabel = new Label(parentComposite, SWT.NONE);
		apiLabel.setText(AndroidHandsetNLS.AndroidPropertiesPage_APIVersionLabel);
		Label apiValue = new Label(parentComposite, SWT.NONE);

		Properties propValues = ((IInstance) androidIntance).getProperties();
		if ((propValues != null) && !propValues.isEmpty()) {
			apiValue.setText(propValues.getProperty("ro.build.version.sdk")); //$NON-NLS-1$
			targetValue.setText(propValues.getProperty("ro.build.version.release")); //$NON-NLS-1$
		} else {
			apiValue.setText(AndroidHandsetNLS.AndroidPropertiesPage_NA);
			targetValue.setText(AndroidHandsetNLS.AndroidPropertiesPage_NA);
		}
		parentComposite.pack();

		noDefaultAndApplyButton();
		return parentComposite;

	}
}
