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
package fr.imag.adele.apam.declarations;

public class UndefinedReference extends ResourceReference {

    private final ResourceReference subject;
    private final Class<? extends ResourceReference> kind;

    public UndefinedReference(ResourceReference subject){
        super("<Unavailable type for "+subject.getClass().getSimpleName()+" "+subject.getName()+">");
        this.subject = subject;
        this.kind= subject.getClass();
    }
    
    public boolean isKind(Class<? extends ResourceReference> kind) {
        return kind.isAssignableFrom(this.kind);
    }
    
   
    @Override
    public <R extends Reference> R as(Class<R> kind) {
    	return subject.as(kind);
    }
    public String getSubject(){
        return subject.getName();
    }
       
    public String toString() {
        return "resource UNKNOWN TYPE "+subject;
    }
}
