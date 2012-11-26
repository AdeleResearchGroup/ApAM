package fr.imag.adele.apam.core;

/**
 * This class represents the visibility rules associated to the content management of
 * composites
 * 
 * @author vega
 *
 */
public class VisibilityDeclaration {

	
	/**
	 * The borrow content
	 */
	private String borrowImplementations;
	
	private String borrowInstances;
	
	/**
	 * The friend imported content
	 */
	private String friendImplementations;
	
	private String friendInstances;
	
	/**
	 * The local content
	 */
	private String localImplementations;
	
	private String localInstances;
	
	/**
	 * The application content
	 */
	private String applicationInstances;

	public VisibilityDeclaration() {
	}
	

	/**
	 * An expression that must be satisfied by all imported implementations 
	 */
	public String getBorrowImplementations() {
		return borrowImplementations;
	}

	public void setBorrowImplementations(String borrowImplementations) {
		this.borrowImplementations = borrowImplementations;
	}

	/**
	 * An expression that must be satisfied by all imported instances 
	 */
	public String getBorrowInstances() {
		return borrowInstances;
	}
	
	public void setBorrowInstances(String borrowInstances) {
		this.borrowInstances = borrowInstances;
	}

	/**
	 * An expression that must be satisfied by all exported implementations that
	 * are available for friend composites 
	 */
	public String getFriendImplementations() {
		return friendImplementations;
	}
	public void setFriendImplementations(String friendImplementations) {
		this.friendImplementations = friendImplementations;
	}

	/**
	 * An expression that must be satisfied by all exported instances that
	 * are available for friend composites 
	 */
	public String getFriendInstances() {
		return friendInstances;
	}

	public void setFriendInstances(String friendInstances) {
		this.friendInstances = friendInstances;
	}

	/**
	 * An expression that must be satisfied by all implementations that
	 * are not available for exporting 
	 */
	public String getLocalImplementations() {
		return localImplementations;
	}

	public void setLocalImplementations(String localImplementations) {
		this.localImplementations = localImplementations;
	}


	/**
	 * An expression that must be satisfied by all instances that
	 * are not available for exporting 
	 */
	public String getLocalInstances() {
		return localInstances;
	}

	public void setLocalInstances(String localInstances) {
		this.localInstances = localInstances;
	}

	/**
	 * An expression that must be satisfied by all exported instances that
	 * are available for other composites in the application 
	 */
	public String getApplicationInstances() {
		return applicationInstances;
	}
	
	public void setApplicationInstances(String applicationInstances) {
		this.applicationInstances = applicationInstances;
	}

	
}
