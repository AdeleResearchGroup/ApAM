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
package fr.imag.adele.obrMan.internal;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class LinkedProperties extends Properties {

    private static final long serialVersionUID = 1L;

    private final LinkedHashSet<Object> keys = new LinkedHashSet<Object>();

    public LinkedProperties() {
    }

    public LinkedProperties(Properties defaults) {
	super(defaults);
    }

    @Override
    public Enumeration<Object> keys() {
	return Collections.<Object> enumeration(keys);
    }

    @Override
    public Set<Object> keySet() {
	return keys;
    }

    @Override
    public Object put(Object key, Object value) {
	keys.add(key);
	return super.put(key, value);
    }
}
