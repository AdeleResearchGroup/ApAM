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
package fr.imag.adele.apam.apform.impl.handlers;

import java.util.Dictionary;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.osgi.service.wireadmin.WireAdmin;

import fr.imag.adele.apam.apform.impl.ApamAtomicComponentFactory;
import fr.imag.adele.apam.apform.impl.ApamInstanceManager;
import fr.imag.adele.apam.apform.impl.RelationCallback;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.CallbackDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.RequirerInstrumentation;

public class RelationInjectionHandler extends ApformHandler {

    /**
     * The registered name of this iPojo handler
     */
    public static final String NAME = "injection";

    /**
     * The WireAdmin reference
     */
    private WireAdmin          wireAdmin;

    /**
     * Get the WireAdmin reference
     */
    public WireAdmin getWireAdmin() {
        return wireAdmin;
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {
        /*
		 * Add interceptors to delegate relation resolution
		 * 
		 * NOTE All validations were already performed when validating the
		 * factory @see initializeComponentFactory, including initializing
		 * unspecified properties with appropriate default values. Here we just
		 * assume metadata is correct.
		 */

        if (!(getFactory() instanceof ApamAtomicComponentFactory))
            return;

        ApamAtomicComponentFactory implementation = (ApamAtomicComponentFactory) getFactory();
        ImplementationDeclaration declaration = implementation.getApform().getDeclaration();

        if (!(declaration instanceof AtomicImplementationDeclaration))
            return;

        AtomicImplementationDeclaration primitive = (AtomicImplementationDeclaration) declaration;
		for (RequirerInstrumentation injection : primitive.getRequirerInstrumentation()) {

            /*
             * Get the field interceptor depending on the kind of reference
             */

            InterfaceReference interfaceReference = injection.getRequiredResource().as(InterfaceReference.class);
            MessageReference messageReference = injection.getRequiredResource().as(MessageReference.class);
            FieldInterceptor interceptor = null;
            try {

                if (interfaceReference != null)
                    interceptor = new InterfaceInjectionManager(getFactory(), getInstanceManager(), injection);

                if (messageReference != null)
                    interceptor = new MessageInjectionManager(getFactory(), getInstanceManager(), injection);

                if (interceptor == null)
                    continue;

            } catch (ClassNotFoundException error) {
				throw new ConfigurationException("error injecting relation "
						+ injection.getName() + " :"
                        + error.getLocalizedMessage());
            }

            if (injection instanceof RequirerInstrumentation.InjectedField) {
                FieldMetadata field = getPojoMetadata().getField(injection.getName());
                if (field != null)
                    getInstanceManager().register(field, interceptor);
            }
        }

        /*
         * Load callback into the ApamInstanceManager
         */
        for (RelationDeclaration relation : primitive.getDependencies()) {
        	for (RelationDeclaration.Event trigger : RelationDeclaration.Event.values()) {
        		Set<CallbackDeclaration> callbacks = relation.getCallback(trigger);
        		
        		if (callbacks == null)
        			continue;
        		
        		for (CallbackDeclaration callback : callbacks) {
        			getInstanceManager().addCallback(new RelationCallback(getInstanceManager(),relation,trigger,callback));
				}
        	}
		}
        
    }

    /**
     * The description of this handler instance
     * 
     */
    private static class Description extends HandlerDescription {

        private final RelationInjectionHandler relationHandler;

        public Description(RelationInjectionHandler relationHandler) {
            super(relationHandler);
            this.relationHandler = relationHandler;
        }

        @Override
        public Element getHandlerInfo() {
            Element root = super.getHandlerInfo();

            if (relationHandler.getInstanceManager() instanceof ApamInstanceManager) {
                ApamInstanceManager instance = relationHandler.getInstanceManager();
                for (RelationInjectionManager relation : instance.getInjections()) {
                    root.addElement(relation.getDescription());
                }
            }
            return root;
        }

    }

    @Override
    public HandlerDescription getDescription() {
        return new Description(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String toString() {
        return "APAM Injection manager for "
                + getInstanceManager().getInstanceName();
    }

}
