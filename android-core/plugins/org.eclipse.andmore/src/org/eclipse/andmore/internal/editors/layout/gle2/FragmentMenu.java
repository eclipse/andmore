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

import static com.android.SdkConstants.ANDROID_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.LAYOUT_RESOURCE_PREFIX;
import static org.eclipse.andmore.internal.editors.layout.gle2.LayoutMetadata.KEY_FRAGMENT_LAYOUT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.resources.CyclicDependencyValidator;
import org.eclipse.andmore.internal.ui.ResourceChooser;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Menu;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment context menu allowing a layout to be chosen for previewing in the fragment frame.
 */
public class FragmentMenu extends SubmenuAction {
    private static final String R_LAYOUT_RESOURCE_PREFIX = "R.layout."; //$NON-NLS-1$
    private static final String ANDROID_R_PREFIX = "android.R.layout"; //$NON-NLS-1$

    /** Associated canvas */
    private final LayoutCanvas mCanvas;

    /**
     * Creates a "Preview Fragment" menu
     *
     * @param canvas associated canvas
     */
    public FragmentMenu(LayoutCanvas canvas) {
        super("Fragment Layout");
        mCanvas = canvas;
    }

    @Override
    protected void addMenuItems(Menu menu) {
        IAction action = new PickLayoutAction("Choose Layout...");
        new ActionContributionItem(action).fill(menu, -1);

        SelectionManager selectionManager = mCanvas.getSelectionManager();
        List<SelectionItem> selections = selectionManager.getSelections();
        if (selections.size() == 0) {
            return;
        }

        SelectionItem first = selections.get(0);
        UiViewElementNode node = first.getViewInfo().getUiViewNode();
        if (node == null) {
            return;
        }
        Element element = (Element) node.getXmlNode();

        String selected = getSelectedLayout();
        if (selected != null) {
            if (selected.startsWith(ANDROID_LAYOUT_RESOURCE_PREFIX)) {
                selected = selected.substring(ANDROID_LAYOUT_RESOURCE_PREFIX.length());
            }
        }

        String fqcn = getFragmentClass(element);
        if (fqcn != null) {
            // Look up the corresponding activity class and try to figure out
            // which layouts it is referring to and list these here as reasonable
            // guesses
            IProject project = mCanvas.getEditorDelegate().getEditor().getProject();
            String source = null;
            try {
                IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
                IType type = javaProject.findType(fqcn);
                if (type != null) {
                    source = type.getSource();
                }
            } catch (CoreException e) {
                AdtPlugin.log(e, null);
            }
            // Find layouts. This is based on just skimming the Fragment class and looking
            // for layout references of the form R.layout.*.
            if (source != null) {
                String self = mCanvas.getLayoutResourceName();
                // Pair of <title,layout> to be displayed to the user
                List<Pair<String, String>> layouts = new ArrayList<Pair<String, String>>();

                if (source.contains("extends ListFragment")) { //$NON-NLS-1$
                    layouts.add(Pair.of("list_content", //$NON-NLS-1$
                            "@android:layout/list_content")); //$NON-NLS-1$
                }

                int index = 0;
                while (true) {
                    index = source.indexOf(R_LAYOUT_RESOURCE_PREFIX, index);
                    if (index == -1) {
                        break;
                    } else {
                        index += R_LAYOUT_RESOURCE_PREFIX.length();
                        int end = index;
                        while (end < source.length()) {
                            char c = source.charAt(end);
                            if (!Character.isJavaIdentifierPart(c)) {
                                break;
                            }
                            end++;
                        }
                        if (end > index) {
                            String title = source.substring(index, end);
                            String layout;
                            // Is this R.layout part of an android.R.layout?
                            int len = ANDROID_R_PREFIX.length() + 1; // prefix length to check
                            if (index > len && source.startsWith(ANDROID_R_PREFIX, index - len)) {
                                layout = ANDROID_LAYOUT_RESOURCE_PREFIX + title;
                            } else {
                                layout = LAYOUT_RESOURCE_PREFIX + title;
                            }
                            if (!self.equals(title)) {
                                layouts.add(Pair.of(title, layout));
                            }
                        }
                    }

                    index++;
                }

                if (layouts.size() > 0) {
                    new Separator().fill(menu, -1);
                    for (Pair<String, String> layout : layouts) {
                        action = new SetFragmentLayoutAction(layout.getFirst(),
                                layout.getSecond(), selected);
                        new ActionContributionItem(action).fill(menu, -1);
                    }
                }
            }
        }

        if (selected != null) {
            new Separator().fill(menu, -1);
            action = new SetFragmentLayoutAction("Clear", null, null);
            new ActionContributionItem(action).fill(menu, -1);
        }
    }

    /**
     * Returns the class name of the fragment associated with the given {@code <fragment>}
     * element.
     *
     * @param element the element for the fragment tag
     * @return the fully qualified fragment class name, or null
     */
    @Nullable
    public static String getFragmentClass(@NonNull Element element) {
        String fqcn = element.getAttribute(ATTR_CLASS);
        if (fqcn == null || fqcn.length() == 0) {
            fqcn = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
        }
        if (fqcn != null && fqcn.length() > 0) {
            return fqcn;
        } else {
            return null;
        }
    }

    /**
     * Returns the layout to be shown for the given {@code <fragment>} node.
     *
     * @param node the node corresponding to the {@code <fragment>} element
     * @return the resource path to a layout to render for this fragment, or null
     */
    @Nullable
    public static String getFragmentLayout(@NonNull Node node) {
        String layout = LayoutMetadata.getProperty(
                node, LayoutMetadata.KEY_FRAGMENT_LAYOUT);
        if (layout != null) {
            return layout;
        }

        return null;
    }

    /** Returns the name of the currently displayed layout in the fragment, or null */
    @Nullable
    private String getSelectedLayout() {
        SelectionManager selectionManager = mCanvas.getSelectionManager();
        for (SelectionItem item : selectionManager.getSelections()) {
            UiViewElementNode node = item.getViewInfo().getUiViewNode();
            if (node != null) {
                String layout = getFragmentLayout(node.getXmlNode());
                if (layout != null) {
                    return layout;
                }
            }
        }
        return null;
    }

    /**
     * Set the given layout as the new fragment layout
     *
     * @param layout the layout resource name to show in this fragment
     */
    public void setNewLayout(@Nullable String layout) {
        LayoutEditorDelegate delegate = mCanvas.getEditorDelegate();
        GraphicalEditorPart graphicalEditor = delegate.getGraphicalEditor();
        SelectionManager selectionManager = mCanvas.getSelectionManager();

        for (SelectionItem item : selectionManager.getSnapshot()) {
            UiViewElementNode node = item.getViewInfo().getUiViewNode();
            if (node != null) {
                Node xmlNode = node.getXmlNode();
                LayoutMetadata.setProperty(delegate.getEditor(), xmlNode, KEY_FRAGMENT_LAYOUT,
                        layout);
            }
        }

        // Refresh
        graphicalEditor.recomputeLayout();
        mCanvas.redraw();
    }

    /** Action to set the given layout as the new layout in a fragment */
    private class SetFragmentLayoutAction extends Action {
        private final String mLayout;

        public SetFragmentLayoutAction(String title, String layout, String selected) {
            super(title, IAction.AS_RADIO_BUTTON);
            mLayout = layout;

            if (layout != null && layout.equals(selected)) {
                setChecked(true);
            }
        }

        @Override
        public void run() {
            if (isChecked()) {
                setNewLayout(mLayout);
            }
        }
    }

    /**
     * Action which brings up the "Create new XML File" wizard, pre-selected with the
     * animation category
     */
    private class PickLayoutAction extends Action {

        public PickLayoutAction(String title) {
            super(title, IAction.AS_PUSH_BUTTON);
        }

        @Override
        public void run() {
            LayoutEditorDelegate delegate = mCanvas.getEditorDelegate();
            IFile file = delegate.getEditor().getInputFile();
            GraphicalEditorPart editor = delegate.getGraphicalEditor();
            ResourceChooser dlg = ResourceChooser.create(editor, ResourceType.LAYOUT)
                .setInputValidator(CyclicDependencyValidator.create(file))
                .setInitialSize(85, 10)
                .setCurrentResource(getSelectedLayout());
            int result = dlg.open();
            if (result == ResourceChooser.CLEAR_RETURN_CODE) {
                setNewLayout(null);
            } else if (result == Window.OK) {
                String newType = dlg.getCurrentResource();
                setNewLayout(newType);
            }
        }
    }
}
