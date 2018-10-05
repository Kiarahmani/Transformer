public class CallGraphs {
	public static void main(String[] args) {
		A a = new A();
		int x = a.mkDouble(12);
		System.out.println(x);
		x = x + 1;
		System.out.println(x);
	}
}

class A {
	int mkDouble(int i) {
		int x = 100;
		while (x<150) {
			if (x < 200)
				x = 160;
			else
				x = 200;
		}

		return x;
	}
}