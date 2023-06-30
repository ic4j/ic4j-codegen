///usr/bin/env jbang "$0" "$@" ; exit $?

package main;

import org.ic4j.codegen.IC4J;

import picocli.CommandLine;

//JAVA 11+
//REPOS mavencentral
//DEPS org.ic4j:ic4j-codegen:0.6.19-RC1
//DEPS org.ic4j:ic4j-agent:0.6.19.3
//DEPS org.ic4j:ic4j-reactnative:0.6.19-RC3
//DEPS org.slf4j:slf4j-simple:2.0.6
//DEPS com.squareup:javapoet:1.13.0
//DEPS info.picocli:picocli:4.7.4

/**
 * Main to run IC4JJBang
 */
public class IC4JJBang {
    public static void main(String... args) {
		int rc = new CommandLine(new IC4J()).execute(args);
		System.exit(rc);
    }
}
