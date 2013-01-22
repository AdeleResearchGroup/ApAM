package fr.imag.adele.apam.distriman.web.samples;


public class SampleImpl implements SampleIface {
	
	@Override
	public String hello(String s) {
		// TODO Auto-generated method stub
		System.out.println("##### Server: Say hello method called!");
		
		return "Hello " + s;
	}
	
	@Override
	public SampleComplexIface getComplex() {
		return new SampleComplex("super complex");
	}
}
