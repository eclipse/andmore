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
package org.eclipse.sequoyah.localization.tools.managers;

import java.util.List;

import org.eclipse.sequoyah.localization.tools.datamodel.node.GrammarCheckerResult;
import org.eclipse.sequoyah.localization.tools.extensions.classes.IGrammarChecker;
import org.eclipse.sequoyah.localization.tools.extensions.providers.GrammarCheckerProvider;

/**
 *
 */
public class GrammarCheckerManager {

	private List<IGrammarChecker> grammarCheckers;

	private GrammarCheckerProvider grammarCheckerProvider;

	private PreferencesManager preferencesManager;

	/**
	 * @param string
	 * @param language
	 * @return
	 */
	public GrammarCheckerResult check(String string, String language) {
		return null;
	}

	/**
	 * @param strings
	 * @param language
	 * @return
	 */
	public List<GrammarCheckerResult> checkAll(List<String> strings,
			String language) {
		return null;
	}

	/**
	 * @param grammarChecker
	 * @param string
	 * @return
	 */
	public GrammarCheckerResult check(IGrammarChecker grammarChecker,
			String string) {
		return null;
	}

	/**
	 * @param grammarChecker
	 * @param strings
	 * @return
	 */
	public List<GrammarCheckerResult> checkAll(IGrammarChecker grammarChecker,
			List<String> strings) {
		return null;
	}

	/**
	 * @return
	 */
	public List<IGrammarChecker> getGrammarCheckers() {
		return grammarCheckers;
	}

	/**
	 * @param name
	 * @return
	 */
	public IGrammarChecker getGrammarCheckerByName(String name) {
		return null;
	}

}
