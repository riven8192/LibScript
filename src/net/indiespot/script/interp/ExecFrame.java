package net.indiespot.script.interp;

import java.util.Arrays;

public class ExecFrame {
	ExecFrame callsite;
	ExecFrame subframe;
	EnvMethod envMethod;
	int instructionPointer;
	int stackPointer;

	static final int NONE = 0;
	private static final int REF = 1;
	private static final int INT = 2;
	private static final int FLOAT = 3;

	public ExecFrame() {
		this(null, null);
	}

	public ExecFrame(ExecFrame callsite, EnvMethod envMethod) {
		this.callsite = callsite;
		this.envMethod = envMethod;

		int maxLocal = (envMethod == null) ? 0 : envMethod.methodNode.maxLocals;
		int maxStack = (envMethod == null) ? 1 /* return value */: envMethod.methodNode.maxStack;

		localType = new int[maxLocal];
		localData = new int[maxLocal];
		stackType = new int[maxStack];
		stackData = new int[maxStack];
		objLookup = new Object[2];
	}

	private final int[] localType;
	private final int[] localData;
	final int[] stackType;
	final int[] stackData;
	private Object[] objLookup;

	private int ensure(Object v) {
		if(v == null)
			return -1;
		for(int i = 0; true; i++) {
			if(i == objLookup.length)
				objLookup = Arrays.copyOf(objLookup, objLookup.length * 2);
			if(objLookup[i] == null)
				objLookup[i] = v;
			if(objLookup[i] == v)
				return i;
		}
	}

	private Object lookup(int ref) {
		return (ref == -1) ? null : objLookup[ref];
	}

	// ref

	void setRef(int idx, Object v) {
		localType[idx] = REF;
		localData[idx] = ensure(v);
	}

	Object getRef(int idx) {
		if(localType[idx] != REF)
			throw new IllegalStateException();
		return lookup(localData[idx]);
	}

	void pushRef(Object v) {
		if(stackType[stackPointer] != NONE)
			throw new IllegalStateException();
		stackType[stackPointer] = REF;
		stackData[stackPointer++] = ensure(v);
	}

	Object popRef() {
		if(stackType[stackPointer - 1] != REF)
			throw new IllegalStateException();
		stackType[stackPointer - 1] = NONE;
		return lookup(stackData[--stackPointer]);
	}

	// int

	void setInt(int idx, int v) {
		localType[idx] = INT;
		localData[idx] = v;
	}

	int getInt(int idx) {
		if(localType[idx] != INT)
			throw new IllegalStateException();
		return localData[idx];
	}

	void pushInt(int v) {
		if(stackType[stackPointer] != NONE)
			throw new IllegalStateException();
		stackType[stackPointer] = INT;
		stackData[stackPointer++] = v;
	}

	int popInt() {
		if(stackType[stackPointer - 1] != INT)
			throw new IllegalStateException();
		stackType[stackPointer - 1] = NONE;
		return stackData[--stackPointer];
	}

	// float

	void setFloat(int idx, float v) {
		localType[idx] = FLOAT;
		localData[idx] = Float.floatToRawIntBits(v);
	}

	float getFloat(int idx) {
		if(localType[idx] != FLOAT)
			throw new IllegalStateException();
		return Float.intBitsToFloat(localData[idx]);
	}

	void pushFloat(float v) {
		if(stackType[stackPointer] != NONE)
			throw new IllegalStateException();
		stackType[stackPointer] = FLOAT;
		stackData[stackPointer++] = Float.floatToRawIntBits(v);
	}

	float popFloat() {
		if(stackType[stackPointer - 1] != FLOAT)
			throw new IllegalStateException();
		stackType[stackPointer - 1] = NONE;
		return Float.intBitsToFloat(stackData[--stackPointer]);
	}
}