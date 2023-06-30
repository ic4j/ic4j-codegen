package org.ic4j.codegen;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "ic4j")
public class IC4J extends IC4JBase implements Callable<Integer>  {
	static final String DEFAULT_NETWORK = "http://localhost:4943/";

	static Logger LOG = LoggerFactory.getLogger(IC4J.class);

	@Option(names = { "--identity" })
	private String identityFile;

	@Option(names = { "--identity-type" })
	String identityType;

	@Option(names = { "--network" })
	private String network = DEFAULT_NETWORK;

	@Option(names = { "--candid" })
	private String candid;

	@Option(names = { "--canister-id" })
	private String canisterId;

	@Option(names = { "--annotate" })
	private boolean annotate = false;

	@Option(names = { "--package-name" })
	private String packageName;

	@Option(names = { "--output-dir" })
	private String outputDir = "";
	
	@Option(names = { "--verbose" })
	private boolean verbose = false;	

	public static void main(String[] args) {

		int rc = new CommandLine(new IC4J()).execute(args);
		System.exit(rc);
	}

	public Integer call() throws Exception {
		System.out.println("Subcommand needed: 'java', 'reactnative', 'motoko' ");
		return 0;		
	}

	@Command(name = "java", description = "Generates Java Proxy inteface from IDL file or canister")
	public void createJavaProxy(
			@Parameters(index = "0", paramLabel = "<className>", description = "Java Proxy interface name") String className) throws Exception {
		try {

			createJavaProxy(this.outputDir, this.packageName,className,this.verbose,this.annotate,this.candid,this.canisterId,this.network,this.identityFile, this.identityType);			

		} catch (Exception e) {
			LOG.info(e.getLocalizedMessage());
			LOG.error(e.getLocalizedMessage(), e);		
			
			throw e;
		}	

	}
	
	@Command(name = "reactnative", description = "Generates React Native Module from IDL file or canister")
	public void createReactNativeModule(
			@Parameters(index = "0", paramLabel = "<className>", description = "Java React Native Module name") String className) throws Exception {
		try {

			createReactNativeModule(this.outputDir, this.packageName,className,this.verbose,this.annotate,this.candid,this.canisterId,this.network,this.identityFile, this.identityType);			

		} catch (Exception e) {
			LOG.info(e.getLocalizedMessage());
			LOG.error(e.getLocalizedMessage(), e);
			
			throw e;
		}		
	}	

}
