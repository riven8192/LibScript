package net.indiespot.script.interp;

public class TestScript {
	public static int add(int a, int b) {
		return a + b;
	}

	public static int sub(int a, int b) {
		return a - b;
	}

	public static int div(int a, int b) {
		return a / b;
	}

	public static int div2(int a, int b) {
		int count = 0;
		while (a >= b) {
			a -= b;
			count++;
		}
		return count;
	}

	public static int div3(int a, int b) {
		int count = 0;
		while (a >= b) {
			a = sub(a, b);
			count++;
		}
		return count;
	}

	public static int bus(Object txt) {
		return (txt == null) ? -1 : bus2(-3, txt, 0.0f, 0);
	}

	public static int bus2(int arg0, Object txt, float arg1, int arg2) {
		return (txt == null) ? -1 : txt.toString().length() + "oi".length() + arg0;
	}
}
