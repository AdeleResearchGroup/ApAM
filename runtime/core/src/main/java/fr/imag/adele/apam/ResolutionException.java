package fr.imag.adele.apam;

/**
 * The exception that is thrown to signal error s in resolution when the failure policy is
 * MissingPolicy.EXCEPTION
 * 
 * @author vega
 *
 */
public class ResolutionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResolutionException() {
	}
	
	public ResolutionException(String message) {
		super(message);
	}
}
