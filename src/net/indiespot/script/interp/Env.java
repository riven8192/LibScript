package net.indiespot.script.interp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class Env {
	private Map<String, EnvClass> name2class = new HashMap<>();

	public void register(ClassNode classNode) {
		EnvClass envClass = new EnvClass(this, classNode);
		name2class.put(classNode.name.replace('.', '/'), envClass);
		name2class.put(classNode.name.replace('/', '.'), envClass);
	}

	public EnvClass findClass(String name) {
		return name2class.get(name);
	}

	public void invokeStatic(ExecFrame frame, MethodInsnNode invoke) {
		String args = invoke.desc.substring(invoke.desc.indexOf('(') + 1, invoke.desc.indexOf(')'));
		char[] paramTypes = EnvClass.parseParams(args);

		EnvClass envClass = frame.envMethod.envClass.env.findClass(invoke.owner);
		EnvMethod envMethod = envClass.findMethod(invoke.name, invoke.desc);

		ExecFrame invocation = new ExecFrame(frame, envMethod);
		for(int i = paramTypes.length - 1; i >= 0; i--) {
			if(paramTypes[i] == 'I')
				invocation.setInt(i, frame.popInt());
			else if(paramTypes[i] == 'F')
				invocation.setFloat(i, frame.popFloat());
			else if(paramTypes[i] == 'A')
				invocation.setRef(i, frame.popRef());
			else
				throw new IllegalStateException("argTypes[" + i + "]=" + paramTypes[i]);
		}

		frame.subframe = invocation;
	}

	public void invokeVirtual(ExecFrame frame, MethodInsnNode invoke) {
		Class<?> clazz;
		try {
			clazz = Class.forName(invoke.owner.replace('/', '.'));
		}
		catch (ClassNotFoundException e1) {
			throw new IllegalStateException(e1);
		}

		Method method = null;
		for(Method mthd : clazz.getMethods())
			if(mthd.getName().equals(invoke.name))
				method = mthd;

		Class<?>[] params = method.getParameterTypes();
		Object[] values = new Object[params.length];
		for(int i = params.length - 1; i >= 0; i--) {
			if(params[i] == int.class)
				values[i] = Integer.valueOf(frame.popInt());
			else if(params[i] == float.class)
				values[i] = Float.valueOf(frame.popFloat());
			else
				values[i] = frame.popRef();
		}

		Object target = frame.popRef();
		if(target == null)
			throw new NullPointerException();

		try {
			Object got = method.invoke(target, values);

			if(method.getReturnType() == int.class)
				frame.pushInt(((Integer) got).intValue());
			else if(method.getReturnType() == float.class)
				frame.pushFloat(((Float) got).floatValue());
			else if(method.getReturnType() != void.class)
				frame.pushRef(got);
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}
}