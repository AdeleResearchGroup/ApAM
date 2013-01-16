package fr.imag.adele.apam.distriman.web;


public class SampleComplex implements SampleComplexIface{

	String value;
	
	public SampleComplex(String value) {
		this.value=value;
	}

	@Override
	public String getValue() {
		// TODO Auto-generated method stub
		return value;
	}


}
