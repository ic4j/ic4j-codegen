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

import java.io.IOException;
import java.io.StringWriter;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.NullType;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import org.ic4j.agent.annotations.Agent;
import org.ic4j.agent.annotations.EffectiveCanister;
import org.ic4j.agent.annotations.Canister;
import org.ic4j.agent.annotations.Identity;
import org.ic4j.agent.annotations.IdentityType;
import org.ic4j.agent.annotations.Properties;
import org.ic4j.agent.annotations.Transport;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.ParserError;
import org.ic4j.candid.types.Label;

import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Type;
import org.ic4j.react.ICModule;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class ReactNativeWriter {
	public boolean useList = false;
	public boolean useFuture = true;
	
	public void write(JavaWriterContext javaWriterContext, Path path, String proxyName, Map<String,IDLType> types, Map<String,IDLType> services) throws IOException
	{				
		javaWriterContext.writer = new StringWriter();	
		
		ReactNativeWriter javaWriter = new ReactNativeWriter();		
		
		for(IDLType idlType : services.values())
		{
			javaWriter.generateModule(javaWriterContext,proxyName, idlType);		
		}		
			
		
		Set<String> keys = javaWriterContext.proxies.keySet(); 
				
		for(String key : keys)
		{
			JavaFile javaFile = javaWriterContext.proxies.get(key);
			javaFile.writeTo(System.out);
			javaFile.writeTo(path);	
		}
			
	}
	
	public void generateModule(JavaWriterContext context,String moduleName, IDLType idlType) throws IOException
	{
		String className = this.normalizeClassName(moduleName);
		
		MethodSpec constructor1 = MethodSpec.constructorBuilder()
			    .addModifiers(Modifier.PUBLIC)
			    .addParameter(ReactApplicationContext.class, "context")
			    .addStatement("super($N,null,null,null,null)", "context")
			    .addException(URISyntaxException.class)
			    .addException(NoSuchAlgorithmException.class)
			    .build();	
		
		MethodSpec constructor2 = MethodSpec.constructorBuilder()
			    .addModifiers(Modifier.PUBLIC)
			    .addParameter(ReactApplicationContext.class, "context")
			    .addParameter(String.class, "location")
			    .addParameter(String.class, "canisterId")
			    .addParameter(String.class, "identityFile")
			    .addStatement("super($N,$N,$N,null,$N)", "context","location","canisterId","identityFile")
			    .addException(URISyntaxException.class)
			    .addException(NoSuchAlgorithmException.class)
			    .build();	
		
		MethodSpec getNameMethod = MethodSpec.methodBuilder("getName").addModifiers(Modifier.PUBLIC).addAnnotation(Override.class).addStatement("return \"$N\"",moduleName).returns(String.class).build();
		
		TypeSpec.Builder moduleBuilder = TypeSpec.classBuilder(className).superclass(ICModule.class).addModifiers(Modifier.PUBLIC).addMethod(constructor1).addMethod(constructor2).addMethod(getNameMethod);
		

		if(context.annotate)
		{	
			if(context.network != null)
			{
				AnnotationSpec transportAnnotation = AnnotationSpec.builder(Transport.class).addMember("url","$S",context.network).build();
				
				
				IdentityType identityType = IdentityType.ANONYMOUS;
				if(context.identityType != null)
				{
					switch(context.identityType)
					{
						case "Basic" : identityType = IdentityType.BASIC;break;
						case "Secp256k1" : identityType = IdentityType.SECP256K1;break;
						case "Prime256v1" : identityType = IdentityType.PRIME256V1;break;
					}
				}
				
				AnnotationSpec identityAnnotation = AnnotationSpec.builder(Identity.class).addMember("type", "$T.$L",IdentityType.class, identityType ).build();

	
				moduleBuilder.addAnnotation(AnnotationSpec.builder(Agent.class).addMember("transport", "$L",transportAnnotation).addMember("identity","$L",identityAnnotation).build());
			}
			
			if(context.canisterId != null)
				moduleBuilder.addAnnotation(AnnotationSpec.builder(Canister.class).addMember("value", "$S", context.canisterId).build());
			if(context.effectiveCanisterId != null)
				moduleBuilder.addAnnotation(AnnotationSpec.builder(EffectiveCanister.class).addMember("value", "$S", context.effectiveCanisterId).build());
			
			if(context.loadIDL)
				moduleBuilder.addAnnotation(AnnotationSpec.builder(Properties.class).addMember("loadIDL", "$L", context.loadIDL).build());
		}
		
		Map<String,IDLType> meths = idlType.getMeths();
		
		Set<String> names = meths.keySet();
		
		for(String name : names)
		{
			if(name != null)
			{
				IDLType methType = meths.get(name);
				
				String funcName = name;
				
				funcName = this.normalizeVarName(funcName);
				
				if(name.equals("void"))
					funcName = "voidFunc";
				
				MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(funcName)
						.addAnnotation(AnnotationSpec.builder(ReactMethod.class).build())
						.addModifiers(Modifier.PUBLIC);
				
				String args = "";
				if(!methType.args.isEmpty())
				{
					int i = 0;
					for(IDLType argType : methType.args)
					{
						String argName = "arg" + i++;
						
						args += "," + argName;
						
						TypeName typeName = this.toTypeName(argType);					
						
						methodBuilder.addParameter(ParameterSpec.builder(typeName, argName).build());
					}
				}
				
				String responseClass = "Void.class";
				
				if(!methType.rets.isEmpty())
				{
					IDLType retType = methType.rets.get(0);
					
					if(retType.getType() == Type.OPT)
						retType = idlType.getInnerType();								

					if(retType != null)
						responseClass = this.toTypeClass(retType.getType()).getSimpleName() + ".class";								
				}			
				
				
				if(!methType.modes.isEmpty())
				{	
					if(methType.modes.get(0) == Mode.QUERY)
					{
						methodBuilder.addStatement("this.query(promise,\"$N\"," + responseClass + args + ")",funcName);
					}
					else if(methType.modes.get(0) == Mode.ONEWAY)
					{
						methodBuilder.addStatement("this.oneway(promise,\"$N\"" + args + ")",funcName);
					}
					else
					{
						methodBuilder.addStatement("this.update(promise,\"$N\"," + responseClass + args + ")",funcName);
					}
				}
				

				

				
				methodBuilder.addParameter(ParameterSpec.builder(Promise.class, "promise").build());
				
				moduleBuilder.addMethod(methodBuilder.build());
			}
		}
		
		TypeSpec moduleSpec = moduleBuilder.build();
		
		JavaFile javaFile = JavaFile.builder(context.packageName, moduleSpec)
			    .build();
		
		context.proxies.put(moduleName, javaFile);

	}
	
	
	
	TypeName toTypeName(IDLType idlType)
	{

		TypeName typeName = ClassName.get(Object.class);

		if(idlType.getType().isPrimitive() || idlType.getType() == Type.FUNC || idlType.getType() == Type.SERVICE )
				typeName = ClassName.get(this.toTypeClass(idlType.getType()));

		else if(idlType.getType() == Type.VARIANT || idlType.getType() == Type.RECORD)
			typeName = ClassName.get(ReadableMap.class);	
		else if(idlType.getType() == Type.OPT)
		{
			IDLType innerIdlType = idlType.getInnerType();
			
			typeName = this.toTypeName(innerIdlType);			
		}
		else if(idlType.getType() == Type.VEC)
		{
			IDLType innerIdlType = idlType.getInnerType();
			
			// handle binary
			if(innerIdlType.getType() == Type.NAT8)
				typeName = ClassName.get(String.class);
			else				
				typeName = ClassName.get(ReadableArray.class);
		}


		return typeName;
	}
	
	Class toTypeClass(Type type)
	{
			switch (type) {
			case NULL:
				return NullType.class;
			case BOOL:
				return Boolean.class;
			case INT:
				return Double.class;
			case INT8:
				return Double.class;
			case INT16:
				return Double.class;
			case INT32:
				return Double.class;
			case INT64:
				return Double.class;
			case NAT:
				return Double.class;
			case NAT8:
				return Double.class;
			case NAT16:
				return Double.class;
			case NAT32:
				return Double.class;
			case NAT64:
				return Double.class;
			case FLOAT32:
				return Double.class;
			case FLOAT64:
				return Double.class;
			case TEXT:
				return String.class;
			case RESERVED:
				return Object.class;
			case EMPTY:
				return Void.class;				
			case PRINCIPAL:
				return String.class;
			case VEC:
				return ReadableArray.class;
			case RECORD:
				return ReadableMap.class;
			case VARIANT:
				return ReadableMap.class;				
			case FUNC:
				return ReadableMap.class;
			case SERVICE:
				return ReadableMap.class;				
			default:
				throw ParserError.create(ParserError.ParserErrorCode.CUSTOM, String.format("Unknown type %s", type));
			}
					
	}
	
	String normalizeClassName(String name)
	{
		if(name == null)
			return name;
		
		name = name.replaceFirst(name.substring(0, 1), name.substring(0, 1).toUpperCase());
		
		return name;
	}
	
	String normalizeVarName(String name)
	{
		if(name.equals("void"))
			name = "voidField";
		if(name.equals("record"))
			name = "recordField";
		
		if(name == null)
			return name;
		
		name = name.replaceFirst(name.substring(0, 1), name.substring(0, 1).toLowerCase());
		
		return name;
	}	
}
