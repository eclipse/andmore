/*
 * Copyright (C) 2009 The Android Open Source Project
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

package org.eclipse.andmore.internal.refactorings.extractstring;

import com.android.SdkConstants;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * An helper utility to get IDs out of an Android XML resource file.
 */
@SuppressWarnings("restriction")
class XmlStringFileHelper {

    /** A temporary cache of R.string IDs defined by a given xml file. The key is the
     * project path of the file, the data is a set of known string Ids for that file.
     *
     * Map type: map [String filename] => map [String id => String value].
     */
    private HashMap<String, Map<String, String>> mResIdCache =
        new HashMap<String, Map<String, String>>();

    public XmlStringFileHelper() {
    }

    /**
     * Utility method used by the wizard to retrieve the actual value definition of a given
     * string ID.
     *
     * @param project The project contain the XML file.
     * @param xmlFileWsPath The project path of the XML file, e.g. "/res/values/strings.xml".
     *          The given file may or may not exist.
     * @param stringId The string ID to find.
     * @return The value string if the ID is defined, null otherwise.
     */
    public String valueOfStringId(IProject project, String xmlFileWsPath, String stringId) {
        Map<String, String> cache = getResIdsForFile(project, xmlFileWsPath);
        return cache.get(stringId);
    }

    /**
     * Utility method that retrieves all the *string* IDs defined in the given Android resource
     * file. The instance maintains an internal cache so a given file is retrieved only once.
     * Callers should consider the set to be read-only.
     *
     * @param project The project contain the XML file.
     * @param xmlFileWsPath The project path of the XML file, e.g. "/res/values/strings.xml".
     *          The given file may or may not exist.
     * @return The map of string IDs => values defined in the given file. Cached. Never null.
     */
    public Map<String, String> getResIdsForFile(IProject project, String xmlFileWsPath) {
        Map<String, String> cache = mResIdCache.get(xmlFileWsPath);
        if (cache == null) {
            cache = internalGetResIdsForFile(project, xmlFileWsPath);
            mResIdCache.put(xmlFileWsPath, cache);
        }
        return cache;
    }

    /**
     * Extract all the defined string IDs from a given file using XPath.
     * @param project The project contain the XML file.
     * @param xmlFileWsPath The project path of the file to parse. It may not exist.
     * @return The map of all string IDs => values defined in the file.
     *   The returned set is always non null. It is empty if the file does not exist.
     */
    private Map<String, String> internalGetResIdsForFile(IProject project, String xmlFileWsPath) {

        TreeMap<String, String> ids = new TreeMap<String, String>();

        // Access the project that contains the resource that contains the compilation unit
        IResource resource = project.getFile(xmlFileWsPath);

        if (resource != null && resource.exists() && resource.getType() == IResource.FILE) {
            IStructuredModel smodel = null;

            try {
                IFile file = (IFile) resource;
                IModelManager modelMan = StructuredModelManager.getModelManager();
                smodel = modelMan.getExistingModelForRead(file);
                if (smodel == null) {
                    smodel = modelMan.getModelForRead(file);
                }

                if (smodel instanceof IDOMModel) {
                    IDOMDocument doc = ((IDOMModel) smodel).getDocument();

                    // We want all the IDs in an XML structure like this:
                    // <resources>
                    //    <string name="ID">something</string>
                    // </resources>

                    Node root = findChild(doc, null, SdkConstants.TAG_RESOURCES);
                    if (root != null) {
                        for (Node strNode = findChild(root, null,
                                                      SdkConstants.TAG_STRING);
                             strNode != null;
                             strNode = findChild(null, strNode,
                                                 SdkConstants.TAG_STRING)) {
                            NamedNodeMap attrs = strNode.getAttributes();
                            Node nameAttr = attrs.getNamedItem(SdkConstants.ATTR_NAME);
                            if (nameAttr != null) {
                                String id = nameAttr.getNodeValue();

                                // Find the TEXT node right after the element.
                                // Whitespace matters so we don't try to normalize it.
                                String text = "";                       //$NON-NLS-1$
                                for (Node txtNode = strNode.getFirstChild();
                                        txtNode != null && txtNode.getNodeType() == Node.TEXT_NODE;
                                        txtNode = txtNode.getNextSibling()) {
                                    text += txtNode.getNodeValue();
                                }

                                ids.put(id, text);
                            }
                        }
                    }
                }

            } catch (Throwable e) {
                AdtPlugin.log(e, "GetResIds failed in %1$s", xmlFileWsPath); //$NON-NLS-1$
            } finally {
                if (smodel != null) {
                    smodel.releaseFromRead();
                }
            }
        }

        return ids;
    }

    /**
     * Utility method that finds the next node of the requested element name.
     *
     * @param parent The parent node. If not null, will to start searching its children.
     *               Set to null when iterating through children.
     * @param lastChild The last child returned. Use null when visiting a parent the first time.
     * @param elementName The element name of the node to find.
     * @return The next children or sibling nide with the requested element name or null.
     */
    private Node findChild(Node parent, Node lastChild, String elementName) {
        if (lastChild == null && parent != null) {
            lastChild = parent.getFirstChild();
        } else if (lastChild != null) {
            lastChild = lastChild.getNextSibling();
        }

        for ( ; lastChild != null ; lastChild = lastChild.getNextSibling()) {
            if (lastChild.getNodeType() == Node.ELEMENT_NODE &&
                    lastChild.getNamespaceURI() == null &&  // resources don't have any NS URI
                    elementName.equals(lastChild.getLocalName())) {
                return lastChild;
            }
        }

        return null;
    }

}
