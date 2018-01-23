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
 * Julia Martinez Perdigueiro (Eldorado Research Institute) - [244856] - Instance View usability should be improved
 * Julia Martinez Perdigueiro (Eldorado Research Institute) - [247085] - Instance manage view buttons are resizing after applying services filter
 * Julia Martinez Perdigueiro (Eldorado Research Institute) - [247288] - Exceptions after Instance Mgt View is closed
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [248036] - New Icons for "New Instance" and "Filter services" on Device View
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [250644] - Instance view keeps enabled buttons while performing a service.
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [252261] - Internal class MobileInstance providing functionalities
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [259243] - image in the wizards
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [246082] - Complement bug #245111 by allowing disable of "Properties" option as well
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [271807] - Improper use of PreferencesUtil.createPropertyDialogOn() on properties editor
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [274502] - Change labels: Instance Management view and Services label
 * Pablo Cobucci Leite (Eldorado Research Institute) - Bug [274977] - Instance Management View does not ask user before removing a instance
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [277469] - Device management view blinks when user performs operations
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [280981] - Add suport for selecting instances programatically 
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [281425] - Instance Management View does not remove instance listerners properly. 
 * Mauren Brenner (Eldorado) - [281377] Support device types whose instances cannot be created by user
 * Fabio Rigo (Eldorado) - Bug [288006] - Unify features of InstanceManager and InstanceRegistry
 * Daniel Barboza Franco - Bug [287996] - Dont show device selection dialog when there is only one device
 * Eric Cloninger (Motorola) - [287883] Adjust the status column in device management view - Modified relative widths
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Marcel Gorri (Eldorado) - [303646] Add support for UI styles.
 * Pablo Leite (Eldorado) - [329548] Allow multiple instances selection on Device Manager View
 * Daniel Barboza Franco (Eldorado) - [329548] Allow multiple instances selection on Device Manager View
 * Julia Martinez Perdigueiro (Eldorado) - [329548] Adding double click behavior and tooltip support for double click behavior
 * Marcelo Marzola Bossoni (Instituto de Pesquisas Eldorado) - [352157] Added basic drag and drop support 
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.DeviceUtils;
import org.eclipse.sequoyah.device.framework.events.IInstanceListener;
import org.eclipse.sequoyah.device.framework.events.InstanceAdapter;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.manager.InstanceManager;
import org.eclipse.sequoyah.device.framework.manager.ServiceManager;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandlerAction;
import org.eclipse.sequoyah.device.framework.status.IStatus;
import org.eclipse.sequoyah.device.framework.status.StatusRegistry;
import org.eclipse.sequoyah.device.framework.ui.DeviceUIPlugin;
import org.eclipse.sequoyah.device.framework.ui.view.model.InstanceMgtViewComparator;
import org.eclipse.sequoyah.device.framework.ui.view.model.InstanceSelectionChangeListener;
import org.eclipse.sequoyah.device.framework.ui.view.model.ViewerAbstractNode;
import org.eclipse.sequoyah.device.framework.ui.view.model.ViewerDeviceNode;
import org.eclipse.sequoyah.device.framework.ui.view.model.ViewerInstanceNode;
import org.eclipse.sequoyah.device.framework.ui.view.provider.InstanceMgtViewContentProvider;
import org.eclipse.sequoyah.device.framework.ui.view.provider.InstanceMgtViewLabelProvider;
import org.eclipse.sequoyah.device.framework.ui.wizard.DeviceWizardExtensionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class InstanceStatusComposite extends Composite
{
	private static final String MENU_DELETE = Messages.InstanceStatusComposite_3; 
	private static final String MENU_PROPERTIES = Messages.InstanceStatusComposite_4; 
	private static final String MENU_NEW = Messages.InstanceStatusComposite_5;  
	private static final String TOOLBAR_NEW_TOOLTIP = Messages.InstanceStatusComposite_6;
	private static final String TOOLBAR_DIALOG_MESSAGE = Messages.InstanceStatusComposite_7;
	private static final String ERROR_DIALOG_TITLE = Messages.InstanceStatusComposite_8; 
	private static final String ERROR_NO_WIZARD_MESSAGE = Messages.InstanceStatusComposite_9; 
	private static final int DEFAULT_MENU_IMAGE_SIZE = 16;


	/**
	 * The main viewer of the instance view. 
	 * It is responsible to construct the tree with columns for correct instance visualization 
	 */
	private TreeViewer viewer;

	private IViewSite viewSite;

    /**
     * The wizard actions
     */
    protected Map<String, Action> wizardActions = new TreeMap<String, Action>();
    
    private IInstanceListener listener = new InstanceAdapter()
	{
	    public void instanceLoaded(InstanceEvent e)
	    {
	        InstanceStatusComposite.this.instanceLoaded(e.getInstance());
	    }

	    public void instanceUnloaded(InstanceEvent e)
	    {
	        InstanceStatusComposite.this.instanceUnloaded(e.getInstance());
	    }

	    public void instanceUpdated(InstanceEvent e) {
	    	InstanceStatusComposite.this.instanceTransitioned(e.getInstance());
	    }
	    
	    public void instanceTransitioned(InstanceEvent e)
	    {
	        InstanceStatusComposite.this.instanceTransitioned(e.getInstance());
	    }
	};
	private boolean useDropDown;

	public InstanceStatusComposite(Composite parent, IViewSite viewSite, boolean useDropDown)
	{
		super(parent, SWT.NONE);
		this.viewSite = viewSite;
		this.useDropDown = useDropDown;
		createContents();

		InstanceEventManager eventMgr = InstanceEventManager.getInstance();
		eventMgr.addInstanceListener(listener);
	}
	
	protected void addInstanceSelectionChangeListener(InstanceSelectionChangeListener listener)	{
		InstanceMgtView.addInstanceSelectionChangeListener(listener);
	}
	
	protected void removeInstanceSelectionChangeListener(InstanceSelectionChangeListener listener) {
		InstanceMgtView.removeInstanceSelectionChangeListener(listener);
	}
	
	private void notifyInstanceSelectionChangeListeners(IInstance instance)	{
		InstanceMgtView.notifyInstanceSelectionChangeListeners(instance);
	}

	protected IInstance getSelectedInstance()
	{
		IInstance instance = null;
		Object lastSelection = getLastSelection();
		
		if (lastSelection instanceof IInstance)
		{
		    instance = (IInstance) lastSelection;
		}
		
		return instance;
	}
	
	protected IDeviceType getSelectedDevice() {
		IDeviceType device = null;
		
		Object lastSelection = getLastSelection();
		
		if (lastSelection instanceof IDeviceType) {
			device = (IDeviceType) lastSelection;
		}
		
		return device;
	}

	private void createContents()
	{
		setLayout(new FillLayout());
		viewer = new TreeViewer(this, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL)
		{
			@Override
			protected void createTreeItem(Widget parent, Object element,
					int index) {
				super.createTreeItem(parent, element, index);
				if(useDropDown)
				{
					if(element instanceof ViewerInstanceNode)
					{
						ViewerInstanceNode instanceNode = (ViewerInstanceNode) element;
						if(instanceNode.getInstance() !=  null)
						{
							configureButtons(instanceNode);
						}
					}
				}
			}
		};
		viewer.addDoubleClickListener(new IDoubleClickListener()
        {
            
            public void doubleClick(DoubleClickEvent event)
            {
                ISelection selection = event.getSelection();
                if (selection instanceof TreeSelection) {
                    TreeSelection treeSelection = (TreeSelection) selection;
                    Object obj = treeSelection.getFirstElement();
                    if (obj instanceof ViewerInstanceNode) {
                        ViewerInstanceNode instanceNode = (ViewerInstanceNode) obj;                        
                        IStatus status = StatusRegistry.getInstance().getStatus(instanceNode.getInstanceStatus());
                        if (status != null && status.getDefaultServiceId() != null) {
                            // run service
                            String serviceId = status.getDefaultServiceId();
                            try {
                                List<IInstance> instances = new ArrayList<IInstance>(1);
                                instances.add(instanceNode.getInstance());
                                ServiceManager.runServices(instances, serviceId);
                            } catch (SequoyahException te){
                                BasePlugin.logError("Error running default service " + serviceId + " for instance " + instanceNode.getInstanceName(), te); //$NON-NLS-1$ //$NON-NLS-1$
                            }
                        }
                    }
                }
            }
        });
		
		Tree tree = viewer.getTree();
		TableLayout layout = new TableLayout();
		tree.setLayout(layout);
		tree.setHeaderVisible(true);

		createColumn(Messages.InstanceStatusComposite_10, 10);
		createColumn(Messages.InstanceStatusComposite_11, 9);

		InstanceMgtViewLabelProvider labelProvider = new InstanceMgtViewLabelProvider();

		ColumnViewerToolTipSupport.enableFor(viewer,ToolTip.NO_RECREATE);
		viewer.setLabelProvider(labelProvider);

		viewer.setContentProvider(new InstanceMgtViewContentProvider());
		viewer.getTree().setLinesVisible(true);
		// Use the custom comparator in emulator views
		viewer.setComparator(new InstanceMgtViewComparator(labelProvider));
		viewer.setInput(viewSite);
		viewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event) {
				notifyInstanceSelectionChangeListeners(getSelectedInstance());
				InstanceMgtView.updateServicesToolbar();
			}	
		});
		
		ArrayList<Transfer> types = new ArrayList<Transfer>();
		types.add(FileTransfer.getInstance());
		types.add(TextTransfer.getInstance());
		viewer.addDropSupport(DND.DROP_COPY | DND.DROP_DEFAULT | DND.DROP_MOVE
				| DND.DROP_LINK | DND.DROP_TARGET_MOVE,
				types.toArray(new Transfer[0]),
				new InstanceStatusCompositeDropAdapter());

        createActions();   
		fillMenuContext();
		fillViewMenu();
		fillViewToolbar();
		//refreshViewer(InstanceMgtView.getSelectedInstance());
		refreshViewer(null);
		viewer.expandAll();
	}

    private void createActions()
    {
        for (final IDeviceType device : DeviceTypeRegistry.getInstance().getDeviceTypes())
        {
        	if ((!device.isAbstract()) && (device.supportsUserInstances())) {
	        	wizardActions.put(device.getLabel(), new Action(device.getLabel())
	            {
	                @Override
	                public void run()
	                {
	                    IWizard wizard = DeviceWizardExtensionManager.getInstance().getDeviceWizard(device.getId());
	                    if (wizard != null)
	                    {
	                        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	                        
	                        // Instantiates the wizard container with the wizard and opens it
	                        WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
	                        dialog.create();
	                        dialog.open();
	                    }
	                    else
	                    {
	                        Display.getDefault().asyncExec(new Runnable()
	                        {
	                            public void run()
	                            {
	                                IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	                                MessageDialog.openError(ww.getShell(), ERROR_DIALOG_TITLE, ERROR_NO_WIZARD_MESSAGE + device.getLabel());
	                            }
	                        });
	                    }
	                }
	                
	                @Override
	                public String toString()
	                {
	                    return getText();
	                }
	            });
            }
        }
    }
    
	protected void clearContextMenu(Menu menu) {
		 // Get rid of existing menu items 
        MenuItem[] items = menu.getItems(); 
        for (int i = 0; i < items.length; i++) { 
            items[i].dispose(); 
        }
		
	}
    
    private void fillMenuContext()
    {
        final Menu menu = new Menu(viewer.getTree()); 
        viewer.getTree().setMenu(menu); 
        menu.addMenuListener(new MenuAdapter() { 
            public void menuShown(MenuEvent e) { 
            	clearContextMenu(menu);
                fillMenuContext(menu, false);              

            }
        }); 
    }
    
    private void fillMenuContext(Menu menu, boolean justActions)
    {
    	MenuItem newItem = null;

        ISelection selection = viewer.getSelection();
        if (selection instanceof IStructuredSelection)
        {
            List <ViewerInstanceNode> instanceNodes = new ArrayList<ViewerInstanceNode>();
            List <ViewerDeviceNode> deviceNodes = new ArrayList<ViewerDeviceNode>();
            
            boolean containInstance = false;
            for (Object item: ((IStructuredSelection) selection).toArray()) {
            	if (item instanceof ViewerInstanceNode) {
            		instanceNodes.add((ViewerInstanceNode)item);
            		containInstance = containInstance || ((ViewerInstanceNode) item).containsInstance();
            	}
            	else if (item instanceof ViewerDeviceNode) {
            		deviceNodes.add((ViewerDeviceNode)item);
            	}
            }
            
            
            if (containInstance)
            {
                fillMenuContextMultiple(menu, getSelection(), justActions);
            }
            else if ((deviceNodes.size() == 1 ))
            {
            	IDeviceType device = getSelectedDevice();
            	
            	if (device.supportsUserInstances()) {
            		// menu item "New..."
            		newItem = new MenuItem(menu, SWT.PUSH);
            		newItem.setText(MENU_NEW);
            		Object firstSelection = ((IStructuredSelection)selection).getFirstElement();
            		String deviceName = ((ViewerDeviceNode) firstSelection).getDeviceName();
            		newItem.addListener(SWT.Selection, new WizardSelectionListener(deviceName));
            	}
            }
        }  
    }
    
    private void insertInstanceOperations(Menu menu, List <Object> selection, boolean dropDown) {
    	
		MenuItem newItem = null;

		boolean canEditProperties = false;
		boolean atLeastOneSupportsUserInstances = false;
		boolean canDeleteInstances = true;
		IStatus status = null;
		IDeviceType device = null;
		
		Set <String> deviceTypesSelected = new HashSet<String>();
		List <IInstance> instances = new ArrayList<IInstance>();
		
		
		for (Object selectionItem: selection) {
			
			if (selectionItem instanceof IInstance) {
				
				IInstance instance = (IInstance)selectionItem; 
				instances.add(instance);
				String statusId = instance.getStatus();
	        	status = StatusRegistry.getInstance().getStatus(statusId);
	        	device = DeviceUtils.getDeviceType(instance);
	        	deviceTypesSelected.add(device.getId());
	        	atLeastOneSupportsUserInstances = atLeastOneSupportsUserInstances || device.supportsUserInstances();
	        	//supportUserInstances = supportUserInstances & device.supportsUserInstances();
	        	canDeleteInstances = canDeleteInstances & status.canDeleteInstance();
	        	canEditProperties = status.canEditProperties();
	        }
			else if (selectionItem instanceof IDeviceType) {
				
				IDeviceType deviceType = (IDeviceType) selectionItem;
				
	        	deviceTypesSelected.add(deviceType.getId());
	        	//atLeastOneSupportsUserInstances = atLeastOneSupportsUserInstances || deviceType.supportsUserInstances();
	        	//supportUserInstances = supportUserInstances & deviceType.supportsUserInstances();
	        	canDeleteInstances = false;
			}
		}
        
        if (atLeastOneSupportsUserInstances) {
            if(!dropDown)
            {
                // menu item "New..."
                newItem = new MenuItem(menu, SWT.PUSH);
                newItem.setText(MENU_NEW);
                String deviceName = device.getLabel();
                newItem.addListener(SWT.Selection, new WizardSelectionListener(deviceName));
                
                if (deviceTypesSelected.size() > 1){
                	newItem.setEnabled(false);
                }

                newItem = new MenuItem(menu, SWT.SEPARATOR);
            }

            // menu item "Delete"
            newItem = new MenuItem(menu, SWT.PUSH);
            newItem.setText(MENU_DELETE);
            
            newItem.addListener(SWT.Selection, new MenuDeleteListener(instances));
            newItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
            newItem.setEnabled(canDeleteInstances);

            newItem = new MenuItem(menu, SWT.SEPARATOR);

        }
        
        // menu item "Properties"
        if (instances.size() >= 1) {
	        newItem = new MenuItem(menu, SWT.PUSH);
	        newItem.setText(MENU_PROPERTIES);
	        newItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(DeviceUIPlugin.ICON_PROPERTY));

			canEditProperties = canEditProperties && (selection.size() == 1);

			if (canEditProperties) {
	        	newItem.addListener(SWT.Selection, new MenuPropertiesListener(instances.get(0)));
	        	newItem.setEnabled(true); // this call is safe because there is only one status since size==1
	        }
	        else {
	        	newItem.setEnabled(false);
	        }
        }

	}
	
	
	private void fillMenuContextMultiple(Menu menu, List <Object> selection, boolean dropDown) {
	
		List <IInstance> instances = new ArrayList<IInstance>();
		for (Object item: selection) {
			if (item instanceof IInstance) {
				instances.add((IInstance)item);
			}
		}
		
		insertInstanceOperations(menu, selection, dropDown);
		
		//If there's only instances selected, insert services.
		if (instances.size() == selection.size()) {
	        insertServices(menu, instances);			
		}


	}
	


	private void insertServices(Menu menu, List <IInstance> instances) {

		MenuItem newItem = null;
		
		List<IService> services = ServiceManager.getCommonServices(instances, (instances.size() > 1));

		if (services.size() > 0 ) {
			newItem = new MenuItem(menu, SWT.SEPARATOR);
		}
		
        for (IService service: services){
            if (service.isVisible()) {
            	
                newItem = new MenuItem(menu, SWT.PUSH);  
                ImageData serviceImageData = service.getImage().getImageData().scaledTo(DEFAULT_MENU_IMAGE_SIZE, DEFAULT_MENU_IMAGE_SIZE);
                Image serviceImage = new Image(getDisplay(), serviceImageData);
                newItem.setImage(serviceImage);
                newItem.setEnabled(true);
                newItem.setText(service.getName());
                
                newItem.addListener(SWT.Selection,  new ServiceHandlerAction(instances, service.getId()));

                // The listener below updates the services composite
                updateServicesComposite(newItem, instances);

                
            }
        }
	}

	protected List<IInstance> getSelectedInstances() {
		List<Object> selection = getSelection();
		List<IInstance> selectedInstances = new ArrayList<IInstance>(selection.size());
		for(Object selected : selection)
		{
			if(selected instanceof IInstance)
			{
				IInstance instance = (IInstance) selected;
				selectedInstances.add(instance);
			}
		}
		return selectedInstances;
	}

	private void updateServicesComposite(MenuItem newItem, final List<IInstance> instances) {
        newItem.addListener(SWT.Selection,  new Listener(){
			public void handleEvent(Event event) {
			    InstanceServicesComposite composite = InstanceMgtView.getInstanceServicesComposite();
				if (composite != null)
				    composite.setSelectedInstances(instances);
			}
		} );	
	}

	private void fillViewMenu()
	{
	    IMenuManager rootMenuManager = viewSite.getActionBars().getMenuManager();
	    IMenuManager wizardSubmenu = new MenuManager(MENU_NEW);
        rootMenuManager.add(wizardSubmenu);
        for (Action action : wizardActions.values())
        {
            wizardSubmenu.add(action);
        }
	}
	
	private void fillViewToolbar()
	{
		    IToolBarManager toolbarManager = viewSite.getActionBars().getToolBarManager();
		    WizardDropDownAction newWizardAction = new WizardDropDownAction();
		    newWizardAction.setToolTipText(TOOLBAR_NEW_TOOLTIP);
		    
			if (wizardActions.size() <= 0) { 
				newWizardAction.setEnabled(false);
			}
	        toolbarManager.add(newWizardAction);
	}
	
	private void createColumn(String columnLabel, int columnWeight)
	{
		Tree tree = viewer.getTree();
		TableLayout layout = (TableLayout) tree.getLayout();
		layout.addColumnData(new ColumnWeightData(columnWeight));
		TreeColumn tc = new TreeColumn(tree, SWT.NONE);
		tc.setMoveable(true);
		tc.setText(columnLabel);
		tc.setResizable(true);

		tc.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				columnSelected(e);
			}
		});
		tc.addControlListener(new ControlAdapter()
		{
			/**
			 * @see org.eclipse.swt.events.ControlListener#controlMoved(ControlEvent)
			 */
			 public void controlMoved(ControlEvent e)
			 {
				 columnMoved(e);
			 }
		});
	}

	protected void refreshViewer(IInstance selectedInstance)
	{
	    Collection<String> expandedDevices = getExpandedDevices();
		viewer.refresh();
		expandToNodeValues(expandedDevices, selectedInstance);		

        notifyInstanceSelectionChangeListeners(getSelectedInstance());
	}
	
	private void configureButtons(final ViewerInstanceNode instanceNode)
	{
		TreeItem[] items = viewer.getTree().getItems();

		TreeItem desiredItem = getInstanceItem(instanceNode, items);

		if(desiredItem != null)
		{
			if(!desiredItem.isDisposed())
			{
				final Tree tree = viewer.getTree();
				final TreeEditor editor = new TreeEditor(tree);

				final Button menuButton = new Button(tree, SWT.ARROW | SWT.DOWN);
				menuButton.setSize(15, 15);
				final Menu menu = new Menu(InstanceStatusComposite.this);
				menuButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						clearContextMenu(menu);
						List <Object> instances = new ArrayList<Object>();
						instances.add(instanceNode.getInstance());
						fillMenuContextMultiple(menu, instances, true);
						selectInstance(instanceNode.getInstance());
						Rectangle rect = menuButton.getBounds();
						Point pt = new Point(0, rect.height);
						pt = menuButton.toDisplay(pt);
						TreeItem item = tree.getItem(new Point(rect.x, rect.y));
						tree.setSelection(item);
						menu.setLocation(pt.x, pt.y);
						menu.setVisible(true);
					}
				});

				editor.horizontalAlignment = SWT.RIGHT;
				editor.minimumHeight = 15;
				editor.minimumWidth = 15;
				editor.setEditor(menuButton, desiredItem, 0);
				desiredItem.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						editor.dispose();
						menuButton.dispose();
					}
				});
			}
		}
	}
	
	private Collection<String> getExpandedDevices()
	{
	    Object[] expandedElements = viewer.getVisibleExpandedElements();

	    Collection<String> devicesCol = new HashSet<String>();

	    for (Object element : expandedElements)
	    {
	        if (element instanceof ViewerDeviceNode)
	        {
	            ViewerDeviceNode node = (ViewerDeviceNode) element;
	            devicesCol.add(node.getDevice().getId());
	        }
	    }
	    
	    return devicesCol;
	}
	
	private void expandToNodeValues(Collection<String> devices, IInstance instance)
	{
        for (TreeItem treeNode : viewer.getTree().getItems())
        {
            Object node = treeNode.getData();
            if (node instanceof ViewerDeviceNode)
            {
                ViewerDeviceNode deviceNode = (ViewerDeviceNode) node;
                if (devices.contains(deviceNode.getDevice().getId()))
                {
                    treeNode.setExpanded(true);
                    viewer.reveal(treeNode);
                    for (ViewerAbstractNode childNode : deviceNode.getChildren())
                    {
                        viewer.reveal(childNode);
                        
                        if ((instance != null) && (instance.equals(((ViewerInstanceNode)childNode).getInstance())))
                        {
                            treeNode.setExpanded(true);
                            viewer.reveal(treeNode);
                            viewer.setSelection(new StructuredSelection(childNode));
                        }
                    }
                }
            }
        }
	}
	
	private Object getLastSelection()
	{
	    Object lastSelection = null;
	    ISelection selection = viewer.getSelection();
        if (selection instanceof IStructuredSelection)
        {
            IStructuredSelection strSelection = (IStructuredSelection) selection;

            if (strSelection.size() == 1)
            {
                Object firstSelection = strSelection.getFirstElement();

                if (firstSelection instanceof ViewerInstanceNode)
                {
                    ViewerInstanceNode node = (ViewerInstanceNode) firstSelection;
                    lastSelection = node.getInstance();
                }
                else if (firstSelection instanceof ViewerDeviceNode)
                {
                    ViewerDeviceNode node = (ViewerDeviceNode) firstSelection;
                    lastSelection = node.getDevice();
                }
            }
        }
        
        return lastSelection;
	}
	
	protected List<Object> getSelection()
	{
	    ISelection selection = viewer.getSelection();
	    List<Object> selectedItems = null;
        if (selection instanceof IStructuredSelection)
        {
            IStructuredSelection strSelection = (IStructuredSelection) selection;

			selectedItems = new ArrayList<Object>(strSelection.size());
			@SuppressWarnings("rawtypes")
			Iterator it = strSelection.iterator();
			while(it.hasNext())
			{
				Object selected = it.next();
				if (selected instanceof ViewerInstanceNode)
				{
					ViewerInstanceNode node = (ViewerInstanceNode) selected;
					selected = node.getInstance();
				}
				else if (selected instanceof ViewerDeviceNode)
				{
					ViewerDeviceNode node = (ViewerDeviceNode) selected;
					selected = node.getDevice();
				}
				selectedItems.add(selected);
			}
        }
        
        return selectedItems != null? selectedItems : Collections.emptyList();
	}

	private void columnSelected(SelectionEvent e)
	{
		// When a column is selected by the user, it is used as basis for sorting
		TreeColumn selectedColumn = (TreeColumn) e.widget;
		Tree tree = selectedColumn.getParent();
		int columnIndex = tree.indexOf(selectedColumn);
		InstanceMgtViewComparator comparator = (InstanceMgtViewComparator) viewer.getComparator();
		comparator.setColumnToSort(columnIndex);
		
		if (tree.getSortColumn() == selectedColumn)
		{
			comparator.toggleAscending();
		}

		tree.setSortColumn(selectedColumn);
		if (comparator.isAscending())
		{
			tree.setSortDirection(SWT.UP);
		}
		else
		{
			tree.setSortDirection(SWT.DOWN);
		}
		refreshViewer(getSelectedInstance());
	}

	private void columnMoved(ControlEvent e)
	{
		TreeColumn treeColumn = (TreeColumn) e.getSource();
		Tree tree = treeColumn.getParent();
		int[] order = tree.getColumnOrder();
		InstanceMgtViewLabelProvider provider =
			(InstanceMgtViewLabelProvider) viewer.getLabelProvider();
		int previousFirstColumnIndex = provider.getFirstColumnIndex();

		if (order[0] != previousFirstColumnIndex)
		{
			// This procedure is made only if the movement causes a column to change its
			// position in comparison with other columns. Otherwise, a performance lack
			// is detected.
			provider.setFirstColumnIndex(order[0]);
			refreshViewer(getSelectedInstance());
		}
	}

	/** 
	 * Remove the selected instance.
	 */
	protected void removeSelected() {
		if (viewer.getSelection().isEmpty()) {
			return;
		}
		
		IInstance instance = getSelectedInstance();

		if (instance != null)
		{
		    InstanceManager.deleteInstance(instance);
		}
	}
	
	
	/** 
	 * Remove the selected instances.
	 */
	protected void removeSelectedInstances() {
		if (viewer.getSelection().isEmpty()) {
			return;
		}
		
		List<Object> instances = getSelection();

		if (instances != null)
		{
		    InstanceManager.deleteInstances(instances);
		}
	}
	
	private void instanceLoaded(final IInstance instance)
	{
	    Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                Collection<String> expandedDevices = getExpandedDevices();
                viewer.refresh();
                expandedDevices.add(instance.getDeviceTypeId());
                expandToNodeValues(expandedDevices, instance);
                notifyInstanceSelectionChangeListeners(instance);
            }});
	}
	
	private void instanceUnloaded(IInstance instance)
	{

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                Collection<String> expandedDevices = getExpandedDevices();
                viewer.refresh();
                expandToNodeValues(expandedDevices, null);
            }});
	}
	
	private void instanceTransitioned(final IInstance instance)
	{
	    Display.getDefault().asyncExec(new Runnable() {
            public void run() {
            	
            	InstanceMgtView.updateServicesToolbar();
            	
                ViewerInstanceNode node = getInstanceNode(instance);
                if (node != null)
                {
                    Collection<String> expandedDevices = getExpandedDevices();
                    IInstance selectedInstance = getSelectedInstance();
                    viewer.update(node, null);
                    expandToNodeValues(expandedDevices, selectedInstance);
                    
                    if (instance.equals(selectedInstance))
                    {
                        notifyInstanceSelectionChangeListeners(selectedInstance);
                    }
                }
            }});
	}
	
	private ViewerInstanceNode getInstanceNode(IInstance instance) {
	    ViewerInstanceNode node = null;	    
	    TreeItem[] deviceItems = viewer.getTree().getItems();
	    
	    for (TreeItem devItem : deviceItems) {	        
	        Object data = devItem.getData();
	        
	        if (data instanceof ViewerDeviceNode) {
	            Set<ViewerAbstractNode> children = ((ViewerDeviceNode)data).getChildren();
	            
	            for (ViewerAbstractNode child : children) {
	                if (child instanceof ViewerInstanceNode) {
	                    IInstance nodeInstance = ((ViewerInstanceNode)child).getInstance();
	                    if ((nodeInstance != null)&&(nodeInstance.equals(instance))) {
	                        node = (ViewerInstanceNode) child;
	                        break;
	                    }
	                }
	            }
	            
	            if (node != null) {
	                break;
	            }
	        }
	    }
	    
	    return node;
	}

    /*
     * Menu handler
     */
	private class MenuDeleteListener implements Listener {
		
		private List <IInstance> instances;
		
		public MenuDeleteListener(List <IInstance> instances)
        {
            this.instances = instances;
        }
		
		public void handleEvent(Event event) {
			final boolean[] result = new boolean[1];
			if(instances != null)
			{
				//Check with User if he really want to remove the instance.
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						String instanceNames = "";
						for (IInstance instance: instances) {
							instanceNames += (instanceNames.equals("")) ? "" : ", ";
							instanceNames += instance.getName();
						}				
						IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						result[0] = MessageDialog.openQuestion(ww.getShell(), Messages.InstanceStatusComposite_0, Messages.InstanceStatusComposite_1 + instanceNames + Messages.InstanceStatusComposite_2);
					}
				});
				
				if(result[0])
				{
					removeSelectedInstances();
				}
			}
		}
	}

    /*
     * Menu handler
     */
    private class MenuPropertiesListener implements Listener {
        private IInstance instance;
        
        public MenuPropertiesListener(IInstance instance)
        {
            this.instance = instance;
        }
        
        public void handleEvent(Event event) {

            Shell shell = new Shell();
            PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(
                    shell,
                    instance,
                    null,
                    null,
                    null);
            dialog.open();
        }
    }
    
    /*
     * Toolbar handler.
     * 
     * Represents a toolbar item with a drop down menu with the devices listed
     * for opening their respective wizard. The toolbar item itself when clicked
     * opens a list selection dialog presenting the devices for opening the
     * wizard.
     */
	private class WizardDropDownAction extends Action implements IMenuCreator
    {
        private Menu fMenu;

        public WizardDropDownAction()
        {
            ImageDescriptor descriptor= AbstractUIPlugin.imageDescriptorFromPlugin(DeviceUIPlugin.PLUGIN_ID, "icons/full/obj16/new_instance.gif"); //$NON-NLS-1$
            setHoverImageDescriptor(descriptor);
            setImageDescriptor(descriptor); 
            
            setMenuCreator(this);
        }
        
        public void dispose()
        {
            if (fMenu != null) {
                fMenu.dispose();
                fMenu = null;
            }
        }

        public Menu getMenu(Control parent)
        {
            if (fMenu != null) {
                fMenu.dispose();
            }
            fMenu= new Menu(parent);
            
            for (Action action : wizardActions.values())
            {
                ActionContributionItem item = new ActionContributionItem(action);
                item.fill(fMenu, -1);
            }
            
            return fMenu;
        }

        public Menu getMenu(Menu parent)
        {
            return null;
        }
        
        @Override
        public void run()
        {
            // this is run when user clicks the toolbar item itself and
            // not the drop down menu on the toolbar item
        	if (wizardActions.size() > 1) {
        	
	           ListDialog dialog = new ListDialog(viewSite.getShell());
	           dialog.setContentProvider(new ArrayContentProvider());
	           dialog.setLabelProvider(new LabelProvider());
	           dialog.setTitle(TOOLBAR_NEW_TOOLTIP);
	           dialog.setMessage(TOOLBAR_DIALOG_MESSAGE);
	           Action[] input = new Action[wizardActions.size()]; 
	           input = wizardActions.values().toArray(input);
	           dialog.setInput(input);
           
	           if (dialog.open() == Window.OK)
	           {
	               ((Action)dialog.getResult()[0]).run();
	           }
        	}
        	else if (wizardActions.size() == 1){
        		((Action)wizardActions.values().toArray()[0]).run();
        	}
        }
        
    }
	
	/*
	 * Menu Context handler
	 */
	private class WizardSelectionListener implements Listener
	{
	    private String deviceName;
	    
	    public WizardSelectionListener(String deviceName)
	    {
	        this.deviceName = deviceName;	        
	    }
	    
	    public void handleEvent(Event event)
	    {
            Action wizardAction = wizardActions.get(deviceName);
            if (wizardAction != null)
            {
                wizardAction.run();
            }
            else
            {
                Display.getDefault().asyncExec(new Runnable()
                {
                    public void run()
                    {
                        IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        MessageDialog.openError(ww.getShell(), ERROR_DIALOG_TITLE, ERROR_NO_WIZARD_MESSAGE + deviceName);
                    }
                });
            }
	    }
	}
	
	
	public void selectInstance(IInstance instance) {
		
		ViewerInstanceNode node = getInstanceNode(instance);
		viewer.setSelection(new StructuredSelection(node));
		
	}

	protected void removeListener() {
		InstanceEventManager eventMgr = InstanceEventManager.getInstance();
		eventMgr.removeInstanceListener(listener);
	}

	TreeItem getInstanceItem(final ViewerInstanceNode instance, TreeItem[] items) {
		TreeItem desiredItem = null;
		for (TreeItem treeNode : items)
		{
		    Object node = treeNode.getData();
		    if (node instanceof ViewerInstanceNode)
		    {
		    	ViewerInstanceNode deviceNode = (ViewerInstanceNode) node;
		    	if(instance.getInstanceName().equals(deviceNode.getInstanceName()))
		    	{
		    		return treeNode;
		    	}
		    }
	    	desiredItem = getInstanceItem(instance, treeNode.getItems());
	    	if(desiredItem != null)
	    	{
	    		break;
	    	}
		}
		return desiredItem;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	List getInstanceItems(TreeItem[] items) {
		List instanceItems = new ArrayList();
		for (TreeItem treeNode : items)
		{
		    Object node = treeNode.getData();
		    if (node instanceof ViewerInstanceNode)
		    {
		    	instanceItems.add(treeNode);
		    }
		    instanceItems.addAll(getInstanceItems(treeNode.getItems()));
		}
		return instanceItems;
	}
}
