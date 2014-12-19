/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.internal.wizards.newxmlfile;

import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.HORIZONTAL_SCROLL_VIEW;
import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.RES_QUALIFIER_SEP;
import static com.android.SdkConstants.SCROLL_VIEW;
import static com.android.SdkConstants.VALUE_FILL_PARENT;
import static com.android.SdkConstants.VALUE_MATCH_PARENT;
import static org.eclipse.andmore.AdtConstants.WS_SEP_CHAR;
import static org.eclipse.andmore.internal.wizards.newxmlfile.ChooseConfigurationPage.RES_FOLDER_ABS;

import com.android.SdkConstants;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.ResourceQualifier;
import com.android.resources.ResourceFolderType;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.Pair;
import com.android.utils.SdkUtils;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.descriptors.DocumentDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IDescriptorProvider;
import org.eclipse.andmore.internal.project.ProjectChooserHelper;
import org.eclipse.andmore.internal.project.ProjectChooserHelper.ProjectCombo;
import org.eclipse.andmore.internal.resources.ResourceNameValidator;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.sdk.Sdk.TargetChangeListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * This is the first page of the {@link NewXmlFileWizard} which provides the ability to create
 * skeleton XML resources files for Android projects.
 * <p/>
 * This page is used to select the project, resource type and file name.
 */
class NewXmlFileCreationPage extends WizardPage {

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        // Ensure the initial focus is in the Name field; you usually don't need
        // to edit the default text field (the project name)
        if (visible && mFileNameTextField != null) {
            mFileNameTextField.setFocus();
        }

        validatePage();
    }

    /**
     * Information on one type of resource that can be created (e.g. menu, pref, layout, etc.)
     */
    static class TypeInfo {
        private final String mUiName;
        private final ResourceFolderType mResFolderType;
        private final String mTooltip;
        private final Object mRootSeed;
        private ArrayList<String> mRoots = new ArrayList<String>();
        private final String mXmlns;
        private final String mDefaultAttrs;
        private final String mDefaultRoot;
        private final int mTargetApiLevel;

        public TypeInfo(String uiName,
                        String tooltip,
                        ResourceFolderType resFolderType,
                        Object rootSeed,
                        String defaultRoot,
                        String xmlns,
                        String defaultAttrs,
                        int targetApiLevel) {
            mUiName = uiName;
            mResFolderType = resFolderType;
            mTooltip = tooltip;
            mRootSeed = rootSeed;
            mDefaultRoot = defaultRoot;
            mXmlns = xmlns;
            mDefaultAttrs = defaultAttrs;
            mTargetApiLevel = targetApiLevel;
        }

        /** Returns the UI name for the resource type. Unique. Never null. */
        String getUiName() {
            return mUiName;
        }

        /** Returns the tooltip for the resource type. Can be null. */
        String getTooltip() {
            return mTooltip;
        }

        /**
         * Returns the name of the {@link ResourceFolderType}.
         * Never null but not necessarily unique,
         * e.g. two types use  {@link ResourceFolderType#XML}.
         */
        String getResFolderName() {
            return mResFolderType.getName();
        }

        /**
         * Returns the matching {@link ResourceFolderType}.
         * Never null but not necessarily unique,
         * e.g. two types use  {@link ResourceFolderType#XML}.
         */
        ResourceFolderType getResFolderType() {
            return mResFolderType;
        }

        /**
         * Returns the seed used to fill the root element values.
         * The seed might be either a String, a String array, an {@link ElementDescriptor},
         * a {@link DocumentDescriptor} or null.
         */
        Object getRootSeed() {
            return mRootSeed;
        }

        /**
         * Returns the default root element that should be selected by default. Can be
         * null.
         *
         * @param project the associated project, or null if not known
         */
        String getDefaultRoot(IProject project) {
            return mDefaultRoot;
        }

        /**
         * Returns the list of all possible root elements for the resource type.
         * This can be an empty ArrayList but not null.
         * <p/>
         * TODO: the root list SHOULD depend on the currently selected project, to include
         * custom classes.
         */
        ArrayList<String> getRoots() {
            return mRoots;
        }

        /**
         * If the generated resource XML file requires an "android" XMLNS, this should be set
         * to {@link SdkConstants#NS_RESOURCES}. When it is null, no XMLNS is generated.
         */
        String getXmlns() {
            return mXmlns;
        }

        /**
         * When not null, this represent extra attributes that must be specified in the
         * root element of the generated XML file. When null, no extra attributes are inserted.
         *
         * @param project the project to get the attributes for
         * @param root the selected root element string, never null
         */
        String getDefaultAttrs(IProject project, String root) {
            return mDefaultAttrs;
        }

        /**
         * When not null, represents an extra string that should be written inside
         * the element when constructed
         *
         * @param project the project to get the child content for
         * @param root the chosen root element
         * @return a string to be written inside the root element, or null if nothing
         */
        String getChild(IProject project, String root) {
            return null;
        }

        /**
         * The minimum API level required by the current SDK target to support this feature.
         *
         * @return the minimum API level
         */
        public int getTargetApiLevel() {
            return mTargetApiLevel;
        }
    }

    /**
     * TypeInfo, information for each "type" of file that can be created.
     */
    private static final TypeInfo[] sTypes = {
        new TypeInfo(
                "Layout",                                                   // UI name
                "An XML file that describes a screen layout.",              // tooltip
                ResourceFolderType.LAYOUT,                                  // folder type
                AndroidTargetData.DESCRIPTOR_LAYOUT,                        // root seed
                LINEAR_LAYOUT,                                              // default root
                SdkConstants.NS_RESOURCES,                                  // xmlns
                "",                                                         // not used, see below
                1                                                           // target API level
                ) {

                @Override
                String getDefaultRoot(IProject project) {
                    // TODO: Use GridLayout by default for new SDKs
                    // (when we've ironed out all the usability issues)
                    //Sdk currentSdk = Sdk.getCurrent();
                    //if (project != null && currentSdk != null) {
                    //    IAndroidTarget target = currentSdk.getTarget(project);
                    //    // fill_parent was renamed match_parent in API level 8
                    //    if (target != null && target.getVersion().getApiLevel() >= 13) {
                    //        return GRID_LAYOUT;
                    //    }
                    //}

                    return LINEAR_LAYOUT;
                };

                // The default attributes must be determined dynamically since whether
                // we use match_parent or fill_parent depends on the API level of the
                // project
                @Override
                String getDefaultAttrs(IProject project, String root) {
                    Sdk currentSdk = Sdk.getCurrent();
                    String fill = VALUE_FILL_PARENT;
                    if (currentSdk != null) {
                        IAndroidTarget target = currentSdk.getTarget(project);
                        // fill_parent was renamed match_parent in API level 8
                        if (target != null && target.getVersion().getApiLevel() >= 8) {
                            fill = VALUE_MATCH_PARENT;
                        }
                    }

                    // Only set "vertical" orientation of LinearLayouts by default;
                    // for GridLayouts for example we want to rely on the real default
                    // of the layout
                    String size = String.format(
                            "android:layout_width=\"%1$s\"\n"        //$NON-NLS-1$
                            + "android:layout_height=\"%2$s\"",        //$NON-NLS-1$
                            fill, fill);
                    if (LINEAR_LAYOUT.equals(root)) {
                        return "android:orientation=\"vertical\"\n" + size; //$NON-NLS-1$
                    } else {
                        return size;
                    }
                }

                @Override
                String getChild(IProject project, String root) {
                    // Create vertical linear layouts inside new scroll views
                    if (SCROLL_VIEW.equals(root) || HORIZONTAL_SCROLL_VIEW.equals(root)) {
                        return "    <LinearLayout "         //$NON-NLS-1$
                            + getDefaultAttrs(project, root).replace('\n', ' ')
                            + " android:orientation=\"vertical\"" //$NON-NLS-1$
                            + "></LinearLayout>\n";         //$NON-NLS-1$
                    }
                    return null;
                }
        },
        new TypeInfo("Values",                                              // UI name
                "An XML file with simple values: colors, strings, dimensions, etc.", // tooltip
                ResourceFolderType.VALUES,                                  // folder type
                SdkConstants.TAG_RESOURCES,                                 // root seed
                null,                                                       // default root
                null,                                                       // xmlns
                null,                                                       // default attributes
                1                                                           // target API level
                ),
        new TypeInfo("Drawable",                                            // UI name
                "An XML file that describes a drawable.",                   // tooltip
                ResourceFolderType.DRAWABLE,                                // folder type
                AndroidTargetData.DESCRIPTOR_DRAWABLE,                      // root seed
                null,                                                       // default root
                SdkConstants.NS_RESOURCES,                                  // xmlns
                null,                                                       // default attributes
                1                                                           // target API level
                ),
        new TypeInfo("Menu",                                                // UI name
                "An XML file that describes an menu.",                      // tooltip
                ResourceFolderType.MENU,                                    // folder type
                SdkConstants.TAG_MENU,                                      // root seed
                null,                                                       // default root
                SdkConstants.NS_RESOURCES,                                  // xmlns
                null,                                                       // default attributes
                1                                                           // target API level
                ),
        new TypeInfo("Color List",                                          // UI name
                "An XML file that describes a color state list.",           // tooltip
                ResourceFolderType.COLOR,                                   // folder type
                AndroidTargetData.DESCRIPTOR_COLOR,                         // root seed
                "selector",  //$NON-NLS-1$                                  // default root
                SdkConstants.NS_RESOURCES,                                  // xmlns
                null,                                                       // default attributes
                1                                                           // target API level
                ),
        new TypeInfo("Property Animation",                                  // UI name
                "An XML file that describes a property animation",          // tooltip
                ResourceFolderType.ANIMATOR,                                // folder type
                AndroidTargetData.DESCRIPTOR_ANIMATOR,                      // root seed
                "set", //$NON-NLS-1$                                        // default root
                SdkConstants.NS_RESOURCES,                                  // xmlns
                null,                                                       // default attributes
                11                                                          // target API level
                ),
        new TypeInfo("Tween Animation",                                     // UI name
                "An XML file that describes a tween animation.",            // tooltip
                ResourceFolderType.ANIM,                                    // folder type
                AndroidTargetData.DESCRIPTOR_ANIM,                          // root seed
                "set", //$NON-NLS-1$                                        // default root
                null,                                                       // xmlns
                null,                                                       // default attributes
                1                                                           // target API level
                ),
        new TypeInfo("AppWidget Provider",                                  // UI name
                "An XML file that describes a widget provider.",            // tooltip
                ResourceFolderType.XML,                                     // folder type
                AndroidTargetData.DESCRIPTOR_APPWIDGET_PROVIDER,            // root seed
                null,                                                       // default root
                SdkConstants.NS_RESOURCES,                                  // xmlns
                null,                                                       // default attributes
                3                                                           // target API level
                ),
        new TypeInfo("Preference",                                          // UI name
                "An XML file that describes preferences.",                  // tooltip
                ResourceFolderType.XML,                                     // folder type
                AndroidTargetData.DESCRIPTOR_PREFERENCES,                   // root seed
                SdkConstants.CLASS_NAME_PREFERENCE_SCREEN,                  // default root
                SdkConstants.NS_RESOURCES,                                  // xmlns
                null,                                                       // default attributes
                1                                                           // target API level
                ),
        new TypeInfo("Searchable",                                          // UI name
                "An XML file that describes a searchable.",                 // tooltip
                ResourceFolderType.XML,                                     // folder type
                AndroidTargetData.DESCRIPTOR_SEARCHABLE,                    // root seed
                null,                                                       // default root
                SdkConstants.NS_RESOURCES,                                  // xmlns
                null,                                                       // default attributes
                1                                                           // target API level
                ),
        // Still missing: Interpolator, Raw and Mipmap. Raw should probably never be in
        // this menu since it's not often used for creating XML files.
    };

    private NewXmlFileWizard.Values mValues;
    private ProjectCombo mProjectButton;
    private Text mFileNameTextField;
    private Combo mTypeCombo;
    private IStructuredSelection mInitialSelection;
    private ResourceFolderType mInitialFolderType;
    private boolean mInternalTypeUpdate;
    private TargetChangeListener mSdkTargetChangeListener;
    private Table mRootTable;
    private TableViewer mRootTableViewer;

    // --- UI creation ---

    /**
     * Constructs a new {@link NewXmlFileCreationPage}.
     * <p/>
     * Called by {@link NewXmlFileWizard#createMainPage}.
     */
    protected NewXmlFileCreationPage(String pageName, NewXmlFileWizard.Values values) {
        super(pageName);
        mValues = values;
        setPageComplete(false);
    }

    public void setInitialSelection(IStructuredSelection initialSelection) {
        mInitialSelection = initialSelection;
    }

    public void setInitialFolderType(ResourceFolderType initialType) {
        mInitialFolderType = initialType;
    }

    /**
     * Called by the parent Wizard to create the UI for this Wizard Page.
     *
     * {@inheritDoc}
     *
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    @SuppressWarnings("unused") // SWT constructors have side effects, they aren't unused
    public void createControl(Composite parent) {
        // This UI is maintained with WindowBuilder.

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout(2, false /*makeColumnsEqualWidth*/));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // label before type radios
        Label typeLabel = new Label(composite, SWT.NONE);
        typeLabel.setText("Resource Type:");

        mTypeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        mTypeCombo.setToolTipText("What type of resource would you like to create?");
        mTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (mInitialFolderType != null) {
            mTypeCombo.setEnabled(false);
        }
        mTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TypeInfo type = getSelectedType();
                if (type != null) {
                    onSelectType(type);
                }
            }
        });

        // separator
        Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gd2 = new GridData(GridData.GRAB_HORIZONTAL);
        gd2.horizontalAlignment = SWT.FILL;
        gd2.horizontalSpan = 2;
        separator.setLayoutData(gd2);

        // Project: [button]
        String tooltip = "The Android Project where the new resource file will be created.";
        Label projectLabel = new Label(composite, SWT.NONE);
        projectLabel.setText("Project:");
        projectLabel.setToolTipText(tooltip);

        ProjectChooserHelper helper =
                new ProjectChooserHelper(getShell(), null /* filter */);

        mProjectButton = new ProjectCombo(helper, composite, mValues.project);
        mProjectButton.setToolTipText(tooltip);
        mProjectButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mProjectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IProject project = mProjectButton.getSelectedProject();
                if (project != mValues.project) {
                    changeProject(project);
                }
            };
        });

        // Filename: [text]
        Label fileLabel = new Label(composite, SWT.NONE);
        fileLabel.setText("File:");
        fileLabel.setToolTipText("The name of the resource file to create.");

        mFileNameTextField = new Text(composite, SWT.BORDER);
        mFileNameTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mFileNameTextField.setToolTipText(tooltip);
        mFileNameTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                mValues.name = mFileNameTextField.getText();
                validatePage();
            }
        });

        // separator
        Label rootSeparator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gd = new GridData(GridData.GRAB_HORIZONTAL);
        gd.horizontalAlignment = SWT.FILL;
        gd.horizontalSpan = 2;
        rootSeparator.setLayoutData(gd);

        // Root Element:
        // [TableViewer]
        Label rootLabel = new Label(composite, SWT.NONE);
        rootLabel.setText("Root Element:");
        new Label(composite, SWT.NONE);

        mRootTableViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
        mRootTable = mRootTableViewer.getTable();
        GridData tableGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        tableGridData.heightHint = 200;
        mRootTable.setLayoutData(tableGridData);

        setControl(composite);

        // Update state the first time
        setErrorMessage(null);
        setMessage(null);

        initializeFromSelection(mInitialSelection);
        updateAvailableTypes();
        initializeFromFixedType();
        initializeRootValues();
        installTargetChangeListener();

        initialSelectType();
        validatePage();
    }

    private void initialSelectType() {
        TypeInfo[] types = (TypeInfo[]) mTypeCombo.getData();
        int typeIndex = getTypeComboIndex(mValues.type);
        if (typeIndex == -1) {
            typeIndex = 0;
        } else {
            assert mValues.type == types[typeIndex];
        }
        mTypeCombo.select(typeIndex);
        onSelectType(types[typeIndex]);
        updateRootCombo(types[typeIndex]);
    }

    private void installTargetChangeListener() {
        mSdkTargetChangeListener = new TargetChangeListener() {
            @Override
            public IProject getProject() {
                return mValues.project;
            }

            @Override
            public void reload() {
                if (mValues.project != null) {
                    changeProject(mValues.project);
                }
            }
        };

        AdtPlugin.getDefault().addTargetListener(mSdkTargetChangeListener);
    }

    @Override
    public void dispose() {

        if (mSdkTargetChangeListener != null) {
            AdtPlugin.getDefault().removeTargetListener(mSdkTargetChangeListener);
            mSdkTargetChangeListener = null;
        }

        super.dispose();
    }

    /**
     * Returns the selected root element string, if any.
     *
     * @return The selected root element string or null.
     */
    public String getRootElement() {
        int index = mRootTable.getSelectionIndex();
        if (index >= 0) {
            Object[] roots = (Object[]) mRootTableViewer.getInput();
            return roots[index].toString();
        }
        return null;
    }

    /**
     * Called by {@link NewXmlFileWizard} to initialize the page with the selection
     * received by the wizard -- typically the current user workbench selection.
     * <p/>
     * Things we expect to find out from the selection:
     * <ul>
     * <li>The project name, valid if it's an android nature.</li>
     * <li>The current folder, valid if it's a folder under /res</li>
     * <li>An existing filename, in which case the user will be asked whether to override it.</li>
     * </ul>
     * <p/>
     * The selection can also be set to a {@link Pair} of {@link IProject} and a workspace
     * resource path (where the resource path does not have to exist yet, such as res/anim/).
     *
     * @param selection The selection when the wizard was initiated.
     */
    private boolean initializeFromSelection(IStructuredSelection selection) {
        if (selection == null) {
            return false;
        }

        // Find the best match in the element list. In case there are multiple selected elements
        // select the one that provides the most information and assign them a score,
        // e.g. project=1 + folder=2 + file=4.
        IProject targetProject = null;
        String targetWsFolderPath = null;
        String targetFileName = null;
        int targetScore = 0;
        for (Object element : selection.toList()) {
            if (element instanceof IAdaptable) {
                IResource res = (IResource) ((IAdaptable) element).getAdapter(IResource.class);
                IProject project = res != null ? res.getProject() : null;

                // Is this an Android project?
                try {
                    if (project == null || !project.hasNature(AdtConstants.NATURE_DEFAULT)) {
                        continue;
                    }
                } catch (CoreException e) {
                    // checking the nature failed, ignore this resource
                    continue;
                }

                int score = 1; // we have a valid project at least

                IPath wsFolderPath = null;
                String fileName = null;
                assert res != null; // Eclipse incorrectly thinks res could be null, so tell it no
                if (res.getType() == IResource.FOLDER) {
                    wsFolderPath = res.getProjectRelativePath();
                } else if (res.getType() == IResource.FILE) {
                    if (SdkUtils.endsWithIgnoreCase(res.getName(), DOT_XML)) {
                        fileName = res.getName();
                    }
                    wsFolderPath = res.getParent().getProjectRelativePath();
                }

                // Disregard this folder selection if it doesn't point to /res/something
                if (wsFolderPath != null &&
                        wsFolderPath.segmentCount() > 1 &&
                        SdkConstants.FD_RESOURCES.equals(wsFolderPath.segment(0))) {
                    score += 2;
                } else {
                    wsFolderPath = null;
                    fileName = null;
                }

                score += fileName != null ? 4 : 0;

                if (score > targetScore) {
                    targetScore = score;
                    targetProject = project;
                    targetWsFolderPath = wsFolderPath != null ? wsFolderPath.toString() : null;
                    targetFileName = fileName;
                }
            } else if (element instanceof Pair<?,?>) {
                // Pair of Project/String
                @SuppressWarnings("unchecked")
                Pair<IProject,String> pair = (Pair<IProject,String>)element;
                targetScore = 1;
                targetProject = pair.getFirst();
                targetWsFolderPath = pair.getSecond();
                targetFileName = "";
            }
        }

        if (targetProject == null) {
            // Try to figure out the project from the active editor
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    IEditorPart activeEditor = page.getActiveEditor();
                    if (activeEditor instanceof AndroidXmlEditor) {
                        Object input = ((AndroidXmlEditor) activeEditor).getEditorInput();
                        if (input instanceof FileEditorInput) {
                            FileEditorInput fileInput = (FileEditorInput) input;
                            targetScore = 1;
                            IFile file = fileInput.getFile();
                            targetProject = file.getProject();
                            IPath path = file.getParent().getProjectRelativePath();
                            targetWsFolderPath = path != null ? path.toString() : null;
                        }
                    }
                }
            }
        }

        if (targetProject == null) {
            // If we didn't find a default project based on the selection, check how many
            // open Android projects we can find in the current workspace. If there's only
            // one, we'll just select it by default.
            IJavaProject[] projects = AdtUtils.getOpenAndroidProjects();
            if (projects != null && projects.length == 1) {
                targetScore = 1;
                targetProject = projects[0].getProject();
            }
        }

        // Now set the UI accordingly
        if (targetScore > 0) {
            mValues.project = targetProject;
            mValues.folderPath = targetWsFolderPath;
            mProjectButton.setSelectedProject(targetProject);
            mFileNameTextField.setText(targetFileName != null ? targetFileName : ""); //$NON-NLS-1$

            // If the current selection context corresponds to a specific file type,
            // select it.
            if (targetWsFolderPath != null) {
                int pos = targetWsFolderPath.lastIndexOf(WS_SEP_CHAR);
                if (pos >= 0) {
                    targetWsFolderPath = targetWsFolderPath.substring(pos + 1);
                }
                String[] folderSegments = targetWsFolderPath.split(RES_QUALIFIER_SEP);
                if (folderSegments.length > 0) {
                    mValues.configuration = FolderConfiguration.getConfig(folderSegments);
                    String folderName = folderSegments[0];
                    selectTypeFromFolder(folderName);
                }
            }
        }

        return true;
    }

    private void initializeFromFixedType() {
        if (mInitialFolderType != null) {
            for (TypeInfo type : sTypes) {
                if (type.getResFolderType() == mInitialFolderType) {
                    mValues.type = type;
                    updateFolderPath(type);
                    break;
                }
            }
        }
    }

    /**
     * Given a folder name, such as "drawable", select the corresponding type in
     * the dropdown.
     */
    void selectTypeFromFolder(String folderName) {
        List<TypeInfo> matches = new ArrayList<TypeInfo>();
        boolean selected = false;

        TypeInfo selectedType = getSelectedType();
        for (TypeInfo type : sTypes) {
            if (type.getResFolderName().equals(folderName)) {
                matches.add(type);
                selected |= type == selectedType;
            }
        }

        if (matches.size() == 1) {
            // If there's only one match, select it if it's not already selected
            if (!selected) {
                selectType(matches.get(0));
            }
        } else if (matches.size() > 1) {
            // There are multiple type candidates for this folder. This can happen
            // for /res/xml for example. Check to see if one of them is currently
            // selected. If yes, leave the selection unchanged. If not, deselect all type.
            if (!selected) {
                selectType(null);
            }
        } else {
            // Nothing valid was selected.
            selectType(null);
        }
    }

    /**
     * Initialize the root values of the type infos based on the current framework values.
     */
    private void initializeRootValues() {
        IProject project = mValues.project;
        for (TypeInfo type : sTypes) {
            // Clear all the roots for this type
            ArrayList<String> roots = type.getRoots();
            if (roots.size() > 0) {
                roots.clear();
            }

            // depending of the type of the seed, initialize the root in different ways
            Object rootSeed = type.getRootSeed();

            if (rootSeed instanceof String) {
                // The seed is a single string, Add it as-is.
                roots.add((String) rootSeed);
            } else if (rootSeed instanceof String[]) {
                // The seed is an array of strings. Add them as-is.
                for (String value : (String[]) rootSeed) {
                    roots.add(value);
                }
            } else if (rootSeed instanceof Integer && project != null) {
                // The seed is a descriptor reference defined in AndroidTargetData.DESCRIPTOR_*
                // In this case add all the children element descriptors defined, recursively,
                // and avoid infinite recursion by keeping track of what has already been added.

                // Note: if project is null, the root list will be empty since it has been
                // cleared above.

                // get the AndroidTargetData from the project
                IAndroidTarget target = null;
                AndroidTargetData data = null;

                target = Sdk.getCurrent().getTarget(project);
                if (target == null) {
                    // A project should have a target. The target can be missing if the project
                    // is an old project for which a target hasn't been affected or if the
                    // target no longer exists in this SDK. Simply log the error and dismiss.

                    AdtPlugin.log(IStatus.INFO,
                            "NewXmlFile wizard: no platform target for project %s",  //$NON-NLS-1$
                            project.getName());
                    continue;
                } else {
                    data = Sdk.getCurrent().getTargetData(target);

                    if (data == null) {
                        // We should have both a target and its data.
                        // However if the wizard is invoked whilst the platform is still being
                        // loaded we can end up in a weird case where we have a target but it
                        // doesn't have any data yet.
                        // Lets log a warning and silently ignore this root.

                        AdtPlugin.log(IStatus.INFO,
                              "NewXmlFile wizard: no data for target %s, project %s",  //$NON-NLS-1$
                              target.getName(), project.getName());
                        continue;
                    }
                }

                IDescriptorProvider provider = data.getDescriptorProvider((Integer)rootSeed);
                ElementDescriptor descriptor = provider.getDescriptor();
                if (descriptor != null) {
                    HashSet<ElementDescriptor> visited = new HashSet<ElementDescriptor>();
                    initRootElementDescriptor(roots, descriptor, visited);
                }

                // Sort alphabetically.
                Collections.sort(roots);
            }
        }
    }

    /**
     * Helper method to recursively insert all XML names for the given {@link ElementDescriptor}
     * into the roots array list. Keeps track of visited nodes to avoid infinite recursion.
     * Also avoids inserting the top {@link DocumentDescriptor} which is generally synthetic
     * and not a valid root element.
     */
    private void initRootElementDescriptor(ArrayList<String> roots,
            ElementDescriptor desc, HashSet<ElementDescriptor> visited) {
        if (!(desc instanceof DocumentDescriptor)) {
            String xmlName = desc.getXmlName();
            if (xmlName != null && xmlName.length() > 0) {
                roots.add(xmlName);
            }
        }

        visited.add(desc);

        for (ElementDescriptor child : desc.getChildren()) {
            if (!visited.contains(child)) {
                initRootElementDescriptor(roots, child, visited);
            }
        }
    }

    /**
     * Changes mProject to the given new project and update the UI accordingly.
     * <p/>
     * Note that this does not check if the new project is the same as the current one
     * on purpose, which allows a project to be updated when its target has changed or
     * when targets are loaded in the background.
     */
    private void changeProject(IProject newProject) {
        mValues.project = newProject;

        // enable types based on new API level
        updateAvailableTypes();
        initialSelectType();

        // update the folder name based on API level
        updateFolderPath(mValues.type);

        // update the Type with the new descriptors.
        initializeRootValues();

        // update the combo
        updateRootCombo(mValues.type);

        validatePage();
    }

    private void onSelectType(TypeInfo type) {
        // Do nothing if this is an internal modification or if the widget has been
        // deselected.
        if (mInternalTypeUpdate) {
            return;
        }

        mValues.type = type;

        if (type == null) {
            return;
        }

        // update the combo
        updateRootCombo(type);

        // update the folder path
        updateFolderPath(type);

        validatePage();
    }

    /** Updates the selected type in the type dropdown control */
    private void setSelectedType(TypeInfo type) {
        TypeInfo[] types = (TypeInfo[]) mTypeCombo.getData();
        if (types != null) {
            for (int i = 0, n = types.length; i < n; i++) {
                if (types[i] == type) {
                    mTypeCombo.select(i);
                    break;
                }
            }
        }
    }

    /** Returns the selected type in the type dropdown control */
    private TypeInfo getSelectedType() {
        int index = mTypeCombo.getSelectionIndex();
        if (index != -1) {
            TypeInfo[] types = (TypeInfo[]) mTypeCombo.getData();
            return types[index];
        }

        return null;
    }

    /** Returns the selected index in the type dropdown control */
    private int getTypeComboIndex(TypeInfo type) {
        TypeInfo[] types = (TypeInfo[]) mTypeCombo.getData();
        for (int i = 0, n = types.length; i < n; i++) {
            if (type == types[i]) {
                return i;
            }
        }

        return -1;
    }

    /** Updates the folder path to reflect the given type */
    private void updateFolderPath(TypeInfo type) {
        String wsFolderPath = mValues.folderPath;
        String newPath = null;
        FolderConfiguration config = mValues.configuration;
        ResourceQualifier qual = config.getInvalidQualifier();
        if (qual == null) {
            // The configuration is valid. Reformat the folder path using the canonical
            // value from the configuration.
            newPath = RES_FOLDER_ABS + config.getFolderName(type.getResFolderType());
        } else {
            // The configuration is invalid. We still update the path but this time
            // do it manually on the string.
            if (wsFolderPath.startsWith(RES_FOLDER_ABS)) {
                wsFolderPath = wsFolderPath.replaceFirst(
                        "^(" + RES_FOLDER_ABS +")[^-]*(.*)",         //$NON-NLS-1$ //$NON-NLS-2$
                        "\\1" + type.getResFolderName() + "\\2");    //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                newPath = RES_FOLDER_ABS + config.getFolderName(type.getResFolderType());
            }
        }

        if (newPath != null && !newPath.equals(wsFolderPath)) {
            mValues.folderPath = newPath;
        }
    }

    /**
     * Helper method that fills the values of the "root element" combo box based
     * on the currently selected type radio button. Also disables the combo is there's
     * only one choice. Always select the first root element for the given type.
     *
     * @param type The currently selected {@link TypeInfo}, or null
     */
    private void updateRootCombo(TypeInfo type) {
        IBaseLabelProvider labelProvider = new ColumnLabelProvider() {
            @Override
            public Image getImage(Object element) {
                return IconFactory.getInstance().getIcon(element.toString());
            }
        };
        mRootTableViewer.setContentProvider(new ArrayContentProvider());
        mRootTableViewer.setLabelProvider(labelProvider);

        if (type != null) {
            // get the list of roots. The list can be empty but not null.
            ArrayList<String> roots = type.getRoots();
            mRootTableViewer.setInput(roots.toArray());

            int index = 0; // default is to select the first one
            String defaultRoot = type.getDefaultRoot(mValues.project);
            if (defaultRoot != null) {
                index = roots.indexOf(defaultRoot);
            }
            mRootTable.select(index < 0 ? 0 : index);
            mRootTable.showSelection();
        }
    }

    /**
     * Helper method to select the current type in the type dropdown
     *
     * @param type The TypeInfo matching the radio button to selected or null to deselect them all.
     */
    private void selectType(TypeInfo type) {
        mInternalTypeUpdate = true;
        mValues.type = type;
        if (type == null) {
            if (mTypeCombo.getSelectionIndex() != -1) {
                mTypeCombo.deselect(mTypeCombo.getSelectionIndex());
            }
        } else {
            setSelectedType(type);
        }
        updateRootCombo(type);
        mInternalTypeUpdate = false;
    }

    /**
     * Add the available types in the type combobox, based on whether they are available
     * for the current SDK.
     * <p/>
     * A type is available either if:
     * - if mProject is null, API level 1 is considered valid
     * - if mProject is !null, the project->target->API must be >= to the type's API level.
     */
    private void updateAvailableTypes() {
        IProject project = mValues.project;
        IAndroidTarget target = project != null ? Sdk.getCurrent().getTarget(project) : null;
        int currentApiLevel = 1;
        if (target != null) {
            currentApiLevel = target.getVersion().getApiLevel();
        }

        List<String> items = new ArrayList<String>(sTypes.length);
        List<TypeInfo> types = new ArrayList<TypeInfo>(sTypes.length);
        for (int i = 0, n = sTypes.length; i < n; i++) {
            TypeInfo type = sTypes[i];
            if (type.getTargetApiLevel() <= currentApiLevel) {
                items.add(type.getUiName());
                types.add(type);
            }
        }
        mTypeCombo.setItems(items.toArray(new String[items.size()]));
        mTypeCombo.setData(types.toArray(new TypeInfo[types.size()]));
    }

    /**
     * Validates the fields, displays errors and warnings.
     * Enables the finish button if there are no errors.
     */
    private void validatePage() {
        String error = null;
        String warning = null;

        // -- validate type
        TypeInfo type = mValues.type;
        if (error == null) {
            if (type == null) {
                error = "One of the types must be selected (e.g. layout, values, etc.)";
            }
        }

        // -- validate project
        if (mValues.project == null) {
            error = "Please select an Android project.";
        }

        // -- validate type API level
        if (error == null) {
            IAndroidTarget target = Sdk.getCurrent().getTarget(mValues.project);
            int currentApiLevel = 1;
            if (target != null) {
                currentApiLevel = target.getVersion().getApiLevel();
            }

            assert type != null;
            if (type.getTargetApiLevel() > currentApiLevel) {
                error = "The API level of the selected type (e.g. AppWidget, etc.) is not " +
                        "compatible with the API level of the project.";
            }
        }

        // -- validate filename
        if (error == null) {
            String fileName = mValues.getFileName();
            assert type != null;
            ResourceFolderType folderType = type.getResFolderType();
            error = ResourceNameValidator.create(true, folderType).isValid(fileName);
        }

        // -- validate destination file doesn't exist
        if (error == null) {
            IFile file = mValues.getDestinationFile();
            if (file != null && file.exists()) {
                warning = "The destination file already exists";
            }
        }

        // -- update UI & enable finish if there's no error
        setPageComplete(error == null);
        if (error != null) {
            setMessage(error, IMessageProvider.ERROR);
        } else if (warning != null) {
            setMessage(warning, IMessageProvider.WARNING);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }

    /**
     * Returns the {@link TypeInfo} for the given {@link ResourceFolderType}, or null if
     * not found
     *
     * @param folderType the {@link ResourceFolderType} to look for
     * @return the corresponding {@link TypeInfo}
     */
    static TypeInfo getTypeInfo(ResourceFolderType folderType) {
        for (TypeInfo typeInfo : sTypes) {
            if (typeInfo.getResFolderType() == folderType) {
                return typeInfo;
            }
        }

        return null;
    }
}
