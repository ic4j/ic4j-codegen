/*
 * Copyright 2026 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */

package org.ic4j.codegen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class JavaIdentifier {
	private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
			"abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
			"const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
			"finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
			"interface", "long", "native", "new", "package", "private", "protected", "public",
			"return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
			"throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false",
			"null", "_"));

	private JavaIdentifier() {
	}

	static String className(String value) {
		return normalize(value, true, "GeneratedType");
	}

	static String memberName(String value) {
		return normalize(value, false, "generatedMember");
	}

	static String unique(String candidate, Set<String> used) {
		String unique = candidate;
		int suffix = 2;
		while (!used.add(unique))
			unique = candidate + suffix++;
		return unique;
	}

	private static String normalize(String value, boolean capitalize, String fallback) {
		if (value == null || value.isEmpty())
			return fallback;

		StringBuilder result = new StringBuilder();
		boolean capitalizeNext = capitalize;
		for (int i = 0; i < value.length(); i++) {
			char current = value.charAt(i);
			if (!Character.isJavaIdentifierPart(current)) {
				capitalizeNext = result.length() > 0;
				continue;
			}

			if (result.length() == 0 && !Character.isJavaIdentifierStart(current))
				result.append(capitalize ? "Type" : "value");

			result.append(capitalizeNext ? Character.toUpperCase(current) : current);
			capitalizeNext = false;
		}

		if (result.length() == 0)
			result.append(fallback);
		if (!capitalize)
			result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
		if (KEYWORDS.contains(result.toString()))
			result.append(capitalize ? "Type" : "Value");
		return result.toString();
	}
}
