/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Mobility, Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributors:
 * Otavio Ferranti (Motorola)
 * 
 * Contributors:
 * Fabio Rigo (Eldorado) - Bug [288006] - Unify features of InstanceManager and InstanceRegistry
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Daniel Pastore (Eldorado) - [278436] New device wizard does not offer a way to set its window title
 * 
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.wizard.ui;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.ui.wizard.DeviceWizardResources;
import org.eclipse.sequoyah.device.framework.wizard.model.IWizardProjectPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * Creates a new instance wizard.
 * @author Otavio Ferranti
 *
 */
public class WizardNewProjectPage extends WizardPage implements IWizardProjectPage {

    // constants
    private static final String INSTANCE_NAME = "instance name"; //$NON-NLS-1$
	private static final int SIZING_HOST_FIELD_WIDTH = 250;
    	
	private String initialProjectName = ""; //$NON-NLS-1$
	private String projectName;
    
    private Text projectNameField;
	
   
    /**
     * Constructor - Creates a new project creation wizard page.
     *
     * @param pageName the name of this page
     */
    public WizardNewProjectPage(String pageName) {
        super(pageName);
        setPageComplete(false);
    }

    /**
     * Creates the project name specification controls.
     *
     * @param parent the parent composite
     */
    private final void createPropertyGroup(Composite parent) {
        // project specification group
        Composite propertyGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        propertyGroup.setLayout(layout);
        propertyGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // new project label
        Label projectNameLabel = new Label(propertyGroup, SWT.NONE);
        projectNameLabel.setText(WizardNewProjectPage.INSTANCE_NAME);
        projectNameLabel.setFont(parent.getFont());

        projectNameField = new Text(propertyGroup, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = SIZING_HOST_FIELD_WIDTH;
        projectNameField.setLayoutData(data);
        projectNameField.setFont(parent.getFont());
        
        // Set the initial value first before listener
        // to avoid handling an event during the creation.
        if (initialProjectName != null) {
        	projectNameField.setText(initialProjectName);
		}
        projectNameField.addListener(SWT.Modify, new Listener() {
	            public void handleEvent(Event e) {
	            	//setDisplayForSelection();
	                boolean valid = validatePage();
	                setPageComplete(valid);
	            }
	        });
    }

	/**
	 * Returns whether this page's controls currently all contain valid 
	 * values.
	 *
	 * @return <code>true</code> if all controls are valid, and
	 *   <code>false</code> if at least one is invalid
	 */
    protected boolean validatePage() {
    	InstanceRegistry registry = InstanceRegistry.getInstance();
    	String name = projectNameField.getText();
    	String errorMessage = null;
    	boolean retVal = false;
    	
    	if (name != null) {
    		name = name.trim();
    		if (!name.equals("")) { //$NON-NLS-1$
	        	if (registry.getInstancesByName(name).size() == 0) {
	        		retVal = true;
	        	} else {
	        		errorMessage =
	        			DeviceWizardResources.SEQUOYAH_Emulator_Wizard_Project_Description_Duplicated_Error;
	        	}
	        }
     	}
    	
    	this.setErrorMessage(errorMessage);
        return retVal;
    }
    
    /* (non-Javadoc)
     * 
     * Method declared on IDialogPage.
     */
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setFont(parent.getFont());

        initializeDialogUnits(parent);

        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        createPropertyGroup(composite);
		
        setPageComplete(validatePage());
        // Show description on opening
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);
    }
    
    /**
     * Retrieves the project name.
     * 
     * @return The project name.
     * @see org.eclipse.sequoyah.device.framework.wizard.model.IWizardProjectPage#getProjectName()
     */
    public String getProjectName() {
        if (projectNameField == null) {
			return initialProjectName;
		}
        projectName = projectNameField.getText().trim();	
		return projectName;
    }
    
    /*
     * see @DialogPage.setVisible(boolean)
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
			projectNameField.setFocus();
		}
    }
}

