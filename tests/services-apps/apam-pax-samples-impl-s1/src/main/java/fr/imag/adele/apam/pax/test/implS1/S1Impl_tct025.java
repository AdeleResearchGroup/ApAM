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
package fr.imag.adele.apam.pax.test.implS1;

public class S1Impl_tct025 {
    String injectedInternal;
    String injectedExternal;
    String injectedBothSetted;
    String injectedBothUnsetted;

    public S1Impl_tct025() {
	injectedInternal="Constructor defined value";
	injectedExternal="Constructor defined value";
	injectedBothSetted="Constructor defined value";
	injectedBothUnsetted="Constructor defined value";
    }

    public String whoami() {
	return this.getClass().getName();
    }

    public void start() {
	System.out.println("Starting:" + this.getClass().getName());
    }

    public void stop() {
	System.out.println("Stopping:" + this.getClass().getName());
    }

    public String getInjectedInternal() {
	return injectedInternal;
    }

    public void setInjectedInternal(String injectedInternal) {
	this.injectedInternal = injectedInternal;
    }
    
    public String getInjectedExternal() {
	return injectedExternal;
    }

    public void setInjectedExternal(String injectedExternal) {
	this.injectedExternal = injectedExternal;
    }

    public String getInjectedBothSetted() {
	return injectedBothSetted;
    }

    public void setInjectedBoth(String injectedBothSetted) {
	this.injectedBothSetted = injectedBothSetted;
    }

    public String getInjectedBothUnsetted() {
	return injectedBothUnsetted;
    }

    public void setInjectedBothUnsetted(String injectedBothUnsetted) {
	this.injectedBothUnsetted = injectedBothUnsetted;
    }


}
