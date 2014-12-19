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

package org.eclipse.andmore.gltrace.views.detail;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import java.util.List;

public interface IDetailProvider {
	/** Create the controls to display the details. */
	void createControl(Composite parent);

	/** Dispose off any created controls. */
	void disposeControl();

	/** Obtain the top level control used by this detail provider. */
	Control getControl();

	/**
	 * Obtain a list of tool bar items to be displayed when this provider is
	 * active.
	 */
	List<IContributionItem> getToolBarItems();
}
