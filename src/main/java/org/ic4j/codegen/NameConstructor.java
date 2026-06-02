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
				Package javaPackage = javaType.getPackage();

				// Synthetic or anonymous classes may have no package metadata.
				if(javaPackage == null || javaPackage.getName() == null || javaPackage.getName().isEmpty())
					return (name != null) ? name : javaType.getSimpleName();

				String packageName = javaPackage.getName();
				String[] parts = packageName.split("\\.");
				if(parts.length == 0)
					return (name != null) ? name : javaType.getSimpleName();

				// ignore internal Java classes
				if("java".equals(parts[0]) || "javax".equals(parts[0]))
					return (name != null) ? name : javaType.getSimpleName();	
				
				name = javaType.getSimpleName();
				
				if(this.hasPrefix)
				{
					int prefixIndex = parts.length - this.prefixId;
					if(prefixIndex >= 0 && prefixIndex < parts.length)
					{
						String part = parts[prefixIndex];
						if(!part.isEmpty())
						{
							String prefix  = part.substring(0, 1).toUpperCase() + part.substring(1);
							name = prefix + name;
						}
					}
				}
				
				if(this.hasPostfix)
				{
					int postfixIndex = parts.length - this.postfixId;
					if(postfixIndex >= 0 && postfixIndex < parts.length)
					{
						String part = parts[postfixIndex];
						if(!part.isEmpty())
						{
							String postfix  = part.substring(0, 1).toUpperCase() + part.substring(1);
							name = name + postfix;
						}
					}					
				}
			}
		}

		return name;
	}

}
