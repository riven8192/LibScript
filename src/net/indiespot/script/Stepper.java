package net.indiespot.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Stepper {
	public Scope scope;

	public Stepper(Node root) {
		scope = new Scope(this, root);
		scope.isFunction = true;
	}

	public static enum State {
		RUNNING, YIELDED, TERMINATED
	}

	public State step() {
		if(scope == null)
			return State.TERMINATED;

		if(scope.exec == null) {
			// reached end of scope
			scope = scope.parent;
		}
		else if(Tokenizer.control_flow_tokens.contains(scope.exec.token)) {
			switch (scope.exec.token) {

			case "if": {
				Node condBody = scope.exec.children.get(0);
				Node elseBody = scope.exec.children.size() == 2 ? scope.exec.children.get(1) : null;

				Scope curr = scope;
				if(((Boolean) popStack()).booleanValue())
					scope = new Scope(this, condBody);
				else if(elseBody != null)
					scope = new Scope(this, elseBody);
				curr.exec = curr.exec.next;
				break;
			}

			case "while": {
				Node condBody = scope.exec.children.get(0);

				if(((Boolean) popStack()).booleanValue())
					scope = new Scope(this, condBody);
				else
					scope.exec = scope.exec.next;
				break;
			}

			case "return": {
				do {
					scope = scope.parent;
				}
				while (!scope.isFunction);
				break;
			}

			case "yield": {
				scope.exec = scope.exec.next;
				return State.YIELDED;
			}

			default:
				throw new IllegalStateException();
			}
		}
		else {
			Instruction.execute(scope);

			Scope curr = scope;
			if(!scope.exec.children.isEmpty())
				scope = new Scope(this, scope.exec.children.get(0));
			curr.exec = curr.exec.next;
		}

		return State.RUNNING;
	}

	List<Object> stack = new ArrayList<>();
	Map<String, Object> vars = new HashMap<>();

	Object resolveVar(String var) {
		if(var == null || var.isEmpty())
			throw new IllegalStateException();
		if(!vars.containsKey(var))
			throw new IllegalStateException("undefined variable: '" + var + "'");
		Object got = vars.get(var);
		if(got instanceof String) // another var
			got = resolveVar((String) got);
		return got;
	}

	Object popStack() {
		return stack.remove(stack.size() - 1);
	}

	Object getValue() {
		Object result = popStack();
		if(result instanceof String)
			result = this.resolveVar((String) result);
		return result;
	}
}