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

import fr.imag.adele.apam.test.s1.Fib;


public class Fibonacci implements Fib {

	/*
	 * Variable attribute
	 */
	int fibo = -1 ;
	int fibMoins1 = -1 ;
	int fibMoins2 = -2 ;

	Fib moins1 = null ;
	Fib moins2 = null ;

	public int compute (int n) {
		if (n==1) return 1 ;
		return moins1.compute(n-1) + moins2.compute(n-2) ; 
	}

	public int computeSmart (int n) {

		if (fibo != -1) return fibo ;			

		fibo = moins1.compute(n-1) + moins2.compute(n-2) ;
		return fibo ;
	}

}
