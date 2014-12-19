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
package org.eclipse.andmore.internal.wizards.templates;

import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_MIN_API;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_MIN_BUILD_API;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_REVISION;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_BACKGROUND;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_CLIPART_NAME;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_DESCRIPTION;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_FOREGROUND;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_FORMAT;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_NAME;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_PADDING;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_SHAPE;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_SOURCE_TYPE;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_TEXT;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_TRIM;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_TYPE;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.ATTR_VALUE;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.CURRENT_FORMAT;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.TAG_CATEGORY;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.TAG_DEPENDENCY;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.TAG_FORMFACTOR;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.TAG_ICONS;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.TAG_PARAMETER;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.TAG_THUMB;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.assetstudiolib.GraphicGenerator;
import com.android.utils.Pair;
import com.google.common.collect.Lists;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.assetstudio.AssetType;
import org.eclipse.andmore.internal.assetstudio.CreateAssetSetWizardState;
import org.eclipse.andmore.internal.assetstudio.CreateAssetSetWizardState.SourceType;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.layout.gle2.ImageUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** An ADT template along with metadata */
class TemplateMetadata {
    private final Document mDocument;
    private final List<Parameter> mParameters;
    private final Map<String, Parameter> mParameterMap;
    private List<Pair<String, Integer>> mDependencies;
    private Integer mMinApi;
    private Integer mMinBuildApi;
    private Integer mRevision;
    private boolean mNoIcons;
    private CreateAssetSetWizardState mIconState;
	private String mFormFactor;
	private String mCategory;

    TemplateMetadata(@NonNull Document document) {
        mDocument = document;

        NodeList parameters = mDocument.getElementsByTagName(TAG_PARAMETER);
        mParameters = new ArrayList<Parameter>(parameters.getLength());
        mParameterMap = new HashMap<String, Parameter>(parameters.getLength());
        for (int index = 0, max = parameters.getLength(); index < max; index++) {
            Element element = (Element) parameters.item(index);
            Parameter parameter = new Parameter(this, element);
            mParameters.add(parameter);
            if (parameter.id != null) {
                mParameterMap.put(parameter.id, parameter);
            }
        }
    }

    boolean isSupported() {
        String versionString = mDocument.getDocumentElement().getAttribute(ATTR_FORMAT);
        if (versionString != null && !versionString.isEmpty()) {
            try {
                int version = Integer.parseInt(versionString);
                return version <= CURRENT_FORMAT;
            } catch (NumberFormatException nufe) {
                return false;
            }
        }

        // Older templates without version specified: supported
        return true;
    }

    @Nullable
    String getTitle() {
        String name = mDocument.getDocumentElement().getAttribute(ATTR_NAME);
        if (name != null && !name.isEmpty()) {
            return name;
        }

        return null;
    }

    @Nullable
    String getDescription() {
        String description = mDocument.getDocumentElement().getAttribute(ATTR_DESCRIPTION);
        if (description != null && !description.isEmpty()) {
            return description;
        }

        return null;
    }

    int getMinSdk() {
        if (mMinApi == null) {
            mMinApi = 1;
            String api = mDocument.getDocumentElement().getAttribute(ATTR_MIN_API);
            if (api != null && !api.isEmpty()) {
                try {
                    mMinApi = Integer.parseInt(api);
                } catch (NumberFormatException nufe) {
                    // Templates aren't allowed to contain codenames, should always be an integer
                    AdtPlugin.log(nufe, null);
                    mMinApi = 1;
                }
            }
        }

        return mMinApi.intValue();
    }

    int getMinBuildApi() {
        if (mMinBuildApi == null) {
            mMinBuildApi = 1;
            String api = mDocument.getDocumentElement().getAttribute(ATTR_MIN_BUILD_API);
            if (api != null && !api.isEmpty()) {
                try {
                    mMinBuildApi = Integer.parseInt(api);
                } catch (NumberFormatException nufe) {
                    // Templates aren't allowed to contain codenames, should always be an integer
                    AdtPlugin.log(nufe, null);
                    mMinBuildApi = 1;
                }
            }
        }

        return mMinBuildApi.intValue();
    }

    public int getRevision() {
        if (mRevision == null) {
            mRevision = 1;
            String revision = mDocument.getDocumentElement().getAttribute(ATTR_REVISION);
            if (revision != null && !revision.isEmpty()) {
                try {
                    mRevision = Integer.parseInt(revision);
                } catch (NumberFormatException nufe) {
                    AdtPlugin.log(nufe, null);
                    mRevision = 1;
                }
            }
        }

        return mRevision.intValue();
    }
    
    public String getFormFactor() {
        if (mFormFactor == null) {
            mFormFactor = "Mobile";
            
            NodeList formfactorDeclarations = mDocument.getElementsByTagName(TAG_FORMFACTOR);
            if (formfactorDeclarations.getLength() > 0) {
            	Element element = (Element) formfactorDeclarations.item(0);
                String formFactor = element.getAttribute(ATTR_VALUE);
                if (formFactor != null && !formFactor.isEmpty()) {
                	mFormFactor = formFactor;
                }
            }
        }
        return mFormFactor;
    }
    
    public String getCategory() {
        if (mCategory == null) {
        	mCategory = "";
        	NodeList categories = mDocument.getElementsByTagName(TAG_CATEGORY);
            if (categories.getLength() > 0) {
            	Element element = (Element) categories.item(0);
            	String category = element.getAttribute(ATTR_VALUE);
                if (category != null && !category.isEmpty()) {
                	mCategory = category;
                }
            }
        }
        return mCategory;
    }

    /**
     * Returns a suitable icon wizard state instance if this wizard requests
     * icons to be created, and null otherwise
     *
     * @return icon wizard state or null
     */
    @Nullable
    public CreateAssetSetWizardState getIconState(IProject project) {
        if (mIconState == null && !mNoIcons) {
            NodeList icons = mDocument.getElementsByTagName(TAG_ICONS);
            if (icons.getLength() < 1) {
                mNoIcons = true;
                return null;
            }
            Element icon = (Element) icons.item(0);

            mIconState = new CreateAssetSetWizardState();
            mIconState.project = project;

            String typeString = getAttributeOrNull(icon, ATTR_TYPE);
            if (typeString != null) {
                typeString = typeString.toUpperCase(Locale.US);
                boolean found = false;
                for (AssetType type : AssetType.values()) {
                    if (typeString.equals(type.name())) {
                        mIconState.type = type;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    AdtPlugin.log(null, "Unknown asset type %1$s", typeString);
                }
            }

            mIconState.outputName = getAttributeOrNull(icon, ATTR_NAME);
            if (mIconState.outputName != null) {
                // Register parameter such that if it is referencing other values, it gets
                // updated when other values are edited
                Parameter outputParameter = new Parameter(this,
                        Parameter.Type.STRING, "_iconname", mIconState.outputName); //$NON-NLS-1$
                getParameters().add(outputParameter);
            }

            RGB background = getRgb(icon, ATTR_BACKGROUND);
            if (background != null) {
                mIconState.background = background;
            }
            RGB foreground = getRgb(icon, ATTR_FOREGROUND);
            if (foreground != null) {
                mIconState.foreground = foreground;
            }
            String shapeString = getAttributeOrNull(icon, ATTR_SHAPE);
            if (shapeString != null) {
                shapeString = shapeString.toUpperCase(Locale.US);
                boolean found = false;
                for (GraphicGenerator.Shape shape : GraphicGenerator.Shape.values()) {
                    if (shapeString.equals(shape.name())) {
                        mIconState.shape = shape;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    AdtPlugin.log(null, "Unknown shape %1$s", shapeString);
                }
            }
            String trimString = getAttributeOrNull(icon, ATTR_TRIM);
            if (trimString != null) {
                mIconState.trim = Boolean.valueOf(trimString);
            }
            String paddingString = getAttributeOrNull(icon, ATTR_PADDING);
            if (paddingString != null) {
                mIconState.padding = Integer.parseInt(paddingString);
            }
            String sourceTypeString = getAttributeOrNull(icon, ATTR_SOURCE_TYPE);
            if (sourceTypeString != null) {
                sourceTypeString = sourceTypeString.toUpperCase(Locale.US);
                boolean found = false;
                for (SourceType type : SourceType.values()) {
                    if (sourceTypeString.equals(type.name())) {
                        mIconState.sourceType = type;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    AdtPlugin.log(null, "Unknown source type %1$s", sourceTypeString);
                }
            }
            mIconState.clipartName = getAttributeOrNull(icon, ATTR_CLIPART_NAME);

            String textString = getAttributeOrNull(icon, ATTR_TEXT);
            if (textString != null) {
                mIconState.text = textString;
            }
        }

        return mIconState;
    }

    void updateIconName(List<Parameter> parameters, StringEvaluator evaluator) {
        if (mIconState != null) {
            NodeList icons = mDocument.getElementsByTagName(TAG_ICONS);
            if (icons.getLength() < 1) {
                return;
            }
            Element icon = (Element) icons.item(0);
            String name = getAttributeOrNull(icon, ATTR_NAME);
            if (name != null) {
                mIconState.outputName = evaluator.evaluate(name, parameters);
            }
        }
    }

    private static RGB getRgb(@NonNull Element element, @NonNull String name) {
        String colorString = getAttributeOrNull(element, name);
        if (colorString != null) {
            int rgb = ImageUtils.getColor(colorString.trim());
            return ImageUtils.intToRgb(rgb);
        }

        return null;
    }

    @Nullable
    private static String getAttributeOrNull(@NonNull Element element, @NonNull String name) {
        String value = element.getAttribute(name);
        if (value != null && value.isEmpty()) {
            return null;
        }
        return value;
    }

    @Nullable
    String getThumbnailPath() {
        // Apply selector logic. Pick the thumb first thumb that satisfies the largest number
        // of conditions.
        NodeList thumbs = mDocument.getElementsByTagName(TAG_THUMB);
        if (thumbs.getLength() == 0) {
            return null;
        }


        int bestMatchCount = 0;
        Element bestMatch = null;

        for (int i = 0, n = thumbs.getLength(); i < n; i++) {
            Element thumb = (Element) thumbs.item(i);

            NamedNodeMap attributes = thumb.getAttributes();
            if (bestMatch == null && attributes.getLength() == 0) {
                bestMatch = thumb;
            } else if (attributes.getLength() <= bestMatchCount) {
                // Already have a match with this number of attributes, no point checking
                continue;
            } else {
                boolean match = true;
                for (int j = 0, max = attributes.getLength(); j < max; j++) {
                    Attr attribute = (Attr) attributes.item(j);
                    Parameter parameter = mParameterMap.get(attribute.getName());
                    if (parameter == null) {
                        AdtPlugin.log(null, "Unexpected parameter in template thumbnail: %1$s",
                                attribute.getName());
                        continue;
                    }
                    String thumbNailValue = attribute.getValue();
                    String editedValue = parameter.value != null ? parameter.value.toString() : "";
                    if (!thumbNailValue.equals(editedValue)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    bestMatch = thumb;
                    bestMatchCount = attributes.getLength();
                }
            }
        }

        if (bestMatch != null) {
            NodeList children = bestMatch.getChildNodes();
            for (int i = 0, n = children.getLength(); i < n; i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    return child.getNodeValue().trim();
                }
            }
        }

        return null;
    }

    /**
     * Returns the dependencies (as a list of pairs of names and revisions)
     * required by this template
     */
    List<Pair<String, Integer>> getDependencies() {
        if (mDependencies == null) {
            NodeList elements = mDocument.getElementsByTagName(TAG_DEPENDENCY);
            if (elements.getLength() == 0) {
                return Collections.emptyList();
            }

            List<Pair<String, Integer>> dependencies = Lists.newArrayList();
            for (int i = 0, n = elements.getLength(); i < n; i++) {
                Element element = (Element) elements.item(i);
                String name = element.getAttribute(ATTR_NAME);
                int revision = -1;
                String revisionString = element.getAttribute(ATTR_REVISION);
                if (!revisionString.isEmpty()) {
                    revision = Integer.parseInt(revisionString);
                }
                dependencies.add(Pair.of(name, revision));
            }
            mDependencies = dependencies;
        }

        return mDependencies;
    }

    /** Returns the list of available parameters */
    @NonNull
    List<Parameter> getParameters() {
        return mParameters;
    }

    /**
     * Returns the parameter of the given id, or null if not found
     *
     * @param id the id of the target parameter
     * @return the corresponding parameter, or null if not found
     */
    @Nullable
    public Parameter getParameter(@NonNull String id) {
        for (Parameter parameter : mParameters) {
            if (id.equals(parameter.id)) {
                return parameter;
            }
        }

        return null;
    }

    /** Returns a default icon for templates */
    static Image getDefaultTemplateIcon() {
        return IconFactory.getInstance().getIcon("default_template"); //$NON-NLS-1$
    }
}
