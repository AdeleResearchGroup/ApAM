package fr.imag.adele.apam.declarations;

/**
 * A reference to a produced or consumed message type
 * 
 * @author vega
 *
 */
public class MessageReference extends ResourceReference {

  
    
    public MessageReference(String name) {
        super(name);       
    }

   
    @Override
    public String toString() {
        return "message " + getIdentifier();
    }

}
