package fr.imag.adele.apam.declarations.encoding;


/**
 * This interface allow coder/decoder to signal errors and other messages found
 * during encoding/decoding
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