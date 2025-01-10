/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Inc and others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Otavio Ferranti (Eldorado Research Institute) - bug#221733 - Adding data persistence
 * 
 * Contributors:
 * Fabio Rigo (Eldorado Research Institute) - [244951] Implement listener/event mechanism at device framework
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [271682] - Default Wizard Page accepting invalid names
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [274502] - Change labels: Instance Management view and Services label
 * Fabio Rigo (Eldorado) - Bug [288006] - Unify features of InstanceManager and InstanceRegistry
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui.editors;

import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.internal.model.MobileInstance;
import org.eclipse.sequoyah.device.framework.model.AbstractMobileInstance;
import org.eclipse.sequoyah.device.framework.ui.DeviceUIResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Basic device instance property editor
 * @author Otavio Ferranti
 */
public class InstancePropertyEditor extends PropertyPage {

	private static final String LABEL_INSTANCE_NAME = "Device name: "; //$NON-NLS-1$
	private static final String LABEL_PROPERTIES = "Properties: "; //$NON-NLS-1$
	private static final String COLUMN_NAME_KEY = "Key"; //$NON-NLS-1$
	private static final String COLUMN_NAME_VALUE = "Value"; //$NON-NLS-1$

	private Table table;
	private MobileInstance instance;
	private Text textInstanceName;
	private String initialInstanceName;
	
	/**
	 * Constructor - Creates a device instance property editor
	 */
	public InstancePropertyEditor() {
		
	}

	/**
	 * Listener handler.
	 */
	private void onMouseDoubleClick() {
		String key = table.getSelection()[0].getText(0);
		String value = table.getSelection()[0].getText(1);
		
        InputDialog dialog = new InputDialog(this.getShell(),
        		key + " Property Value", //$NON-NLS-1$
        		"Enter a new value for " + key, //$NON-NLS-1$
        		value,
        		null);
        if (dialog.open() == Window.OK) {
        	table.getSelection()[0].setText(1, dialog.getValue().trim());
        }
	}
	
    /* (non-Javadoc)
     * Called to verify if this instance name is duplicated.
     * @param instanceName
     * @return
     */
    private boolean validateName(String instanceName) {
    	InstanceRegistry registry = InstanceRegistry.getInstance();
    	String errorMessage = null;
    	boolean retVal = false;
    	
    	if (instanceName != null) {
    		//instanceName = instanceName.trim();
    		if (!instanceName.equals("")) { //$NON-NLS-1$
    			
	        	if (registry.getInstancesByName(instanceName).size() == 0 ||
	        		instanceName.equals(initialInstanceName)) {
	        		retVal = true;
	        	} else {
	        		errorMessage = DeviceUIResources.SEQUOYAH_Instance_Name_Duplicated_Error;
	        	}
	        	
	        	if (retVal) {
	        		if (!AbstractMobileInstance.validName(instanceName)) {
	        			retVal = false;
	        			errorMessage = DeviceUIResources.SEQUOYAH_Instance_Name_Invalid_Error;
	        		}
	        	}
	        }
     	}
    	this.setErrorMessage(errorMessage);
        return retVal;
    }
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@SuppressWarnings("rawtypes")
	public Control createContents(Composite parent) {

		String[] columnNames = {InstancePropertyEditor.COLUMN_NAME_KEY,
								InstancePropertyEditor.COLUMN_NAME_VALUE};

        Font font = parent.getFont();
		
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
        composite.setFont(font);

        instance = (MobileInstance) getElement().getAdapter(MobileInstance.class);

        Label label = new Label(composite, SWT.NULL);
        label.setText(InstancePropertyEditor.LABEL_INSTANCE_NAME);
        textInstanceName = new Text(composite, SWT.BORDER);
        initialInstanceName = instance.getName().trim();
        textInstanceName.setText(initialInstanceName);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        textInstanceName.setLayoutData(gridData);
        textInstanceName.addListener(SWT.Modify, new Listener() {
	            public void handleEvent(Event e) {
	                boolean valid = validateName(textInstanceName.getText());
	                setValid(valid);
	            }
        	});

        label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL );
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
        label.setLayoutData(gridData);
        
        label = new Label(composite, SWT.NULL);
        label.setText(InstancePropertyEditor.LABEL_PROPERTIES);
        
		this.table = new Table(composite, SWT.FULL_SELECTION);
	
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 2;
		gridData.grabExcessVerticalSpace = true;
		
		table.setLayoutData(gridData);

	    new TableColumn(table, SWT.CENTER).setText(columnNames[0]);
	    new TableColumn(table, SWT.CENTER).setText(columnNames[1]);
	    
		Properties properties = instance.getProperties();
		
		for (Enumeration e = properties.keys();
		 		e.hasMoreElements() ;) {
	        String propertyKey = (String) e.nextElement();
	        String propertyVal = properties.getProperty(propertyKey);

	        TableItem item = new TableItem (table, SWT.NONE);
	        item.setText(new String[] {propertyKey, propertyVal});
		}

		int columns = table.getColumnCount();
	    for (int i = 0; i < columns; i++) {
	        table.getColumn(i).pack();
	    }

	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);

	    table.addMouseListener(new MouseAdapter() {
	    	public void mouseDoubleClick(MouseEvent event) {
	    		onMouseDoubleClick();
	        }
		});
	    
	    noDefaultAndApplyButton();
		return composite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		
		TableItem items[] = table.getItems();
		for (int i = 0; i < items.length; i ++) {
			String key = items[i].getText(0);
			String value = items[i].getText(1).trim();
			instance.getProperties().setProperty(key, value);
		}
		instance.setName(textInstanceName.getText().trim());
        InstanceEventManager.getInstance().notifyListeners(new InstanceEvent(InstanceEventType.INSTANCE_UPDATED, instance));
		return true;
	}
}
