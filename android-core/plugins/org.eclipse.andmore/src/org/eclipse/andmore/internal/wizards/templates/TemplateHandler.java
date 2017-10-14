/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.wizards.templates;

import static com.android.SdkConstants.ATTR_PACKAGE;
import static com.android.SdkConstants.DOT_AIDL;
import static com.android.SdkConstants.DOT_FTL;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.DOT_RS;
import static com.android.SdkConstants.DOT_SVG;
import static com.android.SdkConstants.DOT_TXT;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.EXT_XML;
import static com.android.SdkConstants.FD_NATIVE_LIBS;
import static com.android.SdkConstants.XMLNS_PREFIX;
import static org.eclipse.andmore.internal.wizards.templates.InstallDependencyPage.SUPPORT_LIBRARY_NAME;
import static org.eclipse.andmore.internal.wizards.templates.TemplateManager.getTemplateRootFolder;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.xml.XmlFormatStyle;
import com.android.manifmerger.ManifestMerger2;
import com.android.manifmerger.ManifestMerger2.MergeFailureException;
import com.android.manifmerger.ManifestMerger2.MergeType;
import com.android.manifmerger.MergingReport;
import com.android.manifmerger.MergingReport.MergedManifestKind;
import com.android.resources.ResourceFolderType;
import com.android.utils.FileUtils;
import com.android.utils.SdkUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.actions.AddSupportJarAction;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlFormatPreferences;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlPrettyPrinter;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.SWT;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Handler which manages instantiating FreeMarker templates, copying resources
 * and merging into existing files
 */
class TemplateHandler {
    /** Highest supported format; templates with a higher number will be skipped
     * <p>
     * <ul>
     * <li> 1: Initial format, supported by ADT 20 and up.
     * <li> 2: ADT 21 and up. Boolean variables that have a default value and are not
     *    edited by the user would end up as strings in ADT 20; now they are always
     *    proper Booleans. Templates which rely on this should specify format >= 2.
     * <li> 3: The wizard infrastructure passes the {@code isNewProject} boolean variable
     *    to indicate whether a wizard is created as part of a new blank project
     * <li> 4: The templates now specify dependencies in the recipe file.
     * </ul>
     */
    static final int CURRENT_FORMAT = 4;

    /**
     * Special marker indicating that this path refers to the special shared
     * resource directory rather than being somewhere inside the root/ directory
     * where all template specific resources are found
     */
    private static final String VALUE_TEMPLATE_DIR = "$TEMPLATEDIR"; //$NON-NLS-1$

    /**
     * Directory within the template which contains the resources referenced
     * from the template.xml file
     */
    private static final String DATA_ROOT = "root";      //$NON-NLS-1$

    /**
     * Shared resource directory containing common resources shared among
     * multiple templates
     */
    private static final String RESOURCE_ROOT = "resources";   //$NON-NLS-1$

    /** Reserved filename which describes each template */
    static final String TEMPLATE_XML = "template.xml";   //$NON-NLS-1$

    // Various tags and attributes used in the template metadata files - template.xml,
    // globals.xml.ftl, recipe.xml.ftl, etc.

    static final String TAG_MERGE = "merge";             //$NON-NLS-1$
    static final String TAG_EXECUTE = "execute";         //$NON-NLS-1$
    static final String TAG_GLOBALS = "globals";         //$NON-NLS-1$
    static final String TAG_GLOBAL = "global";           //$NON-NLS-1$
    static final String TAG_PARAMETER = "parameter";     //$NON-NLS-1$
    static final String TAG_COPY = "copy";               //$NON-NLS-1$
    static final String TAG_INSTANTIATE = "instantiate"; //$NON-NLS-1$
    static final String TAG_OPEN = "open";               //$NON-NLS-1$
    static final String TAG_THUMB = "thumb";             //$NON-NLS-1$
    static final String TAG_THUMBS = "thumbs";           //$NON-NLS-1$
    static final String TAG_DEPENDENCY = "dependency";   //$NON-NLS-1$
    static final String TAG_ICONS = "icons";             //$NON-NLS-1$
    static final String TAG_FORMFACTOR = "formfactor";   //$NON-NLS-1$
    static final String TAG_CATEGORY = "category";       //$NON-NLS-1$
    static final String ATTR_FORMAT = "format";          //$NON-NLS-1$
    static final String ATTR_REVISION = "revision";      //$NON-NLS-1$
    static final String ATTR_VALUE = "value";            //$NON-NLS-1$
    static final String ATTR_DEFAULT = "default";        //$NON-NLS-1$
    static final String ATTR_SUGGEST = "suggest";        //$NON-NLS-1$
    static final String ATTR_ID = "id";                  //$NON-NLS-1$
    static final String ATTR_NAME = "name";              //$NON-NLS-1$
    static final String ATTR_DESCRIPTION = "description";//$NON-NLS-1$
    static final String ATTR_TYPE = "type";              //$NON-NLS-1$
    static final String ATTR_HELP = "help";              //$NON-NLS-1$
    static final String ATTR_FILE = "file";              //$NON-NLS-1$
    static final String ATTR_TO = "to";                  //$NON-NLS-1$
    static final String ATTR_FROM = "from";              //$NON-NLS-1$
    static final String ATTR_CONSTRAINTS = "constraints";//$NON-NLS-1$
    static final String ATTR_BACKGROUND = "background";  //$NON-NLS-1$
    static final String ATTR_FOREGROUND = "foreground";  //$NON-NLS-1$
    static final String ATTR_SHAPE = "shape";            //$NON-NLS-1$
    static final String ATTR_TRIM = "trim";              //$NON-NLS-1$
    static final String ATTR_PADDING = "padding";        //$NON-NLS-1$
    static final String ATTR_SOURCE_TYPE = "source";     //$NON-NLS-1$
    static final String ATTR_CLIPART_NAME = "clipartName";//$NON-NLS-1$
    static final String ATTR_TEXT = "text";              //$NON-NLS-1$
    static final String ATTR_SRC_DIR = "srcDir";         //$NON-NLS-1$
    static final String ATTR_SRC_OUT = "srcOut";         //$NON-NLS-1$
    static final String ATTR_RES_DIR = "resDir";         //$NON-NLS-1$
    static final String ATTR_RES_OUT = "resOut";         //$NON-NLS-1$
    static final String ATTR_MANIFEST_DIR = "manifestDir";//$NON-NLS-1$
    static final String ATTR_MANIFEST_OUT = "manifestOut";//$NON-NLS-1$
    static final String ATTR_PROJECT_DIR = "projectDir"; //$NON-NLS-1$
    static final String ATTR_PROJECT_OUT = "projectOut"; //$NON-NLS-1$
    static final String ATTR_MAVEN_URL = "mavenUrl";     //$NON-NLS-1$
    static final String ATTR_DEBUG_KEYSTORE_SHA1 = 
    		"debugKeystoreSha1";                         //$NON-NLS-1$

    static final String CATEGORY_ACTIVITIES = "activities";//$NON-NLS-1$
    static final String CATEGORY_PROJECTS = "projects";    //$NON-NLS-1$
    static final String CATEGORY_OTHER = "other";          //$NON-NLS-1$

    static final String MAVEN_SUPPORT_V4 = "support-v4";   //$NON-NLS-1$
    static final String MAVEN_SUPPORT_V13 = "support-v13"; //$NON-NLS-1$
    static final String MAVEN_APPCOMPAT = "appcompat-v7";  //$NON-NLS-1$

    /** Default padding to apply in wizards around the thumbnail preview images */
    static final int PREVIEW_PADDING = 10;

    /** Default width to scale thumbnail preview images in wizards to */
    static final int PREVIEW_WIDTH = 200;

    /**
     * List of files to open after the wizard has been created (these are
     * identified by {@link #TAG_OPEN} elements in the recipe file
     */
    private final List<String> mOpen = Lists.newArrayList();

    /**
     * List of actions to perform after the wizard has finished.
     */
    protected List<Runnable> mFinalizingActions = Lists.newArrayList();

    /** Path to the directory containing the templates */
    @NonNull
    private final File mRootPath;

    /** The changes being processed by the template handler */
    private List<Change> mMergeChanges;
    private List<Change> mTextChanges;
    private List<Change> mOtherChanges;

    /** The project to write the template into */
    private IProject mProject;

    /** The template loader which is responsible for finding (and sharing) template files */
    private final MyTemplateLoader mLoader;

    /** Agree to all file-overwrites from now on? */
    private boolean mYesToAll = false;

    /** Is writing the template cancelled? */
    private boolean mNoToAll = false;

    /**
     * Should files that we merge contents into be backed up? If yes, will
     * create emacs-style tilde-file backups (filename.xml~)
     */
    private boolean mBackupMergedFiles = true;

    /**
     * Template metadata
     */
    private TemplateMetadata mTemplate;

    private final TemplateManager mManager;

    /** Creates a new {@link TemplateHandler} for the given root path */
    static TemplateHandler createFromPath(File rootPath) {
        return new TemplateHandler(rootPath, new TemplateManager());
    }

    /** Creates a new {@link TemplateHandler} for the template name, which should
     * be relative to the templates directory */
    static TemplateHandler createFromName(String category, String name) {
        TemplateManager manager = new TemplateManager();

        // Use the TemplateManager iteration which should merge contents between the
        // extras/templates/ and tools/templates folders and pick the most recent version
        List<File> templates = manager.getTemplates(category);
        for (File file : templates) {
            if (file.getName().equals(name) && category.equals(file.getParentFile().getName())) {
                return new TemplateHandler(file, manager);
            }
        }

        return new TemplateHandler(new File(getTemplateRootFolder(),
                category + File.separator + name), manager);
    }

    private TemplateHandler(File rootPath, TemplateManager manager) {
        mRootPath = rootPath;
        mManager = manager;
        mLoader = new MyTemplateLoader();
        mLoader.setPrefix(mRootPath.getPath());
    }

    public TemplateManager getManager() {
        return mManager;
    }

    public void setBackupMergedFiles(boolean backupMergedFiles) {
        mBackupMergedFiles = backupMergedFiles;
    }

    @NonNull
    public List<Change> render(IProject project, Map<String, Object> args) {
        mOpen.clear();

        mProject = project;
        mMergeChanges = new ArrayList<Change>();
        mTextChanges = new ArrayList<Change>();
        mOtherChanges = new ArrayList<Change>();

        // Render the instruction list template.
        Map<String, Object> paramMap = createParameterMap(args);
        Configuration freemarker = new Configuration();
        freemarker.setObjectWrapper(new DefaultObjectWrapper());
        freemarker.setTemplateLoader(mLoader);

        processVariables(freemarker, TEMPLATE_XML, paramMap);

        // Add the changes in the order where merges are shown first, then text files,
        // and finally other files (like jars and icons which don't have previews).
        List<Change> changes = new ArrayList<Change>();
        changes.addAll(mMergeChanges);
        changes.addAll(mTextChanges);
        changes.addAll(mOtherChanges);
        return changes;
    }

    Map<String, Object> createParameterMap(Map<String, Object> args) {
        final Map<String, Object> paramMap = createBuiltinMap();

        // Wizard parameters supplied by user, specific to this template
        paramMap.putAll(args);

        return paramMap;
    }

    /** Data model for the templates */
    static Map<String, Object> createBuiltinMap() {
        // Create the data model.
        final Map<String, Object> paramMap = new HashMap<String, Object>();

        // Builtin conversion methods
        paramMap.put("slashedPackageName", new FmSlashedPackageNameMethod());       //$NON-NLS-1$
        paramMap.put("camelCaseToUnderscore", new FmCamelCaseToUnderscoreMethod()); //$NON-NLS-1$
        paramMap.put("underscoreToCamelCase", new FmUnderscoreToCamelCaseMethod()); //$NON-NLS-1$
        paramMap.put("activityToLayout", new FmActivityToLayoutMethod());           //$NON-NLS-1$
        paramMap.put("layoutToActivity", new FmLayoutToActivityMethod());           //$NON-NLS-1$
        paramMap.put("classToResource", new FmClassNameToResourceMethod());         //$NON-NLS-1$
        paramMap.put("escapeXmlAttribute", new FmEscapeXmlStringMethod());          //$NON-NLS-1$
        paramMap.put("escapeXmlText", new FmEscapeXmlStringMethod());               //$NON-NLS-1$
        paramMap.put("escapeXmlString", new FmEscapeXmlStringMethod());             //$NON-NLS-1$
        paramMap.put("extractLetters", new FmExtractLettersMethod());               //$NON-NLS-1$

        // This should be handled better: perhaps declared "required packages" as part of the
        // inputs? (It would be better if we could conditionally disable template based
        // on availability)
        Map<String, String> builtin = new HashMap<String, String>();
        builtin.put("templatesRes", VALUE_TEMPLATE_DIR); //$NON-NLS-1$
        paramMap.put("android", builtin);                //$NON-NLS-1$

        return paramMap;
    }

    static void addDirectoryParameters(Map<String, Object> parameters, IProject project) {
        IPath srcDir = project.getFile(SdkConstants.SRC_FOLDER).getProjectRelativePath();
        parameters.put(ATTR_SRC_DIR, srcDir.toString());

        IPath resDir = project.getFile(SdkConstants.RES_FOLDER).getProjectRelativePath();
        parameters.put(ATTR_RES_DIR, resDir.toString());

        IPath manifestDir = project.getProjectRelativePath();
        parameters.put(ATTR_MANIFEST_DIR, manifestDir.toString());
        parameters.put(ATTR_MANIFEST_OUT, manifestDir.toString());
        
        parameters.put(ATTR_PROJECT_DIR, manifestDir.toString());
        parameters.put(ATTR_PROJECT_OUT, manifestDir.toString());
        
        parameters.put(ATTR_DEBUG_KEYSTORE_SHA1, "");
    }

    @Nullable
    public TemplateMetadata getTemplate() {
        if (mTemplate == null) {
            mTemplate = mManager.getTemplate(mRootPath);
        }

        return mTemplate;
    }

    @NonNull
    public String getResourcePath(String templateName) {
        return new File(mRootPath.getPath(), templateName).getPath();
    }

    /**
     * Load a text resource for the given relative path within the template
     *
     * @param relativePath relative path within the template
     * @return the string contents of the template text file
     */
    @Nullable
    public String readTemplateTextResource(@NonNull String relativePath) {
        try {
            return Files.toString(new File(mRootPath,
                    relativePath.replace('/', File.separatorChar)), Charsets.UTF_8);
        } catch (IOException e) {
            AndmoreAndroidPlugin.log(e, null);
            return null;
        }
    }

    @Nullable
    public String readTemplateTextResource(@NonNull File file) {
        assert file.isAbsolute();
        try {
            return Files.toString(file, Charsets.UTF_8);
        } catch (IOException e) {
            AndmoreAndroidPlugin.log(e, null);
            return null;
        }
    }

    /**
     * Reads the contents of a resource
     *
     * @param relativePath the path relative to the template directory
     * @return the binary data read from the file
     */
    @Nullable
    public byte[] readTemplateResource(@NonNull String relativePath) {
        try {
            return Files.toByteArray(new File(mRootPath, relativePath));
        } catch (IOException e) {
            AndmoreAndroidPlugin.log(e, null);
            return null;
        }
    }

    /**
     * Most recent thrown exception during template instantiation. This should
     * basically always be null. Used by unit tests to see if any template
     * instantiation recorded a failure.
     */
    @VisibleForTesting
    public static Exception sMostRecentException;

    /** Read the given FreeMarker file and process the variable definitions */
    private void processVariables(final Configuration freemarker,
            String file, final Map<String, Object> paramMap) {
        try {
            String xml;
            if (file.endsWith(DOT_XML)) {
                // Just read the file
                xml = readTemplateTextResource(file);
                if (xml == null) {
                    return;
                }
            } else {
                mLoader.setTemplateFile(new File(mRootPath, file));
                Template inputsTemplate = freemarker.getTemplate(file);
                StringWriter out = new StringWriter();
                inputsTemplate.process(paramMap, out);
                out.flush();
                xml = out.toString();
            }

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new ByteArrayInputStream(xml.getBytes()), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String name,
                        Attributes attributes)
                                throws SAXException {
                    if (TAG_PARAMETER.equals(name)) {
                        String id = attributes.getValue(ATTR_ID);
                        if (!paramMap.containsKey(id)) {
                            String value = attributes.getValue(ATTR_DEFAULT);
                            Object mapValue = value;
                            if (value != null && !value.isEmpty()) {
                                String type = attributes.getValue(ATTR_TYPE);
                                if ("boolean".equals(type)) { //$NON-NLS-1$
                                    mapValue = Boolean.valueOf(value);
                                }
                            }
                            paramMap.put(id, mapValue);
                        }
                    } else if (TAG_GLOBAL.equals(name)) {
                        String id = attributes.getValue(ATTR_ID);
                        if (!paramMap.containsKey(id)) {
                        	paramMap.put(id, TypedVariable.parseGlobal(attributes));
                        }
                    } else if (TAG_GLOBALS.equals(name)) {
                        // Handle evaluation of variables
                        String path = attributes.getValue(ATTR_FILE);
                        if (path != null) {
                            processVariables(freemarker, path, paramMap);
                        } // else: <globals> root element
                    } else if (TAG_EXECUTE.equals(name)) {
                        String path = attributes.getValue(ATTR_FILE);
                        if (path != null) {
                            execute(freemarker, path, paramMap);
                        }
                    } else if (TAG_DEPENDENCY.equals(name)) {
                        String dependencyName = attributes.getValue(ATTR_NAME);
                        if (dependencyName.equals(SUPPORT_LIBRARY_NAME)) {
                            // We assume the revision requirement has been satisfied
                            // by the wizard
                            File path = AddSupportJarAction.getSupportJarFile();
                            if (path != null) {
                                IPath to = getTargetPath(FD_NATIVE_LIBS +'/' + path.getName());
                                try {
                                    copy(path, to);
                                } catch (IOException ioe) {
                                    AndmoreAndroidPlugin.log(ioe, null);
                                }
                            }
                        }
                    } else if (!name.equals("template") && !name.equals(TAG_CATEGORY) &&
                    		!name.equals(TAG_FORMFACTOR) && !name.equals("option") &&
                    		!name.equals(TAG_THUMBS) && !name.equals(TAG_THUMB) &&
                    		!name.equals(TAG_ICONS)) {
                        System.err.println("WARNING: Unknown template directive " + name);
                    }
                }
            });
        } catch (Exception e) {
            sMostRecentException = e;
            AndmoreAndroidPlugin.log(e, null);
        }
    }

    @SuppressWarnings("unused")
    private boolean canOverwrite(File file) {
        if (file.exists()) {
            // Warn that the file already exists and ask the user what to do
            if (!mYesToAll) {
                MessageDialog dialog = new MessageDialog(null, "File Already Exists", null,
                        String.format(
                                "%1$s already exists.\nWould you like to replace it?",
                                file.getPath()),
                                MessageDialog.QUESTION, new String[] {
                    // Yes will be moved to the end because it's the default
                    "Yes", "No", "Cancel", "Yes to All"
                }, 0);
                int result = dialog.open();
                switch (result) {
                case 0:
                    // Yes
                    break;
                case 3:
                    // Yes to all
                    mYesToAll = true;
                    break;
                case 1:
                    // No
                    return false;
                case SWT.DEFAULT:
                case 2:
                    // Cancel
                    mNoToAll = true;
                    return false;
                }
            }

            if (mBackupMergedFiles) {
                return makeBackup(file);
            } else {
                return file.delete();
            }
        }

        return true;
    }

    /** Executes the given recipe file: copying, merging, instantiating, opening files etc */
    private void execute(
            final Configuration freemarker,
            String file,
            final Map<String, Object> paramMap) {
        try {
            mLoader.setTemplateFile(new File(mRootPath, file));
            Template freemarkerTemplate = freemarker.getTemplate(file);

            StringWriter out = new StringWriter();
            freemarkerTemplate.process(paramMap, out);
            out.flush();
            String xml = out.toString();

            // Parse and execute the resulting instruction list.
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            saxParser.parse(new ByteArrayInputStream(xml.getBytes()),
                    new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String name,
                        Attributes attributes)
                                throws SAXException {
                    if (mNoToAll) {
                        return;
                    }

                    try {
                        boolean instantiate = TAG_INSTANTIATE.equals(name);
                        if (TAG_COPY.equals(name) || instantiate) {
                            String fromPath = attributes.getValue(ATTR_FROM);
                            String toPath = attributes.getValue(ATTR_TO);
                            if (toPath == null || toPath.isEmpty()) {
                                toPath = attributes.getValue(ATTR_FROM);
                                toPath = AdtUtils.stripSuffix(toPath, DOT_FTL);
                            }
                            IPath to = getTargetPath(toPath);
                            if (instantiate) {
                                instantiate(freemarker, paramMap, fromPath, to);
                            } else {
                                copyTemplateResource(fromPath, to);
                            }
                        } else if (TAG_MERGE.equals(name)) {
                            String fromPath = attributes.getValue(ATTR_FROM);
                            String toPath = attributes.getValue(ATTR_TO);
                            if (toPath == null || toPath.isEmpty()) {
                                toPath = attributes.getValue(ATTR_FROM);
                                toPath = AdtUtils.stripSuffix(toPath, DOT_FTL);
                            }
                            // Resources in template.xml are located within root/
                            IPath to = getTargetPath(toPath);
                            merge(freemarker, paramMap, fromPath, to);
                        } else if (name.equals(TAG_OPEN)) {
                            // The relative path here is within the output directory:
                            String relativePath = attributes.getValue(ATTR_FILE);
                            if (relativePath != null && !relativePath.isEmpty()) {
                                mOpen.add(relativePath);
                            }
                        } else if (TAG_DEPENDENCY.equals(name)) {
                            String dependencyUrl = attributes.getValue(ATTR_MAVEN_URL);
                            File path;
                            if (dependencyUrl.contains(MAVEN_SUPPORT_V4)) {
                                // We assume the revision requirement has been satisfied
                                // by the wizard
                                path = AddSupportJarAction.getSupportJarFile();
                            } else if (dependencyUrl.contains(MAVEN_SUPPORT_V13)) {
                                path = AddSupportJarAction.getSupport13JarFile();
                            } else if (dependencyUrl.contains(MAVEN_APPCOMPAT)) {
                                path = null;
                                mFinalizingActions.add(new Runnable() {
                                    @Override
                                    public void run() {
                                        AddSupportJarAction.installAppCompatLibrary(mProject, true);
                                    }
                                });
                            } else {
                                path = null;
                                System.err.println("WARNING: Unknown dependency type");
                            }

                            if (path != null) {
                                IPath to = getTargetPath(FD_NATIVE_LIBS +'/' + path.getName());
                                try {
                                    copy(path, to);
                                } catch (IOException ioe) {
                                    AndmoreAndroidPlugin.log(ioe, null);
                                }
                            }
                        } else if (!name.equals("recipe") && !name.equals(TAG_DEPENDENCY)) { //$NON-NLS-1$
                            System.err.println("WARNING: Unknown template directive " + name);
                        }
                    } catch (Exception e) {
                        sMostRecentException = e;
                        AndmoreAndroidPlugin.log(e, null);
                    }
                }
            });

        } catch (Exception e) {
            sMostRecentException = e;
            AndmoreAndroidPlugin.log(e, null);
        }
    }

    @NonNull
    private File getFullPath(@NonNull String fromPath) {
        if (fromPath.startsWith(VALUE_TEMPLATE_DIR)) {
            return new File(getTemplateRootFolder(), RESOURCE_ROOT + File.separator
                    + fromPath.substring(VALUE_TEMPLATE_DIR.length() + 1).replace('/',
                            File.separatorChar));
        }
        return new File(mRootPath, DATA_ROOT + File.separator + fromPath);
    }

    @NonNull
    private IPath getTargetPath(@NonNull String relative) {
        if (relative.indexOf('\\') != -1) {
            relative = relative.replace('\\', '/');
        }
        return new Path(relative);
    }

    @NonNull
    private IFile getTargetFile(@NonNull IPath path) {
        return mProject.getFile(path);
    }

    private void merge(
            @NonNull final Configuration freemarker,
            @NonNull final Map<String, Object> paramMap,
            @NonNull String relativeFrom,
            @NonNull IPath toPath) throws IOException, TemplateException {

        String currentXml = null;

        IFile to = getTargetFile(toPath);
        if (to.exists()) {
            currentXml = AndmoreAndroidPlugin.readFile(to);
        }

        if (currentXml == null) {
            // The target file doesn't exist: don't merge, just copy
            boolean instantiate = relativeFrom.endsWith(DOT_FTL);
            if (instantiate) {
                instantiate(freemarker, paramMap, relativeFrom, toPath);
            } else {
                copyTemplateResource(relativeFrom, toPath);
            }
            return;
        }

        if (!to.getFileExtension().equals(EXT_XML)) {
            throw new RuntimeException("Only XML files can be merged at this point: " + to);
        }

        String xml = null;
        File from = getFullPath(relativeFrom);
        if (relativeFrom.endsWith(DOT_FTL)) {
            // Perform template substitution of the template prior to merging
            mLoader.setTemplateFile(from);
            Template template = freemarker.getTemplate(from.getName());
            Writer out = new StringWriter();
            template.process(paramMap, out);
            out.flush();
            xml = out.toString();
        } else {
            xml = readTemplateTextResource(from);
            if (xml == null) {
                return;
            }
        }

        Document currentDocument = DomUtilities.parseStructuredDocument(currentXml);
        assert currentDocument != null : currentXml;
        Document fragment = DomUtilities.parseStructuredDocument(xml);
        assert fragment != null : xml;

        XmlFormatStyle formatStyle = XmlFormatStyle.MANIFEST;
        boolean modified;
        boolean ok;
        String fileName = to.getName();
        if (fileName.equals(SdkConstants.FN_ANDROID_MANIFEST_XML)) {
            Document mergedManifest = mergeManifest(currentDocument, fragment);
            modified = ok = mergedManifest != null;
            currentDocument = mergedManifest;
        } else {
            // Merge plain XML files
            String parentFolderName = to.getParent().getName();
            ResourceFolderType folderType = ResourceFolderType.getFolderType(parentFolderName);
            if (folderType != null) {
                formatStyle = EclipseXmlPrettyPrinter.getForFile(toPath);
            } else {
                formatStyle = XmlFormatStyle.FILE;
            }

            modified = mergeResourceFile(currentDocument, fragment, folderType, paramMap);
            ok = true;
        }

        // Finally write out the merged file (formatting etc)
        String contents = null;
        if (ok) {
            if (modified) {
                contents = EclipseXmlPrettyPrinter.prettyPrint(currentDocument,
                        EclipseXmlFormatPreferences.create(), formatStyle, null,
                        currentXml.endsWith("\n")); //$NON-NLS-1$
            }
        } else {
            // Just insert into file along with comment, using the "standard" conflict
            // syntax that many tools and editors recognize.
            String sep = SdkUtils.getLineSeparator();
            contents =
                    "<<<<<<< Original" + sep
                    + currentXml + sep
                    + "=======" + sep
                    + xml
                    + ">>>>>>> Added" + sep;
        }

        if (contents != null) {
            TextFileChange change = new TextFileChange("Merge " + fileName, to);
            MultiTextEdit rootEdit = new MultiTextEdit();
            rootEdit.addChild(new ReplaceEdit(0, currentXml.length(), contents));
            change.setEdit(rootEdit);
            change.setTextType(SdkConstants.EXT_XML);
            mMergeChanges.add(change);
        }
    }

    /** Merges the given resource file contents into the given resource file
     * @param paramMap */
    private static boolean mergeResourceFile(Document currentDocument, Document fragment,
            ResourceFolderType folderType, Map<String, Object> paramMap) {
        boolean modified = false;

        // Copy namespace declarations
        NamedNodeMap attributes = fragment.getDocumentElement().getAttributes();
        if (attributes != null) {
            for (int i = 0, n = attributes.getLength(); i < n; i++) {
                Attr attribute = (Attr) attributes.item(i);
                if (attribute.getName().startsWith(XMLNS_PREFIX)) {
                    currentDocument.getDocumentElement().setAttribute(attribute.getName(),
                            attribute.getValue());
                }
            }
        }

        // For layouts for example, I want to *append* inside the root all the
        // contents of the new file.
        // But for resources for example, I want to combine elements which specify
        // the same name or id attribute.
        // For elements like manifest files we need to insert stuff at the right
        // location in a nested way (activities in the application element etc)
        // but that doesn't happen for the other file types.
        Element root = fragment.getDocumentElement();
        NodeList children = root.getChildNodes();
        List<Node> nodes = new ArrayList<Node>(children.getLength());
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            nodes.add(child);
            root.removeChild(child);
        }
        Collections.reverse(nodes);

        root = currentDocument.getDocumentElement();

        if (folderType == ResourceFolderType.VALUES) {
            // Try to merge items of the same name
            Map<String, Node> old = new HashMap<String, Node>();
            NodeList newSiblings = root.getChildNodes();
            for (int i = newSiblings.getLength() - 1; i >= 0; i--) {
                Node child = newSiblings.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) child;
                    String name = getResourceId(element);
                    if (name != null) {
                        old.put(name, element);
                    }
                }
            }

            for (Node node : nodes) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String name = getResourceId(element);
                    Node replace = name != null ? old.get(name) : null;
                    if (replace != null) {
                        // There is an existing item with the same id: just replace it
                        // ACTUALLY -- let's NOT change it.
                        // Let's say you've used the activity wizard once, and it
                        // emits some configuration parameter as a resource that
                        // it depends on, say "padding". Then the user goes and
                        // tweaks the padding to some other number.
                        // Now running the wizard a *second* time for some new activity,
                        // we should NOT go and set the value back to the template's
                        // default!
                        //root.replaceChild(node, replace);

                        // ... ON THE OTHER HAND... What if it's a parameter class
                        // (where the template rewrites a common attribute). Here it's
                        // really confusing if the new parameter is not set. This is
                        // really an error in the template, since we shouldn't have conflicts
                        // like that, but we need to do something to help track this down.
                        AndmoreAndroidPlugin.log(null,
                                "Warning: Ignoring name conflict in resource file for name %1$s",
                                name);
                    } else {
                        root.appendChild(node);
                        modified = true;
                    }
                }
            }
        } else {
            // In other file types, such as layouts, just append all the new content
            // at the end.
            for (Node node : nodes) {
                root.appendChild(node);
                modified = true;
            }
        }
        return modified;
    }

    /** Merges the given manifest fragment into the given manifest file */
    private static Document mergeManifest(Document currentManifest, Document fragment) {
        if (currentManifest == null || fragment == null) {
            return null;
        }

        // TODO change MergerLog.wrapSdkLog by a custom IMergerLog that will create
        // and maintain error markers.

        // Transfer package element from manifest to merged in root; required by
        // manifest merger
        Element fragmentRoot = fragment.getDocumentElement();
        Element manifestRoot = currentManifest.getDocumentElement();
        if (fragmentRoot == null || manifestRoot == null) {
            return null;
        }
        String pkg = fragmentRoot.getAttribute(ATTR_PACKAGE);
        if (pkg == null || pkg.isEmpty()) {
            pkg = manifestRoot.getAttribute(ATTR_PACKAGE);
            if (pkg != null && !pkg.isEmpty()) {
                fragmentRoot.setAttribute(ATTR_PACKAGE, pkg);
            }
        }

        try {
            File currentManifestFile = createTemp(currentManifest);
            File fragmentFile = createTemp(fragment);

            // TODO Extract the correct type
            MergingReport report = ManifestMerger2.newMerger(currentManifestFile, AndmoreAndroidPlugin.getDefault(), MergeType.APPLICATION).addLibraryManifest(fragmentFile).merge();
            if (report.getResult().isError()) {
                return null;
            }
            return DomUtilities.parseStructuredDocument(report.getMergedDocument(MergedManifestKind.MERGED));
        }
        catch (IOException e) {
            StatusManager.getManager().handle(new Status(IStatus.WARNING, AndmoreAndroidPlugin.PLUGIN_ID, "Unable to create temporary file for manifest merging.", e));
            return null;
        } catch (MergeFailureException e) {
            StatusManager.getManager().handle(new Status(IStatus.WARNING, AndmoreAndroidPlugin.PLUGIN_ID, "An unexpected error occurred while merging manifests.", e));
            return null;
        }
    }
    
    private static File createTemp(Document doc) throws IOException {
        File tmp = File.createTempFile("android-manifest", "xml");
        tmp.delete();

        FileUtils.createFile(tmp, EclipseXmlPrettyPrinter.prettyPrint(doc,
                EclipseXmlFormatPreferences.create(),  XmlFormatStyle.MANIFEST, null,
                false));
        tmp.deleteOnExit();
        return tmp;
    }

    /**
     * Makes a backup of the given file, if it exists, by renaming it to name~
     * (and removing an old name~ file if it exists)
     */
    private static boolean makeBackup(File file) {
        if (!file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            return false;
        }

        File backupFile = new File(file.getParentFile(), file.getName() + '~');
        if (backupFile.exists()) {
            backupFile.delete();
        }
        return file.renameTo(backupFile);
    }

    private static String getResourceId(Element element) {
        String name = element.getAttribute(ATTR_NAME);
        if (name == null) {
            name = element.getAttribute(ATTR_ID);
        }

        return name;
    }

    /** Instantiates the given template file into the given output file */
    private void instantiate(
            @NonNull final Configuration freemarker,
            @NonNull final Map<String, Object> paramMap,
            @NonNull String relativeFrom,
            @NonNull IPath to) throws IOException, TemplateException {
        // For now, treat extension-less files as directories... this isn't quite right
        // so I should refine this! Maybe with a unique attribute in the template file?
        boolean isDirectory = relativeFrom.indexOf('.') == -1;
        if (isDirectory) {
            // It's a directory
            copyTemplateResource(relativeFrom, to);
        } else {
            File from = getFullPath(relativeFrom);
            mLoader.setTemplateFile(from);
            Template template = freemarker.getTemplate(from.getName());
            Writer out = new StringWriter(1024);
            template.process(paramMap, out);
            out.flush();
            String contents = out.toString();

            contents = format(mProject, contents, to);
            IFile targetFile = getTargetFile(to);
            TextFileChange change = createNewFileChange(targetFile);
            MultiTextEdit rootEdit = new MultiTextEdit();
            rootEdit.addChild(new InsertEdit(0, contents));
            change.setEdit(rootEdit);
            mTextChanges.add(change);
        }
    }

    private static String format(IProject project, String contents, IPath to) {
        String name = to.lastSegment();
        if (name.endsWith(DOT_XML)) {
            XmlFormatStyle formatStyle = EclipseXmlPrettyPrinter.getForFile(to);
            EclipseXmlFormatPreferences prefs = EclipseXmlFormatPreferences.create();
            return EclipseXmlPrettyPrinter.prettyPrint(contents, prefs, formatStyle, null);
        } else if (name.endsWith(DOT_JAVA)) {
            Map<?, ?> options = null;
            if (project != null && project.isAccessible()) {
                try {
                    IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
                    if (javaProject != null) {
                        options = javaProject.getOptions(true);
                    }
                } catch (CoreException e) {
                    AndmoreAndroidPlugin.log(e, null);
                }
            }
            if (options == null) {
                options = JavaCore.getOptions();
            }

            CodeFormatter formatter = ToolFactory.createCodeFormatter(options);

            try {
                IDocument doc = new org.eclipse.jface.text.Document();
                // format the file (the meat and potatoes)
                doc.set(contents);
                TextEdit edit = formatter.format(
                        CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS,
                        contents, 0, contents.length(), 0, null);
                if (edit != null) {
                    edit.apply(doc);
                }

                return doc.get();
            } catch (Exception e) {
                AndmoreAndroidPlugin.log(e, null);
            }
        }

        return contents;
    }

    private static TextFileChange createNewFileChange(IFile targetFile) {
        String fileName = targetFile.getName();
        String message;
        if (targetFile.exists()) {
            message = String.format("Replace %1$s", fileName);
        } else {
            message = String.format("Create %1$s", fileName);
        }

        TextFileChange change = new TextFileChange(message, targetFile) {
            @Override
            protected IDocument acquireDocument(IProgressMonitor pm) throws CoreException {
                IDocument document = super.acquireDocument(pm);

                // In our case, we know we *always* use this TextFileChange
                // to *create* files, we're not appending to existing files.
                // However, due to the following bug we can end up with cached
                // contents of previously deleted files that happened to have the
                // same file name:
                //   https://bugs.eclipse.org/bugs/show_bug.cgi?id=390402
                // Therefore, as a workaround, wipe out the cached contents here
                if (document.getLength() > 0) {
                    try {
                        document.replace(0, document.getLength(), "");
                    } catch (BadLocationException e) {
                        // pass
                    }
                }

                return document;
            }
        };
        change.setTextType(fileName.substring(fileName.lastIndexOf('.') + 1));
        return change;
    }

    /**
     * Returns the list of files to open when the template has been created
     *
     * @return the list of files to open
     */
    @NonNull
    public List<String> getFilesToOpen() {
        return mOpen;
    }

    /**
     * Returns the list of actions to perform when the template has been created
     *
     * @return the list of actions to perform
     */
    @NonNull
    public List<Runnable> getFinalizingActions() {
        return mFinalizingActions;
    }

    /** Copy a template resource */
    private final void copyTemplateResource(
            @NonNull String relativeFrom,
            @NonNull IPath output) throws IOException {
        File from = getFullPath(relativeFrom);
        copy(from, output);
    }

    /** Returns true if the given file contains the given bytes */
    private static boolean isIdentical(@Nullable byte[] data, @NonNull IFile dest) {
        assert dest.exists();
        byte[] existing = AdtUtils.readData(dest);
        return Arrays.equals(existing, data);
    }

    /**
     * Copies the given source file into the given destination file (where the
     * source is allowed to be a directory, in which case the whole directory is
     * copied recursively)
     */
    private void copy(File src, IPath path) throws IOException {
        if (src.isDirectory()) {
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    copy(child, path.append(child.getName()));
                }
            }
        } else {
            IResource dest = mProject.getFile(path);
            if (dest.exists() && !(dest instanceof IFile)) {// Don't attempt to overwrite a folder
                assert false : dest.getClass().getName();
            return;
            }
            IFile file = (IFile) dest;
            String targetName = path.lastSegment();
            if (dest instanceof IFile) {
                if (dest.exists() && isIdentical(Files.toByteArray(src), file)) {
                    String label = String.format(
                            "Not overwriting %1$s because the files are identical", targetName);
                    NullChange change = new NullChange(label);
                    change.setEnabled(false);
                    mOtherChanges.add(change);
                    return;
                }
            }

            if (targetName.endsWith(DOT_XML)
                    || targetName.endsWith(DOT_JAVA)
                    || targetName.endsWith(DOT_TXT)
                    || targetName.endsWith(DOT_RS)
                    || targetName.endsWith(DOT_AIDL)
                    || targetName.endsWith(DOT_SVG)) {

                String newFile = Files.toString(src, Charsets.UTF_8);
                newFile = format(mProject, newFile, path);

                TextFileChange addFile = createNewFileChange(file);
                addFile.setEdit(new InsertEdit(0, newFile));
                mTextChanges.add(addFile);
            } else {
                // Write binary file: Need custom change for that
                IPath workspacePath = mProject.getFullPath().append(path);
                mOtherChanges.add(new CreateFileChange(targetName, workspacePath, src));
            }
        }
    }

    /**
     * A custom {@link TemplateLoader} which locates and provides templates
     * within the plugin .jar file
     */
    private static final class MyTemplateLoader implements TemplateLoader {
        private String mPrefix;

        public void setPrefix(String prefix) {
            mPrefix = prefix;
        }

        public void setTemplateFile(File file) {
            setTemplateParent(file.getParentFile());
        }

        public void setTemplateParent(File parent) {
            mPrefix = parent.getPath();
        }

        @Override
        public Reader getReader(Object templateSource, String encoding) throws IOException {
            URL url = (URL) templateSource;
            return new InputStreamReader(url.openStream(), encoding);
        }

        @Override
        public long getLastModified(Object templateSource) {
            return 0;
        }

        @Override
        public Object findTemplateSource(String name) throws IOException {
            String path = mPrefix != null ? mPrefix + '/' + name : name;
            File file = new File(path);
            if (file.exists()) {
                return file.toURI().toURL();
            }
            return null;
        }

        @Override
        public void closeTemplateSource(Object templateSource) throws IOException {
        }
    }

    /**
     * Validates this template to make sure it's supported
     * @param currentMinSdk the minimum SDK in the project, or -1 or 0 if unknown (e.g. codename)
     * @param buildApi the build API, or -1 or 0 if unknown (e.g. codename)
     *
     * @return a status object with the error, or null if there is no problem
     */
    @SuppressWarnings("cast") // In Eclipse 3.6.2 cast below is needed
    @Nullable
    public IStatus validateTemplate(int currentMinSdk, int buildApi) {
        TemplateMetadata template = getTemplate();
        if (template == null) {
            return null;
        }
        if (!template.isSupported()) {
            String versionString = AndmoreAndroidPlugin.getDefault().getBundle().getHeaders().get(
                    Constants.BUNDLE_VERSION);
            Version version = new Version(versionString);
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    String.format("This template requires a more recent version of the " +
                            "Android Eclipse plugin. Please update from version %1$d.%2$d.%3$d.",
                            version.getMajor(), version.getMinor(), version.getMicro()));
        }
        int templateMinSdk = template.getMinSdk();
        if (templateMinSdk > currentMinSdk && currentMinSdk >= 1) {
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    String.format("This template requires a minimum SDK version of at " +
                            "least %1$d, and the current min version is %2$d",
                            templateMinSdk, currentMinSdk));
        }
        int templateMinBuildApi = template.getMinBuildApi();
        if (templateMinBuildApi >  buildApi && buildApi >= 1) {
            return new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                    String.format("This template requires a build target API version of at " +
                            "least %1$d, and the current version is %2$d",
                            templateMinBuildApi, buildApi));
        }

        return null;
    }
}
