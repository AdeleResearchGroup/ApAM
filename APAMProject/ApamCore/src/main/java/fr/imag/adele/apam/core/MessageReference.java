package fr.imag.adele.apam.core;

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
    public Type getType() {
    	return Type.MESSAGE;
	}   

    @Override
    public String toString() {
        return "message " + getIdentifier();
    }
}
