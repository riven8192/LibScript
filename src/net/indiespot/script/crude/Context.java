package net.indiespot.script.crude;

public interface Context {
	public void schedule(Runnable task, long delay);

	public State signal(String text);

	public boolean query(String var);

	public void nextSleep(String time);
}