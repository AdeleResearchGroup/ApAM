package fr.imag.adele.apam.test.s5Impl;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.apamImpl.CST;
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
    public void apamStart(Instance apamInstance) {

        boolean res;
        ApamFilter f = ApamFilter.newInstance("()");
        res = apamInstance.match("(s5-spec=coucous5)");
        res = apamInstance.match(ApamFilter.newInstance("(s5-spec=\"coucou5\")")); // false
        res = apamInstance.match(ApamFilter.newInstance("(&(s5b=false)(s5-spec=coucous5))"));
        res = apamInstance.match(ApamFilter.newInstance("(s5b=true)"));
        res = apamInstance.match(ApamFilter.newInstance("(s5c=vals5c)"));

        apamInstance.getComposite().getCompType().put(CST.A_LOCALINSTANCE, CST.V_TRUE);
        apamInstance.getComposite().getCompType().put(CST.A_LOCALIMPLEM, CST.V_TRUE);
        System.out.println("set LOCAL instancfe et implem of S5CompEx to true.");
    }

    @Override
    public void apamStop() {
        // TODO Auto-generated method stub

    }

    @Override
    public void apamRelease() {
        // TODO Auto-generated method stub

    }

}