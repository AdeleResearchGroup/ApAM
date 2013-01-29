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
package fr.imag.adele.apam.test.s5Impl;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ApamComponent;
//import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s5.S5;

public class S5Impl implements S5, ApamComponent {
    S2 s2_inv;

    @Override
    public void callS5(String s) {
        System.out.println("S5 called " + s);
        if (s2_inv != null)
            s2_inv.callBackS2(" back to S2 from s5");
        else
            System.err.println("s2_inv is null");
    }

    @Override
    public void apamInit(Instance apamInstance) {

        boolean res;
        //        ApamFilter f = ApamFilter.newInstance("()");
        res = apamInstance.match("(s5-spec=coucous5)");
        res = apamInstance.match(ApamFilter.newInstance("(s5-spec=\"coucou5\")")); // false
        res = apamInstance.match(ApamFilter.newInstance("(&(s5b=false)(s5-spec=coucous5))"));
        res = apamInstance.match(ApamFilter.newInstance("(s5b=true)"));
        res = apamInstance.match(ApamFilter.newInstance("(s5c=vals5c)"));

        //        apamInstance.getComposite().getCompType().put(CST.A_LOCALINSTANCE, CST.V_TRUE);
        //        apamInstance.getComposite().getCompType().put(CST.A_LOCALIMPLEM, CST.V_TRUE);
        System.out.println("set LOCAL instance et implem of S5CompEx");
    }

    @Override
    public void apamRemove() {
        // TODO Auto-generated method stub
    }
}