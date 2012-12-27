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

/**
 * This class represents the visibility rules associated to the content management of
 * composites
 * 
 * @author vega
 *
 */
public class VisibilityDeclaration {

	
	/**
	 * The borrow content
	 */
	private String importImplementations;
	
	private String importInstances;
		
	/**
	 * The local content
	 */
	private String exportImplementations;
	
	private String exportInstances;
	
	/**
	 * The application content
	 */
	private String applicationInstances;

	public VisibilityDeclaration() {
	}
	

	/**
	 * An expression that must be satisfied by all imported implementations 
	 */
	public String getImportImplementations() {
		return importImplementations;
	}

	public void setBorrowImplementations(String borrowImplementations) {
		this.importImplementations = borrowImplementations;
	}

	/**
	 * An expression that must be satisfied by all imported instances 
	 */
	public String getImportInstances() {
		return importInstances;
	}
	
	public void setImportInstances(String borrowInstances) {
		this.importInstances = borrowInstances;
	}

	/**
	 * An expression that must be satisfied by all implementations that
	 * are not available for exporting 
	 */
	public String getExportImplementations() {
		return exportImplementations;
	}

	public void setExportImplementations(String localImplementations) {
		this.exportImplementations = localImplementations;
	}


	/**
	 * An expression that must be satisfied by all instances that
	 * are not available for exporting 
	 */
	public String getExportInstances() {
		return exportInstances;
	}

	public void setExportInstances(String localInstances) {
		this.exportInstances = localInstances;
	}

	/**
	 * An expression that must be satisfied by all exported instances that
	 * are available for other composites in the application 
	 */
	public String getApplicationInstances() {
		return applicationInstances;
	}
	
	public void setApplicationInstances(String applicationInstances) {
		this.applicationInstances = applicationInstances;
	}

	
}
