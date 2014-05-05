package net.indiespot.script;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {

	static final String line_break = "\n";

	static final Set<String> control_flow_tokens = new HashSet<>();
	static final Set<String> infix_ops = new HashSet<>();
	static final Set<String> math_infix_ops = new HashSet<>();
	static final Set<String> bool_math_infix_ops = new HashSet<>();
	static final Set<String> int_math_infix_ops = new HashSet<>();
	static final Set<String> float_math_infix_ops = new HashSet<>();

	static {
		control_flow_tokens.add("if");
		control_flow_tokens.add("else");
		control_flow_tokens.add("while");
		control_flow_tokens.add("return");
		control_flow_tokens.add("yield");

		bool_math_infix_ops.add("==");
		bool_math_infix_ops.add("!=");

		int_math_infix_ops.add("*");
		int_math_infix_ops.add("/");
		int_math_infix_ops.add("%");
		int_math_infix_ops.add("+");
		int_math_infix_ops.add("-");
		int_math_infix_ops.add(">>>");
		int_math_infix_ops.add(">>");
		int_math_infix_ops.add("<<");
		int_math_infix_ops.add("&");
		int_math_infix_ops.add("|");
		int_math_infix_ops.add("^");
		for(String op : new ArrayList<>(int_math_infix_ops))
			int_math_infix_ops.add(op + "=");
		int_math_infix_ops.add("==");
		int_math_infix_ops.add("!=");
		int_math_infix_ops.add("<");
		int_math_infix_ops.add(">");
		int_math_infix_ops.add("<=");
		int_math_infix_ops.add(">=");

		float_math_infix_ops.add("*");
		float_math_infix_ops.add("/");
		float_math_infix_ops.add("%");
		float_math_infix_ops.add("+");
		float_math_infix_ops.add("-");
		for(String op : new ArrayList<>(float_math_infix_ops))
			float_math_infix_ops.add(op + "=");
		float_math_infix_ops.add("==");
		float_math_infix_ops.add("!=");
		float_math_infix_ops.add("<");
		float_math_infix_ops.add(">");
		float_math_infix_ops.add("<=");
		float_math_infix_ops.add(">=");

		math_infix_ops.addAll(bool_math_infix_ops);
		math_infix_ops.addAll(int_math_infix_ops);
		math_infix_ops.addAll(float_math_infix_ops);

		infix_ops.add(".");
		infix_ops.add("=");
		infix_ops.addAll(math_infix_ops);

	}

	public static class Rules {
		public static boolean isScopeOpener(String token) {
			return token.equals("{") || token.equals("(");
		}

		public static boolean isScopeCloser(String token) {
			return token.equals("}") || token.equals(")");
		}
	}

	public static class ParserState {
		List<String> tokens;
		int lineNumber = 1;
		int tokenIndex = -1;

		public ParserState(List<String> tokens) {
			this.tokens = tokens;
		}

		public ParserState advance() {
			tokenIndex++;
			return this;
		}

		public String token() {
			if(tokenIndex == -1)
				return null;
			return tokens.get(tokenIndex);
		}

		public boolean hasNext() {
			return tokenIndex < tokens.size() - 1;
		}

		public void raise(String msg) {
			throw new IllegalStateException("line " + lineNumber + ": " + msg);
		}
	}

	public static Node parse(String eval) {

		List<String> tokens = tokenize(eval);

		for(String token : tokens)
			System.out.println("[" + token + "]");

		ParserState state = new ParserState(tokens);

		Node root = new Node(state, null);
		root.consume(state);
		System.out.println();

		System.out.println("-> tokenized input");
		root.print();
		System.out.println();

		class Stitcher implements NodeVisitor {

			Set<String> opEq = new HashSet<>();
			{
				opEq.add("+");
				opEq.add("-");
				opEq.add("*");
				opEq.add("/");
				opEq.add("%");
				opEq.add("&");
				opEq.add("|");
				opEq.add("^");
				opEq.add(">>>");
				opEq.add(">>");
				opEq.add("<<");

				opEq.add("<");
				opEq.add(">");
				opEq.add("=");
				opEq.add("!");
			}

			@Override
			public void visit(Node node) {
				if(node.token == null)
					return;

				if(opEq.contains(node.token) && node.next() != null && "=".equals(node.next().token)) {
					node.next().detach();
					node.token = node.token + "=";
					throw new SignalTreeModification();
				}

				if(node.token.equals(".")) {
					Node prev = node.prev();
					Node next = node.next();

					String sL = (prev == null) ? null : prev.token.trim();
					String sR = (next == null) ? null : next.token.trim();

					boolean iL = PostProcessor.isInteger(sL, true);
					boolean iR = PostProcessor.isInteger(sR, false);

					if(iL | iR) {
						if(iL)
							prev.detach();
						if(iR)
							next.detach();

						node.token = (iL ? sL : "") + "." + (iR ? sR : "");

						throw new SignalTreeModification();
					}
				}
			}
		}

		class WhiteSpaceRemover implements NodeVisitor {

			@Override
			public void visit(Node node) {
				if(node.token == null)
					return;

				if(node.token.trim().isEmpty()) {// || node.token.equals(";")) {
					PostProcessor.mergeChildrenInParent(node);

					throw new SignalTreeModification();
				}
			}
		}

		class StatementIsolator implements NodeVisitor {

			@Override
			public void visit(Node node) {
				int stmts = 0;
				for(Node child : node.children)
					if(";".equals(child.token))
						stmts++;

				if(stmts == 0) {
					return;
				}

				if(stmts == 1 && ";".equals(node.lastSibling().token)) {
					node.lastSibling().detach();
					return;
				}

				List<Node> holders = new ArrayList<>();
				Node holder = null;

				while (!node.children.isEmpty()) {
					Node child = node.children.get(0);
					child.detach();

					if(holder == null) {
						holder = new Node(null, null);
						holder.lineNumber = child.lineNumber;
						holders.add(holder);
					}

					if(";".equals(child.token)) {
						holder = null;
					}
					else {
						child.attachTo(holder);
					}
				}

				for(Node h : holders) {
					h.attachTo(node);
				}

				throw new SignalTreeModification();
			}
		}

		System.out.println("-> stitch floats back together");
		PostProcessor.rewrite(root, new Stitcher());
		root.print();
		System.out.println();

		System.out.println("-> remove whitespace");
		PostProcessor.rewrite(root, new WhiteSpaceRemover());
		root.print();
		System.out.println();

		System.out.println("-> isolate statements");
		PostProcessor.rewrite(root, new StatementIsolator());
		root.print();
		System.out.println();

		return root;
	}

	private static List<String> tokenize(String input) {
		input = input.replace("\r\n", "\n").replace("\r", "\n");

		List<String> tokens = new ArrayList<>();

		StringBuilder pattern = new StringBuilder();
		for(char c : "(){}.+-*/%&|^;,!=<>".toCharArray()) {
			pattern.append("\\\\").append(c);
		}

		Matcher m = Pattern.compile("[" + pattern.toString() + "\\s]").matcher(input);

		int last = 0;
		while (m.find()) {
			if(last != m.start())
				tokens.add(input.substring(last, m.start()));
			if(!m.group().isEmpty())
				tokens.add(m.group());
			last = m.end();
		}
		if(last != input.length())
			tokens.add(input.substring(last));
		return tokens;
	}
}
