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
package org.eclipse.andmore.internal.editors.layout.refactoring;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_DRAWABLE_BOTTOM;
import static com.android.SdkConstants.ATTR_DRAWABLE_LEFT;
import static com.android.SdkConstants.ATTR_DRAWABLE_PADDING;
import static com.android.SdkConstants.ATTR_DRAWABLE_RIGHT;
import static com.android.SdkConstants.ATTR_DRAWABLE_TOP;
import static com.android.SdkConstants.ATTR_GRAVITY;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_ORIENTATION;
import static com.android.SdkConstants.ATTR_SRC;
import static com.android.SdkConstants.EXT_XML;
import static com.android.SdkConstants.IMAGE_VIEW;
import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.TEXT_VIEW;
import static com.android.SdkConstants.VALUE_VERTICAL;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.xml.XmlFormatStyle;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlFormatPreferences;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlPrettyPrinter;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a LinearLayout with exactly a TextView child and an ImageView child into
 * a single TextView with a compound drawable.
 */
@SuppressWarnings("restriction") // XML model
public class UseCompoundDrawableRefactoring extends VisualRefactoring {
    /**
     * Constructs a new {@link UseCompoundDrawableRefactoring}
     *
     * @param file the file to refactor in
     * @param editor the corresponding editor
     * @param selection the editor selection, or null
     * @param treeSelection the canvas selection, or null
     */
    public UseCompoundDrawableRefactoring(IFile file, LayoutEditorDelegate editor,
            ITextSelection selection, ITreeSelection treeSelection) {
        super(file, editor, selection, treeSelection);
    }

    /**
     * This constructor is solely used by {@link Descriptor}, to replay a
     * previous refactoring.
     *
     * @param arguments argument map created by #createArgumentMap.
     */
    private UseCompoundDrawableRefactoring(Map<String, String> arguments) {
        super(arguments);
    }

    @VisibleForTesting
    UseCompoundDrawableRefactoring(List<Element> selectedElements, LayoutEditorDelegate editor) {
        super(selectedElements, editor);
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();

        try {
            pm.beginTask("Checking preconditions...", 6);

            if (mSelectionStart == -1 || mSelectionEnd == -1) {
                status.addFatalError("Nothing to convert");
                return status;
            }

            // Make sure the selection is contiguous
            if (mTreeSelection != null) {
                List<CanvasViewInfo> infos = getSelectedViewInfos();
                if (!validateNotEmpty(infos, status)) {
                    return status;
                }

                // Enforce that the selection is -contiguous-
                if (!validateContiguous(infos, status)) {
                    return status;
                }
            }

            // Ensures that we have a valid DOM model:
            if (mElements.size() == 0) {
                status.addFatalError("Nothing to convert");
                return status;
            }

            // Ensure that we have selected precisely one LinearLayout
            if (mElements.size() != 1 ||
                    !(mElements.get(0).getTagName().equals(LINEAR_LAYOUT))) {
                status.addFatalError("Must select exactly one LinearLayout");
                return status;
            }

            Element layout = mElements.get(0);
            List<Element> children = DomUtilities.getChildren(layout);
            if (children.size() != 2) {
                status.addFatalError("The LinearLayout must have exactly two children");
                return status;
            }
            Element first = children.get(0);
            Element second = children.get(1);
            boolean haveTextView =
                    first.getTagName().equals(TEXT_VIEW)
                    || second.getTagName().equals(TEXT_VIEW);
            boolean haveImageView =
                    first.getTagName().equals(IMAGE_VIEW)
                    || second.getTagName().equals(IMAGE_VIEW);
            if (!(haveTextView && haveImageView)) {
                status.addFatalError("The LinearLayout must have exactly one TextView child " +
                        "and one ImageView child");
                return status;
            }

            pm.worked(1);
            return status;

        } finally {
            pm.done();
        }
    }

    @Override
    protected VisualRefactoringDescriptor createDescriptor() {
        String comment = getName();
        return new Descriptor(
                mProject.getName(), //project
                comment, //description
                comment, //comment
                createArgumentMap());
    }

    @Override
    protected Map<String, String> createArgumentMap() {
        return super.createArgumentMap();
    }

    @Override
    public String getName() {
        return "Convert to Compound Drawable";
    }

    @Override
    protected @NonNull List<Change> computeChanges(IProgressMonitor monitor) {
        String androidNsPrefix = getAndroidNamespacePrefix();
        IFile file = mDelegate.getEditor().getInputFile();
        List<Change> changes = new ArrayList<Change>();
        if (file == null) {
            return changes;
        }
        TextFileChange change = new TextFileChange(file.getName(), file);
        MultiTextEdit rootEdit = new MultiTextEdit();
        change.setTextType(EXT_XML);

        // (1) Build up the contents of the new TextView. This is identical
        //     to the old contents, but with the addition of a drawableTop/Left/Right/Bottom
        //     attribute (depending on the orientation and order), as well as any layout
        //     params from the LinearLayout.
        // (2) Delete the linear layout and replace with the text view.
        // (3) Reformat.

        // checkInitialConditions has already validated that we have exactly a LinearLayout
        // with an ImageView and a TextView child (in either order)
        Element layout = mElements.get(0);
        List<Element> children = DomUtilities.getChildren(layout);
        Element first = children.get(0);
        Element second = children.get(1);
        final Element text;
        final Element image;
        if (first.getTagName().equals(TEXT_VIEW)) {
            text = first;
            image = second;
        } else {
            text = second;
            image = first;
        }

        // Horizontal is the default, so if no value is specified it is horizontal.
        boolean isVertical = VALUE_VERTICAL.equals(layout.getAttributeNS(ANDROID_URI,
                ATTR_ORIENTATION));

        // The WST DOM implementation doesn't correctly implement cloneNode: this returns
        // an empty document instead:
        //   text.getOwnerDocument().cloneNode(false/*deep*/);
        // Luckily we just need to clone a single element, not a nested structure, so it's
        // easy enough to do this manually:
        Document tempDocument = DomUtilities.createEmptyDocument();
        if (tempDocument == null) {
            return changes;
        }
        Element newTextElement = tempDocument.createElement(text.getTagName());
        tempDocument.appendChild(newTextElement);

        NamedNodeMap attributes =  text.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Attr attribute = (Attr) attributes.item(i);
            String name = attribute.getLocalName();
            if (name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)
                    && ANDROID_URI.equals(attribute.getNamespaceURI())
                    && !(name.equals(ATTR_LAYOUT_WIDTH) || name.equals(ATTR_LAYOUT_HEIGHT))) {
                // Ignore layout params: the parent layout is going away
            } else {
                newTextElement.setAttribute(attribute.getName(), attribute.getValue());
            }
        }

        // Apply all layout params from the parent (except width and height),
        // as well as android:gravity
        List<Attr> layoutAttributes = findLayoutAttributes(layout);
        for (Attr attribute : layoutAttributes) {
            String name = attribute.getLocalName();
            if ((name.equals(ATTR_LAYOUT_WIDTH) || name.equals(ATTR_LAYOUT_HEIGHT))
                    && ANDROID_URI.equals(attribute.getNamespaceURI())) {
                // Already handled specially
                continue;
            }
            newTextElement.setAttribute(attribute.getName(), attribute.getValue());
        }
        String gravity = layout.getAttributeNS(ANDROID_URI, ATTR_GRAVITY);
        if (gravity.length() > 0) {
            setAndroidAttribute(newTextElement, androidNsPrefix, ATTR_GRAVITY, gravity);
        }

        String src = image.getAttributeNS(ANDROID_URI, ATTR_SRC);

        // Set the drawable
        String drawableAttribute;
        // The space between the image and the text can have margins/padding, both
        // from the text's perspective and from the image's perspective. We need to
        // combine these.
        String padding1 = null;
        String padding2 = null;
        if (isVertical) {
            if (first == image) {
                drawableAttribute = ATTR_DRAWABLE_TOP;
                padding1 = getPadding(image, ATTR_LAYOUT_MARGIN_BOTTOM);
                padding2 = getPadding(text, ATTR_LAYOUT_MARGIN_TOP);
            } else {
                drawableAttribute = ATTR_DRAWABLE_BOTTOM;
                padding1 = getPadding(text, ATTR_LAYOUT_MARGIN_BOTTOM);
                padding2 = getPadding(image, ATTR_LAYOUT_MARGIN_TOP);
            }
        } else {
            if (first == image) {
                drawableAttribute = ATTR_DRAWABLE_LEFT;
                padding1 = getPadding(image, ATTR_LAYOUT_MARGIN_RIGHT);
                padding2 = getPadding(text, ATTR_LAYOUT_MARGIN_LEFT);
            } else {
                drawableAttribute = ATTR_DRAWABLE_RIGHT;
                padding1 = getPadding(text, ATTR_LAYOUT_MARGIN_RIGHT);
                padding2 = getPadding(image, ATTR_LAYOUT_MARGIN_LEFT);
            }
        }

        setAndroidAttribute(newTextElement, androidNsPrefix, drawableAttribute, src);

        String padding = combine(padding1, padding2);
        if (padding != null) {
            setAndroidAttribute(newTextElement, androidNsPrefix, ATTR_DRAWABLE_PADDING, padding);
        }

        // If the removed LinearLayout is the root container, transfer its namespace
        // declaration to the TextView
        if (layout.getParentNode() instanceof Document) {
            List<Attr> declarations = findNamespaceAttributes(layout);
            for (Attr attribute : declarations) {
                if (attribute instanceof IndexedRegion) {
                    newTextElement.setAttribute(attribute.getName(), attribute.getValue());
                }
            }
        }

        // Update any layout references to the layout to point to the text view
        String layoutId = getId(layout);
        if (layoutId.length() > 0) {
            String id = getId(text);
            if (id.length() == 0) {
                id = ensureHasId(rootEdit, text, null, false);
                setAndroidAttribute(newTextElement, androidNsPrefix, ATTR_ID, id);
            }

            IStructuredModel model = mDelegate.getEditor().getModelForRead();
            try {
                IStructuredDocument doc = model.getStructuredDocument();
                if (doc != null) {
                    List<TextEdit> replaceIds = replaceIds(androidNsPrefix,
                            doc, mSelectionStart, mSelectionEnd, layoutId, id);
                    for (TextEdit edit : replaceIds) {
                        rootEdit.addChild(edit);
                    }
                }
            } finally {
                model.releaseFromRead();
            }
        }

        String xml = EclipseXmlPrettyPrinter.prettyPrint(
                tempDocument.getDocumentElement(),
                EclipseXmlFormatPreferences.create(),
                XmlFormatStyle.LAYOUT, null, false);

        TextEdit replace = new ReplaceEdit(mSelectionStart, mSelectionEnd - mSelectionStart, xml);
        rootEdit.addChild(replace);

        if (AdtPrefs.getPrefs().getFormatGuiXml()) {
            MultiTextEdit formatted = reformat(rootEdit, XmlFormatStyle.LAYOUT);
            if (formatted != null) {
                rootEdit = formatted;
            }
        }

        change.setEdit(rootEdit);
        changes.add(change);
        return changes;
    }

    @Nullable
    private static String getPadding(@NonNull Element element, @NonNull String attribute) {
        String padding = element.getAttributeNS(ANDROID_URI, attribute);
        if (padding != null && padding.isEmpty()) {
            padding = null;
        }
        return padding;
    }

    @VisibleForTesting
    @Nullable
    static String combine(@Nullable String dimension1, @Nullable String dimension2) {
        if (dimension1 == null || dimension1.isEmpty()) {
            if (dimension2 != null && dimension2.isEmpty()) {
                return null;
            }
            return dimension2;
        } else if (dimension2 == null || dimension2.isEmpty()) {
            if (dimension1 != null && dimension1.isEmpty()) {
                return null;
            }
            return dimension1;
        } else {
            // Two dimensions are specified (e.g. marginRight for the left one and marginLeft
            // for the right one); we have to add these together. We can only do that if
            // they use the same units, and do not use resources.
            if (dimension1.startsWith(PREFIX_RESOURCE_REF)
                    || dimension2.startsWith(PREFIX_RESOURCE_REF)) {
                return null;
            }

            Pattern p = Pattern.compile("([\\d\\.]+)(.+)"); //$NON-NLS-1$
            Matcher matcher1 = p.matcher(dimension1);
            Matcher matcher2 = p.matcher(dimension2);
            if (matcher1.matches() && matcher2.matches()) {
                String unit = matcher1.group(2);
                if (unit.equals(matcher2.group(2))) {
                    float value1 = Float.parseFloat(matcher1.group(1));
                    float value2 = Float.parseFloat(matcher2.group(1));
                    return AdtUtils.formatFloatAttribute(value1 + value2) + unit;
                }
            }
        }

        return null;
    }

    /**
     * Sets an Android attribute (in the Android namespace) on an element
     * without a given namespace prefix. This is done when building a new Element
     * in a temporary document such that the namespace prefix matches when the element is
     * formatted and replaced in the target document.
     */
    private static void setAndroidAttribute(Element element, String prefix, String name,
            String value) {
        element.setAttribute(prefix + ':' + name, value);
    }

    @Override
    public VisualRefactoringWizard createWizard() {
        return new UseCompoundDrawableWizard(this, mDelegate);
    }

    @SuppressWarnings("javadoc")
    public static class Descriptor extends VisualRefactoringDescriptor {
        public Descriptor(String project, String description, String comment,
                Map<String, String> arguments) {
            super("org.eclipse.andmore.refactoring.usecompound", //$NON-NLS-1$
                    project, description, comment, arguments);
        }

        @Override
        protected Refactoring createRefactoring(Map<String, String> args) {
            return new UseCompoundDrawableRefactoring(args);
        }
    }
}
