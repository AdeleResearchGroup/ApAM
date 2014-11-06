package fr.imag.adele.apam.maven.plugin.validation.property;

import org.osgi.framework.Version;

/**
 * The basic primitive property type
 * 
 * @author vega
 *
 */
public enum PrimitiveType implements Type {

	STRING {
		
		@Override
		public String getName() {
			return "string";
		}

		@Override
		public boolean isValue(Object value) {
			return value instanceof String;
		}

		@Override
		public Object value(String value) {
			return value;
		}

		@Override
		public String toString(Object value) {
			return (String) value;
		}

		@Override
		public boolean isAssignableTo(String className) {
			return 	Mapping.isAssignableFrom(className, String.class);
		}

		@Override
		public boolean isAssignableFrom(String className) {
			return 	Mapping.isAssignableFrom(String.class,className);
		}
	},
	
	INTEGER {
		
		@Override
		public String getName() {
			return "integer";
		}

		@Override
		public boolean isValue(Object value) {
			return value instanceof Integer;
		}

		@Override
		public Object value(String value) {
			try {
				return Integer.valueOf(value);
			}
			catch (NumberFormatException invalidValue) {
				return null;
			}
		}

		@Override
		public String toString(Object value) {
			return ((Integer)value).toString();
		}

		@Override
		public boolean isAssignableTo(String className) {
			return 	Mapping.isAssignableFrom(className, Integer.TYPE) ||
					Mapping.isAssignableFrom(className, Integer.class);
		}

		@Override
		public boolean isAssignableFrom(String className) {
			return 	Mapping.isAssignableFrom(Integer.TYPE,className) ||
					Mapping.isAssignableFrom(Integer.class,className);
		}
		
	},

	FLOAT {
		
		@Override
		public String getName() {
			return "float";
		}

		@Override
		public boolean isValue(Object value) {
			return value instanceof Float;
		}

		@Override
		public Object value(String value) {
			try {
				return Float.valueOf(value);
			}
			catch (NumberFormatException invalidValue) {
				return null;
			}
		}

		@Override
		public String toString(Object value) {
			return ((Float)value).toString();
		}
		
		@Override
		public boolean isAssignableTo(String className) {
			return 	Mapping.isAssignableFrom(className, Float.TYPE) ||
					Mapping.isAssignableFrom(className, Float.class);
		}

		@Override
		public boolean isAssignableFrom(String className) {
			return 	Mapping.isAssignableFrom(Float.TYPE,className) ||
					Mapping.isAssignableFrom(Float.class,className);
		}
		
	},
	
	BOOLEAN {
		
		@Override
		public String getName() {
			return "boolean";
		}

		@Override
		public boolean isValue(Object value) {
			return value instanceof Boolean;
		}

		@Override
		public Object value(String value) {
			return Boolean.valueOf(value);
		}

		@Override
		public String toString(Object value) {
			return ((Boolean)value).toString();
		}
		
		@Override
		public boolean isAssignableTo(String className) {
			return 	Mapping.isAssignableFrom(className, Boolean.TYPE) ||
					Mapping.isAssignableFrom(className, Boolean.class);
		}

		@Override
		public boolean isAssignableFrom(String className) {
			return 	Mapping.isAssignableFrom(Boolean.TYPE,className) ||
					Mapping.isAssignableFrom(Boolean.class,className);
		}
		
	},
	
	VERSION {
		
		@Override
		public String getName() {
			return "version";
		}

		@Override
		public boolean isValue(Object value) {
			return value instanceof Version;
		}

		@Override
		public Object value(String value) {
			try {
				return Version.parseVersion(value);
			}
			catch (IllegalArgumentException invalidValue) {
				return null;
			}
		}

		@Override
		public String toString(Object value) {
			return ((Version)value).toString();
		}
		
		@Override
		public boolean isAssignableTo(String className) {
			return 	Mapping.isAssignableFrom(className, Version.class);
		}

		@Override
		public boolean isAssignableFrom(String className) {
			return 	Mapping.isAssignableFrom(Version.class,className);
		}
		
	};

	@Override
	public String toString() {
		return getName();
	}

	
	/**
	 * Primitive types are only assignable to itself and Type.ANY
	 */
	@Override
	public boolean isAssignableTo(Type type) {
		return this.equals(type) || type == Type.ANY;
	}
}
