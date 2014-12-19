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
package org.eclipse.andmore.android.wizards.elements;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Base interface to create a screen block.
 */
public interface IBaseBlock extends Listener {

	/**
	 * Create the content.
	 */
	Composite createContent(Composite parent);

	/**
	 * This method must be invoked when the user wants to refresh the screen.
	 */
	void refresh();

	/**
	 * Returns <code>true</code> if the user can finish the wizard. This is used
	 * when the block is used in wizards.
	 */
	boolean isPageComplete();

	/**
	 * Returns the error message in this block, null if there is no error.
	 */
	String getErrorMessage();

	/**
	 * Configures the shell used by this block.
	 * 
	 * @param shell
	 *            The shell.
	 */
	void setShell(Shell shell);

	/**
	 * Returns the shell used by this block.
	 * 
	 * @return Rhe shell used by this block.
	 */
	Shell getShell();

	/**
	 * Returns <code>true</code> if the user can flip to the next page. This is
	 * used when the block is used in wizards.
	 */
	boolean canFlipToNextPage();

	/**
	 * Sets the widget that will receive focus when
	 * {@link IBaseBlock#setFocus()} is called on the first time the block is
	 * visible.
	 * */
	void setDefaultFocus();

	/**
	 * Sets the focus on the default widget with the block. This method should
	 * call {@link Control#setFocus()} on one of its control widget, for
	 * instance a {@link Text} control.
	 * */
	void setFocus();
}
