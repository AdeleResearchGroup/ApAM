package fr.imag.adele.apam.distriman.web;


public class SampleImpl implements SampleIface{

	SampleComplexIface complex=new SampleComplex("super complex");
	
	@Override
	public String hello(String s) {
		// TODO Auto-generated method stub
		System.out.println("##### Server: Say hello method called!");
		
		return "Hello " + s;
	}
	
	public SampleComplexIface getComplex() {
		return complex;
	}
}
