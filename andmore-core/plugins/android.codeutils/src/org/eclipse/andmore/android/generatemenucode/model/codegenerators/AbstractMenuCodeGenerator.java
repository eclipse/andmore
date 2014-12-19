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
package org.eclipse.andmore.android.generatemenucode.model.codegenerators;

import org.eclipse.andmore.android.generatecode.AbstractCodeGenerator;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Class that have common methods to generate code based on menu
 */
public abstract class AbstractMenuCodeGenerator extends AbstractCodeGenerator {
	protected CodeGeneratorDataBasedOnMenu codeGeneratorData;

	/**
	 * @param codeGeneratorData
	 *            input data (representing menu.xml file) to use for creating
	 *            automatic code
	 * @param typeDeclaration
	 *            AST type where to insert the code
	 */
	public AbstractMenuCodeGenerator(CodeGeneratorDataBasedOnMenu codeGeneratorData, TypeDeclaration typeDeclaration) {
		super(typeDeclaration);
		this.codeGeneratorData = codeGeneratorData;
	}

	/**
	 * @return the codeGeneratorData
	 */
	protected CodeGeneratorDataBasedOnMenu getCodeGeneratorData() {
		return codeGeneratorData;
	}
}
