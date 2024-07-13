package org.ic4j.codegen.test;

import org.ic4j.candid.parser.IDLParser;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.codegen.SpringWriter;
import org.ic4j.codegen.SpringWriterContext;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringWriterTest {
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
		LOG = LoggerFactory.getLogger(SpringWriterTest.class);
	}

	@Test
	public void test() {

		try {
			this.generateService(ORBIT_IDL_FILE, "org.ic4j.spring.orbit","OrbitService", "OrbitProxy");
			this.generateService(DOCUTRACK_IDL_FILE, "org.ic4j.spring.docutrack","DocuTrackService", "DocuTrackProxy");			
			this.generateService(SWOP_IDL_FILE, "org.ic4j.spring.swop", "SwopService","SwopProxy");	
			this.generateService(PYTHIA_IDL_FILE, "org.ic4j.spring.orally.pythia","PythiaService", "PythiaProxy");			
			this.generateService(WS_TYPES_IDL_FILE, "org.ic4j.spring.websocket","WSService", "WSProxy");
			this.generateService(ORIGYN_IDL_FILE, "org.ic4j.spring.origyn", "OrigynService","OrigynProxy");
			this.generateService(LOAN_IDL_FILE, "org.ic4j.spring.loan", "LoanService","LoanProxy");
			this.generateService(IC_TEST_IDL_FILE, "org.ic4j.spring.ic","ICTestService", "ICTestProxy");
			this.generateService(TRADING_TEST_IDL_FILE, "org.ic4j.spring.trading","TradingService", "TradingProxy");
			this.generateService(SWIFT_IDL_FILE, "org.ic4j.spring.swift","SwiftService", "SwiftProxy");
			this.generateService(ROSETTANET_IDL_FILE, "org.ic4j.spring.rosettanet","RosettaNetService", "RosettaNetProxy");

		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getLocalizedMessage());
		}

	}

	void generateService(String idlFileName, String packageName, String serviceClassName, String proxyClassName) throws IOException {

		Reader reader = Files
				.newBufferedReader(Paths.get(getClass().getClassLoader().getResource(idlFileName).getPath()));
		IDLParser idlParser = new IDLParser(reader);
		idlParser.parse();

		Map<String, IDLType> types = idlParser.getTypes();

		Map<String, IDLType> services = idlParser.getServices();

		SpringWriter springWriter = new SpringWriter();
		
		SpringWriterContext springWriterContext = new SpringWriterContext();
		
		springWriterContext.packageName = packageName;
		springWriterContext.canisterId = "un4fu-tqaaa-aaaab-qadjq-cai";
		springWriterContext.effectiveCanisterId = "un4fu-tqaaa-aaaab-qadjq-cai";
		
		springWriterContext.identityType = "Basic";
		
		springWriterContext.network = "http://localhost:4943/";
		
		springWriter.useFuture = true;

		springWriter.write(springWriterContext,Paths.get(""),serviceClassName, proxyClassName, types, services);
	}
}
