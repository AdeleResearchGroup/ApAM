package fr.liglab.adele.apam.device.access;

public interface Lock {
	
	/**
	 * Activate card based access authorization
	 */
	public void enableAuthorization();

	/**
	 * Deactivate access authorization and lock/unlock the door to
	 * everyone
	 */
	public void disableAuthorization(boolean lock);

}
