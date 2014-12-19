/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.eclipse.andmore.gltrace.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model a single GL API function call's specification.
 */
public class GLAPISpec {
	private static final String GL_SPECS_FILE = "/entries.in"; //$NON-NLS-1$
	private static final String GLES2_ENTRIES_HEADER_V1 = "# org.eclipse.andmore.gltrace.glentries, v1"; //$NON-NLS-1$
	private static Map<String, GLAPISpec> sApiSpecs;

	private final String mGLFunction;
	private final GLDataTypeSpec mReturnType;
	private final List<GLDataTypeSpec> mArgs;

	private GLAPISpec(String glFunction, GLDataTypeSpec returnType, List<GLDataTypeSpec> args) {
		mGLFunction = glFunction;
		mReturnType = returnType;
		mArgs = args;
	}

	public String getFunction() {
		return mGLFunction;
	}

	public GLDataTypeSpec getReturnValue() {
		return mReturnType;
	}

	public List<GLDataTypeSpec> getArgs() {
		return mArgs;
	}

	public static Map<String, GLAPISpec> getSpecs() {
		if (sApiSpecs == null) {
			sApiSpecs = parseApiSpecs(GLAPISpec.class.getResourceAsStream(GL_SPECS_FILE));
		}

		return sApiSpecs;
	}

	private static Map<String, GLAPISpec> parseApiSpecs(InputStream specFile) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(specFile));
		Map<String, GLAPISpec> specs = new HashMap<String, GLAPISpec>(400);

		try {
			String header = reader.readLine().trim();
			assert header.equals(GLES2_ENTRIES_HEADER_V1);

			String line;
			while ((line = reader.readLine()) != null) {
				// strip away the comments
				int commentPos = line.indexOf('#');
				if (commentPos != -1) {
					line = line.substring(0, commentPos);
				}
				line = line.trim();

				// parse non empty lines
				if (line.length() > 0) {
					GLAPISpec spec = parseLine(line);
					specs.put(spec.getFunction(), spec);
				}
			}

			specFile.close();
		} catch (IOException e) {
			// this is unlikely to happen as the file is present within this
			// .jar file.
			// Even if it does happen, we just return whatever we've read till
			// now. The net
			// impact will be that the function calls will not be parsed fully
			// and will just
			// display the function name.
		}

		return specs;
	}

	/**
	 * Parse a GL API Specification entry from "/entries.in". Each line is of
	 * the format: {@code returnType, funcName, arg*}. This method is package
	 * private for testing.
	 */
	static GLAPISpec parseLine(String line) {
		List<String> words = Arrays.asList(line.split(","));

		String retType = words.get(0).trim();
		String func = words.get(1).trim();
		List<String> argDefinitions = words.subList(2, words.size());

		List<GLDataTypeSpec> glArgs = new ArrayList<GLDataTypeSpec>(argDefinitions.size() / 2);
		for (String argDefn : argDefinitions) {
			// an argDefn is something like: "const GLvoid* data"
			argDefn = argDefn.trim();
			int lastSeparator = argDefn.lastIndexOf(' ');
			if (lastSeparator == -1) {
				// no space => a void type with no argument name
				glArgs.add(new GLDataTypeSpec(argDefn, null));
			} else {
				// everything upto the last space is the type
				String type = argDefn.substring(0, lastSeparator);

				// and the last word is the variable name
				String name = argDefn.substring(lastSeparator + 1);
				glArgs.add(new GLDataTypeSpec(type, name));
			}
		}

		return new GLAPISpec(func, new GLDataTypeSpec(retType, null), glArgs);
	}
}
