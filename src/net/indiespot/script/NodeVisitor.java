package net.indiespot.script;

public interface NodeVisitor {
	public static final class SignalTreeModification extends RuntimeException {
		private static final long serialVersionUID = 5222209206687976911L;
	}

	public void visit(Node node);
}