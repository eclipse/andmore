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

package org.eclipse.andmore.internal.lint;

import static com.android.SdkConstants.ATTR_IGNORE;
import static com.android.SdkConstants.ATTR_TARGET_API;
import static com.android.SdkConstants.DOT_XML;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.SdkVersionInfo;
import com.android.tools.lint.checks.ApiDetector;
import com.google.common.collect.Lists;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fix for adding {@code tools:ignore="id"} attributes in XML files.
 */
class AddSuppressAttribute implements ICompletionProposal {
    private final AndroidXmlEditor mEditor;
    private final String mId;
    private final IMarker mMarker;
    private final Element mElement;
    private final String mDescription;
    /**
     * Should it create a {@code tools:targetApi} attribute instead of a
     * {@code tools:ignore} attribute? If so pass a non null API level
     */
    private final String mTargetApi;


    private AddSuppressAttribute(
            @NonNull AndroidXmlEditor editor,
            @NonNull String id,
            @NonNull IMarker marker,
            @NonNull Element element,
            @NonNull String description,
            @Nullable String targetApi) {
        mEditor = editor;
        mId = id;
        mMarker = marker;
        mElement = element;
        mDescription = description;
        mTargetApi = targetApi;
    }

    @Override
    public Point getSelection(IDocument document) {
        return null;
    }

    @Override
    public String getAdditionalProposalInfo() {
        return null;
    }

    @Override
    public String getDisplayString() {
        return mDescription;
    }

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }

    @Override
    public Image getImage() {
        return IconFactory.getInstance().getIcon("newannotation"); //$NON-NLS-1$
    }

    @Override
    public void apply(IDocument document) {
        String attribute;
        String value;
        if (mTargetApi != null) {
            attribute = ATTR_TARGET_API;
            value = mTargetApi;
        } else {
            attribute = ATTR_IGNORE;
            value = mId;
        }
        AdtUtils.setToolsAttribute(mEditor, mElement, mDescription, attribute, value,
                true /*reveal*/, true /*append*/);

        try {
            // Remove the marker now that the suppress attribute has been added
            // (so the user doesn't have to re-run lint just to see it disappear)
            mMarker.delete();
        } catch (CoreException e) {
            AdtPlugin.log(e, "Could not remove marker");
        }
    }

    /**
     * Returns a quickfix to suppress a specific lint issue id on the node corresponding to
     * the given marker.
     *
     * @param editor the associated editor containing the marker
     * @param marker the marker to create fixes for
     * @param id the issue id
     * @return a list of fixes for this marker, possibly empty
     */
    @NonNull
    public static List<AddSuppressAttribute> createFixes(
            @NonNull AndroidXmlEditor editor,
            @NonNull IMarker marker,
            @NonNull String id) {
        // This only applies to XML files:
        String fileName = marker.getResource().getName();
        if (!fileName.endsWith(DOT_XML)) {
            return Collections.emptyList();
        }

        int offset = marker.getAttribute(IMarker.CHAR_START, -1);
        Node node;
        if (offset == -1) {
            node = DomUtilities.getNode(editor.getStructuredDocument(), 0);
            if (node != null) {
                node = node.getOwnerDocument().getDocumentElement();
            }
        } else {
            node = DomUtilities.getNode(editor.getStructuredDocument(), offset);
        }
        if (node == null) {
            return Collections.emptyList();
        }
        Document document = node.getOwnerDocument();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getParentNode();
        }
        if (node == null) {
            node = document.getDocumentElement();
            if (node == null) {
                return Collections.emptyList();
            }
        }

        String desc = String.format("Add ignore '%1$s\' to element", id);
        Element element = (Element) node;
        List<AddSuppressAttribute> fixes = Lists.newArrayListWithExpectedSize(2);
        fixes.add(new AddSuppressAttribute(editor, id, marker, element, desc, null));

        int api = -1;
        if (id.equals(ApiDetector.UNSUPPORTED.getId())
                || id.equals(ApiDetector.INLINED.getId())) {
            String message = marker.getAttribute(IMarker.MESSAGE, null);
            if (message != null) {
                Pattern pattern = Pattern.compile("\\s(\\d+)\\s"); //$NON-NLS-1$
                Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    api = Integer.parseInt(matcher.group(1));
                    String targetApi;
                    String buildCode = SdkVersionInfo.getBuildCode(api);
                    if (buildCode != null) {
                        targetApi = buildCode.toLowerCase(Locale.US);
                        fixes.add(new AddSuppressAttribute(editor, id, marker, element,
                                String.format("Add targetApi '%1$s\' to element", targetApi),
                                targetApi));
                    }
                    targetApi = Integer.toString(api);
                    fixes.add(new AddSuppressAttribute(editor, id, marker, element,
                            String.format("Add targetApi '%1$s\' to element", targetApi),
                            targetApi));
                }
            }
        }

        return fixes;
    }

    /**
     * Returns a quickfix to suppress a given issue type on the <b>root element</b>
     * of the given editor.
     *
     * @param editor the associated editor containing the marker
     * @param marker the marker to create fixes for
     * @param id the issue id
     * @return a fix for this marker, or null if unable
     */
    @Nullable
    public static AddSuppressAttribute createFixForAll(
            @NonNull AndroidXmlEditor editor,
            @NonNull IMarker marker,
            @NonNull String id) {
        // This only applies to XML files:
        String fileName = marker.getResource().getName();
        if (!fileName.endsWith(DOT_XML)) {
            return null;
        }

        Node node = DomUtilities.getNode(editor.getStructuredDocument(), 0);
        if (node != null) {
            node = node.getOwnerDocument().getDocumentElement();
            String desc = String.format("Add ignore '%1$s\' to element", id);
            Element element = (Element) node;
            return new AddSuppressAttribute(editor, id, marker, element, desc, null);
        }

        return null;
    }
}
