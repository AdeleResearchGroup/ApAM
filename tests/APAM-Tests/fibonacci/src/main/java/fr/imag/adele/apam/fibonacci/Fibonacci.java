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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
	Set<Integer> fibil = new HashSet<Integer> (Arrays.asList(5,6,8));
	Set<String>   fibn = new HashSet<String> ();

	String fString ;
	Integer fInt ;
	Set<Integer> setInt = new HashSet<Integer> (Arrays.asList(5,6,8));
	Set<String> setS =  new HashSet<String> () ;
	
	Fib moins1  ;
	Fib moins2  ;
	
	Instance apamInstance ;

	public int compute (int n) {
		if ( n < 2 ) return 1 ;
		System.out.println("n=" + n);
		fibil.add (Integer.valueOf(n)) ;
		System.out.println("fibil Java: " + fibil.toString());
		System.out.println("Afibil attr: " + apamInstance.getProperty("Afibl").toString());
		
		fibn.add(Integer.toString (n));
		System.out.println("fibi Java: " + fibn);
		System.out.println("Afibin Attr: " + apamInstance.getProperty("Afibn").toString());
		
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
		this.apamInstance = apamInstance ;
		FibMain.nbInst ++;
		fibnb = 5 ;
		sfibo = true ;
		fibil.add(5) ;
		fibil.add(6) ;

		fibn.add("val1") ;
		fibn.add("val2") ;
		fibn.add("val3") ;
	}

	@Override
	public void apamRemove() {
	}

	public void wiredFor(String resource) {
	}

	public void unWiredFor(String resource) {
	}

}
