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
import java.nio.file.Files;
import java.nio.file.Paths;

import org.ic4j.candid.jaxb.jakarta.JAXBUtils;
import org.ic4j.candid.parser.IDLType;



public class JakartaJAXBGenerator extends JAXBGenerator {

	public static void main(String[] args) 
	{
		if(args.length < 2) {
			System.err.println("Usage: JakartaJAXBGenerator <dictionary-file> <output-dir> [convert-name]");
			System.exit(2);
		}
		
		MotokoWriter motokoWriter =  new MotokoWriter();
		
		boolean convertName = false;
		if(args.length > 2)
			convertName = Boolean.parseBoolean(args[2]);
		
		motokoWriter.nameConstructor.convertName = convertName;
		
		motokoWriter.nameConstructor.hasPostfix = true;
		motokoWriter.nameConstructor.postfixId = 1;
		
		JAXBGenerator jaxbGenerator = new JakartaJAXBGenerator(motokoWriter );
		
		try {
			jaxbGenerator.writeTypes(args[0], args[1]);
		} catch (CodegenException e) {
			LOG.error(e.getMessage(), e);
			System.exit(1);
		}
	}
	
	public JakartaJAXBGenerator(TypeWriter typeWriter) {
		super(typeWriter);
	}	

	
	@Override
	public void writeType(Class<?> type, String outDir, String fileName) throws IOException
	{
		IDLType idlType = JAXBUtils.getIDLType(type);
		if(Files.notExists(Paths.get(outDir)))
			Files.createDirectories(Paths.get(outDir));
		typeWriter.writeFile(Paths.get(outDir,fileName), idlType);
	}
	


}
