package org.ic4j.codegen.test;

import java.io.InputStream;
import java.util.Properties;

import org.ic4j.agent.Agent;
import org.ic4j.agent.AgentBuilder;
import org.ic4j.agent.FuncProxy;
import org.ic4j.agent.NonceFactory;
import org.ic4j.agent.ProxyBuilder;
import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.ServiceProxy;
import org.ic4j.agent.http.ReplicaApacheHttpTransport;
import org.ic4j.candid.jaxb.javax.JAXBDeserializer;
import org.ic4j.candid.jaxb.javax.JAXBSerializer;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.ic4j.types.Service;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prowidesoftware.swift.model.mx.MxPain00100103;
import com.prowidesoftware.swift.model.mx.dic.CustomerCreditTransferInitiationV03;
import com.prowidesoftware.swift.utils.Lib;

public class SwiftTest {
	static Logger LOG;
	
	static String PROPERTIES_FILE_NAME = "test.properties";
	static final String CREDIT_TRANSFER_FILE = "CustomerCreditTransferInitiationV03.xml";	

	static String IC_URL_PROPERTY = "swift.ic.location";
	static String IC_CANISTER_ID_PROPERTY = "swift.ic.canister";

	static {
		LOG = LoggerFactory.getLogger(SwiftTest.class);
	}

	@Test
	public void test() {
		InputStream propInputStream = SwiftTest.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);

		try {
			Properties props = new Properties();
			props.load(propInputStream);
			
			MxPain00100103 mx = MxPain00100103.parse(Lib.readResource(CREDIT_TRANSFER_FILE));
				
			CustomerCreditTransferInitiationV03 transfer = mx.getCstmrCdtTrfInitn();
			
			ReplicaTransport transport = ReplicaApacheHttpTransport.create(props.getProperty(IC_URL_PROPERTY));
			
			Agent agent = new AgentBuilder().transport(transport).nonceFactory(new NonceFactory())
					.build();
				
			agent.fetchRootKey();
			
			Service service = new Service(Principal.fromString(props.getProperty(IC_CANISTER_ID_PROPERTY)));
				
			ProxyBuilder proxyBuilder = ProxyBuilder.create(agent);
				
			ServiceProxy serviceProxy = proxyBuilder.getServiceProxy(service);				
			
			Func funcValue = new Func("initiateTransfer");
			
			
			FuncProxy<CustomerCreditTransferInitiationV03> jaxbFuncProxy2 = serviceProxy.getFuncProxy(funcValue);
			
			jaxbFuncProxy2.setSerializers(new JAXBSerializer());
			jaxbFuncProxy2.setDeserializer(new JAXBDeserializer());
			
			jaxbFuncProxy2.setResponseClass(CustomerCreditTransferInitiationV03.class);
			
			//Verify XML Content
	        String xmlContentBefore = mx.document();
	        
	        LOG.info(xmlContentBefore);	
			
			CustomerCreditTransferInitiationV03 transferResult = jaxbFuncProxy2.call(transfer);
			
			mx.setCstmrCdtTrfInitn(transferResult);

	        //Verify XML Content
	        String xmlContentAfter = mx.document();
	        
	        LOG.info(xmlContentAfter);	

	        Assertions.assertEquals(xmlContentBefore, xmlContentAfter);	
			
		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
		}

	}
}
