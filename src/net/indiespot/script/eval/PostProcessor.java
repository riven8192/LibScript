package net.indiespot.script.eval;

import java.util.HashSet;
import java.util.Set;

public class PostProcessor {

	static void mergeChildrenInParent(Node node) {
		int io = node.parent.children.indexOf(node);
		while (!node.children.isEmpty()) {
			Node child = node.children.get(node.children.size() - 1);
			child.detach();
			child.attachTo(node.parent, io);
		}
		node.detach();
	}

	static boolean isVarName(String token) {
		if(token == null || token.isEmpty())
			return false;

		for(int i = 0, len = token.length(); i < len; i++) {
			char c = token.charAt(i);
			if((c < 'a' || c > 'z') && //
					(c < 'A' || c > 'Z') && //
					(i == 0 || (c < '0' || c > '9')) //
					&& (c != '_' && c != '$')) {
				return false;
			}
		}
		return true;
	}

	static boolean isBoolean(String token) {
		return "true".equals(token) || "false".equals(token);
	}

	static boolean isInteger(String token, boolean allowSign) {
		if(token == null || token.isEmpty())
			return false;
		if(allowSign && token.length() >= 2) {
			if(token.startsWith("-") || token.startsWith("+")) {
				return isInteger(token.substring(1), false);
			}
		}
		for(int i = 0, len = token.length(); i < len; i++) {
			char c = token.charAt(i);
			if(c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	static boolean isFloat(String token) {
		if(token == null || token.isEmpty())
			return false;
		int io = token.indexOf('.');
		if(io == -1)
			return false;

		String sL = token.substring(0, io);
		String sR = token.substring(io + 1);
		boolean bL = isInteger(sL, true);
		boolean bR = isInteger(sR, false);
		return bL || bR;
	}

	static boolean rewrite(Node root, NodeVisitor visitor) {
		boolean dirty = false;
		while (true) {
			try {
				root.visit(visitor);
				break;
			}
			catch (NodeVisitor.SignalTreeModification mod) {
				dirty = true;
				continue;
			}
		}
		return dirty;
	}

	public static Node process(Node root) {

		class ControlFlowRewrite implements NodeVisitor {

			@Override
			public void visit(Node node) {
				if(node.token == null)
					return;

				if(!Tokenizer.control_flow_tokens.contains(node.token))
					return;

				if(node.token.equals("return") || node.token.equals("yield")) {
					if(node == node.lastSibling())
						return; // already rewritten
					if(node != node.firstSibling() && //
							!"{".equals(node.prev().token) && //
							!Tokenizer.control_flow_tokens.contains(node.prev().token))
						throw new IllegalStateException(node.prev().token);

					// move return/yield to end of statement
					Node p = node.detach();
					node.attachTo(p);

					if(node != node.lastSibling())
						throw new IllegalStateException();

					throw new SignalTreeModification();
				}

				Node condScope = node.next();
				if(condScope == null || !condScope.token.equals("("))
					return;

				Node bodyScope = condScope.next();
				if(bodyScope == null || !bodyScope.token.equals("{"))
					throw new IllegalStateException("missing '" + node.token + "' body");

				Node elseStmt = node.token.equals("if") ? bodyScope.next() : null;

				if(node.token.equals("while"))
					condScope.copy().attachTo(bodyScope);
				condScope.detach();
				condScope.attachTo(node.parent, node.indexOf());

				condScope.token = null;

				bodyScope.detach();
				bodyScope.attachTo(node);

				if(elseStmt != null && "else".equals(elseStmt.token)) {
					Node elseScope = elseStmt.next();
					if(elseScope != null && "{".equals(elseScope.token)) {
						elseStmt.detach();
						elseStmt.attachTo(node);

						elseScope.detach();
						elseScope.attachTo(elseStmt);

						mergeChildrenInParent(elseScope);
						elseStmt.token = "{";
					}
					else {
						throw new IllegalStateException("missing '" + elseStmt.token + "' body");
					}
				}

				throw new SignalTreeModification();
			}
		}

		class FuncFinder implements NodeVisitor {

			@Override
			public void visit(Node node) {
				if(node.token == null)
					return;

				if(!isVarName(node.token))
					return;

				Node params = node.next();
				if(params == null)
					return;

				if(!"(".equals(params.token))
					return;

				int io = node.parent.children.indexOf(node);

				params.detach();
				while (!params.children.isEmpty()) {
					Node param = params.children.get(0);
					param.detach();
					if(!",".equals(param.token))
						param.attachTo(node.parent, io++);
				}

				new Node(null, node).token = "{";

				throw new SignalTreeModification();
			}
		}

		class InfixOp implements NodeVisitor {
			private final Set<String> ops;

			public InfixOp(String... args) {
				ops = new HashSet<>();
				for(String op : args)
					ops.add(op);
			}

			@Override
			public void visit(Node node) {
				if(node.token == null)
					return;

				if(!ops.contains(node.token))
					return;

				if(node.parent.children.size() <= 3)
					return;

				System.out.println("op: " + node);

				Node prev = node.prev();
				Node next = node.next();

				prev.detach();
				next.detach();

				prev.attachTo(node);
				new Node(null, node);
				next.attachTo(node);

				node.token = null;

				throw new SignalTreeModification();
			}
		}

		class ReversePolishNotation implements NodeVisitor {
			@Override
			public void visit(Node node) {
				if(!Tokenizer.infix_ops.contains(node.token))
					return;

				if(node.parent.children.size() != 3)
					throw new IllegalStateException();

				Node self = node.parent.children.get(1);
				if(node != self)
					return;
				Node parent = self.detach();
				self.attachTo(parent);

				throw new SignalTreeModification();
			}
		}

		class ScopeFlattener implements NodeVisitor {
			@Override
			public void visit(Node node) {
				if(node.parent == null)
					return;
				if("if".equals(node.parent.token))
					return; // due to else
				if(node.token != null && !node.token.equals("(") && !node.token.equals("{"))
					return;

				//if(node.token == null && node.children.size() == 1) {
				//	mergeChildrenInParent(node.children.get(0));
				//}

				mergeChildrenInParent(node);

				throw new SignalTreeModification();
			}
		}

		class BraceCleanup implements NodeVisitor {
			@Override
			public void visit(Node node) {
				if("{".equals(node.token)) {
					node.token = null;
					throw new SignalTreeModification();
				}
			}
		}

		System.out.println("-> handle control-flow");
		rewrite(root, new ControlFlowRewrite());
		root.print();
		System.out.println();

		System.out.println("-> handle functions");
		rewrite(root, new FuncFinder());
		root.print();
		System.out.println();

		if(true) {
			System.out.println("-> apply operator precedence");

			rewrite(root, new InfixOp("."));
			rewrite(root, new InfixOp("*", "/", "%"));
			rewrite(root, new InfixOp("+", "-"));
			rewrite(root, new InfixOp("<<"));
			rewrite(root, new InfixOp(">>"));
			rewrite(root, new InfixOp(">>>"));
			rewrite(root, new InfixOp("<", "<="));
			rewrite(root, new InfixOp(">", ">="));
			rewrite(root, new InfixOp("=="));
			rewrite(root, new InfixOp("!="));
			rewrite(root, new InfixOp("&"));
			rewrite(root, new InfixOp("^"));
			rewrite(root, new InfixOp("|"));
			rewrite(root, new InfixOp("&&"));
			rewrite(root, new InfixOp("||"));
			rewrite(root, new InfixOp("?", ":"));
			rewrite(root, new InfixOp("="));
			rewrite(root, new InfixOp("*=", "/=", "+=", "-=", "%=", "<<=", ">>=", ">>>>=", "&=", "^=", "|="));

			root.print();
			System.out.println();
		}

		if(true) {
			System.out.println("-> apply reverse polish notation transformation");
			rewrite(root, new ReversePolishNotation());
			root.print();
			System.out.println();
		}

		if(true) {
			System.out.println("-> flatten tree nodes where possible");
			rewrite(root, new ScopeFlattener());
			root.print();
			System.out.println();
		}

		if(true) {
			System.out.println("-> brace cleanup");
			rewrite(root, new BraceCleanup());
			root.print();
			System.out.println();
		}

		root.link();

		return root;
	}
}
