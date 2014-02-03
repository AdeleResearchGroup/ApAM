package fr.liglab.adele.apam.device.access;

public interface Lock {
	
	/**
	 * Activate access control
	 */
	public void enableAcces();

	/**
	 * Deactivate access control and lock/unlock the door to
	 * everyone
	 */
	public void disableAcces(boolean lock);

}
