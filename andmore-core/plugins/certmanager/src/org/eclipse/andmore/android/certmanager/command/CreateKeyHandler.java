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
package org.eclipse.andmore.android.certmanager.command;

import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.certmanager.ui.wizards.CreateKeyWizard;
import org.eclipse.andmore.android.common.utilities.ui.WidgetsUtil;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

/**
 * Handler to execute the create key wizard.
 */
public class CreateKeyHandler extends AbstractHandler2 implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// retrieves the first element of the selection
		// note that this command should be enabled only when selection.count ==
		// 1.
		ITreeNode node = getSelection().get(0);

		if (node instanceof IKeyStore) {
			// get keystore where to create the new key
			final IKeyStore keyStore = (IKeyStore) node;
			CreateKeyWizard createKeyWizard = new CreateKeyWizard(keyStore);
			WidgetsUtil.runWizard(createKeyWizard);
		}

		return null;
	}
}
