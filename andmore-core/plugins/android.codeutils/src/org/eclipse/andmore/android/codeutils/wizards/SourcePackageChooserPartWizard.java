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
package org.eclipse.andmore.android.codeutils.wizards;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.swt.widgets.Composite;

/**
 * <p>
 * Class which holds a Source/Package selection part. It is a workaround in
 * order to use Source/Package selection twice in the same page of a certain
 * wizard.
 * </p>
 * <p>
 * Despite the fact the this class implements <code>NewTypeWizardPage</code>, it
 * should be be used as a wizard. It is to be used as a part of it to add a
 * Source/Package functionality.
 * </p>
 */
@SuppressWarnings("restriction")
public class SourcePackageChooserPartWizard extends NewTypeWizardPage {
	/**
	 * Command representing the event of a message dispatched.
	 */
	public static final String MESSAGE_DISPATCHED_ACTION = "MESSAGE_DISPATCHED";

	/**
	 * Id representing the action of a message dispatched.
	 */
	public static final int MESSAGE_DISPATCHED_ID_ACTION = 0;

	private int numColumnsGridLayout = 0;

	private final List<ActionListener> messageActionListenerList = new ArrayList<ActionListener>();

	private IStatus mostSevereStatus;

	/**
	 * Get the most severe status of this Part Page.
	 * 
	 * @return The most severe status.
	 */
	public IStatus getMostSevereStatus() {
		return mostSevereStatus;
	}

	/**
	 * Constructor.
	 * 
	 * @param pageName
	 *            Page Name
	 * @param project
	 *            Project related to the wizard
	 * @param defaultPackageName
	 *            The name of the default package to use on the package field
	 * @param parent
	 *            The parent composite
	 * @param numColumnsGridLayout
	 *            The number of columns on the grid layout
	 */
	public SourcePackageChooserPartWizard(String pageName, IProject project, String defaultPackageName,
			Composite parent, int numColumnsGridLayout) {
		super(true, pageName);
		// set description and title
		setDescription(CodeUtilsNLS.UI_PersistenceWizardPageDescriptionDeploy);
		setTitle(CodeUtilsNLS.UI_PersistenceWizardPageTitleDeploy);

		// set attributes
		this.numColumnsGridLayout = numColumnsGridLayout;
		if (project != null) {
			// get the java project
			IJavaProject javaProject = JavaCore.create(project);
			IPackageFragmentRoot[] possibleRoots = null;
			// continue in case it does exist
			if (javaProject != null) {
				try {
					// get all possible roots
					possibleRoots = javaProject.getPackageFragmentRoots();
					// select the first one, in case it does exist
					if ((possibleRoots != null) && (possibleRoots.length > 0)) {
						// set the first one
						setPackageFragmentRoot(possibleRoots[0], true);
					}
				} catch (JavaModelException e) {
					StudioLogger.error(this.getClass(), CodeUtilsNLS.Db_GenerateManagementClassesError, e);
					IStatus status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID, e.getLocalizedMessage());
					EclipseUtils.showErrorDialog(CodeUtilsNLS.Db_GenerateManagementClassesError,
							CodeUtilsNLS.Db_GenerateManagementClassesError, status);
				}
			}
		}

		doStatusUpdate();

		// create GUI here because since this GUI is an auxiliary one, the
		// interface does not work when created in the
		// org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite
		// method
		setPackageFragmentRoot(getPackageFragmentRoot(), true);
		createContainerControls(parent, this.numColumnsGridLayout);

		boolean defaultPackageUsed = false;
		if (defaultPackageName != null) {
			// try to use the manifest package, but if this fails, use the
			// default getPackageFragment() logic
			try {
				setPackageFragment(getPackageFragmentRoot().getPackageFragment(defaultPackageName), true);
				defaultPackageUsed = true;
			} catch (Exception e) {
				// do nothing
			}
		}
		if (!defaultPackageUsed) {
			setPackageFragment(getPackageFragment(), true);
		}
		createPackageControls(parent, this.numColumnsGridLayout);
	}

	/**
	 * Add an action listener to which notificates the caller when a message is
	 * dispatched.
	 * 
	 * @param actionListener
	 *            Listener to be notified when some field changed.
	 */
	public void addMessageNotificationActionListener(ActionListener actionListener) {
		messageActionListenerList.add(actionListener);
	}

	/*
	 * @see NewContainerWizardPage#handleFieldChanged
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);

		doStatusUpdate();

		ActionEvent actionEvent = new ActionEvent(this, MESSAGE_DISPATCHED_ID_ACTION, MESSAGE_DISPATCHED_ACTION);

		// execute listeners
		for (ActionListener listener : messageActionListenerList) {
			listener.actionPerformed(actionEvent);
		}
	}

	private void doStatusUpdate() {
		// status of all used components
		IStatus[] status = new IStatus[] { fContainerStatus,
				isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus, };

		this.mostSevereStatus = StatusUtil.getMostSevere(status);
		if (this.mostSevereStatus != null) {
			StatusUtil.applyToStatusLine(this, this.mostSevereStatus);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		// do nothing
	}
}
