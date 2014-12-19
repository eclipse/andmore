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
package org.eclipse.andmore.android.model.manifest.parser;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.andmore.android.model.manifest.dom.AbstractBuildingBlockNode;
import org.eclipse.andmore.android.model.manifest.dom.AbstractIconLabelNameNode;
import org.eclipse.andmore.android.model.manifest.dom.AbstractSimpleNameNode;
import org.eclipse.andmore.android.model.manifest.dom.ActionNode;
import org.eclipse.andmore.android.model.manifest.dom.ActivityAliasNode;
import org.eclipse.andmore.android.model.manifest.dom.ActivityNode;
import org.eclipse.andmore.android.model.manifest.dom.ApplicationNode;
import org.eclipse.andmore.android.model.manifest.dom.CategoryNode;
import org.eclipse.andmore.android.model.manifest.dom.CommentNode;
import org.eclipse.andmore.android.model.manifest.dom.DataNode;
import org.eclipse.andmore.android.model.manifest.dom.GrantUriPermissionNode;
import org.eclipse.andmore.android.model.manifest.dom.IAndroidManifestProperties;
import org.eclipse.andmore.android.model.manifest.dom.InstrumentationNode;
import org.eclipse.andmore.android.model.manifest.dom.IntentFilterNode;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.andmore.android.model.manifest.dom.MetadataNode;
import org.eclipse.andmore.android.model.manifest.dom.PermissionGroupNode;
import org.eclipse.andmore.android.model.manifest.dom.PermissionNode;
import org.eclipse.andmore.android.model.manifest.dom.PermissionTreeNode;
import org.eclipse.andmore.android.model.manifest.dom.ProviderNode;
import org.eclipse.andmore.android.model.manifest.dom.ReceiverNode;
import org.eclipse.andmore.android.model.manifest.dom.ServiceNode;
import org.eclipse.andmore.android.model.manifest.dom.UnknownNode;
import org.eclipse.andmore.android.model.manifest.dom.UsesFeatureNode;
import org.eclipse.andmore.android.model.manifest.dom.UsesLibraryNode;
import org.eclipse.andmore.android.model.manifest.dom.UsesPermissionNode;
import org.eclipse.andmore.android.model.manifest.dom.UsesSDKNode;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Comment;
import org.w3c.dom.NamedNodeMap;

/**
 * Abstract class that contains methods to parse the AndroidManifest.xml file
 * nodes
 */
abstract class AndroidManifestNodeParser implements IAndroidManifestProperties {
	/**
	 * Errors when parsing
	 */
	protected final List<String> parseErrors;

	/**
	 * Default constructor
	 */
	public AndroidManifestNodeParser() {
		parseErrors = new LinkedList<String>();
	}

	/**
	 * Parses a <manifest> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a ManifestNode object corresponding to the node
	 */
	protected ManifestNode parseManifestNode(NamedNodeMap attributes) {
		ManifestNode manifestNode = new ManifestNode("");
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_PACKAGE)) {
				manifestNode.setPackage(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_SHAREDUSERID)) {
				manifestNode.setSharedUserId(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_VERSIONCODE)) {
				Integer versionCode;

				try {
					versionCode = Integer.parseInt(attrValue);
				} catch (NumberFormatException nfe) {
					versionCode = 1;
					String errMsg = NLS.bind(UtilitiesNLS.ERR_AndroidManifestNodeParser_ErrorParsingVersionCode,
							attrValue, versionCode.toString());
					parseErrors.add(errMsg);
					StudioLogger.error(AndroidManifestNodeParser.class, errMsg, nfe);
				}

				manifestNode.setVersionCode(versionCode);
			} else if (attrName.equalsIgnoreCase(PROP_VERSIONNAME)) {
				manifestNode.setVersionName(attrValue);
			} else if (!attrName.equalsIgnoreCase(PROP_XMLNS)) {
				manifestNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return manifestNode;
	}

	/**
	 * Parses an <uses-permission> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a UsesPermissionNode object corresponding to the node
	 */
	protected UsesPermissionNode parseUsesPermissionNode(NamedNodeMap attributes) {
		UsesPermissionNode usesPermissionNode = new UsesPermissionNode("");
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_NAME)) {
				usesPermissionNode.setName(attrValue);
			} else {
				usesPermissionNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return usesPermissionNode;
	}

	/**
	 * Parses a <permission> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a PermissionNode object corresponding to the node
	 */
	protected PermissionNode parsePermissionNode(NamedNodeMap attributes) {
		PermissionNode permissionNode = new PermissionNode("");
		String attrName, attrValue;

		parseAbstractIconLabelNameNode(attributes, permissionNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_DESCRIPTION)) {
				permissionNode.setDescription(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PERMISSIONGROUP)) {
				permissionNode.setPermissionGroup(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PROTECTIONLEVEL)) {
				permissionNode.setProtectionLevel(PermissionNode.getProtectionLevel(attrValue));
			} else {
				permissionNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return permissionNode;
	}

	/**
	 * Parses a <permission-tree> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a PermissionTreeNode object corresponding to the node
	 */
	protected PermissionTreeNode parsePermissionTreeNode(NamedNodeMap attributes) {
		PermissionTreeNode permissionTreeNode = new PermissionTreeNode("");
		String attrName, attrValue;

		parseAbstractIconLabelNameNode(attributes, permissionTreeNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();
			permissionTreeNode.addUnknownProperty(attrName, attrValue);
		}

		return permissionTreeNode;
	}

	/**
	 * Parses a <permission-group> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a PermissionGroupNode object corresponding to the node
	 */
	protected PermissionGroupNode parsePermissionGroupNode(NamedNodeMap attributes) {
		PermissionGroupNode permissionGroupNode = new PermissionGroupNode("");
		String attrName, attrValue;

		parseAbstractIconLabelNameNode(attributes, permissionGroupNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_DESCRIPTION)) {
				permissionGroupNode.setDescription(attrValue);
			} else {
				permissionGroupNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return permissionGroupNode;
	}

	/**
	 * Parses an <instrumentation> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return an InstrumentationNode object corresponding to the node
	 */
	protected InstrumentationNode parseInstrumentationNode(NamedNodeMap attributes) {
		InstrumentationNode instrumentationNode = new InstrumentationNode("");
		String attrName, attrValue;

		parseAbstractIconLabelNameNode(attributes, instrumentationNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_FUNCTIONALTEST)) {
				Boolean functionalTest = Boolean.parseBoolean(attrValue);
				instrumentationNode.setFunctionalTest(functionalTest);
			} else if (attrName.equalsIgnoreCase(PROP_HANDLEPROFILING)) {
				Boolean handleProfiling = Boolean.parseBoolean(attrValue);
				instrumentationNode.setHandleProfiling(handleProfiling);
			} else if (attrName.equalsIgnoreCase(PROP_TARGETPACKAGE)) {
				instrumentationNode.setTargetPackage(attrValue);
			} else {
				instrumentationNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return instrumentationNode;
	}

	/**
	 * Parses an <uses-sdk> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return an UsesNode object corresponding to the node
	 */
	protected UsesSDKNode parseUsesSdkNode(NamedNodeMap attributes) {
		UsesSDKNode usesSDKNode = new UsesSDKNode();
		String attrName, attrValue;
		String minSdkVersion;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_MINSDKVERSION)) {
				minSdkVersion = attrValue;
				usesSDKNode.setMinSdkVersion(minSdkVersion);
			} else if (attrName.equalsIgnoreCase(PROP_MAXSDKVERSION)) {
				usesSDKNode.setPropMaxSdkVersion(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_TARGETSDKVERSION)) {
				usesSDKNode.setPropTargetSdkVersion(attrValue);
			} else {
				usesSDKNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return usesSDKNode;
	}

	/**
	 * Parses an <application> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return an ApplicationNode object corresponding to the node
	 */
	protected ApplicationNode parseApplicationNode(NamedNodeMap attributes) {
		ApplicationNode applicationNode = new ApplicationNode("");
		String attrName, attrValue;

		parseAbstractIconLabelNameNode(attributes, applicationNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_ALLOWCLEARUSERDATA)) {
				Boolean allowClearUserData = Boolean.parseBoolean(attrValue);
				applicationNode.setAllowClearUserData(allowClearUserData);
			} else if (attrName.equalsIgnoreCase(PROP_ALLOWTASKREPARENTING)) {
				Boolean allowTaskReparentig = Boolean.parseBoolean(attrValue);
				applicationNode.setAllowTaskReparenting(allowTaskReparentig);
			} else if (attrName.equalsIgnoreCase(PROP_DEBUGGABLE)) {
				Boolean debbugable = Boolean.parseBoolean(attrValue);
				applicationNode.setDebuggable(debbugable);
			} else if (attrName.equalsIgnoreCase(PROP_DESCRIPTION)) {
				applicationNode.setDescription(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_ENABLED)) {
				Boolean enabled = Boolean.parseBoolean(attrValue);
				applicationNode.setEnabled(enabled);
			} else if (attrName.equalsIgnoreCase(PROP_HASCODE)) {
				Boolean hasCode = Boolean.parseBoolean(attrValue);
				applicationNode.setHasCode(hasCode);
			} else if (attrName.equalsIgnoreCase(PROP_MANAGESPACEACTIVITY)) {
				applicationNode.setManageSpaceActivity(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PERMISSION)) {
				applicationNode.setPermission(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PERSISTENT)) {
				Boolean persistent = Boolean.parseBoolean(attrValue);
				applicationNode.setPersistent(persistent);
			} else if (attrName.equalsIgnoreCase(PROP_PROCESS)) {
				applicationNode.setProcess(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_TASKAFFINITY)) {
				applicationNode.setTaskAffinity(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_THEME)) {
				applicationNode.setTheme(attrValue);
			} else {
				applicationNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return applicationNode;
	}

	/**
	 * Parses an <activity> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return an ActivityNode object corresponding to the node
	 */
	protected ActivityNode parseActivityNode(NamedNodeMap attributes) {
		ActivityNode activityNode = new ActivityNode("");
		String attrName, attrValue;

		parseAbstractBuildingBlockNode(attributes, activityNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_ALLOWTASKREPARENTING)) {
				Boolean allowTaskReparentig = Boolean.parseBoolean(attrValue);
				activityNode.setAllowTaskReparenting(allowTaskReparentig);
			} else if (attrName.equalsIgnoreCase(PROP_ALWAYSRETAINTASKSTATE)) {
				Boolean alwaysRetainTaskState = Boolean.parseBoolean(attrValue);
				activityNode.setAlwaysRetainTaskState(alwaysRetainTaskState);
			} else if (attrName.equalsIgnoreCase(PROP_CLEARTASKONLAUNCH)) {
				Boolean clearTaskOnLaunch = Boolean.parseBoolean(attrValue);
				activityNode.setClearTaskOnLaunch(clearTaskOnLaunch);
			} else if (attrName.equalsIgnoreCase(PROP_CONFIGCHANGES)) {
				String configChanges[] = attrValue.split("\\|");

				for (String configChange : configChanges) {
					activityNode.addConfigChanges(ActivityNode.getConfigChangeFromName(configChange));
				}
			} else if (attrName.equalsIgnoreCase(PROP_EXCLUDEFROMRECENTS)) {
				Boolean excludeFromRecents = Boolean.parseBoolean(attrValue);
				activityNode.setExcludeFromRecents(excludeFromRecents);
			} else if (attrName.equalsIgnoreCase(PROP_FINISHONTASKLAUNCH)) {
				Boolean finishOnTaskLaunch = Boolean.parseBoolean(attrValue);
				activityNode.setFinishOnTaskLaunch(finishOnTaskLaunch);
			} else if (attrName.equalsIgnoreCase(PROP_LAUNCHMODE)) {
				activityNode.setLaunchMode(ActivityNode.getLaunchModeFromName(attrValue));
			} else if (attrName.equalsIgnoreCase(PROP_MULTIPROCESS)) {
				Boolean multiprocess = Boolean.parseBoolean(attrValue);
				activityNode.setMultiprocess(multiprocess);
			} else if (attrName.equalsIgnoreCase(PROP_SCREENORIENTATION)) {
				activityNode.setScreenOrientation(ActivityNode.getScreenOrientationFromName(attrValue));
			} else if (attrName.equalsIgnoreCase(PROP_STATENOTNEEDED)) {
				Boolean stateNotNeeded = Boolean.parseBoolean(attrValue);
				activityNode.setStateNotNeeded(stateNotNeeded);
			} else if (attrName.equalsIgnoreCase(PROP_TASKAFFINITY)) {
				activityNode.setTaskAffinity(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_THEME)) {
				activityNode.setTheme(attrValue);
			} else {
				activityNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return activityNode;
	}

	/**
	 * Parses an <intent-filter> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return an IntentFilterNode object corresponding to the node
	 */
	protected IntentFilterNode parseIntentFilterNode(NamedNodeMap attributes) {
		IntentFilterNode intentFilterNode = new IntentFilterNode();
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_ICON)) {
				intentFilterNode.setIcon(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_LABEL)) {
				intentFilterNode.setLabel(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PRIORITY)) {
				try {
					Integer priority = Integer.parseInt(attrValue);
					intentFilterNode.setPriority(priority);
				} catch (NumberFormatException nfe) {
					String errMsg = NLS
							.bind(UtilitiesNLS.ERR_AndroidManifestNodeParser_ErrorParsingPriority, attrValue);
					parseErrors.add(errMsg);
					StudioLogger.error(AndroidManifestNodeParser.class, errMsg, nfe);
				}
			} else {
				intentFilterNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return intentFilterNode;
	}

	/**
	 * Parses an <action> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return an ActionNode object corresponding to the node
	 */
	protected ActionNode parseActionNode(NamedNodeMap attributes) {
		ActionNode actionNode = new ActionNode("");
		String attrName, attrValue;

		parseAbstractSimpleNameNode(attributes, actionNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			actionNode.addUnknownProperty(attrName, attrValue);
		}

		return actionNode;
	}

	/**
	 * Parses a <category> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a CategoryNode object corresponding to the node
	 */
	protected CategoryNode parseCategoryNode(NamedNodeMap attributes) {
		CategoryNode categoryNode = new CategoryNode("");
		String attrName, attrValue;

		parseAbstractSimpleNameNode(attributes, categoryNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			categoryNode.addUnknownProperty(attrName, attrValue);
		}

		return categoryNode;
	}

	/**
	 * Parses a <data> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a DataNode object corresponding to the node
	 */
	protected DataNode parseDataNode(NamedNodeMap attributes) {
		DataNode dataNode = new DataNode();
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_HOST)) {
				dataNode.setHost(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_MIMETYPE)) {
				dataNode.setMimeType(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PATH)) {
				dataNode.setPath(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PATHPATTERN)) {
				dataNode.setPathPattern(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PATHPREFIX)) {
				dataNode.setPathPrefix(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PORT)) {
				dataNode.setPort(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_SCHEME)) {
				dataNode.setScheme(attrValue);
			} else {
				dataNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return dataNode;
	}

	/**
	 * Parses a <metadata> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a MetadataNode object corresponding to the node
	 */
	protected MetadataNode parseMetadataNode(NamedNodeMap attributes) {
		MetadataNode metadataNode = new MetadataNode("");
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_NAME)) {
				metadataNode.setName(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_RESOURCE)) {
				metadataNode.setResource(attrValue);
			} else if (attrName.equals(PROP_VALUE)) {
				metadataNode.setValue(attrValue);
			} else {
				metadataNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return metadataNode;
	}

	/**
	 * Parses an <activity-alias> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return an ActivityNode object corresponding to the node
	 */
	protected ActivityAliasNode parseActivityAliasNode(NamedNodeMap attributes) {
		ActivityAliasNode activityAliasNode = new ActivityAliasNode("", "");
		String attrName, attrValue;

		parseAbstractIconLabelNameNode(attributes, activityAliasNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_ENABLED)) {
				Boolean enabled = Boolean.parseBoolean(attrValue);
				activityAliasNode.setEnabled(enabled);
			} else if (attrName.equalsIgnoreCase(PROP_EXPORTED)) {
				Boolean exported = Boolean.parseBoolean(attrValue);
				activityAliasNode.setExported(exported);
			} else if (attrName.equalsIgnoreCase(PROP_PERMISSION)) {
				activityAliasNode.setPermission(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_TARGETACTIVITY)) {
				activityAliasNode.setTargetActivity(attrValue);
			} else {
				activityAliasNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return activityAliasNode;
	}

	/**
	 * Parses a <service> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a ServiceNode object corresponding to the node
	 */
	protected ServiceNode parseServiceNode(NamedNodeMap attributes) {
		ServiceNode serviceNode = new ServiceNode("");
		String attrName, attrValue;

		parseAbstractBuildingBlockNode(attributes, serviceNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			serviceNode.addUnknownProperty(attrName, attrValue);
		}

		return serviceNode;
	}

	/**
	 * Parses a <receiver> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a ReceiverNode object corresponding to the node
	 */
	protected ReceiverNode parseReceiverNode(NamedNodeMap attributes) {
		ReceiverNode receiverNode = new ReceiverNode("");
		String attrName, attrValue;

		parseAbstractBuildingBlockNode(attributes, receiverNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			receiverNode.addUnknownProperty(attrName, attrValue);
		}

		return receiverNode;
	}

	/**
	 * Parses a <provider> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a ProviderNode object corresponding to the node
	 */
	protected ProviderNode parseProviderNode(NamedNodeMap attributes) {
		ProviderNode providerNode = new ProviderNode("", "");
		String attrName, attrValue;

		parseAbstractBuildingBlockNode(attributes, providerNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_AUTHORITIES)) {
				String[] authorities = attrValue.split(";");

				for (String authority : authorities) {
					providerNode.addAuthority(authority);
				}

				providerNode.removeAuthority("");
			} else if (attrName.equalsIgnoreCase(PROP_GRANTURIPERMISSIONS)) {
				Boolean grantUriPermissions = Boolean.parseBoolean(attrValue);
				providerNode.setGrantUriPermissions(grantUriPermissions);
			} else if (attrName.equalsIgnoreCase(PROP_INITORDER)) {
				try {
					Integer initOrder = Integer.parseInt(attrValue);
					providerNode.setInitOrder(initOrder);
				} catch (NumberFormatException nfe) {
					String errMsg = NLS.bind(UtilitiesNLS.ERR_AndroidManifestNodeParser_ErrorParsingInitOrder,
							attrValue);
					parseErrors.add(errMsg);
					StudioLogger.error(AndroidManifestNodeParser.class, errMsg, nfe);
				}
			} else if (attrName.equalsIgnoreCase(PROP_MULTIPROCESS)) {
				Boolean multiprocess = Boolean.parseBoolean(attrValue);
				providerNode.setMultiprocess(multiprocess);
			} else if (attrName.equalsIgnoreCase(PROP_READPERMISSION)) {
				providerNode.setReadPermission(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_SYNCABLE)) {
				Boolean syncable = Boolean.parseBoolean(attrValue);
				providerNode.setSyncable(syncable);
			} else if (attrName.equalsIgnoreCase(PROP_WRITEPERMISSION)) {
				providerNode.setWritePermission(attrValue);
			} else {
				providerNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return providerNode;
	}

	/**
	 * Parses a <grant-uri-permission> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a GrantUriPermissionNode object corresponding to the node
	 */
	protected GrantUriPermissionNode parseGrantUriPermissionNode(NamedNodeMap attributes) {
		GrantUriPermissionNode grantUriPermissionNode = new GrantUriPermissionNode();
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_PATH)) {
				grantUriPermissionNode.setPath(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PATHPATTERN)) {
				grantUriPermissionNode.setPathPattern(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PATHPREFIX)) {
				grantUriPermissionNode.setPathPrefix(attrValue);
			} else {
				grantUriPermissionNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return grantUriPermissionNode;
	}

	/**
	 * Parses an <uses-library> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return an UsesLibraryNode object corresponding to the node
	 */
	protected UsesLibraryNode parseUsesLibraryNode(NamedNodeMap attributes) {
		UsesLibraryNode usesLibraryNode = new UsesLibraryNode("");
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_NAME)) {
				usesLibraryNode.setName(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_REQUIRED)) {
				usesLibraryNode.setRequired(Boolean.parseBoolean(attrValue));
			} else {
				usesLibraryNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return usesLibraryNode;
	}

	/**
	 * Parses an <uses-feature> node
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return an UsesFeatureNode object corresponding to the node
	 */
	protected UsesFeatureNode parseUsesFeatureNode(NamedNodeMap attributes) {
		UsesFeatureNode usesFeatureNode = new UsesFeatureNode("");
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_NAME)) {
				usesFeatureNode.setName(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_REQUIRED)) {
				usesFeatureNode.setRequired(Boolean.parseBoolean(attrValue));
			} else {
				usesFeatureNode.addUnknownProperty(attrName, attrValue);
			}
		}

		return usesFeatureNode;
	}

	/**
	 * Parses a comment node (<!-- This is a comment -->)
	 * 
	 * @param comment
	 *            The xml comment node
	 * 
	 * @return a CommentNode object representing the xml comment node
	 */
	protected CommentNode parseCommentNode(Comment comment) {
		CommentNode commentNode = new CommentNode();
		commentNode.setComment(comment.getTextContent());

		return commentNode;
	}

	/**
	 * Creates an unknown node based on its attributes. An node is classified as
	 * unknown when it is not specified by the AndroidManifest.xml file
	 * specification or when it is under another node that could not contain it.
	 * 
	 * @param attributes
	 *            The node attributes
	 * @return a UnknownNode object corresponding to the node
	 */
	protected UnknownNode parseUnknownNode(String nodeName, NamedNodeMap attributes) {
		UnknownNode unknownNode = new UnknownNode(nodeName);
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			unknownNode.addUnknownProperty(attrName, attrValue);
		}

		return unknownNode;
	}

	/**
	 * Sets all attributes relative to an AbstractIconLabelNameNode from an
	 * attributes list
	 * 
	 * @param attributes
	 *            The node attributes
	 * @param amNode
	 *            The AbstractIconLabelNameNode where the attributes must be in
	 */
	private void parseAbstractIconLabelNameNode(NamedNodeMap attributes, AbstractIconLabelNameNode amNode) {
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_ICON)) {
				amNode.setIcon(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_LABEL)) {
				amNode.setLabel(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_NAME)) {
				amNode.setName(attrValue);
			}
		}
	}

	/**
	 * Sets all attributes relative to an AbstractBuildingBlockNode from an
	 * attributes list
	 * 
	 * @param attributes
	 *            The node attributes
	 * @param amNode
	 *            The AbstractBuildingBlockNode where the attributes must be in
	 */
	private void parseAbstractBuildingBlockNode(NamedNodeMap attributes, AbstractBuildingBlockNode amNode) {
		String attrName, attrValue;
		Boolean boolValue;

		parseAbstractIconLabelNameNode(attributes, amNode);

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_ENABLED)) {
				boolValue = Boolean.parseBoolean(attrValue);
				amNode.setEnabled(boolValue);
			} else if (attrName.equalsIgnoreCase(PROP_EXPORTED)) {
				boolValue = Boolean.parseBoolean(attrValue);
				amNode.setExported(boolValue);
			} else if (attrName.equalsIgnoreCase(PROP_PERMISSION)) {
				amNode.setPermission(attrValue);
			} else if (attrName.equalsIgnoreCase(PROP_PROCESS)) {
				amNode.setProcess(attrValue);
			}
		}
	}

	/**
	 * Sets all attributes relative to an AbstractSimpleNameNode from an
	 * attributes list
	 * 
	 * @param attributes
	 *            The node attributes
	 * @param amNode
	 *            The AbstractSimpleNameNode where the attributes must be in
	 */
	private void parseAbstractSimpleNameNode(NamedNodeMap attributes, AbstractSimpleNameNode amNode) {
		String attrName, attrValue;

		for (int i = 0; i < attributes.getLength(); i++) {
			attrName = attributes.item(i).getNodeName().trim();
			attrValue = attributes.item(i).getNodeValue();

			if (attrName.equalsIgnoreCase(PROP_NAME)) {
				amNode.setName(attrValue);
			}
		}
	}
}
