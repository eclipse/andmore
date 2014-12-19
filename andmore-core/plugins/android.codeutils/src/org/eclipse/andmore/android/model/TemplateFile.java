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

/**
 * Represents a file which is part of a template.
 */
public class TemplateFile {

	private String type;

	private String modelName;

	private String finalName;

	private String modifier;

	/**
	 * Constructs a new template file setting its initial values.
	 * */
	public TemplateFile(String type, String modelName, String finalName, String modifier) {
		this.type = type;
		this.modelName = modelName;
		this.finalName = finalName;
		this.modifier = modifier;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getFinalName() {
		return finalName;
	}

	public void setFinalName(String finalName) {
		this.finalName = finalName;
	}

	public String getModifier() {
		return modifier;
	}

	public void setModifier(String modifier) {
		this.modifier = modifier;
	}

}
