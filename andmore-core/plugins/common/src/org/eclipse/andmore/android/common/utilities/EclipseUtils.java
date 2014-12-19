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
package org.eclipse.andmore.android.common.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.andmore.android.common.CommonPlugin;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.sequoyah.localization.tools.datamodel.LocaleInfo;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFileBean;
import org.eclipse.sequoyah.localization.tools.datamodel.StringLocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringNode;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ILocalizationSchema;
import org.eclipse.sequoyah.localization.tools.managers.LocalizationManager;
import org.eclipse.sequoyah.localization.tools.managers.ProjectLocalizationManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.internal.browser.WebBrowserEditor;
import org.eclipse.ui.internal.browser.WebBrowserEditorInput;
import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;
import org.osgi.framework.Bundle;

/**
 * Class that contains methods to do common tasks through the Eclipse Platform
 */
@SuppressWarnings("restriction")
public class EclipseUtils {

	private static final String STUDIO_ANDROID_CONSOLE_ID = "Studio for Android";

	private static final String ORG_ECLIPSE_UI_NET_NET_PREFERENCES = "org.eclipse.ui.net.NetPreferences"; //$NON-NLS-1$

	private static final String ORG_ECLIPSE_EQUINOX_SECURE_STORAGE_PREFERENCES = "org.eclipse.equinox.security.ui.storage"; //$NON-NLS-1$

	private static final String LOCALIZATION_FILE_TYPE = "org.eclipse.sequoyah.localization.android.datamodel.AndroidStringLocalizationFile"; //$NON-NLS-1$

	/**
	 * Shows an error dialog
	 * 
	 * @param dialogTitle
	 *            The dialog title
	 * @param dialogMessage
	 *            The dialog message
	 * @param status
	 *            The IStatus object containing the error
	 */
	public static void showErrorDialog(final String dialogTitle, final String dialogMessage, final IStatus status) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				Shell aShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

				Shell shell = new Shell(aShell);

				Rectangle parentSize = aShell.getParent() != null ? aShell.getParent().getBounds() : shell.getBounds();

				ErrorDialog errorDlg = new ErrorDialog(shell, dialogTitle, dialogMessage, status, IStatus.ERROR);

				Rectangle dialogSize = shell.getBounds();

				int x = ((parentSize.width - dialogSize.width) / 2) + parentSize.x;
				int y = ((parentSize.height - dialogSize.height) / 2) + parentSize.y;

				shell.setLocation(x, y);

				errorDlg.open();
			}

		});
	}

	/**
	 * Shows an information dialog
	 * 
	 * @param dialogTitle
	 *            The dialog title
	 * @param dialogMessage
	 *            The dialog message
	 * @param buttonLabels
	 *            The labels to be displayed as buttons in the dialog, or
	 *            <code>null</code> to have only an "OK" button.
	 * 
	 * @return The code of the button pressed by the user
	 */
	public static int showInformationDialog(String dialogTitle, String dialogMessage, String[] buttonLabels) {
		return showInformationDialog(dialogTitle, dialogMessage, null, buttonLabels, MessageDialog.INFORMATION);
	}

	public static int showInformationDialog(String dialogTitle, String dialogMessage, String[] buttonLabels,
			int dialogImageType) {
		return showInformationDialog(dialogTitle, dialogMessage, null, buttonLabels, dialogImageType);
	}

	/**
	 * Shows an information dialog
	 * 
	 * @param dialogTitle
	 *            The dialog title
	 * @param dialogMessage
	 *            The dialog message
	 * @param detailsMessage
	 *            The details message
	 * @param buttonLabels
	 *            The labels to be displayed as buttons in the dialog, or
	 *            <code>null</code> to have only an "OK" button.
	 * @param dialogImageType
	 *            The image to be displayed right before the dialogMessage
	 * 
	 * @return The code of the button pressed by the user
	 */
	public static int showInformationDialogWithDetails(String dialogTitle, String dialogMessage, String detailsMessage,
			String[] buttonLabels) {
		return showInformationDialog(dialogTitle, dialogMessage, detailsMessage, buttonLabels,
				MessageDialog.INFORMATION);
	}

	private static int showInformationDialog(final String dialogTitle, final String dialogMessage,
			final String detailsMessage, final String[] buttonLabels, final int dialogImageType) {
		final int[] returnCode = new int[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

				Rectangle parentSize;
				if (shell.getParent() != null) {
					parentSize = shell.getParent().getBounds();
				} else {

					parentSize = shell.getBounds();
				}

				MessageDialog dlg;
				String[] internButtonLabels = buttonLabels;
				if (internButtonLabels == null) {
					internButtonLabels = new String[] { "OK" };
				}

				if (detailsMessage == null) {
					dlg = new MessageDialog(shell, dialogTitle, null, dialogMessage, dialogImageType,
							internButtonLabels, 0);
				} else {
					dlg = new MessageDialog(shell, dialogTitle, null, dialogMessage, dialogImageType,
							internButtonLabels, 0) {
						@Override
						protected Control createCustomArea(Composite parent) {
							final Composite main = new Composite(parent, parent.getStyle());
							GridLayout layout = new GridLayout();
							main.setLayout(layout);
							GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
							main.setLayoutData(data);

							final Button detailsButton = new Button(main, SWT.PUSH);
							detailsButton.setText(UtilitiesNLS.UI_EclipseUtils_OpenDetails);
							data = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
							detailsButton.setLayoutData(data);
							detailsButton.addSelectionListener(new SelectionAdapter() {
								private Label detailsText;

								@Override
								public void widgetSelected(SelectionEvent e) {
									if ((detailsText != null) && !detailsText.isDisposed()) {
										detailsButton.setText(UtilitiesNLS.UI_EclipseUtils_OpenDetails);
										detailsText.dispose();
									} else {
										detailsButton.setText(UtilitiesNLS.UI_EclipseUtils_CloseDetails);
										detailsText = new Label(main, SWT.WRAP | SWT.BORDER);
										detailsText.setText(detailsMessage);
										GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
										detailsText.setLayoutData(data);
										GridDataFactory
												.fillDefaults()
												.align(SWT.FILL, SWT.BEGINNING)
												.grab(true, false)
												.hint(convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH),
														SWT.DEFAULT).applyTo(detailsText);
									}
									getShell().pack();
								}
							});

							return main;
						}
					};
				}

				Rectangle dialogSize = shell.getBounds();

				int x = ((parentSize.width - dialogSize.width) / 2) + parentSize.x;
				int y = ((parentSize.height - dialogSize.height) / 2) + parentSize.y;

				shell.setLocation(x, y);

				returnCode[0] = dlg.open();
			}
		});

		return returnCode[0];
	}

	/**
	 * Display a yes/no question dialog box
	 * 
	 * @param title
	 *            The title of the dialog box
	 * @param message
	 *            The error message
	 * @param display
	 *            the parent display
	 * @return true if OK was clicked.
	 */
	public final static boolean displayPrompt(final Display display, final String title, final String message) {
		/*
		 * Sometimes we need to ask the user what he wants to do.
		 */
		final boolean[] result = new boolean[1];
		display.syncExec(new Runnable() {

			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				result[0] = MessageDialog.openQuestion(shell, title, message);
			}
		});
		return result[0];
	}

	/**
	 * Opens a Yes/No dialog
	 * 
	 * @param dialogTitle
	 *            The dialog title
	 * @param dialogMessage
	 *            The dialog message
	 * 
	 * @return true if the user answers yes and false otherwise
	 */
	public static boolean openYesNoDialog(final String dialogTitle, final String dialogMessage) {
		final Boolean[] answer = new Boolean[1];

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				MessageBox msgBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.YES | SWT.NO);
				msgBox.setText(dialogTitle);
				msgBox.setMessage(dialogMessage);

				answer[0] = msgBox.open() == SWT.YES;
			}
		});

		return answer[0];
	}

	/**
	 * Show the error message for the given AndroidException
	 * 
	 * @param e
	 *            The AndroidException that generated the error to be displayed.
	 */
	public static void showErrorDialog(AndroidException e) {
		String title = UtilitiesNLS.ERR_Gen_ErrorTitle;
		showErrorDialog(title, e.getMessage());
	}

	/**
	 * Show the error message using the given title and message
	 * 
	 * @param title
	 *            of the error dialog
	 * @param message
	 *            to be displayed in the error dialog.
	 */
	public static void showErrorDialog(final String title, final String message) {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				MessageDialog.openError(ww.getShell(), title, message);
			}
		});
	}

	/**
	 * Show an information message using the given title and message
	 * 
	 * @param title
	 *            of the dialog
	 * @param message
	 *            to be displayed in the dialog.
	 */
	public static void showInformationDialog(final String title, final String message) {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
				Shell shell = ww.getShell();
				MessageDialog.openInformation(shell, title, message);
			}
		});
	}

	/**
	 * Show a warning message using the given title and message
	 * 
	 * @param title
	 *            of the dialog
	 * @param message
	 *            to be displayed in the dialog.
	 */
	public static void showWarningDialog(final String title, final String message) {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
				Shell shell = ww.getShell();
				MessageDialog.openWarning(shell, title, message);
			}
		});
	}

	/**
	 * Show a question message using the given title and message
	 * 
	 * @param title
	 *            of the dialog
	 * @param message
	 *            to be displayed in the dialog.
	 */
	public static boolean showQuestionDialog(final String title, final String message) {
		class BooleanWrapper {
			public boolean bool = false;
		}

		final BooleanWrapper boolWrapper = new BooleanWrapper();
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
				Shell shell = ww.getShell();
				boolWrapper.bool = MessageDialog.openQuestion(shell, title, message);
			}
		});

		return boolWrapper.bool;
	}

	/**
	 * Show a question message using the given title and message
	 * 
	 * @param title
	 *            of the dialog
	 * @param message
	 *            to be displayed in the dialog.
	 */
	public static int showQuestionWithCancelDialog(final String title, final String message) {
		class IntWrapper {
			public int diagReturn = 0;
		}

		final IntWrapper intWrapper = new IntWrapper();
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
				Shell shell = ww.getShell();
				MessageDialog dialog = new MessageDialog(shell, title, null, message, MessageDialog.QUESTION,
						new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
								IDialogConstants.CANCEL_LABEL }, 0);
				int diagResults = dialog.open();
				switch (diagResults) {
				case 0:
					intWrapper.diagReturn = SWT.YES;
					break;
				case 1:
					intWrapper.diagReturn = SWT.NO;
					break;
				case 2:
				default:
					intWrapper.diagReturn = SWT.CANCEL;
					break;
				}
			}
		});

		return intWrapper.diagReturn;
	}

	/**
	 * Show a question message using the given title and message
	 * 
	 * @param title
	 *            of the dialog
	 * @param message
	 *            to be displayed in the dialog.
	 */
	public static int showQuestionYesAllCancelDialog(final String title, final String message) {
		class IntWrapper {
			public int diagReturn = 0;
		}

		final IntWrapper intWrapper = new IntWrapper();
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
				Shell shell = ww.getShell();
				MessageDialog dialog = new MessageDialog(shell, title, null, message, MessageDialog.QUESTION,
						new String[] { IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL,
								IDialogConstants.NO_LABEL }, 0);
				int diagResults = dialog.open();
				switch (diagResults) {
				case 0:
					intWrapper.diagReturn = IDialogConstants.YES_ID;
					break;
				case 1:
					intWrapper.diagReturn = IDialogConstants.YES_TO_ALL_ID;
					break;
				case 2:
				default:
					intWrapper.diagReturn = IDialogConstants.NO_ID;
					break;
				}
			}
		});

		return intWrapper.diagReturn;
	}

	/**
	 * Returns a plugin attribute using the extension as parameter.
	 * 
	 * @param extensionId
	 *            the extension from which the attribute should be collected
	 * @param elementName
	 *            the extension element
	 * 
	 * @return The executable class associated to the provided element
	 * 
	 * @throws CoreException
	 */
	public static Object getExecutable(String extensionId, String elementName) throws CoreException {
		Object executable = null;

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtension fromExtension = registry.getExtension(extensionId);

		if ((fromExtension != null) && (elementName != null)) {
			IConfigurationElement[] elements = fromExtension.getConfigurationElements();

			for (IConfigurationElement element : elements) {
				if (elementName.equals(element.getName())) {
					executable = element.createExecutableExtension("class");
				}
			}
		}

		return executable;
	}

	/**
	 * Returns an array of extensions that are plugged in a provided extension
	 * point
	 * 
	 * @param extensionPointId
	 *            the id of the extension point to look for extensions at
	 * 
	 * @return an array containing the plugins plugged at the extension point
	 */
	public static IExtension[] getInstalledPlugins(String extensionPointId) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(extensionPointId);
		IExtension[] pluggedExtensions;

		if (extensionPoint != null) {
			pluggedExtensions = extensionPoint.getExtensions();
		} else {
			pluggedExtensions = new IExtension[0];
		}

		return pluggedExtensions;
	}

	/**
	 * Retrieves a view object, showing it at the IDE if hidden
	 * 
	 * @param viewId
	 *            The identifier of the view to be shown
	 * 
	 * @return The view that was just opened, or a reference to an already
	 *         opened view
	 * 
	 * @throws PartInitException
	 *             If the view cannot be shown at the workbench part
	 */
	public static IViewPart showView(final String viewId) throws PartInitException {
		final Object[] tempObj = new Object[1];

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (activeWindow != null) {
					IWorkbenchPage activePage = activeWindow.getActivePage();
					if (activePage != null) {
						try {
							tempObj[0] = activePage.showView(viewId);
						} catch (PartInitException e) {
							tempObj[0] = e;
						}
					}
				}
			}
		});

		if (tempObj[0] instanceof PartInitException) {
			throw (PartInitException) tempObj[0];
		}

		return (IViewPart) tempObj[0];
	}

	/**
	 * Retrieves a view object from the active window, but do not show if it is
	 * hidden
	 * 
	 * @param viewId
	 *            The identifier of the view to be retrieved
	 * 
	 * @return A reference to the view identified by viewId if available; null
	 *         otherwise
	 */
	public static IViewPart getActiveView(final String viewId) {
		final Object[] tempObj = new Object[1];

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (activeWindow != null) {
					IWorkbenchPage activePage = activeWindow.getActivePage();
					if (activePage != null) {
						IViewReference ref = activePage.findViewReference(viewId);
						if (ref != null) {
							IViewPart part = ref.getView(false);
							tempObj[0] = part;
						}

					}
				}
			}
		});

		return (IViewPart) tempObj[0];
	}

	/**
	 * Retrieves a view object, but do not show it if hidden
	 * 
	 * @param viewId
	 *            The identifier of the view object to be retrieved
	 * 
	 * @return A collection of views with provided id that are being shown in
	 *         any opened perspective
	 */
	public static Collection<IViewPart> getAllOpenedViewsWithId(final String viewId) {
		final Collection<IViewPart> openedViews = new LinkedHashSet<IViewPart>();

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchWindow[] allWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
				for (IWorkbenchWindow window : allWindows) {
					IWorkbenchPage[] allPagesInWindow = window.getPages();

					for (IWorkbenchPage page : allPagesInWindow) {
						IViewPart view = page.findView(viewId);
						if (view != null) {
							openedViews.add(view);
						}
					}
				}
			}
		});

		return openedViews;
	}

	/**
	 * Retrieves all editor objects
	 * 
	 * @return A collection of all editors
	 */
	public static Collection<IEditorPart> getAllOpenedEditors() {
		final Collection<IEditorPart> editors = new LinkedHashSet<IEditorPart>();

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchWindow[] allWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
				for (IWorkbenchWindow window : allWindows) {
					IWorkbenchPage[] allPagesInWindow = window.getPages();

					for (IWorkbenchPage page : allPagesInWindow) {
						IEditorReference[] editorRefs = page.getEditorReferences();
						for (IEditorReference editorRef : editorRefs) {
							editors.add(editorRef.getEditor(false));
						}
					}
				}
			}
		});

		return editors;
	}

	/**
	 * Retrieves the page for the given editor
	 * 
	 * @param editor
	 * 
	 * @return A page
	 */
	public static IWorkbenchPage getPageForEditor(final IEditorPart editor) {

		final Object[] tempObj = new Object[1];

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchWindow[] allWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
				for (IWorkbenchWindow window : allWindows) {
					IWorkbenchPage[] allPagesInWindow = window.getPages();

					for (IWorkbenchPage page : allPagesInWindow) {
						if (page.findEditor(editor.getEditorInput()) != null) {
							tempObj[0] = page;
							break;
						}
					}
				}
			}
		});

		return (IWorkbenchPage) tempObj[0];
	}

	/**
	 * Open a web browser editor to display the given URL. If there is already
	 * an opened Web Browser Editor for the given URL, it is activated and a new
	 * one is NOT opened.
	 * 
	 * @param wantedUrl
	 *            URL to be opened.
	 * @return the opened Web Browser Editor
	 */
	public static IEditorReference openedWebEditor(IWorkbenchPage page, URL wantedUrl) {
		IEditorReference wantedWebEditor = null;

		if (page != null) {
			for (IEditorReference editor : page.getEditorReferences()) {
				if (WebBrowserEditor.WEB_BROWSER_EDITOR_ID.equals(editor.getId())) {
					try {
						WebBrowserEditorInput webEditorInput = (WebBrowserEditorInput) editor.getEditorInput();
						URL openedURL = webEditorInput.getURL();
						if ((openedURL != null) && openedURL.equals(wantedUrl)) {
							wantedWebEditor = editor;
							break;
						}
					} catch (Exception e) {
						StudioLogger.error(EclipseUtils.class, "Failed to get URL displayed by the opened Web Editor");
					}
				}
			}
		}

		if (wantedWebEditor != null) {
			StudioLogger.debug(EclipseUtils.class,
					"There is already an opened Web Browser Editor displaying the wanted URL. Simply activate it.");
			page.activate(wantedWebEditor.getEditor(true));
		} else {
			StudioLogger.debug(EclipseUtils.class, "Open new Web Browser Editor for: " + wantedUrl);
			WebBrowserEditorInput input = new WebBrowserEditorInput(wantedUrl);

			WebBrowserEditor.open(input);
		}

		return wantedWebEditor;
	}

	/**
	 * Retrieves the install location on the filesystem based on the given
	 * plug-in identifier
	 * 
	 * @param identifier
	 *            the plug-in id.
	 * @return A string containing the install path for the bundle with id -
	 *         identifier.
	 */
	public static String getInstallLocation(String identifier) {
		return getInstallLocation(Platform.getBundle(identifier));
	}

	/**
	 * Retrieves the install location for the given bundle.
	 * 
	 * @param bundle
	 * @return the bundle install location.
	 */
	public static String getInstallLocation(Bundle bundle) {
		String installLocation = "";
		try {
			URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
			URL fileUrl = FileLocator.toFileURL(locationUrl);
			installLocation = (new File(fileUrl.getFile())).getAbsolutePath();
		} catch (Exception e) {
			StudioLogger.error(EclipseUtils.class,
					"Error finding install location for bundle: " + bundle.getBundleId(), e);
		}
		return installLocation;
	}

	/**
	 * Open the preference page with the specified ID
	 * 
	 * @param nodeID
	 *            the id of preference page to show
	 */
	@SuppressWarnings("unchecked")
	public static void openPreference(Shell shell, String nodeID) {
		// Makes the network preferences dialog manager
		PreferenceManager manager = PlatformUI.getWorkbench().getPreferenceManager();
		IPreferenceNode networkNode = null;
		for (IPreferenceNode node : manager.getElements(PreferenceManager.PRE_ORDER)) {
			if (node.getId().equals(nodeID)) {
				networkNode = node;
				break;
			}
		}
		PreferenceManager prefMan = new PreferenceManager();
		if (networkNode != null) {
			prefMan.addToRoot(networkNode);
		}
		PreferenceDialog preferencesDialog = new WorkbenchPreferenceDialog(shell, prefMan);
		preferencesDialog.create();
		preferencesDialog.open();
	}

	/**
	 * Convenience method to open the preferences dialog with the secure storage
	 * preference page
	 */
	public static void openSecureStoragePreferences(Shell shell) {
		openPreference(shell, ORG_ECLIPSE_EQUINOX_SECURE_STORAGE_PREFERENCES);
	}

	/**
	 * Convenience method to open the preferences dialog with the network
	 * preferences preference page
	 */
	public static void openNetworkPreferences(Shell shell) {
		openPreference(shell, ORG_ECLIPSE_UI_NET_NET_PREFERENCES);
	}

	/**
	 * Looks for the most severe {@link Status} within a {@link MultiStatus}.
	 * 
	 * @param errorStatus
	 *            .
	 * @return the most severe status of them all.
	 */
	public static IStatus findMostSevereError(final MultiStatus errorStatus) {
		IStatus mostSevere = null;
		if (!errorStatus.isOK()) {
			for (IStatus status : errorStatus.getChildren()) {
				if (mostSevere == null) {
					mostSevere = status;
				}
				if (status.getSeverity() > mostSevere.getSeverity()) {
					mostSevere = status;
				}
			}
		}
		return mostSevere;
	}

	/**
	 * Reads a resource located inside the plugin, such as a template file.
	 * 
	 * @param resourcePath
	 *            - The path to the resource.
	 * @return An array of bytes from the resource
	 * @throws IOException
	 */
	public static String readEmbeddedResource(Bundle bundle, String resourcePath) {

		InputStream is = null;
		BufferedReader bufferedReader = null;
		String embeddedResourcePath = null;
		try {

			URL url = bundle.getEntry((new StringBuilder("/")).append(resourcePath).toString());
			if (url != null) {
				is = url.openStream();
			}

			if (is != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(is));
				StringBuilder result = new StringBuilder(bufferedReader.readLine());
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					result.append('\n');
					result.append(line);
				}
				embeddedResourcePath = result.toString();
			}
		} catch (IOException ioEx) {

			StudioLogger.error(CommonPlugin.class, "Error while reading an embedded resource", ioEx); //$NON-NLS-1$

		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					// Do nothing.
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// Do nothing.
				}
			}
		}

		return embeddedResourcePath;
	}

	/**
	 * Gets the default package from project.
	 * 
	 * @param javaProject
	 * @return the project's default package.
	 * @throws JavaModelException
	 */
	public static IPackageFragment getDefaultPackageFragment(IJavaProject javaProject) throws JavaModelException {
		IPackageFragment pack = null;
		AndroidManifestFile manifest = null;

		if ((javaProject != null) && javaProject.isOpen()) {
			// First, tries to get the default package from the
			// AndroidManifest.xml file
			try {
				manifest = AndroidProjectManifestFile.getFromProject(javaProject.getProject());
			} catch (AndroidException e) {
				// Do nothing
			} catch (CoreException e) {
				// Do nothing
			}

			if (manifest != null) {
				String defaultPackage = manifest.getManifestNode().getPackageName();

				if ((defaultPackage != null) && (defaultPackage.trim().length() > 0)) {
					IPackageFragment[] allPacks = javaProject.getPackageFragments();

					if (allPacks != null) {
						for (IPackageFragment frag : allPacks) {
							if (frag.getElementName().equals(defaultPackage)) {
								pack = frag;
								break;
							}
						}
					}
				}
			}

			// If the default package could not get from the AndroidManifest.xml
			// file, search for
			// one in the project
			if (pack == null) {
				IPackageFragment[] packs = javaProject.getPackageFragments();
				if (packs != null) {
					for (int i = 0; (i < packs.length) && (pack == null); i++) {
						if (packs[i].getKind() != IPackageFragmentRoot.K_BINARY) {
							if (!isInsideGenFolder(packs[i]) && !packs[i].isDefaultPackage()
									&& packs[i].getElementName().contains(".") && packs[i].exists()) //$NON-NLS-1$
							{
								pack = packs[i];
								break;
							}
						}
					}
				}
			}
		}

		return pack;
	}

	/**
	 * Checks if a package fragment is inside the "gen" folder
	 * 
	 * @param fragment
	 *            The package fragment to be checked
	 * @return true if the package fragment is inside the "gen" folder or false
	 *         otherwise
	 */
	private static boolean isInsideGenFolder(IPackageFragment fragment) {
		boolean isInside = (fragment.getParent() instanceof IPackageFragmentRoot)
				&& fragment.getParent().getElementName().equals(IAndroidConstants.GEN_SRC_FOLDER);

		return isInside;
	}

	/**
	 * This method adds a list of paths to all projects classpaths settings.
	 * 
	 * @param javaProjects
	 *            List of projects that will have the classpath changed
	 * @param libsPaths
	 *            List of lib paths to be added to Projects' classpaths
	 * @param monitor
	 *            Monitor to track progress or null if it's not necessary.
	 * @return IStatus The status of the operation. This method stops processing
	 *         at the first error found.
	 */
	public static IStatus addLibsToProjects(List<IJavaProject> javaProjects, List<IPath> libsPaths,
			IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(UtilitiesNLS.ProjectUtils_AddLibsProgress_ConfiguringClassPaths,
				((javaProjects.size() * 2) + libsPaths.size()) * 1000);
		IStatus status = Status.OK_STATUS;
		IClasspathEntry[] classPathEntries = new IClasspathEntry[libsPaths.size()];
		int i = 0;
		subMonitor.subTask(UtilitiesNLS.ProjectUtils_AddLibsProgress_PreparingPaths);
		for (IPath libPath : libsPaths) {
			IClasspathEntry classpathEntry = JavaCore.newLibraryEntry(libPath, null, null);
			classPathEntries[i] = classpathEntry;
			i++;
			subMonitor.worked(1000);
		}

		subMonitor.subTask(UtilitiesNLS.ProjectUtils_AddLibsProgress_ConfiguringProjects);
		for (IJavaProject javaProject : javaProjects) {
			IClasspathEntry[] rawClasspath;
			try {
				rawClasspath = javaProject.getRawClasspath();
				int length = rawClasspath.length;
				int newEntriesLength = classPathEntries.length;
				int newLenght = length + newEntriesLength;
				IClasspathEntry[] newClassPath = new IClasspathEntry[newLenght];

				System.arraycopy(rawClasspath, 0, newClassPath, 0, length); // Copy
																			// the
																			// existent
																			// classPath
																			// to
																			// the
																			// new
																			// array.
				System.arraycopy(classPathEntries, 0, newClassPath, length, newEntriesLength); // Copy
																								// the
																								// new
																								// entries
																								// to
																								// the
																								// new
																								// array
				subMonitor.worked(1000);
				javaProject.setRawClasspath(newClassPath, subMonitor.newChild(1000)); // Set
																						// the
																						// Project's
																						// classpath.
			} catch (JavaModelException e) {
				status = new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID,
						UtilitiesNLS.ProjectUtils_AddLibsProgress_ErrorSettingClasspaths, e);
				break;
			}
		}
		subMonitor.done();
		return status;
	}

	/**
	 * Verifies if a given libPath is already available on the project
	 * classpath.
	 * 
	 * @param javaProject
	 * @param libPath
	 * @return true if present, false otherwise
	 */
	public static boolean isLibOnClasspath(IJavaProject javaProject, IPath libPath) {
		try {
			IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
			for (IClasspathEntry classpathEntry : rawClasspath) {
				if (classpathEntry.getPath().equals(libPath)) {
					return true;
				}
			}
		} catch (JavaModelException e) {
			return false;
		}
		return false;
	}

	/**
	 * Add strings and array to string.xml file
	 * 
	 * @param project
	 * @param strings
	 *            string entries to add
	 * @param arrays
	 *            array entries to add
	 * @param monitor
	 *            array entries to add
	 * @throws CoreException
	 * @throws IOException
	 */
	public static void createOrUpdateDictionaryFile(IProject project, Map<String, String> strings,
			Map<String, List<String>> arrays, IProgressMonitor monitor) throws IOException, CoreException {
		List<StringNode> stringNodes = new ArrayList<StringNode>();
		List<StringArrayNode> arrayNodes = new ArrayList<StringArrayNode>();

		if (strings != null) {
			Set<String> stringSet = strings.keySet();
			for (String key : stringSet) {
				String strValue = strings.get(key);
				stringNodes.add(new StringNode(key, strValue));
			}
		}
		if (arrays != null) {
			Set<String> arraySet = arrays.keySet();
			for (String key : arraySet) {
				List<String> arrayValues = arrays.get(key);
				StringArrayNode strArray = new StringArrayNode(key);
				for (String value : arrayValues) {
					strArray.addValue(value);
				}
				arrayNodes.add(strArray);
			}
		}
		createOrUpdateDictionaryFile(project, stringNodes, arrayNodes, monitor);
		return;
	}

	/**
	 * Add strings and array to string.xml file
	 * 
	 * @param project
	 * @param strings
	 *            string entries to add
	 * @param arrays
	 *            array entries to add
	 * @param monitor
	 *            array entries to add
	 * @throws CoreException
	 * @throws IOException
	 */
	public static void createOrUpdateDictionaryFile(IProject project, List<StringNode> strings,
			List<StringArrayNode> arrays, IProgressMonitor monitor) throws IOException, CoreException {
		int taskSize = strings != null ? strings.size() : 0;
		taskSize += arrays != null ? arrays.size() : 0;

		monitor.beginTask(UtilitiesNLS.UI_ProjectCreationSupport_Creating_Strings_Task, (taskSize * 100) + 100);

		IFile projectStringXmlFile = project.getFile(IAndroidConstants.RES_DIR + IAndroidConstants.VALUES_DIR
				+ IAndroidConstants.STRINGS_FILE);

		LocalizationFile locFile = null;

		if (projectStringXmlFile.exists()) {

			ProjectLocalizationManager projManager = LocalizationManager.getInstance().getProjectLocalizationManager(
					project, true);

			// load localization file
			locFile = projManager.getProjectLocalizationSchema().loadFile(LOCALIZATION_FILE_TYPE, projectStringXmlFile);
			if (locFile.getLocalizationProject() == null) {
				locFile.setLocalizationProject(projManager.getLocalizationProject());
			}

			// add new string nodes
			for (StringNode strNode : strings) {
				((StringLocalizationFile) locFile).addStringNode(strNode);
			}
			List<StringArrayNode> currentArrays = ((StringLocalizationFile) locFile).getStringArrays();
			List<StringArrayNode> newArrays = new ArrayList<StringArrayNode>();
			newArrays.addAll(currentArrays);

			// add new array nodes
			for (StringArrayNode strArray : arrays) {
				newArrays.add(strArray);
			}
			((StringLocalizationFile) locFile).setStringArrayNodes(newArrays);

			// update file
			LocalizationManager.getInstance().getLocalizationSchema(project).updateFile(locFile);
		} else {
			ILocalizationSchema locSchema = LocalizationManager.getInstance().getLocalizationSchema(project);

			LocalizationFileBean bean = new LocalizationFileBean(LOCALIZATION_FILE_TYPE, projectStringXmlFile,
					new LocaleInfo(), strings, arrays);

			locFile = locSchema.createLocalizationFile(bean);

			locSchema.createLocalizationFile(locFile);

		}
	}

	/**
	 * Retrieves the Studio console {@link IOConsoleOutputStream}. The console
	 * with name: STUDIO_ANDROID_CONSOLE_ID
	 * 
	 * @param activate
	 *            boolean stating whether the console must be activated or not,
	 *            brought to front.
	 * @return the {@link IOConsoleOutputStream} for the Studio console.
	 */
	public static IOConsoleOutputStream getStudioConsoleOutputStream(boolean activate) {
		IConsole activeConsole = null;
		IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
		for (IConsole console : consoles) {
			if (console.getName().equals(STUDIO_ANDROID_CONSOLE_ID)) {
				activeConsole = console;
			}
		}

		if (activeConsole == null) {
			activeConsole = new IOConsole(STUDIO_ANDROID_CONSOLE_ID, null);
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { activeConsole });
		}
		if (activate) {
			((IOConsole) activeConsole).activate();
		}
		IOConsoleOutputStream consoleOut = ((IOConsole) activeConsole).newOutputStream();
		return consoleOut;
	}

}
