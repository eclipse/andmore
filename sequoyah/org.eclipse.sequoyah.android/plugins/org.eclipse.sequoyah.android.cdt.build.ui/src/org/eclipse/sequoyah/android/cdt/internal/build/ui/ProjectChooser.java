/*
 * @(#)ProjectChooser.java
 *
 * (c) COPYRIGHT 2010 MOTOROLA INC.
 */
package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class ProjectChooser extends Composite
{

    private Label lblProject;

    private Text txtProject;

    private Button btnBrowseProject;

    IProject project;

    public IProject getProject()
    {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();
        if ((projects != null) && (projects.length > 0))
        {
            for (IProject innerProject : projects)
            {
                if (innerProject.getName().equals(txtProject.getText()))
                {
                    this.project = innerProject;
                    break;
                }
            }
        }
        return project;
    }

    public String getText()
    {
        return txtProject != null ? txtProject.getText() : ""; //$NON-NLS-1$
    }

    public void setText(String text)
    {
        if (txtProject != null)
        {
            this.txtProject.setText(text);
        }
    }

    public void addModifyListener(ModifyListener modifyListener)
    {
        txtProject.addModifyListener(modifyListener);
    }

    public void setTextFieldEnabled(boolean enabled)
    {
        txtProject.setEnabled(enabled);
    }

    public ProjectChooser(Composite parent, int style)
    {
        super(parent, style);
        setupLayout();
        addComponents();
    }

    private void setupLayout()
    {
        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        this.setLayout(layout);
        this.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
    }

    private void addComponents()
    {
        // add project label
        lblProject = new Label(this, SWT.NONE);
        lblProject.setText(Messages.PROJECTCHOOSER_PROJECT_LABEL);
        lblProject.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

        // add text field
        txtProject = new Text(this, SWT.BORDER);
        txtProject.setText(""); //$NON-NLS-1$
        txtProject.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        // add browner button
        btnBrowseProject = new Button(this, SWT.PUSH);
        btnBrowseProject.setText(Messages.PROJECTCHOOSER_BROWSE_BUTTON);
        btnBrowseProject.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

        btnBrowseProject.addListener(SWT.Selection, new Listener()
        {
            /*
             * (non-Javadoc)
             * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
             */
            public void handleEvent(Event event)
            {
                // get the selected project
                project = openProjectChooser();
                // write the project in case there is one
                if (project != null)
                {
                    txtProject.setText(project.getName());
                }
            }
        });
    }

    private IProject openProjectChooser()
    {
        IProject selectedProject = null;
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final ElementTreeSelectionDialog packageDialog =
                new ElementTreeSelectionDialog(shell, new WorkbenchLabelProvider(),
                        new WorkbenchContentProvider());

        packageDialog.setTitle(Messages.PROJECTCHOOSER_TITLE);
        packageDialog.setMessage(Messages.PROJECTCHOOSER_MESSAGE);

        packageDialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        packageDialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
        packageDialog.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                return element instanceof IProject;
            }
        });
        packageDialog.setValidator(new ISelectionStatusValidator()
        {
            public IStatus validate(Object[] selection)
            {
                IStatus valid = new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID, ""); //$NON-NLS-1$                
                if (selection.length == 1)
                {
                    if (selection[0] instanceof IProject)
                    {
                        valid = new Status(IStatus.OK, UIPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
                    }
                }
                return valid;
            }
        });

        if (packageDialog.open() == IDialogConstants.OK_ID)
        {
            IResource resource = (IResource) packageDialog.getFirstResult();
            if (resource instanceof IProject)
            {
                selectedProject = (IProject) resource;
            }
        }
        return selectedProject;
    }
}
