package org.ic4j.codegen.test;

import org.ic4j.candid.parser.IDLParser;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.codegen.JavaWriter;
import org.ic4j.codegen.JavaWriterContext;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaWriterTest {
	static Logger LOG;

	static final String IC_TEST_IDL_FILE = "ic_test.did";
	static final String TRADING_TEST_IDL_FILE = "Trading.did";
	static final String TRADING2_IDL_FILE = "Trading2.did";	
	static final String SWIFT_IDL_FILE = "Swift.did";
	static final String ROSETTANET_IDL_FILE = "RosettaNet.did";	
	
	static final String LOAN_IDL_FILE = "LoanProvider.did";	
	
	static final String ORIGYN_IDL_FILE = "origyn_nft_reference.did";	
	
	static final String WS_TYPES_IDL_FILE = "pingpong_backend.did";	
	
	static final String PYTHIA_IDL_FILE = "pythia.did";		
	
	static final String SWOP_IDL_FILE = "swop.did";	
	
	static final String DOCUTRACK_IDL_FILE = "DocuTrack.did";	
	
	static final String ORBIT_IDL_FILE = "orbit.did";	

	static {
		LOG = LoggerFactory.getLogger(JavaWriterTest.class);
	}

	@Test
	public void test() {

		try {
			this.generateProxy(ORBIT_IDL_FILE, "org.ic4j.orbit", "Orbit");
			this.generateProxy(DOCUTRACK_IDL_FILE, "org.ic4j.docutrack", "DocuTrack");			
			this.generateProxy(SWOP_IDL_FILE, "org.ic4j.swop", "Swop");	
			this.generateProxy(PYTHIA_IDL_FILE, "org.ic4j.orally.pythia", "Pythia");			
			this.generateProxy(WS_TYPES_IDL_FILE, "org.ic4j.websocket", "WSProxy");
			this.generateProxy(ORIGYN_IDL_FILE, "test.origyn", "OrigynProxy");
			this.generateProxy(LOAN_IDL_FILE, "test.loan", "LoanProxy");
			this.generateProxy(IC_TEST_IDL_FILE, "test.ic", "ICTestProxy");
			this.generateProxy(TRADING_TEST_IDL_FILE, "test.trading", "TradingProxy");
			this.generateProxy(SWIFT_IDL_FILE, "test.swift", "SwiftProxy");
			this.generateProxy(ROSETTANET_IDL_FILE, "test.rosettanet", "RosettaNetProxy");

		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getLocalizedMessage());
		}

	}

	void generateProxy(String idlFileName, String packageName, String proxyClassName) throws IOException {

		Reader reader = Files
				.newBufferedReader(Paths.get(getClass().getClassLoader().getResource(idlFileName).getPath()));
		IDLParser idlParser = new IDLParser(reader);
		idlParser.parse();

		Map<String, IDLType> types = idlParser.getTypes();

		Map<String, IDLType> services = idlParser.getServices();

		JavaWriter javaWriter = new JavaWriter();
		
		JavaWriterContext javaWriterContext = new JavaWriterContext();
		
		javaWriterContext.packageName = packageName;
		javaWriterContext.canisterId = "un4fu-tqaaa-aaaab-qadjq-cai";
		javaWriterContext.effectiveCanisterId = "un4fu-tqaaa-aaaab-qadjq-cai";
		
		javaWriterContext.identityType = "Basic";
		
		javaWriterContext.network = "http://localhost:4943/";
		
		javaWriter.useFuture = false;

		javaWriter.write(javaWriterContext,Paths.get(""), proxyClassName, types, services);

	}
}
