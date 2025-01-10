/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Inc and others. All rights reserved
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Rigo (Eldorado Research Institute) 
 * [245114] Enhance persistence policies
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [271695] - Support to non-persistent instances of devices
 * Mauren Brenner (Eldorado) - Bug [280813] - Support saving instance info outside the workspace
 * Fabio Rigo (Eldorado) - Bug [288006] - Unify features of InstanceManager and InstanceRegistry
 * Daniel Barboza Franco (Eldorado) - Bug [287187] -Save device instance information in a directory defined in runtime.
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [288301] - Device view crashes when there is a device plug-in missing.
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.manager.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.Platform;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class DeviceXmlWriter implements IDeviceXmlTags
{
    private static FileOutputStream outputStream;

	/**
     * Stores the instance data on a XML file located in the workspace root.
     */
    public static void saveInstances() {
        File path = DevicePlugin.getDeviceXmlLocation();
        File file = new File(path, DevicePlugin.getDeviceXmlFileName());

        Document document = createDocument(file);

        if (document != null) {

            try {
                Element root = document.getDocumentElement();
                Element instancesRoot = createInstancesElement(document);
                root.appendChild(instancesRoot);

                Transformer transformer = TransformerFactory.newInstance()
                        .newTransformer();
                transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
                transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$

                if (!file.getParentFile().exists()) {
                	file.getParentFile().mkdirs();
                	if (Platform.OS_WIN32.equals(Platform.getOS())) {
        				String[] hideCommand = new String[] {"attrib", "+H", file.getParentFile().getAbsolutePath()};
        				try {
        					Runtime.getRuntime().exec(hideCommand);
        				} catch (IOException e) {
        					BasePlugin.logError("Unable to hide Sequoyah's configuration directory");
        				}
        			}
                }
                
                outputStream = new FileOutputStream(file
                        .getAbsoluteFile());
                StreamResult result = new StreamResult(outputStream);
                DOMSource source = new DOMSource(document);
                transformer.transform(source, result);
            } catch (IOException ie) {
                ie.printStackTrace();
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerFactoryConfigurationError e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
            finally
            {
                try {
					outputStream.close();
				} catch (IOException e) {
					BasePlugin.logError("Could not close stream: "+e.getMessage());
				}
            }
        }
    }
    
    /**
     * Part of saveInstances
     * 
     * @param file -
     *            The File object representing the XML file.
     * @return The DOM document representing the XML file.
     */
    private static Document createDocument(File file) {    	
        Document document = null;
        if (file != null) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                DOMImplementation impl = builder.getDOMImplementation();

                // Create the document
                document = impl.createDocument(null,
                        SEQUOYAH_XML_ROOT, null);

            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
        return document;
    }

    /**
     * Creates DOM Element which represents all the instances defined. called by
     * saveInstances().
     */
    private static Element createInstancesElement(Document document) {
        Element instancesRoot = document
                .createElement(SEQUOYAH_XML_INSTANCES);
        InstanceRegistry registry = InstanceRegistry.getInstance();
        Iterator<IInstance> iterator = registry.getInstances().iterator();
   
        while (iterator.hasNext()) {
   
        	IInstance iIInst = iterator.next();
            IDeviceType device = DeviceTypeRegistry.getInstance().getDeviceTypeById(iIInst.getDeviceTypeId());

            /* 
             * Check if this instance should be persisted
             * If device == null, it means that this particular instance does not 
             * have a plug-in that declares its device, so we persist it anyway.
             */ 
            if (device == null || device.isPersistent()) {
           
	            Element element = document
	                    .createElement(SEQUOYAH_XML_INSTANCE);
	            element.setAttribute(SEQUOYAH_XML_INSTANCE_NAME, iIInst
	                    .getName());
	            String xml_device_id = iIInst.getDeviceTypeId();
	
	            element.setAttribute(SEQUOYAH_XML_INSTANCE_DEVICE_ID,
	                    xml_device_id);
	            if (element != null)
	                instancesRoot.appendChild(element);
	            Properties propProp = iIInst.getProperties();
	
	            for (Enumeration<?> e = propProp.keys(); e.hasMoreElements();) {
	                String propStr = (String) e.nextElement();
	                String propValStr = propProp.getProperty(propStr);
	
	                Element propElement = document.createElement(propStr);
	                Text propNode = document.createTextNode(propValStr);
	                propElement.appendChild(propNode);
	                element.appendChild(propElement);
	
	            }
            }

        }
        return instancesRoot;
    }
}
