package fr.imag.adele.apam.declarations;


/**
 * This interface allow different tools that manipulate component declarations 
 * to report errors and debug information
 * 
 */
public interface Reporter {

	public enum Severity {
		INFO,SUSPECT, WARNING, ERROR;
	}

	/**
	 * Notifies of an message in processing
	 */
	public void report(Severity severity, String message);
}