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

import static com.android.SdkConstants.FQCN_GRID_LAYOUT;
import static com.android.SdkConstants.GRID_LAYOUT;

import com.android.SdkConstants;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.xml.XmlFormatStyle;
import com.android.resources.ResourceFolderType;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlFormatPreferences;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlPrettyPrinter;
import org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewManager;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.project.SupportLibraryHelper;
import org.eclipse.andmore.internal.wizards.newxmlfile.NewXmlFileCreationPage.TypeInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * The "New Android XML File Wizard" provides the ability to create skeleton XML
 * resources files for Android projects.
 * <p/>
 * The wizard has one page, {@link NewXmlFileCreationPage}, used to select the project,
 * the resource folder, resource type and file name. It then creates the XML file.
 */
public class NewXmlFileWizard extends Wizard implements INewWizard {
    /** The XML header to write at the top of the XML file */
    public static final String XML_HEADER_LINE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"; //$NON-NLS-1$

    private static final String PROJECT_LOGO_LARGE = "android-64"; //$NON-NLS-1$

    protected static final String MAIN_PAGE_NAME = "newAndroidXmlFilePage"; //$NON-NLS-1$

    private NewXmlFileCreationPage mMainPage;
    private ChooseConfigurationPage mConfigPage;
    private Values mValues;

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setHelpAvailable(false); // TODO have help
        setWindowTitle("New Android XML File");
        setImageDescriptor();

        mValues = new Values();
        mMainPage = createMainPage(mValues);
        mMainPage.setTitle("New Android XML File");
        mMainPage.setDescription("Creates a new Android XML file.");
        mMainPage.setInitialSelection(selection);

        mConfigPage = new ChooseConfigurationPage(mValues);

        // Trigger a check to see if the SDK needs to be reloaded (which will
        // invoke onSdkLoaded asynchronously as needed).
        AdtPlugin.getDefault().refreshSdk();
    }

    /**
     * Creates the wizard page.
     * <p/>
     * Please do NOT override this method.
     * <p/>
     * This is protected so that it can be overridden by unit tests.
     * However the contract of this class is private and NO ATTEMPT will be made
     * to maintain compatibility between different versions of the plugin.
     */
    protected NewXmlFileCreationPage createMainPage(NewXmlFileWizard.Values values) {
        return new NewXmlFileCreationPage(MAIN_PAGE_NAME, values);
    }

    // -- Methods inherited from org.eclipse.jface.wizard.Wizard --
    //
    // The Wizard class implements most defaults and boilerplate code needed by
    // IWizard

    /**
     * Adds pages to this wizard.
     */
    @Override
    public void addPages() {
        addPage(mMainPage);
        addPage(mConfigPage);

    }

    /**
     * Performs any actions appropriate in response to the user having pressed
     * the Finish button, or refuse if finishing now is not permitted: here, it
     * actually creates the workspace project and then switch to the Java
     * perspective.
     *
     * @return True
     */
    @Override
    public boolean performFinish() {
        final Pair<IFile, IRegion> created = createXmlFile();
        if (created == null) {
            return false;
        } else {
            // Open the file
            // This has to be delayed in order for focus handling to work correctly
            AdtPlugin.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    IFile file = created.getFirst();
                    IRegion region = created.getSecond();
                    try {
                        IEditorPart editor = AdtPlugin.openFile(file, null,
                                false /*showEditorTab*/);
                        if (editor instanceof AndroidXmlEditor) {
                            final AndroidXmlEditor xmlEditor = (AndroidXmlEditor)editor;
                            if (!xmlEditor.hasMultiplePages()) {
                                xmlEditor.show(region.getOffset(), region.getLength(),
                                        true /* showEditorTab */);
                            }
                        }
                    } catch (PartInitException e) {
                        AdtPlugin.log(e, "Failed to create %1$s: missing type", //$NON-NLS-1$
                                file.getFullPath().toString());
                    }
                }});

            return true;
        }
    }

    // -- Custom Methods --

    private Pair<IFile, IRegion> createXmlFile() {
        IFile file = mValues.getDestinationFile();
        TypeInfo type = mValues.type;
        if (type == null) {
            // this is not expected to happen
            String name = file.getFullPath().toString();
            AdtPlugin.log(IStatus.ERROR, "Failed to create %1$s: missing type", name);  //$NON-NLS-1$
            return null;
        }
        String xmlns = type.getXmlns();
        String root = mMainPage.getRootElement();
        if (root == null) {
            // this is not expected to happen
            AdtPlugin.log(IStatus.ERROR, "Failed to create %1$s: missing root element", //$NON-NLS-1$
                    file.toString());
            return null;
        }

        String attrs = type.getDefaultAttrs(mValues.project, root);
        String child = type.getChild(mValues.project, root);
        return createXmlFile(file, xmlns, root, attrs, child, type.getResFolderType());
    }

    /** Creates a new file using the given root element, namespace and root attributes */
    private static Pair<IFile, IRegion> createXmlFile(IFile file, String xmlns,
            String root, String rootAttributes, String child, ResourceFolderType folderType) {
        String name = file.getFullPath().toString();
        boolean need_delete = false;

        if (file.exists()) {
            if (!AdtPlugin.displayPrompt("New Android XML File",
                String.format("Do you want to overwrite the file %1$s ?", name))) {
                // abort if user selects cancel.
                return null;
            }
            need_delete = true;
        } else {
            AdtUtils.createWsParentDirectory(file.getParent());
        }

        StringBuilder sb = new StringBuilder(XML_HEADER_LINE);

        if (folderType == ResourceFolderType.LAYOUT && root.equals(GRID_LAYOUT)) {
            IProject project = file.getParent().getProject();
            int minSdk = ManifestInfo.get(project).getMinSdkVersion();
            if (minSdk < 14) {
                root = SupportLibraryHelper.getTagFor(project, FQCN_GRID_LAYOUT);
                if (root.equals(FQCN_GRID_LAYOUT)) {
                    root = GRID_LAYOUT;
                }
            }
        }

        sb.append('<').append(root);
        if (xmlns != null) {
            sb.append('\n').append("  xmlns:android=\"").append(xmlns).append('"');  //$NON-NLS-1$
        }

        if (rootAttributes != null) {
            sb.append("\n  ");                       //$NON-NLS-1$
            sb.append(rootAttributes.replace("\n", "\n  "));  //$NON-NLS-1$ //$NON-NLS-2$
        }

        sb.append(">\n");                            //$NON-NLS-1$

        if (child != null) {
            sb.append(child);
        }

        boolean autoFormat = AdtPrefs.getPrefs().getUseCustomXmlFormatter();

        // Insert an indented caret. Since the markup here will be reformatted, we need to
        // insert text tokens that the formatter will preserve, which we can then turn back
        // into indentation and a caret offset:
        final String indentToken = "${indent}"; //$NON-NLS-1$
        final String caretToken = "${caret}";   //$NON-NLS-1$
        sb.append(indentToken);
        sb.append(caretToken);
        if (!autoFormat) {
            sb.append('\n');
        }

        sb.append("</").append(root).append(">\n");  //$NON-NLS-1$ //$NON-NLS-2$

        EclipseXmlFormatPreferences formatPrefs = EclipseXmlFormatPreferences.create();
        String fileContents;
        if (!autoFormat) {
            fileContents = sb.toString();
        } else {
            XmlFormatStyle style = EclipseXmlPrettyPrinter.getForFolderType(folderType);
            fileContents = EclipseXmlPrettyPrinter.prettyPrint(sb.toString(), formatPrefs,
                    style, null /*lineSeparator*/);
        }

        // Remove marker tokens and replace them with whitespace
        fileContents = fileContents.replace(indentToken, formatPrefs.getOneIndentUnit());
        int caretOffset = fileContents.indexOf(caretToken);
        if (caretOffset != -1) {
            fileContents = fileContents.replace(caretToken, ""); //$NON-NLS-1$
        }

        String error = null;
        try {
            byte[] buf = fileContents.getBytes("UTF8");    //$NON-NLS-1$
            InputStream stream = new ByteArrayInputStream(buf);
            if (need_delete) {
                file.delete(IResource.KEEP_HISTORY | IResource.FORCE, null /*monitor*/);
            }
            file.create(stream, true /*force*/, null /*progress*/);
            IRegion region = caretOffset != -1 ? new Region(caretOffset, 0) : null;

            // If you introduced a new locale, or new screen variations etc, ensure that
            // the list of render previews is updated if necessary
            if (file.getParent().getName().indexOf('-') != -1
                    && (folderType == ResourceFolderType.LAYOUT
                        || folderType == ResourceFolderType.VALUES)) {
                RenderPreviewManager.bumpRevision();
            }

            return Pair.of(file, region);
        } catch (UnsupportedEncodingException e) {
            error = e.getMessage();
        } catch (CoreException e) {
            error = e.getMessage();
        }

        error = String.format("Failed to generate %1$s: %2$s", name, error);
        AdtPlugin.displayError("New Android XML File", error);
        return null;
    }

    /**
     * Returns true if the New XML Wizard can create new files of the given
     * {@link ResourceFolderType}
     *
     * @param folderType the folder type to create a file for
     * @return true if this wizard can create new files for the given folder type
     */
    public static boolean canCreateXmlFile(ResourceFolderType folderType) {
        TypeInfo typeInfo = NewXmlFileCreationPage.getTypeInfo(folderType);
        return typeInfo != null && (typeInfo.getDefaultRoot(null /*project*/) != null ||
                typeInfo.getRootSeed() instanceof String);
    }

    /**
     * Creates a new XML file using the template according to the given folder type
     *
     * @param project the project to create the file in
     * @param file the file to be created
     * @param folderType the type of folder to look up a template for
     * @return the created file
     */
    public static Pair<IFile, IRegion> createXmlFile(IProject project, IFile file,
            ResourceFolderType folderType) {
        TypeInfo type = NewXmlFileCreationPage.getTypeInfo(folderType);
        String xmlns = type.getXmlns();
        String root = type.getDefaultRoot(project);
        if (root == null) {
            root = type.getRootSeed().toString();
        }
        String attrs = type.getDefaultAttrs(project, root);
        return createXmlFile(file, xmlns, root, attrs, null, folderType);
    }

    /**
     * Returns an image descriptor for the wizard logo.
     */
    private void setImageDescriptor() {
        ImageDescriptor desc = IconFactory.getInstance().getImageDescriptor(PROJECT_LOGO_LARGE);
        setDefaultPageImageDescriptor(desc);
    }

    /**
     * Specific New XML File wizard tied to the {@link ResourceFolderType#LAYOUT} type
     */
    public static class NewLayoutWizard extends NewXmlFileWizard {
        /** Creates a new {@link NewLayoutWizard} */
        public NewLayoutWizard() {
        }

        @Override
        public void init(IWorkbench workbench, IStructuredSelection selection) {
            super.init(workbench, selection);
            setWindowTitle("New Android Layout XML File");
            super.mMainPage.setTitle("New Android Layout XML File");
            super.mMainPage.setDescription("Creates a new Android Layout XML file.");
            super.mMainPage.setInitialFolderType(ResourceFolderType.LAYOUT);
        }
    }

    /**
     * Specific New XML File wizard tied to the {@link ResourceFolderType#VALUES} type
     */
    public static class NewValuesWizard extends NewXmlFileWizard {
        /** Creates a new {@link NewValuesWizard} */
        public NewValuesWizard() {
        }

        @Override
        public void init(IWorkbench workbench, IStructuredSelection selection) {
            super.init(workbench, selection);
            setWindowTitle("New Android Values XML File");
            super.mMainPage.setTitle("New Android Values XML File");
            super.mMainPage.setDescription("Creates a new Android Values XML file.");
            super.mMainPage.setInitialFolderType(ResourceFolderType.VALUES);
        }
    }

    /** Value object which holds the current state of the wizard pages */
    public static class Values {
        /** The currently selected project, or null */
        public IProject project;
        /** The root name of the XML file to create, or null */
        public String name;
        /** The type of XML file to create */
        public TypeInfo type;
        /** The path within the project to create the new file in */
        public String folderPath;
        /** The currently chosen configuration */
        public FolderConfiguration configuration = new FolderConfiguration();

        /**
         * Returns the destination filename or an empty string.
         *
         * @return the filename, never null.
         */
        public String getFileName() {
            String fileName;
            if (name == null) {
                fileName = ""; //$NON-NLS-1$
            } else {
                fileName = name.trim();
                if (fileName.length() > 0 && fileName.indexOf('.') == -1) {
                    fileName = fileName + SdkConstants.DOT_XML;
                }
            }

            return fileName;
        }

        /**
         * Returns a {@link IFile} for the destination file.
         * <p/>
         * Returns null if the project, filename or folder are invalid and the
         * destination file cannot be determined.
         * <p/>
         * The {@link IFile} is a resource. There might or might not be an
         * actual real file.
         *
         * @return an {@link IFile} for the destination file
         */
        public IFile getDestinationFile() {
            String fileName = getFileName();
            if (project != null && folderPath != null && folderPath.length() > 0
                    && fileName.length() > 0) {
                IPath dest = new Path(folderPath).append(fileName);
                IFile file = project.getFile(dest);
                return file;
            }
            return null;
        }
    }
}
