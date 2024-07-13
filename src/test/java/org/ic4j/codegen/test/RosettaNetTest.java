package org.ic4j.codegen.test;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

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
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.ic4j.types.Service;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dscope.rosettanet.interchange.purchaseorderrequest.v02_05.ProductLineItemType;
import io.dscope.rosettanet.interchange.purchaseorderrequest.v02_05.PurchaseOrderRequest;
import io.dscope.rosettanet.interchange.purchaseorderrequest.v02_05.PurchaseOrderRequestType;
import io.dscope.rosettanet.interchange.purchaseorderrequest.v02_05.PurchaseOrderType;

public class RosettaNetTest {
	static Logger LOG;
	
	static final String PURCHASE_ORDER_FILE = "PurchaseOrderRequest.xml";	

	static String PROPERTIES_FILE_NAME = "test.properties";

	static String IC_URL_PROPERTY = "rosettanet.ic.location";
	static String IC_CANISTER_ID_PROPERTY = "rosettanet.ic.canister";

	static {
		LOG = LoggerFactory.getLogger(RosettaNetTest.class);
	}

	@Test
	public void test() {
		InputStream propInputStream = SwiftTest.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);

		try {
			Properties props = new Properties();
			props.load(propInputStream);
			
			InputStream pipPropInputStream = RosettaNetTest.class.getClassLoader().getResourceAsStream("RosettaNet.properties");
			
			Properties env = new Properties();
			env.load(pipPropInputStream);
			
			String packages = env.getProperty("packages");
				
		    JAXBContext context = JAXBContext.newInstance(packages);
		    Unmarshaller jaxbUnmarshaller = context.createUnmarshaller();

		    PurchaseOrderRequest po = (PurchaseOrderRequest)jaxbUnmarshaller.unmarshal(RosettaNetTest.class.getClassLoader().getResourceAsStream(PURCHASE_ORDER_FILE));
		    
			StringWriter sw = new StringWriter();
			Marshaller jaxbMarshaller = context.createMarshaller();
			jaxbMarshaller.marshal(po, sw);
			String poString = sw.toString();
			
			LOG.debug(poString);
			
			/*
		    IDLValue idlValue = IDLValue.create(po, JAXBSerializer.create());
			List<IDLValue> args = new ArrayList<IDLValue>();
			args.add(idlValue);

			IDLArgs idlArgs = IDLArgs.create(args);
			
			byte[] buf = idlArgs.toBytes();	
			
			PurchaseOrderRequest poResult = IDLArgs.fromBytes(buf).getArgs().get(0)
					.getValue(JAXBDeserializer.create(), PurchaseOrderRequest.class);
			
			*/
			
			ReplicaTransport transport = ReplicaApacheHttpTransport.create(props.getProperty(IC_URL_PROPERTY));
			
			Agent agent = new AgentBuilder().transport(transport).nonceFactory(new NonceFactory())
					.build();
				
			agent.fetchRootKey();
			
			Service service = new Service(Principal.fromString(props.getProperty(IC_CANISTER_ID_PROPERTY)));
				
			ProxyBuilder proxyBuilder = ProxyBuilder.create(agent);
				
			ServiceProxy serviceProxy = proxyBuilder.getServiceProxy(service);	

			
			Func funcValue = new Func("purchaseOrderRequest");
			
			FuncProxy<PurchaseOrderRequest> jaxbFuncProxy2 = serviceProxy.getFuncProxy(funcValue);
			
			jaxbFuncProxy2.setSerializers(new JAXBSerializer());
			jaxbFuncProxy2.setDeserializer(new JAXBDeserializer());
			
			jaxbFuncProxy2.setResponseClass(PurchaseOrderRequest.class);
			
			PurchaseOrderRequest poResult = jaxbFuncProxy2.call(po);
						
			
			sw = new StringWriter();
			jaxbMarshaller.marshal(poResult, sw);
			String poResultString = sw.toString();	
			
			LOG.debug(poResultString);

			Assertions.assertEquals(poString, poResultString);
			
		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
		}

	}
}
