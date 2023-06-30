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

	static {
		LOG = LoggerFactory.getLogger(ReactNativeWriterTest.class);
	}

	@Test
	public void test() {

		try {
			this.generateModule(IC_TEST_IDL_FILE, "test.reactnative.ic", "ICReactNativeModule");
			//this.generateModule(TRADING_TEST_IDL_FILE, "test.trading", "TradingProxy");
			//this.generateModule(SWIFT_IDL_FILE, "test.swift", "SwiftProxy");
			//this.generateModule(ROSETTANET_IDL_FILE, "test.rosettanet", "RosettaNetProxy");

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

		reactNativeWriter.write(javaWriterContext,Paths.get(""), moduleClassName, types, services);

	}
}
