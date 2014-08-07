package net.indiespot.script.interp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class EnvClass {
	Env env;
	ClassNode classNode;
	Map<String, EnvMethod> nameDesc2method;

	public EnvClass(Env env, ClassNode classNode) {
		this.env = env;
		this.classNode = classNode;
		this.nameDesc2method = new HashMap<>();

		for(Object m : classNode.methods) {
			MethodNode methodNode = (MethodNode) m;
			nameDesc2method.put(methodNode.name + " " + methodNode.desc, new EnvMethod(this, methodNode));
		}
	}

	public EnvMethod findMethod(String name, String desc) {
		EnvMethod envMethod = nameDesc2method.get(name + " " + desc);
		if(envMethod == null)
			throw new NoSuchElementException("method: " + classNode.name + "." + name + "" + desc);
		return envMethod;
	}

	private final static Map<String, char[]> args2types = new HashMap<>();

	public static synchronized char[] parseParams(String args) {
		char[] types = args2types.get(args);
		if(types == null)
			args2types.put(args, types = parseArgsImpl(args));
		return types;
	}

	private static char[] parseArgsImpl(String args) {
		char[] types = new char[8];

		int len = 0;
		for(int i = 0; i < args.length(); i++) {
			if(args.charAt(i) == 'I') {
				types[len++] = 'I';
			}
			else if(args.charAt(i) == 'F') {
				types[len++] = 'F';
			}
			else if(args.charAt(i) == 'L') {
				types[len++] = 'A';
				while (args.charAt(++i) != ';')
					continue;
			}
			else if(args.charAt(i) == '[') {
				continue;
			}
			else {
				throw new IllegalStateException();
			}
		}
		return Arrays.copyOf(types, len);
	}
}
