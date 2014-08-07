package net.indiespot.script.crude;

import net.indiespot.script.crude.CrudeScript.Block;

public abstract class AbstractContext implements Context, Runnable {
	private final Block script;
	protected Eval eval;
	private State state;

	public AbstractContext(Block script) {
		if(script == null)
			throw new NullPointerException();
		this.script = script;
		this.eval = script.eval(this);
	}

	public void start() {
		if(state != null)
			throw new IllegalStateException();
		this.schedule(this, 0L);
	}

	public void resume() {
		if(state != State.WAITING)
			throw new IllegalStateException();
		this.schedule(this, 0L);
	}

	public void call(String function) {
		if(eval != null)
			throw new IllegalStateException();
		eval = script.eval(this, function);
		state = null;
		this.schedule(this, 0L);
	}

	public void run() {
		if(eval == null) {
			return; // inactive...
		}

		while (true) {
			switch (state = eval.tick()) {
			case HALTED:
			case TERMINATED:
				eval = null;
				state = null;
				return; // no reschedule

			case RAISED:
				throw new IllegalStateException();

			case YIELDED:
				this.schedule(this, 1L);
				return; // reschedule next game tick

			case SLEEPING:
				this.schedule(this, nextSleep);
				return; // reschedule with delay

			case WAITING:
				return; // do not reschedule

			case RUNNING:
				break; // continue executing
			}
		}
	}

	@Override
	public boolean query(String var) {
		throw new IllegalStateException("query var: '" + var + "'");
	}

	@Override
	public State signal(String text) {
		System.out.println("SIGNAL " + text);
		return State.RUNNING;
	}

	private long nextSleep;

	@Override
	public void nextSleep(String time) {
		Long got = (Long) eval.get(time, null);
		if(got != null)
			nextSleep = got.longValue();
		else if(time.endsWith("ms"))
			nextSleep = Long.parseLong(time.substring(0, time.length() - 2));
		else if(time.endsWith("s"))
			nextSleep = Long.parseLong(time.substring(0, time.length() - 1)) * 1000;
		else
			throw new IllegalStateException();
	}
}
