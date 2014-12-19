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

package org.eclipse.andmore.android.generatemenucode.ui;

import java.util.List;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.generatecode.JDTUtils;
import org.eclipse.andmore.android.generatemenucode.model.codegenerators.CodeGeneratorDataBasedOnMenu;
import org.eclipse.andmore.android.generatemenucode.model.codegenerators.JavaModifierBasedOnMenu;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog to generate code to deal with Android menus base on a selected
 * menu.xml file
 */
public class GenerateMenuCodeDialog extends TitleAreaDialog {

	private ICompilationUnit javaFile;

	private IProject javaProject = null;

	private JavaModifierBasedOnMenu modifier;

	private Combo projectNameComboBox;

	private Combo classNameComboBox;

	private Combo menuFileNameComboBox;

	private String menuFileErrorMessage;

	private final String helpID = CodeUtilsActivator.PLUGIN_ID + ".generate-code-from-context-menu-dialog"; //$NON-NLS-1$

	private Image image = null;

	private List<IType> availableFragmentClasses = null;

	private final String defaultMessage;

	private final String title;

	private final String shellTitle;

	/**
	 * Default constructor for the dialog
	 * 
	 * @param parentShell
	 *            shell to open the dialog
	 * @param description
	 *            text to show dialog
	 * @param title
	 *            text
	 * @param shellTitle
	 *            window text
	 * @param image
	 *            icon to show in the dialog
	 */
	public GenerateMenuCodeDialog(Shell parentShell, String description, String title, String shellTitle, Image image) {
		super(parentShell);
		this.defaultMessage = description != null ? description : ""; //$NON-NLS-1$
		this.title = title != null ? title : ""; //$NON-NLS-1$
		this.shellTitle = shellTitle != null ? shellTitle : ""; //$NON-NLS-1$
		this.image = image;
	}

	/**
	 * Set the initial values for project name and class name, if there is some
	 * selected. Also, set the modifier that will be used to generate code - the
	 * dialog set its codeGeneratorData according to the selected menu.
	 * 
	 * @param modifier
	 *            modifier that will have its codeGeneratorData set
	 * @param javaProject
	 *            the project that will be selected when the dialog appears
	 * @param javaFile
	 *            the class that will be selected when the dialog appears
	 * */
	public void init(JavaModifierBasedOnMenu modifier, IProject javaProject, IFile javaFile) {
		setJavaModifier(modifier);
		setJavaProject(javaProject);
		setJavaFile(javaFile);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control c = super.createContents(parent);
		setTitle(title);
		if (image != null) {
			setTitleImage(image);
		}
		validate();
		return c;
	}

	@Override
	protected final Control createDialogArea(Composite parent) {
		if (helpID != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, helpID);
		}
		Composite parentComposite = (Composite) super.createDialogArea(parent);
		Composite mainComposite = new Composite(parentComposite, SWT.NULL);
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		mainComposite.setLayout(new GridLayout(2, false));
		createProjectNameArea(mainComposite);
		createClassNameArea(mainComposite);
		createMenuFileNameArea(mainComposite);

		Label separator = new Label(mainComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

		return parentComposite;

	}

	/**
	 * Create GUI items for project name selection.
	 * 
	 * @param optionsComposite
	 */
	private void createProjectNameArea(Composite parent) {
		Label projectLabel = new Label(parent, SWT.NONE);
		projectLabel.setText(CodeUtilsNLS.GenerateMenuCodeDialog_ProjectLabel);
		projectLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

		projectNameComboBox = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		projectNameComboBox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		projectNameComboBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setJavaProject((IProject) projectNameComboBox.getData(projectNameComboBox.getText()));
				populateClasses();
				populateMenuFileNames();
			}
		});
		populateProjects();
	}

	/**
	 * Create GUI items for class name selection.
	 * 
	 * @param parent
	 */
	private void createClassNameArea(Composite parent) {
		Label classLabel = new Label(parent, SWT.NONE);
		classLabel.setText(CodeUtilsNLS.GenerateMenuCodeDialog_TargetClassLabel);
		classLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

		classNameComboBox = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		classNameComboBox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		classNameComboBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setJavaFile(((IType) classNameComboBox.getData(classNameComboBox.getText())).getCompilationUnit());
				populateMenuFileNames();
			}
		});
		populateClasses();
	}

	/**
	 * Create GUI items for layout file name selection.
	 * 
	 * @param parent
	 */
	private void createMenuFileNameArea(Composite parent) {
		Label menuFileLabel = new Label(parent, SWT.NONE);
		menuFileLabel.setText(CodeUtilsNLS.GenerateMenuCodeDialog_MenuFileLabel);
		menuFileLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

		menuFileNameComboBox = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		menuFileNameComboBox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));

		menuFileNameComboBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validateMenuFileNames(menuFileNameComboBox.getText());
			}
		});
		populateMenuFileNames();

	}

	/**
	 * Populate the combobox that holds projects, with information gathered from
	 * the ResourcesPlugin. also selects the project set in the init method
	 */
	private void populateProjects() {
		if (projectNameComboBox != null) {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			int i = 0, selectedProjectIndex = -1;

			for (IProject prj : projects) {
				try {
					if (prj.hasNature(IAndroidConstants.ANDROID_NATURE)) {
						projectNameComboBox.add(prj.getName());
						projectNameComboBox.setData(prj.getName(), prj);
						if ((javaProject != null) && prj.equals(javaProject)) {
							selectedProjectIndex = i;
						}
						i++;
					}
				} catch (CoreException e) {
					StudioLogger.info("Project nature could not be checked."); //$NON-NLS-1$
				}
			}

			if (projectNameComboBox.getItemCount() > 0) {
				if (selectedProjectIndex == -1) {
					projectNameComboBox.select(0);
					setJavaProject((IProject) projectNameComboBox.getData(projectNameComboBox.getText()));
				} else {
					projectNameComboBox.select(selectedProjectIndex);
				}
			}

		}
		validate();
	}

	/**
	 * Populate the combobox that holds class names.
	 */
	private void populateClasses() {
		if (classNameComboBox != null) {
			classNameComboBox.removeAll();
			if (javaProject != null) {
				int i = 0, selectedTypeIndex = -1;
				try {
					List<IType> availableClasses = JDTUtils.getAvailableActivities(javaProject,
							new NullProgressMonitor());

					// Fragment classes can have menu too, so add all fragment
					// classes to menu combo box

					if (availableFragmentClasses != null) {
						availableFragmentClasses.clear();
					}
					availableFragmentClasses = JDTUtils.getAvailableFragmentsSubclasses(
							(IProject) projectNameComboBox.getData(projectNameComboBox.getText()),
							new NullProgressMonitor());

					availableClasses.addAll(availableFragmentClasses);

					for (IType availableClass : availableClasses) {
						classNameComboBox.add(availableClass.getFullyQualifiedName());
						classNameComboBox.setData(availableClass.getFullyQualifiedName(), availableClass);
						if ((getJavaFile() != null) && availableClass.getCompilationUnit().equals(getJavaFile())) {
							selectedTypeIndex = i;
						}
						i++;
					}
					if (classNameComboBox.getItemCount() > 0) {
						if (selectedTypeIndex == -1) {
							classNameComboBox.select(0);
							setJavaFile(((IType) classNameComboBox.getData(classNameComboBox.getText()))
									.getCompilationUnit());
						} else {
							classNameComboBox.select(selectedTypeIndex);
						}
					}
				} catch (JavaModelException e) {
					StudioLogger.info("Could not get available classes for the selected project"); //$NON-NLS-1$
				}

				classNameComboBox.setEnabled(classNameComboBox.getItemCount() > 0);
			}
		}
		validate();
	}

	/**
	 * Validate the selected menu file of the combobox that holds menu file
	 * names.
	 */
	protected void validateMenuFileNames(String menuFileName) {
		menuFileErrorMessage = null;

		if ((menuFileNameComboBox != null) && (menuFileNameComboBox.getData(menuFileName) instanceof String)) {
			// If the data type is a String, it represents an error message.
			menuFileErrorMessage = (String) menuFileNameComboBox.getData(menuFileName);
		}

		validate();
	}

	/**
	 * Populate the combobox that holds menu file names.
	 */
	private void populateMenuFileNames() {
		if ((menuFileNameComboBox != null) && (getJavaFile() != null)) {
			menuFileNameComboBox.removeAll();
			menuFileErrorMessage = null;
			CompilationUnit cpAstNode = JDTUtils.parse(getJavaFile());
			if (!JDTUtils.hasErrorInCompilationUnitAstUtils(cpAstNode)) {
				IProject prj = (IProject) this.projectNameComboBox.getData(this.projectNameComboBox.getText());

				IFolder menuFolder = prj.getFolder(IAndroidConstants.FD_RESOURCES).getFolder(IAndroidConstants.FD_MENU);

				String inflatedMenu = JDTUtils.getInflatedMenuFileName(getJavaProject(), getJavaFile());

				if ((inflatedMenu != null) && (inflatedMenu.length() > 0)) {
					// the activity/fragment already inflates a menu
					// select this menu on the combo box
					try {
						inflatedMenu = inflatedMenu + '.' + IAndroidConstants.MENU_FILE_EXTENSION;
						menuFileNameComboBox.add(inflatedMenu);
						CodeGeneratorDataBasedOnMenu codeGeneratorData;
						codeGeneratorData = JDTUtils.createMenuFile(getJavaProject(), getJavaFile(), inflatedMenu,
								getTypeAssociatedToJavaFile());

						menuFileNameComboBox.setData(inflatedMenu, codeGeneratorData);
						menuFileNameComboBox.select(0);

						setMessage(CodeUtilsNLS.GenerateMenuCodeDialog_InflatedMessage, IMessageProvider.NONE);

					} catch (AndroidException e) {
						menuFileErrorMessage = e.getMessage();
						menuFileNameComboBox.setData(inflatedMenu, menuFileErrorMessage);
					}

					// Disable the menu combo box so the user cannot change the
					// menu file
					menuFileNameComboBox.select(0);
					menuFileNameComboBox.setEnabled(false);
				} else {
					// there is no inflated menu

					setMessage(defaultMessage, IMessageProvider.NONE);
					// iterate over all files inside res/menu
					try {
						for (IResource menuFile : menuFolder.members()) {
							// only consider xml files
							if ((menuFile.getType() == IResource.FILE)
									&& menuFile.getFileExtension().equals(IAndroidConstants.MENU_FILE_EXTENSION)) {

								menuFileNameComboBox.add(menuFile.getName());

								try {
									CodeGeneratorDataBasedOnMenu codeGeneratorData = JDTUtils.createMenuFile(
											getJavaProject(), getJavaFile(), menuFile.getName(),
											getTypeAssociatedToJavaFile());

									menuFileNameComboBox.setData(menuFile.getName(), codeGeneratorData);
								}

								catch (AndroidException e) {
									// The malformed xml files
									menuFileErrorMessage = e.getMessage();
									menuFileNameComboBox.setData(menuFile.getName(), menuFileErrorMessage);
								}

							}
						}
					} catch (CoreException e) {
						menuFileErrorMessage = CodeUtilsNLS.GenerateMenuCodeDialog_Error_MenuFolderDoesNotExist;
					}

					menuFileNameComboBox.select(0); // if the combo box is
													// empty, the selection is
													// ignored
					menuFileNameComboBox.setEnabled(menuFileNameComboBox.getItemCount() > 0);
				}
			} else {
				menuFileErrorMessage = CodeUtilsNLS.GenerateMenuCodeDialog_Class_Error;
				this.javaFile = null;
				menuFileNameComboBox.setEnabled(false);
			}
		} else {
			menuFileNameComboBox.setEnabled(false);
		}
		validate();
	}

	/**
	 * Retrieve the type ACTIVITY/FRAGMENT from the android class
	 * 
	 * @return
	 */
	private CodeGeneratorDataBasedOnMenu.TYPE getTypeAssociatedToJavaFile() {
		CodeGeneratorDataBasedOnMenu.TYPE type = CodeGeneratorDataBasedOnMenu.TYPE.ACTIVITY;
		for (IType availableClass : availableFragmentClasses) {
			if (availableClass.getCompilationUnit().equals(getJavaFile())) {
				type = CodeGeneratorDataBasedOnMenu.TYPE.FRAGMENT;
				break;
			}
		}
		return type;
	}

	@Override
	protected void configureShell(Shell newShell) {
		newShell.setSize(640, 280);
		newShell.setText(shellTitle);
		super.configureShell(newShell);
	}

	/**
	 * Validate the UI
	 * 
	 * @return
	 */
	protected void validate() {

		String errorMessage = null;

		if ((projectNameComboBox != null) && (projectNameComboBox.getItemCount() == 0)) {
			errorMessage = CodeUtilsNLS.GenerateMenuCodeDialog_NoSuitableProjects;
		}
		if ((errorMessage == null) && (classNameComboBox != null) && (classNameComboBox.getItemCount() == 0)) {
			errorMessage = CodeUtilsNLS.GenerateMenuCodeDialog_NoSuitableClasses;
		}
		if ((errorMessage == null) && (menuFileNameComboBox != null)) {
			if ((menuFileErrorMessage != null)
					&& (menuFileNameComboBox.getSelectionIndex() >= 0)
					&& (menuFileNameComboBox.getData(menuFileNameComboBox.getItem(menuFileNameComboBox
							.getSelectionIndex())) instanceof String)) {
				errorMessage = (String) menuFileNameComboBox.getData(menuFileNameComboBox.getItem(menuFileNameComboBox
						.getSelectionIndex()));
			} else {
				if ((menuFileNameComboBox.getItemCount() == 0) && (getJavaFile() != null)) {
					errorMessage = CodeUtilsNLS.GenerateMenuCodeDialog_NoSuitableMenus;
				} else if (getJavaFile() == null) {
					errorMessage = menuFileErrorMessage;
				}
			}
		}

		this.setMessage(errorMessage, IMessageProvider.ERROR);

		if (errorMessage == null) {
			if ((this.getMessage() == null) || (this.getMessage().length() == 0)) {
				this.setMessage(defaultMessage, IMessageProvider.NONE);
			}
		}

		if (getButton(OK) != null) {
			getButton(OK).setEnabled(
					(getErrorMessage() == null)
							|| ((getErrorMessage() != null) && (getErrorMessage().trim().isEmpty())));
		}
	}

	private void setJavaFile(IFile javaFile) {
		if (javaFile != null) {
			setJavaFile(JavaCore.createCompilationUnitFrom(javaFile));
		}
	}

	private void setJavaFile(ICompilationUnit javaFile) {
		this.javaFile = javaFile;
		setJavaProject(javaFile.getJavaProject().getProject());
	}

	/**
	 * @return Compilation unit for the selected file (activity or fragment)
	 */
	public ICompilationUnit getJavaFile() {
		return this.javaFile;
	}

	private void setJavaProject(IProject javaProject) {
		this.javaProject = javaProject;
	}

	private IProject getJavaProject() {
		return this.javaProject;
	}

	/**
	 * Returns CodeGeneratorDataBasedOnMenu according to the selected menu file,
	 * or null if no menu is selected.
	 * */
	private CodeGeneratorDataBasedOnMenu getCodeGeneratorData() {
		CodeGeneratorDataBasedOnMenu result = null;
		if (menuFileNameComboBox.getSelectionIndex() >= 0) {
			result = (CodeGeneratorDataBasedOnMenu) this.menuFileNameComboBox.getData(menuFileNameComboBox.getText());
		}

		return result;
	}

	/**
	 * @return the modifier responsible to modify the source code
	 */
	public JavaModifierBasedOnMenu getJavaModifier() {
		return modifier;
	}

	/**
	 * @param modifier
	 *            the modifier to set
	 */
	public void setJavaModifier(JavaModifierBasedOnMenu modifier) {
		this.modifier = modifier;
	}

	@Override
	protected void okPressed() {
		modifier.setCodeGeneratorData(getCodeGeneratorData());

		super.okPressed();
	}

	/**
	 * Sets the focus of the content area of the dialog
	 */
	public void setFocus() {
		this.getContents().setFocus();
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
}