package fr.imag.adele.apam.util;

import java.util.Set;

import fr.imag.adele.apam.core.ComponentDeclaration;

/**
 * This class represents a tool being able to parse one of the different APAM Core representations
 * 
 * @author vega
 *
 */
public interface CoreParser {

	/**
	 * Get the list of all the declared components
	 */
	public Set<ComponentDeclaration> getDeclarations(ErrorHandler errorHandler);

	/**
	 * This interface allow parser users to be notified of all errors found during parsing
	 */
	public interface ErrorHandler {
		
		public enum Severity { SUSPECT, WARNING, ERROR;}
		
		/**
		 * Notifies of an error in a declaration
		 */
		public void error(Severity severity, String message);
	}

	
}
