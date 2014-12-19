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

package org.eclipse.andmore.android.db.core.ui.wizards.createdb;

import java.util.List;

import org.eclipse.andmore.android.db.core.DbCoreActivator;
import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.model.TableModel;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class CreateDatabaseWizard extends Wizard {
	private static final String WIZBAN_ICON = "icons/wizban/create_database_ban.png"; //$NON-NLS-1$

	// it is an old plugin because of backward compatibility
	private static final String DB_PERSPECTIVE = "org.eclipse.andmore.android.db.perspective"; //$NON-NLS-1$

	private static final String SWITCH_MOTODEV_DATABASE_PERSPECTIVE = "switch.perspective.to.motodevstudio.database"; //$NON-NLS-1$

	private final List<String> alreadyAvailableDbs;

	private CreateDatabaseWizardPage createDatabaseWizardPage;

	private String dbName;

	private List<TableModel> tables;

	public CreateDatabaseWizard(final List<String> alreadyAvailableDbs) {
		this.alreadyAvailableDbs = alreadyAvailableDbs;

		setWindowTitle(DbCoreNLS.CreateDatabaseWizardPage_UI_PageTitle);
		setDefaultPageImageDescriptor(AbstractUIPlugin
				.imageDescriptorFromPlugin(DbCoreActivator.PLUGIN_ID, WIZBAN_ICON));
		setNeedsProgressMonitor(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		createDatabaseWizardPage = new CreateDatabaseWizardPage(alreadyAvailableDbs);
		addPage(createDatabaseWizardPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		dbName = createDatabaseWizardPage.getDatabaseName();
		tables = createDatabaseWizardPage.getTables();
		boolean canProceed = (dbName != null) && !"".equals(dbName); //$NON-NLS-1$
		if (canProceed) {
			changePerspective();
		}
		return canProceed;
	}

	/**
	 * @return the dbName
	 */
	public String getDbName() {
		return dbName;
	}

	/**
	 * @return the tables
	 */
	public List<TableModel> getTables() {
		return tables;
	}

	private boolean confirmPerspectiveSwitch(IWorkbenchWindow window, IPerspectiveDescriptor perspective) {
		IPreferenceStore store = DbCoreActivator.getDefault().getPreferenceStore();
		String preference = store.getString(SWITCH_MOTODEV_DATABASE_PERSPECTIVE);

		if (preference.equals("")) //$NON-NLS-1$
		{
			store.setValue(SWITCH_MOTODEV_DATABASE_PERSPECTIVE, MessageDialogWithToggle.PROMPT);
			preference = MessageDialogWithToggle.PROMPT;
		}

		boolean result;

		if (MessageDialogWithToggle.ALWAYS.equals(preference)) {
			result = true;
		} else if (MessageDialogWithToggle.NEVER.equals(preference)) {
			result = false;
		} else {
			MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(window.getShell(),
					DbCoreNLS.UI_CreateDatabaseWizard_ChangePerspectiveTitle,
					DbCoreNLS.UI_CreateDatabaseWizard_ChangePerspectiveQuestion, null, false, store,
					SWITCH_MOTODEV_DATABASE_PERSPECTIVE);
			int dialogResult = dialog.getReturnCode();

			result = dialogResult == IDialogConstants.YES_ID;
		}

		return result;
	}

	/**
	 * Changes the perspective
	 * 
	 */
	private void changePerspective() {
		IPerspectiveRegistry reg = PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor perspective = reg.findPerspectiveWithId(DB_PERSPECTIVE);

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IPerspectiveDescriptor currentPersp = page.getPerspective();

			if ((currentPersp != null) && !DB_PERSPECTIVE.contains(currentPersp.getId())) {
				boolean changePerspective = confirmPerspectiveSwitch(window, perspective);
				if (changePerspective) {
					page.setPerspective(perspective);
				}
			}
		}
	}

}
