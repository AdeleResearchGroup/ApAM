package fr.imag.adele.apam.core;


public class ContextualResolutionPolicy extends ConstrainedReference {

    /**
     * Whether a dependency matching this policy must be eagerly resolved
     */
    private final boolean					isEager;
    
    /**
     * Whether a resolution error must trigger a backtrack in the architecture
     */

    private final boolean 					mustHide;
    
    /**
     * The exception to throw in case of resolution error
     * 
     */
    private String							missingException;
    
  	public ContextualResolutionPolicy(ComponentReference<?> resource, boolean isEager, boolean mustHide) {
		super(resource);

        this.isEager			= isEager;
        this.mustHide			= mustHide;
        this.missingException	= null;
  	}

  	@Override
  	public ComponentReference<?> getTarget() {
  		return (ComponentReference<?>) super.getTarget();
  	}
    /**
     * Whether dependencies matching this contextual policy must be resolved eagerly
     */
  	public boolean isEager() {
		return isEager;
	}
  	
    /**
     * Whether an error resolving a dependency matching this policy should trigger a backtrack
     * in resolution
     */
  	public boolean mustHide() {
		return mustHide;
	}

    /**
     * Get the exception associated with the missing policy
     */
    public String getMissingException() {
        return missingException;
    }

    /**
     * Set the missing exception used for this dependency
     */
    public void setMissingExeception(String missingException) {
        this.missingException = missingException;
    }    
  	
}
