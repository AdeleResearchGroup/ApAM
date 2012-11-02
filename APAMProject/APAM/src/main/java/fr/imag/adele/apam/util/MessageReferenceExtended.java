package fr.imag.adele.apam.util;

import org.apache.felix.ipojo.parser.MethodMetadata;

import fr.imag.adele.apam.core.MessageReference;

public class MessageReferenceExtended extends MessageReference {

    private final boolean messageApAMType;
    private MethodMetadata methodMetadata;
    private boolean resourceUndefined=false;
    
    public MessageReferenceExtended(String name) {
        this(name,false);
    }
        
    public MessageReferenceExtended(String name, boolean messageApAMType) {
        super(name);
        this.messageApAMType = messageApAMType;
    }

    public boolean isMessageApAMType() {
        return messageApAMType;
    }

    protected void setCallbackMetadata(MethodMetadata methodMetadata ){
        this.methodMetadata = methodMetadata;
    }
    
    public MethodMetadata getCallbackMetadata() {        
        return methodMetadata;
    }

    public boolean isResourceUndefined(){
        return resourceUndefined;
    }
    
    protected void setResourceUndefined(boolean resourceUndifined) {
       this.resourceUndefined =  resourceUndifined;
    }
   
}
