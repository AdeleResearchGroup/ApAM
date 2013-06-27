package fr.liglab.adele.apam.device.access;

public interface Lock {

	public void lock();
	
	public void unlock();
	
	public boolean isLocked();
}
