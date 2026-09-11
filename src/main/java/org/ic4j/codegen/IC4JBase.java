package org.ic4j.codegen;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.ic4j.agent.Agent;
import org.ic4j.agent.AgentBuilder;
import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.http.ReplicaOkHttpTransport;
import org.ic4j.agent.identity.AnonymousIdentity;
import org.ic4j.agent.identity.BasicIdentity;
import org.ic4j.agent.identity.Identity;
import org.ic4j.agent.identity.Prime256v1Identity;
import org.ic4j.agent.identity.Secp256k1Identity;
import org.ic4j.candid.parser.IDLParser;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.types.Principal;

public abstract class IC4JBase {
	
	protected static IDLParser parseIDL(String candid, String canisterId, String network, String identityFile, String identityType) throws IOException, URISyntaxException, CodegenException {
		String normalizedIdentityType = normalizeIdentityType(identityType);
		if (candid != null) {
			try (Reader idlReader = Files.newBufferedReader(Paths.get(candid))) {
				return parseIDL(idlReader);
			} catch (IOException e) {
				throw new CodegenException("Cannot read IDL file " + candid, e);
			}
		}

		if(isBlank(canisterId))
			throw new CodegenException("Define option --canister-id when --candid is not provided");
		if(isBlank(network))
			throw new CodegenException("Define option --network when --candid is not provided");

		Principal canister = Principal.fromString(canisterId);
		Identity identity = loadIdentity(identityFile, normalizedIdentityType);
		ReplicaTransport transport = ReplicaOkHttpTransport.create(network);
		try {
			Agent agent = new AgentBuilder().transport(transport).identity(identity).build();
			String serviceIDL = agent.getIDL(canister);
			return parseIDL(new StringReader(serviceIDL));
		} finally {
			transport.close();
		}
	}

	private static IDLParser parseIDL(Reader idlReader) throws IOException {
		IDLParser idlParser = new IDLParser(idlReader);
		idlParser.parse();
		return idlParser;
	}

	private static Identity loadIdentity(String identityFile, String identityType) throws CodegenException {
		if(isBlank(identityFile))
			return new AnonymousIdentity();

		java.nio.file.Path identityPath = Paths.get(identityFile);
		try {
			switch(identityType) {
			case "basic":
				return BasicIdentity.fromPEMFile(identityPath);
			case "prime256v1":
				return Prime256v1Identity.fromPEMFile(identityPath);
			case "secp256k1":
			default:
				return Secp256k1Identity.fromPEMFile(identityPath);
			}
		} catch (RuntimeException e) {
			throw new CodegenException("Cannot load " + identityType + " identity from " + identityFile, e);
		}
	}

	static String normalizeIdentityType(String identityType) throws CodegenException {
		if(isBlank(identityType))
			return "secp256k1";

		String normalized = identityType.trim().toLowerCase(java.util.Locale.ROOT);
		if(!normalized.equals("basic") && !normalized.equals("secp256k1") && !normalized.equals("prime256v1"))
			throw new CodegenException("Unsupported identity type '" + identityType
					+ "'. Expected basic, secp256k1, or prime256v1");
		return normalized;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
	
	protected static void createJavaProxy(String outputDir, String packageName, String className, boolean verbose, boolean annotate, String candid, String canisterId, String network, String identityFile, String identityType) throws CodegenException, IOException, URISyntaxException {
		IDLParser idlParser = parseIDL(candid,canisterId,network,identityFile, identityType);	

		Map<String, IDLType> types = idlParser.getTypes();

		Map<String, IDLType> services = idlParser.getServices();

		JavaWriter javaWriter = new JavaWriter();

		JavaWriterContext javaWriterContext = new JavaWriterContext();

		javaWriterContext.packageName = packageName;
		javaWriterContext.canisterId = canisterId;
		javaWriterContext.network = network;			
		javaWriterContext.identityType = normalizeWriterIdentityType(identityType);
		javaWriterContext.verbose = verbose;
		javaWriterContext.annotate = annotate;			
		
		javaWriter.write(javaWriterContext, Paths.get(outputDir), className, types, services);
	}
	
	protected static void createSpringService(String outputDir, String packageName, String serviceClassName, String className,  boolean verbose, boolean annotate, String candid, String canisterId, String network, String identityFile, String identityType) throws CodegenException, IOException, URISyntaxException {
		IDLParser idlParser = parseIDL(candid,canisterId,network,identityFile, identityType);	

		Map<String, IDLType> types = idlParser.getTypes();

		Map<String, IDLType> services = idlParser.getServices();

		SpringWriter springWriter = new SpringWriter();

		SpringWriterContext springWriterContext = new SpringWriterContext();

		springWriterContext.packageName = packageName;
		springWriterContext.canisterId = canisterId;
		springWriterContext.network = network;			
		springWriterContext.identityType = normalizeWriterIdentityType(identityType);
		springWriterContext.verbose = verbose;
		springWriterContext.annotate = annotate;		
		
		springWriter.write(springWriterContext, Paths.get(outputDir),serviceClassName, className, types, services);
	}	
	
	protected static void createReactNativeModule(String outputDir, String packageName, String className, boolean verbose, boolean annotate, String candid, String canisterId, String network, String identityFile, String identityType) throws CodegenException, IOException, URISyntaxException {
		IDLParser idlParser = parseIDL(candid,canisterId,network,identityFile, identityType);	

		Map<String, IDLType> types = idlParser.getTypes();

		Map<String, IDLType> services = idlParser.getServices();


		ReactNativeWriter reactNativeWriter = new ReactNativeWriter();

		JavaWriterContext javaWriterContext = new JavaWriterContext();

		javaWriterContext.packageName = packageName;
		javaWriterContext.canisterId = canisterId;
		javaWriterContext.network = network;			
		javaWriterContext.identityType = normalizeWriterIdentityType(identityType);
		javaWriterContext.verbose = verbose;
		javaWriterContext.annotate = annotate;			
		
		reactNativeWriter.write(javaWriterContext, Paths.get(outputDir), className, types, services);
	}	

	private static String normalizeWriterIdentityType(String identityType) throws CodegenException {
		return isBlank(identityType) ? null : normalizeIdentityType(identityType);
	}
}
