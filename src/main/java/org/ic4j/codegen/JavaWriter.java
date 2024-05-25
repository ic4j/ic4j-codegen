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

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.ic4j.agent.annotations.Agent;
import org.ic4j.agent.annotations.Argument;
import org.ic4j.agent.annotations.Canister;
import org.ic4j.agent.annotations.EffectiveCanister;
import org.ic4j.agent.annotations.Identity;
import org.ic4j.agent.annotations.IdentityType;
import org.ic4j.agent.annotations.Transport;
import org.ic4j.candid.annotations.Field;
import org.ic4j.candid.annotations.Modes;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.ParserError;
import org.ic4j.candid.types.Label;

import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.ic4j.types.Service;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class JavaWriter {
	public boolean useList = false;
	public boolean useFuture = true;
	
	public void write(JavaWriterContext javaWriterContext, Path path, String proxyName, Map<String,IDLType> types, Map<String,IDLType> services) throws IOException
	{				
		javaWriterContext.writer = new StringWriter();	
		
		JavaWriter javaWriter = new JavaWriter();
		
		for(IDLType idlType : types.values())
		{
			javaWriter.generateType(javaWriterContext, idlType);
		}
		
		for(IDLType idlType : services.values())
		{
			javaWriter.generateProxy(javaWriterContext,proxyName, idlType);		
		}		
		
		Set<String> keys = javaWriterContext.types.keySet(); 
		
		for(String key : keys)
		{
			JavaFile javaFile = javaWriterContext.types.get(key);
			
			if(javaWriterContext.verbose)
				javaFile.writeTo(System.out);
			
			javaFile.writeTo(path);			
		}		
		
		keys = javaWriterContext.proxies.keySet(); 
				
		for(String key : keys)
		{
			JavaFile javaFile = javaWriterContext.proxies.get(key);
			if(javaWriterContext.verbose)
				javaFile.writeTo(System.out);
			
			javaFile.writeTo(path);	
		}
			
	}
	
	public void generateProxy(JavaWriterContext context,String proxyName, IDLType idlType) throws IOException
	{
		String className = this.normalizeClassName(proxyName);
		
		TypeSpec.Builder proxyBuilder = TypeSpec.interfaceBuilder(className).addModifiers(Modifier.PUBLIC);
		
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
					}
				}
				
				AnnotationSpec identityAnnotation = AnnotationSpec.builder(Identity.class).addMember("type", "$T.$L",IdentityType.class, identityType ).build();
				
	
				proxyBuilder.addAnnotation(AnnotationSpec.builder(Agent.class).addMember("transport", "$L",transportAnnotation).addMember("identity","$L",identityAnnotation).build());
			}		
			
			if(context.canisterId != null)
				proxyBuilder.addAnnotation(AnnotationSpec.builder(Canister.class).addMember("value", "$S", context.canisterId).build());
			if(context.effectiveCanisterId != null)
				proxyBuilder.addAnnotation(AnnotationSpec.builder(EffectiveCanister.class).addMember("value", "$S", context.effectiveCanisterId).build());
		}
		

		Map<String,IDLType> meths = idlType.getMeths();
		
		Set<String> names = meths.keySet();
		
		for(String name : names)
		{
			if(name != null)
			{
				IDLType methType = meths.get(name);
				
				String funcName = name;
				
				funcName = this.normalizeMethodName(funcName);
				
				if(name.equals("void"))
					funcName = "voidFunc";
				
				MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(funcName)
						.addAnnotation(AnnotationSpec.builder(Name.class).addMember("value", "$S", name).build())
						.addModifiers(Modifier.PUBLIC,Modifier.ABSTRACT);
				
				
				boolean isFuture = true;
				if(!methType.modes.isEmpty())
				{	
					if(methType.modes.get(0) == Mode.QUERY)
					{
						methodBuilder.addAnnotation(AnnotationSpec.builder(Modes.class).addMember("value", "$T.$L",Mode.class, Mode.QUERY).build());
						isFuture = false;
					}
					else if(methType.modes.get(0) == Mode.ONEWAY)
					{
						methodBuilder.addAnnotation(AnnotationSpec.builder(Modes.class).addMember("value", "$T.$L",Mode.class, Mode.ONEWAY).build());
						isFuture = false;
					}
				}
				
				if(!methType.args.isEmpty())
				{
					int i = 0;
					for(IDLType argType : methType.args)
					{
						String argName = "arg" + i++;
						
						this.setTypeName(context, argType, this.normalizeClassName(funcName) + this.normalizeClassName(argName));
						
						TypeName typeName = this.toTypeName(argType, context.packageName, false);
						
						Type type = argType.getType();
						
						if(type == Type.OPT || type == Type.VEC)
							type = argType.getInnerType().getType();
						
						methodBuilder.addParameter(ParameterSpec.builder(typeName, argName).addAnnotation(AnnotationSpec.builder(Argument.class).addMember("value", "$T.$L",Type.class, type).build()).build());
					}
				}
				
				if(!methType.rets.isEmpty())
				{
					IDLType retType = methType.rets.get(0);
					
					this.setTypeName(context, retType, this.normalizeClassName(funcName) + "Response");
				
					TypeName typeName = this.toTypeName(retType, context.packageName, isFuture);
					
					methodBuilder.returns(typeName);
				}
				else if(isFuture && this.useFuture)
				{
					ClassName future = ClassName.get("java.util.concurrent", "CompletableFuture");
					TypeName futureTypeName = ParameterizedTypeName.get(future, ClassName.get(Void.class));
					methodBuilder.returns(futureTypeName);
				}
				
				proxyBuilder.addMethod(methodBuilder.build());
			}
		}
		
		TypeSpec proxySpec = proxyBuilder.build();
		
		JavaFile javaFile = JavaFile.builder(context.packageName, proxySpec)
			    .build();
		
		context.proxies.put(proxyName, javaFile);

	}
	
	public void generateType(JavaWriterContext context, IDLType idlType) throws IOException
	{

		String name = idlType.getName();

		if(name == null)
			return;
		
		if(idlType.getType().isPrimitive()) 
			return;
		
		if(idlType.getType() == Type.OPT || idlType.getType() == Type.VEC)
		{
			this.generateType(context, idlType.getInnerType());
			return;
		}

		if(idlType.getType() == Type.RECORD)
		{			
			TypeSpec recordSpec = this.generateRecord(context, name, idlType);
			
			JavaFile javaFile = JavaFile.builder(context.packageName, recordSpec)
				    .build();
			
			context.types.put(name, javaFile);
			
			return;	
		}
		if(idlType.getType() == Type.VARIANT)
		{			
			TypeSpec variantSpec = this.generateVariant(context, name, idlType);
			
			JavaFile javaFile = JavaFile.builder(context.packageName, variantSpec)
				    .build();
			
			context.types.put(name, javaFile);			
			
			return;		
		}		
	}
	
	public TypeSpec generateRecord(JavaWriterContext context,String typeName, IDLType idlType) throws IOException
	{
		String className = this.normalizeClassName(typeName);
		
		TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
		
		Map<Label, IDLType> types = idlType.getTypeMap();
		
		Set<Label> labels = types.keySet();
		
		for(Label label : labels)
		{
			String name = label.toString();
			if(name != null)
			{
				IDLType fieldType = types.get(label);
				
				Type type = fieldType.getType();
				
				String fieldName = name;
				
				this.setTypeName(context, fieldType, typeName + this.normalizeClassName(fieldName));
				
				fieldName = this.normalizeVarName(fieldName);
							
				TypeName fieldTypeName = this.toTypeName(fieldType, context.packageName, false);
				
				if(type == Type.OPT || type == Type.VEC)	
					type = fieldType.getInnerType().getType();
						
				FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldTypeName, fieldName)
						.addAnnotation(AnnotationSpec.builder(Name.class).addMember("value", "$S", name).build())
						.addAnnotation(AnnotationSpec.builder(Field.class).addMember("value", "$T.$L",Type.class, type).build())
						.addModifiers(Modifier.PUBLIC);

				
				typeBuilder.addField(fieldBuilder.build());
			}
		}
		
		return typeBuilder.build();
	}	
	
	public TypeSpec generateVariant(JavaWriterContext context,String typeName, IDLType idlType) throws IOException
	{
		String className = this.normalizeClassName(typeName);
		
		TypeSpec.Builder typeBuilder = TypeSpec.enumBuilder(className).addModifiers(Modifier.PUBLIC);
		
		Map<Label, IDLType> types = idlType.getTypeMap();
		
		Set<Label> labels = types.keySet();
		
		for(Label label : labels)
		{
			String name = label.toString();
			if(name != null)
			{
				typeBuilder.addEnumConstant(name);
				
				IDLType fieldType = types.get(label);
						
				String fieldName = name + "Value";
				
				fieldName = this.normalizeVarName(fieldName);
				
				if(name.equals("void"))
					fieldName = "voidField";
				if(name.equals("record"))
					fieldName = "recordField";				
				
				if(fieldType != null)					
				{					
					Type type = fieldType.getType();					

					this.setTypeName(context, fieldType, typeName + this.normalizeClassName(fieldName));
					
					TypeName fieldTypeName = this.toTypeName(fieldType, context.packageName, false);
					
					if(type == Type.OPT || type == Type.VEC)	
						type = fieldType.getInnerType().getType();
																
					FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldTypeName, fieldName)
							.addAnnotation(AnnotationSpec.builder(Name.class).addMember("value", "$S", name).build())
							.addAnnotation(AnnotationSpec.builder(Field.class).addMember("value", "$T.$L",Type.class, type).build())
							.addModifiers(Modifier.PUBLIC);
	
					
					typeBuilder.addField(fieldBuilder.build());
				}
			}
		}
		
		return typeBuilder.build();
	}
	
	TypeName toTypeName(IDLType idlType, String packageName, boolean isFuture)
	{

		TypeName typeName = ClassName.get(Object.class);

		if(idlType.getType().isPrimitive() || idlType.getType() == Type.FUNC || idlType.getType() == Type.SERVICE )
				typeName = ClassName.get(this.toTypeClass(idlType.getType()));

		else if(idlType.getType() == Type.VARIANT || idlType.getType() == Type.RECORD)
				typeName = ClassName.get(packageName, this.normalizeClassName(idlType.getName()) );
		else if(idlType.getType() == Type.OPT)
		{
			IDLType innerIdlType = idlType.getInnerType();
			
			TypeName innerTypeName = this.toTypeName(innerIdlType, packageName, false);
			
			ClassName optional = ClassName.get("java.util", "Optional");
			typeName = ParameterizedTypeName.get(optional, innerTypeName);
		}
		else if(idlType.getType() == Type.VEC)
		{
			IDLType innerIdlType = idlType.getInnerType();
			
			TypeName innerTypeName = this.toTypeName(innerIdlType, packageName, false);

			if(this.useList)
			{
				ClassName list = ClassName.get("java.util", "List");
				typeName = ParameterizedTypeName.get(list, innerTypeName);
			}
			else
				typeName = ArrayTypeName.of(innerTypeName);			
		}

		if(isFuture && this.useFuture)
		{
			ClassName future = ClassName.get("java.util.concurrent", "CompletableFuture");
			TypeName futureTypeName = ParameterizedTypeName.get(future, typeName);
			
			return futureTypeName;
		}
		else
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
				return BigInteger.class;
			case INT8:
				return Byte.class;
			case INT16:
				return Short.class;
			case INT32:
				return Integer.class;
			case INT64:
				return Long.class;
			case NAT:
				return BigInteger.class;
			case NAT8:
				return Byte.class;
			case NAT16:
				return Short.class;
			case NAT32:
				return Integer.class;
			case NAT64:
				return Long.class;
			case FLOAT32:
				return Float.class;
			case FLOAT64:
				return Double.class;
			case TEXT:
				return String.class;
			case RESERVED:
				return Object.class;
			case EMPTY:
				return Void.class;				
			case PRINCIPAL:
				return Principal.class;
			case FUNC:
				return Func.class;
			case SERVICE:
				return Service.class;				
			default:
				throw ParserError.create(ParserError.ParserErrorCode.CUSTOM, String.format("Unknown type %s", type));
			}
					
	}
	
	void setTypeName(JavaWriterContext context, IDLType idlType, String name) throws IOException
	{
		if(idlType.getType() == Type.OPT || idlType.getType() == Type.VEC)
			this.setTypeName(context, idlType.getInnerType(), name);
		else	
		{
			if(idlType.getName() == null)
			{	
				idlType.setName(name);		
				this.generateType(context, idlType);
			}
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
		if(name == null)
			return name;
		
		if(name.equals("void"))
			name = "voidField";
		if(name.equals("record"))
			name = "recordField";		
		
		if (!name.matches("^[a-zA-Z][a-zA-Z0-9]*?$")) 
			 name = "field" + name;
	
		
		name = name.replaceFirst(name.substring(0, 1), name.substring(0, 1).toLowerCase());
		
		return name;
	}
	
	String normalizeMethodName(String name)
	{
		if(name == null)
			return name;
		
		name = name.replaceFirst(name.substring(0, 1), name.substring(0, 1).toLowerCase());
		
		return name;
	}	
}
