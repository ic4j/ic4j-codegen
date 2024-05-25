package org.ic4j.codegen.test;

import org.ic4j.candid.parser.IDLParser;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.codegen.JavaWriterContext;
import org.ic4j.codegen.ReactNativeWriter;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReactNativeWriterTest {
	static Logger LOG;

	static final String IC_TEST_IDL_FILE = "ic_test.did";
	static final String TRADING_TEST_IDL_FILE = "Trading.did";
	static final String TRADING2_IDL_FILE = "Trading2.did";	
	static final String SWIFT_IDL_FILE = "Swift.did";
	static final String ROSETTANET_IDL_FILE = "RosettaNet.did";	
	static final String ORIGYN_IDL_FILE = "origyn_nft_reference.did";	
	
	static final String PYTHIA_IDL_FILE = "pythia.did";		
	
	static final String SWOP_IDL_FILE = "swop.did";	
	
	static final String DOCUTRACK_IDL_FILE = "DocuTrack.did";	

	static {
		LOG = LoggerFactory.getLogger(ReactNativeWriterTest.class);
	}

	@Test
	public void test() {

		try {
			this.generateModule(DOCUTRACK_IDL_FILE, "org.ic4j.reactnative.docutrack", "DocuTrackModule");			
			this.generateModule(SWOP_IDL_FILE, "org.ic4j.reactnative.swop", "SwopModule");	
			this.generateModule(PYTHIA_IDL_FILE, "org.ic4j.reactnative.orally.pythia", "PythiaModule");				
			this.generateModule(IC_TEST_IDL_FILE, "test.reactnative.ic", "ICReactNativeModule");
			this.generateModule(ORIGYN_IDL_FILE, "test.reactnative.origyn", "OrigynModule");
		    this.generateModule(TRADING_TEST_IDL_FILE, "test.reactnative.trading", "TradingModule");
			this.generateModule(SWIFT_IDL_FILE, "test.reactnative.swift", "SwiftModule");
			this.generateModule(ROSETTANET_IDL_FILE, "test.reactnative.rosettanet", "RosettaNetModule");

		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getLocalizedMessage());
		}

	}

	void generateModule(String idlFileName, String packageName, String moduleClassName) throws IOException {

		Reader reader = Files
				.newBufferedReader(Paths.get(getClass().getClassLoader().getResource(idlFileName).getPath()));
		IDLParser idlParser = new IDLParser(reader);
		idlParser.parse();

		Map<String, IDLType> types = idlParser.getTypes();

		Map<String, IDLType> services = idlParser.getServices();

		ReactNativeWriter reactNativeWriter = new ReactNativeWriter();
		
		JavaWriterContext javaWriterContext = new JavaWriterContext();
		
		javaWriterContext.packageName = packageName;
		javaWriterContext.canisterId = "un4fu-tqaaa-aaaab-qadjq-cai";
		javaWriterContext.effectiveCanisterId = "un4fu-tqaaa-aaaab-qadjq-cai";
		
		javaWriterContext.network= "https://m7sm4-2iaaa-aaaab-qabra-cai.ic0.app/";
		
		javaWriterContext.identityType = "Secp256k1";

		reactNativeWriter.write(javaWriterContext,Paths.get(""), moduleClassName, types, services);

	}
}
