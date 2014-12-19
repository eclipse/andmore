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
package org.eclipse.andmore.android.emulator.device.instance.options;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class provides methods to manage startup options
 * 
 */
public class StartupOptionsMgt implements IStartupOptionsConstants {

	/**
	 * List of all startup options groups (the startup options themselves will
	 * be accessed through the use of a method in the group object that returns
	 * the startup options in that group)
	 */
	private static List<StartupOptionsGroup> startupOptionsGroupsList = null;

	/**
	 * List of all startup options, indexed by their names, for fast access
	 */
	private static Map<String, StartupOption> startupOptionsMap = new HashMap<String, StartupOption>();

	/*
	 * Load the startup options / groups list
	 */
	static {
		load();
	}

	/**
	 * Get the startup options groups list
	 * 
	 * @return startup options groups list
	 */
	public static List<StartupOptionsGroup> getStartupOptionsGroupsList() {
		return startupOptionsGroupsList;
	}

	/**
	 * Read all groups and startup options available for editing from a XML and
	 * stores the information in the correspondent beans
	 */
	public static void load() {

		try {
			// Clear startup options groups list
			startupOptionsGroupsList = new ArrayList<StartupOptionsGroup>();

			// Define XML path
			InputStream xmlStream = EmulatorPlugin.getDefault().getBundle().getEntry(STARTUP_OPTIONS_XML_PATH)
					.openStream();

			// Load XML
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(xmlStream);

			/*
			 * Iterate through Startup Groups
			 */
			Element rootNode = document.getDocumentElement();
			NodeList startupOptionsGroups = rootNode.getElementsByTagName(GROUP_TAG);
			for (int i = 0; i < startupOptionsGroups.getLength(); i++) {
				/*
				 * Create group
				 */
				Element group = (Element) startupOptionsGroups.item(i);

				String strKey = group.getAttributeNode(GROUP_TAG_ID).getNodeValue();
				strKey = Platform.getResourceString(EmulatorPlugin.getDefault().getBundle(), strKey);

				StartupOptionsGroup startupOptionsGroup = new StartupOptionsGroup(strKey);
				startupOptionsGroup.setTitle(startupOptionsGroup.getId());

				/*
				 * Iterate through Startup Options in this group
				 */
				NodeList startupOptions = group.getElementsByTagName(STARTUP_OPT_TAG);
				startupOptionsGroup.setStartupOptions(new ArrayList<StartupOption>()); // clear
																						// startup
																						// options
				for (int j = 0; j < startupOptions.getLength(); j++) {
					/*
					 * Create startup option
					 */
					Element option = (Element) startupOptions.item(j);
					StartupOption startupOption = new StartupOption(option.getAttributeNode(STARTUP_OPT_TAG_NAME)
							.getNodeValue(), getStartupOptionType(option.getAttributeNode(STARTUP_OPT_TAG_TYPE)
							.getNodeValue())); // name and type
					strKey = option.getAttributeNode(STARTUP_OPT_TAG_FRIENDLY_NAME).getNodeValue();

					strKey = Platform.getResourceString(EmulatorPlugin.getDefault().getBundle(), strKey);

					startupOption.setUserFriendlyName(strKey); // friendly name

					strKey = option.getElementsByTagName(STARTUP_OPT_TAG_DESCRIPTION).item(0).getTextContent();

					strKey = Platform.getResourceString(EmulatorPlugin.getDefault().getBundle(), strKey);

					startupOption.setDescription(strKey); // description

					if (option.getAttributeNode(STARTUP_OPT_TAG_TYPE_DETAILS) != null) {
						startupOption.setTypeDetails(option.getAttributeNode(STARTUP_OPT_TAG_TYPE_DETAILS)
								.getNodeValue()); // type details
					}
					// Iterate through startup option pre-defined values, if any
					NodeList preDefinedValuesContainer = option.getElementsByTagName(PREDEFINED_VALUES_TAG);
					startupOption.setPreDefinedValues(new ArrayList<String>()); // clear
																				// pre-defined
																				// values
					if (preDefinedValuesContainer.getLength() > 0) {
						NodeList preDefinedValues = ((Element) preDefinedValuesContainer.item(0))
								.getElementsByTagName(PREDEFINED_VALUE_TAG);
						for (int k = 0; k < preDefinedValues.getLength(); k++) {
							// Add pre-defined values to the option
							Element preDefinedValue = (Element) preDefinedValues.item(k);
							startupOption.getPreDefinedValues().add(preDefinedValue.getTextContent());
						}
					}

					/*
					 * Add startup options to the group
					 */
					startupOptionsGroup.getStartupOptions().add(startupOption);

					startupOptionsMap.put(startupOption.getName(), startupOption);

				}

				/*
				 * Add groups to the groups list
				 */
				startupOptionsGroupsList.add(startupOptionsGroup);
			}

		} catch (Exception e) {
			StudioLogger.error("Failed to load startup options");
		}

	}

	/**
	 * Validate the values assigned for the startup options marked as checked
	 * (the ones that are being used), according to its type and type details.
	 * 
	 * @return Status object with the result of the validation
	 */
	public static Status validate() {
		Status status = (Status) Status.OK_STATUS;
		String msg = null;

		/*
		 * Iterate through Startup Groups
		 */
		for (StartupOptionsGroup group : getStartupOptionsGroupsList()) {
			/*
			 * Iterate through Startup Options in this group
			 */
			for (StartupOption startupOption : group.getStartupOptions()) {
				/*
				 * Check if the Startup Option is checked
				 */
				if (startupOption.isChecked() && (status.isOK())) {

					String name = startupOption.getName(); // startup option
															// name
					String ufname = startupOption.getUserFriendlyName(); // user-friendly
																			// startup
																			// option
																			// name
					String value = startupOption.getValue(); // startup option
																// value
					String typeDetails = startupOption.getTypeDetails(); // startup
																			// option
																			// type
																			// detail

					/*
					 * General validation: no quotes in values
					 */
					if ((!startupOption.getName().equals(OTHERS_OTHER)) && (value.indexOf("\"") >= 0)) {
						msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_NoQuotes, ufname);
					} else {

						/*
						 * Call the appropriate validation method
						 */
						switch (startupOption.getType()) {
						case TYPE_TEXT:
							msg = validateTextField(name, ufname, value, typeDetails);
							break;

						case TYPE_NUMBER:
							msg = validadeNumberField(name, ufname, value, typeDetails);
							break;

						case TYPE_PATH:
							msg = validadePathField(name, ufname, value, typeDetails);
							break;
						}
					}

					/*
					 * If some validation has failed, return with an error
					 * message
					 */
					if (msg != null) {
						status = new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID, msg);
						break;
					}

				}
			}
		}

		return status;

	}

	/**
	 * Validate the startup option value for an startup option of "text" type
	 * 
	 * @param name
	 *            the startup option name
	 * @param ufname
	 *            the user-friendly startup option name
	 * @param value
	 *            the current assigned value for the startup option
	 * @param typeDetails
	 *            any special requirements that the assigned value must be match
	 * @return null if the value assigned for the startup option is a valid one
	 *         or an error message otherwise
	 */
	private static String validateTextField(String name, String ufName, String value, String typeDetails) {
		String msg = null;

		// Check if the value is blank
		if ((value == null) || (value.equals(""))) {
			msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_TextBlank, ufName);
		}

		return msg;

	}

	/**
	 * Validate the startup option value for an startup option of "number" type
	 * 
	 * @param name
	 *            the startup option name
	 * @param ufname
	 *            the user-friendly startup option name
	 * @param value
	 *            the current assigned value for the startup option
	 * @param typeDetails
	 *            any special requirements that the assigned value must be match
	 * @return null if the value assigned for the startup option is a valid one
	 *         or an error message otherwise
	 */
	private static String validadeNumberField(String name, String ufName, String value, String typeDetails) {
		String msg = null;

		// Check if the value is blank
		if ((value == null) || (value.equals(""))) {
			msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_NumberRequired, ufName);
		} else {

			try {
				/*
				 * Check if it's an Integer. If it's not, an exception will be
				 * thrown
				 */
				int intValue = Integer.parseInt(value);

				/*
				 * Check if it's positive
				 */
				if (intValue < 0) {
					msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_NumberMustBePositiveInteger,
							ufName);
				} else {

					/*
					 * Check if the value is in the correct range
					 */
					if (typeDetails != null) {
						String[] valueRange = typeDetails.split(";");
						if ((intValue < Integer.parseInt(valueRange[0]))
								|| (intValue > Integer.parseInt(valueRange[1]))) {
							// the value is not in the correct range
							msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_NumberIntRange,
									new String[] { ufName, valueRange[0], valueRange[1] });
						}
					}
				}

			} catch (NumberFormatException ex) {
				// it's not a number
				msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_NumberMustBeInteger, ufName);
			}
		}

		return msg;

	}

	/**
	 * Validate the startup option value for an startup option of "path" type
	 * 
	 * @param name
	 *            the startup option name
	 * @param ufname
	 *            the user-friendly startup option name
	 * @param value
	 *            the current assigned value for the startup option
	 * @param typeDetails
	 *            any special requirements that the assigned value must be match
	 * @return null if the value assigned for the startup option is a valid one
	 *         or an error message otherwise
	 */
	private static String validadePathField(String name, String ufName, String value, String typeDetails) {
		String msg = null;

		if ((value == null) || (value.equals(""))) {
			msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_PathRequired, ufName);
		} else {

			File file = new File(value);

			/*
			 * Validate folder
			 */
			if (typeDetails.equals(TYPE_PATH_DIR)) {
				/*
				 * Check if the path exists
				 */
				if (!file.exists()) {
					// the folder doesn't exist
					msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_PathDirNotExist, ufName);
				} else {
					if (file.isFile()) {
						// it's not a folder
						msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_PathMustBeDir, ufName);
					}
				}
			}
			/*
			 * Validate file
			 */
			else {
				if (!file.exists()) {
					// the file doesn't exist
					msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_PathFileNotExist, ufName);
				} else {
					// it's not a file
					if (file.isDirectory()) {
						msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_PathMustBeFile, ufName);
					}
					// it doesn't have the correct extension
					else {
						if (!typeDetails.equals("." + (new Path(value)).getFileExtension())) {
							msg = NLS.bind(EmulatorNLS.ERR_PropertiesMainComposite_StartupOpt_PathIncorrectFileType,
									new String[] { ufName, typeDetails });
						}
					}
				}
			}
		}
		return msg;

	}

	/**
	 * Generates the list of parameters that shall to be sent to the Emulator
	 * 
	 * @return the list of parameters that shall to be sent to the Emulator
	 */
	public static String getParamList() {
		String paramList = "";

		/*
		 * Iterate through Startup Groups
		 */
		for (StartupOptionsGroup group : getStartupOptionsGroupsList()) {
			/*
			 * Iterate through Startup Options in this group
			 */
			int startupOptionType;
			for (StartupOption startupOption : group.getStartupOptions()) {
				startupOptionType = startupOption.getType();
				if (startupOption.isChecked()) // check if the startup option is
												// being used
				{
					if (startupOptionType == TYPE_NONE) {
						paramList += ((paramList.equals("")) ? "" : " ") + startupOption.getName();
					} else {
						if ((startupOption.getName().equals(OTHERS_OTHER))) {

							paramList += ((paramList.equals("")) ? "" : " ") + startupOption.getValue();

						} else {
							String value = startupOption.getValue();

							if (Platform.getOS().equals(Platform.OS_WIN32)) {
								if (value.contains(" ")) {
									value = "\"" + value + "\"";
								}
							} else {
								if (value.contains("\\")) {
									value = value.replace("\\", "\\\\");
								}

								if (value.contains(" ")) {
									value = value.replace(" ", "\\ ");
								}
							}

							paramList += ((paramList.equals("")) ? "" : " ") + startupOption.getName()
									+ (value.trim().length() > 0 ? " " + value : "");
						}
					}
				}
			}
		}

		return paramList;

	}

	/**
	 * Load values from a Properties object
	 * 
	 * @param properties
	 *            properties object containing the values that must be loaded
	 *            into de model
	 */
	private static void loadValues(Properties properties) {
		/*
		 * Iterate through Startup Groups
		 */
		for (StartupOptionsGroup group : getStartupOptionsGroupsList()) {
			/*
			 * Iterate through Startup Options in this group
			 */
			String soValue = "";
			for (StartupOption startupOption : group.getStartupOptions()) {
				soValue = properties.getProperty(startupOption.getName());
				if (soValue != null) {
					startupOption.setChecked(true);
					startupOption.setValue(soValue);
				} else {
					startupOption.setChecked(false);
					startupOption.setValue("");
				}
				startupOption.updateUI();
			}
		}

	}

	/**
	 * Create a properties object with the information contained in a command
	 * line
	 * 
	 * @param commandLine
	 *            the command line used to start the emulator
	 * @return properties object with the information contained in a command
	 *         line
	 */
	public static Properties parseCommandLine(String commandLine) {
		Properties properties = new Properties();

		if (!commandLine.equals("")) {

			/*
			 * Iterate through Startup Groups
			 */
			for (StartupOptionsGroup group : getStartupOptionsGroupsList()) {
				/*
				 * Iterate through Startup Options in this group
				 */
				String soName, soValue = "";
				int soType, shift = 0;
				for (StartupOption startupOption : group.getStartupOptions()) {
					soName = startupOption.getName();
					soType = startupOption.getType();
					if (commandLine.startsWith(soName)) {
						if (soType == TYPE_NONE) {
							soValue = new Boolean(true).toString();
							shift = soName.length() + 1;
						} else {
							commandLine = commandLine.substring(soName.length() + 1, commandLine.length());
							// int endValueIndex = commandLine.indexOf("\"");
							ParameterBean param = getNextParameterValue(commandLine);
							soValue = param.getValue();
							shift = param.getLastPosition() + 1;
						}

						properties.put(startupOption.getName(), soValue);

						if (shift < commandLine.length() - 1) {
							commandLine = commandLine.substring(shift, commandLine.length());
						} else {
							commandLine = "";
						}
					}
				}
			}

			if (!commandLine.equals("")) {
				properties.put(OTHERS_OTHER, commandLine);
			}
		}

		return properties;

	}

	/**
	 * Load values from a command line
	 * 
	 * @param commandLine
	 *            the command line used to start the emulator
	 */
	public static void loadFromCommandLine(String commandLine) {
		loadValues(parseCommandLine(commandLine));
	}

	/**
	 * Convert the type of the startup option from a string to a number
	 * 
	 * @param type
	 *            string that represents the type
	 * @return number that represents the type
	 */
	private static int getStartupOptionType(String type) {
		return (TYPE_MAP.get(type)).intValue();
	}

	/**
	 * Parses the next parameter value on a command line
	 * 
	 * @param commandLine
	 *            the command line
	 * 
	 * @return a bean containing the parameter value and the last position
	 *         looked at the command line
	 */
	private static ParameterBean getNextParameterValue(String commandLine) {
		boolean isWin32 = Platform.getOS().equals(Platform.OS_WIN32);
		boolean escaped = false;
		boolean quoted = false;

		char c;
		String value = "";
		int i;

		for (i = 0; i < commandLine.length(); i++) {
			c = commandLine.charAt(i);

			if (escaped) {
				value += c;
				escaped = false;
			} else if (c == '\\' && !isWin32) {
				escaped = true;
			} else if (c == '"' && isWin32) {
				if (value.length() == 0) {
					quoted = true;
				} else if (quoted) {
					break;
				} else {
					value += c;
				}
			} else if ((c == ' ') && (!quoted)) {
				break;
			} else {
				value += c;
			}
		}

		return new ParameterBean(value, ((quoted) ? i + 1 : i));
	}

	/**
	 * Bean used to identify a parameter value when parsing the startup options
	 * 
	 */
	private static class ParameterBean {
		private final String value;

		private final int lastPosition;

		/**
		 * Constructor
		 * 
		 * @param value
		 *            The parameter value
		 * @param lastPosition
		 *            The last position looked at the command line before
		 *            stopping the parse operation
		 */
		public ParameterBean(String value, int lastPosition) {
			this.value = value;
			this.lastPosition = lastPosition;
		}

		/**
		 * Retrieves the parameter value
		 * 
		 * @return the parameter value
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Retrieves the last position looked at the command line before
		 * stopping the parse operation
		 * 
		 * @return the last position looked at the command line before stopping
		 *         the parse operation
		 */
		public int getLastPosition() {
			return lastPosition;
		}
	}
}
