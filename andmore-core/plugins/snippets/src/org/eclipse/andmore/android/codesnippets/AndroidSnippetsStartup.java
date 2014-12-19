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

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.snippets.core.ISnippetItem;
import org.eclipse.wst.common.snippets.core.ISnippetsEntry;
import org.eclipse.wst.common.snippets.internal.ui.SnippetsView;

/**
 * This class register listeners in the Snippets View to monitor changes on its
 * selection. This way, MOTODEV Studio for Android displays the snippet preview
 * when appropriate
 * 
 */
@SuppressWarnings("restriction")
public class AndroidSnippetsStartup implements IStartup {

	public final static String SNIPPETS_VIEW_ID = "org.eclipse.wst.common.snippets.internal.ui.SnippetsView";

	private static SnippetsViewContributionItem searchContributionItem;

	private static TooltipDisplayConfigContriutionItem tooltipDisplayConfigcontributionItem;

	/*
	 * The tool tip being displayed
	 */
	private AndroidSnippetsTooltip tooltip = null;

	private static void addSearchBar(final SnippetsView view) {
		if (searchContributionItem == null) {
			ToolBarManager tbManager = (ToolBarManager) view.getViewSite().getActionBars().getToolBarManager();

			// the item which searches for words within snippets titles,
			// descriptions and codes
			searchContributionItem = new SnippetsViewContributionItem(view);

			tbManager.add(searchContributionItem);
			tbManager.update(true);
			view.getViewSite().getActionBars().updateActionBars();
		}

		if (tooltipDisplayConfigcontributionItem == null) {
			ToolBarManager tbManager = (ToolBarManager) view.getViewSite().getActionBars().getToolBarManager();

			// the item which configures the display of the tool tip
			tooltipDisplayConfigcontributionItem = new TooltipDisplayConfigContriutionItem();

			tbManager.add(tooltipDisplayConfigcontributionItem);
			tbManager.update(true);
			view.getViewSite().getActionBars().updateActionBars();
		}
	}

	/**
	 * Add a mouse listener to Snippets View Open the snippet preview tooltip
	 * when the user clicks on a snippet item. Close it when he clicks again in
	 * the same item
	 * 
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	@Override
	public void earlyStartup() {

		/*
		 * This is the listener responsible for monitoring mouse clicks on
		 * snippet items. When the user clicks on an item, it triggers the
		 * action to display the snippet preview
		 */
		final MouseListener snippetsMouseListener = new MouseListener() {
			/**
			 * Do nothing
			 * 
			 * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
			 */
			@Override
			public void mouseUp(MouseEvent e) {
				// nothing
			}

			/**
			 * Open/Toogle tooltip
			 * 
			 * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
			 */
			@Override
			public void mouseDown(MouseEvent e) {

				// get the Snippets View
				SnippetsView snippetsView = (SnippetsView) EclipseUtils.getActiveView(SNIPPETS_VIEW_ID);

				// get the selected snippet
				ISnippetsEntry snippetEntry = snippetsView.getSelectedEntry();

				// check the tooltip is already being displayed
				// open it if it's not and its config allows so, close it
				// otherwise
				if ((snippetEntry instanceof ISnippetItem)
						&& ((tooltip == null) || (!tooltip.getItem().equals(snippetEntry)))
						&& tooltipDisplayConfigcontributionItem.isTooltipDisplayed()) {
					Control snippetsControl = snippetsView.getViewer().getControl();
					tooltip = new AndroidSnippetsTooltip((ISnippetItem) snippetEntry, snippetsControl);
					tooltip.setPopupDelay(250);
					tooltip.show(new Point(snippetsControl.getBounds().width, 0));

				} else {
					if (tooltip != null) {
						tooltip.hide();
					}
					tooltip = null;
				}
			}

			/**
			 * Hide tooltip
			 * 
			 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
			 */
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if (tooltip != null) {
					tooltip.hide();
				}
				tooltip = null;
			}
		};

		/**
		 * this listener is called when the mouse is dragged. It simply hides
		 * the tool tip when this action occurs.
		 */
		final DragDetectListener snippetsMouseDragListener = new DragDetectListener() {

			/**
			 * Hide the tool tip.
			 */
			@Override
			public void dragDetected(DragDetectEvent e) {
				// simply hide the tool tip
				if (tooltip != null) {
					tooltip.hide();
				}
			}
		};

		/*
		 * This is the listener that is attached to the workspace and monitor
		 * perspectives activation, as well as changes in the views being
		 * displayed (for example: view opened/closed). It intends to attach the
		 * snippetsMouseListener listener declared above in the Snippets View
		 * being used by Eclipse
		 */
		final IPerspectiveListener perspectiveListener = new PerspectiveAdapter() {
			// Set if it has already been executed
			// If so, it doesn't need to be executed again
			boolean executed = false;

			/**
			 * This action is called when the user goes to another perspective
			 * (it's not called for the first perspective displayed when he
			 * opens Eclipse)
			 * 
			 * @see org.eclipse.ui.PerspectiveAdapter#perspectiveActivated(org.eclipse.ui.IWorkbenchPage,
			 *      org.eclipse.ui.IPerspectiveDescriptor)
			 */
			@Override
			public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
				IViewReference[] viewReferences = page.getViewReferences();
				for (IViewReference viewReference : viewReferences) {
					if (SNIPPETS_VIEW_ID.equals(viewReference.getId())) {
						SnippetsView snippetsView = (SnippetsView) viewReference.getView(true);
						if (snippetsView != null) {
							addSearchBar(snippetsView);
							PaletteViewer palleteViewer = snippetsView.getViewer();
							if (palleteViewer != null) {
								Control control = palleteViewer.getControl();
								if (control != null) {
									control.removeMouseListener(snippetsMouseListener); // remove
																						// mouse
																						// listener
																						// just
																						// to
																						// ensure
																						// we
																						// never
																						// have
																						// two
																						// listeners
																						// registered
									control.removeDragDetectListener(snippetsMouseDragListener); // remove
																									// mouse
																									// draggable
																									// listener
																									// just
																									// to
																									// ensure
																									// we
																									// never
																									// have
																									// two
																									// listeners
																									// registered
									control.addMouseListener(snippetsMouseListener);
									control.addDragDetectListener(snippetsMouseDragListener);
									executed = true;
								}
							}
						}
					}
				}
			}

			/**
			 * This is called when something in the perspective have changed.
			 * For example: when a view is opened or closed
			 * 
			 * @see org.eclipse.ui.PerspectiveAdapter#perspectiveChanged(org.eclipse.ui.IWorkbenchPage,
			 *      org.eclipse.ui.IPerspectiveDescriptor,
			 *      org.eclipse.ui.IWorkbenchPartReference, java.lang.String)
			 */
			@Override
			public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective,
					IWorkbenchPartReference partRef, String changeId) {
				// check if it's the Snippet View
				if (SNIPPETS_VIEW_ID.equals(partRef.getId())) {
					// if it's the Snippet View and it's being OPENED
					if (IWorkbenchPage.CHANGE_VIEW_SHOW.equals(changeId)) {
						if (!executed) {
							SnippetsView snippetsView = (SnippetsView) partRef.getPart(false);
							addSearchBar(snippetsView);
							snippetsView.getViewer().getControl().removeMouseListener(snippetsMouseListener); // remove
																												// mouse
																												// listener
																												// just
																												// to
																												// ensure
																												// we
																												// never
																												// have
																												// two
																												// listeners
																												// registered
							snippetsView.getViewer().getControl().removeDragDetectListener(snippetsMouseDragListener); // remove
																														// mouse
																														// draggable
																														// listener
																														// just
																														// to
																														// ensure
																														// we
																														// never
																														// have
																														// two
																														// liseteners
																														// registered
							snippetsView.getViewer().getControl().addMouseListener(snippetsMouseListener);
							snippetsView.getViewer().getControl().addDragDetectListener(snippetsMouseDragListener);
							// it doesn't need to add the mouse listener to the
							// Snippets View in further opportunities
							executed = true;
						}
					}
					// if it's the Snippet View and it's being CLOSED
					else if (IWorkbenchPage.CHANGE_VIEW_HIDE.equals(changeId)) {
						// it must add the mouse listener to the Snippets View
						// again next time the view is opened
						if (searchContributionItem != null) {
							searchContributionItem.clean();
							searchContributionItem.getParent().remove(searchContributionItem);
						}

						if (tooltipDisplayConfigcontributionItem != null) {
							tooltipDisplayConfigcontributionItem.getParent().remove(
									tooltipDisplayConfigcontributionItem);
						}

						searchContributionItem = null;
						tooltipDisplayConfigcontributionItem = null;

						executed = false;
					}
				}

			}

		};

		/*
		 * Attach the perspectiveListener declared above into the active window
		 * Also try to add the snippetsMouseListener listener to the Snippet
		 * View, if it's already being displayed
		 */
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				/*
				 * Add mouse listener to Snippet View
				 */
				final IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

				activeWindow.addPerspectiveListener(perspectiveListener);

				IViewReference viewReference = activeWindow.getActivePage().findViewReference(SNIPPETS_VIEW_ID);

				if (viewReference != null) {
					final SnippetsView snippetsView = (SnippetsView) viewReference.getView(true);
					if (snippetsView != null) {
						addSearchBar(snippetsView);
						snippetsView.getViewer().getControl().addMouseListener(snippetsMouseListener);
						snippetsView.getViewer().getControl().addDragDetectListener(snippetsMouseDragListener);
					}

				}

			}

		});

	}
}