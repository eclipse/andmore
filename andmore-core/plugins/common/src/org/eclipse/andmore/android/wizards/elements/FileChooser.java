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
package org.eclipse.andmore.android.wizards.elements;

import org.eclipse.andmore.android.common.CommonPlugin;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * Composite to allow users to choose between filesystem and workspace files
 */
public class FileChooser extends Composite {
	/*
	 * Label: TextField Workspace... Filesystem...
	 */
	private final String label;

	private Label fileLabel = null;

	private Text pathWidget = null;

	private Button filesystem = null, workspace = null;

	private String[] extensionFilter = null;

	private IContainer container;

	/**
	 * Constructs a new FileChooser object
	 * 
	 * @param parent
	 *            the parent composite
	 * @param style
	 *            the style
	 * @param label
	 *            A label to put before the path text or null to no label
	 */
	public FileChooser(Composite parent, int style, String label) {
		super(parent, style);
		this.label = label;
		setupLayout();
		createFields();
	}

	/**
	 * Constructs a new FileChooser object
	 * 
	 * @param container
	 *            Container in which limits the range of the filter. For
	 *            instance if a project is passed as a container, all the search
	 *            will be done within the passed-project.
	 * @param parent
	 *            the parent composite
	 * @param style
	 *            the style
	 * @param label
	 *            A label to put before the path text or null to no label
	 */
	public FileChooser(IContainer container, Composite parent, int style, String label) {
		// call constructor
		this(parent, style, label);
		// assign container
		this.container = container;
	}

	/**
	 * Setup this composite layout
	 */
	private void setupLayout() {
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	private void createFields() {
		GridData gridData;
		if (label != null) {
			fileLabel = new Label(this, SWT.NONE);
			fileLabel.setText(label);
			gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
			fileLabel.setLayoutData(gridData);
		}

		pathWidget = new Text(this, SWT.BORDER);
		gridData = new GridData(SWT.FILL, SWT.CENTER, true, false, label != null ? 1 : 2, 1);
		pathWidget.setLayoutData(gridData);

		Composite buttonsComposite = new Composite(this, SWT.NONE);
		gridData = new GridData(SWT.RIGHT, SWT.CENTER, true, false, 2, 1);
		buttonsComposite.setLayoutData(gridData);
		buttonsComposite.setLayout(new RowLayout(SWT.HORIZONTAL));

		filesystem = new Button(buttonsComposite, SWT.PUSH);
		filesystem.setLayoutData(new RowData());
		filesystem.setText(UtilitiesNLS.UI_FileChooser_Filesystem);
		filesystem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
				fileDialog.setFilterExtensions(extensionFilter);
				fileDialog.setFilterPath(pathWidget.getText());
				String returnedPath = fileDialog.open();
				if (returnedPath != null) {
					pathWidget.setText(returnedPath);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});

		workspace = new Button(buttonsComposite, SWT.PUSH);
		workspace.setLayoutData(new RowData());
		workspace.setText(UtilitiesNLS.UI_FileChooser_Workspace);
		workspace.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
						new WorkbenchLabelProvider(), new WorkbenchContentProvider());
				dialog.setTitle(UtilitiesNLS.UI_FileChooser_Dialog_Title);
				dialog.setMessage(UtilitiesNLS.UI_FileChooser_Dialog_Message);
				// set the input depending whether the container exists
				if (container != null) {
					// the container exists, set it as the limit
					dialog.setInput(container);
				} else {
					// the container does not exists, set the workspace as the
					// limit
					dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
				}
				dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
				// filter extensions
				dialog.addFilter(new ViewerFilter() {

					@Override
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						boolean filtered = false;

						if ((extensionFilter != null) && (element instanceof IFile)) {
							IFile file = (IFile) element;
							int i = 0;
							while ((i < extensionFilter.length) && !filtered) {
								String fileExtensionToShow = extensionFilter[i].substring(extensionFilter[i]
										.lastIndexOf(".") + 1);
								String fileExtension = file.getFileExtension();
								if ((fileExtension != null) && !fileExtension.equals(fileExtensionToShow)) {
									filtered = true;
								}
								i++;
							}

						}
						return !filtered;
					}
				});
				// user can select only one FILE
				dialog.setValidator(new ISelectionStatusValidator() {

					@Override
					public IStatus validate(Object[] selection) {
						IStatus valid = new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
						if (selection.length == 1) {
							if (selection[0] instanceof IFile) {
								valid = new Status(IStatus.OK, CommonPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
							}
						}
						return valid;
					}
				});

				if (dialog.open() == IDialogConstants.OK_ID) {
					IResource resource = (IResource) dialog.getFirstResult();
					pathWidget.setText(resource.getLocation().toOSString());
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (fileLabel != null) {
			fileLabel.setEnabled(enabled);
		}
		workspace.setEnabled(enabled);
		filesystem.setEnabled(enabled);
		pathWidget.setEnabled(enabled);
	}

	/**
	 * 
	 * @return the text with the path of the file selected
	 */
	public String getText() {
		return pathWidget.getText();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.FileDialog#setFilterExtensions(String[])
	 */
	public void setFilterExtensions(String[] filter) {
		extensionFilter = filter;
	}

	public void setText(String text) {
		pathWidget.setText(text);
	}

	/**
	 * Add modify text listener
	 * 
	 * @param modifyListener
	 */
	public void addModifyListener(ModifyListener modifyListener) {
		pathWidget.addModifyListener(modifyListener);
	}

	/**
	 * Sets the container.
	 * 
	 * @param container
	 */
	public void setContainer(IContainer container) {
		this.container = container;
	}

}
