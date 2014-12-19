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

package org.eclipse.andmore.android.perspective;

import java.net.URL;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.ui.JavaPerspectiveFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.IIntroConstants;
import org.osgi.framework.BundleContext;

@SuppressWarnings("restriction")
public class MotodevStudioAndroidPerspective extends JavaPerspectiveFactory {
	private static String VIEW_PACKAGE_EXPLORER = "org.eclipse.jdt.ui.PackageExplorer";

	private static String VIEW_PROBLEM = "org.eclipse.ui.views.ProblemView";

	private static String VIEW_CONSOLE = "org.eclipse.ui.console.ConsoleView";

	private static String VIEW_OUTLINE = "org.eclipse.ui.views.ContentOutline";

	private static String VIEW_ANDROID_EMULATOR = "org.eclipse.andmore.android.emulator.androidView";

	private static String VIEW_SNIPPETS = "org.eclipse.wst.common.snippets.internal.ui.SnippetsView";

	// DDMS Views

	private static String DDMSVIEW_EMULATOR_CONTROL = "org.eclipse.andmore.ddms.views.EmulatorControlView";

	private static String DDMSVIEW_LOGCAT = "org.eclipse.andmore.ddms.views.LogCatView";

	private static String DDMSVIEW_FILE_EXPLORER = "org.eclipse.andmore.ddms.views.FileExplorerView";

	private static String PERSPECTIVE_ANDROID = "org.eclipse.andmore.android.perspective";

	private static String PERSPECTIVE_OPHONE = "org.eclipse.andmore.android.ophone.perspective";

	private static String PERSPECTIVE_DDMS = "org.eclipse.andmore.ddms.Perspective";

	private static String PERSPECTIVE_EMULATOR = "org.eclipse.andmore.android.emulator.perspective";

	private static String PERSPECTIVE_DEBUG = "org.eclipse.debug.ui.DebugPerspective";

	private static String LAUNCH_COOLBAR_SHORTCUT = "org.eclipse.debug.ui.launchActionSet";

	private static String VIEW_TML_DEV_MGT = "org.eclipse.sequoyah.device.framework.ui.InstanceMgtView";

	private static String WIZARD_PROJECT = "org.eclipse.andmore.android.wizards.newProjectWizard";

	private static String WIZARD_WIDGET_PROJECT = "org.eclipse.andmore.android.wizards.newWidgetProjectWizard";

	private static String WIZARD_ACTIVITY = "org.eclipse.andmore.android.wizards.newActivityWizard";

	private static String WIZARD_ACTIVITY_BASED_ON_TEMPLATE = "org.eclipse.andmore.android.wizards.newActivityBasedOnTemplateWizard";

	private static String WIZARD_RECEIVER = "org.eclipse.andmore.android.wizards.newReceiverWizard";

	private static String WIZARD_SERVICE = "org.eclipse.andmore.android.wizards.newServiceWizard";

	private static String WIZARD_PROVIDER = "org.eclipse.andmore.android.wizards.newProviderWizard";

	private static String WIZARD_ANDROID_XML = "org.eclipse.andmore.eclipse.editors.wizards.NewXmlFileWizard";

	private static String WIZARD_WIDGET_PROVIDER = "org.eclipse.andmore.android.wizard.newWidgetProviderWizard";

	private static String WIZARD_JAVA_PACKAGE = "org.eclipse.jdt.ui.wizards.NewPackageCreationWizard";

	private static String WIZARD_JAVA_CLASS = "org.eclipse.jdt.ui.wizards.NewClassCreationWizard";

	private static String WIZARD_JAVA_INTERFACE = "org.eclipse.jdt.ui.wizards.NewInterfaceCreationWizard";

	private static String WIZARD_NEW_FOLDER = "org.eclipse.ui.wizards.new.folder";

	private static String VIEW_APPLICATION_SIGNING_TOOL = "org.eclipse.andmore.android.packaging.ui.signingview";

	private static final String STUDIO_INFO_INITIAL_PAGE_PROPERTY = "studio.android.initial.page";

	private static final String STUDIO_INFO_INITIAL_PAGE_FILE = "MOTODEV/index.html";

	private static IPerspectiveListener perspectiveListener = null;

	/**
	 * Creates the initial layout for a page.
	 * 
	 * @param layout
	 *            the page layout
	 * 
	 * @see IPerspectiveFactory#createInitialLayout(IPageLayout)
	 */
	@Override
	public void createInitialLayout(final IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.2f, editorArea);
		left.addView(VIEW_PACKAGE_EXPLORER);
		left.addView(DDMSVIEW_FILE_EXPLORER);

		IFolderLayout leftBottom = layout.createFolder("leftBottom", IPageLayout.BOTTOM, 0.59f, "left");
		leftBottom.addView(VIEW_SNIPPETS);
		leftBottom.addView(VIEW_OUTLINE);

		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.6f, editorArea);
		right.addView(VIEW_ANDROID_EMULATOR);

		IFolderLayout bottomMiddle = layout.createFolder("bottomMiddle", IPageLayout.BOTTOM, 0.59f, editorArea);
		bottomMiddle.addView(VIEW_TML_DEV_MGT);
		bottomMiddle.addView(DDMSVIEW_EMULATOR_CONTROL);
		bottomMiddle.addView(DDMSVIEW_LOGCAT);
		bottomMiddle.addView(VIEW_CONSOLE);
		bottomMiddle.addView(VIEW_PROBLEM);
		bottomMiddle.addView(VIEW_APPLICATION_SIGNING_TOOL);

		layout.addShowViewShortcut(VIEW_TML_DEV_MGT);
		layout.addShowViewShortcut(VIEW_ANDROID_EMULATOR);
		layout.addShowViewShortcut(VIEW_SNIPPETS);
		layout.addShowViewShortcut(VIEW_APPLICATION_SIGNING_TOOL);
		// layout.addShowViewShortcut(VIEW_VIDEOS);

		layout.addPerspectiveShortcut(PERSPECTIVE_ANDROID);
		layout.addPerspectiveShortcut(PERSPECTIVE_OPHONE);
		layout.addPerspectiveShortcut(PERSPECTIVE_DDMS);
		layout.addPerspectiveShortcut(PERSPECTIVE_DEBUG);
		layout.addPerspectiveShortcut(PERSPECTIVE_EMULATOR);

		layout.addActionSet(LAUNCH_COOLBAR_SHORTCUT);

		layout.addNewWizardShortcut(WIZARD_PROJECT);
		layout.addNewWizardShortcut(WIZARD_WIDGET_PROJECT);
		layout.addNewWizardShortcut(WIZARD_ACTIVITY);
		layout.addNewWizardShortcut(WIZARD_ACTIVITY_BASED_ON_TEMPLATE);
		layout.addNewWizardShortcut(WIZARD_RECEIVER);
		layout.addNewWizardShortcut(WIZARD_SERVICE);
		layout.addNewWizardShortcut(WIZARD_PROVIDER);
		layout.addNewWizardShortcut(WIZARD_WIDGET_PROVIDER);
		layout.addNewWizardShortcut(WIZARD_JAVA_PACKAGE);
		layout.addNewWizardShortcut(WIZARD_JAVA_CLASS);
		layout.addNewWizardShortcut(WIZARD_JAVA_INTERFACE);
		layout.addNewWizardShortcut(WIZARD_ANDROID_XML);
		layout.addNewWizardShortcut(WIZARD_NEW_FOLDER);

		final IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage activePage = null;
		if (activeWindow != null) {
			activePage = activeWindow.getActivePage();
			addPerspectiveListener(activeWindow);
			firePerspectiveInitActions(activePage, layout);
		}

		IPageListener pageListener = new IPageListener() {
			@Override
			public void pageActivated(IWorkbenchPage page) {
				firePerspectiveInitActions(page, layout);
				Display disp = PlatformUI.getWorkbench().getDisplay();
				final IPageListener thisListener = this;
				disp.asyncExec(new Runnable() {
					@Override
					public void run() {
						if (activeWindow != null) {
							activeWindow.removePageListener(thisListener);
						}
					}
				});
			}

			@Override
			public void pageClosed(IWorkbenchPage page) {
				// do nothing
			}

			@Override
			public void pageOpened(IWorkbenchPage page) {
				// do nothing
			}

		};
		if (activeWindow != null) {
			activeWindow.addPageListener(pageListener);
		}

		StudioLogger.debug(MotodevStudioAndroidPerspective.class, "MOTODEV Studio Perspective created.");
	}

	/**
	 * Creates and adds a perspective listener that will remove the
	 * "maximized state" of the Welcome (intro) view before hiding it.
	 * 
	 * @param activeWindow
	 *            Workbench Window where the listener will be added.
	 */
	private synchronized static void addPerspectiveListener(IWorkbenchWindow activeWindow) {
		if (perspectiveListener == null) {
			perspectiveListener = new PerspectiveAdapter() {

				@Override
				public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective,
						IWorkbenchPartReference partRef, String changeId) {
					if (PERSPECTIVE_ANDROID.equals(perspective.getId())) {
						if (IIntroConstants.INTRO_VIEW_ID.equals(partRef.getId())) {
							if (IWorkbenchPage.CHANGE_VIEW_HIDE.equals(changeId)) {
								if (IWorkbenchPage.STATE_MAXIMIZED == page.getPartState(partRef)) {
									page.toggleZoom(partRef);
								}
							}
						}
					}
				}

			};
			activeWindow.addPerspectiveListener(perspectiveListener);
		}

	}

	/**
	 * 
	 * @param activeWindow
	 * @param layout
	 */
	private void firePerspectiveInitActions(final IWorkbenchPage activePage, final IPageLayout layout) {
		if (activePage != null) {
			// Open MOTODEV Web Resources on a Web Browser Editor
			openStudioInfoOnWebBrowserEditor(activePage);
		}

	}

	/**
	 * Opens a web browser with useful information from Studio for Android
	 */
	public static void openStudioInfoOnWebBrowserEditor(IWorkbenchPage page) {

		URL initialPageURL = getWebResourcesURL();

		if ((initialPageURL != null) && (!Platform.getOS().equals(Platform.OS_LINUX))) {
			EclipseUtils.openedWebEditor(page, initialPageURL);
		}
	}

	/**
	 * @return
	 */
	public static URL getWebResourcesURL() {
		URL initialPageURL = null;

		try {
			BundleContext context = AndroidPlugin.getDefault().getBundle().getBundleContext();
			String initialPage = context.getProperty(STUDIO_INFO_INITIAL_PAGE_PROPERTY);
			StudioLogger.debug(MotodevStudioAndroidPerspective.class, "Read initial page property:"
					+ STUDIO_INFO_INITIAL_PAGE_PROPERTY + " = " + initialPage);
			if (initialPage != null) {
				StudioLogger.debug(MotodevStudioAndroidPerspective.class,
						"Using the customized URL to be opened in the Web Browser Editor:" + initialPage);
				initialPageURL = new URL(initialPage);
			}
		} catch (Exception e) {
			StudioLogger.error(MotodevStudioAndroidPerspective.class,
					"Unable to read customized URL to be opened in the Web Browser Editor..." + e.getMessage());
		}

		if (initialPageURL == null) {
			try {
				StudioLogger.debug(MotodevStudioAndroidPerspective.class,
						"Use the default URL to be opened in the Web Browser Editor.");
				URL installDir = Platform.getInstallLocation().getURL();
				initialPageURL = new URL(installDir, STUDIO_INFO_INITIAL_PAGE_FILE);
			} catch (Exception e) {
				StudioLogger.error(MotodevStudioAndroidPerspective.class,
						"Unable to show Web Browser Editor with URL: " + e.getMessage());
			}
		}
		return initialPageURL;
	}

}
