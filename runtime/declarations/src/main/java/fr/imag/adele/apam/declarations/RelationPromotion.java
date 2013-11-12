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

public class RelationPromotion {

    /**
     * The relation to be promoted
     */
    private final RelationDeclaration.Reference source;

    /**
     * The composite relation that will be the target of the promotion
     */
    private final RelationDeclaration.Reference target;

    public RelationPromotion(RelationDeclaration.Reference source,
	    RelationDeclaration.Reference target) {
	this.source = source;
	this.target = target;
    }

    /**
     * The target of the promotion
     */
    public RelationDeclaration.Reference getCompositeRelation() {
	return target;
    }

    /**
     * The relation to be promote
     */
    public RelationDeclaration.Reference getContentRelation() {
	return source;
    }
}
