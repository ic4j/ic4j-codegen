package org.ic4j.codegen.test;


import org.ic4j.candid.parser.IDLType;
import org.ic4j.codegen.MotokoWriter;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


public class XSDWriterTest {
	static Logger LOG;

	static final String MOTOKO_FIToFICustomerCreditTransferV07_TYPES_FILE = "FIToFICustomerCreditTransferV07Types.mo";
	static final String MOTOKO_FinancialInstitutionCreditTransferV09_TYPES_FILE = "FinancialInstitutionCreditTransferV09Types.mo";
	
	static final String XSD_TEST_IC_FILE = "TestSchema.xsd";

	static {
		LOG = LoggerFactory.getLogger(XSDWriterTest.class);
	}

	@Test
	public void test() {

		try {
			Reader reader = Files
					.newBufferedReader(Paths.get(getClass().getClassLoader().getResource(XSD_TEST_IC_FILE).getPath()));
			XmlSchemaCollection schemaCol = new XmlSchemaCollection();
			XmlSchema schema = schemaCol.read(reader);
			
			Map<QName,XmlSchemaElement> schemaElements = schema.getElements();
			
			Map<QName, XmlSchemaType> schemaTypes = schema.getSchemaTypes();
			
			List<XmlSchemaObject> schemaObjects = schema.getItems();
			
			Document[] schemas =schema.getAllSchemas();
			/*
			IDLType type = JAXBSerializer.getIDLType(GroupHeader70.class);
			
			type = JAXBSerializer.getIDLType(TaxRecordPeriod1Code.class);
					
			type = JAXBSerializer.getIDLType(FIToFICustomerCreditTransferV07.class);
			
			MotokoWriter motokoWriter = new MotokoWriter();
			
			motokoWriter.writeFile(Paths.get(MOTOKO_FIToFICustomerCreditTransferV07_TYPES_FILE), type);
			
			type = JAXBSerializer.getIDLType(FinancialInstitutionCreditTransferV09.class);
			
			motokoWriter.writeFile(Paths.get(MOTOKO_FinancialInstitutionCreditTransferV09_TYPES_FILE), type);	
			*/		
			
		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
		}

	}
}
