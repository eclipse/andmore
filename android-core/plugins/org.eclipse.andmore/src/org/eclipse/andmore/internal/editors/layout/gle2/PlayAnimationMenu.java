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
package org.eclipse.andmore.internal.editors.layout.gle2;

import static com.android.SdkConstants.FD_RESOURCES;
import static com.android.SdkConstants.FD_RES_ANIMATOR;
import static org.eclipse.andmore.AdtConstants.WS_SEP;

import com.android.ide.common.rendering.api.Capability;
import com.android.ide.common.rendering.api.IAnimationListener;
import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.Result;
import com.android.resources.ResourceType;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.wizards.newxmlfile.NewXmlFileWizard;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * "Play Animation" context menu which lists available animations in the project and in
 * the framework, as well as a "Create Animation" shortcut, and allows the animation to be
 * run on the selection
 * <p/>
 * TODO: Add transport controls for play/rewind/pause/loop, and (if possible) scrubbing
 */
public class PlayAnimationMenu extends SubmenuAction {
    /** Associated canvas */
    private final LayoutCanvas mCanvas;
    /** Whether this menu is showing local animations or framework animations */
    private boolean mFramework;

    /**
     * Creates a "Play Animation" menu
     *
     * @param canvas associated canvas
     */
    public PlayAnimationMenu(LayoutCanvas canvas) {
        this(canvas, "Play Animation", false);
    }

    /**
     * Creates an animation menu; this can be used either for the outer Play animation
     * menu, or the inner frameworks-animations list
     *
     * @param canvas the associated canvas
     * @param title menu item name
     * @param framework true to show the framework animations, false for the project (and
     *            nested framework-animation-menu) animations
     */
    private PlayAnimationMenu(LayoutCanvas canvas, String title, boolean framework) {
        super(title);
        mCanvas = canvas;
        mFramework = framework;
    }

    @Override
    protected void addMenuItems(Menu menu) {
        SelectionManager selectionManager = mCanvas.getSelectionManager();
        List<SelectionItem> selection = selectionManager.getSelections();
        if (selection.size() != 1) {
            addDisabledMessageItem("Select exactly one widget");
            return;
        }

        GraphicalEditorPart graphicalEditor = mCanvas.getEditorDelegate().getGraphicalEditor();
        if (graphicalEditor.renderingSupports(Capability.PLAY_ANIMATION)) {
            // List of animations
            Collection<String> animationNames = graphicalEditor.getResourceNames(mFramework,
                    ResourceType.ANIMATOR);
            if (animationNames.size() > 0) {
                // Sort alphabetically
                List<String> sortedNames = new ArrayList<String>(animationNames);
                Collections.sort(sortedNames);

                for (String animation : sortedNames) {
                    String title = animation;
                    IAction action = new PlayAnimationAction(title, animation, mFramework);
                    new ActionContributionItem(action).fill(menu, -1);
                }

                new Separator().fill(menu, -1);
            }

            if (!mFramework) {
                // Not in the framework submenu: include recent list and create new actions

                // "Create New" action
                new ActionContributionItem(new CreateAnimationAction()).fill(menu, -1);

                // Framework resources submenu
                new Separator().fill(menu, -1);
                PlayAnimationMenu sub = new PlayAnimationMenu(mCanvas, "Android Builtin", true);
                new ActionContributionItem(sub).fill(menu, -1);
            }
        } else {
            addDisabledMessageItem(
                    "Not supported for this SDK version; try changing the Render Target");
        }
    }

    private class PlayAnimationAction extends Action {
        private final String mAnimationName;
        private final boolean mIsFrameworkAnim;

        public PlayAnimationAction(String title, String animationName, boolean isFrameworkAnim) {
            super(title, IAction.AS_PUSH_BUTTON);
            mAnimationName = animationName;
            mIsFrameworkAnim = isFrameworkAnim;
        }

        @Override
        public void run() {
            SelectionManager selectionManager = mCanvas.getSelectionManager();
            List<SelectionItem> selection = selectionManager.getSelections();
            SelectionItem canvasSelection = selection.get(0);
            CanvasViewInfo info = canvasSelection.getViewInfo();

            Object viewObject = info.getViewObject();
            if (viewObject != null) {
                ViewHierarchy viewHierarchy = mCanvas.getViewHierarchy();
                RenderSession session = viewHierarchy.getSession();
                Result r = session.animate(viewObject, mAnimationName, mIsFrameworkAnim,
                        new IAnimationListener() {
                            private boolean mPendingDrawing = false;

                            @Override
                            public void onNewFrame(RenderSession s) {
                                SelectionOverlay selectionOverlay = mCanvas.getSelectionOverlay();
                                if (!selectionOverlay.isHiding()) {
                                    selectionOverlay.setHiding(true);
                                }
                                HoverOverlay hoverOverlay = mCanvas.getHoverOverlay();
                                if (!hoverOverlay.isHiding()) {
                                    hoverOverlay.setHiding(true);
                                }

                                ImageOverlay imageOverlay = mCanvas.getImageOverlay();
                                imageOverlay.setImage(s.getImage(), s.isAlphaChannelImage());
                                synchronized (this) {
                                    if (mPendingDrawing == false) {
                                        mCanvas.getDisplay().asyncExec(new Runnable() {
                                            @Override
                                            public void run() {
                                                synchronized (this) {
                                                    mPendingDrawing = false;
                                                }
                                                mCanvas.redraw();
                                            }
                                        });
                                        mPendingDrawing = true;
                                    }
                                }
                            }

                            @Override
                            public boolean isCanceled() {
                                return false;
                            }

                            @Override
                            public void done(Result result) {
                                SelectionOverlay selectionOverlay = mCanvas.getSelectionOverlay();
                                selectionOverlay.setHiding(false);
                                HoverOverlay hoverOverlay = mCanvas.getHoverOverlay();
                                hoverOverlay.setHiding(false);

                                // Must refresh view hierarchy to force objects back to
                                // their original positions in case animations have left
                                // them elsewhere
                                mCanvas.getDisplay().asyncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        GraphicalEditorPart graphicalEditor = mCanvas
                                                .getEditorDelegate().getGraphicalEditor();
                                        graphicalEditor.recomputeLayout();
                                    }
                                });
                            }
                        });

                if (!r.isSuccess()) {
                    if (r.getErrorMessage() != null) {
                        AdtPlugin.log(r.getException(), r.getErrorMessage());
                    }
                }
            }
        }
    }

    /**
     * Action which brings up the "Create new XML File" wizard, pre-selected with the
     * animation category
     */
    private class CreateAnimationAction extends Action {
        public CreateAnimationAction() {
            super("Create...", IAction.AS_PUSH_BUTTON);
        }

        @Override
        public void run() {
            Shell parent = mCanvas.getShell();
            NewXmlFileWizard wizard = new NewXmlFileWizard();
            LayoutEditorDelegate editor = mCanvas.getEditorDelegate();
            IWorkbenchWindow workbenchWindow =
                editor.getEditor().getEditorSite().getWorkbenchWindow();
            IWorkbench workbench = workbenchWindow.getWorkbench();
            String animationDir = FD_RESOURCES + WS_SEP + FD_RES_ANIMATOR;
            Pair<IProject, String> pair = Pair.of(editor.getEditor().getProject(), animationDir);
            IStructuredSelection selection = new StructuredSelection(pair);
            wizard.init(workbench, selection);
            WizardDialog dialog = new WizardDialog(parent, wizard);
            dialog.create();
            dialog.open();
        }
    }
}
