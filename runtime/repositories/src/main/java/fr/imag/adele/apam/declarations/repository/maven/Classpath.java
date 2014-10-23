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
package fr.imag.adele.apam.declarations.repository.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class represents a build-time classpath. 
 * 
 * NOTE Currently we avoid to load classes at build-time, as it may require to simulate much of the behavior of the
 * OSGi runtime.  For this reason, the only supported functionality right now is to decide if class exists in the
 * classpath
 *  
 * @author vega
 *
 */
public class Classpath {
	
	public interface Entry {
		
		/**
		 * Whether the class is defined in this classpath entry
		 */
		public boolean contains(String fullyQualifiedClassName);
	}
	
	private final List<Entry> entries;
	
	public Classpath(Entry ... initialEntries) {
		this(initialEntries != null ? Arrays.asList(initialEntries) : Collections.<Entry>emptyList());
	}		

		public Classpath(List<Entry> initialEntries) {
		this.entries = new ArrayList<Classpath.Entry>();
		
		for (Entry entry : initialEntries) {
			add(entry);
		}
	}
	
	/**
	 * Adds a new entry at the end of this classpath
	 * @param entry
	 */
	public void add(Entry entry) {
		entries.add(entry);
	}
	
	/**
	 * Whether the class is defined in this classpath entry
	 */
	public boolean contains(String fullyQualifiedClassName) {
		
		for (Entry entry : entries) {
			if (entry.contains(fullyQualifiedClassName))
				return true;
		}
		
		/*
		 * try the boot class loader to see if it is a system class
		 */
		try {
			Class.forName(fullyQualifiedClassName,false,ClassLoader.getSystemClassLoader());
			return true;
			
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

}