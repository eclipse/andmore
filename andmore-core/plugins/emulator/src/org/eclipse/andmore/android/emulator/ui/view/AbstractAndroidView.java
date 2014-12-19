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
package org.eclipse.andmore.android.emulator.ui.view;

import static org.eclipse.andmore.android.common.log.StudioLogger.debug;
import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.andmore.android.common.preferences.DialogWithToggleUtils;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.PluginUtils;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.devfrm.DeviceFrameworkManager;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStopException;
import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.andmore.android.emulator.core.exception.StartCancelledException;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.model.IEmulatorView;
import org.eclipse.andmore.android.emulator.core.skin.IAndroidSkin;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.logic.AbstractStartAndroidEmulatorLogic;
import org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance;
import org.eclipse.andmore.android.emulator.logic.StartVncServerLogic;
import org.eclipse.andmore.android.emulator.logic.AbstractStartAndroidEmulatorLogic.LogicMode;
import org.eclipse.andmore.android.emulator.logic.stop.AndroidEmulatorStopper;
import org.eclipse.andmore.android.emulator.ui.IUIHelpConstants;
import org.eclipse.andmore.android.emulator.ui.controls.IAndroidComposite;
import org.eclipse.andmore.android.emulator.ui.controls.RemoteCLIDisplay;
import org.eclipse.andmore.android.emulator.ui.controls.nativewindow.NativeWindowComposite;
import org.eclipse.andmore.android.nativeos.IDevicePropertiesOSConstants;
import org.eclipse.andmore.android.nativeos.NativeUIUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.vnc.protocol.PluginProtocolActionDelegate;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolMessage;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.IRemoteDisplay;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.SWTRemoteDisplay;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * DESCRIPTION: This class represents the Android Emulator view. It provides the
 * generic methods of the Emulator Views. The specific ones must be defined by
 * the classes that extend it.
 *
 * RESPONSIBILITY: - Show the viewers to the end user
 *
 * COLABORATORS: None.
 *
 * USAGE: The public interface provides static and dynamic methods: STATIC
 * METHODS: - Call getActiveInstance to retrieve the instance that corresponds
 * to the emulator running at the active tab - Call updateActiveViewers to
 * guarantee that the active viewer of all views is up to date in all emulator
 * views opened, but do not make further verifications. DYNAMIC METHODS: - Call
 * refreshView to updates all viewers including creation of viewers for started
 * instances and removal of viewers - Call updateActiveViewer to guarantee that
 * the active viewer is up to date in all emulator views opened, but do not make
 * further verifications
 */
public abstract class AbstractAndroidView extends ViewPart implements IEmulatorView {
	private final MenuManager menuManager;

	public static final String POPUP_MENU_ID = "org.eclipse.andmore.android.emulator.view.popup";

	private MouseListener mouseClickListener;

	/**
	 * Preference key of the Question Dialog about stopping the emulators by
	 * closing view
	 */
	private static String STOP_BY_CLOSING_VIEW_KEY_PREFERENCE = "stop.by.closing.view";

	/**
	 * Preference key of the Question Dialog about displaying all emulators in
	 * the IDE
	 * 
	 */
	private static String SHOW_EMULATOR_IN_THE_IDE_KEY_PREFERENCE = "show.view.for.started.emulators";

	/**
	 * Preference key of the Question Dialog about stopping all emulators in
	 * shutdown
	 * 
	 */
	private static String STOP_ALL_EMULATORS_IN_SHUTDOWN_KEY_PREFERENCE = "stop.all.emulators.in.shutdown";

	/**
	 * All event types handled by the listeners in this class
	 */
	public static final int[] SWT_EVENT_TYPES = new int[] { SWT.KeyDown, SWT.KeyUp, SWT.MouseDown, SWT.MouseUp,
			SWT.MouseMove, SWT.MouseDoubleClick };

	/**
	 * All possible Layout Operations
	 */
	public enum LayoutOpp {
		KEEP, NEXT
	};

	/**
	 * Tab folder where to place each instance tab
	 */
	private TabFolder tabFolder;

	Listener listener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (tabFolder.getItemCount() > 0) {
				TabItem activeTabItem = getActiveTabItem();

				if ((activeTabItem != null) && (activeTabItem.getControl() != null)) {
					info("Setting focus to Android Emulator " + activeTabItem.getData());
					activeTabItem.getControl().setFocus();
				}
			}
		}
	};

	/**
	 * Map to collect the emulator AndroidViewData committed to its emulator
	 * instance.
	 */
	private final Map<IAndroidEmulatorInstance, AndroidViewData> instanceDataMap = new LinkedHashMap<IAndroidEmulatorInstance, AndroidViewData>();

	/**
	 * Listener required to code the work-around for Sticky Views on
	 * perspectiveChanged method.
	 */
	private PerspectiveListenerImpl perspectiveListenerImpl;

	/**
	 * Listener necessary to determine when the view is closed.
	 */
	private PartListenerImpl partListenerImpl;

	/**
	 * Listener used to know if the view is being closed due to workbench
	 * shutdown.
	 */
	private static WorkbenchListenerImpl workbenchListenerImpl;

	// the view is being closed during the Studio shutdown.
	private boolean closingOnShutdown = false;

	// Collection of the ids of the opened views that overwrite this class
	private static final List<String> childrenIDs = new ArrayList<String>();

	// the instance being currently active at the emulator views
	private static IAndroidEmulatorInstance activeInstance;

	/**
	 * Listeners of tab switch event
	 */
	private static final Collection<Listener> tabSwitchListeners = new ArrayList<Listener>();

	/**
	 * Lock to assure that only the first thread will display the show view
	 * question
	 */
	private static Lock showViewLock = new ReentrantReadWriteLock().writeLock();

	/**
	 * Add a listener to be called when the tab selection changes
	 * 
	 * @param listener
	 *            the listener to be added
	 */
	public static void addTabSwitchListener(Listener listener) {
		tabSwitchListeners.add(listener);
	}

	/**
	 * Remove a listener that listen to tab switch events
	 * 
	 * @param listener
	 *            the listener to be removed
	 */
	public static void removeTabSwitchListener(Listener listener) {
		tabSwitchListeners.remove(listener);
	}

	/**
	 * Call listeners of tab switch events
	 */
	protected void handleTabSwitchEvent() {
		for (Listener listener : tabSwitchListeners) {
			listener.handleEvent(null);
		}
	}

	/**
	 * Returns the View Identification.
	 * 
	 * @return the unique ViewId
	 */
	protected abstract String getViewId();

	/**
	 * Creates the graphical elements representing the emulator that will be
	 * shown by the viewer in its tab item.
	 * 
	 * @param tab
	 *            the tab item that will hold the graphical elements that
	 *            represents the emulator
	 * @param instance
	 *            the emulator instance
	 * @param emulatorData
	 *            the object to be defined with the elements created.
	 * @throws SkinException
	 *             if the AVD configured skin does not exists
	 */
	protected abstract void createWidgets(TabItem tab, final IAndroidEmulatorInstance instance,
			final AndroidViewData emulatorData) throws SkinException;

	/**
	 * Forces the refreshing of the menu elements.
	 */
	protected abstract void refreshMenuElements();

	/**
	 * Retrieves the instance being currently active at the emulator views.
	 *
	 * @return The active instance, or null if there is no active instance
	 */
	public static IAndroidEmulatorInstance getActiveInstance() {
		return activeInstance;
	}

	/**
	 * Retrieves the instance being currently active at the emulator views.
	 *
	 * @return The active instance, or null if there is no active instance
	 */
	public static void setInstance(IAndroidEmulatorInstance emulatorInstance) {
		if (!childrenIDs.isEmpty()) {
			AbstractAndroidView view = (AbstractAndroidView) EclipseUtils.getActiveView(childrenIDs.get(0));
			if (view != null) {
				view.setActiveInstanceId(emulatorInstance.getInstanceIdentifier());
				activeInstance = emulatorInstance;
			}
		}
	}

	/**
	 * Retrieves the information about the viewer used to display the given
	 * emulator instance and returns null if there is no AndroidViewData for the
	 * given emulator instance. viewer.
	 * 
	 * @param the
	 *            emulator instance whose Android Viewer data need to be
	 *            retrieved.
	 * @return the AndroidViewerData for the given Emulator instance (null if
	 *         none is available).
	 */
	public AndroidViewData getViewData(IAndroidEmulatorInstance instance) {
		AndroidViewData viewData = instanceDataMap.get(instance);
		return viewData;
	}

	public IAndroidSkin getSkin(IAndroidEmulatorInstance instance) {
		IAndroidSkin skin = null;
		AndroidViewData viewData = getViewData(instance);
		if (viewData != null) {
			skin = viewData.getSkin();
		}
		return skin;
	}

	/**
	 * Gets the layout to set, if opp is NEXT or PREVIOUS
	 * 
	 * @param viewId
	 *            The view that is currently active
	 * @param opp
	 *            The layout operation to perform
	 */
	public static String getPreviousOrNextLayout(String viewId, LayoutOpp opp) {
		String prevNextLayout = null;
		AbstractAndroidView view = (AbstractAndroidView) EclipseUtils.getActiveView(viewId);
		if (view != null) {
			prevNextLayout = view.getPreviousOrNextLayout(opp);
		}

		return prevNextLayout;
	}

	/**
	 * Gets the layout to set, if opp is NEXT or PREVIOUS
	 * 
	 * @param opp
	 *            The layout operation to perform
	 */
	@SuppressWarnings("incomplete-switch")
	private String getPreviousOrNextLayout(LayoutOpp opp) {
		String prevNextLayout = null;
		if (activeInstance != null) {
			String referenceLayout = activeInstance.getCurrentLayout();
			AndroidViewData viewData = instanceDataMap.get(activeInstance);
			if (viewData != null) {
				IAndroidComposite androidComposite = viewData.getComposite();
				if ((androidComposite != null)) {
					IAndroidSkin androidSkin = viewData.getSkin();

					if (androidSkin != null) {
						switch (opp) {
						case NEXT:
							prevNextLayout = androidSkin.getNextLayout(referenceLayout);
							break;
						}
					}
				}
			}
		}

		return prevNextLayout;
	}

	/**
	 * Updates the zoom action that needs to be checked in all emulator views.
	 * This method must be called every time the focus changes to another
	 * viewer.
	 * 
	 * @param layoutName
	 *            The layout name to set
	 */
	public static void changeLayout(String layoutName) {
		for (String viewId : childrenIDs) {
			AbstractAndroidView view = (AbstractAndroidView) EclipseUtils.getActiveView(viewId);
			if (view != null) {
				view.updateActiveViewer(layoutName);
			}
		}
	}

	/**
	 * Gets the help ID to be used for attaching context sensitive help.
	 * 
	 * Classes that extends this class and want to set their on help should
	 * override this method
	 */
	protected String getHelpId() {
		return IUIHelpConstants.EMULATOR_VIEW_HELP;
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, getHelpId());

		this.tabFolder = new TabFolder(parent, SWT.BORDER | SWT.BACKGROUND);
		IViewSite viewSite = getViewSite();

		// Add listeners
		tabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setActiveInstanceId();
				updateMenuAndToolbars();
				handleTabSwitchEvent();
			}
		});

		tabFolder.addFocusListener(new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				handleTabSwitchEvent();
			}

		});

		perspectiveListenerImpl = new PerspectiveListenerImpl();
		viewSite.getWorkbenchWindow().addPerspectiveListener(perspectiveListenerImpl);

		partListenerImpl = new PartListenerImpl();
		viewSite.getPage().addPartListener(partListenerImpl);

		if (workbenchListenerImpl == null) {
			workbenchListenerImpl = new WorkbenchListenerImpl();
			viewSite.getWorkbenchWindow().getWorkbench().addWorkbenchListener(workbenchListenerImpl);
		}

		// Update UI
		refreshView();

		IActionBars actionBars = viewSite.getActionBars();
		if (actionBars != null) {
			IMenuManager menuManager = actionBars.getMenuManager();
			if (menuManager != null) {
				menuManager.addMenuListener(new IMenuListener() {
					@Override
					public void menuAboutToShow(IMenuManager manager) {
						// Calls the manager update method to guarantee that the
						// command have its handler
						// initialized. Otherwise, the next command will not
						// work properly
						if (manager != null) {
							manager.update(true);
						}
						updateMenuAndToolbars();
					}
				});
			}
		}

		// register the popup menu
		viewSite.registerContextMenu(POPUP_MENU_ID, menuManager, null);

		// create listener
		if (Platform.getOS().contains(Platform.OS_MACOSX)) {
			mouseClickListener = new MouseListener() {

				@Override
				public void mouseDoubleClick(MouseEvent e) {
					// do nothing
				}

				@Override
				public void mouseDown(MouseEvent e) {
					if ((e.button == 1) && (e.stateMask == SWT.CONTROL)) {
						menuManager.getMenu().setVisible(true);
					}
				}

				@Override
				public void mouseUp(MouseEvent e) {
					// do nothing
				}

			};
		} else {
			mouseClickListener = new MouseListener() {

				@Override
				public void mouseDoubleClick(MouseEvent e) {
					// do nothing
				}

				@Override
				public void mouseDown(MouseEvent e) {
					if (e.button == 3) {
						menuManager.getMenu().setVisible(true);
					}
				}

				@Override
				public void mouseUp(MouseEvent e) {
					// do nothing
				}

			};

		}

	}

	/**
	 * Constructor default
	 */
	public AbstractAndroidView() {
		childrenIDs.add(getViewId());
		menuManager = new MenuManager("", POPUP_MENU_ID);
		addTabSwitchListener(listener);
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		if (tabFolder.getItemCount() > 0) {
			TabItem activeTabItem = getActiveTabItem();

			if ((activeTabItem != null) && (activeTabItem.getControl() != null)) {
				info("Setting focus to Android Emulator " + activeTabItem.getData());
				activeTabItem.getControl().setFocus();
			} else {
				info("Setting focus to Android Emulator View");
				tabFolder.setFocus();
			}
		} else {
			info("Setting focus to Android Emulator View");
			tabFolder.setFocus();
		}

		updateMenuAndToolbars();
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		removeTabSwitchListener(listener);
		debug("Disposing View: " + getClass());
		getViewSite().getWorkbenchWindow().removePerspectiveListener(perspectiveListenerImpl);
		getViewSite().getPage().removePartListener(partListenerImpl);
		perspectiveListenerImpl = null;
		partListenerImpl = null;
		instanceDataMap.clear();
		tabFolder.dispose();
		childrenIDs.remove(getViewId());
		super.dispose();
	}

	/**
	 * This method rebuilds the skin, adding a new tab in the Android Emulator
	 * View to show it.
	 *
	 * It should be used when the Android Emulator view is being created when
	 * the Android Emulator instance is not stopped.
	 */
	@Override
	public void refreshView() {
		Job refreshViews = new Job("Refresh Emulator View") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				info("Updating Android Emulator viewers");

				final DeviceFrameworkManager framework = DeviceFrameworkManager.getInstance();

				Collection<IAndroidEmulatorInstance> startedInstances = framework.getAllStartedInstances();

				for (final IAndroidEmulatorInstance instance : startedInstances) {
					if (instance.getProperties()
							.getProperty(IDevicePropertiesOSConstants.useVnc, NativeUIUtils.getDefaultUseVnc())
							.equals("true")) {
						if (!instance.isConnected()) {
							IStatus returnStatus = null;
							returnStatus = connectVNC(instance, monitor);
							if (returnStatus.isOK()) {
								PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
									@Override
									public void run() {
										createViewer(instance);
									}
								});
							}
						}
					}
				}

				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {

						Collection<IAndroidEmulatorInstance> connectedInstances = framework.getAllConnectedInstances();

						Collection<IAndroidEmulatorInstance> instancesWithViewerCollection = getInstancesWithViewer();

						for (IAndroidEmulatorInstance instance : connectedInstances) {
							if (!instancesWithViewerCollection.contains(instance)) {
								createViewer(instance);
							} else {
								// update the collection for removing the
								// stopped instances
								instancesWithViewerCollection.remove(instance);
							}
						}

						// Remove not started instances from viewer
						for (IAndroidEmulatorInstance instance : instancesWithViewerCollection) {
							disposeViewer(instance);
							info("Disposed viewer of " + instance);
						}

						// Update the active instance variable after any
						// creation/disposal is
						// made. Update the active viewer only if the active
						// viewer is new
						activeInstance = getActiveInstanceFromCurrentView();
						if (activeInstance != null) {
							setActiveInstanceId();
							handleTabSwitchEvent();
						}

						updateMenuAndToolbars();
					}

				});

				return Status.OK_STATUS;
			}
		};
		refreshViews.setRule(new RefreshRule());
		refreshViews.schedule();
	}

	class RefreshRule implements ISchedulingRule {
		@Override
		public boolean contains(ISchedulingRule rule) {
			return this == rule;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule instanceof RefreshRule;
		}
	}

	/**
	 * Updates the zoom action that needs to be checked. This method must be
	 * called every time the focus changes to another viewer.
	 */
	public void updateActiveViewer() {
		updateActiveViewer(null);
	}

	/**
	 * Updates the zoom action that needs to be checked, after performing a
	 * layout operation
	 * 
	 * @param layoutName
	 *            The name of the layout to set if opp is SETLAYOUT
	 */
	public void updateActiveViewer(String layoutName) {
		info("Updating Android Emulator view");

		if (activeInstance != null) {
			AndroidViewData viewData = instanceDataMap.get(activeInstance);
			if (viewData != null) {
				IAndroidComposite androidComposite = viewData.getComposite();
				if ((androidComposite != null)) {
					if ((activeInstance.getProperties().getProperty(IDevicePropertiesOSConstants.useVnc,
							NativeUIUtils.getDefaultUseVnc())).equals("true")) {
						IAndroidSkin androidSkin = viewData.getSkin();

						if (androidSkin != null) {
							if (layoutName != null) {
								activeInstance.setCurrentLayout(layoutName);
							}

							boolean isNeeded = androidSkin.isSwapWidthHeightNeededAtLayout(activeInstance
									.getCurrentLayout());
							IRemoteDisplay.Rotation rotation = (isNeeded ? IRemoteDisplay.Rotation.ROTATION_90DEG_COUNTERCLOCKWISE
									: IRemoteDisplay.Rotation.ROTATION_0DEG);
							viewData.getMainDisplay().setRotation(rotation);
							androidComposite.applyLayout(activeInstance.getCurrentLayout());
						}
					}
					androidComposite.applyZoomFactor();
				}

			}
		}

		updateMenuAndToolbars();

		info("Updated Android Emulator view");
	}

	public void changeToNextLayout() {
		AndroidViewData viewData = instanceDataMap.get(activeInstance);
		IAndroidComposite androidComposite = viewData.getComposite();
		if (androidComposite instanceof NativeWindowComposite) {
			((NativeWindowComposite) androidComposite).changeToNextLayout();
		}
	}

	/**
	 * Retrieves the instance being currently displayed at this view.
	 *
	 * @return The active instance, or null if there is no active instance
	 */
	private IAndroidEmulatorInstance getActiveInstanceFromCurrentView() {
		TabItem activeInstanceItem = getActiveTabItem();
		IAndroidEmulatorInstance instance = null;
		if (activeInstanceItem != null) {
			instance = (IAndroidEmulatorInstance) (activeInstanceItem.getData());
		} else {
			debug("No active instance being shown at emulator view");
		}

		return instance;
	}

	/**
	 * Executes the procedure to connect to the VNC
	 * 
	 * @param androidDevice
	 *            The device being connected
	 */
	public IStatus connectVNC(final IAndroidEmulatorInstance instance, IProgressMonitor monitor) {
		IStatus statusToReturn = Status.OK_STATUS;

		try {
			IAndroidLogicInstance logicInstance = (IAndroidLogicInstance) instance;
			AbstractStartAndroidEmulatorLogic startLogic = logicInstance.getStartLogic();

			startLogic.execute(logicInstance, LogicMode.TRANSFER_AND_CONNECT_VNC, logicInstance.getTimeout(), monitor);
		} catch (StartCancelledException e1) {
			info("The user canceled the transfer/connect to VNC phase.");
			statusToReturn = Status.CANCEL_STATUS;
		} catch (Exception e1) {
			error("Could not establish VNC Connection to " + instance);
			statusToReturn = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID, NLS.bind(
					EmulatorNLS.ERR_CannotConnectToVNC, instance.getName()));
		}
		return statusToReturn;
	}

	/**
	 * Shows the Android Emulator view, if not being shown
	 */
	public static void showView() {
		info("Open and move focus to the emulator view");

		boolean emulatorViewOpened = !EclipseUtils.getAllOpenedViewsWithId(AndroidView.ANDROID_VIEW_ID).isEmpty();

		try {
			// if emulator view is opened previously or if no emulator view is
			// opened,
			// show / refresh the emulator view.
			if (emulatorViewOpened) {
				EclipseUtils.showView(AndroidView.ANDROID_VIEW_ID);
			} else {
				// Make sure only one open view (due to the transition to
				// online) will occur at the same time.
				// e.g. if the "question open dialog" is already opened, it is
				// not needed one

				if (showViewLock.tryLock()) {
					try {

						boolean openEmulatorView = DialogWithToggleUtils.showQuestion(
								SHOW_EMULATOR_IN_THE_IDE_KEY_PREFERENCE,
								EmulatorNLS.QUESTION_AbstractAndroidView_OpenViewForStartedEmulatorsTitle,
								EmulatorNLS.QUESTION_AbstractAndroidView_OpenViewForStartedEmulatorsMessage);
						if (openEmulatorView) {
							EclipseUtils.showView(AndroidView.ANDROID_VIEW_ID);
						}
					} finally {
						showViewLock.unlock();
					}
				}
			}
		} catch (PartInitException e) {
			error("The Android Emulator View could not be opened programatically");
			EclipseUtils.showErrorDialog(EmulatorNLS.GEN_Error,
					EmulatorNLS.EXC_AbstractAndroidView_ViewNotAccessibleProgramatically);
		}
	}

	/**
	 * Creates a viewer for the provided instance
	 *
	 * @param instance
	 *            The instance that will have a viewer created at this view
	 */
	private void createViewer(final IAndroidEmulatorInstance instance) {
		if (instance != null) {
			info("Creating tab for " + instance + " on " + getClass());

			Set<IAndroidEmulatorInstance> currentInstancesWithTab = getInstancesWithAtLeastOneViewer();

			// Creates a tab item to hold the skin at the view
			TabItem newTabItem = new TabItem(tabFolder, SWT.NONE);

			// Set parameters at the tab item
			newTabItem.setText(instance.getFullName());
			newTabItem.setData(instance);
			AndroidViewData emulatorData = new AndroidViewData();
			instanceDataMap.put(instance, emulatorData);

			try {
				createWidgets(newTabItem, instance, emulatorData);
				tabFolder.setSelection(newTabItem);
				setActiveInstanceId();

				// add popup menu
				if (newTabItem.getControl() != null) {
					menuManager.createContextMenu(newTabItem.getControl());
					newTabItem.getControl().addMouseListener(mouseClickListener);
				}

				ProtocolMessage setEncodingMsg = new ProtocolMessage(2);
				setEncodingMsg.setFieldValue("padding", 0);
				setEncodingMsg.setFieldValue("number-of-encodings", 1);
				setEncodingMsg.setFieldValue("encoding-type", "encoding-types", 0, 0);
				PluginProtocolActionDelegate.sendMessageToServer(instance.getProtocolHandle(), setEncodingMsg);

				info("Created tab for " + instance);

				if (instance.getProperties()
						.getProperty(IDevicePropertiesOSConstants.useVnc, NativeUIUtils.getDefaultUseVnc()).toString()
						.equals("true")) {
					startVncDisplays(instance);
					info("Started displays for " + instance);

					// overwrite original tml listeners
					addListenersToMainDisplay(emulatorData);
				} else {
					IAndroidComposite parentComposite = emulatorData.getComposite();
					((NativeWindowComposite) parentComposite).addMouseListener(mouseClickListener);
				}

				IAndroidComposite androidComposite = emulatorData.getComposite();
				if (androidComposite != null) {
					androidComposite.applyZoomFactor();
				}

				// If this is the first view to be opened, guarantee that the
				// screen orientation is
				// synchronized with the current layout (only when using VNC)
				if (!currentInstancesWithTab.contains(instance)
						&& instance.getProperties()
								.getProperty(IDevicePropertiesOSConstants.useVnc, NativeUIUtils.getDefaultUseVnc())
								.toString().equals("true")) {
					IAndroidSkin skin = getSkin(instance);
					if (skin != null) {
						instance.changeOrientation(skin.getLayoutScreenCommand(instance.getCurrentLayout()));
					}
				}

				updateActiveViewer();

				info("Created tab for Android Emulator " + instance);
			} catch (SkinException e) {
				error("The skin associated to this instance (" + instance.getName()
						+ ") is not installed or is corrupted.");
				EclipseUtils.showErrorDialog(e);

				try {
					instance.stop(true);
					disposeViewer(instance);
				} catch (InstanceStopException e1) {
					error("Error while running service for stopping virtual machine");
					EclipseUtils.showErrorDialog(EmulatorNLS.GEN_Error, EmulatorNLS.EXC_General_CannotRunStopService);
				}
			}
		}
	}

	private void addListenersToMainDisplay(AndroidViewData emulatorData) {
		// TmL registers listeners during start, and unregisters all of them
		// during
		// stop. To adapt the listeners to Studio needs, we are including the
		// following
		// operations after the start display call from TmL. With this we are
		// achieving:
		//
		// 1. TmL registers several listeners at its canvas (TmL start method)
		// 2. Studio unregisters the TmL listeners (this method)
		// 3. Studio registers new listeners to replace the TmL ones (this
		// method)
		// 4. TmL unregisters Studio listeners instead of its own (TmL stop
		// method)

		SWTRemoteDisplay remoteDisplay = emulatorData.getMainDisplay();
		final Canvas canvas = remoteDisplay.getCanvas();
		IAndroidComposite parentComposite = emulatorData.getComposite();

		for (int eventType : SWT_EVENT_TYPES) {
			for (Listener listener : canvas.getListeners(eventType)) {
				canvas.removeListener(eventType, listener);
			}
		}

		KeyListener keyListener = parentComposite.getKeyListener();
		final MouseListener mouseListener = parentComposite.getMouseListener();
		MouseMoveListener mouseMoveListener = parentComposite.getMouseMoveListener();

		canvas.addKeyListener(keyListener);
		canvas.addMouseListener(mouseListener);
		canvas.addMouseMoveListener(mouseMoveListener);

		// Due to the differences in listener registration between TmL and
		// Studio, it will
		// remain a registered listener when the viewer is disposed. For this
		// reason, the
		// following dispose listener is being registered.
		DisposeListener disposeListener = new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				canvas.removeMouseListener(mouseListener);
				canvas.removeMouseListener(mouseClickListener);
			}
		};
		emulatorData.setDisposeListener(disposeListener);
		canvas.addDisposeListener(disposeListener);
		canvas.addMouseListener(mouseClickListener);
	}

	/**
	 * Disposes the viewer of the provided instance
	 *
	 * @param instance
	 *            The instance that will have a viewer disposed from this view
	 */
	private void disposeViewer(final IAndroidEmulatorInstance instance) {
		info("Disposing tab of Android Emulator at " + instance);

		TabItem item = getTabItem(instance);
		if (item != null) {

			stopVncDisplays(instance);

			// if there are no other viewers, we can stop protocol and vnc
			// server
			if ((childrenIDs.size() == 1)
					&& (instance.getProperties()
							.getProperty(IDevicePropertiesOSConstants.useVnc, NativeUIUtils.getDefaultUseVnc())
							.toString().equals("true"))) {
				info("There is only one view opened, stop VNC protocol and VNC Server");
				stopVncProtocol((IAndroidLogicInstance) instance);
				stopVncServer(instance);
			}

			AndroidViewData data = instanceDataMap.get(instance);
			if (data != null) {
				SWTRemoteDisplay mainDisplay = data.getMainDisplay();
				if (mainDisplay != null) {
					Canvas canvas = mainDisplay.getCanvas();

					if (canvas != null) {
						canvas.removeDisposeListener(data.getDisposeListener());
					}
				}
			}

			Control c = item.getControl();
			if (c != null) {
				c.dispose();
			}
			item.setControl(null);

			item.dispose();
			instanceDataMap.remove(instance);
			updateMenuAndToolbars();
			info("Disposed tab of Android Emulator at " + instance);
		}

	}

	/**
	 * Gets the list of instances with viewers associated.
	 * 
	 * @return the collection of instances
	 */
	private Collection<IAndroidEmulatorInstance> getInstancesWithViewer() {
		final Collection<IAndroidEmulatorInstance> instancesWithViewer = new LinkedHashSet<IAndroidEmulatorInstance>();

		if (!tabFolder.isDisposed()) {
			final TabItem[] allItems = tabFolder.getItems();

			for (TabItem item : allItems) {
				if (!item.isDisposed()) {
					instancesWithViewer.add((IAndroidEmulatorInstance) item.getData());
				}
			}
		}

		return instancesWithViewer;
	}

	private static Set<IAndroidEmulatorInstance> getInstancesWithAtLeastOneViewer() {
		Set<IAndroidEmulatorInstance> instancesSet = new HashSet<IAndroidEmulatorInstance>();
		for (String viewId : childrenIDs) {
			AbstractAndroidView view = (AbstractAndroidView) EclipseUtils.getActiveView(viewId);
			if (view != null) {
				instancesSet.addAll(view.getInstancesWithViewer());
			}
		}

		return instancesSet;
	}

	/**
	 * Gets the tab item related to the instance
	 * 
	 * @param instance
	 *            the emulator instance
	 * @return the tab item
	 */
	private TabItem getTabItem(IAndroidEmulatorInstance instance) {
		TabItem result = null;
		if (!tabFolder.isDisposed()) {
			TabItem[] allItems = tabFolder.getItems();

			for (TabItem item : allItems) {
				if (instance.equals(item.getData())) {
					result = item;
					break;
				}
			}
		}

		return result;
	}

	/**
	 * Gets the tab item related to the instance
	 * 
	 * @param instance
	 *            the emulator instance
	 * @return the tab item
	 */
	private TabItem getTabItem(IInstance instance) {
		TabItem result = null;
		if (!tabFolder.isDisposed()) {
			TabItem[] allItems = tabFolder.getItems();

			for (TabItem item : allItems) {
				if (instance.getName().equals(((IAndroidEmulatorInstance) item.getData()).getName())) {
					result = item;
					break;
				}
			}
		}

		return result;
	}

	/**
	 * Retrieves the active tab at view
	 *
	 * @return The active tab, or null of there is no tab at tab folder
	 */
	private TabItem getActiveTabItem() {
		int activeInstanceIndex = this.tabFolder.getSelectionIndex();

		TabItem activeTabItem = null;

		if (activeInstanceIndex >= 0) {
			activeTabItem = this.tabFolder.getItem(activeInstanceIndex);
		}

		return activeTabItem;
	}

	/**
	 * Updates the zoom action that needs to be checked. This method must be
	 * called every time the focus changes to another viewer
	 */
	private void updateMenuAndToolbars() {
		IViewSite viewSite = getViewSite();

		if (viewSite != null) {
			IActionBars actionBars = viewSite.getActionBars();

			if (actionBars != null) {
				IMenuManager menuManager = actionBars.getMenuManager();
				updateMenuManager(menuManager, viewSite);

				IToolBarManager toolbarManager = actionBars.getToolBarManager();
				if (toolbarManager != null) {
					IContributionItem[] items = toolbarManager.getItems();
					for (IContributionItem item : items) {
						item.update();
					}
				}
			}

			refreshMenuElements();
		}
	}

	/**
	 * Recursive method to update items at menus. The recursion helps to update
	 * submenus
	 * 
	 * @param manager
	 *            The manager that holds a menu items
	 * @param viewSite
	 *            The current view site.
	 */
	private void updateMenuManager(IMenuManager manager, IViewSite viewSite) {
		// Update the items in menu manager
		if (manager != null) {
			IContributionItem[] items = manager.getItems();
			for (IContributionItem item : items) {
				if (item instanceof IMenuManager) {
					updateMenuManager((IMenuManager) item, viewSite);
				} else {
					item.update();
				}
			}
		}
	}

	/**
	 * Stops all emulator instances with the Progress Monitor opened.
	 */
	private void stopEmulatorInstances() {
		// defines the runnable object for stopping emulator instances.
		final IRunnableWithProgress stopRunnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				Collection<IAndroidEmulatorInstance> startedInstances = DeviceFrameworkManager.getInstance()
						.getAllStartedInstances();
				boolean errorsHappened = false;

				for (IAndroidEmulatorInstance instance : startedInstances) {
					try {
						instance.stop(true);
					} catch (InstanceStopException e) {
						errorsHappened = true;
					}
				}

				// if closing on shutdown, use a progress bar and stall UI
				if (closingOnShutdown) {
					// start a progress monitor
					monitor.beginTask("", IProgressMonitor.UNKNOWN);

					// make sure the stop instance job finished
					Job[] jobs = Job.getJobManager().find(null); // get all jobs
					for (Job job : jobs) {
						if (job.getName().equals(EmulatorNLS.UI_AbstractAndroidView_StopInstanceJob)) {
							// when job result is not null, it has finished
							while (job.getResult() == null) {
								try {
									// sleep a little so the waiting is not too
									// busy
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// do nothing
								}
							}
						}
					}
				}

				if (errorsHappened) {
					EclipseUtils.showErrorDialog(EmulatorNLS.GEN_Error,
							EmulatorNLS.EXC_AncroidView_CannotRunMultipleStopServices);
				}

			}
		};

		// executes the runnable defined above.
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				Shell currentShell = getViewSite().getShell();
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(currentShell);
				try {
					dialog.run(true, false, stopRunnable);
				} catch (Exception e) {
					// Should not have exceptions.
					// The runnable is not interrupted and it handles exceptions
					// internally
					// Log runtime exceptions
					error("Runtime exception was thrown: " + e.getClass().getSimpleName());
				}
			}
		});
	}

	/**
	 * Sets the identifier of the instance being currently displayed at view
	 */
	private void setActiveInstanceId(String activeHost) {
		for (String viewId : childrenIDs) {
			Collection<IViewPart> viewsToUpdateMenu = EclipseUtils.getAllOpenedViewsWithId(viewId);
			for (IViewPart view : viewsToUpdateMenu) {
				AbstractAndroidView emulatorView = (AbstractAndroidView) view;
				emulatorView.setSelection(activeHost);
			}

		}
	}

	/**
	 * Sets the identifier of the instance being currently displayed at view
	 */
	private void setActiveInstanceId() {
		TabItem activeInstanceItem = getActiveTabItem();
		if ((activeInstanceItem != null) && (activeInstanceItem.getData() != null)) {

			activeInstance = (IAndroidEmulatorInstance) activeInstanceItem.getData();

			String activeId = ((IAndroidEmulatorInstance) activeInstanceItem.getData()).getInstanceIdentifier();

			setActiveInstanceId(activeId);
		} else {
			debug("No active instance being shown at emulator view");
		}

	}

	/**
	 * Starts the main display associating it to the protocol.
	 * 
	 * @param handle
	 *            the protocol handle
	 * @param mainDisplay
	 *            the main display object
	 */
	private void startDisplay(ProtocolHandle handle, SWTRemoteDisplay mainDisplay) {
		// Stop any running screens
		if ((mainDisplay.isActive()) && (!mainDisplay.isDisposed())) {
			mainDisplay.stop();
		}

		try {
			info("Starting main display refresh");
			mainDisplay.start(handle);
		} catch (Exception e) {
			error("Viewers could not be started.");
			EclipseUtils.showErrorDialog(EmulatorNLS.GEN_Error, EmulatorNLS.EXC_AndroidView_ErrorStartingScreens);

			GC gc = new GC(mainDisplay.getCanvas());
			gc.fillRectangle(0, 0, mainDisplay.getScreenWidth(), mainDisplay.getScreenHeight());
			gc.dispose();
		}
	}

	/**
	 * Starts viewer (main display and CLI display) of the emulator instance.
	 * 
	 * @param instance
	 *            the emulator instance
	 */
	protected void startVncDisplays(final IAndroidEmulatorInstance instance) {
		AndroidViewData viewData = instanceDataMap.get(instance);
		if (viewData != null) {
			if (viewData.getMainDisplay() != null) {
				startDisplay(instance.getProtocolHandle(), viewData.getMainDisplay());
			}
			if ((viewData.getCliDisplay() != null) && instance.getHasCli()) {
				viewData.getCliDisplay().start();
			}
		}
	}

	/**
	 * Stops viewer (main display and CLI display) of the emulator instance.
	 */
	private void stopVncDisplays(final IAndroidEmulatorInstance instance) {
		info("Stop the VNC Display " + getViewId() + " for " + instance);
		AndroidViewData viewData = instanceDataMap.get(instance);

		if ((viewData != null)) {
			SWTRemoteDisplay mainDisplay = viewData.getMainDisplay();
			if ((mainDisplay != null) && mainDisplay.isActive() && !mainDisplay.isDisposed()) {
				mainDisplay.stop();
				if ((mainDisplay.getBackground() != null) && !mainDisplay.getBackground().isDisposed()) {
					mainDisplay.getBackground().dispose();
				}
			}

			RemoteCLIDisplay cliDisplay = viewData.getCliDisplay();
			if ((cliDisplay != null) && cliDisplay.isDisplayActive() && !cliDisplay.isDisposed()) {
				cliDisplay.stop();
				if ((cliDisplay.getBackground() != null) && !cliDisplay.getBackground().isDisposed()) {
					cliDisplay.getBackground().dispose();
				}
			}
		}
	}

	/**
	 * @param instance
	 */
	private void stopVncProtocol(IAndroidLogicInstance instance) {
		AndroidEmulatorStopper.stopInstance(instance, true, false, new NullProgressMonitor());

	}

	/**
	 * Stops the execution of the vnc server if it is running on the given
	 * instance. This acts as if the Control+C was pressed in the shell where
	 * the vnc server is executing...
	 * 
	 * @param instance
	 */
	private void stopVncServer(IAndroidEmulatorInstance instance) {
		StartVncServerLogic.cancelCurrentVncServerJobs(instance);
	}

	/**
	 * Class to implement the IPerspectiveListener that you be used as
	 * ParListener2 of current page when the Emulator View is opened. It is
	 * required to code the work-around for Sticky Views on perspectiveChanged
	 * method.
	 */
	private class PerspectiveListenerImpl implements IPerspectiveListener {

		@Override
		public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
			// Nothing to do.
		}

		@Override
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
			if (changeId.equals(IWorkbenchPage.CHANGE_VIEW_HIDE)) {

				// if the emulator view was hidden
				if (page.findView(getViewId()) == null) {

					// This is a "sticky view" so when it is hidden or shown for
					// one
					// perspective, its state is remembered for the other ones.
					// However, the view reference count is just updated when
					// the current active perspective is changed. The code below
					// forces the perspective changing in order to dispose the
					// view
					// immediately after it is hidden.
					for (IPerspectiveDescriptor pd : page.getOpenPerspectives()) {
						if (!pd.equals(perspective)) {
							page.setPerspective(pd);
						}
					}
					page.setPerspective(perspective);
				}
			}
		}
	}

	/**
	 * Class to implement IPartListener2 that you be used as ParListener2 of
	 * current page when the Emulator View is opened. It is necessary to
	 * determine when the view is closed.
	 */
	private class PartListenerImpl implements IPartListener2 {

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			// Nothing to do.
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
			// Nothing to do.
		}

		@Override
		public void partClosed(final IWorkbenchPartReference partRef) {

			// if view that is being closed is not THIS view.
			if (!partRef.getId().equals(getViewId())) {
				return;
			}

			// executed on async mode to avoid UI blocking
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					boolean openedViewsExist = false;

					for (String viewId : childrenIDs) {
						if (!getViewId().equals(viewId) && (partRef.getPage().findView(viewId) != null)) {
							openedViewsExist = true;
							break;
						}
					}

					// stops all viewers and clear the tabs list
					Collection<IAndroidEmulatorInstance> instances = getInstancesWithViewer();
					for (IAndroidEmulatorInstance instance : instances) {
						disposeViewer(instance);
					}

					// if the tool is not being closed and there is no other
					// emulator
					// view opened.
					if (!closingOnShutdown && !openedViewsExist) {

						Collection<IAndroidEmulatorInstance> startedInstances = DeviceFrameworkManager.getInstance()
								.getAllStartedInstances();

						boolean oneInstanceStarted = (startedInstances.size() > 0);

						if (oneInstanceStarted
								&& (DialogWithToggleUtils.showQuestion(STOP_BY_CLOSING_VIEW_KEY_PREFERENCE,
										EmulatorNLS.QUESTION_AndroidView_StopAllInstancesOnDisposeTitle,
										EmulatorNLS.QUESTION_AndroidView_StopAllInstancesOnDisposeMessage))) {

							stopEmulatorInstances();
						}
					}

				}

			});
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
			// Nothing to do.
		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
			// Nothing to do.
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
			// Nothing to do.
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			// Nothing to do.
		}

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			if (partRef.getId().equals(getViewId())) {
				refreshView();
			}
		}
	}

	/**
	 * Class to implement the IWorkbenchListener that you be used as
	 * WorkbenchListener of workbench when the Emulator View is opened. It is
	 * used to know if the view is being closed due to workbench shutdown.
	 */
	private class WorkbenchListenerImpl implements IWorkbenchListener {

		@Override
		public void postShutdown(IWorkbench workbench) {
			// Nothing to do.
		}

		@Override
		public boolean preShutdown(IWorkbench workbench, boolean forced) {
			closingOnShutdown = true;

			Collection<IAndroidEmulatorInstance> startedInstances = DeviceFrameworkManager.getInstance()
					.getAllStartedInstances();

			if (startedInstances.size() > 0) {

				boolean stopEmulatorInstances = false;
				if (PluginUtils.getOS() != PluginUtils.OS_LINUX) {
					stopEmulatorInstances = DialogWithToggleUtils.showQuestion(
							STOP_ALL_EMULATORS_IN_SHUTDOWN_KEY_PREFERENCE,
							EmulatorNLS.QUESTION_RunningInstancesOnClose_Title,
							EmulatorNLS.QUESTION_RunningInstancesOnClose_Text);
				} else {
					DialogWithToggleUtils.showWarning(STOP_ALL_EMULATORS_IN_SHUTDOWN_KEY_PREFERENCE,
							EmulatorNLS.WARN_RunningInstancesOnClose_Linux_Title,
							EmulatorNLS.WARN_RunningInstancesOnClose_Linux_Text);
					// stopEmulatorInstances = true;
				}

				if (stopEmulatorInstances) {
					stopEmulatorInstances();
				}

			}

			return true;
		}
	}

	/**
	 * Selects the tab that has this data host and set the activeHost
	 * 
	 * @param host
	 *            IP address
	 */
	private void setSelection(final String host) {

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				TabItem selectedTab = null;

				TabItem[] tabArray = tabFolder.getItems();

				for (TabItem tabItem : tabArray) {
					String tabItemHost = ((IAndroidEmulatorInstance) tabItem.getData()).getInstanceIdentifier();
					if ((host != null) && (host.equals(tabItemHost))) {
						selectedTab = tabItem;
						break;
					}
				}

				if (selectedTab != null) {
					tabFolder.setSelection(selectedTab);
					updateMenuAndToolbars();
				}

			}
		});

	}

	public static void updateInstanceName(final IInstance instance) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				if (!childrenIDs.isEmpty()) {
					AbstractAndroidView view = (AbstractAndroidView) EclipseUtils.getActiveView(childrenIDs.get(0));
					if (view != null) {
						if ((instance != null)) {
							TabItem tabItem = view.getTabItem(instance);
							if (tabItem != null) {
								tabItem.setText(((IAndroidEmulatorInstance) tabItem.getData()).getFullName());
							}
						}
					}
				}
			}
		});
	}

	/**
	 * Sets the skin zoom factor
	 * 
	 * @param instance
	 *            the emulator instance
	 * @param zoom
	 *            the zoom factor
	 */
	public final void setZoomFactor(IAndroidEmulatorInstance instance, double zoom) {
		try {
			AndroidViewData viewData = instanceDataMap.get(instance);
			if (viewData != null) {
				IAndroidComposite composite = viewData.getComposite();
				if (composite != null) {
					composite.setZoomFactor(zoom);
				}
			}
		} catch (Exception e) {
			error("Detached zoom could not be set.");
		}
	}

	/**
	 * Gets the skin zoom factor
	 * 
	 * @param instance
	 *            the emulator instance
	 * @return the zoom factor
	 */
	public final double getZoomFactor(IAndroidEmulatorInstance instance) {
		double zoomFactor = 0.0;
		AndroidViewData viewData = instanceDataMap.get(instance);
		if (viewData != null) {
			IAndroidComposite composite = viewData.getComposite();
			if (composite != null) {
				zoomFactor = composite.getZoomFactor();
			}
		}
		return zoomFactor;
	}
}