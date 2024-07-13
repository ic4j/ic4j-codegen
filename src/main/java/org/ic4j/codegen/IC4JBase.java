package org.ic4j.codegen;

import java.io.FileReader;
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
import org.ic4j.agent.identity.Secp256k1Identity;
import org.ic4j.candid.parser.IDLParser;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.types.Principal;

public abstract class IC4JBase {
	
	protected static IDLParser parseIDL(String candid, String canisterId, String network, String identityFile, String identityType) throws IOException, URISyntaxException, CodegenException {
		Reader idlReader;
		if (candid != null) {
			try {
				idlReader = Files
						.newBufferedReader(Paths.get(candid));
			} catch (Throwable t) {
				throw new CodegenException("Cannot read IDL file " + candid);
			}
		} else {
			if(canisterId == null)
				throw new CodegenException("Define properly option --canister-id");
			
			Principal canister = Principal.fromString(canisterId);

			Identity identity = new AnonymousIdentity();

			if (identityFile != null) {
				Reader sourceReader = new FileReader(identityFile);

				if (identityType != null && "basic".equals(identityType))
					identity = BasicIdentity.fromPEMFile(sourceReader);
				else
					identity = Secp256k1Identity.fromPEMFile(sourceReader);
			}
			
			
			if(canisterId == null)
				throw new CodegenException("Define properly option --network");
			
			ReplicaTransport transport = ReplicaOkHttpTransport.create(network);
			Agent agent = new AgentBuilder().transport(transport).identity(identity).build();

			String serviceIDL = agent.getIDL(canister);

			idlReader = new StringReader(serviceIDL);
		}

		IDLParser idlParser = new IDLParser(idlReader);

		idlParser.parse();

		return idlParser;
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
		javaWriterContext.verbose = verbose;
		javaWriterContext.annotate = annotate;			
		
		reactNativeWriter.write(javaWriterContext, Paths.get(outputDir), className, types, services);
	}	
}
