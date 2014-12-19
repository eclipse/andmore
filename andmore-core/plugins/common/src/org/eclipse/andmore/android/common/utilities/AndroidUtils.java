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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.andmore.android.common.CommonPlugin;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.Bundle;

public class AndroidUtils {
	private static final String CLASS_COM_ANDROID_IDE_ECLIPSE_ADT_INTERNAL_SDK_ANDROID_TARGET_DATA = "org.eclipe.andmore.internal.sdk.AndroidTargetData"; //$NON-NLS-1$

	private static final String CLASS_COM_ANDROID_SDKLIB_I_ANDROID_TARGET = "com.android.sdklib.IAndroidTarget"; //$NON-NLS-1$

	private static final String CLASS_COM_ANDROID_SDKLIB_ANDROID_VERSION = "com.android.sdklib.AndroidVersion"; //$NON-NLS-1$

	private static final String CLASS_COM_ANDROID_IDE_ECLIPSE_ADT_INTERNAL_SDK_SDK = "org.eclipe.andmore.internal.sdk.Sdk"; //$NON-NLS-1$

	// Constants
	private static final String ANDROID_VERSION_API_LEVEL = "AndroidVersion.ApiLevel"; //$NON-NLS-1$

	private static final String SOURCE_PROPERTIES = "source.properties"; //$NON-NLS-1$

	private static final String ANDROID_JAR = "android.jar"; //$NON-NLS-1$

	private static final String PLATFORMS = "platforms"; //$NON-NLS-1$

	private static final String ANDROID = "android-"; //$NON-NLS-1$

	private static final String TARGET = "target"; //$NON-NLS-1$

	private static final String DEFAULT_PROPERTIES = "default.properties"; //$NON-NLS-1$

	private static final String PROJECT_PROPERTIES = "project.properties"; //$NON-NLS-1$

	/*
	 * Contains the exceptions to the general rule (that prepends
	 * android.permission. to the permissionName)
	 */
	private static final Map<String, String> permissionNameToPrefixToAppend = new HashMap<String, String>();

	static {
		permissionNameToPrefixToAppend.put("SET_ALARM", "com.android.alarm.permission"); //$NON-NLS-1$ //$NON-NLS-2$
		permissionNameToPrefixToAppend.put("READ_HISTORY_BOOKMARKS", //$NON-NLS-1$
				"com.android.browser.permission"); //$NON-NLS-1$
		permissionNameToPrefixToAppend.put("WRITE_HISTORY_BOOKMARKS", //$NON-NLS-1$
				"com.android.browser.permission"); //$NON-NLS-1$
		permissionNameToPrefixToAppend.put("ADD_VOICEMAIL", "com.android.voicemail.permission"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Gets Android Sdk set in the preference page
	 * 
	 * @return
	 */
	public static String getSDKPathByPreference() {
		IEclipsePreferences pref = InstanceScope.INSTANCE.getNode("org.eclipe.andmore"); //$NON-NLS-1$
		return pref.get("org.eclipe.andmore.sdk", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Gets android jar from the project
	 * 
	 * @param projectDir
	 * @return path to android.jar
	 * @throws AndroidException
	 */
	public static File getAndroidJar(File projectDir) throws AndroidException {
		File androidTargetFolder = getAndroidTargetPathForProject(projectDir);
		return new File(androidTargetFolder, "android.jar"); //$NON-NLS-1$
	}

	/**
	 * Returns all available intent filter permissions from a given project
	 * 
	 * @return String array containing the available permissions
	 */
	public static String[] getIntentFilterPermissions(IProject project) {
		String[] attributeValues = new String[0];

		if ((project != null) && project.isOpen()) {
			try {
				// reflection for: Sdk sdk = Sdk.getCurrent();
				Class<?> clsSdk = Class.forName(CLASS_COM_ANDROID_IDE_ECLIPSE_ADT_INTERNAL_SDK_SDK);
				Method mtdGetCurrent = clsSdk.getMethod("getCurrent", (Class[]) null); //$NON-NLS-1$
				Object sdk = mtdGetCurrent.invoke(null, (Object[]) null);

				// reflection for: IAndroidTarget target =
				// sdk.getTarget(project);
				Method mtdGetTarget = clsSdk.getMethod("getTarget", IProject.class); //$NON-NLS-1$
				Object target = mtdGetTarget.invoke(sdk, project);

				// reflection for: AndroidTargetData targetData =
				// sdk.getTargetData(target);
				Class<?> interfaceIAndroidTarget = Class.forName(CLASS_COM_ANDROID_SDKLIB_I_ANDROID_TARGET);
				Method mtdGetTargetData = clsSdk.getMethod("getTargetData", interfaceIAndroidTarget); //$NON-NLS-1$
				Object targetData = mtdGetTargetData.invoke(sdk, target);

				if (targetData != null) {
					//reflection for: attributeValues = targetData.getAttributeValues("uses-permission", "android:name"); //$NON-NLS-1$ //$NON-NLS-2$
					Class<?> clsAndroidTargetData = Class
							.forName(CLASS_COM_ANDROID_IDE_ECLIPSE_ADT_INTERNAL_SDK_ANDROID_TARGET_DATA);
					Method mtdGetAttributeValues = clsAndroidTargetData.getMethod("getAttributeValues", String.class, //$NON-NLS-1$
							String.class);
					attributeValues = (String[]) mtdGetAttributeValues.invoke(targetData, "uses-permission", //$NON-NLS-1$
							"android:name"); //$NON-NLS-1$
				}
			} catch (Exception e) {
				StudioLogger.warn("It was not possible to reach ADT methods (reflection break)", e.getMessage());
				try {
					attributeValues = getIntentFilterPermissions().toArray(attributeValues);
				} catch (IOException e1) {
					StudioLogger.error(UtilitiesNLS.AndroidUtils_NotPossibleToReachPermissionsFile_Error,
							e1.getMessage());
				}
				EclipseUtils.showWarningDialog(UtilitiesNLS.AndroidUtils_ERROR_GETINTENTPERMISSIONSBYREFLECTION_TITLE,
						UtilitiesNLS.AndroidUtils_ERROR_GETINTENTPERMISSIONSBYREFLECTION_MESSAGE);
			}
		}

		return attributeValues;
	}

	/**
	 * Retrieves the path to platform/$target$, where $target$ is defined inside
	 * default.properties/project.properties file
	 * 
	 * @param projectDir
	 * @return file with path to target folder
	 * @throws AndroidException
	 *             problem to read default.properties/project.properties
	 */
	public static File getAndroidTargetPathForProject(File projectDir) throws AndroidException {
		File androidTarget = null;
		Properties properties = new Properties();
		// changed from default.properties to project.properties after R14
		File defaultPropertiesFile = new File(projectDir, PROJECT_PROPERTIES);
		if (!defaultPropertiesFile.exists()) {
			// WARNING: do not remove statement below assigning
			// default.properties file to keep compatibility with projects
			// created with ADTs before R14
			defaultPropertiesFile = new File(projectDir, DEFAULT_PROPERTIES);
		}
		if (defaultPropertiesFile.exists()) {
			FileInputStream fileInputStream = null;
			try {
				fileInputStream = new FileInputStream(defaultPropertiesFile);
				properties.load(fileInputStream);
			} catch (IOException e) {
				throw new AndroidException(UtilitiesNLS.AndroidUtils_ErrorReadingDefaultPropertiesFile, e);
			} finally {
				if (fileInputStream != null) {
					try {
						fileInputStream.close();
					} catch (Exception e) {
						// Do nothing.
					}
				}
			}
			if (properties.containsKey(TARGET)) {
				String targetValue = properties.getProperty(TARGET);
				if (targetValue != null) {
					if (!targetValue.startsWith(ANDROID)) {
						try {
							// add-on => <name>:<model>:<version>
							int colonIndex = targetValue.lastIndexOf(":"); //$NON-NLS-1$
							if (colonIndex >= 0) {
								targetValue = ANDROID + targetValue.substring(colonIndex + 1);
							}
						} catch (Exception e) {
							// Do nothing.
						}
					}
				}

				boolean androidSdkPreferenceDefined = !getSDKPathByPreference().equals(""); //$NON-NLS-1$
				String sdkPath = getSdkPath(androidSdkPreferenceDefined);

				if (sdkPath != null) {
					// found sdk path
					androidTarget = new File(sdkPath + File.separator + PLATFORMS + File.separator + targetValue);
					if (!androidTarget.exists()) {
						// if not found the exact version, then look for one
						// version of android that is greater than target value
						// (and retrieve platform folder)
						File baseFolder = new File(sdkPath + File.separator + PLATFORMS);
						File[] androidPlatforms = baseFolder.listFiles();
						boolean foundJar = false;
						if (androidPlatforms.length > 0) {
							for (File androidPlatform : androidPlatforms) {
								File sourcePropsFile = new File(androidPlatform, SOURCE_PROPERTIES);
								File jar = new File(androidPlatform, ANDROID_JAR);
								if (sourcePropsFile.exists() && jar.exists()) {
									Properties sourceProperties = new Properties();
									try {
										fileInputStream = new FileInputStream(sourcePropsFile);
										sourceProperties.load(fileInputStream);
									} catch (IOException e) {
										throw new AndroidException(
												UtilitiesNLS.AndroidUtils_ErrorReadingDefaultPropertiesFile, e);
									} finally {
										if (fileInputStream != null) {
											try {
												fileInputStream.close();
											} catch (Exception e) {
												// Do nothing.
											}
										}
									}
									// platform api level
									String apiLevel = sourceProperties.getProperty(ANDROID_VERSION_API_LEVEL);
									int index = targetValue.indexOf("-"); //$NON-NLS-1$
									if ((index >= 0) && (apiLevel != null)) {
										// project target declared
										String versionName = targetValue.substring(index + 1);
										try {
											Integer version = Integer.valueOf(versionName);
											Integer apiLevelVersion = Integer.valueOf(apiLevel);
											if (apiLevelVersion >= version) {
												// found a compatible platform
												foundJar = true;
												androidTarget = androidPlatform;
												break;
											}
										} catch (NumberFormatException nfe) {
											// ignore this folder (add-on or
											// preview)
										}
									}
								}
							}
							if (!foundJar) {
								throw new AndroidException(androidTarget.getAbsolutePath()
										+ UtilitiesNLS.AndroidUtils_ERROR_SDK_TARGETPLATFORM_NOTFOUND);
							}
						}
					}
				} else {
					throw new AndroidException(UtilitiesNLS.AndroidUtils_ERROR_SDKPATHNOTFOUND);
				}
			}
		} else {
			throw new AndroidException(UtilitiesNLS.AndroidUtils_ERROR_DEFAULTPROPERTIESNOTFOUND);
		}
		return androidTarget;
	}

	/**
	 * Returns the path for Android SDK
	 * 
	 * @param preferenceDefined
	 *            true if ADT preference for Android SDK is defined (take sdk
	 *            path from it), false otherwise (take sdk path from environment
	 *            variable)
	 * @return resolved path to Android SDK
	 */
	private static String getSdkPath(boolean preferenceDefined) {
		String sdkPath = null;
		if (!preferenceDefined) {
			// sdk was not defined - take from environment variable
			String pathVariable = System.getenv("PATH"); //$NON-NLS-1$
			String pathSeparator = System.getProperty("path.separator"); //$NON-NLS-1$
			String subPath = null;
			File checkedPath = null;
			String[] folderList = null;

			StringTokenizer token = new StringTokenizer(pathVariable, pathSeparator);
			while (token.hasMoreTokens()) {
				subPath = token.nextToken();
				checkedPath = new File(subPath);
				if (checkedPath.isDirectory()) {
					folderList = checkedPath.list();
					for (String s : folderList) {
						if (s.equals("emulator") || s.equals("emulator.exe") || s.equals("adb") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								|| s.equals("adb.exe")) //$NON-NLS-1$
						{
							File root = checkedPath.getParentFile();
							sdkPath = root.getAbsolutePath();
							break;
						}
					}
				}
			}

		} else {
			// sdk was defined on execution
			sdkPath = getSDKPathByPreference();
		}
		return sdkPath;
	}

	/**
	 * Retrieves all activity actions from a given {@link IProject}.
	 * 
	 * @param project
	 *            The android project.
	 * @return An {@link String} array containing all the activity actions from
	 *         the given project.
	 * @throws AndroidException
	 *             if an error occurred while attempting to get the activity
	 *             actions.
	 */
	public static String[] getActivityActions(IProject project) throws AndroidException {
		String[] activityActions = new String[1];
		File androidTargetFile = AndroidUtils.getAndroidTargetPathForProject(project.getLocation().toFile());
		TargetDataReader targetDataReader = new TargetDataReader(androidTargetFile);
		try {
			activityActions = targetDataReader.getActivityActions().toArray(activityActions);
		} catch (IOException e) {
			throw new AndroidException(e);
		}
		return activityActions;
	}

	/**
	 * Retrieves all receiver actions from a given {@link IProject}.
	 * 
	 * @param project
	 *            The android project.
	 * @return An {@link String} array containing all the receiver actions from
	 *         the given project.
	 * @throws AndroidException
	 *             if an error occurred while attempting to get the receiver
	 *             actions.
	 */
	public static String[] getReceiverActions(IProject project) throws AndroidException {
		String[] receiverActions = new String[1];
		File androidTargetFile = AndroidUtils.getAndroidTargetPathForProject(project.getLocation().toFile());
		TargetDataReader targetDataReader = new TargetDataReader(androidTargetFile);
		try {
			receiverActions = targetDataReader.getReceiverActions().toArray(receiverActions);
		} catch (IOException e) {
			throw new AndroidException(e);
		}
		return receiverActions;
	}

	/**
	 * Retrieves all intent filter categories from a given {@link IProject}.
	 * 
	 * @param project
	 *            The android project.
	 * @return An {@link String} array containing all the intent filter
	 *         categories from the given project.
	 * @throws AndroidException
	 *             if an error occurred while attempting to get the intent
	 *             filter categories.
	 */
	public static String[] getIntentFilterCategories(IProject project) throws AndroidException {
		String[] intentFilterCategories = new String[1];
		File androidTargetFile = AndroidUtils.getAndroidTargetPathForProject(project.getLocation().toFile());
		TargetDataReader targetDataReader = new TargetDataReader(androidTargetFile);
		try {
			intentFilterCategories = targetDataReader.getIntentFilterCategories().toArray(intentFilterCategories);
		} catch (IOException e) {
			throw new AndroidException(e);
		}
		return intentFilterCategories;
	}

	/**
	 * Retrieves all service actions from a given {@link IProject}.
	 * 
	 * @param project
	 *            The android project.
	 * @return An {@link String} array containing all the service actions from
	 *         the given project.
	 * @throws AndroidException
	 *             if an error occurred while attempting to get the service
	 *             actions.
	 */
	public static String[] getServiceActions(IProject project) throws AndroidException {
		String[] serviceActions = new String[1];
		File androidTargetFile = AndroidUtils.getAndroidTargetPathForProject(project.getLocation().toFile());
		TargetDataReader targetDataReader = new TargetDataReader(androidTargetFile);
		try {
			serviceActions = targetDataReader.getServiceActions().toArray(serviceActions);
		} catch (IOException e) {
			throw new AndroidException(e);
		}
		return serviceActions;
	}

	/**
	 * Get the api version number for a given project. This method accesses ADT
	 * via reflection. If an error occurred an AndroidException will be thrown.
	 * 
	 * @param project
	 *            : the project
	 * @return the api version number or 0 if some error occurs
	 * @throws Exception
	 *             if any error occurred while attempting to access the ADT via
	 *             reflection.
	 */
	public static int getApiVersionNumberForProject(IProject project) throws AndroidException {
		int api = 0;

		try {
			// reflection for: Sdk sdk = Sdk.getCurrent();
			Class<?> clsSdk = Class.forName(CLASS_COM_ANDROID_IDE_ECLIPSE_ADT_INTERNAL_SDK_SDK);
			Method mtdGetCurrent = clsSdk.getMethod("getCurrent", (Class[]) null); //$NON-NLS-1$
			Object sdk = mtdGetCurrent.invoke(null, (Object[]) null);

			// reflection for: IAndroidTarget target = sdk.getTarget(project);
			Method mtdGetTarget = clsSdk.getMethod("getTarget", IProject.class); //$NON-NLS-1$
			Object target = mtdGetTarget.invoke(sdk, project);

			// reflection for: AndroidVersion version =
			// target.getVersion(target);
			Class<?> clsAndroidVersion = Class.forName(CLASS_COM_ANDROID_SDKLIB_ANDROID_VERSION);

			Class<?> interfaceIAndroidTarget = Class.forName(CLASS_COM_ANDROID_SDKLIB_I_ANDROID_TARGET);
			Method mtdGetVersion = interfaceIAndroidTarget.getMethod("getVersion", (Class[]) null); //$NON-NLS-1$

			Object version = mtdGetVersion.invoke(target, (Object[]) null);

			if (version != null) {
				// reflection for: version.getApiLevel();
				Method mtdGetApiLevel = clsAndroidVersion.getMethod("getApiLevel", (Class[]) null); //$NON-NLS-1$
				Object apiLevel = mtdGetApiLevel.invoke(version, (Object[]) null);
				api = (Integer) apiLevel;
			}
		} catch (Exception e) {
			StudioLogger.info("It was not possible to reach ADT methods (reflection break)", e.getMessage());
			throw new AndroidException(UtilitiesNLS.AndroidUtils_NotPossibleToGetAPIVersionNumber_Error);
		}
		return api;
	}

	/**
	 * Reads permissions.txt file inside plugin and creates the list of
	 * permissions available in the target (the list is based on the class
	 * <code>Manifest.Permission</code> from Android)
	 * 
	 * <br>
	 * <br>
	 * It appends android.permission. to the items, the unique exceptions are:
	 * -com.android.alarm.permission.SET_ALARM
	 * -com.android.browser.permission.READ_HISTORY_BOOKMARKS
	 * -com.android.browser.permission.WRITE_HISTORY_BOOKMARKS
	 * -com.android.voicemail.permission.ADD_VOICEMAIL
	 * 
	 * @see http
	 *      ://developer.android.com/reference/android/Manifest.permission.html
	 * @return list of Intent Filters Permissions available
	 * @throws IOException
	 *             if file not found, or if there is any problem reading the
	 *             permissions file
	 */
	private static List<String> getIntentFilterPermissions() throws IOException {
		// path inside <plugin>\files\permissions.txt
		URL permissionsURL = null;

		Bundle bundle = Platform.getBundle(CommonPlugin.PLUGIN_ID);
		permissionsURL = bundle.getEntry((new StringBuilder(IPath.SEPARATOR)).append(
				"files" + IPath.SEPARATOR + "permissions.txt") //$NON-NLS-1$ //$NON-NLS-2$
				.toString());

		List<String> items = new ArrayList<String>();
		InputStream is = null;
		BufferedReader bufferedReader = null;
		try {
			if (permissionsURL != null) {
				is = permissionsURL.openStream();
			}

			if (is != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(is));
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					String prefix = ""; //$NON-NLS-1$
					if (!permissionNameToPrefixToAppend.containsKey(line.trim())) {
						// general rule - append android.permission.
						prefix = "android.permission"; //$NON-NLS-1$
					} else {
						// exception rule - append the prefix available in the
						// map
						prefix = permissionNameToPrefixToAppend.get(line.trim());
					}
					items.add(prefix + "." + line.trim()); //$NON-NLS-1$
				}
			}
		} finally {
			if (is != null) {
				is.close();
			}
			if (bufferedReader != null) {
				bufferedReader.close();
			}
		}
		return items;
	}
}
