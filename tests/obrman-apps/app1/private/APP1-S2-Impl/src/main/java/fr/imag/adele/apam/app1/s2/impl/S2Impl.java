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
package fr.imag.adele.apam.app1.s2.impl;

import fr.imag.adele.apam.app1.s2.spec.S2;
import fr.imag.adele.apam.app1.s3.spec.S3;

public class S2Impl implements S2 {

    private S3 s3;

    @Override
    public void call(String texte) {
        texte = texte + " >>> " + S2.class.getSimpleName();
        System.out.println(texte);
        s3.call(texte);

    }
}
