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
package org.eclipse.andmore.android.generateviewbylayout.ui;

import java.util.List;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.generatecode.JDTUtils;
import org.eclipse.andmore.android.generateviewbylayout.JavaModifierBasedOnLayout;
import org.eclipse.andmore.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;
import org.eclipse.andmore.android.generateviewbylayout.model.JavaLayoutData;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;

/**
 * Abstract dialog to deal with common methods for the UI dialog involved into
 * the code generation
 */
public abstract class AbstractLayoutItemsDialog extends TitleAreaDialog {

	private JavaModifierBasedOnLayout modifier;

	private ICompilationUnit javaFile;

	private IProject javaProject;

	private CodeGeneratorDataBasedOnLayout codeGeneratorData;

	private Combo projectNameComboBox;

	private Combo classNameComboBox;

	private Label layoutFileNameLabel;

	private TableViewer viewer;

	private Button unselectAllButton;

	private Button selectAllButton;

	private String layoutFileErrorMessage;

	private String helpID = null;

	protected static final String DEFAULT_LAYOUT_FILE_NAME_LABEL_VALUE = "--"; //$NON-NLS-1$

	private final String defaultMessage;

	private final String title;

	private final String shellTitle;

	private final Image image;

	public AbstractLayoutItemsDialog(Shell parentShell, String description, String title, String shellTitle, Image image) {
		super(parentShell);
		this.defaultMessage = description != null ? description : ""; //$NON-NLS-1$
		this.title = title != null ? title : ""; //$NON-NLS-1$
		this.shellTitle = shellTitle != null ? shellTitle : ""; //$NON-NLS-1$
		this.image = image;
	}

	/**
	 * Initializes the modifier to enable code generation
	 * 
	 * @param modifier
	 *            the responsible to modify code
	 * @param javaProject
	 *            the project of the Android file to modify
	 * @param javaFile
	 *            the Android file to change
	 */
	public void init(JavaModifierBasedOnLayout modifier, IProject javaProject, IFile javaFile) {
		setModifier(modifier);
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
		Composite mainComposite = new Composite(parentComposite, SWT.NONE);
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		mainComposite.setLayout(new GridLayout(2, false));
		createProjectNameArea(mainComposite);
		createClassNameArea(mainComposite);
		createLayoutFileNameArea(mainComposite);

		Label separator = new Label(mainComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

		createTableArea(mainComposite);
		createCustomContentArea(mainComposite);

		separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 2, 1));

		return parentComposite;

	}

	/**
	 * Create a custom area in the end of the dialog The parent composite has a
	 * grid layout with two columns
	 * 
	 * @param mainComposite
	 */
	protected abstract void createCustomContentArea(Composite mainComposite);

	/**
	 * Create GUI items for project name selection.
	 * 
	 * @param optionsComposite
	 */
	private void createProjectNameArea(Composite parent) {
		Label projectLabel = new Label(parent, SWT.NONE);
		projectLabel.setText(CodeUtilsNLS.ChooseLayoutItemsDialog_Project);
		projectLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

		projectNameComboBox = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		projectNameComboBox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		projectNameComboBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setJavaProject((IProject) projectNameComboBox.getData(projectNameComboBox.getText()));
				setErrorMessage(null);
				javaFile = null;
				layoutFileErrorMessage = null;
				codeGeneratorData = null;
				populateClasses();
				populateLayouts();
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
		classLabel.setText(CodeUtilsNLS.ChooseLayoutItemsDialog_TargetClass);
		classLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

		classNameComboBox = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		classNameComboBox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		classNameComboBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setJavaFile(((IType) classNameComboBox.getData(classNameComboBox.getText())).getCompilationUnit());
				codeGeneratorData = null;
				populateLayouts();
			}
		});
		populateClasses();
	}

	/**
	 * Create GUI items for layout file name selection.
	 * 
	 * @param parent
	 */
	private void createLayoutFileNameArea(Composite parent) {
		Label layoutFileLabel = new Label(parent, SWT.NONE);
		layoutFileLabel.setText(CodeUtilsNLS.ChooseLayoutItemsDialog_SourceLayoutFile);
		layoutFileLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

		layoutFileNameLabel = new Label(parent, SWT.NONE);
		layoutFileNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		layoutFileNameLabel.setText(DEFAULT_LAYOUT_FILE_NAME_LABEL_VALUE);
		populateLayouts();

	}

	/**
	 * Create GUI items for GUI Items selection.
	 * 
	 * @param parent
	 */
	private void createTableArea(Composite parent) {
		Composite tableArea = new Composite(parent, SWT.NONE);
		tableArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		tableArea.setLayout(new GridLayout(2, false));

		Label tableLabel = new Label(tableArea, SWT.NONE);
		tableLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		tableLabel.setText(CodeUtilsNLS.ChooseLayoutItemsDialog_GUIItems);

		viewer = new TableViewer(tableArea, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER
				| SWT.CHECK);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Composite buttonsComposite = new Composite(tableArea, SWT.NONE);
		buttonsComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, true, 1, 1));
		RowLayout buttonsLayout = new RowLayout(SWT.VERTICAL);
		buttonsLayout.pack = false;
		buttonsComposite.setLayout(buttonsLayout);

		selectAllButton = new Button(buttonsComposite, SWT.PUSH);
		selectAllButton.setText(CodeUtilsNLS.UI_SelectAll);
		selectAllButton.setLayoutData(new RowData());
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkAllItems(true);
				validate();
			}

		});

		unselectAllButton = new Button(buttonsComposite, SWT.PUSH);
		unselectAllButton.setText(CodeUtilsNLS.UI_UnselectAll);
		unselectAllButton.setLayoutData(new RowData());
		unselectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkAllItems(false);
				validate();
			}
		});

		createColumns(viewer);

		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		ArrayContentProvider provider = new ArrayContentProvider();
		viewer.setContentProvider(provider);

		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				if (event.getSource() instanceof TableViewer) {
					Table tb = ((TableViewer) event.getSource()).getTable();
					TableItem[] items = tb.getSelection();
					items[0].setChecked(!items[0].getChecked());
					itemCheckStateChanged(items[0]);
					validate();
				}
			}
		});

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});

		populateViewer();

	}

	/**
	 * Notify when the check state changed. This is a workaround for SWT not
	 * notifying when we change widget state programatically
	 * 
	 * @param item
	 */
	protected void itemCheckStateChanged(TableItem item) {
		// default implementation does nothing
	};

	private void checkAllItems(boolean checked) {
		for (TableItem item : viewer.getTable().getItems()) {
			item.setChecked(checked);
			itemCheckStateChanged(item);
		}

	}

	/**
	 * Creates the columns for the GUI Items table.
	 * 
	 * @param viewer
	 *            the TableViewer whose columns will be created.
	 */
	protected void createColumns(final TableViewer viewer) {
		String[] titles = { CodeUtilsNLS.ChooseLayoutItemsDialog_Id, CodeUtilsNLS.ChooseLayoutItemsDialog_Type,
				CodeUtilsNLS.ChooseLayoutItemsDialog_VariableName, };
		int[] bounds = { 150, 100, 170 };

		TableViewerColumn col = createTableViewerColumn(viewer, titles[0], bounds[0], 0);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				LayoutNode node = (LayoutNode) element;
				return node.getNodeId();
			}
		});

		col = createTableViewerColumn(viewer, titles[1], bounds[1], 1);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				LayoutNode node = (LayoutNode) element;
				return node.getNodeType();
			}
		});

		col = createTableViewerColumn(viewer, titles[2], bounds[2], 2);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				LayoutNode node = (LayoutNode) element;
				return node.getNodeId();
			}
		});

	}

	/**
	 * Creates a column for the GUI Items table.
	 * 
	 * @param parent
	 */
	protected final TableViewerColumn createTableViewerColumn(TableViewer viewer, String title, int bound,
			final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;

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
					StudioLogger.info(CodeUtilsNLS.Info_ChooseLayoutItemsDialog_Project_Nature);
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

			validate();

		}
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

					availableClasses.addAll(JDTUtils.getAvailableFragmentsSubclasses(
							(IProject) projectNameComboBox.getData(projectNameComboBox.getText()),
							new NullProgressMonitor()));

					// add the fragments JDTUtils.getAvailableFragments to
					// activityClasses

					for (IType availableClass : availableClasses) {
						classNameComboBox.add(availableClass.getFullyQualifiedName());
						classNameComboBox.setData(availableClass.getFullyQualifiedName(), availableClass);
						if ((javaFile != null) && availableClass.getCompilationUnit().equals(javaFile)) {
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
					StudioLogger.info(CodeUtilsNLS.Info_ChooseLayoutItemsDialog_Available_Classes);
				}

				classNameComboBox.setEnabled(classNameComboBox.getItemCount() > 0);
			}
		}
		validate();
	}

	/**
	 * Populate the combobox that holds layout file names.
	 */
	private void populateLayouts() {
		if (layoutFileNameLabel != null) {
			layoutFileNameLabel.setText(DEFAULT_LAYOUT_FILE_NAME_LABEL_VALUE);
			layoutFileErrorMessage = null;
			IType activity = (IType) classNameComboBox.getData(classNameComboBox.getText());
			if (activity != null) {
				try {
					codeGeneratorData = JDTUtils.createLayoutFile(getJavaProject(), getJavaFile());
					JavaLayoutData viewLayoutData = getCodeGeneratorData().getJavaLayoutData();
					if ((viewLayoutData == null) || (viewLayoutData.hasErrorInCompilationUnitAst())) {
						// there are errors in the compilation unit
						layoutFileErrorMessage = CodeUtilsNLS.ChooseLayoutItemsDialog_TryToGenerateCodeWhenThereIsAnError;
						codeGeneratorData = null;
					} else {
						if (getCodeGeneratorData().getLayoutFile().getName() != null) {
							layoutFileNameLabel.setText(getCodeGeneratorData().getLayoutFile().getName());
						} else {
							layoutFileErrorMessage = CodeUtilsNLS.UI_ChooseLayoutItemsDialog_Error_onCreate_Not_Declared;
						}
					}
				} catch (AndroidException e) {
					// if layout xml is malformed indicate it on screen
					layoutFileErrorMessage = e.getMessage();
					StudioLogger.error(this.getClass(), "Error parsing layout: " + e.getMessage()); //$NON-NLS-1$
				}

				populateViewer();
			}
			validate();
		}
	}

	protected void populateViewer() {
		if (viewer != null) {
			viewer.getTable().removeAll();

			// Get the content for the viewer, setInput will call getElements in
			// the
			// contentProvider
			if (getCodeGeneratorData() != null) {
				viewer.setInput(getGuiItemsList());
				viewer.refresh();
			}

			if (viewer.getTable().getItemCount() == 0) {
				viewer.getTable().setEnabled(false);
				selectAllButton.setEnabled(false);
				unselectAllButton.setEnabled(false);
			} else {
				selectAllButton.setEnabled(true);
				unselectAllButton.setEnabled(true);
				viewer.getTable().setEnabled(true);
			}
			validate();
		}
	}

	/**
	 * Get the list of items to be displayed
	 * 
	 * @return
	 */
	protected List<LayoutNode> getGuiItemsList() {
		return getCodeGeneratorData().getGUIItems(false);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(shellTitle);
	}

	/**
	 * Validate the UI
	 * 
	 * @return
	 */
	protected void validate() {
		String errorMessage = null;

		if ((projectNameComboBox != null) && (projectNameComboBox.getItemCount() == 0)) {
			errorMessage = CodeUtilsNLS.AbstractLayoutItemsDialog_Error_No_Projects_Found;
		}
		if ((errorMessage == null) && (classNameComboBox != null) && (classNameComboBox.getItemCount() == 0)) {
			errorMessage = CodeUtilsNLS.AbstractLayoutItemsDialog_Error_No_Class_Found;
		}
		if ((errorMessage == null) && (layoutFileNameLabel != null)) {
			if (layoutFileErrorMessage != null) {
				errorMessage = layoutFileErrorMessage;
			} else if (getJavaFile() == null) {
				errorMessage = CodeUtilsNLS.AbstractLayoutItemsDialog_Error_No_Layout_Found;
			}
		}

		setErrorMessage(errorMessage);

		if (errorMessage == null) {
			setMessage(defaultMessage);
		}

		if (getButton(OK) != null) {
			getButton(OK).setEnabled((getErrorMessage() == null) && hasAtLeastOneItemChecked());
		}
	}

	private boolean hasAtLeastOneItemChecked() {
		boolean hasItemsChecked = false;

		TableItem[] items = viewer.getTable().getItems();
		int i = 0;
		while (!hasItemsChecked && (i < items.length)) {
			if (items[i++].getChecked()) {
				hasItemsChecked = true;
			}
		}

		return hasItemsChecked;
	}

	/**
	 * @return {@link ICompilationUnit} selected Android file to generate the
	 *         code
	 */
	public ICompilationUnit getJavaFile() {
		return javaFile;
	}

	/**
	 * @return {@link IProject} of the selected Android file
	 */
	public IProject getJavaProject() {
		return javaProject;
	}

	/**
	 * @return {@link JavaModifierBasedOnLayout} responsible to change the
	 *         source code of Android file
	 */
	public JavaModifierBasedOnLayout getModifier() {
		return modifier;
	}

	private void setModifier(JavaModifierBasedOnLayout modifier) {
		this.modifier = modifier;
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

	private void setJavaProject(IProject javaProject) {
		this.javaProject = javaProject;
	}

	/**
	 * @return object representing the input data (from layout XML) to use as
	 *         basis during code generation
	 */
	public CodeGeneratorDataBasedOnLayout getCodeGeneratorData() {
		return codeGeneratorData;
	}

	public void setHelpID(String helpID) {
		this.helpID = helpID;
	}

	/**
	 * @return table to select the Android GUI items to generate code for
	 */
	public TableViewer getViewer() {
		return viewer;
	}

}
