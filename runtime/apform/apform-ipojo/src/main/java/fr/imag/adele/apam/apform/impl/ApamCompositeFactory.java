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
package fr.imag.adele.apam.apform.impl;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apform.ApformCompositeType;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.declarations.CompositeDeclaration;

public class ApamCompositeFactory extends ApamImplementationFactory  {

   
    /**
     * The list of models associated to this composite
     */
    private final Set<ManagerModel> managerModels =  new HashSet<ManagerModel>();

    /**
     * Build a new factory with the specified metadata
     * 
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
    public ApamCompositeFactory(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);

        @SuppressWarnings("unchecked")
        Enumeration<String> paths = context.getBundle().getEntryPaths("/");
        while (paths.hasMoreElements()) {
        	
 				String modelFileName = paths.nextElement();
 				
 				if (! modelFileName.endsWith(".cfg"))
 					continue;
 				
 				if (! modelFileName.startsWith(declaration.getName()))
 					continue;
 				
	            String managerName = modelFileName.substring(declaration.getName().length()+1, modelFileName.lastIndexOf(".cfg"));
	            URL modelURL = context.getBundle().getEntry(modelFileName);
				managerModels.add(new ManagerModel(managerName, modelURL));
        }
        
    }

    /**
     * Get The list of models associated to this composite
     */
    public Set<ManagerModel> getModels() {
        return managerModels;
    }
    
    @Override
    protected ApformImplementation createApform() {
    	return this.new Apform(); 
    }
    
    private class Apform extends ApamImplementationFactory.Apform<CompositeType,CompositeDeclaration> implements ApformCompositeType {

		@Override
		public Set<ManagerModel> getModels() {
			return ApamCompositeFactory.this.getModels();
		}
    	
    };
    
    @Override
    protected final boolean hasInstrumentedCode() {
        return false;
    }

    @Override
    public String getClassName() {
        return declaration.getName();
    }


}
