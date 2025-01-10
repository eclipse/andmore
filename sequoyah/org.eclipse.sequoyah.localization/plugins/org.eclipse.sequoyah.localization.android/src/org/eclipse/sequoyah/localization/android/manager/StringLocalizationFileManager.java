/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Marcel Augusto Gorri (Eldorado) - Bug [323036] - Add support to other localizable resources
 * Matheus Lima (Eldorado) - Bug [326793] - Fixed array support for the String Localization Editor
 * Paulo Faria (Eldorado) - Bug [326793] - Starting new LFE workflow improvements (Refactor visitDomXYZ and NodeManagers)
 ********************************************************************************/
package org.eclipse.sequoyah.localization.android.manager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.FileUtil;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahExceptionStatus;
import org.eclipse.sequoyah.localization.android.AndroidLocalizationPlugin;
import org.eclipse.sequoyah.localization.android.IAndroidLocalizationSchemaConstants;
import org.eclipse.sequoyah.localization.android.LocalizationXMLParserFilter;
import org.eclipse.sequoyah.localization.android.datamodel.AndroidStringLocalizationFile;
import org.eclipse.sequoyah.localization.android.i18n.Messages;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFileBean;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFileFactory;
import org.eclipse.sequoyah.localization.tools.datamodel.StringLocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayItemNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringNode;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSParserFilter;

/**
 * This class deals specifically with localized String / text content.
 * 
 */
public class StringLocalizationFileManager extends ILocalizationFileManager implements
        IAndroidLocalizationSchemaConstants
{

    private IProject localizationFileProject = null;

    /*
     * Static code to add this manager to factory's hashmap
     */
    static
    {
        LocalizationFileManagerFactory.getInstance().addManager(
                StringLocalizationFileManager.class.getName(), StringLocalizationFileManager.class);
    }

    /**
     * Default constructor.
     */
    public StringLocalizationFileManager()
    {
    }

    /**
     * StringLocalizationFileManager knows how to create itself.
     * 
     * @return StringLocalizationFileManager created
     */
    public static ILocalizationFileManager create()
    {
        return new StringLocalizationFileManager();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.sequoyah.localization.android.manager.ILocalizationFileManager
     * #
     * loadFile(org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile
     * )
     */
    @Override
    public LocalizationFile loadFile(LocalizationFile locFile) throws SequoyahException
    {
        Document document;

        if (!locFile.getFile().exists())
        {
            LocalizationFileBean bean = new LocalizationFileBean(locFile);
            bean.setType(StringLocalizationFile.class.getName());
            for (NodeManager nodeManager : NodeManagerProvider.getInstance().getNodeManagers())
            {
                // according to NodeManagerProvider, the first element is a
                // StringNodeManager and the second is an ArrayStringNodeManager
                nodeManager.loadFile(bean, locFile);
            }

            LocalizationFile tempFile =
                    LocalizationFileFactory.getInstance().createLocalizationFile(bean);
            try
            {
                createFile(tempFile);
            }
            catch (SequoyahException e)
            {
                BasePlugin.getLogger().error("Could not create StringLocalizationFile: ", e); //$NON-NLS-1$
            }
        }
        try
        {
            if (System.getProperty("java.version").startsWith("1.5")) { //$NON-NLS-1$ //$NON-NLS-2$
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();

                document = builder.parse(new File(locFile.getFile().getLocation().toString()));

            }
            else
            {
                InputStream inputStream =
                        new FileInputStream(locFile.getFile().getLocation().toFile());
                DOMImplementation dimp =
                        DOMImplementationRegistry.newInstance().getDOMImplementation("XML 3.0"); //$NON-NLS-1$
                DOMImplementationLS dimpls = (DOMImplementationLS) dimp.getFeature("LS", "3.0"); //$NON-NLS-1$ //$NON-NLS-2$
                LSInput lsi = dimpls.createLSInput();
                LSParser lsp =
                        dimpls.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS,
                                "http://www.w3.org/2001/XMLSchema"); //$NON-NLS-1$
                LSParserFilter filter = new LocalizationXMLParserFilter();
                lsp.setFilter(filter);
                lsi.setEncoding("UTF-8"); //$NON-NLS-1$
                lsi.setByteStream(inputStream);
                document = lsp.parse(lsi);

            }

            updateLocalizationFileContent(locFile, document);

        }
        catch (Exception e)
        {
            SequoyahExceptionStatus status =
                    new SequoyahExceptionStatus(IStatus.ERROR, AndroidLocalizationPlugin.PLUGIN_ID,
                            0, Messages.StringLocalizationFileManager_Exception_CouldNotLoadFile
                                    + locFile.getFile().getFullPath().toOSString()
                                    + ". " + e.getMessage(), e); //$NON-NLS-1$
            throw new SequoyahException(status);
        }

        return locFile;
    }

    /*
     * 
     */
    private void updateLocalizationFileContent(LocalizationFile localizationFile, Document document)
    {
        ArrayList<StringNode> stringNodes = new ArrayList<StringNode>();
        ArrayList<StringNode> stringArrays = new ArrayList<StringNode>();

        for (NodeManager nodeManager : NodeManagerProvider.getInstance().getNodeManagers())
        {
            if (nodeManager instanceof StringNodeManager)
            {
                nodeManager.updateLocalizationFileContent(document, stringNodes);
            }
            else if (nodeManager instanceof ArrayStringNodeManager)
            {
                ((ArrayStringNodeManager) nodeManager).updateLocalizationFileContent(document,
                        stringArrays);
            }
        }

        ((AndroidStringLocalizationFile) localizationFile).setSavedXMLDocument(document);

        ((AndroidStringLocalizationFile) localizationFile).clearStringNodes();
        ((AndroidStringLocalizationFile) localizationFile).setStringNodes(stringNodes);

        ArrayList<StringArrayNode> stringArrays2 = new ArrayList<StringArrayNode>();
        for (StringNode node : stringArrays)
        {
            stringArrays2.add((StringArrayNode) node);
        }
        ((AndroidStringLocalizationFile) localizationFile).setStringArrayNodes(stringArrays2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.sequoyah.localization.android.manager.ILocalizationFileManager
     * #
     * createFile(org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile
     * )
     */
    @Override
    public void createFile(LocalizationFile localizationFile) throws SequoyahException
    {
        try
        {
            String filePath = localizationFile.getFile().getFullPath().toOSString();

            if (!localizationFile.getFile().exists())
            {
                localizationFile.getFile().getLocation();
                IPath fileToSave = null;
                if (localizationFile.getLocalizationProject() != null)
                {
                    fileToSave =
                            new Path(localizationFile.getLocalizationProject().getProject()
                                    .getLocation()
                                    + filePath);
                }
                else
                {
                    fileToSave = localizationFile.getFile().getLocation();
                }

                fileToSave.removeLastSegments(1).toFile().mkdirs();
                fileToSave.toFile().createNewFile();

                if (localizationFile.getLocalizationProject() != null)
                {
                    IFile iFile =
                            localizationFile.getLocalizationProject().getProject()
                                    .getFile(new Path(filePath));
                    localizationFile.setFile(iFile);
                }
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.newDocument();

            /*
             * Create XML nodes
             */
            Element resources = document.createElement(XML_RESOURCES_TAG);

            for (NodeManager nodeManager : NodeManagerProvider.getInstance().getNodeManagers())
            {
                // according to NodeManagerProvider, the first element is a
                // StringNodeManager and the second is an ArrayStringNodeManager
                nodeManager.createFile(document, resources, localizationFile);
            }

            document.appendChild(resources);

            saveXMLDocument(localizationFile.getFile().getLocation().toFile(), document);
            ((AndroidStringLocalizationFile) localizationFile).setSavedXMLDocument(document);
            localizationFile.getFile().getProject()
                    .refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
            // loadAllFiles(localizationFile.getLocalizationProject().getProject());

        }
        catch (Exception e)
        {
            throw new SequoyahException();
        }
    }

    /**
     * Saves XML file into the file system
     * 
     * @param file
     * @param document
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerConfigurationException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws TransformerException
     */
    private void saveXMLDocument(File file, Document document)
            throws TransformerFactoryConfigurationError, TransformerConfigurationException,
            FileNotFoundException, UnsupportedEncodingException, TransformerException
    {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", 4); //$NON-NLS-1$
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
        transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$

        FileOutputStream fileOutputStream = null;

        /*
         * At this point, localization file has the correct file
         */
        fileOutputStream = new FileOutputStream(file);

        try
        {
            StreamResult result =
                    new StreamResult(new OutputStreamWriter(fileOutputStream, "UTF-8")); //$NON-NLS-1$

            removeXMLDocumentBlankNodes(document);
            DOMSource source = new DOMSource(document);

            transformer.transform(source, result);
        }
        finally
        {
            if (fileOutputStream != null)
            {
                try
                {
                    fileOutputStream.close();
                }
                catch (IOException e)
                {
                    // do nothing
                }
            }
        }

        unescapeEntity(file);

    }

    /**
     * Unescape some entities (such as &lt;) replacing it for the character form
     * (such as <) in a file. This entities are automatically created by the
     * Transform object, but we need the character form in the localization
     * file.
     * 
     * @param the
     *            text file whose entities are to be unescaped
     */
    private void unescapeEntity(File file1)
    {
        File file2 = null;
        DataOutputStream dataOutput = null;
        FileInputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        BufferedReader reader = null;

        try
        {
            file2 = File.createTempFile("temp_localization", //$NON-NLS-1$
                    "dom"); //$NON-NLS-1$
            FileUtil.copyFile(file1, file2);
            inputStream = new FileInputStream(file2);
            fileOutputStream = new FileOutputStream(file1);
            dataOutput = new DataOutputStream(fileOutputStream);
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8")); //$NON-NLS-1$
            String line = null;

            if (reader != null)
            {
                line = reader.readLine();
                while (line != null)
                {
                    line = unescapeEntity(line);
                    dataOutput.write(line.getBytes("UTF-8")); //$NON-NLS-1$
                    String eol = getEndOfLine();
                    dataOutput.write(eol.getBytes("UTF-8")); //$NON-NLS-1$
                    line = reader.readLine();
                }
            }
        }
        catch (Exception e)
        {
            BasePlugin.logError("Error translating file.", e); //$NON-NLS-1$
        }
        finally
        {

            try
            {
                if (inputStream != null)
                {
                    inputStream.close();
                    inputStream = null;
                }
                if (fileOutputStream != null)
                {
                    fileOutputStream.close();
                    fileOutputStream = null;
                }
                if (reader != null)
                {
                    reader.close();
                    reader = null;
                }
            }
            catch (IOException e)
            {
                BasePlugin.logError("Error cleaning up objects after translation", e); //$NON-NLS-1$
            }

            if (dataOutput != null)
            {
                try
                {
                    dataOutput.close();
                }
                catch (IOException e)
                {
                    BasePlugin.logError("Error closing file after translation", //$NON-NLS-1$
                            e);
                }
                dataOutput = null;
            }

            if (file2 != null)
            {
                if (!file2.delete())
                {
                    file2.deleteOnExit();
                }
            }
            file1 = null;
        }
    }

    /**
     * @return the end-of-line character set on preference page if it exists, or the system default otherwise.
     */
    private String getEndOfLine()
    {
        String result = null;

        //Preferences preferences = getPreferenceNode();
        Preferences preferences =
                Platform.getPreferencesService().getRootNode().node(InstanceScope.SCOPE);

        try
        {
            //check if the eof preference is set
            if ((preferences != null) && preferences.nodeExists(Platform.PI_RUNTIME))
            {
                result =
                        preferences.node(Platform.PI_RUNTIME).get(Platform.PREF_LINE_SEPARATOR,
                                null);
            }
        }
        catch (BackingStoreException e)
        {
            //do nothing...
        }

        //if the preference is set, then return it, else return the system line separator 
        return result != null ? result : System.getProperty("line.separator"); //$NON-NLS-1$;
    }

    /*
     * 
     */
    private String unescapeEntity(String content)
    {
        String returnContent = content;
        returnContent = returnContent.replaceAll("&lt;", //$NON-NLS-1$
                "<"); //$NON-NLS-1$
        returnContent = returnContent.replaceAll("&gt;", //$NON-NLS-1$
                ">"); //$NON-NLS-1$
        returnContent = returnContent.replaceAll("&#13;", ""); //$NON-NLS-2$ //$NON-NLS-1$
        return returnContent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.sequoyah.localization.android.manager.ILocalizationFileManager
     * #
     * updateFile(org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile
     * )
     */
    @Override
    public void updateFile(LocalizationFile locFile) throws SequoyahException
    {
        AndroidStringLocalizationFile androidLocalizationFile =
                (AndroidStringLocalizationFile) locFile;
        Document document = androidLocalizationFile.getSavedXMLDocument();
        updateFile(androidLocalizationFile, document);
        if (document == null)
        {
            document = androidLocalizationFile.getSavedXMLDocument();
        }
        // save modified document
        try
        {
            localizationFileProject = locFile.getFile().getProject();
            saveXMLDocument(locFile.getFile().getLocation().toFile(), document);
        }
        catch (Exception e)
        {
            SequoyahException sqE = new SequoyahException();
            sqE.setStackTrace(e.getStackTrace());
            throw sqE;
        }
        locFile.setDirty(false);
    }

    /**
     * Update the XML representation of the file
     * 
     * @param locFile
     * @param document
     * @throws SequoyahException
     */
    private void updateFile(LocalizationFile locFile, Document document) throws SequoyahException
    {

        AndroidStringLocalizationFile androidLocalizationFile =
                (AndroidStringLocalizationFile) locFile;
        if (document == null)
        {
            // file not created yet, do it
            createFile(androidLocalizationFile);
            document = androidLocalizationFile.getSavedXMLDocument();
        }
        else
        {
            // file already created, update
            // update nodes on XML Document according to LocalizationFile model
            Map<String, StringNode> singleStringsToUpdateOrAdd = new HashMap<String, StringNode>();
            Map<String, StringArrayNode> arrayStringsToUpdateOrAdd =
                    new HashMap<String, StringArrayNode>();

            for (StringNode stringNode : ((StringLocalizationFile) locFile).getStringNodes())
            {

                if (!(stringNode instanceof StringArrayNode))
                {
                    singleStringsToUpdateOrAdd.put(stringNode.getKey(), stringNode);
                }
            }

            for (StringArrayNode stringArray : ((StringLocalizationFile) locFile).getStringArrays())
            {
                arrayStringsToUpdateOrAdd.put(stringArray.getKey(), stringArray);
            }

            NodeList resourcesList = document.getElementsByTagName("resources"); //$NON-NLS-1$
            // if there is no resource tag, add at least one
            if (resourcesList.getLength() == 0)
            {
                Element resources = document.createElement(XML_RESOURCES_TAG);
                document.appendChild(resources);
                resourcesList = document.getElementsByTagName("resources"); //$NON-NLS-1$
            }
            for (int i = 0; i < resourcesList.getLength(); i++)
            {
                Element resource = (Element) resourcesList.item(i);
                // // remove nodes
                visitToRemoveDOMChildren(document,
                        resource.getFirstChild(),
                        "name", //$NON-NLS-1$
                        androidLocalizationFile.getSingleEntryToRemove(),
                        androidLocalizationFile.getArrayEntryToRemove(),
                        androidLocalizationFile.getArrayItemsToRemove());
                // // update nodes
                visitToUpdateDOMChildren(document, resource.getFirstChild(), "name", //$NON-NLS-1$
                        singleStringsToUpdateOrAdd, arrayStringsToUpdateOrAdd);
                // add new nodes (append in the end of file)
                visitToAddDOMChildren(document, singleStringsToUpdateOrAdd,
                        arrayStringsToUpdateOrAdd, resource);
            }
        }
    }

    /**
     * 
     * Visit all child in the DOM tree to add an attribute.
     * 
     * @param document
     * @param singleStringsToUpdateOrAdd
     *            map with single items to update/add (in the end of visit, only
     *            add items should remain)
     * @param arrayStringsToUpdateOrAdd
     *            map with array item to update/add (in the end of visit, only
     *            add item should
     * @param resource
     */
    public void visitToAddDOMChildren(Document document,
            Map<String, StringNode> singleStringsToUpdateOrAdd,
            Map<String, StringArrayNode> arrayStringsToUpdateOrAdd, Element resource)
    {
        for (Map.Entry<String, StringNode> singleEntry : singleStringsToUpdateOrAdd.entrySet())
        {
            StringNode stringNode = singleEntry.getValue();
            StringNodeManager stMgr = NodeManagerProvider.getInstance().getStringNodeManager();
            stMgr.addSingleEntry(document, resource, stringNode);
        }
        for (Map.Entry<String, StringArrayNode> arrayEntry : arrayStringsToUpdateOrAdd.entrySet())
        {
            StringArrayNode stringArray = arrayEntry.getValue();
            ArrayStringNodeManager arrMgr =
                    NodeManagerProvider.getInstance().getArrayStringNodeManager();
            arrMgr.addArrayEntry(document, resource, stringArray);
        }
    }

    /**
     * 
     * Visit all child in the DOM tree, as an attribute is found, it gets the
     * corresponding item on LocalizationFile and updates the values
     * 
     * @param document
     * @param visitingNode
     * @param attrName
     * @param singleStringsToRemove
     *            map with single items to remove (in the end of visit, only add
     *            items should remain)
     * @param arrayItemsToRemove
     *            map with array item to remove (in the end of visit, only add
     *            item should
     */
    public void visitToRemoveDOMChildren(Document document, Node visitingNode, String attrName,
            Map<String, StringNode> singleStringsToRemove,
            Map<String, StringArrayNode> arrayStringsToRemove,
            List<StringArrayItemNode> arrayItemsToRemove)
    {
        while (visitingNode != null)
        {
            Node nextNodeToVisit = visitingNode.getNextSibling();
            if (visitingNode.getNodeType() == Node.ELEMENT_NODE)
            {
                Attr attribute = getAttribute((Element) visitingNode, attrName);
                // check LocalizationFile to get new value for attribute
                if (attribute != null)
                {
                    String keyName = attribute.getValue();
                    // try to find as single string entry
                    StringNode foundStringNode = singleStringsToRemove.get(keyName);
                    if (foundStringNode != null)
                    {
                        // remove single entry
                        visitingNode.getParentNode().removeChild(visitingNode);
                    }
                    else
                    {
                        // not found single string - try to find as entire array
                        // entry
                        StringArrayNode foundStringArray = arrayStringsToRemove.get(keyName);
                        if (foundStringArray != null)
                        {
                            // remove array entry
                            visitingNode.getParentNode().removeChild(visitingNode);
                        }
                        else
                        {
                            // not found entire array entry - try to find as
                            // item
                            // inside array
                            NodeList items = visitingNode.getChildNodes();
                            int itemsIndex = 0;
                            int arrayItemIndex = 0;
                            while (itemsIndex < items.getLength())
                            {
                                Object obj = items.item(itemsIndex);
                                if (obj instanceof Element)
                                {
                                    Element item = (Element) obj;
                                    Iterator<StringArrayItemNode> iterator =
                                            arrayItemsToRemove.iterator();
                                    StringArrayItemNode toRemove = null;
                                    while (iterator.hasNext() && (toRemove == null))
                                    {
                                        StringArrayItemNode candidate = iterator.next();
                                        if (candidate.getKey().equals(keyName)
                                                && (candidate.getPosition() == arrayItemIndex))
                                        {
                                            toRemove = candidate;
                                        }
                                    }
                                    if (toRemove != null)
                                    {
                                        // remove array item
                                        item.getParentNode().removeChild(item);
                                    }
                                    arrayItemIndex++;
                                }
                                itemsIndex++;
                            }
                        }
                    }
                }
            }
            visitingNode = nextNodeToVisit;
        }
    }

    /**
     * 
     * Visit all child in the DOM tree, as an attribute is found, it gets the
     * corresponding item on LocalizationFile and updates the values
     * 
     * @param visitingNode
     * @param attrName
     * @param singleStringsToUpdateOrAdd
     *            map with single items to update/add (in the end of visit, only
     *            add items should remain)
     * @param arrayStringsToUpdateOrAdd
     *            map with array item to update/add (in the end of visit, only
     *            add item should
     */
    public void visitToUpdateDOMChildren(Document document, Node visitingNode, String attrName,
            Map<String, StringNode> singleStringsToUpdateOrAdd,
            Map<String, StringArrayNode> arrayStringsToUpdateOrAdd)
    {
        while (visitingNode != null)
        {
            if (visitingNode.getNodeType() == Node.ELEMENT_NODE)
            {
                Attr attribute = getAttribute((Element) visitingNode, attrName);
                if (attribute != null)
                {
                    // check LocalizationFile to get new value for attribute
                    String keyName = attribute.getValue();
                    // try to find as single string entry
                    StringNode foundStringNode = singleStringsToUpdateOrAdd.get(keyName);
                    if (foundStringNode != null)
                    {
                        // update single entry
                        String newSingleEntryValue = foundStringNode.getValue();
                        visitingNode.setTextContent(newSingleEntryValue);
                        StringNodeManager stMgr =
                                NodeManagerProvider.getInstance().getStringNodeManager();
                        stMgr.createOrUpdateComment(document, foundStringNode,
                                (Element) visitingNode);
                        // remove item from map (it was already updated and it
                        // does
                        // not need to add in the end)
                        singleStringsToUpdateOrAdd.remove(keyName);
                    }
                    else
                    {
                        // not found single string - try to find as array entry
                        StringArrayNode foundStringArray = arrayStringsToUpdateOrAdd.get(keyName);
                        if (foundStringArray != null)
                        {
                            NodeList items = visitingNode.getChildNodes();
                            List<StringArrayItemNode> nodes = foundStringArray.getValues();
                            int itemsIndex = 0;
                            int nodesIndex = 0;
                            while ((itemsIndex < items.getLength()) && (nodesIndex < nodes.size()))
                            {
                                Object obj = items.item(itemsIndex);
                                if (obj instanceof Element)
                                {
                                    Element item = (Element) obj;
                                    StringNode nodeItem = nodes.get(nodesIndex);
                                    // update array entry
                                    String newArrayItemValue = nodeItem.getValue();
                                    item.setTextContent(newArrayItemValue);
                                    ArrayStringNodeManager arrMgr =
                                            NodeManagerProvider.getInstance()
                                                    .getArrayStringNodeManager();
                                    arrMgr.createOrUpdateComment(document, nodeItem, item);
                                    // remove item from map (it was already
                                    // updated
                                    // and it does not need to add in the end)
                                    arrayStringsToUpdateOrAdd.remove(keyName);
                                    nodesIndex++;
                                }
                                itemsIndex++;
                            }
                            while (nodesIndex < nodes.size())
                            {
                                // add new items into exiting array
                                if (visitingNode instanceof Element)
                                {
                                    Element arrayElement = (Element) visitingNode;
                                    StringNode stringNode = nodes.get(nodesIndex);
                                    ArrayStringNodeManager arrMgr =
                                            NodeManagerProvider.getInstance()
                                                    .getArrayStringNodeManager();
                                    arrMgr.createArrayItem(document, arrayElement, stringNode);
                                    nodesIndex++;
                                    arrayStringsToUpdateOrAdd.remove(keyName);
                                }
                            }

                        }
                    }
                }
            }
            visitingNode = visitingNode.getNextSibling();
        }
    }

    /**
     * Get Attribute with the given name
     * 
     * @param el
     *            visiting element
     * @param attrName
     *            attribute name to search
     * @return attribute
     */
    public static Attr getAttribute(Element el, String attrName)
    {
        Attr attr = el.getAttributeNode(attrName);
        return attr;
    }

    /*
     * 
     */
    private String getXMLAsString(Document document)
    {
        String resultString = null;
        try
        {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4); //$NON-NLS-1$
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$

            /*
             * At this point, localization file has the correct file
             */
            StringWriter writer = new StringWriter();

            StreamResult result = new StreamResult(writer); //$NON-NLS-1$

            removeXMLDocumentBlankNodes(document);

            DOMSource source = new DOMSource(document);

            transformer.transform(source, result);

            resultString = writer.toString();

            resultString = unescapeEntity(resultString);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return resultString;
    }

    /**
     * Text nodes are removed from document
     * 
     * @param document
     *            XML document to be edited
     */
    void removeXMLDocumentBlankNodes(Document document)
    {
        ArrayList<Node> nodesToRemove = new ArrayList<Node>();
        ArrayList<Node> arrayItemsToRemove = new ArrayList<Node>();

        NodeList resourceNodeList = document.getElementsByTagName("resources"); //$NON-NLS-1$
        // exactly one resource node
        if (resourceNodeList.getLength() == 1)
        {
            Node resourceNode = resourceNodeList.item(0);
            NodeList nodeList = resourceNode.getChildNodes();

            for (int i = 0; i < nodeList.getLength(); i++)
            {
                Node node = nodeList.item(i);

                if (node.getNodeType() == Node.TEXT_NODE)
                {
                    nodesToRemove.add(node);
                }
                // element nodes enter here
                else if (node.getNodeName().equals("string-array")) { //$NON-NLS-1$
                    arrayItemsToRemove.clear();
                    NodeList arrayItemList = node.getChildNodes();
                    for (int j = 0; j < arrayItemList.getLength(); j++)
                    {
                        Node arrayItem = arrayItemList.item(j);

                        if (arrayItem.getNodeType() == Node.TEXT_NODE)
                        {
                            // mark it for removal
                            arrayItemsToRemove.add(arrayItem);
                        }
                    }
                    for (Node current : arrayItemsToRemove)
                    {
                        node.removeChild(current);
                    }
                }
            }
            // remove blank nodes
            for (Node current : nodesToRemove)
            {
                resourceNode.removeChild(current);
            }
        }
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.sequoyah.localization.android.manager.ILocalizationFileManager
     * # updateLocalizationFileContent()
     */
    @Override
    public void updateLocalizationFileContent(LocalizationFile localizationFile, String content)
            throws SequoyahException
    {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try
        {
            builder = factory.newDocumentBuilder();
            ByteArrayInputStream byteInputStream =
                    new ByteArrayInputStream(content.getBytes("UTF-8")); //$NON-NLS-1$
            Document document = builder.parse(byteInputStream);
            updateLocalizationFileContent((StringLocalizationFile) localizationFile, document);
        }
        catch (Exception e)
        {
            SequoyahExceptionStatus status =
                    new SequoyahExceptionStatus(IStatus.ERROR, AndroidLocalizationPlugin.PLUGIN_ID,
                            0, Messages.AndroidLocalizationSchema_6
                                    + localizationFile.getFile().getFullPath(), e);
            throw new SequoyahException(status);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.sequoyah.localization.android.manager.ILocalizationFileManager
     * # getLocalizationFileContent()
     */
    @Override
    public Object getLocalizationFileContent(LocalizationFile locFile)
    {
        String text = null;
        if (locFile instanceof AndroidStringLocalizationFile)
        {
            AndroidStringLocalizationFile localizationFile =
                    (AndroidStringLocalizationFile) locFile;
            try
            {
                updateFile(localizationFile, localizationFile.getSavedXMLDocument());
            }
            catch (SequoyahException e)
            {

            }
            text = getXMLAsString(localizationFile.getSavedXMLDocument());
        }
        return text;
    }
}