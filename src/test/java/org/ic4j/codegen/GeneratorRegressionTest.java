package org.ic4j.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import org.ic4j.candid.parser.IDLParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class GeneratorRegressionTest {
	@TempDir
	Path outputDir;

	@Test
	void reactNativeDispatchesEveryCandidModeAndUnwrapsOptionalResponses() throws Exception {
		IDLParser parser = parse("service : {"
				+ " read: (opt int) -> (opt int) query;"
				+ " write: (text) -> (text);"
				+ " notify: (text) -> () oneway;"
				+ "}");
		ReactNativeWriter writer = new ReactNativeWriter();
		JavaWriterContext context = context("test.react");

		writer.write(context, outputDir, "TestModule", parser.getTypes(), parser.getServices());

		String source = readGenerated("test/react/TestModule.java");
		assertTrue(source.contains("this.query(promise,\"read\",Double.class,(Object) arg0)"));
		assertTrue(source.contains("this.update(promise,\"write\",String.class,(Object) arg0)"));
		assertTrue(source.contains("this.oneway(promise,\"notify\",(Object) arg0)"));
	}

	@Test
	void reactNativeRemapsFinalObjectMethods() throws Exception {
		IDLParser parser = parse("service : { getClass: () -> (text) query; }");
		ReactNativeWriter writer = new ReactNativeWriter();

		writer.write(context("test.react"), outputDir, "TestModule",
				parser.getTypes(), parser.getServices());

		String source = readGenerated("test/react/TestModule.java");
		assertTrue(source.contains("void getClassValue(Promise promise)"), source);
		assertTrue(source.contains("this.query(promise,\"getClass\",String.class)"), source);
	}

	@Test
	void javaWriterPreservesConfiguredCollectionAndFutureOptions() throws Exception {
		IDLParser parser = parse("service : { write: (vec text) -> (text); }");
		JavaWriter writer = new JavaWriter();
		writer.useList = true;
		writer.useFuture = false;
		JavaWriterContext context = context("test.java");

		writer.write(context, outputDir, "TestProxy", parser.getTypes(), parser.getServices());

		String source = readGenerated("test/java/TestProxy.java");
		assertTrue(source.contains("String write(@Argument(Type.TEXT) List<String> arg0)"));
		assertFalse(source.contains("CompletableFuture"));
	}

	@Test
	void identifiersAreJavaSafeAndCollisionAware() {
		assertEquals("Class", JavaIdentifier.className("class"));
		assertEquals("fooBar", JavaIdentifier.memberName("foo-bar"));
		assertEquals("value123", JavaIdentifier.memberName("123"));
		assertEquals("getClassValue", JavaIdentifier.methodName("getClass"));
		assertEquals("waitValue", JavaIdentifier.methodName("wait"));

		HashSet<String> used = new HashSet<>();
		assertEquals("name", JavaIdentifier.unique("name", used));
		assertEquals("name2", JavaIdentifier.unique("name", used));
	}

	@Test
	void objectMethodsAndSpringLifecycleNamesAreRemapped() throws Exception {
		IDLParser parser = parse("service : {"
				+ " getClass: () -> (text) query;"
				+ " init: () -> (text) query;"
				+ " update: () -> (text);"
				+ "}");
		JavaWriter javaWriter = new JavaWriter();
		javaWriter.write(context("test.reserved"), outputDir, "class",
				parser.getTypes(), parser.getServices());
		String proxy = readGenerated("test/reserved/Class.java");
		assertTrue(proxy.contains("getClassValue()"), proxy);

		SpringWriter springWriter = new SpringWriter();
		springWriter.useFuture = false;
		SpringWriterContext springContext = new SpringWriterContext();
		springContext.packageName = "test.spring";
		springWriter.write(springContext, outputDir, "ReservedService", "foo-bar",
				parser.getTypes(), parser.getServices());

		String service = readGenerated("test/spring/ReservedService.java");
		assertTrue(service.contains("implements FooBar"), service);
		assertTrue(service.contains("super.init(FooBar.class, null, null, null, null)"), service);
		assertTrue(service.contains("void initializeAgent()"), service);
		assertTrue(service.contains("String init()"), service);
		assertFalse(service.contains("@Async"), service);
	}

	@Test
	void collidingLabelsReceiveDistinctNestedTypes() throws Exception {
		IDLParser parser = parse("type Collision = record {"
				+ " record { first: text };"
				+ " value0: record { second: int };"
				+ "}; service : { read: () -> (Collision) query; };");
		JavaWriter writer = new JavaWriter();

		writer.write(context("test.collision"), outputDir, "TestProxy",
				parser.getTypes(), parser.getServices());

		String record = readGenerated("test/collision/Collision.java");
		assertTrue(record.contains("CollisionValue0"), record);
		assertTrue(record.contains("CollisionValue02"), record);
		assertTrue(record.contains(" value0;"));
		assertTrue(record.contains(" value02;"));
		assertTrue(Files.exists(outputDir.resolve("test/collision/CollisionValue0.java")));
		assertTrue(Files.exists(outputDir.resolve("test/collision/CollisionValue02.java")));
	}

	@Test
	void identityTypesAreCaseInsensitiveAndValidated() throws Exception {
		assertEquals("basic", IC4JBase.normalizeIdentityType("Basic"));
		assertEquals("prime256v1", IC4JBase.normalizeIdentityType("PRIME256V1"));
		assertThrows(CodegenException.class, () -> IC4JBase.normalizeIdentityType("unknown"));
	}

	@Test
	void omittedIdentityGeneratesAnonymousAnnotation() throws Exception {
		Path candid = outputDir.resolve("service.did");
		Files.write(candid, "service : { read: () -> (text) query; }"
				.getBytes(java.nio.charset.StandardCharsets.UTF_8));

		IC4JBase.createJavaProxy(outputDir.toString(), "test.identity", "IdentityProxy",
				false, true, candid.toString(), null, "http://localhost:4943/",
				null, null);

		String source = readGenerated("test/identity/IdentityProxy.java");
		assertTrue(source.contains("IdentityType.ANONYMOUS"));
	}

	@Test
	void missingSubcommandReturnsUsageError() {
		assertEquals(CommandLine.ExitCode.USAGE, new CommandLine(new IC4J()).execute());
	}

	@Test
	void multipleReturnValuesFailClearly() throws Exception {
		IDLParser parser = parse("service : { unsupported: () -> (text, int) query; }");
		JavaWriter writer = new JavaWriter();

		Exception error = assertThrows(Exception.class,
				() -> writer.write(context("test.multiple"), outputDir, "TestProxy",
						parser.getTypes(), parser.getServices()));

		assertTrue(error.getMessage().contains("multiple return values"));
	}

	private IDLParser parse(String candid) throws Exception {
		IDLParser parser = new IDLParser(new StringReader(candid));
		parser.parse();
		return parser;
	}

	private JavaWriterContext context(String packageName) {
		JavaWriterContext context = new JavaWriterContext();
		context.packageName = packageName;
		context.annotate = false;
		return context;
	}

	private String readGenerated(String relativePath) throws Exception {
		return new String(Files.readAllBytes(outputDir.resolve(relativePath)), java.nio.charset.StandardCharsets.UTF_8);
	}
}
