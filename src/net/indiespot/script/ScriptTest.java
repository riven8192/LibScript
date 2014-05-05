package net.indiespot.script;

public class ScriptTest {

	public static void main(String[] args) {
		//String eval = "while(hazard.stuff.data( 0.5 + 5. - 5.5 & 3 + 4.4*abc(a) / 2 * 1 )) {\n\tyield;\n}aaa";
		//String eval = "while(a) {\n\tu;\n}";
		//String eval = "x( ( y ) )";
		//String eval = "x(zz)y while(a){\n\tb;}\nc";
		//String eval = "x = 1337 + 1; y = x; 4.5 + 50 / 6 + x ";
		//String eval = "-4.5 * {-400 >> 1 + -5 & 1337} + 1";
		//String eval = "x = 4; if(x == 4) { x = x + 1; } y = 555; if(x == 10) { x = x + 2; } else { x = 123; }";
		//String eval = "x = 4; while(x != 10) { x = x + 1; }";
		//String eval = "x = 4;\nwhile(x != (9+1)) {\n\tx += (1+1*1);\n}";
		//String eval = "x = -2; y = -1; while(func(x, y+1*3, z)) { fan(1,2,3+4/2); }";
		//String eval = "x = 4; y = x + x; if( x == 4 ) { y = 1; } else { y = 2; }";
		//String eval = "x = 4; \n while(x<=7) { \n x=x+1;\n } \n";
		String eval = "b = true; if(false) { return b; } x = 4; \n if(x==4) { \n while(x<=7) { \n x=x+1; yield x; yield b; \n } \n } \n else { \n x = 0; \n } ; return x+3*2 ; \n";
		//String eval = "4*4*4; 3+3; 2; 1";
		System.out.println(eval);
		System.out.println();

		Node script = PostProcessor.process(Tokenizer.parse(eval));

		for(int i = 0; i < 100; i++) {
			long t0 = System.nanoTime();
			Stepper stepper = new Stepper(script);

			outer: for(;;) {
				switch (stepper.step()) {
				case RUNNING:
					break;

				case TERMINATED:
					break outer;

				case YIELDED:
					Object yielded = stepper.getValue();
					//System.out.println("yielded: " + yielded);
					break;
				}

				if(false) {
					System.out.println("  stack: " + stepper.stack);
					System.out.println("  vars: " + stepper.vars);
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException exc) {
						// ok
					}
				}
			}
			long t1 = System.nanoTime();
			System.out.println("returned: " + stepper.getValue() + ", took: " + (t1 - t0) / 1000L + "us");
		}
	}
}
