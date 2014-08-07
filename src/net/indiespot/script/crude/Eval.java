package net.indiespot.script.crude;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.indiespot.script.crude.CrudeScript.Block;
import net.indiespot.script.crude.CrudeScript.Code;
import net.indiespot.script.crude.CrudeScript.Instruction;
import net.indiespot.script.crude.CrudeScript.Trace;

public class Eval {
	private final Context ctx;
	private final Eval parent;
	private Eval sub;
	private Block block;
	private Code exec;
	private Map<String, Object> vars;

	public Eval(Context ctx, Block block) {
		this(ctx, null, block);
	}

	public Eval(Context ctx, Eval parent, Block block) {
		this.ctx = ctx;
		this.parent = parent;
		this.block = block;
	}

	public void define(String var, Object value) {
		if(vars == null)
			vars = new HashMap<>();
		if(vars.containsKey(var))
			throw new IllegalStateException("variable '" + var + "' already defined in this scope");
		vars.put(var, value);
	}

	public void assign(String var, Object value) {
		if(vars != null && vars.containsKey(var))
			vars.put(var, value);
		else if(parent != null)
			parent.assign(var, value);
		else
			throw new IllegalStateException("undefined variable '" + var + "'");
	}

	public Object get(String var) {
		if(vars != null && vars.containsKey(var))
			return vars.get(var);
		if(parent == null)
			throw new IllegalStateException("undefined variable '" + var + "'");
		return parent.get(var);
	}

	public Object get(String var, Object orElse) {
		if(vars != null && vars.containsKey(var))
			return vars.get(var);
		if(parent == null)
			return orElse;
		return parent.get(var);
	}

	public State tick() {
		if(sub != null) {
			State state;
			try {
				state = sub.tick();
			}
			catch (ScriptRaiseException uncaught) {
				Instruction trap = block.findCatch(uncaught.raised);
				if(trap == null)
					throw uncaught;
				exec = trap.prev;
				return State.RUNNING;
			}
			if(state == State.TERMINATED) {
				sub = null;
				state = State.RUNNING;
			}
			return state;
		}

		if(exec == null) {
			if(block.codes.isEmpty())
				return State.TERMINATED;
			exec = block.codes.get(0);
		}
		else {
			exec = exec.next;
			if(exec == null) {
				return State.TERMINATED;
			}
		}

		if(exec instanceof Block) {
			sub = new Eval(ctx, this, (Block) exec);
			return State.RUNNING;
		}

		try {
			return this.exec((Instruction) exec);
		}
		catch (ScriptRaiseException raised) {
			Instruction trap = block.findCatch(raised.raised);
			if(trap == null)
				throw raised;
			exec = trap.prev;
			return State.RUNNING;
		}
	}

	public Trace currentStackElement() {
		if(sub != null)
			return sub.currentStackElement();
		if(exec != null && exec.trace.lineNr > 0)
			return exec.trace;
		if(block != null && block.trace.lineNr > 0)
			return block.trace;
		return null;
	}

	private State exec(Instruction instruction) {
		if("JUMP".equals(instruction.type)) {
			if("IF".equals(instruction.parts[0])) {
				// IF <cond> CALL XY
				// IF <cond> CALL XY ELSE GOTO YZ
				// IF <cond> GOTO YZ
				// IF <cond> GOTO YZ ELSE CALL XY
				// IF <cond> LOOP
				// IF NOT <cond> LOOP ELSE BREAK

				int off = 1;
				boolean ifNot = "NOT".equals(instruction.parts[off]);
				if(ifNot)
					off += 1;
				String ifCond = instruction.parts[off];
				off += 1;
				String ifCmd = instruction.parts[off];
				String ifArg = hasArg(ifCmd) ? instruction.parts[off + 1] : null;
				boolean isCond = ctx.query(ifCond) ^ ifNot;

				off += hasArg(ifCmd) ? 2 : 1;
				boolean hasElse = instruction.parts.length > off;

				String elseCmd = null, elseArg = null;
				if(hasElse) {
					if(!"ELSE".equals(instruction.parts[off]))
						throw new IllegalStateException();
					off += 1;
					elseCmd = instruction.parts[off + 0];
					elseArg = hasArg(elseCmd) ? instruction.parts[off + 1] : null;
					off += hasArg(elseCmd) ? 2 : 1;

					if(instruction.parts.length > off)
						throw new IllegalStateException();
				}

				if(isCond)
					return jump(ifCmd, ifArg);
				if(hasElse)
					return jump(elseCmd, elseArg);
				return State.RUNNING;
			}
			else if("CATCH".equals(instruction.parts[0])) {
				String cmd = instruction.parts[2];
				String arg = hasArg(cmd) ? instruction.parts[3] : null;
				return jump(cmd, arg);
			}
			else {
				String cmd = instruction.parts[0];
				String arg = hasArg(cmd) ? instruction.parts[1] : null;
				return jump(cmd, arg);
			}
		}
		else if("CODE".equals(instruction.type)) {
			return ctx.signal(instruction.text);
		}
		else if("LABEL".equals(instruction.type)) {
			return State.RUNNING;
		}
		else {
			throw new UnsupportedOperationException(instruction.type);
		}
	}

	private State jump(String cmd, String arg) {
		switch (cmd) {
		case "LOOP":
			exec = null;
			return State.RUNNING;
		case "BREAK":
			return State.TERMINATED;
		case "GOTO":
			exec = block.findLabel(arg);
			return State.RUNNING;
		case "CALL":
			sub = new Eval(ctx, this, block.findBlock(arg));
			return State.RUNNING;
		case "HALT":
			sub = null;
			return State.HALTED;
		case "WAIT":
			return State.WAITING;
		case "YIELD":
			return State.YIELDED;
		case "SLEEP":
			ctx.nextSleep(arg);
			return State.SLEEPING;
		case "THROW":
			raise(arg);
			return null;
		default:
			throw new UnsupportedOperationException(cmd);
		}
	}

	private boolean hasArg(String cmd) {
		switch (cmd) {
		case "LOOP":
		case "BREAK":
		case "HALT":
		case "YIELD":
		case "WAIT":
			return false;
		case "GOTO":
		case "CALL":
		case "THROW":
		case "SLEEP":
			return true;
		default:
			throw new UnsupportedOperationException(cmd);
		}
	}

	private void raise(String arg) {
		final String classname = CrudeScript.class.getName();

		List<StackTraceElement> trace = new ArrayList<>();
		Eval eval = this;

		do {
			trace.add(new StackTraceElement(classname, eval.block.name, eval.exec.trace.file, eval.exec.trace.lineNr));
			trace.add(new StackTraceElement(classname, eval.block.name, eval.block.trace.file, eval.block.trace.lineNr));
		}
		while ((eval = eval.parent) != null);

		// remove root
		trace.remove(trace.size() - 1);

		// remove duplicate trace elements, if any
		for(int i = 1; i < trace.size(); i++) {
			StackTraceElement prev = trace.get(i - 1);
			StackTraceElement curr = trace.get(i);
			if(!curr.getClassName().equals(prev.getClassName()))
				continue;
			if(!curr.getFileName().equals(prev.getFileName()))
				continue;
			if(curr.getLineNumber() != prev.getLineNumber())
				continue;
			trace.remove(i--);
		}

		ScriptRaiseException e = new ScriptRaiseException(arg);
		trace.addAll(Arrays.asList(e.getStackTrace()));
		e.setStackTrace(trace.toArray(new StackTraceElement[trace.size()]));
		throw e;
	}
}