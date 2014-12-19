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
import static com.android.SdkConstants.ATTR_NUM_COLUMNS;
import static com.android.SdkConstants.EXPANDABLE_LIST_VIEW;
import static com.android.SdkConstants.GRID_VIEW;
import static com.android.SdkConstants.LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.SdkConstants.VALUE_AUTO_FIT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.AdapterBinding;
import com.android.ide.common.rendering.api.DataBindingItem;
import com.android.ide.common.rendering.api.ResourceReference;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.layout.ProjectCallback;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.progress.WorkbenchJob;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Design-time metadata lookup for layouts, such as fragment and AdapterView bindings.
 */
public class LayoutMetadata {
    /** The default layout to use for list items in expandable list views */
    public static final String DEFAULT_EXPANDABLE_LIST_ITEM = "simple_expandable_list_item_2"; //$NON-NLS-1$
    /** The default layout to use for list items in plain list views */
    public static final String DEFAULT_LIST_ITEM = "simple_list_item_2"; //$NON-NLS-1$
    /** The default layout to use for list items in spinners */
    public static final String DEFAULT_SPINNER_ITEM = "simple_spinner_item"; //$NON-NLS-1$

    /** The string to start metadata comments with */
    private static final String COMMENT_PROLOGUE = " Preview: ";
    /** The property key, included in comments, which references a list item layout */
    public static final String KEY_LV_ITEM = "listitem";        //$NON-NLS-1$
    /** The property key, included in comments, which references a list header layout */
    public static final String KEY_LV_HEADER = "listheader";    //$NON-NLS-1$
    /** The property key, included in comments, which references a list footer layout */
    public static final String KEY_LV_FOOTER = "listfooter";    //$NON-NLS-1$
    /** The property key, included in comments, which references a fragment layout to show */
    public static final String KEY_FRAGMENT_LAYOUT = "layout";        //$NON-NLS-1$
    // NOTE: If you add additional keys related to resources, make sure you update the
    // ResourceRenameParticipant

    /** Utility class, do not create instances */
    private LayoutMetadata() {
    }

    /**
     * Returns the given property specified in the <b>current</b> element being
     * processed by the given pull parser.
     *
     * @param parser the pull parser, which must be in the middle of processing
     *            the target element
     * @param name the property name to look up
     * @return the property value, or null if not defined
     */
    @Nullable
    public static String getProperty(@NonNull XmlPullParser parser, @NonNull String name) {
        String value = parser.getAttributeValue(TOOLS_URI, name);
        if (value != null && value.isEmpty()) {
            value = null;
        }

        return value;
    }

    /**
     * Clears the old metadata from the given node
     *
     * @param node the XML node to associate metadata with
     * @deprecated this method clears metadata using the old comment-based style;
     *             should only be used for migration at this point
     */
    @Deprecated
    public static void clearLegacyComment(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.COMMENT_NODE) {
                String text = child.getNodeValue();
                if (text.startsWith(COMMENT_PROLOGUE)) {
                    Node commentNode = child;
                    // Remove the comment, along with surrounding whitespace if applicable
                    Node previous = commentNode.getPreviousSibling();
                    if (previous != null && previous.getNodeType() == Node.TEXT_NODE) {
                        if (previous.getNodeValue().trim().length() == 0) {
                            node.removeChild(previous);
                        }
                    }
                    node.removeChild(commentNode);
                    Node first = node.getFirstChild();
                    if (first != null && first.getNextSibling() == null
                            && first.getNodeType() == Node.TEXT_NODE) {
                        if (first.getNodeValue().trim().length() == 0) {
                            node.removeChild(first);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the given property of the given DOM node, or null
     *
     * @param node the XML node to associate metadata with
     * @param name the name of the property to look up
     * @return the value stored with the given node and name, or null
     */
    @Nullable
    public static String getProperty(
            @NonNull Node node,
            @NonNull String name) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String value = element.getAttributeNS(TOOLS_URI, name);
            if (value != null && value.isEmpty()) {
                value = null;
            }

            return value;
        }

        return null;
    }

    /**
     * Sets the given property of the given DOM node to a given value, or if null clears
     * the property.
     *
     * @param editor the editor associated with the property
     * @param node the XML node to associate metadata with
     * @param name the name of the property to set
     * @param value the value to store for the given node and name, or null to remove it
     */
    public static void setProperty(
            @NonNull final AndroidXmlEditor editor,
            @NonNull final Node node,
            @NonNull final String name,
            @Nullable final String value) {
        // Clear out the old metadata
        clearLegacyComment(node);

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            final Element element = (Element) node;
            final String undoLabel = "Bind View";
            AdtUtils.setToolsAttribute(editor, element, undoLabel, name, value,
                    false /*reveal*/, false /*append*/);

            // Also apply the same layout to any corresponding elements in other configurations
            // of this layout.
            final IFile file = editor.getInputFile();
            if (file != null) {
                final List<IFile> variations = AdtUtils.getResourceVariations(file, false);
                if (variations.isEmpty()) {
                    return;
                }
                Display display = AdtPlugin.getDisplay();
                WorkbenchJob job = new WorkbenchJob(display, "Update alternate views") {
                    @Override
                    public IStatus runInUIThread(IProgressMonitor monitor) {
                        for (IFile variation : variations) {
                            if (variation.equals(file)) {
                                continue;
                            }
                            try {
                                // If the corresponding file is open in the IDE, use the
                                // editor version instead
                                if (!AdtPrefs.getPrefs().isSharedLayoutEditor()) {
                                    if (setPropertyInEditor(undoLabel, variation, element, name,
                                            value)) {
                                        return Status.OK_STATUS;
                                    }
                                }

                                boolean old = editor.getIgnoreXmlUpdate();
                                try {
                                    editor.setIgnoreXmlUpdate(true);
                                    setPropertyInFile(undoLabel, variation, element, name, value);
                                } finally {
                                    editor.setIgnoreXmlUpdate(old);
                                }
                            } catch (Exception e) {
                                AdtPlugin.log(e, variation.getFullPath().toOSString());
                            }
                        }
                        return Status.OK_STATUS;
                    }

                };
                job.setSystem(true);
                job.schedule();
            }
        }
    }

    private static boolean setPropertyInEditor(
            @NonNull String undoLabel,
            @NonNull IFile variation,
            @NonNull final Element equivalentElement,
            @NonNull final String name,
            @Nullable final String value) {
        Collection<IEditorPart> editors =
                AdtUtils.findEditorsFor(variation, false /*restore*/);
        for (IEditorPart part : editors) {
            AndroidXmlEditor editor = AdtUtils.getXmlEditor(part);
            if (editor != null) {
                Document doc = DomUtilities.getDocument(editor);
                if (doc != null) {
                    Element element = DomUtilities.findCorresponding(equivalentElement, doc);
                    if (element != null) {
                        AdtUtils.setToolsAttribute(editor, element, undoLabel, name,
                                value, false /*reveal*/, false /*append*/);
                        if (part instanceof GraphicalEditorPart) {
                            GraphicalEditorPart g = (GraphicalEditorPart) part;
                            g.recomputeLayout();
                            g.getCanvasControl().redraw();
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean setPropertyInFile(
            @NonNull String undoLabel,
            @NonNull IFile variation,
            @NonNull final Element element,
            @NonNull final String name,
            @Nullable final String value) {
        Document doc = DomUtilities.getDocument(variation);
        if (doc != null && element.getOwnerDocument() != doc) {
            Element other = DomUtilities.findCorresponding(element, doc);
            if (other != null) {
                AdtUtils.setToolsAttribute(variation, other, undoLabel,
                        name, value, false);

                return true;
            }
        }

        return false;
    }

    /** Strips out @layout/ or @android:layout/ from the given layout reference */
    private static String stripLayoutPrefix(String layout) {
        if (layout.startsWith(ANDROID_LAYOUT_RESOURCE_PREFIX)) {
            layout = layout.substring(ANDROID_LAYOUT_RESOURCE_PREFIX.length());
        } else if (layout.startsWith(LAYOUT_RESOURCE_PREFIX)) {
            layout = layout.substring(LAYOUT_RESOURCE_PREFIX.length());
        }

        return layout;
    }

    /**
     * Creates an {@link AdapterBinding} for the given view object, or null if the user
     * has not yet chosen a target layout to use for the given AdapterView.
     *
     * @param viewObject the view object to create an adapter binding for
     * @param map a map containing tools attribute metadata
     * @return a binding, or null
     */
    @Nullable
    public static AdapterBinding getNodeBinding(
            @Nullable Object viewObject,
            @NonNull Map<String, String> map) {
        String header = map.get(KEY_LV_HEADER);
        String footer = map.get(KEY_LV_FOOTER);
        String layout = map.get(KEY_LV_ITEM);
        if (layout != null || header != null || footer != null) {
            int count = 12;
            return getNodeBinding(viewObject, header, footer, layout, count);
        }

        return null;
    }

    /**
     * Creates an {@link AdapterBinding} for the given view object, or null if the user
     * has not yet chosen a target layout to use for the given AdapterView.
     *
     * @param viewObject the view object to create an adapter binding for
     * @param uiNode the ui node corresponding to the view object
     * @return a binding, or null
     */
    @Nullable
    public static AdapterBinding getNodeBinding(
            @Nullable Object viewObject,
            @NonNull UiViewElementNode uiNode) {
        Node xmlNode = uiNode.getXmlNode();

        String header = getProperty(xmlNode, KEY_LV_HEADER);
        String footer = getProperty(xmlNode, KEY_LV_FOOTER);
        String layout = getProperty(xmlNode, KEY_LV_ITEM);
        if (layout != null || header != null || footer != null) {
            int count = 12;
            // If we're dealing with a grid view, multiply the list item count
            // by the number of columns to ensure we have enough items
            if (xmlNode instanceof Element && xmlNode.getNodeName().endsWith(GRID_VIEW)) {
                Element element = (Element) xmlNode;
                String columns = element.getAttributeNS(ANDROID_URI, ATTR_NUM_COLUMNS);
                int multiplier = 2;
                if (columns != null && columns.length() > 0 &&
                        !columns.equals(VALUE_AUTO_FIT)) {
                    try {
                        int c = Integer.parseInt(columns);
                        if (c >= 1 && c <= 10) {
                            multiplier = c;
                        }
                    } catch (NumberFormatException nufe) {
                        // some unexpected numColumns value: just stick with 2 columns for
                        // preview purposes
                    }
                }
                count *= multiplier;
            }

            return getNodeBinding(viewObject, header, footer, layout, count);
        }

        return null;
    }

    private static AdapterBinding getNodeBinding(Object viewObject,
            String header, String footer, String layout, int count) {
        if (layout != null || header != null || footer != null) {
            AdapterBinding binding = new AdapterBinding(count);

            if (header != null) {
                boolean isFramework = header.startsWith(ANDROID_LAYOUT_RESOURCE_PREFIX);
                binding.addHeader(new ResourceReference(stripLayoutPrefix(header),
                        isFramework));
            }

            if (footer != null) {
                boolean isFramework = footer.startsWith(ANDROID_LAYOUT_RESOURCE_PREFIX);
                binding.addFooter(new ResourceReference(stripLayoutPrefix(footer),
                        isFramework));
            }

            if (layout != null) {
                boolean isFramework = layout.startsWith(ANDROID_LAYOUT_RESOURCE_PREFIX);
                if (isFramework) {
                    layout = layout.substring(ANDROID_LAYOUT_RESOURCE_PREFIX.length());
                } else if (layout.startsWith(LAYOUT_RESOURCE_PREFIX)) {
                    layout = layout.substring(LAYOUT_RESOURCE_PREFIX.length());
                }

                binding.addItem(new DataBindingItem(layout, isFramework, 1));
            } else if (viewObject != null) {
                String listFqcn = ProjectCallback.getListAdapterViewFqcn(viewObject.getClass());
                if (listFqcn != null) {
                    if (listFqcn.endsWith(EXPANDABLE_LIST_VIEW)) {
                        binding.addItem(
                                new DataBindingItem(DEFAULT_EXPANDABLE_LIST_ITEM,
                                true /* isFramework */, 1));
                    } else {
                        binding.addItem(
                                new DataBindingItem(DEFAULT_LIST_ITEM,
                                true /* isFramework */, 1));
                    }
                }
            } else {
                binding.addItem(
                        new DataBindingItem(DEFAULT_LIST_ITEM,
                        true /* isFramework */, 1));
            }
            return binding;
        }

        return null;
    }
}
