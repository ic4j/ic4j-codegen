///usr/bin/env jbang "$0" "$@" ; exit $?

package main;

import org.ic4j.codegen.IC4J;

import picocli.CommandLine;

//JAVA 11+
//REPOS mavencentral
//DEPS org.ic4j:ic4j-codegen:0.7.1
//DEPS org.ic4j:ic4j-agent:0.7.1
//DEPS org.ic4j:ic4j-reactnative:0.7.1
//DEPS org.ic4j:ic4j-spring:0.7.1.1
//DEPS org.ic4j:ic4j-candid:0.7.1
//DEPS org.slf4j:slf4j-simple:2.0.13
//DEPS com.squareup:javapoet:1.13.0
//DEPS info.picocli:picocli:4.7.6

/**
 * Main to run IC4JJBang
 */
public class IC4JJBang {
    public static void main(String... args) {
		int rc = new CommandLine(new IC4J()).execute(args);
		System.exit(rc);
    }
}
