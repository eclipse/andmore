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
package org.eclipse.andmore.android.codesnippets;

import org.eclipse.andmore.android.codesnippets.i18n.AndroidSnippetsNLS;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This {@link ControlContribution} adds the check box button which shows or
 * hides the tooltip of the snippet.
 * 
 * @see ControlContribution
 * @see {@link AndroidSnippetsStartup}
 *
 */
public class TooltipDisplayConfigContriutionItem extends ControlContribution {
	/**
	 * Listener which updates the status of whether or not to display the tool
	 * tip.
	 * 
	 * @see SelectionListener
	 * 
	 */
	private final class TooltipSelectionListener implements SelectionListener {
		/**
		 * Here the state of the tooltip display button is persisted in the
		 * {@link IDialogSettings}.
		 * 
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(SelectionEvent e) {
			// action for when the check box is pressed.
			performButtonSelection();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org
		 * .eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing
		}
	}

	/**
	 * Check box {@link Button} which shows or hides the tooltip.
	 */
	private Button showToolTipButton = null;

	/**
	 * {@link IDialogSettings} field for whether or not the tooltip is to be
	 * displayed.
	 */
	private static final String DIALOG_SETTINGS__IS_TOOLTIP_DISPLAYED = "IsTooltipDisplayed";

	/**
	 * Constructor which initiates this {@link ControlContribution}.
	 */
	public TooltipDisplayConfigContriutionItem() {
		super("org.eclipse.andmore.android.codesnippets.tooltipDisplayConfig"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.action.ControlContribution#createControl(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected Control createControl(Composite parent) {
		// adjust layout in order to produce space to the left
		Composite mainComposite = new Composite(parent, SWT.NONE);
		RowLayout layout = new RowLayout(SWT.FILL);
		layout.center = true;
		layout.marginLeft = 10;
		mainComposite.setLayout(layout);

		// create the check box button
		showToolTipButton = new Button(mainComposite, SWT.CHECK);
		showToolTipButton.setText(AndroidSnippetsNLS.TooltipDisplayConfigContriutionItem_ShowPreview);
		showToolTipButton.addSelectionListener(new TooltipSelectionListener());

		// set the selection persisted
		IEclipsePreferences preferences = getEclipsePreferences();
		boolean isTooltipDisplayed = preferences.getBoolean(DIALOG_SETTINGS__IS_TOOLTIP_DISPLAYED, true);
		showToolTipButton.setSelection(isTooltipDisplayed);
		performButtonSelection();

		return mainComposite;
	}

	/**
	 * Returns <code>true</code> in case the tool tip is to be displayed, or
	 * <code>false</code> should it be hidden.
	 * 
	 * @return <code>true</code> in case the tool tip is to be displayed,
	 *         <code>false</code> otherwise.
	 */
	public boolean isTooltipDisplayed() {
		return showToolTipButton.getSelection();
	}

	/**
	 * Method called when the check box {@link Button} is called. It persists
	 * the check box selection state in the {@link IDialogSettings}.
	 */
	private void performButtonSelection() {
		// persist whether or not to show the tooltip
		IEclipsePreferences preferences = getEclipsePreferences();
		preferences.putBoolean(DIALOG_SETTINGS__IS_TOOLTIP_DISPLAYED, showToolTipButton.getSelection());
		try {
			preferences.flush();
		} catch (BackingStoreException bse) {
			StudioLogger.error(TooltipDisplayConfigContriutionItem.class.toString(),
					"Preferences for snippets could not be saved.", bse); //$NON-NLS-1$
		}
	}

	/**
	 * Get Eclipse�s preferences.
	 * 
	 * @return Return Eclipse�s preferences.
	 */
	private IEclipsePreferences getEclipsePreferences() {
		return ConfigurationScope.INSTANCE.getNode(AndroidSnippetsStartup.SNIPPETS_VIEW_ID);
	}
}
