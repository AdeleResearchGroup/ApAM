/**
 * Copyright 2011-2013 Universite Joseph Fourier, LIG, ADELE team
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
 *
 * FilterCheckHelpers.java - 7 nov. 2013
 */
package fr.imag.adele.apam.apammavenplugin.helpers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.apammavenplugin.CheckObr;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Attribute;

/**
 * @author thibaud
 * 
 */
public final class FilterCheckHelpers {
    
    private static Logger logger = LoggerFactory.getLogger(CheckObr.class);
    

    /**
     * @param filt
     * @param component
     * @param validAttr
     * @param f
     * @param spec
     * @return
     */
    public static boolean checkFilterOR(ApamFilter filt,
	    ComponentDeclaration component, Map<String, String> validAttr,
	    String f, String spec) {
	ApamFilter[] filters = (ApamFilter[]) filt.value;
	for (ApamFilter filter : filters) {
	    if (!CheckObr.checkFilter(filter, component, validAttr, f, spec)) {
		return false;
	    }
	}
	return true;
    }
    
    
    /**
     * @param filt
     * @param component
     * @param validAttr
     * @param f
     * @param spec
     */
    public static boolean checkFilterPRESENT(ApamFilter filt,
	    ComponentDeclaration component, Map<String, String> validAttr,
	    String f, String spec) {
	if (!Attribute.isFinalAttribute(filt.attr)
	    && !validAttr.containsKey(filt.attr)) {
	logger.error("Members of component " + spec
		+ " cannot have property " + filt.attr
		+ ". Invalid constraint " + f);
	return false;
	}
	if (validAttr.containsKey(filt.attr)) {
	if (CheckObr.isSubstitute(component, filt.attr)) {
	    CheckObr.error("Filter attribute  " + filt.attr
		    + " is a substitution: .  Invalid constraint " + f);
	    return false;
	}

	if (Attribute.checkAttrType(filt.attr, filt.value,
		validAttr.get(filt.attr)) == null) {
	    return false;
	}
	return CheckObr.checkSubstitute(component, filt.attr,
		validAttr.get(filt.attr), (String) filt.value);

	}
	return true;
    }
    

}
