package fr.imag.adele.apam.pax.test.impl.deviceSwitch;

import java.util.Set;

import fr.imag.adele.apam.pax.test.iface.device.Eletronic;



public class PropertyInjectionTypeSwitch implements Eletronic {

	Set<Integer> setInt;
	Set<String> setString;
	Set<String> OS;
	
	
	public Set<Integer> getSetInt() {
		return setInt;
	}



	public void setSetInt(Set<Integer> setInt) {
		this.setInt = setInt;
	}



	public Set<String> getSetString() {
		return setString;
	}



	public void setSetString(Set<String> setString) {
		this.setString = setString;
	}



	public Set<String> getOS() {
		return OS;
	}



	public void setOS(Set<String> oS) {
		OS = oS;
	}



	@Override
	public void shutdown() {}	
	
}
