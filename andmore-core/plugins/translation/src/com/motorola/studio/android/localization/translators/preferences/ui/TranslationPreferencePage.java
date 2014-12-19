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

package com.motorola.studio.android.localization.translators.preferences.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.osgi.service.prefs.BackingStoreException;

import com.motorola.studio.android.localization.translators.GoogleTranslatorConstants;
import com.motorola.studio.android.localization.translators.TranslationPlugin;
import com.motorola.studio.android.localization.translators.i18n.TranslateNLS;

public class TranslationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private final String GOOGLE_APIS_CONSOLE_LINK = "http://code.google.com/apis/console/"; //$NON-NLS-1$

	private Text apiKeyText;

	private String apiKeyValue;

	@Override
	public void init(IWorkbench workbench) {
		noDefaultAndApplyButton();
	}

	@Override
	public boolean performOk() {
		InstanceScope scope = (InstanceScope) InstanceScope.INSTANCE;
		IEclipsePreferences prefs = scope.getNode(TranslationPlugin.PLUGIN_ID);
		if (apiKeyValue.trim().length() == 0) {
			prefs.remove(GoogleTranslatorConstants.API_KEY_VALUE_PREFERENCE);
		} else {
			prefs.put(GoogleTranslatorConstants.API_KEY_VALUE_PREFERENCE, apiKeyValue.trim());
		}

		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			// do nothing
		}

		return super.performOk();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite entryTable = new Composite(parent, SWT.NULL);
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		entryTable.setLayoutData(data);

		GridLayout layout = new GridLayout();
		entryTable.setLayout(layout);

		layout = new GridLayout(2, false);

		layout = new GridLayout(2, false);
		Group translatorAPIGroup = new Group(entryTable, SWT.NONE);
		translatorAPIGroup.setLayout(layout);
		translatorAPIGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		translatorAPIGroup.setText(TranslateNLS.AndroidPreferencePage_googleApiKey_GroupLabel);

		Link noteLabel = new Link(translatorAPIGroup, SWT.WRAP);
		noteLabel.setText(NLS.bind(TranslateNLS.AndroidPreferencePage_googleApiKey_Note, GOOGLE_APIS_CONSOLE_LINK));
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		data.widthHint = 450;
		noteLabel.setLayoutData(data);

		noteLabel.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();

				/*
				 * open the browser
				 */
				IWebBrowser browser;
				try {
					browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.LOCATION_BAR
							| IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.AS_EXTERNAL,
							"MOTODEV", null, null); //$NON-NLS-1$

					browser.openURL(new URL(GOOGLE_APIS_CONSOLE_LINK));
				} catch (PartInitException ex) {
					StudioLogger.error("Error opening the Google APIs Console link: " //$NON-NLS-1$
							+ ex.getMessage());
				} catch (MalformedURLException ex) {
					StudioLogger.error("Error opening the Google APIs Console link: " //$NON-NLS-1$
							+ ex.getMessage());
				}
			}
		});

		Label apiKeyLabel = new Label(translatorAPIGroup, SWT.NONE);
		apiKeyLabel.setText(TranslateNLS.AndroidPreferencePage_googleApiKey_Label);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		apiKeyLabel.setLayoutData(data);

		apiKeyText = new Text(translatorAPIGroup, SWT.BORDER);
		apiKeyValue = getApiKey();
		apiKeyText.setText(apiKeyValue);
		apiKeyText.setToolTipText(TranslateNLS.AndroidPreferencePage_googleApiKey_Tooltip);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		apiKeyText.setLayoutData(data);
		apiKeyText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				apiKeyValue = apiKeyText.getText();

			}
		});

		return entryTable;
	}

	/**
	 * get the apikey
	 * 
	 * @return the apikey or an empty string for the default one
	 */
	private static String getApiKey() {
		String apiKey = ""; //$NON-NLS-1$
		IPreferenceStore prefStore = TranslationPlugin.getDefault().getPreferenceStore();
		if (!prefStore.isDefault(GoogleTranslatorConstants.API_KEY_VALUE_PREFERENCE)) {
			apiKey = prefStore.getString(GoogleTranslatorConstants.API_KEY_VALUE_PREFERENCE);
			if (apiKey == null) {
				apiKey = ""; //$NON-NLS-1$
			}
		}

		return apiKey;
	}

}
