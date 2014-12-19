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
package org.eclipse.andmore.android.model.manifest.dom;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.core.runtime.IStatus;

/**
 * Class that represents an <activity> node on AndroidManifest.xml file
 */
public class ActivityNode extends AbstractBuildingBlockNode {
	static {
		defaultProperties.add(PROP_ALLOWTASKREPARENTING);
		defaultProperties.add(PROP_ALWAYSRETAINTASKSTATE);
		defaultProperties.add(PROP_CLEARTASKONLAUNCH);
		defaultProperties.add(PROP_CONFIGCHANGES);
		defaultProperties.add(PROP_EXCLUDEFROMRECENTS);
		defaultProperties.add(PROP_FINISHONTASKLAUNCH);
		defaultProperties.add(PROP_LAUNCHMODE);
		defaultProperties.add(PROP_MULTIPROCESS);
		defaultProperties.add(PROP_SCREENORIENTATION);
		defaultProperties.add(PROP_STATENOTNEEDED);
		defaultProperties.add(PROP_TASKAFFINITY);
		defaultProperties.add(PROP_THEME);
	}

	/**
	 * Enumeration for configChanges property
	 */
	public static enum ConfigChanges {
		mcc, mnc, locale, touchscreen, keyboard, keyboardHidden, navigation, orientation, fontscale
	}

	/**
	 * Enumeration for launchMode property
	 */
	public static enum LaunchMode {
		standard, singleTop, singleTask, singleInstance
	}

	/**
	 * Enumeration for screenOrientation property
	 */
	public static enum ScreenOrientation {
		unspecified, user, behind, landscape, portrait, sensor, nonsensor
	}

	/**
	 * Map to resolve the string<->enumeration association of configChanges
	 * property
	 */
	private static Map<String, ConfigChanges> configChanges;

	/**
	 * Map to resolve the string<->enumeration association of launchMode
	 * property
	 */
	private static Map<String, LaunchMode> launchModes;

	/**
	 * Map to resolve the string<->enumeration association of screenOrientation
	 * property
	 */
	private static Map<String, ScreenOrientation> screenOrientations;

	static {
		configChanges = new HashMap<String, ConfigChanges>();

		// Loads the map for configChanges
		for (ConfigChanges configChange : ConfigChanges.values()) {
			configChanges.put(configChange.toString().toLowerCase(), configChange);
		}

		launchModes = new HashMap<String, LaunchMode>();

		// Loads the map for launchMode
		for (LaunchMode launchMode : LaunchMode.values()) {
			launchModes.put(launchMode.toString().toLowerCase(), launchMode);
		}

		screenOrientations = new HashMap<String, ScreenOrientation>();

		// Loads the map for screenOrientation
		for (ScreenOrientation screenOrientation : ScreenOrientation.values()) {
			screenOrientations.put(screenOrientation.toString().toLowerCase(), screenOrientation);
		}
	}

	/**
	 * Gets the configChange parameter name from a given ConfigChanges
	 * enumeration value
	 * 
	 * @param configChange
	 *            the enumeration value
	 * @return the configChange parameter name
	 */
	public static String getConfigChangeName(ConfigChanges configChange) {
		String name = "";

		if (configChange != null) {
			name = configChange.toString();
		}

		return name;
	}

	/**
	 * Gets the enumeration value of the ConfigChanges enumeration from a given
	 * name
	 * 
	 * @param configChangeName
	 *            the configChanges name
	 * @return the enumeration value of the ConfigChanges enumeration
	 */
	public static ConfigChanges getConfigChangeFromName(String configChangeName) {
		ConfigChanges configChange = null;

		if (configChangeName != null) {
			String ccn = configChangeName.trim().toLowerCase();
			configChange = configChanges.get(ccn);
		}

		return configChange;
	}

	/**
	 * Gets the launchMode parameter name from a given LaunchMode enumeration
	 * value
	 * 
	 * @param launchMode
	 *            the enumeration value
	 * @return the LaunchMode parameter name
	 */
	public static String getLaunchModeName(LaunchMode launchMode) {
		String name = "";

		if (launchMode != null) {
			name = launchMode.toString();
		}

		return name;
	}

	/**
	 * Gets the enumeration value of the LaunchMode enumeration from a given
	 * name
	 * 
	 * @param launchModeName
	 *            the launchMode name
	 * @return the enumeration value of the LaunchMode enumeration
	 */
	public static LaunchMode getLaunchModeFromName(String launchModeName) {
		LaunchMode launchMode = null;

		if (launchModeName != null) {
			String lmn = launchModeName.trim().toLowerCase();
			launchMode = launchModes.get(lmn);
		}

		return launchMode;
	}

	/**
	 * Gets the screenOrientation parameter name from a given ScreenOrientation
	 * enumeration value
	 * 
	 * @param screenOrientation
	 *            the enumeration value
	 * @return the ScreenOrientation parameter name
	 */
	public static String getScreenOrientationName(ScreenOrientation screenOrientation) {
		String name = "";

		if (screenOrientation != null) {
			name = screenOrientation.toString();
		}

		return name;
	}

	/**
	 * Gets the enumeration value of the ScreenOrientation enumeration from a
	 * given name
	 * 
	 * @param screenOrientationName
	 *            the screenOrientation name
	 * @return the enumeration value of the ScreenOrientation enumeration
	 */
	public static ScreenOrientation getScreenOrientationFromName(String screenOrientationName) {
		ScreenOrientation screenOrientation = null;

		if (screenOrientationName != null) {
			String son = screenOrientationName.trim().toLowerCase();
			screenOrientation = screenOrientations.get(son);
		}

		return screenOrientation;
	}

	/**
	 * The allowTaskReparenting property
	 */
	private Boolean propAllowTaskReparenting = null;

	/**
	 * The alwaysRetainTaskState property
	 */
	private Boolean propAlwaysRetainTaskState = null;

	/**
	 * The clearTaskOnLaunch property
	 */
	private Boolean propClearTaskOnLaunch = null;

	/**
	 * The configChanges property (can hold more than one value)
	 */
	private final List<ConfigChanges> propConfigChanges = new LinkedList<ConfigChanges>();

	/**
	 * The excludeFromRecents property
	 */
	private Boolean propExcludeFromRecents = null;

	/**
	 * The finishOnTaskLaunch property
	 */
	private Boolean propFinishOnTaskLaunch = null;

	/**
	 * The launchMode property
	 */
	private LaunchMode propLaunchMode = null;

	/**
	 * The multiProcess property
	 */
	private Boolean propMultiprocess = null;

	/**
	 * The screenOrientation property
	 */
	private ScreenOrientation propScreenOrientation = null;

	/**
	 * The stateNotNeeded property
	 */
	private Boolean propStateNotNeeded = null;

	/**
	 * The taskAffinity property
	 */
	private String propTaskAffinity = null;

	/**
	 * The theme property
	 */
	private String propTheme = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            The name property. It must not be null.
	 */
	public ActivityNode(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * canContains
	 * (com.motorola.studio.android.model.manifest.dom.AndroidManifestNode
	 * .NodeType)
	 */
	@Override
	protected boolean canContains(NodeType nodeType) {
		return (nodeType == NodeType.IntentFilter) || (nodeType == NodeType.MetaData) || (nodeType == NodeType.Comment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.model.manifest.dom.AbstractIconLabelNameNode
	 * #addAdditionalProperties()
	 */
	@Override
	protected void addAdditionalProperties() {
		super.addAdditionalProperties();

		if (propAllowTaskReparenting != null) {
			properties.put(PROP_ALLOWTASKREPARENTING, propAllowTaskReparenting.toString());
		}

		if (propAlwaysRetainTaskState != null) {
			properties.put(PROP_ALWAYSRETAINTASKSTATE, propAlwaysRetainTaskState.toString());
		}

		if (propClearTaskOnLaunch != null) {
			properties.put(PROP_CLEARTASKONLAUNCH, propClearTaskOnLaunch.toString());
		}

		if ((propConfigChanges != null) && !propConfigChanges.isEmpty()) {
			String configChangesString = "";

			for (int i = 0; i < (propConfigChanges.size() - 1); i++) {
				configChangesString += getConfigChangeName(propConfigChanges.get(i)) + "|";
			}

			configChangesString += getConfigChangeName(propConfigChanges.get(propConfigChanges.size() - 1));

			properties.put(PROP_CONFIGCHANGES, configChangesString);
		}

		if (propExcludeFromRecents != null) {
			properties.put(PROP_EXCLUDEFROMRECENTS, propExcludeFromRecents.toString());
		}

		if (propFinishOnTaskLaunch != null) {
			properties.put(PROP_FINISHONTASKLAUNCH, propFinishOnTaskLaunch.toString());
		}

		if (propLaunchMode != null) {
			properties.put(PROP_LAUNCHMODE, getLaunchModeName(propLaunchMode));
		}

		if (propMultiprocess != null) {
			properties.put(PROP_MULTIPROCESS, propMultiprocess.toString());
		}

		if (propScreenOrientation != null) {
			properties.put(PROP_SCREENORIENTATION, getScreenOrientationName(propScreenOrientation));
		}

		if (propStateNotNeeded != null) {
			properties.put(PROP_STATENOTNEEDED, propStateNotNeeded.toString());
		}

		if (propTaskAffinity != null) {
			properties.put(PROP_TASKAFFINITY, propTaskAffinity);
		}

		if (propTheme != null) {
			properties.put(PROP_THEME, propTheme);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getNodeType()
	 */
	@Override
	public NodeType getNodeType() {
		return NodeType.Activity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * isNodeValid()
	 */
	@Override
	protected boolean isNodeValid() {
		return super.isNodeValid();
	}

	/**
	 * Gets the allowTaskReparenting property value
	 * 
	 * @return the allowTaskReparenting property value
	 */
	public Boolean getAllowTaskReparenting() {
		return propAllowTaskReparenting;
	}

	/**
	 * Sets the allowTaskReparenting property value. Set it to null to remove
	 * it.
	 * 
	 * @param allowTaskReparenting
	 *            the allowTaskReparenting property value
	 */
	public void setAllowTaskReparenting(Boolean allowTaskReparenting) {
		this.propAllowTaskReparenting = allowTaskReparenting;
	}

	/**
	 * Gets the alwaysRetainTaskState property value
	 * 
	 * @return the alwaysRetainTaskState property value
	 */
	public Boolean getAlwaysRetainTaskState() {
		return propAlwaysRetainTaskState;
	}

	/**
	 * Sets the alwaysRetainTaskState property value. Set it to null to remove
	 * it.
	 * 
	 * @param alwaysRetainTaskState
	 *            the alwaysRetainTaskState property value
	 */
	public void setAlwaysRetainTaskState(Boolean alwaysRetainTaskState) {
		this.propAlwaysRetainTaskState = alwaysRetainTaskState;
	}

	/**
	 * Gets the clearTaskOnLaunch property value
	 * 
	 * @return the clearTaskOnLaunch property value
	 */
	public Boolean getClearTaskOnLaunch() {
		return propClearTaskOnLaunch;
	}

	/**
	 * Sets the clearTaskOnLaunch property value. Set it to null to remove it.
	 * 
	 * @param clearTaskOnLaunch
	 *            the clearTaskOnLaunch property value
	 */
	public void setClearTaskOnLaunch(Boolean clearTaskOnLaunch) {
		this.propClearTaskOnLaunch = clearTaskOnLaunch;
	}

	/**
	 * Gets the configChanges property value as an array
	 * 
	 * @return the configChanges property value as an array
	 */
	public ConfigChanges[] getConfigChanges() {
		ConfigChanges[] configChanges = null;

		if (propConfigChanges != null) {
			configChanges = new ConfigChanges[propConfigChanges.size()];
			configChanges = propConfigChanges.toArray(configChanges);
		}

		return configChanges;
	}

	/**
	 * Adds a new config change to the configChanges property
	 * 
	 * @param configChanges
	 *            the new config change to be added
	 */
	public void addConfigChanges(ConfigChanges configChanges) {
		if (configChanges != null) {
			if (!propConfigChanges.contains(configChanges)) {
				propConfigChanges.add(configChanges);
			}
		}
	}

	/**
	 * Gets the excludeFromRecents property value
	 * 
	 * @return the excludeFromRecents property value
	 */
	public Boolean getExcludeFromRecents() {
		return propExcludeFromRecents;
	}

	/**
	 * Sets the excludeFromRecents property value. Set it to null to remove it.
	 * 
	 * @param excludeFromRecents
	 *            the excludeFromRecents property value
	 */
	public void setExcludeFromRecents(Boolean excludeFromRecents) {
		this.propExcludeFromRecents = excludeFromRecents;
	}

	/**
	 * Gets the finishOnTaskLaunch property value
	 * 
	 * @return the finishOnTaskLaunch property value
	 */
	public Boolean getFinishOnTaskLaunch() {
		return propFinishOnTaskLaunch;
	}

	/**
	 * Sets the finishOnTaskLaunch property value. Set it to null to remove it.
	 * 
	 * @param finishOnTaskLaunch
	 *            the finishOnTaskLaunch property value
	 */
	public void setFinishOnTaskLaunch(Boolean finishOnTaskLaunch) {
		this.propFinishOnTaskLaunch = finishOnTaskLaunch;
	}

	/**
	 * Gets the launchMode property value
	 * 
	 * @return the launchMode property value
	 */
	public LaunchMode getLaunchMode() {
		return propLaunchMode;
	}

	/**
	 * Sets the launchMode property value. Set it to null to remove it.
	 * 
	 * @param launchMode
	 *            the launchMode property value
	 */
	public void setLaunchMode(LaunchMode launchMode) {
		this.propLaunchMode = launchMode;
	}

	/**
	 * Gets the multiProcess property value
	 * 
	 * @return the multiProcess property value
	 */
	public Boolean getMultiprocess() {
		return propMultiprocess;
	}

	/**
	 * Sets the multiProcess property value. Set it to null to remove it.
	 * 
	 * @param multiProcess
	 *            the multiProcess property value
	 */
	public void setMultiprocess(Boolean multiProcess) {
		this.propMultiprocess = multiProcess;
	}

	/**
	 * Gets the screenOrientation property value
	 * 
	 * @return the screenOrientation property value
	 */
	public ScreenOrientation getScreenOrientation() {
		return propScreenOrientation;
	}

	/**
	 * Sets the screenOrientation property value. Set it to null to remove it.
	 * 
	 * @param screenOrientation
	 *            the screenOrientation property value
	 */
	public void setScreenOrientation(ScreenOrientation screenOrientation) {
		this.propScreenOrientation = screenOrientation;
	}

	/**
	 * Gets the stateNotNeeded property value
	 * 
	 * @return the stateNotNeeded property value
	 */
	public Boolean getStateNotNeeded() {
		return propStateNotNeeded;
	}

	/**
	 * Sets the stateNotNeeded property value. Set it to null to remove it.
	 * 
	 * @param stateNotNeeded
	 *            the stateNotNeeded property value
	 */
	public void setStateNotNeeded(Boolean stateNotNeeded) {
		this.propStateNotNeeded = stateNotNeeded;
	}

	/**
	 * Gets the taskAffinity property value
	 * 
	 * @return the taskAffinity property value
	 */
	public String getTaskAffinity() {
		return propTaskAffinity;
	}

	/**
	 * Sets the taskAffinity property value. Set it to null to remove it.
	 * 
	 * @param taskAffinity
	 *            the taskAffinity property value
	 */
	public void setTaskAffinity(String taskAffinity) {
		this.propTaskAffinity = taskAffinity;
	}

	/**
	 * Gets the theme property value
	 * 
	 * @return the theme property value
	 */
	public String getTheme() {
		return propTheme;
	}

	/**
	 * Sets the theme property value. Set it to null to remove it.
	 * 
	 * @param theme
	 *            the theme property value
	 */
	public void setTheme(String theme) {
		this.propTheme = theme;
	}

	/**
	 * Adds an Intent Filter Node to the Activity Node
	 * 
	 * @param intentFilter
	 *            The Intent Filter Node
	 */
	public void addIntentFilterNode(IntentFilterNode intentFilter) {
		if (intentFilter != null) {
			if (!children.contains(intentFilter)) {
				children.add(intentFilter);
			}
		}
	}

	/**
	 * Retrieves all Intent Filter Nodes from the Activity Node
	 * 
	 * @return all Intent Filter Nodes from the Activity Node
	 */
	public List<IntentFilterNode> getIntentFilterNodes() {
		List<IntentFilterNode> intentFilters = new LinkedList<IntentFilterNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.IntentFilter)) {
			intentFilters.add((IntentFilterNode) node);
		}

		return intentFilters;
	}

	/**
	 * Removes an Intent Filter Node from the Activity Node
	 * 
	 * @param intentFilter
	 *            the Intent Filter Node to be removed
	 */
	public void removeIntentFilterNode(IntentFilterNode intentFilter) {
		if (intentFilter != null) {
			children.remove(intentFilter);
		}
	}

	/**
	 * If parameter {@code isMainActivity} is true, then the intent-filters that
	 * represent main activities are added to this activity. If parameter
	 * {@code isMainActivity} is false, then the intent-filters that represent
	 * main activities are removed from this activity.
	 * 
	 * @param isMainActivity
	 *            True is this activity should be set as the main activity.
	 *            False if the activity should not be set as the main activity.
	 * @return True if the operation requested was successfully performed. False
	 *         otherwise. Possible scenarios are: This activity is the main
	 *         activity and {@code isMainActivity} is true. In this case, this
	 *         method returns true. This activity is the main activity and
	 *         {@code isMainActivity} is false. In this case, this method
	 *         returns true if the intent-filters were successfully removed from
	 *         this activity node, false otherwise. This activity is not the
	 *         main activity and {@code isMainActivity} is true. In this case,
	 *         this method returns true if the required intent-filters were
	 *         successfully added to this activity node, false otherwise. This
	 *         activity is not the main activity and {@code isMainActivity} is
	 *         false. In this case, this method returns true.
	 * */
	public boolean setAsMainActivity(boolean isMainActivity) {
		boolean result = false;

		// check if this this activity should be a main activity or not
		if (isMainActivity) {
			// set this activity to be a main activity
			if (this.isMainActivity()) {
				// this already is a main activity
				result = true;
			} else {
				// set as main activity
				IntentFilterNode intentFilterNode = new IntentFilterNode();
				ActionNode actionMainNode = new ActionNode(IAndroidConstants.ACTION_MAIN);
				CategoryNode categoryLauncherNode = new CategoryNode(IAndroidConstants.CATEGORY_LAUNCHER);

				intentFilterNode.addActionNode(actionMainNode);
				intentFilterNode.addCategoryNode(categoryLauncherNode);
				this.addIntentFilterNode(intentFilterNode);

				result = true;
			}
		} else {
			// unset this activity as main activity
			if (!this.isMainActivity()) {
				// this activity is not a main activity
				result = true;
			} else {
				// unset this activity as main activity, i.e., remove action
				// main and category launcher from its intent-filters
				for (IntentFilterNode intentFilterNode : getIntentFilterNodes()) {
					ActionNode actionMainNode = intentFilterNode.getActionNode(IAndroidConstants.ACTION_MAIN);
					CategoryNode categoryLauncherNode = intentFilterNode
							.getCategoryNode(IAndroidConstants.CATEGORY_LAUNCHER);

					if ((actionMainNode != null) && (categoryLauncherNode != null)) {
						// effectivelly remove the nodes
						intentFilterNode.removeActionNode(actionMainNode);
						intentFilterNode.removeCategoryNode(categoryLauncherNode);

						if (intentFilterNode.isEmpty()) {
							// also remove the intent-filter node if it was left
							// empty
							this.removeIntentFilterNode(intentFilterNode);
						}

						result = true;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Check if this activity is set as the main activity, i.e., if it has the
	 * required intent-filter that represent main activities.
	 * 
	 * @return True if this activity is the main activity of the project. False
	 *         otherwise.
	 * */
	public boolean isMainActivity() {
		boolean isMainActivity = false;
		boolean hasActionMain = false;
		boolean hasCategoryLauncher = false;

		// iterate over intent-filter nodes
		// looking for action and category nodes that represents main activities
		for (IntentFilterNode intent : getIntentFilterNodes()) {
			hasActionMain = false;
			hasCategoryLauncher = false;

			// iterate over action nodes
			for (ActionNode actionNode : intent.getActionNodes()) {
				if (actionNode.getName().equals(IAndroidConstants.ACTION_MAIN)) {
					// action node for main activities
					hasActionMain = true;
					break;
				}
			}

			// iterate over category nodes
			for (CategoryNode categoryNode : intent.getCategoryNodes()) {
				if (categoryNode.getName().equals(IAndroidConstants.CATEGORY_LAUNCHER)) {
					// category node for main activities
					hasCategoryLauncher = true;
				}
			}

			// check if the intent-filter has the action and category nodes that
			// represent main activities
			if (hasActionMain && hasCategoryLauncher) {
				isMainActivity = true;
				break;
			}
		}

		return isMainActivity;
	}

	/**
	 * Adds a Metadata Node to the Activity Node
	 * 
	 * @param metadata
	 *            The Metadata Node
	 */
	public void addMetadataNode(MetadataNode metadata) {
		if (metadata != null) {
			if (!children.contains(metadata)) {
				children.add(metadata);
			}
		}
	}

	/**
	 * Retrieves all Metadata Nodes from the Activity Node
	 * 
	 * @return all Metadata Nodes from the Activity Node
	 */
	public List<MetadataNode> getMetadataNodes() {
		List<MetadataNode> metadatas = new LinkedList<MetadataNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.MetaData)) {
			metadatas.add((MetadataNode) node);
		}

		return metadatas;
	}

	/**
	 * Removes a Metadata Node from the Activity Node
	 * 
	 * @param metadata
	 *            the Metadata Node to be removed
	 */
	public void removeMetadataNode(MetadataNode metadata) {
		if (metadata != null) {
			children.remove(metadata);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getSpecificNodeErrors()
	 */
	@Override
	protected List<IStatus> getSpecificNodeProblems() {
		return null;
	}
}
