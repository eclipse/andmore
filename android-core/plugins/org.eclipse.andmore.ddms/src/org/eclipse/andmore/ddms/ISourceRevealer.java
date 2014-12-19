/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.ddms;

/**
 * Classes which implement this interface are able to open a source file based
 * on the provided constraints.
 */
public interface ISourceRevealer {
	/**
	 * Reveal a particular line in the given application.
	 * 
	 * @param applicationName
	 *            the name of the application running the source.
	 * @param className
	 *            the fully qualified class name
	 * @param line
	 *            the line to reveal
	 * @return true if the source was revealed.
	 */
	boolean reveal(String applicationName, String className, int line);

	/**
	 * Reveal a particular Java method.
	 * 
	 * @param fqmn
	 *            fully qualified method name
	 * @param fileName
	 *            file name that contains the method, null if not known
	 * @param lineNumber
	 *            line number in the file, -1 if not known
	 * @param perspective
	 *            If not null, switch to this perspective before revealing the
	 *            source
	 * @return true if the source was revealed.
	 */
	boolean revealMethod(String fqmn, String fileName, int lineNumber, String perspective);
}
