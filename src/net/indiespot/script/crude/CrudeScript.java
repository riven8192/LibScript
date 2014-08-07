package net.indiespot.script.crude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

public class CrudeScript {
	public static Block compile(String file, String code) {
		Block block = new Block(file);
		block.feed(code);
		block.compile();
		System.out.println(block.toString());
		return block;
	}

	public static class Trace {
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

	public static class Code {
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

	public static class Instruction extends Code {
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
		return "#" + skipOverCounter.incrementAndGet() + "";
	}

	public static class Block extends Code {
		public final String name;
		public final List<Code> codes;
		private final List<Code> endCodes;
		private String funcJumper;

		public Block(String file) {
			this(new Trace(file, 0), null, "<root>");
		}

		Block(Trace trace, Block parent, String name) {
			super(trace, parent);
			this.name = (name == null || name.isEmpty()) ? "<anonymous>" : name;
			this.codes = new ArrayList<>();
			this.endCodes = new ArrayList<>();
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

		private boolean feed(String line, Trace trace) {
			int io = line.indexOf('#');
			if(io != -1)
				line = line.substring(0, io);
			line = line.trim();
			if(line.isEmpty())
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
			else if(line.startsWith("FUNCTION ")) {
				if(funcJumper != null)
					throw new IllegalStateException();
				funcJumper = createSkipOverLabel();
				String callName = line.substring("FUNCTION".length()).trim();
				new Instruction(new Trace(null, 0), this, "GOTO " + funcJumper, "JUMP");
				new Block(trace, this, callName);
			}
			else if(line.startsWith("WHILE ") && line.endsWith(" DO")) {
				String cond = line.substring("WHILE".length()).trim();
				cond = cond.substring(0, cond.length() - "DO".length()).trim();
				boolean not = cond.startsWith("NOT ");
				if(not)
					cond = cond.substring("NOT".length()).trim();

				Block block = new Block(trace, this, "<WHILE>");
				block.feed("IF " + (not ? "" : "NOT ") + cond + " BREAK", trace);
				new Instruction(trace, block, "LOOP", "JUMP");
				block.endCodes.add(block.codes.remove(block.codes.size() - 1));
			}
			else if(line.startsWith("IF ") && line.endsWith(" THEN")) {
				String cond = line.substring("IF".length()).trim();
				cond = cond.substring(0, cond.length() - "THEN".length()).trim();
				boolean not = cond.startsWith("NOT ");
				if(not)
					cond = cond.substring("NOT".length()).trim();

				Block block = new Block(trace, this, "<IF>");
				block.feed("IF " + (not ? "" : "NOT ") + cond + " BREAK", trace);
			}
			else if(line.equals("END")) {
				return true; // return control to parent block
			}
			else if(line.endsWith(":")) {
				String label = line.substring(0, line.length() - 1).trim();
				new Instruction(trace, this, label, "LABEL");
			}
			else if(line.equals("BREAK")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.equals("LOOP")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.equals("HALT")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.equals("WAIT")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("SLEEP ")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.equals("YIELD")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("THROW ")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("GOTO ")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("IF ")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("CALL ")) {
				new Instruction(trace, this, line, "JUMP");
			}
			else if(line.startsWith("CATCH ")) {
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
			mergeEndCodes();
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

		private void mergeEndCodes() {
			codes.addAll(endCodes);
			endCodes.clear();

			for(int i = codes.size() - 1; i >= 0; i--) {
				Code code = codes.get(i);

				if(code instanceof Block) {
					((Block) code).mergeEndCodes();
				}
			}
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
		private final Map<String, Instruction> catch2target = new HashMap<>();
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

		Instruction findCatch(String name) {
			Instruction instr = catch2target.get(name);
			if(instr != null)
				return instr;

			for(Code code : codes) {
				if(!(code instanceof Instruction))
					continue;
				instr = (Instruction) code;
				if(!"JUMP".equals(instr.type))
					continue;
				if(!instr.parts[0].equals("CATCH"))
					continue;
				if(!instr.parts[1].equals(name))
					continue;
				catch2target.put(name, instr);
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
}