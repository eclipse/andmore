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

package org.eclipse.andmore.android.codesnippets;

import java.util.Iterator;
import java.util.List;

import org.eclipse.andmore.android.codesnippets.i18n.AndroidSnippetsNLS;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.snippets.internal.palette.SnippetPaletteDrawer;
import org.eclipse.wst.common.snippets.internal.palette.SnippetPaletteItem;
import org.eclipse.wst.common.snippets.internal.ui.SnippetsView;

@SuppressWarnings("restriction")
public class SnippetsViewContributionItem extends ControlContribution {
	private final SnippetsView view;

	public SnippetsViewContributionItem(SnippetsView view) {
		super("org.eclipse.andmore.android.codesnippets.search");
		this.view = view;
	}

	private Text text;

	final String INITIAL_TEXT = AndroidSnippetsNLS.UI_Snippet_SearchLabel;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.action.ControlContribution#computeWidth(org.eclipse
	 * .swt.widgets.Control)
	 */
	@Override
	protected int computeWidth(Control control) {
		return text.computeSize(100, SWT.DEFAULT).x;
	}

	@Override
	protected Control createControl(Composite parent) {

		text = new Text(parent, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH);

		text.setToolTipText(INITIAL_TEXT);
		text.setEnabled(true);
		text.setEditable(true);
		text.setMessage(INITIAL_TEXT);

		resetView();

		text.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				String typed = text.getText().toLowerCase();

				// variables for the first inner loop
				Object rootChildObject = null;
				SnippetPaletteDrawer snippetPaletteDrawer = null;
				SnippetPaletteItem snippetPalletItem = null;

				// variables for the second inner loop
				List<?> snippetPalleteDrawerChildren = null;
				Iterator<?> snippetPalleteDrawerIterator = null;
				Integer foundItemsCount = null;
				Integer lastIndex = null;
				Object snippetPalletObject = null;

				// get text and items to be sought in
				List<?> rootChildren = view.getRoot().getChildren();
				Iterator<?> rootChildremIterator = rootChildren.iterator();

				/*
				 * Here the idea is to iterate through the snippets labels, text
				 * and codes, and find a match. In case there are results, a
				 * number of found items by category is displayed.
				 */
				while (rootChildremIterator.hasNext()) {
					rootChildObject = rootChildremIterator.next();
					if (rootChildObject instanceof SnippetPaletteDrawer) {
						snippetPaletteDrawer = (SnippetPaletteDrawer) rootChildObject;
						snippetPalleteDrawerChildren = snippetPaletteDrawer.getChildren();
						snippetPalleteDrawerIterator = snippetPalleteDrawerChildren.iterator();
						foundItemsCount = 0;
						while (snippetPalleteDrawerIterator.hasNext()) {
							snippetPalletObject = snippetPalleteDrawerIterator.next();
							if (snippetPalletObject instanceof SnippetPaletteItem) {
								snippetPalletItem = (SnippetPaletteItem) snippetPalletObject;

								// there must be a match for either the label,
								// description or code of the snippet
								if (snippetPalletItem.getLabel().toLowerCase().contains(typed)
										|| snippetPalletItem.getDescription().toLowerCase().contains(typed)
										|| snippetPalletItem.getContentString().toLowerCase().contains(typed)) {
									snippetPalletItem.setVisible(true);
									foundItemsCount++;
								} else {
									// since no match was found for the
									// snippets, try to find for its category
									// label
									if (snippetPaletteDrawer.getLabel().toLowerCase().contains(typed)) {
										snippetPalletItem.setVisible(true);
										foundItemsCount++;
									} else {
										snippetPalletItem.setVisible(false);
									}
								}
							}
						}

						// display the number of found items between parenthesis
						lastIndex = snippetPaletteDrawer.getLabel().lastIndexOf(")");
						if (lastIndex == -1) {
							snippetPaletteDrawer.setLabel(snippetPaletteDrawer.getLabel() + " (" + foundItemsCount
									+ ")");
						} else {
							snippetPaletteDrawer.setLabel(snippetPaletteDrawer.getLabel().replaceFirst("\\(\\d+\\)",
									"(" + foundItemsCount.toString() + ")"));
						}

						/*
						 * In case no match is found, hide the pallete and all
						 * its children, otherwise display the number of items
						 * found between parenthesis.
						 */
						if (foundItemsCount == 0) {
							snippetPaletteDrawer.setVisible(false);
							snippetPaletteDrawer.setFilters(new String[] { "!" });
						} else {
							// show the item
							snippetPaletteDrawer.setVisible(true);
							snippetPaletteDrawer.setFilters(new String[] { "*" });
						}
					}
				}
			}

		});

		return text;
	}

	/**
	 * Set all items to be visible.
	 */
	private void resetView() {
		// get text and items to be sought in
		List<?> rootChildren = view.getRoot().getChildren();
		Iterator<?> rootChildremIterator = rootChildren.iterator();

		/*
		 * Here the idea is to iterate through the snippets labels, text and
		 * codes, and set everything to visible, since the view is saving the
		 * state.
		 */
		while (rootChildremIterator.hasNext()) {
			Object rootChildObject = rootChildremIterator.next();
			if (rootChildObject instanceof SnippetPaletteDrawer) {
				SnippetPaletteDrawer snippetPaletteDrawer = (SnippetPaletteDrawer) rootChildObject;
				List<?> snippetPalleteDrawerChildren = snippetPaletteDrawer.getChildren();
				Iterator<?> snippetPalleteDrawerIterator = snippetPalleteDrawerChildren.iterator();
				snippetPaletteDrawer.setVisible(true);
				while (snippetPalleteDrawerIterator.hasNext()) {
					Object snippetPalletObject = snippetPalleteDrawerIterator.next();
					if (snippetPalletObject instanceof SnippetPaletteItem) {
						SnippetPaletteItem snippetPalletItem = (SnippetPaletteItem) snippetPalletObject;
						snippetPalletItem.setVisible(true);
					}
				}
			}

		}
	}

	public void clean() {
		text.setText("");
	}
}
