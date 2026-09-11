/*
 * Copyright 2021 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.ic4j.codegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.squareup.javapoet.JavaFile;
import org.ic4j.candid.parser.IDLType;

public class JavaWriterContext extends TypeWriterContext {
	public String packageName;
	public String canisterId;
	public String effectiveCanisterId;
	
	public String network;
	
	public String identityType;
	
	public boolean loadIDL = false;
	
	public boolean annotate = true;
	
	Map<String, JavaFile> proxies = new HashMap<>();
	
	Map<String, JavaFile> types = new HashMap<>();

	private final Map<IDLType, String> typeNames = new IdentityHashMap<>();
	private final Set<String> usedTypeNames = new HashSet<>();

	String claimTypeName(IDLType type, String suggestedName) {
		String existing = typeNames.get(type);
		if(existing != null)
			return existing;

		String typeName = JavaIdentifier.unique(JavaIdentifier.className(suggestedName), usedTypeNames);
		typeNames.put(type, typeName);
		type.setName(typeName);
		return typeName;
	}
}
