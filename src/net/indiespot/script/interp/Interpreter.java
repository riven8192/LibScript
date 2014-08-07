package net.indiespot.script.interp;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class Interpreter {
	public static enum ExecState {
		RUNNING, YIELDED, SLEEPING, SUSPENDED, TERMINATED;
	}

	public static ExecState step(ExecFrame frame) {
		if(frame.subframe != null) {
			ExecState state = step(frame.subframe);
			if(state == ExecState.TERMINATED)
				state = ExecState.RUNNING;
			return state;
		}

		int ip = frame.instructionPointer;
		int ipOld = ip;
		AbstractInsnNode node = frame.envMethod.instructions.get(ip);

		switch (node.getOpcode()) {
		case Opcodes.FCONST_0:
		case Opcodes.FCONST_1:
		case Opcodes.FCONST_2: {
			frame.pushFloat(node.getOpcode() - Opcodes.FCONST_0);
			break;
		}
		case Opcodes.ICONST_M1:
		case Opcodes.ICONST_0:
		case Opcodes.ICONST_1:
		case Opcodes.ICONST_2:
		case Opcodes.ICONST_3:
		case Opcodes.ICONST_4:
		case Opcodes.ICONST_5: {
			frame.pushInt(node.getOpcode() - Opcodes.ICONST_M1 - 1);
			break;
		}
		case Opcodes.ACONST_NULL: {
			frame.pushRef(null);
			break;
		}
		case Opcodes.BIPUSH:
		case Opcodes.SIPUSH: {
			frame.pushInt(((IntInsnNode) node).operand);
			break;
		}
		case Opcodes.LDC: {
			Object cst = ((LdcInsnNode) node).cst;
			if(cst instanceof Integer)
				frame.pushFloat(((Integer) cst).intValue());
			else if(cst instanceof Float)
				frame.pushFloat(((Float) cst).floatValue());
			else if(cst instanceof String)
				frame.pushRef(cst);
			else
				throw new IllegalStateException();
			break;
		}
		case Opcodes.I2F: {
			frame.pushFloat(frame.popInt());
			break;
		}
		case Opcodes.F2I: {
			frame.pushInt((int) frame.popFloat());
			break;
		}
		case Opcodes.INEG: {
			int var = ((VarInsnNode) node).var;
			frame.pushInt(~frame.getInt(var));
			break;
		}
		case Opcodes.FNEG: {
			int var = ((VarInsnNode) node).var;
			frame.pushFloat(-frame.getFloat(var));
			break;
		}
		case Opcodes.ILOAD: {
			int var = ((VarInsnNode) node).var;
			frame.pushInt(frame.getInt(var));
			break;
		}
		case Opcodes.FLOAD: {
			int var = ((VarInsnNode) node).var;
			frame.pushFloat(frame.getFloat(var));
			break;
		}
		case Opcodes.ALOAD: {
			int var = ((VarInsnNode) node).var;
			frame.pushRef(frame.getRef(var));
			break;
		}
		case Opcodes.ISTORE: {
			int val = frame.popInt();
			int var = ((VarInsnNode) node).var;
			frame.setInt(var, val);
			break;
		}
		case Opcodes.FSTORE: {
			float val = frame.popFloat();
			int var = ((VarInsnNode) node).var;
			frame.setFloat(var, val);
			break;
		}
		case Opcodes.ASTORE: {
			Object val = frame.popRef();
			int var = ((VarInsnNode) node).var;
			frame.setRef(var, val);
			break;
		}
		case Opcodes.IINC: {
			int var = ((IincInsnNode) node).var;
			int incr = ((IincInsnNode) node).incr;
			frame.setInt(var, frame.getInt(var) + incr);
			break;
		}
		case Opcodes.IADD: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			frame.pushInt(op2 + op1);
			break;
		}
		case Opcodes.FADD: {
			float op1 = frame.popFloat();
			float op2 = frame.popFloat();
			frame.pushFloat(op2 + op1);
			break;
		}
		case Opcodes.ISUB: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			frame.pushInt(op2 - op1);
			break;
		}
		case Opcodes.FSUB: {
			float op1 = frame.popFloat();
			float op2 = frame.popFloat();
			frame.pushFloat(op2 - op1);
			break;
		}
		case Opcodes.IMUL: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			frame.pushInt(op2 * op1);
			break;
		}
		case Opcodes.FMUL: {
			float op1 = frame.popFloat();
			float op2 = frame.popFloat();
			frame.pushFloat(op2 * op1);
			break;
		}
		case Opcodes.IDIV: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			frame.pushInt(op2 / op1);
			break;
		}
		case Opcodes.FDIV: {
			float op1 = frame.popFloat();
			float op2 = frame.popFloat();
			frame.pushFloat(op2 / op1);
			break;
		}
		case Opcodes.IREM: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			frame.pushInt(op2 % op1);
			break;
		}
		case Opcodes.FREM: {
			float op1 = frame.popFloat();
			float op2 = frame.popFloat();
			frame.pushFloat(op2 % op1);
			break;
		}
		case Opcodes.IAND: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			frame.pushInt(op2 & op1);
			break;
		}
		case Opcodes.IOR: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			frame.pushInt(op2 | op1);
			break;
		}
		case Opcodes.IXOR: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			frame.pushInt(op2 ^ op1);
			break;
		}
		case Opcodes.ISHL: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			frame.pushInt(op2 << op1);
			break;
		}
		case Opcodes.ISHR: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			frame.pushInt(op2 >> op1);
			break;
		}
		case Opcodes.IUSHR: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			frame.pushInt(op2 >>> op1);
			break;
		}
		// IF**
		case Opcodes.GOTO: {
			ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IFNULL: {
			if(frame.popRef() == null)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IFNONNULL: {
			if(frame.popRef() != null)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IFEQ: {
			if(frame.popInt() == 0)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IFNE: {
			if(frame.popInt() != 0)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IFLT: {
			if(frame.popInt() < 0) {
				ip = frame.envMethod.findLabel(ip);
			}
			break;
		}
		case Opcodes.IFLE: {
			if(frame.popInt() <= 0)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IFGT: {
			if(frame.popInt() > 0)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IFGE: {
			if(frame.popInt() >= 0)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.FCMPL: {
			float op1 = frame.popFloat();
			float op2 = frame.popFloat();
			frame.pushInt(op2 < op1 ? 1 : 0);
			break;
		}
		case Opcodes.FCMPG: {
			float op1 = frame.popFloat();
			float op2 = frame.popFloat();
			frame.pushInt(op2 > op1 ? 1 : 0);
			break;
		}
		// IF_[IA]CMP**
		case Opcodes.IF_ACMPEQ: {
			Object op1 = frame.popRef();
			Object op2 = frame.popRef();
			if(op2 == op1)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IF_ACMPNE: {
			Object op1 = frame.popRef();
			Object op2 = frame.popRef();
			if(op2 != op1)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IF_ICMPEQ: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			if(op2 == op1)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IF_ICMPNE: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			if(op2 == op1)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IF_ICMPGT: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			if(op2 > op1)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IF_ICMPGE: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			if(op2 >= op1)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IF_ICMPLT: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			if(op2 < op1)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.IF_ICMPLE: {
			int op1 = frame.popInt();
			int op2 = frame.popInt();
			if(op2 <= op1)
				ip = frame.envMethod.findLabel(ip);
			break;
		}
		case Opcodes.CHECKCAST: {
			// OK
			Object v = frame.popRef();
			frame.pushRef(v);
			break;
		}
		// STACK
		case Opcodes.SWAP: {
			int t = frame.stackType[frame.stackPointer - 2];
			int d = frame.stackData[frame.stackPointer - 2];

			frame.stackType[frame.stackPointer - 2] = frame.stackType[frame.stackPointer - 1];
			frame.stackData[frame.stackPointer - 2] = frame.stackData[frame.stackPointer - 1];

			frame.stackType[frame.stackPointer - 1] = t;
			frame.stackData[frame.stackPointer - 1] = d;
			break;
		}
		case Opcodes.POP: {
			frame.stackType[--frame.stackPointer] = ExecFrame.NONE;
			break;
		}
		case Opcodes.POP2: {
			frame.stackType[--frame.stackPointer] = ExecFrame.NONE;
			frame.stackType[--frame.stackPointer] = ExecFrame.NONE;
			break;
		}
		case Opcodes.DUP: {
			frame.stackType[frame.stackPointer] = frame.stackType[frame.stackPointer - 1];
			frame.stackData[frame.stackPointer] = frame.stackData[frame.stackPointer - 1];
			frame.stackPointer++;
			break;
		}
		case Opcodes.DUP_X1: {
			int t1 = frame.stackType[--frame.stackPointer];
			int d1 = frame.stackData[frame.stackPointer];
			int t2 = frame.stackType[--frame.stackPointer];
			int d2 = frame.stackData[frame.stackPointer];

			frame.stackType[frame.stackPointer] = t1;
			frame.stackData[frame.stackPointer++] = d1;
			frame.stackType[frame.stackPointer] = t2;
			frame.stackData[frame.stackPointer++] = d2;
			frame.stackType[frame.stackPointer] = t1;
			frame.stackData[frame.stackPointer++] = d1;
			break;
		}
		case Opcodes.DUP_X2: {
			int t1 = frame.stackType[--frame.stackPointer];
			int d1 = frame.stackData[frame.stackPointer];
			int t2 = frame.stackType[--frame.stackPointer];
			int d2 = frame.stackData[frame.stackPointer];
			int t3 = frame.stackType[--frame.stackPointer];
			int d3 = frame.stackData[frame.stackPointer];

			frame.stackType[frame.stackPointer] = t1;
			frame.stackData[frame.stackPointer++] = d1;
			frame.stackType[frame.stackPointer] = t3;
			frame.stackData[frame.stackPointer++] = d3;
			frame.stackType[frame.stackPointer] = t2;
			frame.stackData[frame.stackPointer++] = d2;
			frame.stackType[frame.stackPointer] = t1;
			frame.stackData[frame.stackPointer++] = d1;
			break;
		}
		case Opcodes.DUP2: {
			frame.stackType[frame.stackPointer] = frame.stackType[frame.stackPointer - 2];
			frame.stackData[frame.stackPointer] = frame.stackData[frame.stackPointer - 2];
			frame.stackPointer++;
			frame.stackType[frame.stackPointer] = frame.stackType[frame.stackPointer - 2];
			frame.stackData[frame.stackPointer] = frame.stackData[frame.stackPointer - 2];
			frame.stackPointer++;
			break;
		}
		case Opcodes.DUP2_X1: {
			int t1 = frame.stackType[--frame.stackPointer];
			int d1 = frame.stackData[frame.stackPointer];
			int t2 = frame.stackType[--frame.stackPointer];
			int d2 = frame.stackData[frame.stackPointer];
			int t3 = frame.stackType[--frame.stackPointer];
			int d3 = frame.stackData[frame.stackPointer];

			frame.stackType[frame.stackPointer] = t2;
			frame.stackData[frame.stackPointer++] = d2;
			frame.stackType[frame.stackPointer] = t1;
			frame.stackData[frame.stackPointer++] = d1;
			frame.stackType[frame.stackPointer] = t3;
			frame.stackData[frame.stackPointer++] = d3;
			frame.stackType[frame.stackPointer] = t2;
			frame.stackData[frame.stackPointer++] = d2;
			frame.stackType[frame.stackPointer] = t1;
			frame.stackData[frame.stackPointer++] = d1;
			break;
		}
		case Opcodes.DUP2_X2: {
			int t1 = frame.stackType[--frame.stackPointer];
			int d1 = frame.stackData[frame.stackPointer];
			int t2 = frame.stackType[--frame.stackPointer];
			int d2 = frame.stackData[frame.stackPointer];
			int t3 = frame.stackType[--frame.stackPointer];
			int d3 = frame.stackData[frame.stackPointer];
			int t4 = frame.stackType[--frame.stackPointer];
			int d4 = frame.stackData[frame.stackPointer];

			frame.stackType[frame.stackPointer] = t2;
			frame.stackData[frame.stackPointer++] = d2;
			frame.stackType[frame.stackPointer] = t1;
			frame.stackData[frame.stackPointer++] = d1;
			frame.stackType[frame.stackPointer] = t4;
			frame.stackData[frame.stackPointer++] = d4;
			frame.stackType[frame.stackPointer] = t3;
			frame.stackData[frame.stackPointer++] = d3;
			frame.stackType[frame.stackPointer] = t2;
			frame.stackData[frame.stackPointer++] = d2;
			frame.stackType[frame.stackPointer] = t1;
			frame.stackData[frame.stackPointer++] = d1;
			break;
		}
		// INVOKE
		case Opcodes.INVOKESTATIC: {
			MethodInsnNode invoke = (MethodInsnNode) node;
			if(invoke.owner.equals(Scheduler.class.getName().replace('.', '/'))) {
				frame.instructionPointer++;
				if(invoke.name.equals("yield")) {
					Scheduler.signalYield(frame);
					return ExecState.YIELDED;
				}
				if(invoke.name.equals("sleep")) {
					Scheduler.signalSleep(frame, frame.popInt());
					return ExecState.SLEEPING;
				}
				if(invoke.name.equals("suspend")) {
					Scheduler.signalSuspend(frame);
					return ExecState.SUSPENDED;
				}
				if(invoke.name.equals("echo")) {
					if(frame.peekType() == ExecFrame.INT)
						Scheduler.signalEcho(frame, Integer.valueOf(frame.popInt()));
					else if(frame.peekType() == ExecFrame.FLOAT)
						Scheduler.signalEcho(frame, Float.valueOf(frame.popFloat()));
					else if(frame.peekType() == ExecFrame.REF)
						Scheduler.signalEcho(frame, frame.popRef());
					else
						throw new IllegalStateException();
					return ExecState.RUNNING;
				}
				throw new IllegalStateException();
			}

			frame.envMethod.envClass.env.invokeStatic(frame, invoke);
			break;
		}
		case Opcodes.INVOKEVIRTUAL: {
			frame.envMethod.envClass.env.invokeVirtual(frame, (MethodInsnNode) node);
			break;
		}
		// *RETURN
		case Opcodes.RETURN: {
			frame.callsite.subframe = null;
			frame.instructionPointer = -1;
			return ExecState.TERMINATED;
		}
		case Opcodes.IRETURN: {
			int val = frame.popInt();
			frame.callsite.pushInt(val);
			frame.callsite.subframe = null;
			frame.instructionPointer = -1;
			return ExecState.TERMINATED;
		}
		case Opcodes.FRETURN: {
			float val = frame.popFloat();
			frame.callsite.pushFloat(val);
			frame.callsite.subframe = null;
			frame.instructionPointer = -1;
			return ExecState.TERMINATED;
		}
		case Opcodes.ARETURN: {
			Object val = frame.popRef();
			frame.callsite.pushRef(val);
			frame.callsite.subframe = null;
			frame.instructionPointer = -1;
			return ExecState.TERMINATED;
		}

		case -1:
			break;

		default:
			throw new UnsupportedOperationException("opcode=" + node.getOpcode() + " " + node.getClass().getSimpleName());
		}

		if(ip == ipOld)
			frame.instructionPointer++;
		else
			frame.instructionPointer = ip;

		return ExecState.RUNNING;
	}
}
