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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;

import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.ParserError;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;

public class MotokoWriter implements TypeWriter{
	static final String EXTENSION = "mo";
	
	public NameConstructor nameConstructor = new NameConstructor();
	
	@Override
	public String getExtension() {
		return EXTENSION;
	}	
	
	public void writeFile(Path file, IDLType type) throws IOException
	{		
		FileWriter fstream = new FileWriter (file.toFile());
		
		this.write(new BufferedWriter(fstream), type);			
	}
	
	public void write(Writer writer, IDLType type) throws IOException
	{
		Map<String, IDLType> flattenType = type.flatten();
				
		MotokoWriterContext typeWriterContext = new MotokoWriterContext();
		
		typeWriterContext.writer =  writer;		
		
		typeWriterContext.writer.write("module {"  + String.format("%n"));
		
		for(IDLType idlType : flattenType.values())
		{
			String name = idlType.getName();
			this.generateType(typeWriterContext, idlType);
		}
		
		typeWriterContext.writer.write("}");
		
		typeWriterContext.writer.close();		
	}	
	
	public void generateType(MotokoWriterContext context, IDLType type) throws IOException
	{
		String tabs = "";
		String line=  "";
		
		for(int i = 0; i < context.level;i++)
			tabs = tabs + '\t';
		
		if(type.getType() == Type.PRINCIPAL)
			context.hasPrincipal = true;
		
		if( context.level == 1)
		{
			if(type.getName() != null)
				line = "public type " + nameConstructor.constructName(type) + " = ";
			else
				return;
		}
		
		if(type.getType().isPrimitive()) {
			line = line + toTypeString(type.getType()) + String.format(";%n");
			context.writer.write(line);
		}
		if(type.getType() == Type.OPT)
		{
			line = line + "?";
			context.writer.write(line);
			
			// handle inner type
			if(type.getInnerType().getName() != null)
				context.writer.write(nameConstructor.constructName(type.getInnerType()));
			else if(type.getInnerType().getType().isPrimitive())
				context.writer.write(toTypeString(type.getInnerType().getType()));				
			else
			{
				context.level++;
				this.generateType(context, type.getInnerType());
				context.level--;
			}
			
			if(!type.getInnerType().getType().equals(Type.VEC))
				context.writer.write(String.format(";"));	
			
			//if(type.getInnerType().getType().isPrimitive())
				context.writer.write(String.format("%n"));
		}
		if(type.getType() == Type.VEC)
		{
			// handle blob
			if(type.getInnerType().getType() == Type.NAT8)
				context.writer.write(line = line + String.format("Blob;%n"));
			else
			{
				line = line + "[";
				context.writer.write(line);
				
				// handle inner type
				if(type.getInnerType().getName() != null)
					context.writer.write(nameConstructor.constructName(type.getInnerType()));
				else if(type.getInnerType().getType().isPrimitive())
					context.writer.write(toTypeString(type.getInnerType().getType()));
				else
				{
					context.level++;
					this.generateType(context, type.getInnerType());
					context.level--;
				}
				context.writer.write(String.format("];%n"));
			}
		}
		if(type.getType() == Type.RECORD)
		{
			line = tabs + line + "{" + String.format("%n");
			context.writer.write(line);
			
			for(Label label : type.getTypeMap().keySet())
			{
				context.writer.write(tabs + '\t' + label.toString() + " : " );
				IDLType subType = type.getTypeMap().get(label);
				// handle sub types
				if(subType.getName() != null)
					context.writer.write(nameConstructor.constructName(subType) + String.format(";%n"));
				else
				{
					context.level++;
					this.generateType(context, subType);
					context.level--;
				}				
			}

			context.writer.write(tabs + String.format("};%n"));			
		}
		if(type.getType() == Type.VARIANT)
		{
			line = tabs + line + "{"  + String.format("%n");
			context.writer.write(line);
			
			for(Label label : type.getTypeMap().keySet())
			{
				context.writer.write(tabs + '\t' + "#" + label.toString());
				IDLType subType = type.getTypeMap().get(label);
				
				if(subType != null)
				{
					context.writer.write(" : " );
					// handle sub types
					if(subType.getName() != null)
						context.writer.write(nameConstructor.constructName(subType) + String.format(";%n"));
					else
					{
						context.level++;
						this.generateType(context, subType);
						context.level--;
					}
				}
				else
					context.writer.write(String.format(";%n"));

			}

			context.writer.write(tabs + String.format("};%n"));			
		}
		
		if( context.level == 1)
			context.writer.write(String.format("%n"));

	}
	
	public String toTypeString(Type type)
	{

			switch (type) {
			case NULL:
				return "Null";
			case BOOL:
				return "Bool";
			case INT:
				return "Int";
			case INT8:
				return "Int8";
			case INT16:
				return "Int16";
			case INT32:
				return "Int32";
			case INT64:
				return "Int64";
			case NAT:
				return "Nat";
			case NAT8:
				return "Nat8";
			case NAT16:
				return "Nat16";
			case NAT32:
				return "Nat32";
			case NAT64:
				return "Nat64";
			case FLOAT32:
				return "Float";
			case FLOAT64:
				return "Float";
			case TEXT:
				return "Text";
			case RESERVED:
				return "Any";
			case EMPTY:
				return "Empty";				
			case PRINCIPAL:
				return "Principal";
			case FUNC:
				return "Func";
			case SERVICE:
				return "Service";				
			default:
				throw ParserError.create(ParserError.ParserErrorCode.CUSTOM, String.format("Unknown type %s", type));
			}
					
	}


}
