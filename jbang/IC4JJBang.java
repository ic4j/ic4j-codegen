///usr/bin/env jbang "$0" "$@" ; exit $?

package main;

import org.ic4j.codegen.IC4J;

import picocli.CommandLine;

//JAVA 11+
//REPOS mavencentral
//DEPS org.ic4j:ic4j-codegen:0.8.5
//DEPS org.ic4j:ic4j-agent:0.8.5
//DEPS org.ic4j:ic4j-reactnative:0.8.0
//DEPS org.ic4j:ic4j-spring:0.8.0
//DEPS org.ic4j:ic4j-candid:0.8.5
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.squareup:javapoet:1.13.0
//DEPS info.picocli:picocli:4.7.7

/**
 * Main to run IC4JJBang
 */
public class IC4JJBang {
    public static void main(String... args) {
		int rc = new CommandLine(new IC4J()).execute(args);
		System.exit(rc);
    }
}
