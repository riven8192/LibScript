package net.indiespot.script.crude;

@SuppressWarnings("serial")
class ScriptRaiseException extends RuntimeException {
	final String raised;

	public ScriptRaiseException(String raised) {
		super("RAISED: " + raised);

		this.raised = raised;
	}

	public String getRaised() {
		return raised;
	}
}