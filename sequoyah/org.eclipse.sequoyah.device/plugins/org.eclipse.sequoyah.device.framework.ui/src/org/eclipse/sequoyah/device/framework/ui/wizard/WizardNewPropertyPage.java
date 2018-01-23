/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Motorola)
 *
 * Contributors:
 * Fabio Fantato (Motorola) - bug#221733 - code revisited
 * Otavio Luiz Ferranti (Eldorado Research Institute) - bug#221733 - Cleanup.
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui.wizard;

import java.util.Properties;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.common.utilities.IPropertyConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class WizardNewPropertyPage extends WizardPage implements IWizardPropertyPage {

	private String host;
	private String display;
	private String port;

    // widgets
    private Text hostField;
    private Text displayField;
    private Text portField;
	
    // constants
    private static final int SIZING_HOST_FIELD_WIDTH = 250;
    private static final int SIZING_DISPLAY_FIELD_WIDTH = 50;
    private static final int SIZING_PORT_FIELD_WIDTH = 20;
	
    /**
     * Creates a new project creation wizard page.
     *
     * @param pageName the name of this page
     */
    public WizardNewPropertyPage(String pageName) {
        super(pageName);
        setPageComplete(false);
    }

    /** (non-Javadoc)
     * Method declared on IDialogPage.
     */
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setFont(parent.getFont());

        initializeDialogUnits(parent);

        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        createPropertyGroup(composite);
	
        setPageComplete(true);
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);
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
        Label hostLabel = new Label(propertyGroup, SWT.NONE);
        hostLabel.setText("Host"); //$NON-NLS-1$
        hostLabel.setFont(parent.getFont());

        hostField = new Text(propertyGroup, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = SIZING_HOST_FIELD_WIDTH;
        hostField.setLayoutData(data);
        hostField.setFont(parent.getFont());
        
        Label displayLabel = new Label(propertyGroup, SWT.NONE);
        displayLabel.setText("Display"); //$NON-NLS-1$
        displayLabel.setFont(parent.getFont());
        
        displayField = new Text(propertyGroup, SWT.BORDER);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = SIZING_DISPLAY_FIELD_WIDTH;
        displayField.setLayoutData(data);
        displayField.setFont(parent.getFont());
    
        Label portLabel = new Label(propertyGroup, SWT.NONE);
        portLabel.setText("Port"); //$NON-NLS-1$
        portLabel.setFont(parent.getFont());
        
        portField = new Text(propertyGroup, SWT.BORDER);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = SIZING_PORT_FIELD_WIDTH;
        portField.setLayoutData(data);
        portField.setFont(parent.getFont());
    }
       
    public String getHost() {
        if (hostField != null) {
            host = hostField.getText().trim();
		}
		return host;
    }

    public String getDisplay() {
        if (displayField != null) {
        	display = displayField.getText().trim();
		}
		return display;
    }
    
    public String getPort() {
        if (portField != null) {
        	port = portField.getText().trim();
		}
		return port;
    }

    /*
     * see @DialogPage.setVisible(boolean)
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
			hostField.setFocus();
		}
    }
	
	/* (non-Javadoc)
	 * @see org.eclipse.sequoyah.device.framework.wizard.model.IWizardPropertyPage#getProperties()
	 */
	public Properties getProperties(){
		Properties properties = new Properties();
		properties.setProperty(IPropertyConstants.HOST, getHost());
		properties.setProperty(IPropertyConstants.DISPLAY, getDisplay());
		properties.setProperty(IPropertyConstants.PORT, getPort());
		return properties;
	}
}
