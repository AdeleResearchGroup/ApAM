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

import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apform.ApformCompositeType;
import fr.imag.adele.apam.declarations.CompositeDeclaration;

public class ApformCompositeTypeImpl extends ApformImplementationImpl implements ApformCompositeType {

   
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
    public ApformCompositeTypeImpl(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);

        @SuppressWarnings("unchecked")
        Enumeration<String> paths = context.getBundle().getEntryPaths("/");
        while (paths.hasMoreElements()) {
        	
 				String modelFileName = paths.nextElement();
 				
 				if (! modelFileName.endsWith(".cfg"))
 					continue;
 				
 				if (! modelFileName.startsWith(getDeclaration().getName()))
 					continue;
 				
	            String managerName = modelFileName.substring(getDeclaration().getName().length()+1, modelFileName.lastIndexOf(".cfg"));
	            URL modelURL = context.getBundle().getEntry(modelFileName);
				managerModels.add(new ManagerModel(managerName, modelURL));
        }
        
    }

    @Override
	public void check(Element element) throws ConfigurationException {
    	super.check(element);
	}
	
    @Override
    public CompositeDeclaration getDeclaration() {
    	return (CompositeDeclaration) super.getDeclaration();
    };
    /**
     * This factory doesn't have an associated instrumented class
     */
    @Override
    public boolean hasInstrumentedCode() {
    	return false;
    }
    
    /**
     * Gets the class name.
     * 
     * @return the class name.
     * @see org.apache.felix.ipojo.IPojoFactory#getClassName()
     */
    @Override
    public String getClassName() {
        return getDeclaration().getName();
    }

    /**
     * Get The list of models associated to this composite
     */
    public Set<ManagerModel> getModels() {
        return managerModels;
    }

}
