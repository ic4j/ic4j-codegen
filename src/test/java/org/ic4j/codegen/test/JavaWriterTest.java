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

	static {
		LOG = LoggerFactory.getLogger(JavaWriterTest.class);
	}

	@Test
	public void test() {

		try {
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

		javaWriter.write(javaWriterContext,Paths.get(""), proxyClassName, types, services);

	}
}
