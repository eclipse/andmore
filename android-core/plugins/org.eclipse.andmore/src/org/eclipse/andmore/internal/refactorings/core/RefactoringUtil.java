/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.internal.refactorings.core;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.xml.AndroidManifest.ATTRIBUTE_BACKUP_AGENT;
import static com.android.xml.AndroidManifest.ATTRIBUTE_MANAGE_SPACE_ACTIVITY;
import static com.android.xml.AndroidManifest.ATTRIBUTE_PARENT_ACTIVITY_NAME;
import static com.android.xml.AndroidManifest.ATTRIBUTE_TARGET_ACTIVITY;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.xml.AndroidManifest;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * The utility class for android refactoring
 *
 */
@SuppressWarnings("restriction")
public class RefactoringUtil {

    private static boolean sRefactorAppPackage = false;

    /**
     * Releases SSE read model; saves SSE model if exists edit model
     * Called in dispose method of refactoring change classes
     *
     * @param model the SSE model
     * @param document the document
     */
    public static void fixModel(IStructuredModel model, IDocument document) {
        if (model != null) {
            model.releaseFromRead();
        }
        model = null;
        if (document == null) {
            return;
        }
        try {
            model = StructuredModelManager.getModelManager().getExistingModelForEdit(document);
            if (model != null) {
                model.save();
            }
        } catch (UnsupportedEncodingException e1) {
            // ignore
        } catch (IOException e1) {
            // ignore
        } catch (CoreException e1) {
            // ignore
        } finally {
            if (model != null) {
                model.releaseFromEdit();
            }
        }
    }

    /**
     * Logs the info message
     *
     * @param message the message
     */
    public static void logInfo(String message) {
        AdtPlugin.log(IStatus.INFO, AdtPlugin.PLUGIN_ID, message);
    }

    /**
     * Logs the the exception
     *
     * @param e the exception
     */
    public static void log(Throwable e) {
        AdtPlugin.log(e, e.getMessage());
    }

    /**
     * @return true if Rename/Move package needs to change the application package
     * default is false
     *
     */
    public static boolean isRefactorAppPackage() {
        return sRefactorAppPackage;
    }

    /**
     * @param refactorAppPackage true if Rename/Move package needs to change the application package
     */
    public static void setRefactorAppPackage(boolean refactorAppPackage) {
        RefactoringUtil.sRefactorAppPackage = refactorAppPackage;
    }

    /**
     * Returns the range of the attribute value in the given document
     *
     * @param attr the attribute to look up
     * @param document the document containing the attribute
     * @return the range of the value text, not including quotes, in the document
     */
    public static int getAttributeValueRangeStart(
            @NonNull Attr attr,
            @NonNull IDocument document) {
        IndexedRegion region = (IndexedRegion) attr;
        int potentialStart = attr.getName().length() + 2; // + 2: add ="
        String text;
        try {
            text = document.get(region.getStartOffset(),
                    region.getEndOffset() - region.getStartOffset());
        } catch (BadLocationException e) {
            return -1;
        }
        String value = attr.getValue();
        int index = text.indexOf(value, potentialStart);
        if (index != -1) {
            return region.getStartOffset() + index;
        } else {
            return -1;
        }
    }

    /**
     * Returns the start of the tag name of the given element
     *
     * @param element the element to look up
     * @param document the document containing the attribute
     * @return the index of the start tag in the document
     */
    public static int getTagNameRangeStart(
            @NonNull Element element,
            @NonNull IDocument document) {
        IndexedRegion region = (IndexedRegion) element;
        int potentialStart = 1; // add '<'
        String text;
        try {
            text = document.get(region.getStartOffset(),
                    region.getEndOffset() - region.getStartOffset());
        } catch (BadLocationException e) {
            return -1;
        }
        int index = text.indexOf(element.getTagName(), potentialStart);
        if (index != -1) {
            return region.getStartOffset() + index;
        } else {
            return -1;
        }
    }

    /**
     * Returns whether the given manifest attribute should be considered to describe
     * a class name. These will be eligible for refactoring when classes are renamed
     * or moved.
     *
     * @param attribute the manifest attribute
     * @return true if this attribute can describe a class
     */
    public static boolean isManifestClassAttribute(@NonNull Attr attribute) {
        return isManifestClassAttribute(
                attribute.getOwnerElement().getTagName(),
                attribute.getNamespaceURI(),
                attribute.getLocalName());
    }

    /**
     * Returns whether the given manifest attribute should be considered to describe
     * a class name. These will be eligible for refactoring when classes are renamed
     * or moved.
     *
     * @param tag the tag, if known
     * @param uri the attribute namespace, if any
     * @param name the attribute local name, if any
     * @return true if this attribute can describe a class
     */
    public static boolean isManifestClassAttribute(
            @Nullable String tag,
            @Nullable String uri,
            @Nullable String name) {
        if (name == null) {
            return false;
        }

        if ((name.equals(ATTR_NAME)
                && (AndroidManifest.NODE_ACTIVITY.equals(tag)
                        || AndroidManifest.NODE_APPLICATION.equals(tag)
                        || AndroidManifest.NODE_INSTRUMENTATION.equals(tag)
                        || AndroidManifest.NODE_PROVIDER.equals(tag)
                        || AndroidManifest.NODE_SERVICE.equals(tag)
                        || AndroidManifest.NODE_RECEIVER.equals(tag)))
                || name.equals(ATTRIBUTE_TARGET_ACTIVITY)
                || name.equals(ATTRIBUTE_MANAGE_SPACE_ACTIVITY)
                || name.equals(ATTRIBUTE_BACKUP_AGENT)
                || name.equals(ATTRIBUTE_PARENT_ACTIVITY_NAME)) {
            return ANDROID_URI.equals(uri);
        }

        return false;
    }
}
