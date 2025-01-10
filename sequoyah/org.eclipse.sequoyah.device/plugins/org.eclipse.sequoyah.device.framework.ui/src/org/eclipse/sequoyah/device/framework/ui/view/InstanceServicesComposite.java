/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Mobility, Inc. and Other. All rights reserved
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Julia Martinez Perdigueiro (Eldorado Research Institute) 
 * [244805] - Improvements on Instance view  
 *
 * Contributors:
 * Julia Martinez Perdigueiro (Eldorado Research Institute) - [247085] - Instance manage view buttons are resizing after applying services filter  
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [248036] - New Icons for "New Instance" and "Filter services" on Device View
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [250644] - Instance view keeps enabled buttons while performing a service.
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [252261] - Internal class MobileInstance providing functionalities
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [247179] - Choice of service buttons orientation on Instance Mgt View should be persisted
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [272544] - Default values for filter and orientation choices not being set.
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [274502] - Change labels: Instance Management view and Services label
 * Mauren Brenner (Eldorado) - Bug [282271] - Applying solution contributed by Vinicius Hernandes (Eldorado)
 * Mauren Brenner (Eldorado) - Bug [289577] - Add condition to handle case where button image is not defined
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Pablo Leite (Eldorado) - [329548] Allow multiple instances selection on Device Manager View
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.sequoyah.device.framework.manager.ServiceManager;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandlerAction;
import org.eclipse.sequoyah.device.framework.ui.DeviceUIPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;

public class InstanceServicesComposite extends Composite {

	private static boolean showAllServices = true;
	private static int buttonsOrienation = SWT.HORIZONTAL;
	private List<IInstance> instances = null;
	
	private static final String SERVICES_LABEL = Messages.InstanceServicesComposite_1; 
	private static final String SERVICES_FILTERED_LABEL = Messages.InstanceServicesComposite_2; 
	private static final String NO_LABEL = ""; //$NON-NLS-1$
	private static final int DEFAULT_BUTTONS_WIDTH = 120;
	private static final int DEFAULT_BUTTONS_HEIGHT = 30;
	private static final int MAX_BUTTON_STRING_SIZE = DEFAULT_BUTTONS_WIDTH / 10;
	private static final int DEFAULT_BUTTON_IMAGE_SIZE = 16;
	public static final String AVAILABILITY_TOOL_TIP_TEXT = Messages.InstanceServicesComposite_0;
	
	private CLabel label;
	private ToolBar toolBar;

	private ViewForm viewForm;
	
	private class ServicesFilterAction extends Action
	{
		public ServicesFilterAction()
		{
			super(Messages.InstanceServicesComposite_3); 
			
			PlatformUI.getPreferenceStore().setDefault(DeviceUIPlugin.SERVICE_BUTTONS_ORIENTATION_PREFERENCE , SWT.HORIZONTAL);
	    	PlatformUI.getPreferenceStore().setDefault(DeviceUIPlugin.FILTER_SERVICE_BY_AVAILABILITY_PREFERENCE , false);

			showAllServices = !PlatformUI.getPreferenceStore().getBoolean(DeviceUIPlugin.FILTER_SERVICE_BY_AVAILABILITY_PREFERENCE);
			
			setToolTipText(AVAILABILITY_TOOL_TIP_TEXT); 
			setChecked(!showAllServices);
			setImageDescriptor(DeviceUIPlugin.getDefault().getImageDescriptor(DeviceUIPlugin.ICON_FILTER));
		}
		
		public void run() {
			showAllServices = !showAllServices;
			setChecked(!showAllServices);
			
			PlatformUI.getPreferenceStore().setValue(DeviceUIPlugin.FILTER_SERVICE_BY_AVAILABILITY_PREFERENCE, !showAllServices);
		
			createServicesArea();
		}		
	}
	
	private Listener createServicesAreaListener =  new Listener(){
        
        public void handleEvent(Event event) {
            createServicesArea();
        }
        
    };
	
	private class ServicesOrientationAction extends Action
	{
	    public ServicesOrientationAction()
	    {
	        super(Messages.InstanceServicesComposite_4); 
	        
	        buttonsOrienation = PlatformUI.getPreferenceStore().getInt(DeviceUIPlugin.SERVICE_BUTTONS_ORIENTATION_PREFERENCE);
	        
	        setToolTipText(Messages.InstanceServicesComposite_5); 
	        if (buttonsOrienation ==  SWT.HORIZONTAL)
            {
                setImageDescriptor(DeviceUIPlugin.getDefault().getImageDescriptor(DeviceUIPlugin.ICON_HORIZONTAL));
            }
            else
            {
                setImageDescriptor(DeviceUIPlugin.getDefault().getImageDescriptor(DeviceUIPlugin.ICON_VERTICAL));
            }
	    }
	    
	    public void run()
	    {
	        if (buttonsOrienation ==  SWT.HORIZONTAL)
	        {
	            buttonsOrienation = SWT.VERTICAL;
	            setImageDescriptor(DeviceUIPlugin.getDefault().getImageDescriptor(DeviceUIPlugin.ICON_VERTICAL));
	        }
	        else
	        {
	            buttonsOrienation = SWT.HORIZONTAL;
	            setImageDescriptor(DeviceUIPlugin.getDefault().getImageDescriptor(DeviceUIPlugin.ICON_HORIZONTAL));
	        }

	        PlatformUI.getPreferenceStore().setValue(DeviceUIPlugin.SERVICE_BUTTONS_ORIENTATION_PREFERENCE, buttonsOrienation);
	        createServicesArea();
	    }
	}

	public InstanceServicesComposite(Composite parent) {
		super(parent, SWT.NONE);

		createContents();
		addDisposeListener(new DisposeListener(){

            public void widgetDisposed(DisposeEvent e)
            {
                label.dispose();
                label = null;
                toolBar.dispose();
                toolBar = null;
            }
		});
	}

	public void setSelectedInstances(List<IInstance> instances)
	{
		this.instances = instances;
		
		createServicesArea();
	}	

	private void createContents()
	{
		setLayout(new FillLayout());
		viewForm = new ViewForm(this, SWT.NONE);

		viewForm.setLayout(new GridLayout());

		// populate top part of area
		createToolbarArea();
		
		// populate the services area
		createServicesArea();        
	}
	
	private void createToolbarArea()
	{
		label= new CLabel(viewForm, SWT.NONE);
		label.setText(Messages.InstanceServicesComposite_6); 
		viewForm.setTopLeft(label);
		toolBar= new ToolBar(viewForm, SWT.FLAT | SWT.WRAP);
		viewForm.setTopCenter(toolBar);
		ToolBarManager toolBarMgr = new ToolBarManager(toolBar);
		toolBarMgr.add(new ServicesFilterAction());
		toolBarMgr.add(new Separator());
		toolBarMgr.add(new ServicesOrientationAction());
		toolBarMgr.update(true);
	}

	private void createServicesArea()
	{
	    Control oldContent = viewForm.getContent();
	    viewForm.setContent(null);
	    if (oldContent != null){
	        oldContent.dispose();
	        oldContent = null;
	    }
	    
		ScrolledComposite scrollComposite = new ScrolledComposite(viewForm, SWT.V_SCROLL | SWT.H_SCROLL);
		
		Composite servicesComposite = new Composite(scrollComposite,SWT.NONE);
		
		servicesComposite.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		RowLayout rowLayout = new RowLayout();
		rowLayout.wrap = true;
		rowLayout.pack = false;
		rowLayout.justify = false;
		rowLayout.type = buttonsOrienation;
		rowLayout.marginLeft = 5;
		rowLayout.marginTop = 5;
		rowLayout.marginRight = 5;
		rowLayout.marginBottom = 5;
		rowLayout.spacing = 5;
		
		servicesComposite.setLayout(rowLayout);

		if (instances != null && !instances.isEmpty())
		{	
			List<IService> commonServices = ServiceManager.getCommonServices(instances, (instances.size() > 1));
			List<IService> allServices = ServiceManager.getAllServices(instances, false);
			
			
			final ArrayList<Button> buttons = new ArrayList<Button>();
			for (IService service:allServices){
				if (service.isVisible()) {

					if ((showAllServices) || (commonServices.contains(service)))
					{
						Button serviceButton = new Button(servicesComposite, SWT.PUSH);
						buttons.add(serviceButton);
						serviceButton.setEnabled(commonServices.contains(service));
						serviceButton.addListener(SWT.Selection,  new ServiceHandlerAction(instances, service.getId()));
						
						// Set button enabled to false when performing an operation
						serviceButton.addListener(SWT.Selection, createServicesAreaListener);
						
						serviceButton.addDisposeListener(new DisposeListener(){

                            public void widgetDisposed(DisposeEvent e)
                            {
                               Listener[] selectionListeners = e.widget.getListeners(SWT.Selection);
                               for(Listener selectionListener : selectionListeners){
                                   e.widget.removeListener(SWT.Selection, selectionListener);
                               }
                            }
                           
						});
						
						RowData data = new RowData();
						data.width = DEFAULT_BUTTONS_WIDTH;
						data.height = DEFAULT_BUTTONS_HEIGHT;
						serviceButton.setLayoutData(data);
						
						// set button text and tooltip
						String serviceName = service.getName();
						serviceButton.setToolTipText(serviceName);
						if (serviceName.length() > MAX_BUTTON_STRING_SIZE)
						{
							// if text will not fit, shorten it
							serviceName = serviceName.substring(0, MAX_BUTTON_STRING_SIZE).concat(Dialog.ELLIPSIS);
						}
						serviceButton.setText(serviceName);
						
						// set the button image to 16x16 image
						if (service.getImage() != null) {
							ImageData serviceImageData = service.getImage().getImageData().scaledTo(DEFAULT_BUTTON_IMAGE_SIZE, DEFAULT_BUTTON_IMAGE_SIZE);
							Image serviceImage = new Image(serviceButton.getDisplay(), serviceImageData);
							serviceButton.setImage(serviceImage);
						}
					}
				}
			}
		}
		
		scrollComposite.setContent(servicesComposite);
		scrollComposite.setExpandVertical(true);
	    scrollComposite.setExpandHorizontal(true);
	    Point compositeSize = servicesComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	    scrollComposite.setMinSize(DEFAULT_BUTTONS_WIDTH + 10, compositeSize.y);
		viewForm.setContent(scrollComposite);
		
		if (instances == null)
		{
			label.setText(NO_LABEL);
			toolBar.setVisible(false);
		}
		else
		{
			if (showAllServices)
			{
				label.setText(SERVICES_LABEL);
			}
			else
			{
				label.setText(SERVICES_FILTERED_LABEL);
			}

			toolBar.setVisible(true);
		}
	}

}
