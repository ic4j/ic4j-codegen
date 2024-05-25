/*
 * Copyright 2021 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.ic4j.codegen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public abstract class JAXBGenerator {
	static Logger LOG = LoggerFactory.getLogger(JAXBGenerator.class);
	TypeWriter typeWriter;
	
	
	public JAXBGenerator(TypeWriter typeWriter) {
		super();
		this.typeWriter = typeWriter;
	}

	public void writeTypes(String dictionaryFileName, String outDir)
	{
		Document xmlDocument;
		try {
			xmlDocument = getDocument(dictionaryFileName);
			
			XPath xPath = XPathFactory.newInstance().newXPath();
			String messageExpression = "/messages/message";
			NodeList messageElements = (NodeList) xPath.compile(messageExpression).evaluate(xmlDocument, XPathConstants.NODESET);
			
			for(int i = 0; i < messageElements.getLength(); i++ )
			{
				Element messageElement = (Element) messageElements.item(i);
				
				String className = (messageElement.hasAttribute("class")) ? messageElement.getAttribute("class") : messageElement.getAttribute("name");
				
				className = messageElement.getAttribute("package") + "." + className;
				
				String typeOutDir = outDir + File.separatorChar + messageElement.getAttribute("type").toLowerCase();
				
				String typeFileName = messageElement.getAttribute("name") + "V" + messageElement.getAttribute("version") + "Types." + this.typeWriter.getExtension();
				try {
					Class typeClass = Class.forName(className);
					this.writeType(typeClass, typeOutDir, typeFileName);
				} catch (ClassNotFoundException e) {
					LOG.error(e.getLocalizedMessage(), e);
				}			
			}			
						
		} catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
			LOG.error(e.getLocalizedMessage(), e);
		}  	
	}
	
	public abstract void writeType(Class type, String outDir, String fileName);

	
    static Document getDocument(String fileName) throws SAXException, IOException, ParserConfigurationException
    {
    	FileInputStream fileInputStream = new FileInputStream(fileName);
    	DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = builderFactory.newDocumentBuilder();
    	Document xmlDocument = builder.parse(fileInputStream);
		
    	return xmlDocument;
    }

}
