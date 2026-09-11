/*
 * Copyright 2024 Exilor Inc.
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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Type;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class SpringWriter extends JavaWriter {
	public void write(SpringWriterContext springWriterContext, Path path, String serviceName,  String proxyName, Map<String,IDLType> types, Map<String,IDLType> services) throws IOException
	{				
		super.write(springWriterContext, path, proxyName, types, services);
		
		Set<String> keys = springWriterContext.services.keySet(); 
		
		for(IDLType idlType : services.values())
		{
			this.generateService(springWriterContext,serviceName, proxyName, idlType);
		}
		
		for(String key : keys)
		{
			JavaFile javaFile = springWriterContext.services.get(key);
			if(springWriterContext.verbose)
				javaFile.writeTo(System.out);
			
			javaFile.writeTo(path);	
		}
			
	}
	
	public void generateService(SpringWriterContext context,String serviceName, String proxyName, IDLType idlType) throws IOException
	{
		String className = this.normalizeClassName(serviceName);
		
		TypeSpec.Builder serviceBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
		
		serviceBuilder.addAnnotation(AnnotationSpec.builder(Service.class).build());
		serviceBuilder.addAnnotation(AnnotationSpec.builder(Configuration.class).build());
		
		serviceBuilder.superclass(ClassName.get(org.ic4j.spring.Service.class));
		
		serviceBuilder.addSuperinterface(ClassName.get(context.packageName, this.normalizeClassName(proxyName)));

		MethodSpec constructor = MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PUBLIC)
				.addParameter(ClassName.get("org.springframework.core.io", "ResourceLoader"), "resourceLoader")
				.addStatement("super(resourceLoader)")
				.build();
		serviceBuilder.addMethod(constructor);

		Map<String,IDLType> meths = idlType.getMeths();
		Set<String> names = meths.keySet();
		Map<String,String> generatedMethodNames = new java.util.HashMap<>();
		Set<String> methodNames = new java.util.HashSet<>();
		for(String name : names)
			if(name != null)
				generatedMethodNames.put(name,
						JavaIdentifier.unique(this.normalizeMethodName(name), methodNames));
		String lifecycleMethodName = JavaIdentifier.unique("initializeAgent", methodNames);
		
		MethodSpec.Builder initMethodBuilder = MethodSpec.methodBuilder(lifecycleMethodName)
				.addModifiers(Modifier.PUBLIC);
		
		initMethodBuilder.addException(IOException.class);
		initMethodBuilder.addException(URISyntaxException.class);
		
		initMethodBuilder.addAnnotation(ClassName.get("javax.annotation","PostConstruct"));
		
//		initMethodBuilder.addAnnotation(AnnotationSpec.builder(PostConstruct.class).build());
		
		initMethodBuilder.addStatement("super.init($T.class, null, null, null, null)",
				ClassName.get(context.packageName, this.normalizeClassName(proxyName)));
		
		serviceBuilder.addMethod(initMethodBuilder.build());

		for(String name : names)
		{
			if(name != null)
			{
				IDLType methType = meths.get(name);
				
				String funcName = generatedMethodNames.get(name);
				
				MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(funcName)
						.addModifiers(Modifier.PUBLIC);
				
				
				boolean isFuture = true;
				if(!methType.modes.isEmpty())
				{	
					if(methType.modes.get(0) == Mode.QUERY)
					{
						isFuture = false;
					}
					else if(methType.modes.get(0) == Mode.ONEWAY)
					{
						isFuture = false;
					}
				}
				
				String args = "";
				if(!methType.args.isEmpty())
				{
					int i = 0;

					for(IDLType argType : methType.args)
					{
						String argName = "arg" + i++;
						
						args += ",(Object) " + argName;
						
						this.setTypeName(context, argType, this.normalizeClassName(funcName) + this.normalizeClassName(argName));
						
						TypeName typeName = this.toTypeName(argType, context.packageName, false);
						
						Type type = argType.getType();
						
						if(type == Type.OPT || type == Type.VEC)
							type = argType.getInnerType().getType();
						
						methodBuilder.addParameter(ParameterSpec.builder(typeName, argName).build());
					}
				}
				
				if(methType.rets.size() > 1)
					throw new IOException("Spring generation does not support multiple return values for method " + name);
				if(!methType.rets.isEmpty())
				{
					IDLType retType = methType.rets.get(0);
					
					this.setTypeName(context, retType, this.normalizeClassName(funcName) + "Response");
				
					TypeName typeName = this.toTypeName(retType, context.packageName, isFuture);
					
					methodBuilder.returns(typeName);
					
					if(isFuture && this.useFuture)
						methodBuilder.addAnnotation(AnnotationSpec.builder(Async.class).build());
					
					methodBuilder.addStatement("return this.call($S" + args + ")",name);
				}
				else if(isFuture && this.useFuture)
				{
					ClassName future = ClassName.get("java.util.concurrent", "CompletableFuture");
					TypeName futureTypeName = ParameterizedTypeName.get(future, ClassName.get(Void.class));
					methodBuilder.returns(futureTypeName);
					methodBuilder.addAnnotation(AnnotationSpec.builder(Async.class).build());
					
					methodBuilder.addStatement("return this.call($S" + args + ")",name);
				}
				else
					methodBuilder.addStatement("this.call($S" + args + ")",name);
				
				
				
				serviceBuilder.addMethod(methodBuilder.build());
			}
		}
		
		TypeSpec serviceSpec = serviceBuilder.build();
		
		JavaFile javaFile = JavaFile.builder(context.packageName, serviceSpec)
			    .build();
		
		context.services.put(serviceName, javaFile);

	}
}
