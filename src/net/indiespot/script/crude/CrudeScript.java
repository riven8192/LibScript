package net.indiespot.script.crude;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

public class CrudeScript {
	public static class Worker implements Context {
		private Eval stepper;

		public Worker(Block script) {
			stepper = script.eval(this);
		}

		public void tick() {
			if(stepper == null)
				return;

			System.out.println("water=" + waterLevel + ", food=" + foodLevel + ", health=" + healthLevel);

			waterLevel -= 1;
			foodLevel -= 1;

			if(waterLevel < 0)
				healthLevel -= 3;
			if(foodLevel < 0)
				healthLevel -= 1;

			outer: for(int i = 0; i < 10; i++) {
				switch (stepper.tick()) {
				case TERMINATED:
					stepper = null;
					break outer;
				case HALTED:
					stepper = null;
					break outer;
				case YIELDED:
					System.out.println("...");
					break outer;
				case SLEEPING:
					System.out.println(".~...~.");
					try {
						Thread.sleep(sleepMillis);
					}
					catch (InterruptedException e) {
						// meh
					}
					break outer;
				case RAISED:
					break;
				default:
					break;
				}
			}
		}

		private int waterLevel = 100;
		private int foodLevel = 100;
		private int healthLevel = 100;

		@Override
		public State signal(String[] code) {
			System.out.println("signal" + Arrays.toString(code));

			if(code[0].endsWith("()")) {
				String func = code[0].substring(0, code[0].length() - 2);
				if(func.equals("drinking")) {
					waterLevel += 50;
				}
				else if(func.equals("eating")) {
					foodLevel += 25;
				}
			}

			return State.RUNNING;
		}

		@Override
		public boolean query(String name) {
			System.out.println("query[" + name + "]");
			if(name.equals("chance_of_death"))
				return Math.random() < 0.01;
			if(name.equals("dead"))
				return healthLevel <= 0;
			if(name.equals("thirsty"))
				return waterLevel < 10;
			if(name.equals("hungry"))
				return foodLevel < 10;
			return healthLevel <= 0;
		}

		long sleepMillis;

		@Override
		public void nextSleep(String time) {
			if(time.endsWith("ms"))
				sleepMillis = Long.parseLong(time.substring(0, time.length() - 2));
			else if(time.endsWith("s"))
				sleepMillis = (long) (1000 * Double.parseDouble(time.substring(0, time.length() - 1)));
			else
				throw new IllegalStateException(time);
		}
	}

	public static final void main(String[] args) throws IOException {
		Block script = new Block();
		script.include("Worker.script", readResource("/script3.txt"));
		script.compile();

		System.out.println("=--=");
		System.out.println(script.toString());
		System.out.println("=--=");

		Worker worker = new Worker(script);
		while (true) {
			worker.tick();
		}
	}

	private static String readResource(String res) throws IOException {
		InputStream in = CrudeScript.class.getResourceAsStream(res);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[4 * 1024];
		while (true) {
			int got = in.read(buf);
			if(got == -1)
				break;
			baos.write(buf, 0, got);
		}
		byte[] raw = baos.toByteArray();
		return new String(raw, "ASCII");
	}

	private static class Trace {
		public final String file;
		public final int lineNr;

		public Trace(String file, int lineNr) {
			this.file = file;
			this.lineNr = lineNr;
		}

		@Override
		public String toString() {
			return file + ":" + lineNr;
		}
	}

	private static class Code {
		public final Trace trace;
		public Block parent;
		public Code prev, next;

		public Code(Trace trace, Block parent) {
			if(parent == null && trace.lineNr != 0)
				throw new IllegalStateException();
			this.trace = trace;
			this.parent = parent;
			if(parent != null)
				parent.codes.add(this);
		}

		void print(int indent, StringBuilder out) {
			out.append("\n").append("L").append(trace.lineNr);
			for(int i = 0; i <= indent; i++)
				out.append("\t");
		}
	}

	private static class Instruction extends Code {
		public final String text, type;
		public final String[] parts;

		public Instruction(Trace trace, Block parent, String text, String meta) {
			super(trace, parent);
			this.text = text;
			this.type = meta;
			this.parts = (text == null) ? null : text.split("\\s+");
		}

		void print(int indent, StringBuilder out) {
			super.print(indent, out);

			out.append(type).append("=[").append(text).append("]");
		}

		@Override
		public String toString() {
			return type + "[" + text + "]";
		}
	}

	private static final AtomicInteger skipOverCounter = new AtomicInteger();

	private static String createSkipOverLabel() {
		return "#" + skipOverCounter.incrementAndGet()+"";
	}

	private static class Block extends Code {
		public final String name;
		public final List<Code> codes;
		private String funcJumper;

		public Block() {
			this(new Trace("<file>", 0), null, "<root>");
		}

		Block(Trace trace, Block parent, String name) {
			super(trace, parent);
			this.name = (name == null || name.isEmpty()) ? "<anonymous>" : name;
			this.codes = new ArrayList<>();
		}

		public void include(String file, String script) {
			Block block = new Block(new Trace(file, 0), null, null);

			block.feed(script);

			for(Code code : block.codes) {
				code.parent = this;
				codes.add(code);
			}
		}

		public void feed(String eval) {
			int lineNr = trace.lineNr;
			for(String line : eval.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n").split("\\n")) {
				this.feed(line, new Trace(trace.file, ++lineNr));
			}
		}

		public boolean feed(String line, Trace trace) {
			line = line.trim();
			if(line.isEmpty())
				return false;
			if(line.startsWith("#"))
				return false;

			if(!codes.isEmpty() && codes.get(codes.size() - 1) instanceof Block) {
				boolean isEnd = ((Block) codes.get(codes.size() - 1)).feed(line, trace);
				if(isEnd) {
					new Instruction(trace, this, null, "END");
					if(funcJumper != null) {
						new Instruction(new Trace(null, 0), this, funcJumper, "LABEL");
						funcJumper = null;
					}
				}
			}
			else if(line.startsWith("BEGIN")) {
				String callName = line.substring("BEGIN".length()).trim();
				new Block(trace, this, callName);
			}
			else if(line.startsWith("FUNCTION")) {
				if(funcJumper != null)
					throw new IllegalStateException();
				funcJumper = createSkipOverLabel();
				String callName = line.substring("FUNCTION".length()).trim();
				new Instruction(new Trace(null, 0), this, "GOTO " + funcJumper, "JUMP");
				new Block(trace, this, callName);
			}
			else if(line.equals("END")) {
				return true; // return control to parent block
			}
			else if(line.endsWith(":")) {
				String label = line.substring(0, line.length() - 1).trim();
				new Instruction(trace, this, label, "LABEL");
			}
			else if(line.startsWith("BREAK")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("REPEAT")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("GOTO")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("IF")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("CALL")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("HALT")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("RAISE")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("YIELD")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("SLEEP")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("TRAP")) {
				String jumper = createSkipOverLabel();
				new Instruction(new Trace(null, 0), this, "GOTO " + jumper, "JUMP");
				new Instruction(trace, this, line, "JUMP");
				new Instruction(new Trace(null, 0), this, jumper, "LABEL");
			}
			else {
				new Instruction(trace, this, line, "CODE");
			}
			return false;
		}

		public void compile() {
			cleanupEnd();
			link();
		}

		public Eval eval(Context ctx) {
			return new Eval(ctx, null, this);
		}

		public Eval eval(Context ctx, String function) {
			Block func = this.findBlock(function);
			Eval top = new Eval(ctx, null, this);
			return new Eval(ctx, top, func);
		}

		private void cleanupEnd() {
			for(int i = codes.size() - 1; i >= 0; i--) {
				Code code = codes.get(i);

				if(code instanceof Instruction) {
					if(((Instruction) code).type.equals("END")) {
						codes.remove(i);
					}
				}

				if(code instanceof Block) {
					((Block) code).cleanupEnd();
				}
			}
		}

		private void link() {
			for(int i = 0; i < codes.size() - 1; i++) {
				codes.get(i + 1).prev = codes.get(i + 0);
				codes.get(i + 0).next = codes.get(i + 1);
			}
			for(int i = codes.size() - 1; i >= 0; i--) {
				if(codes.get(i) instanceof Block) {
					((Block) codes.get(i)).link();
				}
			}
		}

		void print(int indent, StringBuilder out) {
			super.print(indent, out);

			out.append("BLOCK '").append(name).append("'");
			for(Code code : codes) {
				if(code != null) {
					code.print(indent + 1, out);
				}
			}
		}

		private final Map<String, Instruction> label2target = new HashMap<>();
		private final Map<String, Instruction> trap2target = new HashMap<>();
		private final Map<String, Block> block2target = new HashMap<>();

		Instruction findLabel(String name) {
			Instruction instr = label2target.get(name);
			if(instr != null)
				return instr;

			for(Code code : codes) {
				if(!(code instanceof Instruction))
					continue;
				instr = (Instruction) code;
				if(!"LABEL".equals(instr.type))
					continue;
				if(!instr.text.equals(name))
					continue;
				label2target.put(name, instr);
				return instr;
			}

			throw new NoSuchElementException("label: '" + name + "'");
		}

		Instruction findTrap(String name) {
			Instruction instr = trap2target.get(name);
			if(instr != null)
				return instr;

			for(Code code : codes) {
				if(!(code instanceof Instruction))
					continue;
				instr = (Instruction) code;
				if(!"JUMP".equals(instr.type))
					continue;
				if(!instr.parts[0].equals("TRAP"))
					continue;
				if(!instr.parts[1].equals(name))
					continue;
				trap2target.put(name, instr);
				return instr;
			}

			return null;
		}

		Block findBlock(String name) {
			Block block = block2target.get(name);
			if(block != null)
				return block;

			for(Code code : codes) {
				if(!(code instanceof Block))
					continue;
				block = (Block) code;
				if(!block.name.equals(name))
					continue;
				block2target.put(name, block);
				return block;
			}

			if(parent != null) {
				block = parent.findBlock(name);
				block2target.put(name, block);
				return block;
			}

			throw new NoSuchElementException("block: '" + name + "'");
		}

		@Override
		public String toString() {
			StringBuilder out = new StringBuilder();
			this.print(0, out);
			return out.toString();
		}
	}

	private static enum State {
		RUNNING, SLEEPING, TERMINATED, HALTED, RAISED, YIELDED
	}

	private static interface Context {
		public State signal(String[] words);

		public boolean query(String var);

		public void nextSleep(String time);
	}

	private static class Eval {
		private final Context ctx;
		private final Eval parent;
		private Eval sub;
		private Block block;
		private Code exec;
		private Map<String, Object> vars;

		public Eval(Context ctx, Eval parent, Block block) {
			this.ctx = ctx;
			this.parent = parent;
			this.block = block;
		}

		public void define(String var) {
			if(vars == null)
				vars = new HashMap<>();
			if(vars.containsKey(var))
				throw new IllegalStateException("variable '" + var + "' already defined in this scope");
			vars.put(var, null);
		}

		public void assign(String var, String value) {
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

		public State tick() {
			if(sub != null) {
				State state;
				try {
					state = sub.tick();
				}
				catch (ScriptRaiseException uncaught) {
					Instruction trap = block.findTrap(uncaught.raised);
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
				Instruction trap = block.findTrap(raised.raised);
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
					// IF <cond> REPEAT 
					// IF <cond> REPEAT ELSE BREAK

					int off = 2;
					String ifCond = instruction.parts[1];
					String ifCmd = instruction.parts[off + 0];
					String ifArg = hasArg(ifCmd) ? instruction.parts[off + 1] : null;
					boolean isCond = ctx.query(ifCond);

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
				else if("TRAP".equals(instruction.parts[0])) {
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
				return ctx.signal(instruction.parts);
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
			case "REPEAT":
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
			case "YIELD":
				return State.YIELDED;
			case "SLEEP":
				ctx.nextSleep(arg);
				return State.SLEEPING;
			case "RAISE":
				raise(arg);
				return null;
			default:
				throw new UnsupportedOperationException(cmd);
			}
		}

		private boolean hasArg(String cmd) {
			switch (cmd) {
			case "REPEAT":
			case "BREAK":
			case "HALT":
			case "YIELD":
				return false;
			case "GOTO":
			case "CALL":
			case "RAISE":
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

	@SuppressWarnings("serial")
	private static class ScriptRaiseException extends RuntimeException {
		private final String raised;

		public ScriptRaiseException(String raised) {
			super("RAISED: " + raised);

			this.raised = raised;
		}

		public String getRaised() {
			return raised;
		}
	}
}