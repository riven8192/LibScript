package net.indiespot.script.eval;

public class Scope {
	Stepper stepper;
	Scope parent;
	Node exec;
	boolean isFunction;

	public Scope(Stepper stepper, Node exec) {
		this.stepper = stepper;
		this.parent = stepper.scope;
		this.exec = exec;
	}
}