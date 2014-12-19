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
package org.eclipse.andmore.android.emulator.skin.android.parser;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.Stack;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.exception.SkinException;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;

/**
 * DESCRIPTION: This class parses a layout file into a LayoutFileModel object
 *
 * RESPONSIBILITY: Parse the layout file
 *
 * COLABORATORS: None.
 *
 * USAGE: Call readLayout method passing a file to be parsed and retrieve the
 * correspondent LayoutFileModel object
 */
public class LayoutFileParser implements ILayoutConstants {
	/**
	 * Name of the layout descriptor file at the skin folder
	 */
	private static final String LAYOUT_FILE_NAME = "layout";

	/**
	 * Name of the pseudo layout descriptor file at the res folder
	 */
	private static final String PSEUDO_LAYOUT_FILE = "res/pseudolayout";

	/**
	 * The pattern used for generating tokens out of the layout file
	 */
	private static final String SPLIT_PATTERN = "[\n\r \t]+";

	/**
	 * Parses a layout file
	 * 
	 * @param skinFilesPath
	 *            The path to the skin folder
	 * 
	 * @return a model containing all data read from the layout file
	 * 
	 * @throws SkinException
	 *             If it is not possible to read the layout file
	 */
	public static LayoutFileModel readLayout(File skinFilesPath) throws SkinException {
		LayoutFileModel model = new LayoutFileModel();
		File layoutPath = new File(skinFilesPath, LAYOUT_FILE_NAME);
		String fileContents;
		if ((layoutPath != null) && (layoutPath.isFile())) {
			fileContents = getLayoutFileContents(layoutPath);
			parseLayoutFile(fileContents, model);
		}

		Collection<String> partNames = model.getPartNames();
		if ((model.getLayoutNames().size() == 0) && (partNames.size() == 1)
				&& (partNames.iterator().next().equals(PartBean.UNIQUE_PART))) {
			fileContents = getPseudoLayoutFileContents();
			parseLayoutFile(fileContents, model);
		}

		return model;
	}

	/**
	 * Parses the provided layout file contents
	 * 
	 * @param fileContents
	 *            All the contents of a layout file
	 * @param model
	 *            The model where to set the parsed data
	 * 
	 * @throws SkinException
	 *             If the layout file is corrupted, or has erroneous syntax
	 */
	private static void parseLayoutFile(String fileContents, LayoutFileModel model) throws SkinException {
		// process given string to remove comments
		String cleanContents = "";
		BufferedReader reader = null;
		try {
			StringBuffer contentBuffer = new StringBuffer();
			reader = new BufferedReader(new StringReader(fileContents));
			String line = null;
			do {
				line = reader.readLine();
				String lineCopy = line;
				if ((line != null) && !lineCopy.trim().startsWith("#")) {
					contentBuffer.append(line + '\n');
				}
			} while (line != null);
			cleanContents = contentBuffer.toString();
		} catch (IOException e) {
			// try to continue with the parser
			cleanContents = fileContents;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				StudioLogger.error("Could not close input stream: ", e.getMessage()); //$NON-NLS-1$
			}
		}

		String[] tokens = cleanContents.split(SPLIT_PATTERN);

		Stack<Object> stack = new Stack<Object>();

		// At this point, the file has been read into hundreds of token,
		// including blocks,
		// "{", "}", keys and values

		String currentTag = null;
		String key = null;

		// Iterate on the tokens
		try {
			for (String aToken : tokens) {
				// When the token is a "{", that means we need to stack
				// something. This "something"
				// will be removed from stack when we find a matching "}"
				if (OPEN_BRACKET.equals(aToken)) {
					// Every word is interpreted as a key at first. If we find a
					// "{", it must be
					// re-interpreted as a tag instead
					if (key != null) {
						currentTag = key;
						key = null;
					}
					addElementsToStack(stack, model, currentTag);
				}
				// When the token is a "}" we must remove something from the
				// stack
				else if (CLOSE_BRACKET.equals(aToken)) {
					removeElementsFromStack(stack);
				} else {
					// A word is interpreted as a key by default. If the key is
					// already set, we will
					// have a key-value pair and are able to assign it to
					// something at the model
					if (key == null) {
						key = aToken;
					} else {
						setKeyValuePair(stack, model, currentTag, key, aToken);
						key = null;
					}
				}
			}

		} catch (EmptyStackException e) {
			throw new SkinException(EmulatorNLS.ERR_LayoutFileParser_BracketsDoNotMatch);
		}

		if (!stack.isEmpty()) {
			// When there is only a part bean at the first level, that means we
			// have finished
			// parsing a single part layout. Remove it from the stack as well.
			//
			// NOTE: when creating the single part layout, we have added this
			// additional element
			// to the stack
			if ((stack.size() == 1) && (stack.get(0) instanceof PartBean)
					&& (((PartBean) stack.get(0)).getName().equals(PartBean.UNIQUE_PART))) {
				stack.pop();
			} else {
				throw new SkinException(EmulatorNLS.ERR_LayoutFileParser_BracketsDoNotMatch);
			}
		}

	}

	/**
	 * Reads the contents of the provided file into a String object
	 * 
	 * @param layoutPath
	 *            A file pointing to an "layout" file
	 * 
	 * @return A string with all the contents of the file
	 * 
	 * @throws SkinException
	 *             If the file cannot be read
	 */
	private static String getLayoutFileContents(File layoutPath) throws SkinException {
		int fileSize = (int) layoutPath.length();
		char[] buffer = new char[fileSize];

		FileReader fr = null;
		try {
			fr = new FileReader(layoutPath);
			fr.read(buffer);
		} catch (IOException e) {
			error("The file " + layoutPath.getAbsolutePath() + " could not be read. cause=" + e.getMessage());
			throw new SkinException(EmulatorNLS.ERR_LayoutFileParser_LayoutFileCouldNotBeRead);
		} finally {
			try {
				if (fr != null) {
					fr.close();
				}
			} catch (IOException e) {
				warn("The file " + layoutPath.getAbsolutePath() + " could not be closed after reading");
			}
		}

		return String.copyValueOf(buffer);
	}

	/**
	 * Gets the contents of the pseudo layout file, for merging to the current
	 * model
	 * 
	 * @return A string containing all the contents of the pseudo layout file
	 * 
	 * @throws SkinException
	 *             If the file cannot be read
	 */
	private static String getPseudoLayoutFileContents() throws SkinException {
		URL url = EmulatorPlugin.getDefault().getBundle().getResource(PSEUDO_LAYOUT_FILE);
		CharBuffer buffer = CharBuffer.allocate(1024);
		int readChars = 0;

		InputStream is = null;
		InputStreamReader isr = null;
		try {
			is = url.openStream();
			isr = new InputStreamReader(is);
			while (readChars != -1) {
				readChars = isr.read(buffer);
			}
			buffer.flip();
		} catch (IOException e) {
			error("The file res/pseudolayout could not be read. cause=" + e.getMessage());
			throw new SkinException(EmulatorNLS.ERR_LayoutFileParser_LayoutFileCouldNotBeRead);
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (isr != null) {
					isr.close();
				}
			} catch (IOException e) {
				warn("The file " + PSEUDO_LAYOUT_FILE + " could not be closed after reading");
			}
		}

		return buffer.toString();
	}

	/**
	 * Stacks an element The stack rules are quite complex. We first start by
	 * special cases (in which we analyze the stack and the current element for
	 * accurate interpretation) and then move to the default cases.
	 * 
	 * Summarizing, the stack will contain objects from this package (*Bean) as
	 * well as String objects. When a bean is at the top of the stack, we may
	 * perform actions on the given object. Strings are added to the stack for
	 * bracket matching and to add a mark for future actions.
	 * 
	 * @param stack
	 *            The stack where to add elements
	 * @param model
	 *            The model being built
	 * @param elementName
	 *            The name of the element to add to stack.
	 */
	private static void addElementsToStack(Stack<Object> stack, LayoutFileModel model, String elementName) {
		int stackSizeAtStart = stack.size();

		// --------------
		// SPECIAL CASES
		// --------------

		// When the stack size is equal to zero, we can have one of those two
		// situations:
		//
		// a) THE LAYOUT FILE CONTAINS MULTIPLE LAYOUT AND/OR PARTS: It is
		// possible to have the
		// following tags: "parts", "layouts", "keyboard" or "network". All of
		// them are handled in the
		// else clause, by adding the tag name at the stack
		//
		// b) THE LAYOUT FILE IS SIMPLE (i.e. it doesn't contain layouts,
		// neither a collection
		// of parts): It is possible to have the following tags: "display",
		// "background", "button",
		// "keyboard", "network". The first three belong to a part definition,
		// so we need to include a
		// PartBean to the stack before the object representing the tag. The
		// last two can be handled the same
		// way as in item (a)
		if (stack.size() == 0) {
			if ((MAIN_LEVEL_DISPLAY.equals(elementName)) || (MAIN_LEVEL_BACKGROUND.equals(elementName))
					|| (MAIN_LEVEL_BUTTON.equals(elementName))) {
				// This is a single part layout. Execute operation described at
				// item (b) above
				PartBean bean = model.newPart();
				stack.push(bean);

				if ((MAIN_LEVEL_BACKGROUND.equals(elementName)) || (MAIN_LEVEL_BUTTON.equals(elementName))) {
					stack.push(elementName);
				} else if (MAIN_LEVEL_DISPLAY.equals(elementName)) {
					RectangleBean display = bean.newDisplay();
					stack.push(display);
				}
			} else {
				// PARTS, LAYOUTS, KEYBOARD, NETWORK
				stack.push(elementName);
			}
		}

		// When the stack size is equal to one, we can have one of those four
		// situations:
		//
		// a) THE ELEMENT AT STACK IS NOT A STRING: In this case, we will handle
		// as default case
		// b) THE ELEMENT AT STACK IS THE "parts" STRING: It means that the
		// element name denotes the name of
		// a part. We must create a part with the name of the element, and add
		// it to the stack
		// c) THE ELEMENT AT STACK IS THE "layouts" STRING: It means that the
		// element name denotes the name of
		// a layout. We must create a layout with the name of the element, and
		// add it to the stack
		// d) THE ELEMENT AT STACK IS ANY OTHER STRING: In this case, we will
		// handle as default case
		else if (stack.size() == 1) {
			Object previousElement = stack.peek();
			if (previousElement instanceof String) {
				if (MAIN_LEVEL_PARTS.equals(previousElement)) {
					// elementName is the name of a new part
					PartBean bean = model.newPart(elementName);
					stack.push(bean);
				} else if (MAIN_LEVEL_LAYOUTS.equals(previousElement)) {
					// elementName is the name of a new layout
					LayoutBean bean = model.newLayout(elementName);
					stack.push(bean);
				}
			}
		}

		// --------------
		// DEFAULT CASES
		// --------------

		// Any other case will be handled below. The following clauses cover any
		// other remaining cases not
		// covered by the special cases. The beans created, when added to the
		// stack, represents structures
		// already known. If it is not possible to guess what structure we need
		// at the current parse iteration
		// or if we need an element at the stack to match a close bracket to
		// come, we simply add it as string
		//
		// We only execute the following block if the previous cases didn't
		// affect the stack
		if (stackSizeAtStart == stack.size()) {
			Object stackElem = stack.peek();
			if (stackElem instanceof PartBean) {
				if (MAIN_LEVEL_DISPLAY.equals(elementName)) {
					RectangleBean display = ((PartBean) stackElem).newDisplay();
					stack.push(display);
				} else if (MAIN_LEVEL_BACKGROUND.equals(elementName)) {
					ImagePositionBean background = ((PartBean) stackElem).newBackground(elementName);
					stack.push(background);
				} else {
					stack.push(elementName);
				}
			} else if (stackElem instanceof LayoutBean) {
				PartRefBean bean = ((LayoutBean) stackElem).newPartRef(elementName);
				stack.push(bean);
			} else if (stackElem instanceof String) {
				if ((MAIN_LEVEL_BUTTON.equals(stackElem) || (PART_BUTTONS.equals(stackElem)))) {
					Object nonStringObj = findFirstNonStringAtStack(stack);
					if (nonStringObj != null) {
						PartBean bean = (PartBean) nonStringObj;
						ImagePositionBean button = bean.newButton(elementName);
						stack.push(button);
					}
				}
			}
		}
	}

	/**
	 * Removes an element from the stack
	 * 
	 * @param stack
	 *            The stack from where to remove elements
	 */
	private static void removeElementsFromStack(Stack<Object> stack) {
		stack.pop();
	}

	/**
	 * Set a key-value pair at the object at the top of the stack. Depending on
	 * the key-value pair, we may set attributes to the model itself
	 * 
	 * @param stack
	 *            The stack containing the element to have a property set
	 * @param model
	 *            The model that can have a property set
	 * @param currentTag
	 *            The name of the tag containing a model property
	 * @param key
	 *            The property key
	 * @param value
	 *            The property value
	 */
	private static void setKeyValuePair(Stack<Object> stack, LayoutFileModel model, String currentTag, String key,
			String value) {
		Object obj = stack.peek();
		if (obj instanceof String) {
			Object notStringObj = findFirstNonStringAtStack(stack);

			if (notStringObj instanceof ILayoutBean) {
				((ILayoutBean) notStringObj).setKeyValue(key, value);
			} else {
				if (MAIN_LEVEL_NETWORK.equals(currentTag)) {
					if (NETWORK_DELAY.equals(key)) {
						model.setNetworkDelay(value);
					} else if (NETWORK_SPEED.equals(key)) {
						model.setNetworkSpeed(value);
					}
				} else if (MAIN_LEVEL_KEYBOARD.equals(currentTag)) {
					if (KEYBOARD_CHARMAP.equals(key)) {
						model.setKeyboardCharmap(value);
					}
				}
			}
		} else {
			((ILayoutBean) obj).setKeyValue(key, value);
		}
	}

	/**
	 * Utility method for finding the first non-String object at the stack
	 * 
	 * @param stack
	 *            The stack were to find the first non-String at
	 * 
	 * @return The non-String object
	 */
	private static Object findFirstNonStringAtStack(Stack<Object> stack) {
		Object firstNonString = null;
		Object tmpObj = null;

		int i = stack.size() - 1;
		while (i >= 0) {
			tmpObj = stack.get(i);
			if (!(tmpObj instanceof String)) {
				firstNonString = tmpObj;
				break;
			} else {
				i--;
			}
		}

		return firstNonString;
	}
}
