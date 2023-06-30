package org.ic4j.codegen;

import org.ic4j.candid.parser.IDLType;

public class NameConstructor {
	public boolean convertName = false;
	public boolean hasPrefix = false;
	public boolean hasPostfix = false;
	
	public int prefixId = 0;
	public int postfixId = 0;
	
	public String constructName(IDLType idlType)
	{
		String name = idlType.getName();
		if(this.convertName)
		{
			if(idlType.getJavaType() != null)
			{
				Class<?> javaType = idlType.getJavaType();
																			
				String packageName = javaType.getPackage().getName();
				String[] parts = packageName.split("\\.");								
				
				// ignore internal Java classes
				if("java".equals(parts[0]) || "javax".equals(parts[0]))
					return name;	
				
				name = javaType.getSimpleName();
				
				if(this.hasPrefix)
				{
					String part = parts[parts.length - this.prefixId];
					String prefix  = part.substring(0, 1).toUpperCase() + part.substring(1);
					name = prefix + name;
				}
				
				if(this.hasPostfix)
				{
					String part = parts[parts.length - this.postfixId];
					String postfix  = part.substring(0, 1).toUpperCase() + part.substring(1);
					name = name + postfix;					
				}
			}
		}

		return name;
	}

}
