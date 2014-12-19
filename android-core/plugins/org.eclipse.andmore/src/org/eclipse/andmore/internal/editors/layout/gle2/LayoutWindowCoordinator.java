/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.editors.layout.gle2;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.collect.Maps;

import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;

import java.util.Map;

/**
 * The {@link LayoutWindowCoordinator} keeps track of Eclipse window events (opening, closing,
 * fronting, etc) and uses this information to manage the propertysheet and outline
 * views such that they are always(*) showing:
 * <ul>
 *  <li> If the Property Sheet and Outline Eclipse views are showing, it does nothing.
 *       "Showing" means "is open", not necessary "is visible", e.g. in a tabbed view
 *       there could be a different view on top.
 *  <li> If just the outline is showing, then the property sheet is shown in a sashed
 *       pane below or to the right of the outline (depending on the dominant dimension
 *       of the window).
 *  <li> TBD: If just the property sheet is showing, should the outline be showed
 *       inside that window? Not yet done.
 *  <li> If the outline is *not* showing, then the outline is instead shown
 *       <b>inside</b> the editor area, in a right-docked view! This right docked view
 *       also includes the property sheet!
 *  <li> If the property sheet is not showing (which includes not showing in the outline
 *       view as well), then it will be shown inside the editor area, along with the outline
 *       which should also be there (since if the outline was showing outside the editor
 *       area, the property sheet would have docked there).
 *  <li> When the editor is maximized, then all views are temporarily hidden. In this
 *       case, the property sheet and outline will show up inside the editor.
 *       When the editor view is un-maximized, the view state will return to what it
 *       was before.
 * </ul>
 * </p>
 * There is one coordinator per workbench window, shared between all editors in that window.
 * <p>
 * TODO: Rename this class to AdtWindowCoordinator. It is used for more than just layout
 * window coordination now. For example, it's also used to dispatch {@code activated()} and
 * {@code deactivated()} events to all the XML editors, to ensure that key bindings are
 * properly dispatched to the right editors in Eclipse 4.x.
 */
public class LayoutWindowCoordinator implements IPartListener2 {
    static final String PROPERTY_SHEET_PART_ID = "org.eclipse.ui.views.PropertySheet"; //$NON-NLS-1$
    static final String OUTLINE_PART_ID = "org.eclipse.ui.views.ContentOutline"; //$NON-NLS-1$
    /** The workbench window */
    private final IWorkbenchWindow mWindow;
    /** Is the Eclipse property sheet ViewPart open? */
    private boolean mPropertiesOpen;
    /** Is the Eclipse outline ViewPart open? */
    private boolean mOutlineOpen;
    /** Is the editor maximized? */
    private boolean mEditorMaximized;
    /**
     * Has the coordinator been initialized? We may have to delay initialization
     * and perform it lazily if the workbench window does not have an active
     * page when the coordinator is first started
     */
    private boolean mInitialized;

    /** Map from workbench windows to each layout window coordinator instance for that window */
    private static Map<IWorkbenchWindow, LayoutWindowCoordinator> sCoordinators =
            Maps.newHashMapWithExpectedSize(2);

    /**
     * Returns the coordinator for the given window.
     *
     * @param window the associated window
     * @param create whether to create the window if it does not already exist
     * @return the new coordinator, never null if {@code create} is true
     */
    @Nullable
    public static LayoutWindowCoordinator get(@NonNull IWorkbenchWindow window, boolean create) {
        synchronized (LayoutWindowCoordinator.class){
            LayoutWindowCoordinator coordinator = sCoordinators.get(window);
            if (coordinator == null && create) {
                coordinator = new LayoutWindowCoordinator(window);

                IPartService service = window.getPartService();
                if (service != null) {
                    // What if the editor part is *already* open? How do I deal with that?
                    service.addPartListener(coordinator);
                }

                sCoordinators.put(window, coordinator);
            }

            return coordinator;
        }
    }


    /** Disposes this coordinator (when a window is closed) */
    public void dispose() {
        IPartService service = mWindow.getPartService();
        if (service != null) {
            service.removePartListener(this);
        }

        synchronized (LayoutWindowCoordinator.class){
            sCoordinators.remove(mWindow);
        }
    }

    /**
     * Returns true if the main editor window is maximized
     *
     * @return true if the main editor window is maximized
     */
    public boolean isEditorMaximized() {
        return mEditorMaximized;
    }

    private LayoutWindowCoordinator(@NonNull IWorkbenchWindow window) {
        mWindow = window;

        initialize();
    }

    private void initialize() {
        if (mInitialized) {
            return;
        }

        IWorkbenchPage activePage = mWindow.getActivePage();
        if (activePage == null) {
            return;
        }

        mInitialized = true;

        // Look up current state of the properties and outline windows (in case
        // they have already been opened before we added our part listener)
        IViewReference ref = findPropertySheetView(activePage);
        if (ref != null) {
            IWorkbenchPart part = ref.getPart(false /*restore*/);
            if (activePage.isPartVisible(part)) {
                mPropertiesOpen = true;
            }
        }
        ref = findOutlineView(activePage);
        if (ref != null) {
            IWorkbenchPart part = ref.getPart(false /*restore*/);
            if (activePage.isPartVisible(part)) {
                mOutlineOpen = true;
            }
        }
        if (!syncMaximizedState(activePage)) {
            syncActive();
        }
    }

    static IViewReference findPropertySheetView(IWorkbenchPage activePage) {
        return activePage.findViewReference(PROPERTY_SHEET_PART_ID);
    }

    static IViewReference findOutlineView(IWorkbenchPage activePage) {
        return activePage.findViewReference(OUTLINE_PART_ID);
    }

    /**
     * Checks the maximized state of the page and updates internal state if
     * necessary.
     * <p>
     * This is used in Eclipse 4.x, where the {@link IPartListener2} does not
     * fire {@link IPartListener2#partHidden(IWorkbenchPartReference)} when the
     * editor is maximized anymore (see issue
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=382120 for details).
     * Instead, the layout editor listens for resize events, and upon resize it
     * looks up the part state and calls this method to ensure that the right
     * maximized state is known to the layout coordinator.
     *
     * @param page the active workbench page
     * @return true if the state changed, false otherwise
     */
    public boolean syncMaximizedState(IWorkbenchPage page) {
        boolean maximized = isPageZoomed(page);
        if (mEditorMaximized != maximized) {
            mEditorMaximized = maximized;
            syncActive();
            return true;
        }
        return false;
    }

    private boolean isPageZoomed(IWorkbenchPage page) {
        IWorkbenchPartReference reference = page.getActivePartReference();
        if (reference != null && reference instanceof IEditorReference) {
            int state = page.getPartState(reference);
            boolean maximized = (state & IWorkbenchPage.STATE_MAXIMIZED) != 0;
            return maximized;
        }

        // If the active reference isn't the editor, then the editor can't be maximized
        return false;
    }

    /**
     * Syncs the given editor's view state such that the property sheet and or
     * outline are shown or hidden according to the visibility of the global
     * outline and property sheet views.
     * <p>
     * This is typically done when a layout editor is fronted. For view updates
     * when the view is already showing, the {@link LayoutWindowCoordinator}
     * will automatically handle the current fronted window.
     *
     * @param editor the editor to sync
     */
    private void sync(@Nullable GraphicalEditorPart editor) {
        if (editor == null) {
            return;
        }
        if (mEditorMaximized) {
            editor.showStructureViews(true /*outline*/, true /*properties*/, true /*layout*/);
        } else if (mOutlineOpen) {
            editor.showStructureViews(false /*outline*/, false /*properties*/, true /*layout*/);
            editor.getCanvasControl().getOutlinePage().setShowPropertySheet(!mPropertiesOpen);
        } else {
            editor.showStructureViews(true /*outline*/, !mPropertiesOpen /*properties*/,
                    true /*layout*/);
        }
    }

    private void sync(IWorkbenchPart part) {
        if (part instanceof AndroidXmlEditor) {
            LayoutEditorDelegate editor = LayoutEditorDelegate.fromEditor((IEditorPart) part);
            if (editor != null) {
                sync(editor.getGraphicalEditor());
            }
        }
    }

    private void syncActive() {
        IWorkbenchPage activePage = mWindow.getActivePage();
        if (activePage != null) {
            IEditorPart editor = activePage.getActiveEditor();
            sync(editor);
        }
    }

    private void propertySheetClosed() {
        mPropertiesOpen = false;
        syncActive();
    }

    private void propertySheetOpened() {
        mPropertiesOpen = true;
        syncActive();
    }

    private void outlineClosed() {
        mOutlineOpen = false;
        syncActive();
    }

    private void outlineOpened() {
        mOutlineOpen = true;
        syncActive();
    }

    // ---- Implements IPartListener2 ----

    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
        // We ignore partOpened() and partClosed() because these methods are only
        // called when a view is opened in the first perspective, and closed in the
        // last perspective. The outline is typically used in multiple perspectives,
        // so closing it in the Java perspective does *not* fire a partClosed event.
        // There is no notification for "part closed in perspective" (see issue
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=54559 for details).
        // However, the workaround we can use is to listen to partVisible() and
        // partHidden(). These will be called more often than we'd like (e.g.
        // when the tab order causes a view to be obscured), however, we can use
        // the workaround of looking up IWorkbenchPage.findViewReference(id) after
        // partHidden(), which will return null if the view is closed in the current
        // perspective. For partOpened, we simply look in partVisible() for whether
        // our flags tracking the view state have been initialized already.
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
        // partClosed() doesn't get called when a window is closed unless it has
        // been closed in *all* perspectives. See partOpened() for more.
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
        IWorkbenchPage activePage = mWindow.getActivePage();
        if (activePage == null) {
            return;
        }
        initialize();

        // See if this looks like the window was closed in this workspace
        // See partOpened() for an explanation.
        String id = partRef.getId();
        if (PROPERTY_SHEET_PART_ID.equals(id)) {
            if (activePage.findViewReference(id) == null) {
                propertySheetClosed();
                return;
            }
        } else if (OUTLINE_PART_ID.equals(id)) {
            if (activePage.findViewReference(id) == null) {
                outlineClosed();
                return;
            }
        }

        // Does this look like a window getting maximized?
        syncMaximizedState(activePage);
    }

    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
        IWorkbenchPage activePage = mWindow.getActivePage();
        if (activePage == null) {
            return;
        }
        initialize();

        String id = partRef.getId();
        if (mEditorMaximized) {
            // Return to their non-maximized state
            mEditorMaximized = false;
            syncActive();
        }

        IWorkbenchPart part = partRef.getPart(false /*restore*/);
        sync(part);

        // See partOpened() for an explanation
        if (PROPERTY_SHEET_PART_ID.equals(id)) {
            if (!mPropertiesOpen) {
                propertySheetOpened();
                assert mPropertiesOpen;
            }
        } else if (OUTLINE_PART_ID.equals(id)) {
            if (!mOutlineOpen) {
                outlineOpened();
                assert mOutlineOpen;
            }
        }
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        if (part instanceof AndroidXmlEditor) {
            ((AndroidXmlEditor)part).activated();
        }
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        if (part instanceof AndroidXmlEditor) {
            ((AndroidXmlEditor)part).deactivated();
        }
    }
}