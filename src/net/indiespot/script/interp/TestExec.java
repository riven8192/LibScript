package net.indiespot.script.interp;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import org.objectweb.asm.util.TraceClassVisitor;

public class TestExec {
	public static void main(String[] args) throws IOException {

		{
			InputStream in = TestExec.class.getResourceAsStream("/net/indiespot/script/interp/TestScript.class");

			ClassReader classReader = new ClassReader(in);
			PrintWriter printWriter = new PrintWriter(System.out);
			TraceClassVisitor traceClassVisitor = new TraceClassVisitor(printWriter);
			classReader.accept(traceClassVisitor, ClassReader.SKIP_DEBUG);
		}

		InputStream in = TestExec.class.getResourceAsStream("/net/indiespot/script/interp/TestScript.class");

		ClassReader cr = new ClassReader(in);
		ClassNode classNode = new ClassNode();
		cr.accept(classNode, 0);

		System.out.println(classNode);

		System.out.println(TestScript.div(1337, 14));
		System.out.println(TestScript.div2(1337, 14));

		//Let's move through all the methods
		Env env = new Env();
		env.register(classNode);

		{
			EnvMethod envMethod = env.findClass("net.indiespot.script.interp.TestScript").findMethod("bus", "(Ljava/lang/Object;)I");

			ExecFrame callsite = new ExecFrame();
			ExecFrame execFrame = envMethod.call(callsite);

			execFrame.setRef(0, "nice-bus");
			while (execFrame.instructionPointer != -1) {
				Interpreter.step(execFrame);
			}
			System.out.println(callsite.popInt());
		}
		{
			EnvMethod envMethod = env.findClass("net.indiespot.script.interp.TestScript").findMethod("div3", "(II)I");

			ExecFrame callsite = new ExecFrame();
			ExecFrame execFrame = envMethod.call(callsite);

			execFrame.setInt(0, 1337);
			execFrame.setInt(1, 14);
			while (execFrame.instructionPointer != -1) {
				Interpreter.step(execFrame);
			}
			System.out.println(callsite.popInt());
		}
	}

}
