/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.util;

import org.apache.felix.ipojo.parser.MethodMetadata;

import fr.imag.adele.apam.declarations.MessageReference;

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
