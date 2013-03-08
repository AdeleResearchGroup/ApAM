package fr.imag.adele.apam.pax.test.performance;

public class FibonacciIterative implements Fibonacci {

	public int compute(int n) {
		int prev1 = 0, prev2 = 1;
		for (int i = 0; i < n; i++) {
			int savePrev1 = prev1;
			prev1 = prev2;
			prev2 = savePrev1 + prev2;
		}
		return prev1;

	}

	@Override
	public int getCalls() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void callsInit(int val) {
		
		
	}
	
}
