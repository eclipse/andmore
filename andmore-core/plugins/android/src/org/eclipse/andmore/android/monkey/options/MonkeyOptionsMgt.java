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
package org.eclipse.andmore.android.monkey.options;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class provides methods to manage monkey options
 */
public class MonkeyOptionsMgt implements IMonkeyOptionsConstants {

	/**
	 * List of all monkey options groups (the monkey options themselves will be
	 * accessed through the use of a method in the group object that returns the
	 * monkey options in that group)
	 */
	private static List<MonkeyOptionsGroup> monkeyOptionsGroupsList = null;

	/**
	 * List of all monkey options, indexed by their names, for fast access
	 */
	private static Map<String, MonkeyOption> monkeyOptionsMap = new HashMap<String, MonkeyOption>();

	/*
	 * Load the monkey options / groups list
	 */
	static {
		load();
	}

	/**
	 * Get the monkey options groups list
	 * 
	 * @return monkey options groups list
	 */
	public static List<MonkeyOptionsGroup> getMonkeyOptionsGroupsList() {
		return monkeyOptionsGroupsList;
	}

	/**
	 * Read all groups and monkey options available for editing from a XML and
	 * stores the information in the correspondent beans
	 */
	public static void load() {

		try {
			// Clear monkey options groups list
			monkeyOptionsGroupsList = new ArrayList<MonkeyOptionsGroup>();

			// Define XML path
			InputStream xmlStream = AndroidPlugin.getDefault().getBundle().getEntry(MONKEY_OPTIONS_XML_PATH)
					.openStream();

			// Load XML
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(xmlStream);

			/*
			 * Iterate through Monkey Groups
			 */
			Element rootNode = document.getDocumentElement();
			NodeList monkeyOptionsGroups = rootNode.getElementsByTagName(GROUP_TAG);
			for (int i = 0; i < monkeyOptionsGroups.getLength(); i++) {
				/*
				 * Create group
				 */
				Element group = (Element) monkeyOptionsGroups.item(i);
				String strKey = group.getAttributeNode(GROUP_TAG_ID).getNodeValue();

				strKey = Platform.getResourceString(AndroidPlugin.getDefault().getBundle(), strKey.trim());

				MonkeyOptionsGroup monkeyOptionsGroup = new MonkeyOptionsGroup(strKey);
				monkeyOptionsGroup.setTitle(monkeyOptionsGroup.getId());

				/*
				 * Iterate through Monkey Options in this group
				 */
				NodeList monkeyOptions = group.getElementsByTagName(MONKEY_OPT_TAG);
				monkeyOptionsGroup.setMonkeyOptions(new ArrayList<MonkeyOption>()); // clear
																					// monkey
																					// options
				for (int j = 0; j < monkeyOptions.getLength(); j++) {
					/*
					 * Create monkey option
					 */
					Element option = (Element) monkeyOptions.item(j);
					MonkeyOption monkeyOption = new MonkeyOption(option.getAttributeNode(MONKEY_OPT_TAG_NAME)
							.getNodeValue(), getMonkeyOptionType(option.getAttributeNode(MONKEY_OPT_TAG_TYPE)
							.getNodeValue())); // name and type

					strKey = option.getAttributeNode(MONKEY_OPT_TAG_FRIENDLY_NAME).getNodeValue();

					strKey = Platform.getResourceString(AndroidPlugin.getDefault().getBundle(), strKey.trim());

					monkeyOption.setUserFriendlyName(strKey); // friendly name

					strKey = option.getElementsByTagName(MONKEY_OPT_TAG_DESCRIPTION).item(0).getTextContent();

					strKey = Platform.getResourceString(AndroidPlugin.getDefault().getBundle(), strKey.trim());

					monkeyOption.setDescription(strKey); // description
					if (option.getAttributeNode(MONKEY_OPT_TAG_TYPE_DETAILS) != null) {
						monkeyOption
								.setTypeDetails(option.getAttributeNode(MONKEY_OPT_TAG_TYPE_DETAILS).getNodeValue()); // type
																														// details
					}
					// Iterate through monkey option pre-defined values, if any
					NodeList preDefinedValuesContainer = option.getElementsByTagName(PREDEFINED_VALUES_TAG);
					monkeyOption.setPreDefinedValues(new ArrayList<String>()); // clear
																				// pre-defined
																				// values
					if (preDefinedValuesContainer.getLength() > 0) {
						NodeList preDefinedValues = ((Element) preDefinedValuesContainer.item(0))
								.getElementsByTagName(PREDEFINED_VALUE_TAG);
						for (int k = 0; k < preDefinedValues.getLength(); k++) {
							// Add pre-defined values to the option
							Element preDefinedValue = (Element) preDefinedValues.item(k);
							monkeyOption.getPreDefinedValues().add(preDefinedValue.getTextContent());
						}
					}

					/*
					 * Add monkey options to the group
					 */
					monkeyOptionsGroup.getMonkeyOptions().add(monkeyOption);

					monkeyOptionsMap.put(monkeyOption.getName(), monkeyOption);

				}

				/*
				 * Add groups to the groups list
				 */
				monkeyOptionsGroupsList.add(monkeyOptionsGroup);
			}

		} catch (Exception e) {
			StudioLogger.error("Failed to load monkey options");
		}

	}

	/**
	 * Validate the values assigned for the monkey options marked as checked
	 * (the ones that are being used), according to its type and type details.
	 * 
	 * @return Status object with the result of the validation
	 */
	public static Status validate() {
		Status status = (Status) Status.OK_STATUS;
		String msg = null;

		/*
		 * Iterate through Monkey Groups
		 */
		for (MonkeyOptionsGroup group : getMonkeyOptionsGroupsList()) {
			/*
			 * Iterate through monkey options in this group
			 */
			for (MonkeyOption monkeyOption : group.getMonkeyOptions()) {
				/*
				 * Check if the Monkey Option is checked
				 */
				if (monkeyOption.isChecked() && (status.isOK())) {

					String name = monkeyOption.getName(); // monkey option name
					String ufname = monkeyOption.getUserFriendlyName(); // user-friendly
																		// monkey
																		// option
																		// name
					String value = monkeyOption.getValue(); // monkey option
															// value
					String typeDetails = monkeyOption.getTypeDetails(); // monkey
																		// option
																		// type
																		// detail

					/*
					 * General validation: no quotes in values
					 */
					if ((!monkeyOption.getName().equals(OTHERS_OTHER)) && (value.indexOf("\"") >= 0)) {
						msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_NoQuotes, ufname);
					} else {

						/*
						 * Call the appropriate validation method
						 */
						switch (monkeyOption.getType()) {
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
						status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID, msg);
						break;
					}

				}
			}
		}

		return status;

	}

	/**
	 * Validate the monkey option value for an monkey option of "text" type
	 * 
	 * @param name
	 *            the monkey option name
	 * @param ufname
	 *            the user-friendly monkey option name
	 * @param value
	 *            the current assigned value for the monkey option
	 * @param typeDetails
	 *            any special requirements that the assigned value must be match
	 * @return null if the value assigned for the monkey option is a valid one
	 *         or an error message otherwise
	 */
	private static String validateTextField(String name, String ufName, String value, String typeDetails) {
		String msg = null;

		// Check if the value is blank
		if ((value == null) || (value.equals(""))) {
			msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_TextBlank, ufName);
		}

		return msg;

	}

	/**
	 * Validate the monkey option value for an monkey option of "number" type
	 * 
	 * @param name
	 *            the monkey option name
	 * @param ufname
	 *            the user-friendly monkey option name
	 * @param value
	 *            the current assigned value for the monkey option
	 * @param typeDetails
	 *            any special requirements that the assigned value must be match
	 * @return null if the value assigned for the monkey option is a valid one
	 *         or an error message otherwise
	 */
	private static String validadeNumberField(String name, String ufName, String value, String typeDetails) {
		String msg = null;

		// Check if the value is blank
		if ((value == null) || (value.equals(""))) {
			msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_NumberRequired, ufName);
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
					msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_NumberMustBePositiveInteger, ufName);
				} else {

					/*
					 * Check if the value is in the correct range
					 */
					if (typeDetails != null) {
						String[] valueRange = typeDetails.split(";");
						if ((intValue < Integer.parseInt(valueRange[0]))
								|| (intValue > Integer.parseInt(valueRange[1]))) {
							// the value is not in the correct range
							msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_NumberIntRange, new String[] {
									ufName, valueRange[0], valueRange[1] });
						}
					}
				}

			} catch (NumberFormatException ex) {
				// it's not a number
				msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_NumberMustBeInteger, ufName);
			}
		}

		return msg;

	}

	/**
	 * Validate the monkey option value for an monkey option of "path" type
	 * 
	 * @param name
	 *            the monkey option name
	 * @param ufname
	 *            the user-friendly monkey option name
	 * @param value
	 *            the current assigned value for the monkey option
	 * @param typeDetails
	 *            any special requirements that the assigned value must be match
	 * @return null if the value assigned for the monkey option is a valid one
	 *         or an error message otherwise
	 */
	private static String validadePathField(String name, String ufName, String value, String typeDetails) {
		String msg = null;

		if ((value == null) || (value.equals(""))) {
			msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_PathRequired, ufName);
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
					msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_PathDirNotExist, ufName);
				} else {
					if (file.isFile()) {
						// it's not a folder
						msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_PathMustBeDir, ufName);
					}
				}
			}
			/*
			 * Validate file
			 */
			else {
				if (!file.exists()) {
					// the file doesn't exist
					msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_PathFileNotExist, ufName);
				} else {
					// it's not a file
					if (file.isDirectory()) {
						msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_PathMustBeFile, ufName);
					}
					// it doesn't have the correct extension
					else {
						if (!typeDetails.equals("." + (new Path(value)).getFileExtension())) {
							msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_PathIncorrectFileType,
									new String[] { ufName, typeDetails });
						}
					}
				}
			}
		}
		return msg;

	}

	/**
	 * Generates the list of parameters that shall to be sent to adb shell
	 * 
	 * @return the list of parameters that shall to be sent to the adb shell
	 */
	public static String getParamList() {
		String paramList = "";

		/*
		 * Iterate through Monkey Groups
		 */
		for (MonkeyOptionsGroup group : getMonkeyOptionsGroupsList()) {
			/*
			 * Iterate through Monkey Options in this group
			 */
			int monkeyOptionType;
			for (MonkeyOption monkeyOption : group.getMonkeyOptions()) {
				monkeyOptionType = monkeyOption.getType();
				if (monkeyOption.isChecked()) // check if the monkey option is
												// being used
				{
					if (monkeyOptionType == TYPE_NONE) {
						paramList += ((paramList.equals("")) ? "" : " ") + monkeyOption.getName();
					} else {
						if ((monkeyOption.getName().equals(OTHERS_OTHER))) {

							paramList += ((paramList.equals("")) ? "" : " ") + monkeyOption.getValue();

						} else {
							if ((monkeyOption.getName().equals(CATEGORY_OPTION))) {
								String[] values = monkeyOption.getValue().split(" ");
								for (int i = 0; i < values.length; i++) {
									if (values[i].trim().length() > 0) {
										paramList += ((paramList.equals("")) ? "" : " ") + monkeyOption.getName() + " "
												+ values[i];
									}
								}
							} else {
								String value = monkeyOption.getValue();

								if (!value.equals("")) {
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

									paramList += ((paramList.equals("")) ? "" : " ") + monkeyOption.getName()
											+ (value.trim().length() > 0 ? " " + value : "");
								}
							}
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
		 * Iterate through Monkey Groups
		 */
		for (MonkeyOptionsGroup group : getMonkeyOptionsGroupsList()) {
			/*
			 * Iterate through monkey options in this group
			 */
			String soValue = "";
			for (MonkeyOption monkeyOption : group.getMonkeyOptions()) {
				soValue = properties.getProperty(monkeyOption.getName());
				if (soValue != null) {
					monkeyOption.setChecked(true);
					monkeyOption.setValue(soValue);
				} else {
					monkeyOption.setChecked(false);
					monkeyOption.setValue("");
				}
				monkeyOption.updateUI();
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
			 * Iterate through Monkey Groups
			 */
			for (MonkeyOptionsGroup group : getMonkeyOptionsGroupsList()) {
				/*
				 * Iterate through monkey options in this group
				 */
				String soName, soValue = "";
				int soType, shift = 0;
				for (MonkeyOption monkeyOption : group.getMonkeyOptions()) {
					soName = monkeyOption.getName();
					soType = monkeyOption.getType();
					if (commandLine.startsWith(soName)) {
						if (soType == TYPE_NONE) {
							soValue = new Boolean(true).toString();
							shift = soName.length() + 1;
						} else {
							if ((monkeyOption.getName().equals(CATEGORY_OPTION))) {
								String soValueCat = "";
								while (commandLine.startsWith(CATEGORY_OPTION)) {
									commandLine = commandLine.substring(soName.length() + 1, commandLine.length());
									ParameterBean param = getNextParameterValue(commandLine);
									soValue = param.getValue();
									shift = param.getLastPosition() + 1;

									soValueCat = (soValueCat.equals("") ? soValueCat : soValueCat + " ") + soValue;

									if (shift < (commandLine.length() - 1)) {
										commandLine = commandLine.substring(shift, commandLine.length());
									} else {
										commandLine = "";
									}
								}
								shift = 0;
								soValue = soValueCat;
							} else {
								commandLine = commandLine.substring(soName.length() + 1, commandLine.length());
								ParameterBean param = getNextParameterValue(commandLine);
								soValue = param.getValue();
								shift = param.getLastPosition() + 1;
							}
						}

						properties.put(monkeyOption.getName(), soValue);

						if (shift < (commandLine.length() - 1)) {
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
	 * Convert the type of the monkey option from a string to a number
	 * 
	 * @param type
	 *            string that represents the type
	 * @return number that represents the type
	 */
	private static int getMonkeyOptionType(String type) {
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
			} else if ((c == '\\') && !isWin32) {
				escaped = true;
			} else if ((c == '"') && isWin32) {
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
	 * Bean used to identify a parameter value when parsing the monkey options
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
