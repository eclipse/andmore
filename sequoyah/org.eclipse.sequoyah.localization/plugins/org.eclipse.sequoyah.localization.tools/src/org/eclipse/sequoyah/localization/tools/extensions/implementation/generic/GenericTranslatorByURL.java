/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Vinicius Hernandes (Motorola)
 * 
 * Contributors:
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.extensions.implementation.generic;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.sequoyah.localization.tools.datamodel.node.TranslationResult;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ITranslator;

/**
 *
 */
public class GenericTranslatorByURL extends ITranslator {

	@Override
	public TranslationResult translate(String word, String fromLanguage,
			String toLanguage) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TranslationResult> translateAll(List<String> words,
			String fromLanguage, String toLanguage, IProgressMonitor monitor)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TranslationResult> translate(String word, String fromLanguage,
			List<String> toLanguages) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TranslationResult> translateAll(List<String> words,
			List<String> fromLanguage, List<String> toLanguage,
			IProgressMonitor monitor) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
