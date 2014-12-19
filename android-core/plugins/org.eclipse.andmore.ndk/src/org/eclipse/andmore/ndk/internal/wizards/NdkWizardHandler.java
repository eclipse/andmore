/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.ndk.internal.wizards;

import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.ui.wizards.STDWizardHandler;

public class NdkWizardHandler extends STDWizardHandler {

	public NdkWizardHandler() {
		super(null, null);
	}

	@Override
	public IToolChain[] getSelectedToolChains() {
		IToolChain[] tcs = ManagedBuildManager.getRealToolChains();
		for (IToolChain tc : tcs) {
			if (tc.getId().equals("com.android.toolchain.gcc")) //$NON-NLS-1$
				return new IToolChain[] { tc };
		}
		return super.getSelectedToolChains();
	}

}
