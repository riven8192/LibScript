package net.indiespot.script.interp;

import java.util.Arrays;
import java.util.NoSuchElementException;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class EnvMethod {
	EnvClass envClass;
	MethodNode methodNode;
	InsnList instructions;

	public EnvMethod(EnvClass envClass, MethodNode methodNode) {
		this.envClass = envClass;
		this.methodNode = methodNode;
		instructions = (methodNode == null) ? null : methodNode.instructions;

		labelOffsetCache = new int[instructions == null ? 0 : instructions.size()];
		Arrays.fill(labelOffsetCache, -1);
	}

	public ExecFrame prepare(TerminationHandler handler) {
		return new ExecFrame(new ExecFrame(handler), this);
	}

	public ExecFrame call(ExecFrame callsite) {
		return new ExecFrame(callsite, this);
	}

	//

	private int[] labelOffsetCache;

	int findLabel(int instructionPointer) {
		int offset = labelOffsetCache[instructionPointer];
		if(offset == -1)
			labelOffsetCache[instructionPointer] = offset = this.findLabelImpl(instructionPointer);
		return offset;
	}

	private int findLabelImpl(int instructionPointer) {
		JumpInsnNode jump = (JumpInsnNode) instructions.get(instructionPointer);
		for(int i = 0, len = instructions.size(); i < len; i++)
			if(instructions.get(i) == jump.label)
				return i;
		throw new NoSuchElementException("label");
	}
}