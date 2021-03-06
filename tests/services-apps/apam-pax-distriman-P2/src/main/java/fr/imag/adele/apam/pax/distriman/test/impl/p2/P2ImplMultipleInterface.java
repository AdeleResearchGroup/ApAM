package fr.imag.adele.apam.pax.distriman.test.impl.p2;

import java.util.ArrayList;
import java.util.List;

import fr.imag.adele.apam.pax.distriman.test.iface.P2Spec;
import fr.imag.adele.apam.pax.distriman.test.iface.P2Spec2;
import fr.imag.adele.apam.pax.distriman.test.iface.P2SpecKeeper;

public class P2ImplMultipleInterface implements  P2Spec,P2Spec2{ 

	@Override
	public String getName(){
		return "P2 REMOTE Instance";
	}

	@Override
	public List<String> getListNames() {
		// TODO Auto-generated method stub
		return new ArrayList<String>(){{add("goethe");add("nietzsche");add("bulkovisk");}};
	}

	@Override
	public P2SpecKeeper getKeeper() {
		// TODO Auto-generated method stub
		return new P2SpecKeeper("keeper value ok");
	}

	@Override
	public String getSpec2Name() {
		// TODO Auto-generated method stub
		return "qsdqdsq";
	}

}
