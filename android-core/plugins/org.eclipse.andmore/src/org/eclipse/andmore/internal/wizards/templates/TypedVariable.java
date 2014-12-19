package org.eclipse.andmore.internal.wizards.templates;

import java.util.Locale;

import org.xml.sax.Attributes;

public class TypedVariable {
	public enum Type {
		STRING,
		BOOLEAN,
		INTEGER;

		public static Type get(String name) {
			if (name == null) {
				return STRING;
			}
			try {
				return valueOf(name.toUpperCase(Locale.US));
			} catch (IllegalArgumentException e) {
				System.err.println("Unexpected global type '" + name + "'");
				System.err.println("Expected one of :");
				for (Type s : Type.values()) {
					System.err.println("  " + s.name().toLowerCase(Locale.US));
				}
			}

			return STRING;
		}
	}

	public static Object parseGlobal(Attributes attributes) {
		String value = attributes.getValue(TemplateHandler.ATTR_VALUE);
		Type type = Type.get(attributes.getValue(TemplateHandler.ATTR_TYPE));

		switch (type) {
		case STRING:
			return value;
		case BOOLEAN:
			return Boolean.parseBoolean(value);
		case INTEGER:
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				return value;
			}
		}

		return value;
	}
}
