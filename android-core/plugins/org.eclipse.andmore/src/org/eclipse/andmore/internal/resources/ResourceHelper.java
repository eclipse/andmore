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

package org.eclipse.andmore.internal.resources;

import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_COLOR;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_TYPE;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.EXT_XML;
import static com.android.SdkConstants.FD_RESOURCES;
import static com.android.SdkConstants.FD_RES_VALUES;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_RESOURCES;
import static org.eclipse.andmore.AdtConstants.WS_SEP;

import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.ResourceDeltaKind;
import com.android.ide.common.resources.ResourceResolver;
import com.android.ide.common.resources.ResourceUrl;
import com.android.ide.common.resources.configuration.CountryCodeQualifier;
import com.android.ide.common.resources.configuration.DensityQualifier;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.KeyboardStateQualifier;
import com.android.ide.common.resources.configuration.LanguageQualifier;
import com.android.ide.common.resources.configuration.LayoutDirectionQualifier;
import com.android.ide.common.resources.configuration.NavigationMethodQualifier;
import com.android.ide.common.resources.configuration.NavigationStateQualifier;
import com.android.ide.common.resources.configuration.NetworkCodeQualifier;
import com.android.ide.common.resources.configuration.NightModeQualifier;
import com.android.ide.common.resources.configuration.RegionQualifier;
import com.android.ide.common.resources.configuration.ResourceQualifier;
import com.android.ide.common.resources.configuration.ScreenDimensionQualifier;
import com.android.ide.common.resources.configuration.ScreenHeightQualifier;
import com.android.ide.common.resources.configuration.ScreenOrientationQualifier;
import com.android.ide.common.resources.configuration.ScreenRatioQualifier;
import com.android.ide.common.resources.configuration.ScreenSizeQualifier;
import com.android.ide.common.resources.configuration.ScreenWidthQualifier;
import com.android.ide.common.resources.configuration.SmallestScreenWidthQualifier;
import com.android.ide.common.resources.configuration.TextInputMethodQualifier;
import com.android.ide.common.resources.configuration.TouchScreenQualifier;
import com.android.ide.common.resources.configuration.UiModeQualifier;
import com.android.ide.common.resources.configuration.VersionQualifier;
import com.android.resources.FolderTypeRelationship;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.Hyperlinks;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.layout.gle2.ImageUtils;
import org.eclipse.andmore.internal.editors.layout.refactoring.VisualRefactoring;
import org.eclipse.andmore.internal.wizards.newxmlfile.NewXmlFileWizard;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.document.ElementImpl;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Helper class to deal with SWT specifics for the resources.
 */
@SuppressWarnings("restriction") // XML model
public class ResourceHelper {

    private final static Map<Class<?>, Image> sIconMap = new HashMap<Class<?>, Image>(
            FolderConfiguration.getQualifierCount());

    static {
        try {
            IconFactory factory = IconFactory.getInstance();
            sIconMap.put(CountryCodeQualifier.class,        factory.getIcon("mcc")); //$NON-NLS-1$
            sIconMap.put(NetworkCodeQualifier.class,        factory.getIcon("mnc")); //$NON-NLS-1$
            sIconMap.put(LanguageQualifier.class,           factory.getIcon("language")); //$NON-NLS-1$
            sIconMap.put(RegionQualifier.class,             factory.getIcon("region")); //$NON-NLS-1$
            sIconMap.put(LayoutDirectionQualifier.class,    factory.getIcon("bidi")); //$NON-NLS-1$
            sIconMap.put(ScreenSizeQualifier.class,         factory.getIcon("size")); //$NON-NLS-1$
            sIconMap.put(ScreenRatioQualifier.class,        factory.getIcon("ratio")); //$NON-NLS-1$
            sIconMap.put(ScreenOrientationQualifier.class,  factory.getIcon("orientation")); //$NON-NLS-1$
            sIconMap.put(UiModeQualifier.class,             factory.getIcon("dockmode")); //$NON-NLS-1$
            sIconMap.put(NightModeQualifier.class,          factory.getIcon("nightmode")); //$NON-NLS-1$
            sIconMap.put(DensityQualifier.class,            factory.getIcon("dpi")); //$NON-NLS-1$
            sIconMap.put(TouchScreenQualifier.class,        factory.getIcon("touch")); //$NON-NLS-1$
            sIconMap.put(KeyboardStateQualifier.class,      factory.getIcon("keyboard")); //$NON-NLS-1$
            sIconMap.put(TextInputMethodQualifier.class,    factory.getIcon("text_input")); //$NON-NLS-1$
            sIconMap.put(NavigationStateQualifier.class,    factory.getIcon("navpad")); //$NON-NLS-1$
            sIconMap.put(NavigationMethodQualifier.class,   factory.getIcon("navpad")); //$NON-NLS-1$
            sIconMap.put(ScreenDimensionQualifier.class,    factory.getIcon("dimension")); //$NON-NLS-1$
            sIconMap.put(VersionQualifier.class,            factory.getIcon("version")); //$NON-NLS-1$
            sIconMap.put(ScreenWidthQualifier.class,        factory.getIcon("width")); //$NON-NLS-1$
            sIconMap.put(ScreenHeightQualifier.class,       factory.getIcon("height")); //$NON-NLS-1$
            sIconMap.put(SmallestScreenWidthQualifier.class,factory.getIcon("swidth")); //$NON-NLS-1$
        } catch (Throwable t) {
            AdtPlugin.log(t , null);
        }
    }

    /**
     * Returns the icon for the qualifier.
     */
    public static Image getIcon(Class<? extends ResourceQualifier> theClass) {
        return sIconMap.get(theClass);
    }

    /**
     * Returns a {@link ResourceDeltaKind} from an {@link IResourceDelta} value.
     * @param kind a {@link IResourceDelta} integer constant.
     * @return a matching {@link ResourceDeltaKind} or null.
     *
     * @see IResourceDelta#ADDED
     * @see IResourceDelta#REMOVED
     * @see IResourceDelta#CHANGED
     */
    public static ResourceDeltaKind getResourceDeltaKind(int kind) {
        switch (kind) {
            case IResourceDelta.ADDED:
                return ResourceDeltaKind.ADDED;
            case IResourceDelta.REMOVED:
                return ResourceDeltaKind.REMOVED;
            case IResourceDelta.CHANGED:
                return ResourceDeltaKind.CHANGED;
        }

        return null;
    }

    /**
     * Is this a resource that can be defined in any file within the "values" folder?
     * <p>
     * Some resource types can be defined <b>both</b> as a separate XML file as well
     * as defined within a value XML file. This method will return true for these types
     * as well. In other words, a ResourceType can return true for both
     * {@link #isValueBasedResourceType} and {@link #isFileBasedResourceType}.
     *
     * @param type the resource type to check
     * @return true if the given resource type can be represented as a value under the
     *         values/ folder
     */
    public static boolean isValueBasedResourceType(ResourceType type) {
        List<ResourceFolderType> folderTypes = FolderTypeRelationship.getRelatedFolders(type);
        for (ResourceFolderType folderType : folderTypes) {
            if (folderType == ResourceFolderType.VALUES) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is this a resource that is defined in a file named by the resource plus the XML
     * extension?
     * <p>
     * Some resource types can be defined <b>both</b> as a separate XML file as well as
     * defined within a value XML file along with other properties. This method will
     * return true for these resource types as well. In other words, a ResourceType can
     * return true for both {@link #isValueBasedResourceType} and
     * {@link #isFileBasedResourceType}.
     *
     * @param type the resource type to check
     * @return true if the given resource type is stored in a file named by the resource
     */
    public static boolean isFileBasedResourceType(ResourceType type) {
        List<ResourceFolderType> folderTypes = FolderTypeRelationship.getRelatedFolders(type);
        for (ResourceFolderType folderType : folderTypes) {
            if (folderType != ResourceFolderType.VALUES) {

                if (type == ResourceType.ID) {
                    // The folder types for ID is not only VALUES but also
                    // LAYOUT and MENU. However, unlike resources, they are only defined
                    // inline there so for the purposes of isFileBasedResourceType
                    // (where the intent is to figure out files that are uniquely identified
                    // by a resource's name) this method should return false anyway.
                    return false;
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if this class can create the given resource
     *
     * @param resource the resource to be created
     * @return true if the {@link #createResource} method can create this resource
     */
    public static boolean canCreateResource(String resource) {
        // Cannot create framework resources
        if (resource.startsWith(ANDROID_PREFIX)) {
            return false;
        }

        ResourceUrl parsed = ResourceUrl.parse(resource);
        if (parsed != null) {
            if (parsed.framework) {
                return false;
            }
            ResourceType type = parsed.type;
            String name = parsed.name;

            // Make sure the name is valid
            ResourceNameValidator validator =
                ResourceNameValidator.create(false, (Set<String>) null /* existing */, type);
            if (validator.isValid(name) != null) {
                return false;
            }

            return canCreateResourceType(type);
        }

        return false;
    }

    /**
     * Returns true if this class can create resources of the given resource
     * type
     *
     * @param type the type of resource to be created
     * @return true if the {@link #createResource} method can create resources
     *         of this type (provided the name parameter is also valid)
     */
    public static boolean canCreateResourceType(ResourceType type) {
        // We can create all value types
        if (isValueBasedResourceType(type)) {
            return true;
        }

        // We can create -some- file-based types - those supported by the New XML wizard:
        for (ResourceFolderType folderType : FolderTypeRelationship.getRelatedFolders(type)) {
            if (NewXmlFileWizard.canCreateXmlFile(folderType)) {
                return true;
            }
        }

        return false;
    }

    /** Creates a file-based resource, like a layout. Used by {@link #createResource} */
    private static Pair<IFile,IRegion> createFileResource(IProject project, ResourceType type,
            String name) {

        ResourceFolderType folderType = null;
        for (ResourceFolderType f : FolderTypeRelationship.getRelatedFolders(type)) {
            if (NewXmlFileWizard.canCreateXmlFile(f)) {
                folderType = f;
                break;
            }
        }
        if (folderType == null) {
            return null;
        }

        // Find "dimens.xml" file in res/values/ (or corresponding name for other
        // value types)
        IPath projectPath = new Path(FD_RESOURCES + WS_SEP + folderType.getName() + WS_SEP
            + name + '.' + EXT_XML);
        IFile file = project.getFile(projectPath);
        return NewXmlFileWizard.createXmlFile(project, file, folderType);
    }

    /**
     * Creates a resource of a given type, name and (if applicable) value
     *
     * @param project the project to contain the resource
     * @param type the type of resource
     * @param name the name of the resource
     * @param value the value of the resource, if it is a value-type resource
     * @return a pair of the file containing the resource and a region where the value
     *         appears
     */
    public static Pair<IFile,IRegion> createResource(IProject project, ResourceType type,
            String name, String value) {
        if (!isValueBasedResourceType(type)) {
            return createFileResource(project, type, name);
        }

        // Find "dimens.xml" file in res/values/ (or corresponding name for other
        // value types)
        String typeName = type.getName();
        String fileName = typeName + 's';
        String projectPath = FD_RESOURCES + WS_SEP + FD_RES_VALUES + WS_SEP
            + fileName + '.' + EXT_XML;
        Object editRequester = project;
        IResource member = project.findMember(projectPath);
        String tagName = Hyperlinks.getTagName(type);
        boolean createEmptyTag = type == ResourceType.ID;
        if (member != null) {
            if (member instanceof IFile) {
                IFile file = (IFile) member;
                // File exists: Must add item to the XML
                IModelManager manager = StructuredModelManager.getModelManager();
                IStructuredModel model = null;
                try {
                    model = manager.getExistingModelForEdit(file);
                    if (model == null) {
                        model = manager.getModelForEdit(file);
                    }
                    if (model instanceof IDOMModel) {
                        model.beginRecording(editRequester, String.format("Add %1$s",
                                type.getDisplayName()));
                        IDOMModel domModel = (IDOMModel) model;
                        Document document = domModel.getDocument();
                        Element root = document.getDocumentElement();
                        IStructuredDocument structuredDocument = model.getStructuredDocument();
                        Node lastElement = null;
                        NodeList childNodes = root.getChildNodes();
                        String indent = null;
                        for (int i = childNodes.getLength() - 1; i >= 0; i--) {
                            Node node = childNodes.item(i);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                lastElement = node;
                                indent = AndroidXmlEditor.getIndent(structuredDocument, node);
                                break;
                            }
                        }
                        if (indent == null || indent.length() == 0) {
                            indent = "    "; //$NON-NLS-1$
                        }
                        Node nextChild = lastElement != null ? lastElement.getNextSibling() : null;
                        Text indentNode = document.createTextNode('\n' + indent);
                        root.insertBefore(indentNode, nextChild);
                        Element element = document.createElement(tagName);
                        if (createEmptyTag) {
                            if (element instanceof ElementImpl) {
                                ElementImpl elementImpl = (ElementImpl) element;
                                elementImpl.setEmptyTag(true);
                            }
                        }
                        element.setAttribute(ATTR_NAME, name);
                        if (!tagName.equals(typeName)) {
                            element.setAttribute(ATTR_TYPE, typeName);
                        }
                        root.insertBefore(element, nextChild);
                        IRegion region = null;

                        if (createEmptyTag) {
                            IndexedRegion domRegion = VisualRefactoring.getRegion(element);
                            int endOffset = domRegion.getEndOffset();
                            region = new Region(endOffset, 0);
                        } else {
                            Node valueNode = document.createTextNode(value);
                            element.appendChild(valueNode);

                            IndexedRegion domRegion = VisualRefactoring.getRegion(valueNode);
                            int startOffset = domRegion.getStartOffset();
                            int length = domRegion.getLength();
                            region = new Region(startOffset, length);
                        }
                        model.save();
                        return Pair.of(file, region);
                    }
                } catch (Exception e) {
                    AdtPlugin.log(e, "Cannot access XML value model");
                } finally {
                    if (model != null) {
                        model.endRecording(editRequester);
                        model.releaseFromEdit();
                    }
                }
            }

            return null;
        } else {
            // No such file exists: just create it
            String prolog = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"; //$NON-NLS-1$
            StringBuilder sb = new StringBuilder(prolog);

            String root = TAG_RESOURCES;
            sb.append('<').append(root).append('>').append('\n');
            sb.append("    "); //$NON-NLS-1$
            sb.append('<');
            sb.append(tagName);
            sb.append(" name=\""); //$NON-NLS-1$
            sb.append(name);
            sb.append('"');
            if (!tagName.equals(typeName)) {
                sb.append(" type=\""); //$NON-NLS-1$
                sb.append(typeName);
                sb.append('"');
            }
            int start, end;
            if (createEmptyTag) {
                sb.append("/>");                             //$NON-NLS-1$
                start = sb.length();
                end = sb.length();
            } else {
                sb.append('>');
                start = sb.length();
                sb.append(value);
                end = sb.length();
                sb.append('<').append('/');
                sb.append(tagName);
                sb.append('>');
            }
            sb.append('\n').append('<').append('/').append(root).append('>').append('\n');
            String result = sb.toString();
            // TODO: Pretty print string (wait until that CL is integrated)
            String error = null;
            try {
                byte[] buf = result.getBytes("UTF8");    //$NON-NLS-1$
                InputStream stream = new ByteArrayInputStream(buf);
                IFile file = project.getFile(new Path(projectPath));
                file.create(stream, true /*force*/, null /*progress*/);
                IRegion region = new Region(start, end - start);
                return Pair.of(file, region);
            } catch (UnsupportedEncodingException e) {
                error = e.getMessage();
            } catch (CoreException e) {
                error = e.getMessage();
            }

            error = String.format("Failed to generate %1$s: %2$s", name, error);
            AdtPlugin.displayError("New Android XML File", error);
        }
        return null;
    }

    /**
     * Returns the theme name to be shown for theme styles, e.g. for "@style/Theme" it
     * returns "Theme"
     *
     * @param style a theme style string
     * @return the user visible theme name
     */
    public static String styleToTheme(String style) {
        if (style.startsWith(STYLE_RESOURCE_PREFIX)) {
            style = style.substring(STYLE_RESOURCE_PREFIX.length());
        } else if (style.startsWith(ANDROID_STYLE_RESOURCE_PREFIX)) {
            style = style.substring(ANDROID_STYLE_RESOURCE_PREFIX.length());
        } else if (style.startsWith(PREFIX_RESOURCE_REF)) {
            // @package:style/foo
            int index = style.indexOf('/');
            if (index != -1) {
                style = style.substring(index + 1);
            }
        }
        return style;
    }

    /**
     * Returns true if the given style represents a project theme
     *
     * @param style a theme style string
     * @return true if the style string represents a project theme, as opposed
     *         to a framework theme
     */
    public static boolean isProjectStyle(String style) {
        assert style.startsWith(STYLE_RESOURCE_PREFIX)
            || style.startsWith(ANDROID_STYLE_RESOURCE_PREFIX) : style;

        return style.startsWith(STYLE_RESOURCE_PREFIX);
    }

    /**
     * Returns the layout resource name for the given layout file, e.g. for
     * /res/layout/foo.xml returns foo.
     *
     * @param layoutFile the layout file whose name we want to look up
     * @return the layout name
     */
    public static String getLayoutName(IFile layoutFile) {
        String layoutName = layoutFile.getName();
        int dotIndex = layoutName.indexOf('.');
        if (dotIndex != -1) {
            layoutName = layoutName.substring(0, dotIndex);
        }
        return layoutName;
    }

    /**
     * Tries to resolve the given resource value to an actual RGB color. For state lists
     * it will pick the simplest/fallback color.
     *
     * @param resources the resource resolver to use to follow color references
     * @param color the color to resolve
     * @return the corresponding {@link RGB} color, or null
     */
    public static RGB resolveColor(ResourceResolver resources, ResourceValue color) {
        color = resources.resolveResValue(color);
        if (color == null) {
            return null;
        }
        String value = color.getValue();

        while (value != null) {
            if (value.startsWith("#")) { //$NON-NLS-1$
                try {
                    int rgba = ImageUtils.getColor(value);
                    // Drop alpha channel
                    return ImageUtils.intToRgb(rgba);
                } catch (NumberFormatException nfe) {
                    // Pass
                }
                return null;
            }
            if (value.startsWith(PREFIX_RESOURCE_REF)) {
                boolean isFramework = color.isFramework();
                color = resources.findResValue(value, isFramework);
                if (color != null) {
                    value = color.getValue();
                } else {
                    break;
                }
            } else {
                File file = new File(value);
                if (file.exists() && file.getName().endsWith(DOT_XML)) {
                    // Parse
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    BufferedInputStream bis = null;
                    try {
                        bis = new BufferedInputStream(new FileInputStream(file));
                        InputSource is = new InputSource(bis);
                        factory.setNamespaceAware(true);
                        factory.setValidating(false);
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document document = builder.parse(is);
                        NodeList items = document.getElementsByTagName(TAG_ITEM);

                        value = findColorValue(items);
                        continue;
                    } catch (Exception e) {
                        AdtPlugin.log(e, "Failed parsing color file %1$s", file.getName());
                    } finally {
                        if (bis != null) {
                            try {
                                bis.close();
                            } catch (IOException e) {
                                // Nothing useful can be done here
                            }
                        }
                    }
                }

                return null;
            }
        }

        return null;
    }

    /**
     * Searches a color XML file for the color definition element that does not
     * have an associated state and returns its color
     */
    private static String findColorValue(NodeList items) {
        for (int i = 0, n = items.getLength(); i < n; i++) {
            // Find non-state color definition
            Node item = items.item(i);
            boolean hasState = false;
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) item;
                if (element.hasAttributeNS(ANDROID_URI, ATTR_COLOR)) {
                    NamedNodeMap attributes = element.getAttributes();
                    for (int j = 0, m = attributes.getLength(); j < m; j++) {
                        Attr attribute = (Attr) attributes.item(j);
                        if (attribute.getLocalName().startsWith("state_")) { //$NON-NLS-1$
                            hasState = true;
                            break;
                        }
                    }

                    if (!hasState) {
                        return element.getAttributeNS(ANDROID_URI, ATTR_COLOR);
                    }
                }
            }
        }

        return null;
    }
}
