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

package #package_name#;

import java.io.Serializable;

/**
 * Element which belongs to each row of the List of this view.
 */
class ListElement implements Serializable {
	
	private static final long serialVersionUID = -1494059051539426231L;
	String text;
	Integer imageId;

	/**
	 * Always create the object with a text and an image.
	 * 
	 * @param text
	 *            Text of the {@link ListElement}.
	 * @param imageId
	 *            Image identifier of the {@link ListElement}.
	 */
	public ListElement(String text, Integer imageId) {
		this.text = text;
		this.imageId = imageId;
	}

	/**
	 * Get the Text.
	 * 
	 * @return Return the Text.
	 */
	public String getText() {
		return text;
	}

	/**
	 * Set the Text.
	 * 
	 * @param text
	 *            Text to be set.
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Get the Image identifier.
	 * 
	 * @return Return the Image identifier.
	 */
	public Integer getImageId() {
		return imageId;
	}

	/**
	 * Set the Image identifier.
	 * 
	 * @param imageId
	 *            Image Identifier to be set.
	 */
	public void setImageId(Integer imageId) {
		this.imageId = imageId;
	}

	/**
	 * Here we consider two {@link ListElement}s to be equal when they have
	 * the same text field.
	 */
	@Override
	public boolean equals(Object element) {
		return element != null && element instanceof ListElement
				&& this.text != null
				&& ((ListElement) element).text != null
				&& ((ListElement) element).text.equals(text);
	}

	@Override
	public int hashCode() {
		return text != null ? text.length() * 3 : 4;
	}
}