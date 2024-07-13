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
	public boolean useList = false;
	public boolean useFuture = true;
	
	public void write(SpringWriterContext springWriterContext, Path path, String serviceName,  String proxyName, Map<String,IDLType> types, Map<String,IDLType> services) throws IOException
	{				
		super.write(springWriterContext, path, proxyName, types, services);
		
		SpringWriter springWriter = new SpringWriter();
		
		Set<String> keys = springWriterContext.services.keySet(); 
		
		for(IDLType idlType : services.values())
		{
			springWriter.generateService(springWriterContext,serviceName, proxyName, idlType);		
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
		
		serviceBuilder.addSuperinterface(ClassName.get(context.packageName, proxyName));
		
		MethodSpec.Builder initMethodBuilder = MethodSpec.methodBuilder("init")
				.addModifiers(Modifier.PUBLIC);
		
		initMethodBuilder.addException(IOException.class);
		initMethodBuilder.addException(URISyntaxException.class);
		
		initMethodBuilder.addAnnotation(ClassName.get("jakarta.annotation","PostConstruct"));
		
//		initMethodBuilder.addAnnotation(AnnotationSpec.builder(PostConstruct.class).build());
		
		initMethodBuilder.addStatement("super.init($N ,null, null, null, null)",ClassName.get(context.packageName, proxyName) + ".class");
		
		serviceBuilder.addMethod(initMethodBuilder.build());

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
						
						args += "," + argName;
						
						this.setTypeName(context, argType, this.normalizeClassName(funcName) + this.normalizeClassName(argName));
						
						TypeName typeName = this.toTypeName(argType, context.packageName, false);
						
						Type type = argType.getType();
						
						if(type == Type.OPT || type == Type.VEC)
							type = argType.getInnerType().getType();
						
						methodBuilder.addParameter(ParameterSpec.builder(typeName, argName).build());
					}
				}
				
				if(!methType.rets.isEmpty())
				{
					IDLType retType = methType.rets.get(0);
					
					this.setTypeName(context, retType, this.normalizeClassName(funcName) + "Response");
				
					TypeName typeName = this.toTypeName(retType, context.packageName, isFuture);
					
					methodBuilder.returns(typeName);
					
					if(isFuture)
						methodBuilder.addAnnotation(AnnotationSpec.builder(Async.class).build());
					
					methodBuilder.addStatement("return this.call(\"$N\"" + args + ")",funcName);
				}
				else if(isFuture && this.useFuture)
				{
					ClassName future = ClassName.get("java.util.concurrent", "CompletableFuture");
					TypeName futureTypeName = ParameterizedTypeName.get(future, ClassName.get(Void.class));
					methodBuilder.returns(futureTypeName);
					methodBuilder.addAnnotation(AnnotationSpec.builder(Async.class).build());
					
					methodBuilder.addStatement("return this.call(\"$N\"" + args + ")",funcName);
				}
				else
					methodBuilder.addStatement("this.call(\"$N\"" + args + ")",funcName);
				
				
				
				serviceBuilder.addMethod(methodBuilder.build());
			}
		}
		
		TypeSpec serviceSpec = serviceBuilder.build();
		
		JavaFile javaFile = JavaFile.builder(context.packageName, serviceSpec)
			    .build();
		
		context.services.put(serviceName, javaFile);

	}
}
