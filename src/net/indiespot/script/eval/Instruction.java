package net.indiespot.script.eval;

public class Instruction {

	public static void execute(Scope frame) {
		Node node = frame.exec;

		if(node.token == null) {
			// no-op
		}
		else if(node.type != null) {
			switch (node.type) {
			case BOOL:
			case INT:
			case FLOAT:
				frame.stepper.stack.add(node.typeConst);
				break;

			case NAME:
				if(node.children.isEmpty()) {
					// variable
					frame.stepper.stack.add(node.token);
				}
				else {
					// function
					System.out.println("calling function: " + node.token + "()");

					// FIXME
					if(node.token.equals("func"))
						frame.stepper.stack.add(Boolean.FALSE);
					else if(node.token.equals("fan"))
						; // no op
					else
						throw new IllegalStateException();
				}
				break;

			case ASSIGN: {
				Object v2 = (Object) frame.stepper.popStack();
				String v1 = (String) frame.stepper.popStack();
				frame.stepper.vars.put(v1, v2);
				break;
			}

			case DEREFERENCE: {
				String fieldname = (String) frame.stepper.popStack();
				String reference = (String) frame.stepper.popStack();

				Object target = frame.stepper.resolveVar(reference);
				Object result = null;//target.getClass().getField(fieldname).get(target);

				frame.stepper.stack.add(result);
				break;
			}
			}
		}
		else if(Tokenizer.math_infix_ops.contains(node.token)) {
			Object v2 = frame.stepper.popStack();
			Object v1 = frame.stepper.popStack();
			String var = (v1 instanceof String) ? (String) v1 : null;

			if(v2 instanceof String)
				v2 = frame.stepper.resolveVar((String) v2);
			if(v1 instanceof String)
				v1 = frame.stepper.resolveVar((String) v1);

			if((v2 instanceof Boolean) && (v1 instanceof Boolean)) {
				if(!Tokenizer.bool_math_infix_ops.contains(node.token))
					throw new IllegalStateException();

				boolean b2 = ((Boolean) v2).booleanValue();
				boolean b1 = ((Boolean) v1).booleanValue();

				if(node.token.equals("=="))
					frame.stepper.stack.add(b1 == b2);
				else if(node.token.equals("!="))
					frame.stepper.stack.add(b1 != b2);
				else
					throw new IllegalStateException();
			}
			else if((v2 instanceof Float) || (v1 instanceof Float)) {
				if(!Tokenizer.float_math_infix_ops.contains(node.token))
					throw new IllegalStateException();

				float f2 = ((Number) v2).floatValue();
				float i1 = ((Number) v1).floatValue();

				switch (node.token) {
				case "+":
					frame.stepper.stack.add(i1 + f2);
					break;
				case "-":
					frame.stepper.stack.add(i1 - f2);
					break;
				case "*":
					frame.stepper.stack.add(i1 * f2);
					break;
				case "/":
					frame.stepper.stack.add(i1 / f2);
					break;
				case "%":
					frame.stepper.stack.add(i1 % f2);
					break;
				case "==":
					frame.stepper.stack.add(i1 == f2);
					break;
				case "!=":
					frame.stepper.stack.add(i1 != f2);
					break;
				case "<":
					frame.stepper.stack.add(i1 < f2);
					break;
				case ">":
					frame.stepper.stack.add(i1 > f2);
					break;
				case "<=":
					frame.stepper.stack.add(i1 <= f2);
					break;
				case ">=":
					frame.stepper.stack.add(i1 >= f2);
					break;
				case "+=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 + f2);
					break;
				case "-=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 - f2);
					break;
				case "*=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 * f2);
					break;
				case "/=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 / f2);
					break;
				case "%=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 % f2);
					break;
				default:
					throw new IllegalStateException("token: " + node.token);
				}
			}
			else if((v2 instanceof Integer) && (v1 instanceof Integer)) {
				if(!Tokenizer.int_math_infix_ops.contains(node.token))
					throw new IllegalStateException();

				int i2 = ((Number) v2).intValue();
				int i1 = ((Number) v1).intValue();

				switch (node.token) {
				case "+":
					frame.stepper.stack.add(i1 + i2);
					break;
				case "-":
					frame.stepper.stack.add(i1 - i2);
					break;
				case "*":
					frame.stepper.stack.add(i1 * i2);
					break;
				case "/":
					frame.stepper.stack.add(i1 / i2);
					break;
				case "%":
					frame.stepper.stack.add(i1 % i2);
					break;
				case ">>>":
					frame.stepper.stack.add(i1 >>> i2);
					break;
				case ">>":
					frame.stepper.stack.add(i1 >> i2);
					break;
				case "<<":
					frame.stepper.stack.add(i1 << i2);
					break;
				case "&":
					frame.stepper.stack.add(i1 & i2);
					break;
				case "|":
					frame.stepper.stack.add(i1 | i2);
					break;
				case "^":
					frame.stepper.stack.add(i1 ^ i2);
					break;
				case "==":
					frame.stepper.stack.add(i1 == i2);
					break;
				case "!=":
					frame.stepper.stack.add(i1 != i2);
					break;
				case "<":
					frame.stepper.stack.add(i1 < i2);
					break;
				case ">":
					frame.stepper.stack.add(i1 > i2);
					break;
				case "<=":
					frame.stepper.stack.add(i1 <= i2);
					break;
				case ">=":
					frame.stepper.stack.add(i1 >= i2);
					break;
				case "+=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 + i2);
					break;
				case "-=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 - i2);
					break;
				case "*=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 * i2);
					break;
				case "/=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 / i2);
					break;
				case "%=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 % i2);
					break;
				case ">>>=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 >>> i2);
					break;
				case ">>=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 >> i2);
					break;
				case "<<=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 << i2);
					break;
				case "&=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 & i2);
					break;
				case "|=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 | i2);
					break;
				case "^=":
					if(var == null)
						throw new IllegalStateException();
					frame.stepper.vars.put(var, i1 ^ i2);
					break;
				default:
					throw new IllegalStateException("token: " + node.token);
				}
			}
		}
		else {
			throw new UnsupportedOperationException("token: " + node.token);
		}
	}
}
