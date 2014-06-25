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
package fr.imag.adele.apam;

import java.util.Set;

import fr.imag.adele.apam.declarations.CompositeDeclaration;

public interface CompositeType extends Implementation {
	/**
	 * Adds an "import" relationship towards "destination". Not in the interface
	 * 
	 * @param destination
	 */
	public void addImport(CompositeType destination);

	/**
	 * return true if the current type contains "impl"
	 * 
	 * @param spec
	 * @return
	 */
	public boolean containsImpl(Implementation impl);

	/**
	 * 
	 * @return the declaration of this composite
	 */
	public CompositeDeclaration getCompoDeclaration();

	/**
	 * return the contextual relation of that Identifier. Needs that the
	 * relation source is an ancestor of parameter source and source same kind
	 * as sourceType
	 * 
	 * @param id
	 * @return
	 */
	public RelationDefinition getCtxtRelation(Component source, String id);

	/**
	 * return the contextual relation of that Identifier. Needs that the
	 * relation source is an ancestor of parameter source and source same kind
	 * as sourceType
	 * 
	 * @param id
	 * @return
	 */
	public Set<RelationDefinition> getCtxtRelations(Component source);

	/**
	 * return the composite types embedded in the current one.
	 * 
	 * @return
	 */
	public Set<CompositeType> getEmbedded();

	/**
	 * return all the implementation contained in this type
	 * 
	 * @return
	 */
	public Set<Implementation> getImpls();

	/**
	 * returns all the "import" relationships
	 * 
	 * @return
	 */
	public Set<CompositeType> getImport();

	/**
	 * returns the composite types that contain this one.
	 * 
	 * @return
	 */
	public Set<CompositeType> getInvEmbedded();

	/**
	 * 
	 * @return the main implementation
	 */
	public Implementation getMainImpl();

	/**
	 * 
	 */
	public ManagerModel getModel(ContextualManager manager);

    public ManagerModel getModel(String managerName);


	/**
	 * returns all the models
	 * 
	 * @return
	 */
	public Set<ManagerModel> getModels();

	/**
	 * return true if the composite type import "destination"
	 * 
	 * @param destination
	 * @return
	 */
	public boolean isFriend(CompositeType destination);

}
