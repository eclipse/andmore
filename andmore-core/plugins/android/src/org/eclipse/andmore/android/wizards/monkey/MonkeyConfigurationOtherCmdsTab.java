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
package org.eclipse.andmore.android.wizards.monkey;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.monkey.options.MonkeyOptionsMgt;
import org.eclipse.andmore.android.nativeos.NativeUIUtils;
import org.eclipse.andmore.android.wizards.monkey.AbstractPropertiesComposite.PropertyCompositeChangeEvent;
import org.eclipse.andmore.android.wizards.monkey.AbstractPropertiesComposite.PropertyCompositeChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * This class implements the Options tab of the Monkey Launch Configuration.
 */
public class MonkeyConfigurationOtherCmdsTab extends AbstractLaunchConfigurationTab {

	private MonkeyOptionsComposite monkeyOptionsComposite;

	// handle changes
	private final PropertyCompositeChangeListener compositeChangeListener = new PropertyCompositeChangeListener() {
		@Override
		public void compositeChanged(PropertyCompositeChangeEvent e) {
			String errorMessage = monkeyOptionsComposite.getErrorMessage();
			if (errorMessage != null) {
				setErrorMessage(errorMessage);
				updateLaunchConfigurationDialog();
			} else {
				setErrorMessage(null);
				updateLaunchConfigurationDialog();
			}
		}
	};

	@Override
	public void createControl(Composite parent) {
		setErrorMessage(null);
		setMessage(AndroidNLS.UI_MonkeyWizardOptionsPage_PageMessage);

		// Define layout
		GridLayout mainLayout = new GridLayout(1, false);
		mainLayout.marginTop = 5;
		mainLayout.marginWidth = 5;
		mainLayout.marginHeight = 5;

		// Create Monkey Options area
		monkeyOptionsComposite = new MonkeyOptionsComposite(parent, NativeUIUtils.getDefaultCommandLine());

		AbstractPropertiesComposite.addCompositeChangeListener(compositeChangeListener);

		// Set layout
		monkeyOptionsComposite.setLayout(mainLayout);

		setControl(monkeyOptionsComposite);
	}

	@Override
	public String getName() {
		return AndroidNLS.UI_MonkeyComposite_TabOtherCmdName;
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		String otherCmds = "";
		try {
			otherCmds = configuration.getAttribute(IMonkeyConfigurationConstants.ATTR_OTHER_CMDS,
					IMonkeyConfigurationConstants.DEFAULT_VERBOSE_VALUE);
			monkeyOptionsComposite.reloadValues(otherCmds);
		} catch (CoreException e) {
			StudioLogger.error(MonkeyConfigurationOtherCmdsTab.class,
					"Failed to initialize Monkey Launch Configuration Other Cmds:" + e.getMessage());
		}

	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IMonkeyConfigurationConstants.ATTR_OTHER_CMDS, MonkeyOptionsMgt.getParamList());

	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return DebugUITools.getImage(IInternalDebugUIConstants.IMG_OBJS_COMMON_TAB);
	}

	/**
	 * @see ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {

		boolean isValid;
		String errorMessage = monkeyOptionsComposite.getErrorMessage();
		if (errorMessage != null) {
			setErrorMessage(errorMessage);
			isValid = false;
		} else {
			setErrorMessage(null);
			isValid = true;
		}
		return isValid;
	}

	@Override
	public void dispose() {
		AbstractPropertiesComposite.removeCompositeChangeListener(compositeChangeListener);
		super.dispose();
	}

}
