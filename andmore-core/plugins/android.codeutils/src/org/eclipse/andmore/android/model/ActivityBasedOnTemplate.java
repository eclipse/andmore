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
package org.eclipse.andmore.android.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.andmore.android.model.manifest.dom.ActionNode;
import org.eclipse.andmore.android.model.manifest.dom.ActivityNode;
import org.eclipse.andmore.android.model.manifest.dom.ApplicationNode;
import org.eclipse.andmore.android.model.manifest.dom.CategoryNode;
import org.eclipse.andmore.android.model.manifest.dom.IntentFilterNode;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.andmore.android.model.manifest.dom.UsesPermissionNode;
import org.eclipse.andmore.android.model.resources.ResourceFile;
import org.eclipse.andmore.android.model.resources.types.ResourcesNode;
import org.eclipse.andmore.android.model.resources.types.AbstractResourceNode.NodeType;
import org.eclipse.andmore.android.resources.AndroidProjectResources;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.core.DocumentAdapter;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringNode;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Activity Controller Model. As part of a MVC architecture, this class should
 * communicate with the Wizard UI to provide all needed information to create a
 * functional Activity.
 */
@SuppressWarnings("restriction")
public class ActivityBasedOnTemplate extends Launcher {
	private static final String NEW_LINE = "\n";

	public static String DATABASE_LIST_SAMPLE_LOCALIZED = ""; //$NON-NLS-1$

	public static String LIST_ACTIVITIES_SAMPLE_LOCALIZED = ""; //$NON-NLS-1$

	private static final String INTENT_ACTION_MAIN_NAME = "android.intent.action.MAIN"; //$NON-NLS-1$

	private static final String INTENT_CATEGORY_LAUNCHER_NAME = "android.intent.category.LAUNCHER"; //$NON-NLS-1$

	private static final String ACTIVITY_RESOURCE_LABEL_SUFFIX = "ActivityLabel"; //$NON-NLS-1$

	private static final int MANIFEST_UPDATING_STEPS = 6;

	private static final int RESOURCES_UPDATING_STEPS = 3;

	public static final String ACTIVITY_SAMPLES_FOLDER = "templates/activity_samples"; //$NON-NLS-1$

	private static final String SAMPLES_CONFIG_FILE_NAME = "samples_config.xml"; //$NON-NLS-1$

	private static final String LIST_ACTIVITIES_CONFIG_FILE_NAME = "list_activities_config.xml"; //$NON-NLS-1$

	private static final String STRINGS_FILE = "strings.xml"; //$NON-NLS-1$

	public static final String PREVIEW_FILE_NAME = "preview.png"; //$NON-NLS-1$

	public static final String NINE_PATCH_QUALIFIER = ".9";

	private static final String RES_TYPE_LAYOUT = "layout"; //$NON-NLS-1$

	private static final String RES_TYPE_DRAWABLE = "drawable"; //$NON-NLS-1$

	private static final String RES_TYPE_ANIM = "anim"; //$NON-NLS-1$

	private static final String RES_TYPE_SOURCE = "src"; //$NON-NLS-1$

	private static final String RES_TYPE_VALUES = "values"; //$NON-NLS-1$

	private static final String RES_TYPE_MENU = "menu"; //$NON-NLS-1$

	private static final String RES_TYPE_XML = "xml"; //$NON-NLS-1$

	private static final String DESCRIPTION_TAG = "description"; //$NON-NLS-1$

	private static final String PREVIEW_TAG = "preview"; //$NON-NLS-1$

	private static final String RESOURCE_TYPE_TAG = "resourceType"; //$NON-NLS-1$

	private static final String MODIFIER_TAG = "modifier"; //$NON-NLS-1$

	private static final String FILE_TAG = "file"; //$NON-NLS-1$

	private static final String NAME_TAG = "name"; //$NON-NLS-1$

	private static final String FINAL_NAME_TAG = "finalName"; //$NON-NLS-1$

	private static final String SAMPLE_TAG = "sample"; //$NON-NLS-1$

	private static final String DRAWABLE_REPLACE_TAG = "#drawable_name#"; //$NON-NLS-1$

	private static final String ANIM_REPLACE_TAG = "#anim_name#"; //$NON-NLS-1$

	private static final String LAYOUT_REPLACE_TAG = "#layout_name#"; //$NON-NLS-1$

	private static final String PACKAGE_REPLACE_TAG = "#package_name#"; //$NON-NLS-1$

	private static final String CLASS_REPLACE_TAG = "#class_name#"; //$NON-NLS-1$

	private static final String MAIN_ACTIVITY_REPLACE_TAG = "#main_activity#"; //$NON-NLS-1$

	private static final String XML_REPLACE_TAG = "#xml_name#"; //$NON-NLS-1$

	private static final String MENU_REPLACE_TAG = "#menu_name#"; //$NON-NLS-1$

	private static final String JAVA_EXTENSION = ".java"; //$NON-NLS-1$

	public static final String DATABASE_LIST_SAMPLE = "Database List"; //$NON-NLS-1$

	public static final String LIST_ACTIVITIES_SAMPLE = "List Activities"; //$NON-NLS-1$

	public static final String TABS_SAMPLE = "Tabs"; //$NON-NLS-1$

	public static enum SAMPLE_CATEGORY {
		SAMPLE_ACTIVITIES_CATEGORY, LIST_ACTIVITIES_CATEGORY
	};

	private boolean onStart = false;

	private String sample = null;

	private SAMPLE_CATEGORY sampleCategory = SAMPLE_CATEGORY.SAMPLE_ACTIVITIES_CATEGORY;

	private boolean createOpenHelper = false;

	private HashMap<String, HashMap<String, List<TemplateFile>>> availableSamples = null;

	private HashMap<String, HashMap<String, List<TemplateFile>>> availableListSamples = null;

	private final HashMap<String, String> samplesDescription = new HashMap<String, String>();

	private final HashMap<String, String> samplesPreview = new HashMap<String, String>();

	HashMap<String, String> workspaceResourceNames = null;

	private List<StringNode> sampleStringNodes = null;

	private List<StringArrayNode> sampleArrayNodes = null;

	/*
	 * Boolean flag to tell if the Activity is based on the DatabaseList sample
	 * or not
	 */
	private boolean useSampleDatabaseColumnsSelected = false;

	/*
	 * Boolean flag to tell if a table is selected if user has choose Database
	 * template
	 */
	private boolean isDatabaseTableSelected = false;

	/*
	 * Boolean flag to tell if the Activity is based on the DatabaseList defines
	 * the SQL Open Helper (class name and package name)
	 */
	private boolean sqlOpenHelperDefined = false;

	/*
	 * Boolean flag to tell if a list activity template is selected.
	 */
	private boolean isListActivitySelected = false;

	private boolean isDatabaseTemplateSelected = false;

	/*
	 * Collector interface specific to the DatabaseList sample
	 */
	private IDatabaseSampleActivityParametersWizardCollector collector = null;

	/*
	 * Database name used by the collector
	 */
	private String collectorDatabaseName = null;

	/*
	 * Database table used by the collector
	 */
	private Table collectorTable = null;

	/*
	 * Database sqlOpenHelper class name used by the collector
	 */
	private String sqlOpenHelperClassName = null;

	/*
	 * Database sqlOpenHelper package used by the collector
	 */
	private String sqlOpenHelperPackageName = null;

	/*
	 * List of columns used by the collector
	 */
	private ArrayList<Column> collectorColumnList = new ArrayList<Column>();

	/*
	 * Boolean flag to tell if the Activity will be set as MAIN or not in the
	 * AndroidManifest.
	 */
	private boolean isMainActivity = false;

	/**
	 * Check if the onStart Method should be created
	 * 
	 * @return
	 */
	public boolean isOnStart() {
		return onStart;
	}

	/**
	 * Change the onStart create property
	 * 
	 * @param onStart
	 */
	public void setOnStart(boolean onStart) {
		this.onStart = onStart;
	}

	/**
	 * Constructor for the Activity.
	 */
	public ActivityBasedOnTemplate() {
		super(IAndroidConstants.CLASS_ACTIVITY);
		DATABASE_LIST_SAMPLE_LOCALIZED = ResourceBundle.getBundle("plugin").getString("Activity_Samples_DB_name"); //$NON-NLS-1$ //$NON-NLS-2$ 
		LIST_ACTIVITIES_SAMPLE_LOCALIZED = ResourceBundle
				.getBundle("plugin").getString("Activity_Samples_ListActivities_name"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void setSample(String sample) {
		this.sample = sample;
	}

	public void setSampleCategoty(SAMPLE_CATEGORY sampleCategory) {
		this.sampleCategory = sampleCategory;
	}

	public SAMPLE_CATEGORY getSampleCategoty() {
		return this.sampleCategory;
	}

	public String getSample() {
		return sample;
	}

	public boolean isBasicInformationFilledIn() {
		boolean needInfo = false;
		IStatus status = getStatus();

		if (status.getSeverity() > IStatus.WARNING) {
			needInfo = true;
		}

		return needInfo;
	}

	/**
	 * Enables finish button when page is filled.
	 */
	@Override
	public boolean needMoreInformation() {
		boolean needInfo = isBasicInformationFilledIn();

		if ((sample == null)) {
			needInfo = true;
		} else if (sample.equals(ActivityBasedOnTemplate.DATABASE_LIST_SAMPLE_LOCALIZED)) {
			if (useSampleDatabaseColumnsSelected && sqlOpenHelperDefined && isDatabaseTableSelected) {
				needInfo = false || needInfo;
			} else {
				needInfo = true;
			}
		} else if (sample.equals(ActivityBasedOnTemplate.LIST_ACTIVITIES_SAMPLE_LOCALIZED)) {
			if (isListActivitySelected) {
				needInfo = false || needInfo;
			} else {
				needInfo = true;
			}
		}

		return needInfo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.model.IWizardModel#save(org.eclipse.jface
	 * .wizard.IWizardContainer, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public boolean save(IWizardContainer container, IProgressMonitor monitor) throws AndroidException {
		boolean templateFiles = true;
		String sourceFolder = null;
		workspaceResourceNames = new HashMap<String, String>();

		try {
			HashMap<String, List<TemplateFile>> selectedSample = null;

			if (sampleCategory.equals(SAMPLE_CATEGORY.SAMPLE_ACTIVITIES_CATEGORY)) {
				selectedSample = availableSamples.get(getSample());
			} else {
				selectedSample = availableListSamples.get(getSample());
			}

			Iterator<String> iterator = selectedSample.keySet().iterator();

			// UDC log for activity samples use
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_SAMPLE_ACTIVITY_CREATED,
					UsageDataConstants.KIND_SAMPLE_ACTIVITY_CREATED, "Activity using sample " + getSample(), //$NON-NLS-1$
					CodeUtilsActivator.PLUGIN_ID, CodeUtilsActivator.getDefault().getBundle().getVersion().toString());

			/*
			 * Copy resources that have no references to other types
			 */
			while (iterator.hasNext() && templateFiles) {
				String resourceType = iterator.next();

				/*
				 * First of all copy animation files, since they have no
				 * references so far
				 */
				if (resourceType.equals(RES_TYPE_ANIM)) {
					// handle animation files
					List<TemplateFile> filesList = selectedSample.get(resourceType);

					sourceFolder = getAnimFolder(null);
					for (int i = 0; i < filesList.size(); i++) {
						TemplateFile templateFile = filesList.get(i);

						templateFiles = templateFiles
								& copyTemplateFile(templateFile.getModelName(), templateFile.getFinalName(),
										sourceFolder, ANIM_REPLACE_TAG, false, monitor);
					}

				} else if (resourceType.equals(RES_TYPE_DRAWABLE)) {
					List<TemplateFile> filesList = selectedSample.get(resourceType);

					for (int i = 0; i < filesList.size(); i++) {
						TemplateFile templateFile = filesList.get(i);

						sourceFolder = getDrawableFolder(templateFile.getModifier());

						templateFiles = templateFiles
								& copyTemplateFile(templateFile.getModelName(), templateFile.getFinalName(),
										sourceFolder, DRAWABLE_REPLACE_TAG, templateFile.getFinalName()
												.endsWith(".xml"), monitor);

					}
				} else if (resourceType.equals(RES_TYPE_XML)) {
					List<TemplateFile> filesList = selectedSample.get(resourceType);

					for (int i = 0; i < filesList.size(); i++) {
						TemplateFile templateFile = filesList.get(i);

						sourceFolder = getXmlFolder(templateFile.getModifier());

						templateFiles = templateFiles
								& copyTemplateFile(templateFile.getModelName(), templateFile.getFinalName(),
										sourceFolder, XML_REPLACE_TAG, false, monitor);
					}
				} else if (resourceType.equals(RES_TYPE_VALUES)) {
					// treat sample string.xml
					List<TemplateFile> filesList = selectedSample.get(resourceType);

					for (int i = 0; i < filesList.size(); i++) {
						TemplateFile templateFile = filesList.get(i);

						sourceFolder = getValuesFolder(templateFile.getModifier());

						parseStringXmlNodes(templateFile.getModelName());

						// add to string.xml
						EclipseUtils.createOrUpdateDictionaryFile(getProject(), getSampleStringNodes(),
								getSampleArrayNodes(), monitor);
					}
				} else if (resourceType.equals(RES_TYPE_MENU)) {
					// treat menu xml files
					List<TemplateFile> filesList = selectedSample.get(resourceType);

					sourceFolder = getMenuFolder(null);

					for (int i = 0; i < filesList.size(); i++) {
						TemplateFile templateFile = filesList.get(i);

						templateFiles = templateFiles
								& copyTemplateFile(templateFile.getModelName(), templateFile.getFinalName(),
										sourceFolder, MENU_REPLACE_TAG, false, monitor);
					}
				}

			}

			/*
			 * Copy layout files and replace references
			 */
			List<TemplateFile> filesList = selectedSample.get(RES_TYPE_LAYOUT);

			if (filesList != null) {
				for (int i = 0; i < filesList.size(); i++) {
					TemplateFile templateFile = filesList.get(i);

					sourceFolder = getLayoutFolder(templateFile.getModifier());

					templateFiles = templateFiles
							& copyTemplateFile(templateFile.getModelName(), templateFile.getFinalName(), sourceFolder,
									LAYOUT_REPLACE_TAG, true, monitor);

				}
			}
		} catch (Exception e) {
			throw new AndroidException(e);
		}

		boolean classCreated = false;
		if (templateFiles) {
			classCreated = createActivityClass(monitor);
		}
		boolean addedOnManifest = false;

		if (classCreated) {
			addedOnManifest = createActivityOnManifest(monitor);
		}

		// Logs all permissions used in UDC log
		super.save(container, monitor);

		try {
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} catch (CoreException e) {
			// do nothing
		}
		return classCreated && addedOnManifest && templateFiles;

	}

	private String getValuesFolder(String modifier) {
		return IAndroidConstants.FD_RES + IPath.SEPARATOR + IAndroidConstants.FD_VALUES
				+ ((modifier != null) ? "-" + modifier : "");
	}

	private String getXmlFolder(String modifier) {
		return IAndroidConstants.FD_RES + IPath.SEPARATOR + IAndroidConstants.FD_XML
				+ ((modifier != null) ? "-" + modifier : "");
	}

	private String getDrawableFolder(String modifier) {
		return IAndroidConstants.FD_RES + IPath.SEPARATOR + IAndroidConstants.FD_DRAWABLE
				+ ((modifier != null) ? "-" + modifier : "");
	}

	private String getAnimFolder(String modifier) {
		return IAndroidConstants.FD_RES + IPath.SEPARATOR + IAndroidConstants.FD_ANIM;
	}

	private String getLayoutFolder(String modifier) {
		return IAndroidConstants.FD_RES + IPath.SEPARATOR + IAndroidConstants.FD_LAYOUT
				+ ((modifier != null) ? "-" + modifier : "");
	}

	private String getMenuFolder(String modifier) {
		return IAndroidConstants.FD_RES + IPath.SEPARATOR + IAndroidConstants.FD_MENU
				+ ((modifier != null) ? "-" + modifier : "");
	}

	public List<StringNode> getSampleStringNodes() {
		if (sampleStringNodes == null) {
			sampleStringNodes = new ArrayList<StringNode>();
		}
		return sampleStringNodes;
	}

	private void setSampleStringNodes(List<StringNode> sampleStringNodes) {
		this.sampleStringNodes = sampleStringNodes;
	}

	public List<StringArrayNode> getSampleArrayNodes() {
		if (sampleArrayNodes == null) {
			sampleArrayNodes = new ArrayList<StringArrayNode>();
		}
		return sampleArrayNodes;
	}

	private void setSampleArrayNodes(List<StringArrayNode> sampleArrayNodes) {
		this.sampleArrayNodes = sampleArrayNodes;
	}

	private void parseStringXmlNodes(String sampleStringFileName) {
		List<StringArrayNode> newArrayList = new ArrayList<StringArrayNode>();
		List<StringNode> newStrList = new ArrayList<StringNode>();

		try {
			File sampleStringXmlFile = new File(FileLocator.toFileURL(
					CodeUtilsActivator.getDefault().getBundle()
							.getEntry(ACTIVITY_SAMPLES_FOLDER + IPath.SEPARATOR + sampleStringFileName)).getFile());

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(sampleStringXmlFile);
			doc.getDocumentElement().normalize();

			// string nodes elements
			NodeList nodeLst = doc.getElementsByTagName("string"); //$NON-NLS-1$
			for (int s = 0; s < nodeLst.getLength(); s++) {
				Node fstNode = nodeLst.item(s);

				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fstElmnt = (Element) fstNode;
					String nameAtr = fstElmnt.getAttribute("name"); //$NON-NLS-1$
					String stringValue = fstElmnt.getTextContent();
					if ((nameAtr != null) && (nameAtr.length() > 0)) {
						newStrList.add(new StringNode(nameAtr, stringValue));
					}
				}
			}

			// array nodes elements
			nodeLst = doc.getElementsByTagName("string-array"); //$NON-NLS-1$
			for (int s = 0; s < nodeLst.getLength(); s++) {
				Node fstNode = nodeLst.item(s);

				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fstElmnt = (Element) fstNode;
					String nameAtr = fstElmnt.getAttribute("name"); //$NON-NLS-1$
					StringArrayNode strArray = new StringArrayNode(nameAtr);

					NodeList arrayItems = fstElmnt.getChildNodes();
					for (int i = 0; i < arrayItems.getLength(); i++) {
						Node itemNode = arrayItems.item(i);
						if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
							Element itemElmnt = (Element) itemNode;
							String itemValue = itemElmnt.getTextContent();
							strArray.addValue(itemValue);
						}
					}
					newArrayList.add(strArray);
				}
			}

			setSampleStringNodes(newStrList);
			setSampleArrayNodes(newArrayList);
		} catch (Exception e) {
			StudioLogger.error(ActivityBasedOnTemplate.class, e.getMessage() + " " //$NON-NLS-1$
					+ CodeUtilsNLS.UI_SampleSelectionPage_ErrorParsingStringXml, e);
		}

		return;
	}

	/*
	 * @param modelFilename Where file is going to be copied from
	 * 
	 * @param finalFilename Final name for resource, if any
	 * 
	 * @param sourceFolder Where file is going to be copied to
	 * 
	 * @param monitor Progress monitor
	 * 
	 * @param replacementTag tag do be replaced in the class file
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	private boolean copyTemplateFile(String modelFilename, String finalFilename, String sourceFolder,
			String replacementTag, boolean replaceReferences, IProgressMonitor monitor) throws Exception {

		IFolder pkgFolder = getProject().getFolder(sourceFolder);
		if (!pkgFolder.exists()) {
			pkgFolder.create(true, true, monitor);
		}
		IFile destFile = null;

		int i = 0;

		String[] resourceNameAndExtension = getResourceNameAndExtension(finalFilename);
		String resourceName = resourceNameAndExtension[0];
		String extension = resourceNameAndExtension[1];

		resourceName = filterResourceName(resourceName);

		// ensures file to be copied still does not exist
		do {
			if (i == 0) {
				destFile = pkgFolder.getFile(resourceName + extension);
			} else {
				destFile = pkgFolder.getFile(resourceName + i + extension);
			}
			i++;
		} while (destFile.exists());

		if (i != 1) {
			resourceName = resourceName + --i;
		}

		// updates tags to be replaced at class file
		String literalFileName = Matcher.quoteReplacement(modelFilename);
		workspaceResourceNames.put(replacementTag + literalFileName + "#", resourceName); //$NON-NLS-1$

		monitor.beginTask(CodeUtilsNLS.UI_ActivityBasedOnTemplateSupport_Configuring_Sample_Source_Task, 150);
		InputStream is = null;

		try {

			// gets template stream and creates a copy at user workspace

			if (!replaceReferences) {

				Bundle bundle = CodeUtilsActivator.getDefault().getBundle();

				URL url = bundle.getEntry((new StringBuilder("/")).append( //$NON-NLS-1$
						ACTIVITY_SAMPLES_FOLDER + IPath.SEPARATOR + modelFilename).toString());

				is = url.openStream();

			} else {
				// Get the original file as a string
				String fileString = EclipseUtils.readEmbeddedResource(CodeUtilsActivator.getDefault().getBundle(),
						ACTIVITY_SAMPLES_FOLDER + IPath.SEPARATOR + modelFilename);

				fileString = replaceResourceNames(fileString);

				is = new ByteArrayInputStream(fileString.getBytes("UTF-8"));

			}

			destFile.create(is, IResource.NONE, new SubProgressMonitor(monitor, 100));

		} finally {
			if (is != null) {
				is.close();
			}
			monitor.done();
		}
		return true;
	}

	private String[] getResourceNameAndExtension(String fullName) {

		String[] result = new String[2];

		String resourceName = fullName.substring(fullName.lastIndexOf("/") + 1, //$NON-NLS-1$
				fullName.lastIndexOf(".")); //$NON-NLS-1$
		String extension = fullName.substring(fullName.lastIndexOf(".")); //$NON-NLS-1$

		// detect 9-patch files
		if (extension.equals(".png") && resourceName.endsWith(NINE_PATCH_QUALIFIER)) {
			resourceName = resourceName.substring(0, resourceName.length() - NINE_PATCH_QUALIFIER.length());
			extension = NINE_PATCH_QUALIFIER + extension;
		}
		result[0] = resourceName;
		result[1] = extension;

		return result;

	}

	/*
	 * Creates the Activity java class
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return true if the class has been created or false otherwise
	 * 
	 * @throws AndroidException
	 */
	private boolean createActivityClass(IProgressMonitor monitor) throws AndroidException {
		boolean created = false;

		monitor.subTask(CodeUtilsNLS.UI_Activity_CreatingTheActivityJavaClass);

		String[] samplePath = null;

		try {

			samplePath = getSamplePath(RES_TYPE_SOURCE);
			createJavaClassFileFromTemplate(samplePath, monitor);
			created = true;
		} catch (JavaModelException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotCreateTheActivityClass, getName(),
					e.getLocalizedMessage());

			throw new AndroidException(errMsg);
		} catch (AndroidException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotCreateTheActivityClass, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		}

		return created;
	}

	/**
	 * Package name (based on project name declared on manifest)
	 * 
	 * @param project
	 * @return
	 * @throws CoreException
	 *             Exception thrown in case there are problems handling the
	 *             android project
	 * @throws AndroidException
	 *             Exception thrown in case there are problems handling the
	 *             android project
	 */
	protected String getManifestPackageName(IProject project) throws AndroidException, CoreException {
		// get android application name
		AndroidManifestFile androidManifestFile = AndroidProjectManifestFile.getFromProject(project);
		String appNamespace = ""; //$NON-NLS-1$
		if (androidManifestFile != null) {
			ManifestNode manifestNode = androidManifestFile.getManifestNode();
			appNamespace = manifestNode.getPackageName();
		}
		// return the android application name along with a persistence constant
		return appNamespace;
	}

	/**
	 * Creates the Java Class file based on text template file
	 * 
	 * @param sourcePath
	 *            The path to the file
	 * @param monitor
	 *            The progress monitor
	 * @throws JavaModelException
	 * @throws AndroidException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void createJavaClassFileFromTemplate(String[] sourcePath, IProgressMonitor monitor)
			throws JavaModelException, AndroidException {

		// only one class supported
		for (int i = 0; i < sourcePath.length; i++) {
			String loadedTemplate = EclipseUtils.readEmbeddedResource(CodeUtilsActivator.getDefault().getBundle(),
					sourcePath[i]);
			String packageName = getPackageFragment().getElementName();

			loadedTemplate = loadedTemplate.replaceAll(CLASS_REPLACE_TAG, getName());
			loadedTemplate = loadedTemplate.replaceAll(PACKAGE_REPLACE_TAG, packageName);
			loadedTemplate = loadedTemplate.replaceAll("#FILL_PARENT_LPARAM#", getApiVersion() > 7 ? "MATCH_PARENT"
					: "FILL_PARENT");
			try {
				loadedTemplate = loadedTemplate.replaceAll("#ManifestPackageName#", //$NON-NLS-1$
						getManifestPackageName(getProject()));
			} catch (CoreException e) {
				throw new AndroidException("Failed to get manifest file to add import from R", e); //$NON-NLS-1$
			}

			if ((sample != null) && sample.equals(ActivityBasedOnTemplate.DATABASE_LIST_SAMPLE_LOCALIZED)) {
				collector = getDatabaseSampleActivityParametersWizardCollector();
				if (collector != null) {

					collector.setDatabaseName(this.collectorDatabaseName);
					collector.setTable(this.collectorTable);
					collector.setSelectedColumns(collectorColumnList);
					collector.setSqlOpenHelperClassName(getSqlOpenHelperClassName());
					collector.setSqlOpenHelperPackageName(getSqlOpenHelperPackageName());
					collector.setCreateOpenHelper(isCreateOpenHelper());

					// using Database list sample - it is an special case
					// because it get data from an specific .db table
					loadedTemplate = loadedTemplate.replaceAll("#dbName#", collector.getDatabaseName()); //$NON-NLS-1$
					loadedTemplate = loadedTemplate.replaceAll("#tableName#", collector.getTableName()); //$NON-NLS-1$
					loadedTemplate = loadedTemplate.replaceAll("#tableNameUpperCase#", collector.getTableName() //$NON-NLS-1$
							.toUpperCase());
					loadedTemplate = loadedTemplate.replaceAll("#tableNameLowerCase#", collector.getTableName() //$NON-NLS-1$
							.toLowerCase());
					loadedTemplate = loadedTemplate.replaceAll("#columsNames#", collector.getColumnsNames()); //$NON-NLS-1$
					loadedTemplate = loadedTemplate.replaceAll("#constColumnsNames#", //$NON-NLS-1$
							collector.getConstColumnsNames());
					loadedTemplate = loadedTemplate.replaceAll("#columnGetValues#", collector.getCursorValues()); //$NON-NLS-1$
					loadedTemplate = loadedTemplate.replaceAll("#columnAddRows#", collector.getAddColumnsToRow()); //$NON-NLS-1$
					loadedTemplate = loadedTemplate.replaceAll("#sqlOpenHelperName#", //$NON-NLS-1$
							getSqlOpenHelperClassName());
					loadedTemplate = loadedTemplate.replaceAll("#imports#", collector.getImports()); //$NON-NLS-1$
					loadedTemplate = loadedTemplate.replaceAll("#getReadableDatabase#", //$NON-NLS-1$
							collector.getReadableDatabase());

					if (isCreateOpenHelper()) {
						collector.createSqlOpenHelper(getProject(), monitor);
					}
				}
			}

			// replace the main activity of the project, first used by
			// action_bar template
			try {
				// assume mainActivity be the activity we are creating (if the
				// project has no main activity). Try to find the real main
				// activity if the isMainActivity flag is false.
				String mainActivityName = getName();

				if (!isMainActivity) {
					ActivityNode mainActivityNode = AndroidProjectManifestFile.getFromProject(getProject())
							.getMainActivity();
					if (mainActivityNode != null) {
						mainActivityName = mainActivityNode.getNodeProperties().get("android:name");
						// remove a possible '.' that activities may contain
						// before the name
						if ((mainActivityName.length() > 0) && (mainActivityName.charAt(0) == '.')) {
							mainActivityName = mainActivityName.substring(1, mainActivityName.length());
						}
					}
				}

				loadedTemplate = loadedTemplate.replaceAll(MAIN_ACTIVITY_REPLACE_TAG, mainActivityName);
			} catch (CoreException e) {
				StudioLogger.error("Could not get Android Manifest File from project.");
			}

			loadedTemplate = replaceResourceNames(loadedTemplate);

			IPackageFragment targetPackage = getPackageFragmentRoot().getPackageFragment(
					getPackageFragment().getElementName());

			if (!targetPackage.exists()) {
				getPackageFragmentRoot().createPackageFragment(targetPackage.getElementName(), true, monitor);
			}

			/*
			 * Create activity class. Only the first src resource will become
			 * the Activity subclass. The other classes will be copied AS IS
			 */

			String resourceName = i == 0 ? getName() + JAVA_EXTENSION : Path.fromPortableString(sourcePath[i])
					.lastSegment();
			ICompilationUnit cu = targetPackage.createCompilationUnit(resourceName, loadedTemplate, true, monitor);

			// indent activity class
			try {
				ICompilationUnit workingCopy = cu.getWorkingCopy(monitor);
				IDocument document = new DocumentAdapter(workingCopy.getBuffer());

				// get project indentation configuration
				Map mapOptions = JavaCore.create(getProject()).getOptions(true);

				// changes to be applyed to the document
				TextEdit textEdit = CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT
						| CodeFormatter.F_INCLUDE_COMMENTS, document.get(), 0,
						System.getProperty("line.separator"), mapOptions); //$NON-NLS-1$

				workingCopy.applyTextEdit(textEdit, monitor);
				workingCopy.reconcile(ICompilationUnit.NO_AST, false, null, null);
				workingCopy.commitWorkingCopy(true, monitor);
				workingCopy.discardWorkingCopy();
			} catch (Exception ex) {
				// do nothing - indentation fails
			}
		}

	}

	private String replaceResourceNames(String textFile) {
		for (String curTag : workspaceResourceNames.keySet()) {
			textFile = textFile.replaceAll(curTag, workspaceResourceNames.get(curTag));
		}

		return textFile;
	}

	public boolean isCreateOpenHelper() {
		return createOpenHelper;
	}

	public void setCreateOpenHelper(boolean createOpenHelper) {
		this.createOpenHelper = createOpenHelper;
	}

	/*
	 * Creates the Activity class entry on AndroidManifest.xml file
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return true if the entry has been added or false otherwise
	 * 
	 * @throws AndroidException
	 */
	private boolean createActivityOnManifest(IProgressMonitor monitor) throws AndroidException {
		boolean created = false;

		try {
			int totalWork = MANIFEST_UPDATING_STEPS + RESOURCES_UPDATING_STEPS;

			monitor.beginTask("", totalWork); //$NON-NLS-1$

			monitor.subTask(CodeUtilsNLS.UI_Common_UpdatingTheAndroidManifestXMLFile);

			AndroidManifestFile androidManifestFile = AndroidProjectManifestFile.getFromProject(getProject());

			monitor.worked(1);

			ManifestNode manifestNode = androidManifestFile != null ? androidManifestFile.getManifestNode() : null;

			ApplicationNode applicationNode = manifestNode != null ? manifestNode.getApplicationNode() : null;

			monitor.worked(1);

			if (applicationNode != null) {
				// Adds the added permission nodes to manifest file
				List<String> permissionsNames = new ArrayList<String>();
				for (UsesPermissionNode i : manifestNode.getUsesPermissionNodes()) {
					if (!permissionsNames.contains(i.getName())) {
						permissionsNames.add(i.getName());
					}
				}

				for (String intentFilterPermission : getIntentFilterPermissionsAsArray()) {
					if (!permissionsNames.contains(intentFilterPermission)) {
						manifestNode.addChild(new UsesPermissionNode(intentFilterPermission));
					}
				}

				boolean activityExists = false;

				// Existing activity, if exists
				ActivityNode existingActivity = null;

				String classQualifier = (getPackageFragment().getElementName().equals(manifestNode.getPackageName()) ? "" : getPackageFragment() //$NON-NLS-1$
								.getElementName())
						+ "."; //$NON-NLS-1$

				for (ActivityNode activityNode : applicationNode.getActivityNodes()) {
					if (activityNode.getName().substring(activityNode.getName().lastIndexOf('.') + 1).equals(getName())) {
						activityExists = true;
						existingActivity = activityNode;
						break;
					}
				}

				if (isMainActivity) {
					boolean actionRemoved = false;

					// check if there is a main activity. If so, removes actions
					// and intent filter
					for (ActivityNode activityNode : applicationNode.getActivityNodes()) {
						if ((existingActivity != null) && existingActivity.equals(activityNode)) {
							continue;
						}

						List<IntentFilterNode> intentList = activityNode.getIntentFilterNodes();
						for (IntentFilterNode currentIntent : intentList) {
							actionRemoved = false;
							List<ActionNode> actionList = currentIntent.getActionNodes();
							for (ActionNode currentAction : actionList) {
								if (currentAction.getName().equals(INTENT_ACTION_MAIN_NAME)) {
									currentIntent.removeActionNode(currentAction);
									actionRemoved = true;
								}
							}
							// if INTENT_ACTION_MAIN_NAME is found remove
							// INTENT_CATEGORY_LAUNCHER_NAME too
							if (actionRemoved) {
								List<CategoryNode> categoryList = currentIntent.getCategoryNodes();
								for (CategoryNode currentCategory : categoryList) {
									if (currentCategory.getName().equals(INTENT_CATEGORY_LAUNCHER_NAME)) {
										currentIntent.removeCategoryNode(currentCategory);
									}
								}
							}
							// remove intent filter if empty
							if (actionRemoved && (currentIntent.getChildren().length == 0)) {
								activityNode.removeIntentFilterNode(currentIntent);
							}
						}
					}
				}

				monitor.worked(1);

				if (!activityExists) {
					ActivityNode activityNode = new ActivityNode(classQualifier + getName());

					String activityLabel = createActivityLabel(monitor);

					if (activityLabel != null) {
						activityNode.setLabel(AndroidProjectResources.STRING_CALL_PREFIX + activityLabel);
					}

					IntentFilterNode intentFilterNode = new IntentFilterNode();

					for (String intentFilterAction : getIntentFilterActionsAsArray()) {
						intentFilterNode.addActionNode(new ActionNode(intentFilterAction));
					}

					for (String intentFilterCategory : getIntentFilterCategoriesAsArray()) {
						intentFilterNode.addCategoryNode(new CategoryNode(intentFilterCategory));
					}

					// Check if we need to insert a filter action and filter
					// category setting this activity as MAIN
					if (isMainActivity) {
						intentFilterNode.addActionNode(new ActionNode(INTENT_ACTION_MAIN_NAME));
						intentFilterNode.addCategoryNode(new CategoryNode(INTENT_CATEGORY_LAUNCHER_NAME));

					}

					if (intentFilterNode.getChildren().length > 0) {
						activityNode.addIntentFilterNode(intentFilterNode);
					}

					applicationNode.addActivityNode(activityNode);

					monitor.worked(1);

					monitor.subTask(CodeUtilsNLS.UI_Common_SavingTheAndroidManifestXMLFile);

					AndroidProjectManifestFile.saveToProject(getProject(), androidManifestFile, true);
					created = true;

					monitor.worked(1);
				} else {
					if (isMainActivity) {
						boolean hasMainAction = false;
						boolean hasLauncherCategory = false;

						// Check if the existing activity already has the MAIN
						// and LAUNCHER intents
						if (existingActivity != null) {
							// Retrieve list of intent nodes
							List<IntentFilterNode> intentFilterNodeList = existingActivity.getIntentFilterNodes();

							// Create a intent filter in case it does not exist
							if (intentFilterNodeList.size() < 1) {
								IntentFilterNode intentNode = new IntentFilterNode();
								intentFilterNodeList.add(intentNode);
								existingActivity.addIntentFilterNode(intentNode);
							}

							for (IntentFilterNode intentFilterNode : intentFilterNodeList) {
								// Retrieve a list of intent actions
								List<ActionNode> actionNodes = intentFilterNode.getActionNodes();

								for (ActionNode actionNode : actionNodes) {
									if (actionNode.getName().equals(INTENT_ACTION_MAIN_NAME)) {
										hasMainAction = true;
									}
									break;

								}

								// Retrieve a list of intent categories
								List<CategoryNode> categoryNodes = intentFilterNode.getCategoryNodes();

								for (CategoryNode categoryNode : categoryNodes) {
									if (categoryNode.getName().equals(INTENT_CATEGORY_LAUNCHER_NAME)) {
										hasLauncherCategory = true;
									}
									break;
								}

								// If both the action and launcher are missing,
								// insert them and break the loop to avoid
								// duplicates
								if (!hasMainAction && !hasLauncherCategory) {
									intentFilterNode.addActionNode(new ActionNode(INTENT_ACTION_MAIN_NAME));
									intentFilterNode.addCategoryNode(new CategoryNode(INTENT_CATEGORY_LAUNCHER_NAME));
									break;
								}
							}
						}

						monitor.subTask(CodeUtilsNLS.UI_Common_SavingTheAndroidManifestXMLFile);

						AndroidProjectManifestFile.saveToProject(getProject(), androidManifestFile, true);
						created = true;

						monitor.worked(1);

					} else {
						created = true;
					}
				}

			}
		} catch (AndroidException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotUpdateTheManifestFile, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} catch (CoreException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotUpdateTheManifestFile, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} finally {
			monitor.done();
		}

		return created;
	}

	/**
	 * Adds the Activity label value on the strings resource file
	 * 
	 * @param monitor
	 *            the progress monitor
	 * 
	 * @return The label value if it has been added to the strings resource file
	 *         or null otherwise
	 * @throws AndroidException
	 */
	protected String createActivityLabel(IProgressMonitor monitor) throws AndroidException {
		String resLabel = null;

		if ((getLabel() != null) && (getLabel().trim().length() > 0)) {
			try {
				monitor.subTask(CodeUtilsNLS.UI_Common_UpdatingTheStringsResourceFile);

				ResourceFile stringsFile = AndroidProjectResources.getResourceFile(getProject(), NodeType.String);

				monitor.worked(1);

				if (stringsFile.getResourcesNode() == null) {
					stringsFile.addResourceEntry(new ResourcesNode());
				}

				resLabel = stringsFile.getNewResourceName(getName() + ACTIVITY_RESOURCE_LABEL_SUFFIX);

				org.eclipse.andmore.android.model.resources.types.StringNode strNode = new org.eclipse.andmore.android.model.resources.types.StringNode(
						resLabel);
				strNode.setNodeValue(getLabel());

				stringsFile.getResourcesNode().addChildNode(strNode);

				monitor.worked(1);

				AndroidProjectResources.saveResourceFile(getProject(), stringsFile, NodeType.String);

				monitor.worked(1);
			} catch (CoreException e) {
				String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotCreateTheActivityLabel,
						e.getLocalizedMessage());
				throw new AndroidException(errMsg);
			} catch (AndroidException e) {
				String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotCreateTheActivityLabel,
						e.getLocalizedMessage());
				throw new AndroidException(errMsg);
			}
		}

		return resLabel;
	}

	/**
	 * Return the default onStart() method signature including return value and
	 * visibility level.
	 * 
	 * @return
	 */
	public String getOnStartMessage() {
		return "protected void onStart()"; //$NON-NLS-1$
	}

	/**
	 * @return The selected sample's file.
	 */
	public String[] getSamplePath(String resourceType) {
		List<TemplateFile> resTypeFiles = null;

		if (sampleCategory.equals(SAMPLE_CATEGORY.SAMPLE_ACTIVITIES_CATEGORY)) {
			resTypeFiles = availableSamples.get(getSample()).get(resourceType);
		} else {
			resTypeFiles = availableListSamples.get(getSample()).get(resourceType);
		}

		String[] resTypePaths = new String[resTypeFiles.size()];

		for (int i = 0; i < resTypeFiles.size(); i++) {
			resTypePaths[i] = ACTIVITY_SAMPLES_FOLDER + IPath.SEPARATOR + resTypeFiles.get(i).getModelName();
		}
		return resTypePaths;
	}

	/**
	 * Evaluate available samples parsing samples_config.xml
	 * 
	 * @return
	 */
	public void evaluateSamplesList(SAMPLE_CATEGORY category) {

		HashMap<String, HashMap<String, List<TemplateFile>>> currentMap = null;
		try {
			String configFileName;

			if (category.equals(SAMPLE_CATEGORY.SAMPLE_ACTIVITIES_CATEGORY)) {
				configFileName = SAMPLES_CONFIG_FILE_NAME;
				availableSamples = new HashMap<String, HashMap<String, List<TemplateFile>>>();
				currentMap = availableSamples;
			} else if (category.equals(SAMPLE_CATEGORY.LIST_ACTIVITIES_CATEGORY)) {
				configFileName = LIST_ACTIVITIES_CONFIG_FILE_NAME;
				availableListSamples = new HashMap<String, HashMap<String, List<TemplateFile>>>();
				currentMap = availableListSamples;
			} else {
				throw new Exception();
			}

			File configFile = new File(FileLocator.toFileURL(
					CodeUtilsActivator.getDefault().getBundle()
							.getEntry(ACTIVITY_SAMPLES_FOLDER + IPath.SEPARATOR + configFileName)).getFile());

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(configFile);
			doc.getDocumentElement().normalize();
			NodeList nodeLst = doc.getElementsByTagName(SAMPLE_TAG);

			for (int s = 0; s < nodeLst.getLength(); s++) {

				HashMap<String, List<TemplateFile>> currentSample = null;
				Node fstNode = nodeLst.item(s);

				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {

					Element fstElmnt = (Element) fstNode;
					NodeList fstNmElmntLst = fstElmnt.getElementsByTagName(FILE_TAG);
					String nameAtr = fstElmnt.getAttribute(NAME_TAG);
					nameAtr = Platform.getResourceString(CodeUtilsActivator.getDefault().getBundle(), nameAtr.trim());

					String descAtr = fstElmnt.getAttribute(DESCRIPTION_TAG);
					descAtr = Platform.getResourceString(CodeUtilsActivator.getDefault().getBundle(), descAtr.trim());

					String folderAtr = fstElmnt.getAttribute(PREVIEW_TAG);
					if (folderAtr != null) {
						samplesPreview.put(nameAtr, folderAtr);
					}

					samplesDescription.put(nameAtr, descAtr);
					currentSample = new HashMap<String, List<TemplateFile>>();

					for (int n = 0; n < fstNmElmntLst.getLength(); n++) {
						Node fileNode = fstNmElmntLst.item(n);
						Element fileElmnt = (Element) fileNode;

						String resType = fileElmnt.getAttribute(RESOURCE_TYPE_TAG);
						String resName = fileElmnt.getAttribute(NAME_TAG);
						String finalName = fileElmnt.getAttribute(FINAL_NAME_TAG);

						String modifier = fileElmnt.getAttribute(MODIFIER_TAG);

						TemplateFile templateFile = new TemplateFile(resType, resName,
								((finalName.equals("")) ? resName : finalName), (((modifier.equals("")) ? null
										: modifier)));

						if ((resName != null) && (resName.lastIndexOf(".") > -1)) //$NON-NLS-1$
						{
							if (currentSample.get(resType) == null) {
								List<TemplateFile> curRes = new ArrayList<TemplateFile>();
								curRes.add(templateFile);
								currentSample.put(resType, curRes);
							} else {
								List<TemplateFile> curRes = currentSample.get(resType);
								curRes.add(templateFile);
							}
						}
					}

					currentMap.put(nameAtr, currentSample);
				}
			}
		} catch (Exception e) {
			currentMap.clear();
			StudioLogger.error(ActivityBasedOnTemplate.class, CodeUtilsNLS.UI_GenerateSampleListError, e);
		}
	}

	/**
	 * @return available samples
	 */
	public HashMap<String, HashMap<String, List<TemplateFile>>> getAvailableSamples() {
		return availableSamples;
	}

	/**
	 * Return available list activities samples.
	 * 
	 * @return available list activities samples
	 */
	public HashMap<String, HashMap<String, List<TemplateFile>>> getListActivitiesSamples() {
		return availableListSamples;
	}

	public String getSamplePreview() {
		return samplesPreview.get(getSample());
	}

	/**
	 * Returns selected sample description
	 * 
	 * @return sample description
	 */
	public String getSampleDescription() {
		StringBuffer result = new StringBuffer();
		HashMap<String, List<TemplateFile>> selectedSample = null;

		if (sampleCategory.equals(SAMPLE_CATEGORY.SAMPLE_ACTIVITIES_CATEGORY)) {
			selectedSample = availableSamples.get(getSample());
		} else {
			selectedSample = availableListSamples.get(getSample());
		}

		if (selectedSample != null) {
			result.append(samplesDescription.get(getSample()));
			result.append(NEW_LINE);
			if (selectedSample.size() > 0) {
				result.append(NEW_LINE);
				result.append(CodeUtilsNLS.UI_SampleSelectionPage_Description);
				result.append(NEW_LINE);

				for (String resourceType : selectedSample.keySet()) {
					result.append("\n - "); //$NON-NLS-1$
					List<TemplateFile> filesList = selectedSample.get(resourceType);
					if (resourceType.equals(RES_TYPE_SOURCE)) {
						for (int i = 0; i < filesList.size(); i++) {
							if (i > 0) {
								result.append("\n - "); //$NON-NLS-1$
							}

							if (getPackageFragment() != null) {
								IResource packageResource = getPackageFragment().getResource();
								IPath projectRelativePath = packageResource.getProjectRelativePath();
								String tmpName = filesList.get(i).getFinalName()
										.substring(filesList.get(i).getFinalName().indexOf("/") + 1); //$NON-NLS-1$

								/*
								 * Resolve class name. Keeping backward
								 * compatibility, the first declared resource of
								 * type RES_TYPE_SOURCE will be considered as
								 * the class that will become the Activity So
								 * the other RES_TYPE_SOURCE resources names are
								 * kept unchanged
								 */

								if (i == 0) {
									String className = getName();
									if ((className == null) || ((className != null) && (className.length() == 0))) {
										className = "<Activity>";
									}
									tmpName = tmpName.replace(tmpName.substring(0, tmpName.indexOf(".")), //$NON-NLS-1$
											className);
								}
								result.append(projectRelativePath.toString() + IPath.SEPARATOR + tmpName);
								result.append(" (" + getSampleResourceTypeFriendlyName(resourceType) //$NON-NLS-1$
										+ ")"); //$NON-NLS-1$                                
							}
						}
					} else {
						for (int i = 0; i < filesList.size(); i++) {
							if (i > 0) {
								result.append("\n - "); //$NON-NLS-1$
							}

							// append folder
							if (resourceType.equals(RES_TYPE_DRAWABLE)) {
								result.append(getDrawableFolder(filesList.get(i).getModifier()) + IPath.SEPARATOR);
							} else if (resourceType.equals(RES_TYPE_LAYOUT)) {
								result.append(getLayoutFolder(filesList.get(i).getModifier()) + IPath.SEPARATOR);
							} else if (resourceType.equals(RES_TYPE_VALUES)) {
								result.append(getValuesFolder(filesList.get(i).getModifier()) + IPath.SEPARATOR);
							} else if (resourceType.equals(RES_TYPE_ANIM)) {
								result.append(getAnimFolder(filesList.get(i).getModifier()) + IPath.SEPARATOR);
							} else if (resourceType.equals(RES_TYPE_XML)) {
								result.append(getXmlFolder(filesList.get(i).getModifier()) + IPath.SEPARATOR);
							} else if (resourceType.equals(RES_TYPE_MENU)) {
								result.append(getMenuFolder(filesList.get(i).getModifier()) + IPath.SEPARATOR);
							}

							// append file name
							if (resourceType.equals(RES_TYPE_VALUES)) {
								result.append(STRINGS_FILE);
							} else {
								String[] resourceNameAndExtension = getResourceNameAndExtension(filesList.get(i)
										.getFinalName());
								String resourceName = resourceNameAndExtension[0];
								String extension = resourceNameAndExtension[1];

								result.append(filterResourceName(resourceName) + extension);
							}
							result.append(" (" + getSampleResourceTypeFriendlyName(resourceType) //$NON-NLS-1$
									+ ")"); //$NON-NLS-1$
						}
					}
				}
			}
		}

		return result.toString();
	}

	private String getSampleResourceTypeFriendlyName(String resourceType) {
		String result = null;
		if (resourceType.equals(RES_TYPE_LAYOUT)) {
			result = CodeUtilsNLS.UI_SampleSelectionPage_Description_LayoutFile;
		} else if (resourceType.equals(RES_TYPE_DRAWABLE)) {
			result = CodeUtilsNLS.UI_SampleSelectionPage_Description_DrawableFile;
		} else if (resourceType.equals(RES_TYPE_SOURCE)) {
			result = CodeUtilsNLS.UI_SampleSelectionPage_Description_JavaFile;
		} else if (resourceType.equals(RES_TYPE_VALUES)) {
			result = CodeUtilsNLS.UI_SampleSelectionPage_Description_StringFile;
		} else if (resourceType.equals(RES_TYPE_XML)) {
			result = CodeUtilsNLS.UI_SampleSelectionPage_Description_XMLFile;
		} else if (resourceType.equals(RES_TYPE_ANIM)) {
			result = CodeUtilsNLS.UI_SampleSelectionPage_Description_AnimFile;
		} else if (resourceType.equals(RES_TYPE_MENU)) {
			result = CodeUtilsNLS.UI_SampleSelectionPage_Description_MenuFile;
		}
		return result;

	}

	/*
	 * Filter file names to [a-z][1-9]
	 * 
	 * @param name: string typed by the user
	 * 
	 * @return filtered string
	 */
	private String filterResourceName(String name) {
		String resourceName = name.toLowerCase();
		StringBuilder bld = new StringBuilder();

		for (int i = 0; i < resourceName.length(); i++) {
			char temp = resourceName.charAt(i);
			if (Character.isLetter(temp) || Character.isDigit(temp)) {
				bld.append(temp);
			}
		}

		return bld.toString();
	}

	/*
	 * Extension point related ids section
	 */
	private static final String PARAMETER_COLLECTOR_EXTENSION_POINT_ID = CodeUtilsActivator.PLUGIN_ID
			+ ".sampleActivityDatabase"; //$NON-NLS-1$

	private static final String PARAMETER_COLLECTOR_ELEM = "parameterCollector"; //$NON-NLS-1$

	private static final String IMPL_CLASS_ATTR = "class"; //$NON-NLS-1$

	public IDatabaseSampleActivityParametersWizardCollector getDatabaseSampleActivityParametersWizardCollector() {
		IExtensionRegistry extReg = Platform.getExtensionRegistry();
		IExtensionPoint extPoint = extReg.getExtensionPoint(PARAMETER_COLLECTOR_EXTENSION_POINT_ID);
		IExtension[] extensions = extPoint.getExtensions();
		IDatabaseSampleActivityParametersWizardCollector collector = null;

		for (IExtension aExtension : extensions) {
			IConfigurationElement[] configElements = aExtension.getConfigurationElements();
			for (IConfigurationElement aConfig : configElements) {
				if (aConfig.getName().equals(PARAMETER_COLLECTOR_ELEM)) {
					try {
						collector = (IDatabaseSampleActivityParametersWizardCollector) aConfig
								.createExecutableExtension(IMPL_CLASS_ATTR);
						break;
					} catch (CoreException e) {
						// Do nothing.
						// If a device framework cannot be instantiated, it will
						// not be plugged to emulator core plugin.
					}
				}
			}
		}
		return collector;
	}

	public boolean isUseSampleDatabaseTableSelected() {
		return useSampleDatabaseColumnsSelected;
	}

	public void setUseSampleDatabaseTableSelected(boolean useSampleDatabaseTableSelected) {
		this.useSampleDatabaseColumnsSelected = useSampleDatabaseTableSelected;
	}

	public boolean isListActivitySelected() {
		return isListActivitySelected;
	}

	public void setIsListActivitySelected(boolean isListActivitySelected) {
		this.isListActivitySelected = isListActivitySelected;
	}

	/**
	 * @return the isDatabaseTemplateSelected
	 */
	public boolean isDatabaseTemplateSelected() {
		return isDatabaseTemplateSelected;
	}

	/**
	 * @param isDatabaseTemplateSelected
	 *            the isDatabaseTemplateSelected to set
	 */
	public void setDatabaseTemplateSelected(boolean isDatabaseTemplateSelected) {
		this.isDatabaseTemplateSelected = isDatabaseTemplateSelected;
	}

	/**
	 * @return the isDatabaseTableSelected
	 */
	public boolean isDatabaseTableSelected() {
		return isDatabaseTableSelected;
	}

	/**
	 * @param isDatabaseTableSelected
	 *            the isDatabaseTableSelected to set
	 */
	public void setDatabaseTableSelected(boolean isDatabaseTableSelected) {
		this.isDatabaseTableSelected = isDatabaseTableSelected;
	}

	/**
	 * @param collectorDatabaseName
	 *            the collectorDatabaseName to set
	 */
	public void setCollectorDatabaseName(String collectorDatabaseName) {
		this.collectorDatabaseName = collectorDatabaseName;
	}

	/**
	 * @param collectorTable
	 *            the collectorTable to set
	 */
	public void setCollectorTable(Table collectorTable) {
		this.collectorTable = collectorTable;
	}

	/**
	 * @return the collectorTable
	 */
	public Table getCollectorTable() {
		return collectorTable;
	}

	/**
	 * @param collectorColumnList
	 *            the collectorColumnList to set
	 */
	public void setCollectorColumnList(ArrayList<Column> collectorColumnList) {
		this.collectorColumnList = collectorColumnList;
	}

	/**
	 * @return the collectorColumnList
	 */
	public ArrayList<Column> getCollectorColumnList() {
		return collectorColumnList;
	}

	/**
	 * @return the isMainActivity
	 */
	public boolean isMainActivity() {
		return isMainActivity;
	}

	/**
	 * @param isMainActivity
	 *            the isMainActivity to set
	 */
	public void setMainActivity(boolean isMainActivity) {
		this.isMainActivity = isMainActivity;
	}

	public String getSqlOpenHelperClassName() {
		return sqlOpenHelperClassName;
	}

	public void setSqlOpenHelperClassName(String sqlOpenHelperClassName) {
		this.sqlOpenHelperClassName = sqlOpenHelperClassName;
	}

	public String getSqlOpenHelperPackageName() {
		return sqlOpenHelperPackageName;
	}

	public void setSqlOpenHelperPackageName(String sqlOpenHelperPackageName) {
		this.sqlOpenHelperPackageName = sqlOpenHelperPackageName;
	}

	public boolean isSqlOpenHelperDefined() {
		return sqlOpenHelperDefined;
	}

	public void setSqlOpenHelperDefined(boolean sqlOpenHelperDefined) {
		this.sqlOpenHelperDefined = sqlOpenHelperDefined;
	}

}