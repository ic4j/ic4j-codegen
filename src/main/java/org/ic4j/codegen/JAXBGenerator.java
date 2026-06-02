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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public abstract class JAXBGenerator {
	static Logger LOG = LoggerFactory.getLogger(JAXBGenerator.class);
	TypeWriter typeWriter;
	
	
	public JAXBGenerator(TypeWriter typeWriter) {
		super();
		this.typeWriter = typeWriter;
	}

	public void writeTypes(String dictionaryFileName, String outDir)
	{
		Document xmlDocument;
		try {
			xmlDocument = getDocument(dictionaryFileName);
			final long timeoutMs = Long.getLong("ic4j.codegen.writeTypeTimeoutMs", 0L);
			final Set<String> skippedClasses = this.getSkippedClasses();
			final boolean traceGraph = Boolean.getBoolean("ic4j.codegen.traceTypeGraph");
			final int traceDepth = Integer.getInteger("ic4j.codegen.traceTypeGraphDepth", 4);
			
			XPath xPath = XPathFactory.newInstance().newXPath();
			String messageExpression = "/messages/message";
			NodeList messageElements = (NodeList) xPath.compile(messageExpression).evaluate(xmlDocument, XPathConstants.NODESET);
			
			for(int i = 0; i < messageElements.getLength(); i++ )
			{
				Element messageElement = (Element) messageElements.item(i);
				
				String className = (messageElement.hasAttribute("class")) ? messageElement.getAttribute("class") : messageElement.getAttribute("name");
				
				className = messageElement.getAttribute("package") + "." + className;

				if(skippedClasses.contains(className))
				{
					LOG.warn(String.format("[%d/%d] Skipping configured class %s", i + 1, messageElements.getLength(), className));
					continue;
				}
				
				String typeOutDir = outDir + File.separatorChar + messageElement.getAttribute("type").toLowerCase();
				
				String typeFileName = messageElement.getAttribute("name") + "V" + messageElement.getAttribute("version") + "Types." + this.typeWriter.getExtension();
				LOG.info(String.format("[%d/%d] Generating %s -> %s", i + 1, messageElements.getLength(), className, typeFileName));
				long start = System.nanoTime();
				try {
					Class<?> typeClass = Class.forName(className);
					if(traceGraph)
						this.logTypeGraph(typeClass, traceDepth);
					if(timeoutMs > 0)
						this.writeTypeWithTimeout(typeClass, typeOutDir, typeFileName, timeoutMs);
					else
						this.writeType(typeClass, typeOutDir, typeFileName);
					long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
					LOG.info(String.format("[%d/%d] Generated %s in %d ms", i + 1, messageElements.getLength(), typeFileName, elapsed));
				} catch (StackOverflowError e) {
					LOG.error(String.format("Skipped recursive type graph for %s while generating %s", className, typeFileName));
				} catch (ClassNotFoundException e) {
					LOG.error(String.format("Failed to load type class %s for %s", className, typeFileName), e);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					LOG.error(String.format("Timed out or interrupted while generating %s into %s", className, typeFileName), e);
				} catch (RuntimeException e) {
					LOG.error(String.format("Failed to generate type for %s into %s", className, typeFileName), e);
				}			
			}			
						
		} catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
			LOG.error(e.getLocalizedMessage(), e);
		}  	
	}
	
	public abstract void writeType(Class<?> type, String outDir, String fileName);

	void writeTypeWithTimeout(final Class<?> type, final String outDir, final String fileName, final long timeoutMs)
			throws InterruptedException, ExecutionException, TimeoutException
	{
		ThreadFactory threadFactory = runnable -> {
			Thread thread = new Thread(runnable, "jaxb-write-type");
			thread.setDaemon(true);
			return thread;
		};

		ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
		try {
			Callable<Void> task = () -> {
				this.writeType(type, outDir, fileName);
				return null;
			};
			Future<Void> future = executor.submit(task);
			future.get(timeoutMs, TimeUnit.MILLISECONDS);
		} finally {
			executor.shutdownNow();
		}
	}

	Set<String> getSkippedClasses()
	{
		String value = System.getProperty("ic4j.codegen.skipClasses", "");
		if(value == null || value.trim().isEmpty())
			return Collections.emptySet();

		Set<String> result = new HashSet<>();
		for(String token : value.split(","))
		{
			String className = token.trim();
			if(!className.isEmpty())
				result.add(className);
		}
		return result;
	}

	
    static Document getDocument(String fileName) throws SAXException, IOException, ParserConfigurationException
    {
    	FileInputStream fileInputStream = new FileInputStream(fileName);
    	DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = builderFactory.newDocumentBuilder();
    	Document xmlDocument = builder.parse(fileInputStream);
		
    	return xmlDocument;
    }
	void logTypeGraph(Class<?> rootType, int maxDepth)
	{
		Map<Class<?>, Integer> depths = new HashMap<>();
		Map<Class<?>, Integer> fanOut = new LinkedHashMap<>();
		Set<Class<?>> cycles = new LinkedHashSet<>();
		Deque<Class<?>> path = new ArrayDeque<>();
		this.walkTypeGraph(rootType, 0, maxDepth, depths, fanOut, cycles, path);

		LOG.info(String.format("Type graph summary for %s: uniqueTypes=%d, cycles=%d, topFanOut=%s",
				rootType.getName(), depths.size(), cycles.size(), this.formatTopFanOut(fanOut, 8)));
		this.logHotBranches(rootType, Math.max(1, maxDepth - 1));
		if(!cycles.isEmpty())
			LOG.info(String.format("Type graph cycles for %s: %s", rootType.getName(), cycles));
	}

	void logHotBranches(Class<?> rootType, int maxDepth)
	{
		List<Class<?>> children = this.getReferencedTypes(rootType);
		List<String> branchSummaries = new java.util.ArrayList<>();
		for(Class<?> child : children)
		{
			String hotPath = this.describeHotPath(child, maxDepth - 1, new LinkedHashSet<>());
			int childFanOut = this.getReferencedTypes(child).size();
			branchSummaries.add(String.format("%s (fanOut=%d)", hotPath, childFanOut));
		}
		LOG.info(String.format("Hot branches for %s: %s", rootType.getSimpleName(), String.join(" | ", branchSummaries)));
	}

	String describeHotPath(Class<?> type, int remainingDepth, Set<Class<?>> path)
	{
		if(type == null)
			return "";

		path.add(type);
		String current = type.getSimpleName();
		if(remainingDepth <= 0)
			return current;

		Class<?> bestChild = null;
		int bestFanOut = -1;
		for(Class<?> child : this.getReferencedTypes(type))
		{
			if(path.contains(child))
				continue;

			int childFanOut = this.getReferencedTypes(child).size();
			if(childFanOut > bestFanOut)
			{
				bestFanOut = childFanOut;
				bestChild = child;
			}
		}

		if(bestChild == null)
			return current;

		return current + " -> " + this.describeHotPath(bestChild, remainingDepth - 1, path);
	}

	void walkTypeGraph(Class<?> type, int depth, int maxDepth, Map<Class<?>, Integer> depths,
			Map<Class<?>, Integer> fanOut, Set<Class<?>> cycles, Deque<Class<?>> path)
	{
		if(type == null || depth > maxDepth)
			return;

		Integer seenDepth = depths.get(type);
		if(seenDepth != null && seenDepth <= depth)
			return;

		depths.put(type, depth);
		path.push(type);
		try {
			List<Class<?>> children = this.getReferencedTypes(type);
			fanOut.put(type, children.size());
			for(Class<?> child : children)
			{
				if(path.contains(child))
				{
					cycles.add(child);
					continue;
				}
				this.walkTypeGraph(child, depth + 1, maxDepth, depths, fanOut, cycles, path);
			}
		} finally {
			path.pop();
		}
	}

	List<Class<?>> getReferencedTypes(Class<?> type)
	{
		Set<Class<?>> result = new LinkedHashSet<>();
		for(Field field : type.getDeclaredFields())
		{
			if(Modifier.isStatic(field.getModifiers()))
				continue;

			Class<?> fieldType = field.getType();
			if(this.isLikelyGraphType(fieldType))
				result.add(fieldType);

			Type genericType = field.getGenericType();
			if(genericType instanceof ParameterizedType)
			{
				for(Type arg : ((ParameterizedType) genericType).getActualTypeArguments())
				{
					if(arg instanceof Class)
					{
						Class<?> argClass = (Class<?>) arg;
						if(this.isLikelyGraphType(argClass))
							result.add(argClass);
					}
				}
			}
		}
		return new java.util.ArrayList<>(result);
	}

	boolean isLikelyGraphType(Class<?> type)
	{
		if(type == null)
			return false;
		if(type.isPrimitive() || type.isEnum() || type.isArray())
			return false;
		String packageName = type.getPackage() != null ? type.getPackage().getName() : "";
		for(String ignoredPrefix : new String[] {"java.", "javax.", "jakarta.", "org.w3c.", "org.xml.", "com.sun."})
			if(packageName.startsWith(ignoredPrefix))
				return false;
		return true;
	}

	String formatTopFanOut(Map<Class<?>, Integer> fanOut, int limit)
	{
		StringBuilder builder = new StringBuilder();
		int index = 0;
		for(Map.Entry<Class<?>, Integer> entry : fanOut.entrySet())
		{
			if(index >= limit)
				break;
			if(builder.length() > 0)
				builder.append(", ");
			builder.append(entry.getKey().getSimpleName()).append("=").append(entry.getValue());
			index++;
		}
		return builder.toString();
	}

}
