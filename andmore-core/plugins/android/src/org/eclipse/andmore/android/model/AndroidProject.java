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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.Sample;
import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.utilities.AndroidStatus;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;

import com.android.sdklib.IAndroidTarget;

/**
 * Class that implements the model for the new project wizard
 */
public class AndroidProject implements IWizardModel {
	private static final IWorkspace WORKSPACE = ResourcesPlugin.getWorkspace();

	private static final String SDK_VERSION = "1.5"; //$NON-NLS-1$

	private static final String BIN_FOLDER = "bin"; //$NON-NLS-1$

	private static final String CLASS_EXTENSION = ".class"; //$NON-NLS-1$

	private static final String R_DRAWABLE_CLASS = "R$drawable" + CLASS_EXTENSION; //$NON-NLS-1$

	private static final String SETTINGS_FOLDER = ".settings"; //$NON-NLS-1$

	private static final String SETTINGS_FILE = "org.eclipse.core.resources.prefs"; //$NON-NLS-1$

	public static final String ANDROID_NATURE = "org.eclipse.andmore.AndroidNature"; //$NON-NLS-1$

	private static final int MAX_PATH_LENGTH = 255;

	/**
	 * Represents the Type of new Projects
	 */
	public static enum SourceTypes {
		SAMPLE, EXISTING, NEW, WIDGET
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#finalize()
	 */
	@Override
	public void finalize() throws Throwable {
		if (listener != null) {
			AndroidPlugin.getDefault().removeSDKLoaderListener(listener);
		}
		super.finalize();
	}

	private SourceTypes sourceType = SourceTypes.NEW;

	private Sample sample = null;

	private String location = null;

	private boolean useDefaultLocation = true;

	private boolean addNativeSupport = false;

	private boolean needToObfuscate = false;

	/**
	 * Return the Type of the new Project
	 * 
	 * @return
	 */
	public SourceTypes getSourceType() {
		return sourceType;
	}

	/**
	 * Change the Type of the new Project
	 * 
	 * @param sourceType
	 */
	public void setSourceType(SourceTypes sourceType) {
		this.sourceType = sourceType;
	}

	/**
	 * Check if adding native support
	 * 
	 * @return
	 */
	public boolean isAddingNativeSupport() {
		return addNativeSupport;
	}

	/**
	 * Set add native support property
	 * 
	 * @param hasNativeSupport
	 */
	public void setAddNativeSupport(boolean addNativeSupport) {
		this.addNativeSupport = addNativeSupport;
	}

	public void setNeedToObfuscate(boolean needToObfuscate) {
		this.needToObfuscate = needToObfuscate;
	}

	public boolean needToObfuscate() {
		return needToObfuscate;
	}

	/**
	 * Returns the Project location.
	 * 
	 * @see #isUsingDefaultLocation()
	 * @return
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Return the sample to use
	 * 
	 * @see #getSourceType()
	 * @return
	 */
	public Sample getSample() {
		return sample;
	}

	/**
	 * Change the used sample
	 * 
	 * @see #getSourceType()
	 * @param sample
	 */
	public void setSample(Sample sample) {
		this.sample = sample;
	}

	/**
	 * Change the Project Location
	 * 
	 * @param location
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * Check if using the default location
	 * 
	 * @return
	 */
	public boolean isUsingDefaultLocation() {
		return useDefaultLocation;
	}

	/**
	 * Set use default location property
	 * 
	 * @param useDefaultLocation
	 */
	public void setUseDefaultLocation(boolean useDefaultLocation) {
		this.useDefaultLocation = useDefaultLocation;
	}

	private String minSdkVersion = null;

	private String name = ""; //$NON-NLS-1$

	private String activityName = ""; //$NON-NLS-1$

	private String packageName = ""; //$NON-NLS-1$

	private IAndroidTarget sdkTarget = null;

	private List<String> sourceFolders = null;

	private boolean usingDefaultPackage = true;

	private final String PACKAGE_ROOT = "com."; //$NON-NLS-1$

	private Runnable listener = null;

	private String appName;

	/**
	 * Check if using the defaul package name
	 * 
	 * @return
	 */
	public boolean isUsingDefaultPackage() {
		return usingDefaultPackage;
	}

	/**
	 * Set if using the Default package name
	 * 
	 * @param usingDefaultPackage
	 */
	public void setUsingDefaultPackage(boolean usingDefaultPackage) {
		this.usingDefaultPackage = usingDefaultPackage;
	}

	/**
	 * Creates a new AndroidProject
	 */
	public AndroidProject() {
		// initialize SDK Targets
		if (SdkUtils.getCurrentSdk() == null) {
			// this listener will be called when a SDK is configured
			listener = new Runnable() {

				@Override
				public void run() {
					IAndroidTarget[] targets = SdkUtils.getAllTargets();
					if ((targets != null) && (targets.length > 0)) {
						int maxApiVersion = -1;
						for (IAndroidTarget aTarget : targets) {
							int apiVersion = aTarget.getVersion().getApiLevel();
							if (maxApiVersion < apiVersion) {
								sdkTarget = aTarget;
								maxApiVersion = apiVersion;
							}
						}
						// set the API string as the min SDK version - this is
						// the way ADT does
						minSdkVersion = sdkTarget.getVersion().getApiString();
					}
					AndroidPlugin.getDefault().removeSDKLoaderListener(listener);
					listener = null;
				}

			};
			AndroidPlugin.getDefault().addSDKLoaderListener(listener);
		} else {
			IAndroidTarget[] targets = SdkUtils.getAllTargets();
			if ((targets != null) && (targets.length > 0)) {
				int maxApiVersion = -1;
				for (IAndroidTarget aTarget : targets) {
					int apiVersion = aTarget.getVersion().getApiLevel();
					if (maxApiVersion < apiVersion) {
						sdkTarget = aTarget;
						maxApiVersion = apiVersion;
					}
				}
				// set the API string as the min SDK version - this is the way
				// ADT does
				minSdkVersion = sdkTarget.getVersion().getApiString();
			}
		}

		sourceFolders = new ArrayList(3);
		sourceFolders.add(IAndroidConstants.FD_SOURCES);
		sourceFolders.add(IAndroidConstants.FD_GEN_SOURCES);
	}

	/**
	 * Return the Default Package Name.
	 * 
	 * @return
	 */
	private String getDefaultPackageName() {
		return PACKAGE_ROOT + name2package(name);
	}

	/**
	 * Return the default name for activities.
	 * 
	 * @return
	 */
	public String getDefaultActivityName() {
		String activityName = name;
		activityName = activityName.replaceAll("^[0-9]+", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		activityName = activityName.replaceAll("[\\.]+", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		return activityName;
	}

	/**
	 * Returns a valid package name from the Project Name
	 * 
	 * @param name
	 * @return
	 */
	protected String name2package(String name) {
		String packageName = getDefaultActivityName().toLowerCase();
		packageName = packageName.replaceAll("[^A-Za-z0-9_]+", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		if (packageName.endsWith("_") && (packageName.length() > 1)) //$NON-NLS-1$
		{
			packageName = packageName.substring(0, packageName.length() - 1);
		}
		return packageName;
	}

	/**
	 * Return the Minimum SDK Version
	 * 
	 * @return
	 */
	public String getMinSdkVersion() {
		return minSdkVersion;
	}

	/**
	 * Returns <code>true</code> in case the SDK is a preview,
	 * <code>false</code> otherwise.
	 * 
	 * @return Returns <code>true</code> in case the SDK is a preview,
	 *         <code>false</code> otherwise.
	 */
	public boolean isSdkPreview() {
		return getSdkTarget().getVersion().isPreview();
	}

	/**
	 * Return the project Name
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the package name. Or the Default package name, if using it.
	 * 
	 * @return
	 */
	public String getPackageName() {
		String packageName;
		if (isUsingDefaultPackage()) {
			packageName = getDefaultPackageName();
		} else {
			packageName = this.packageName;
		}
		return packageName;
	}

	/**
	 * Return the SDK target used by this project.
	 * 
	 * @return
	 */
	public IAndroidTarget getSdkTarget() {
		return sdkTarget;
	}

	/**
	 * Return the Source Folder to be created in the project.
	 * 
	 * @return
	 */
	public List<String> getSourceFolders() {
		return sourceFolders;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.IWizardModel#getStatus()
	 */
	@Override
	public IStatus getStatus() {
		IStatus status = Status.OK_STATUS;
		// Validating Project Name
		if ((name == null) || (name.length() == 0)) {
			status = new AndroidStatus(IStatus.ERROR, AndroidNLS.ERR_AndroidProject_ProjectNameMustBeSpecified);
		} else {
			Pattern pattern = Pattern.compile("([A-Za-z0-9_\\.])+"); //$NON-NLS-1$
			Matcher matcher = pattern.matcher(name);

			if (!matcher.matches()) {
				String errMsg = NLS.bind(AndroidNLS.ERR_AndroidProject_InvalidProjectName, name);
				status = new AndroidStatus(IStatus.ERROR, errMsg);
			} else if (WORKSPACE.getRoot().getProject(name).exists()) {
				status = new AndroidStatus(IStatus.ERROR,
						AndroidNLS.ERR_AndroidProject_DuplicatedProjectNameInWorkspace);
			} else {
				status = WORKSPACE.validateName(name, IResource.PROJECT);
			}
		}

		if (status.isOK() && (sourceType == SourceTypes.EXISTING)) {

			// Check first if the path is empty. If true, we show a info
			// message to the user
			if (getLocation().length() == 0) {
				status = new AndroidStatus(IStatus.INFO, AndroidNLS.ERR_AndroidProject_EmptySourceLocation);
			} else {
				// Check if the manifest exists
				Path path = new Path(getLocation());
				String osPath = path.append(IAndroidConstants.FN_ANDROID_MANIFEST).toOSString();
				File manifest = new File(osPath);
				if (!manifest.exists() || !manifest.isFile()) {
					String errMsg = NLS.bind(AndroidNLS.ERR_AndroidProject_FileNotFoundError,
							IAndroidConstants.FN_ANDROID_MANIFEST, path.lastSegment());
					status = new AndroidStatus(IStatus.ERROR, errMsg);
				}
			}
		}

		// Validating Project Location
		if (status.isOK() && !isUsingDefaultLocation()) {
			IProject handle = WORKSPACE.getRoot().getProject(name);
			status = WORKSPACE.validateProjectLocation(handle, new Path(location));
			if (status.isOK() && isNewProject() && (getLocation() != null)) {
				boolean projectLocationIsEmpty = ProjectCreationSupport.validateNewProjectLocationIsEmpty(new Path(
						getLocation()));
				if (!projectLocationIsEmpty) {
					status = new AndroidStatus(IStatus.ERROR, AndroidNLS.UI_ProjectCreationSupport_NonEmptyFolder);
				}
			}
		}

		// Validating Project Location when adding Native Support on Windows
		if (status.isOK() && isAddingNativeSupport() && Platform.getOS().equals(Platform.OS_WIN32)) {
			if ((isUsingDefaultLocation() && ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString()
					.contains(" ")) //$NON-NLS-1$
					|| (!isUsingDefaultLocation() && location.contains(" "))) //$NON-NLS-1$
			{

				status = new AndroidStatus(IStatus.ERROR, AndroidNLS.ERR_AndroidProject_LocationContainsWhitespaces);
			}

		}

		// Validating if the project could create a "file name too long"
		// structure
		if (status.isOK()) {
			String basePath = isUsingDefaultLocation() ? getDefaultLocation() : getLocation();
			String relativePackagePath = getPackageName().replace('.', File.separatorChar);
			String binPath = basePath + File.separatorChar + BIN_FOLDER + File.separatorChar + relativePackagePath;

			String rDrawableBinClass = binPath + File.separatorChar + R_DRAWABLE_CLASS;

			String settingsFilePath = basePath + File.separatorChar + SETTINGS_FOLDER + File.separatorChar
					+ SETTINGS_FILE;

			String defaultClassBin = binPath + File.separator + getDefaultActivityName() + CLASS_EXTENSION;

			int maxFilenameLength = getMax(new int[] { rDrawableBinClass.length(), settingsFilePath.length(),
					defaultClassBin.length() });

			if (maxFilenameLength > MAX_PATH_LENGTH) {
				status = new AndroidStatus(IStatus.ERROR, AndroidNLS.ERR_AndroidProject_ProjectNameTooLong);
			}
		}

		// Validating SDK Target
		if (status.isOK() && (sdkTarget == null)) {
			status = new AndroidStatus(IStatus.ERROR, AndroidNLS.ERR_AndroidProject_ASdkTargetMustBeSpecified);
		}

		// Validates the application name
		if (status.isOK() && ((sourceType == SourceTypes.NEW) || (sourceType == SourceTypes.WIDGET))) {
			if (appName.trim().length() == 0) {
				status = new AndroidStatus(IStatus.WARNING, AndroidNLS.WARN_AndroidProject_ApplicationNameIsEmpty);
			}
			if (appName.contains("&")) //$NON-NLS-1$
			{
				status = new AndroidStatus(IStatus.ERROR, NLS.bind(
						AndroidNLS.ERR_AndroidProject_InvalidApplicationName, appName));
			}

		}

		// Validating PackageName
		if (status.isOK() && ((sourceType == SourceTypes.NEW) || (sourceType == SourceTypes.WIDGET))
				&& !isUsingDefaultPackage()) {
			if (getPackageName().length() == 0) {
				status = new AndroidStatus(IStatus.ERROR, AndroidNLS.ERR_AndroidProject_EmptyPackageName);
			} else if (status.isOK()) {
				status = JavaConventions.validatePackageName(getPackageName(), SDK_VERSION, SDK_VERSION);
			}

			if (status.isOK() && (getPackageName().indexOf('.') == -1)) {
				// The Android Activity Manager does not accept packages names
				// with only one
				// identifier. Check the package name has at least one dot in
				// them (the previous rule
				// validated that if such a dot exist, it's not the first nor
				// last characters of the
				// string.)
				status = new AndroidStatus(IStatus.ERROR, AndroidNLS.ERR_AndroidProject_InvalidPackageName);
			}

			if (status.isOK()) {
				Pattern pattern = Pattern.compile("[A-Za-z0-9_\\.]+"); //$NON-NLS-1$
				Matcher matcher = pattern.matcher(getPackageName());

				if (!matcher.matches()) {
					String errMsg = NLS.bind(AndroidNLS.ERR_AndroidProject_InvalidCharsInPackageName, getPackageName());

					status = new AndroidStatus(IStatus.ERROR, errMsg);
				}
			}
		}

		// validating activity name
		if ((this.getSourceType() == SourceTypes.NEW) || (this.getSourceType() == SourceTypes.WIDGET)) {
			if ((activityName == null) || (activityName.length() == 0)) {
				status = new AndroidStatus(IStatus.ERROR, AndroidNLS.ERR_AndroidProject_ActivityNameMustBeSpecified);
			} else {
				String onlyChars = "[A-Za-z_]"; //$NON-NLS-1$
				Pattern pattern = Pattern.compile("(" + onlyChars + ")(\\w)+"); //$NON-NLS-1$ //$NON-NLS-2$
				Matcher matcher = pattern.matcher(activityName);

				if (!matcher.matches()) {
					String errMsg = NLS.bind(AndroidNLS.ERR_AndroidProject_InvalidActivityName, activityName);
					status = new AndroidStatus(IStatus.ERROR, errMsg);
				}
			}
		}

		// Validating Min Sdk Version
		if (status.isOK() && ((sourceType == SourceTypes.NEW) || (sourceType == SourceTypes.WIDGET))) {
			// validate in case the sdk is preview
			if (isSdkPreview()) {
				String sdkAPI = getSdkTarget().getVersion().getApiString();
				if (!sdkAPI.equals(getMinSdkVersion())) {
					status = new AndroidStatus(IStatus.ERROR, NLS.bind(
							AndroidNLS.AndroidProject_MsgSDKVersionIsPreview, sdkAPI));
				}
			}
			// since it is not a preview, validate it normally
			else {
				int version = -1;
				try {
					// If not empty, it must be a valid integer > 0
					version = Integer.parseInt(getMinSdkVersion());
				} catch (NumberFormatException nfe) {
					status = new AndroidStatus(IStatus.ERROR, AndroidNLS.ERR_AndroidProject_InvalidSdkVersion);
				}

				if (status.isOK() && (version < 1)) {
					status = new AndroidStatus(IStatus.ERROR, AndroidNLS.ERR_AndroidProject_InvalidSdkVersion);
				}

				if (status.isOK() && (getSdkTarget() != null)) {
					if (getSdkTarget().getVersion().getApiLevel() > version) {
						status = new AndroidStatus(IStatus.WARNING, AndroidNLS.ERR_AndroidProject_InvalidApiLevel);
					} else if (getSdkTarget().getVersion().getApiLevel() < version) {
						status = new AndroidStatus(IStatus.ERROR,
								AndroidNLS.EXC_AndroidProject_InvalidMinimumSdkVersion);
					}
				}
			}
		}

		// validate if there are available samples
		if (status.isOK() && (sourceType == SourceTypes.SAMPLE) && (sdkTarget != null)
				&& (SdkUtils.getSamples(sdkTarget).length == 0)) {
			status = new AndroidStatus(IStatus.ERROR, AndroidNLS.EXC_AndroidProject_NoSamplesAvailable);
		}

		// Validating Project Location when adding Obfuscation Support
		if (status.isOK() && needToObfuscate()) {
			if ((isUsingDefaultLocation() && ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString()
					.contains(" ")) //$NON-NLS-1$
					|| (!isUsingDefaultLocation() && location.contains(" "))) //$NON-NLS-1$
			{

				status = new AndroidStatus(IStatus.WARNING,
						AndroidNLS.WRN_Obfuscation_ProjectLocationContainWhitespaces);
			}
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.IWizardModel#needMoreInformation()
	 */
	@Override
	public boolean needMoreInformation() {
		boolean needMoreInformation = (name == null) || (name.length() == 0)
				|| !WORKSPACE.validateName(name, IResource.PROJECT).isOK();

		if (!needMoreInformation) {
			boolean projectPathInformation = (location == null)
					|| ((location.length() == 0) || !WORKSPACE.validateProjectLocation(
							WORKSPACE.getRoot().getProject(name), new Path(location)).isOK());
			boolean newProjectInformation = !useDefaultLocation && projectPathInformation;
			switch (sourceType) {
			case NEW:
				needMoreInformation = newProjectInformation;
				break;
			case SAMPLE:
				needMoreInformation = newProjectInformation || (sample == null);
				break;
			case EXISTING:
				needMoreInformation = projectPathInformation;
				break;
			case WIDGET:
				needMoreInformation = newProjectInformation;
			}
		}

		if (!needMoreInformation) {
			if (((sourceType == SourceTypes.NEW) || (sourceType == SourceTypes.WIDGET)) && !isUsingDefaultPackage()) {
				needMoreInformation = (getPackageName().length() == 0)
						|| !JavaConventions.validatePackageName(getPackageName(), SDK_VERSION, SDK_VERSION).isOK()
						|| (getPackageName().indexOf('.') == -1);
			}
		}

		if (!needMoreInformation) {
			needMoreInformation = sdkTarget == null;
		}
		return needMoreInformation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.model.IWizardModel#save(org.eclipse.jface
	 * .wizard.IWizardContainer, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public boolean save(IWizardContainer container, IProgressMonitor monitor) {
		try {
			return ProjectCreationSupport.createProject(this, container);
		} catch (AndroidException e) {
			IStatus status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID, e.getLocalizedMessage());

			EclipseUtils.showErrorDialog(AndroidNLS.GEN_Error,
					AndroidNLS.EXC_AndroidProject_AnErrorHasOccurredWhenCreatingTheProject, status);
			return false;
		}
	}

	/**
	 * Change the SDK
	 * 
	 * @param minSdkVersion
	 */
	public void setMinSdkVersion(String minSdkVersion) {
		this.minSdkVersion = minSdkVersion;
	}

	/**
	 * Change the project name
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name.trim();
	}

	/**
	 * Change the package name. If using default package name will throw an
	 * {@link IllegalStateException}
	 * 
	 * @param packageName
	 */
	public void setPackageName(String packageName) {
		if (isUsingDefaultPackage()) {
			throw new IllegalStateException();
		}
		this.packageName = packageName.trim();
	}

	/**
	 * Change the SDK Target
	 * 
	 * @param sdkTarget
	 */
	public void setSdkTarget(IAndroidTarget sdkTarget) {
		this.sdkTarget = sdkTarget;
	}

	/**
	 * Change the Source Folder
	 * 
	 * @param sourceFolder
	 */
	public void setSourceFolder(List<String> sourceFolders) {
		this.sourceFolders.clear();
		this.sourceFolders.addAll(sourceFolders);
	}

	/**
	 * Return the Default location for this project
	 * 
	 * @return
	 */
	public String getDefaultLocation() {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() + File.separator + name;
	}

	/**
	 * Check if this project is a new project.
	 * 
	 * @return
	 */
	public boolean isNewProject() {
		return getSourceType() != SourceTypes.EXISTING;
	}

	/**
	 * Set the name of the activity being created with the project This
	 * attribute is only valid for New Projects. Other kind of project will
	 * ignore this attribute.
	 */
	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}

	/**
	 * Return this project default activity name
	 * 
	 * @return an Activity name as string
	 */
	public String getActivityName() {
		return this.activityName;
	}

	/**
	 * Retrieves the maximum value from an int array
	 * 
	 * @param values
	 *            the int array
	 * 
	 * @return the maximum value from the int array
	 */
	private int getMax(int[] values) {
		int max = values[0];

		for (int i = 1; i < values.length; i++) {
			max = Math.max(max, values[i]);
		}

		return max;
	}

	/**
	 * Set the application name to be used The app name will be used only when
	 * the project is new
	 * 
	 * @param app_name
	 */
	public void setApplicationName(String app_name) {
		this.appName = app_name;
	}

	/**
	 * get the user defined application name
	 * 
	 * @return
	 */
	public String getApplicationName() {
		return this.appName;
	}
}
