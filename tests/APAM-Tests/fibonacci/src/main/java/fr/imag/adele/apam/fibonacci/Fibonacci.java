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
	
//	<definition name="Ifibo" field="fibnb" type="int"/>
//	<definition name="Sfibo" field="sfibo" type="boolean"/>
//	<definition name="Afibo" field="fibil" type="{int}"/>
//	<definition name="Afibn" field="fibn" type="{string}" />

	int fibnb ;
	boolean sfibo ;
	int[] fibil ;
	String [] fibn ;

	
	Fib moins1  ;
	Fib moins2  ;

	public int compute (int n) {
		if ( n < 2 ) return 1 ;
		System.out.println("n=" + n);
		fibil[2] = n ;
		System.out.println(fibil);
		fibn [1]= Integer.toString (n); ;
		System.out.println(fibil);
		
		return moins1.compute(n-1) + moins2.compute(n-2) ; 
	}

	public int computeSmart (int n) {
//return 0 ;
		if (fibnb == n) return fibo ;			
		fibnb=n ;
		if ( n > 1 ) {
			fibo = moins1.computeSmart(n-1) + moins2.computeSmart(n-2) ;
		} else fibo = 1 ;
		return fibo ;
	}

	@Override
	public void apamInit(Instance apamInstance) {
		FibMain.nbInst ++;
		fibnb = 5 ;
		sfibo = true ;
		fibil = new int[8] ;
		for (int i = 0 ; i < fibil.length ; i++) {
			fibil[i] = i ;
		}
		fibn = new String [3] ;
		fibn[0]= "val1" ;
		fibn[1]= "val2" ;
		fibn[2]= "val3" ;
	}

	@Override
	public void apamRemove() {
	}

	public void wiredFor(String resource) {
	}

	public void unWiredFor(String resource) {
	}

}
