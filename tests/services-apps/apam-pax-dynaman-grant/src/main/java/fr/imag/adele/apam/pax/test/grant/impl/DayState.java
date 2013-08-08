/**
 * Copyright 2011-2013 Universite Joseph Fourier, LIG, ADELE team
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
 *
 * DayState.java - 7 août 2013
 */
package fr.imag.adele.apam.pax.test.grant.impl;

/**
 * @author thibaud
 *
 */
public class DayState {
    private int hour=0;
    private String state="night";
    
    public void setHour(int hour) {
	this.hour=hour;
	if(hour<6 || hour>22)
	    state="night";
	else if (hour <12)
	    state="morning";
	else
	    state="afternoon";
    }
 }
