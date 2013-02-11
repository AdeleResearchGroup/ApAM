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
package fr.imag.adele.apam.fibonacci;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;



public class Fibonacci implements Fib, ApamComponent {

	/*
	 * Variable attribute
	 */
	int fibo = -1 ;
	int fibMoins1 = -1 ;
	int fibMoins2 = -2 ;

	Fib moins1  ;
	Fib moins2  ;

	public int compute (int n) {
		if ( n < 2 ) return 1 ;
		return moins1.compute(n-1) + moins2.compute(n-2) ; 
	}

	public int computeSmart (int n) {

		if (fibo != -1) return fibo ;			

		if ( n < 2 ) return 1 ;
		
		fibo = moins1.computeSmart(n-1) + moins2.computeSmart(n-2) ;
		return fibo ;
	}

	@Override
	public void apamInit(Instance apamInstance) {
		FibMain.nbInst ++;
	}

	@Override
	public void apamRemove() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void wiredFor(String resource) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unWiredFor(String resource) {
		// TODO Auto-generated method stub
		
	}

}
