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
package fr.imag.adele.apam.test.s2Impl;

import fr.imag.adele.apam.test.s2.S2;

public class S2Final implements S2 {

	public void callS2(String s) {
		System.out.println("In S2Final " + s);
	}

	@Override
	public void callBackS2(String s) {
		System.out.println("call back in S2Final " + s);
	}

	@Override
	public String getName() {

		return "S2Final";
	}
}
