package net.indiespot.script;

import java.util.ArrayList;
import java.util.List;

import net.indiespot.script.Tokenizer.ParserState;
import net.indiespot.script.Tokenizer.Rules;

public class Node {
	public int lineNumber;
	public String token;
	public Node parent;
	public final List<Node> children;

	public Node prev, next;
	public Type type;
	public Object typeConst;

	public static enum Type {
		BOOL, INT, FLOAT, NAME, ASSIGN, DEREFERENCE
	}

	public Node(ParserState state, Node parent) {
		if(state != null) {
			this.lineNumber = state.lineNumber;
			this.token = state.token();
		}
		else if(parent != null) {
			this.lineNumber = parent.lineNumber;
			this.token = parent.token;
		}
		else {
			this.lineNumber = -1;
		}

		this.parent = parent;
		this.children = new ArrayList<>();

		if(parent != null) {
			parent.children.add(this);
		}
	}

	public Node copy() {
		Node copy = new Node(null, null);
		copy.lineNumber = this.lineNumber;
		copy.token = this.token;
		for(Node child : children)
			child.copy().attachTo(copy);
		return copy;
	}

	public void link() {
		if(this.parent != null) {
			this.prev = this.prev();
			this.next = this.next();

			if(parent != null && "if".equals(parent.token))
				this.next = null; // don't jump from end of if-block, to beginning of else-block
		}

		if(token == null) {
			type = null;
			typeConst = null;
		}
		else if(PostProcessor.isBoolean(token)) {
			type = Type.BOOL;
			typeConst = Boolean.parseBoolean(token);
		}
		else if(PostProcessor.isInteger(token, true)) {
			type = Type.INT;
			typeConst = Integer.parseInt(token);
		}
		else if(PostProcessor.isFloat(token)) {
			type = Type.FLOAT;
			typeConst = Float.parseFloat(token);
		}
		else if(PostProcessor.isVarName(token)) {
			type = Type.NAME;
			typeConst = token;
		}
		else if(token.equals("=")) {
			type = Type.ASSIGN;
			typeConst = null;
		}
		else if(token.equals(".")) {
			type = Type.DEREFERENCE;
			typeConst = null;
		}
		else {
			type = null;
			typeConst = null;
		}

		for(Node child : children)
			child.link();
	}

	public int indexOf() {
		return parent.children.indexOf(this);
	}

	public Node firstChild() {
		return children.isEmpty() ? null : children.get(0);
	}

	public Node lastChild() {
		return children.isEmpty() ? null : children.get(children.size() - 1);
	}

	public Node firstSibling() {
		return parent.firstChild();
	}

	public Node lastSibling() {
		return parent.lastChild();
	}

	public Node prev() {
		if(parent == null)
			throw new IllegalStateException();
		int io = this.indexOf();
		if(io == -1)
			throw new IllegalStateException();
		if(io == 0)
			return null;
		return parent.children.get(io - 1);
	}

	public Node next() {
		if(parent == null)
			throw new IllegalStateException();
		int io = this.indexOf();
		if(io == -1)
			throw new IllegalStateException();
		if(io == parent.children.size() - 1)
			return null;
		return parent.children.get(io + 1);
	}

	public void attachTo(Node newParent) {
		if(parent != null)
			throw new IllegalStateException("parent defined");

		parent = newParent;
		parent.children.add(this);
	}

	public void attachTo(Node newParent, int index) {
		if(parent != null)
			throw new IllegalStateException("parent defined");

		parent = newParent;
		parent.children.add(index, this);
	}

	public Node detach() {
		if(parent == null)
			throw new IllegalStateException();

		if(!parent.children.remove(this))
			throw new IllegalStateException();
		Node p = parent;
		parent = null;
		return p;
	}

	public void consume(ParserState state) {
		while (state.hasNext()) {
			String token = state.advance().token();

			if(token.equals(Tokenizer.line_break)) {
				state.lineNumber++;
			}
			else if(Rules.isScopeOpener(token)) {
				new Node(state, this).consume(state);
			}
			else if(Rules.isScopeCloser(token)) {
				return; // hand over control to parent
			}
			else {
				new Node(state, this);
			}
		}

		if(this.parent != null)
			throw new IllegalStateException("scope depth: " + this.depth());
	}

	public void print() {
		StringBuilder sb = new StringBuilder();
		sb.append('L').append(lineNumber).append(":\t");
		for(int i = 0, len = this.depth(); i < len; i++)
			sb.append('\t');
		if(token == null)
			sb.append("--->");
		else
			sb.append('[').append(token).append(']');
		//if(token != null)
		System.out.println(sb);

		for(Node child : children) {
			child.print();
		}
	}

	public int depth() {
		int depth = 0;
		for(Node node = this; node.parent != null; node = node.parent) {
			depth++;
		}
		return depth;
	}

	public void visit(NodeVisitor visitor) {
		visitor.visit(this);

		for(Node child : children) {
			child.visit(visitor);
		}
	}

	@Override
	public String toString() {
		return "Node[" + token + "] @ depth=" + this.depth() + ", parent.children=" + (parent == null ? -1 : parent.children.size());
	}
}