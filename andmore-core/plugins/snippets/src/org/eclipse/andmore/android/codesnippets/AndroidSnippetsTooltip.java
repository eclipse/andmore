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

import org.eclipse.andmore.android.codesnippets.i18n.AndroidSnippetsNLS;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.common.snippets.core.ISnippetItem;

/**
 * Customized tooltip for snippets in Snippet View. It's intended to display the
 * snippet preview
 * 
 */
@SuppressWarnings("restriction")
public class AndroidSnippetsTooltip extends ToolTip {

	/*
	 * The snippet item to be displayed in the tooltip
	 */
	private final ISnippetItem item;

	/**
	 * Constructor
	 * 
	 * @param item
	 *            the snippet item to be displayed
	 * @param control
	 */
	public AndroidSnippetsTooltip(ISnippetItem item, Control control) {
		super(control, NO_RECREATE, true);
		this.item = item;
	}

	/**
	 * Display the snippet preview by using a JAVA Source Viewer, which is used
	 * to highlight the code
	 * 
	 * @see org.eclipse.jface.window.ToolTip#createToolTipContentArea(org.eclipse.swt.widgets.Event,
	 *      org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createToolTipContentArea(Event event, Composite parent) {
		// the main composite
		Composite mainComposite = new Composite(parent, SWT.NULL);
		mainComposite.setLayout(new GridLayout(1, true));

		/*
		 * snippet preview label
		 */
		Label textElem = new Label(mainComposite, SWT.LEFT);
		textElem.setText(AndroidSnippetsNLS.UI_Snippet_Preview);
		textElem.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

		/*
		 * JAVA source viewer
		 */
		final ScrolledComposite scroll = new ScrolledComposite(mainComposite, SWT.H_SCROLL | SWT.V_SCROLL);
		scroll.setLayout(new FillLayout());
		// create scroll layout which receives values to limit its area of
		// display
		GridData scrollLayoutData = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		Rectangle visibleArea = parent.getDisplay().getActiveShell().getMonitor().getClientArea();
		scrollLayoutData.heightHint = visibleArea.height / 3;
		scrollLayoutData.widthHint = visibleArea.width / 3;
		scroll.setLayoutData(scrollLayoutData);

		final Composite javaSourceViewerComposite = new Composite(scroll, SWT.NULL);
		javaSourceViewerComposite.setLayout(new FillLayout());

		int styles = SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION;
		Document document = new Document(item.getContentString());
		IPreferenceStore store = JavaPlugin.getDefault().getCombinedPreferenceStore();
		JavaTextTools javaTextTools = JavaPlugin.getDefault().getJavaTextTools();

		SourceViewer javaSourceViewer = new JavaSourceViewer(javaSourceViewerComposite, null, null, false, styles,
				store);
		javaSourceViewer
				.configure(new JavaSourceViewerConfiguration(javaTextTools.getColorManager(), store, null, null));
		javaSourceViewer.getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		javaTextTools.setupJavaDocumentPartitioner(document);
		javaSourceViewer.setDocument(document);
		javaSourceViewer.setEditable(false);

		// set up scroll
		scroll.setContent(javaSourceViewerComposite);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				scroll.setMinSize(javaSourceViewerComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		return mainComposite;
	}

	/**
	 * Get the snippet item being displayed
	 * 
	 * @return the snippet item being displayed
	 */
	public ISnippetItem getItem() {
		return item;
	}

}
