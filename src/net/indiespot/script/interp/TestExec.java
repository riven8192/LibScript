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

		Env env = new Env();
		env.register(classNode);
		EnvClass clazz = env.findClass("net.indiespot.script.interp.TestScript");

		Scheduler scheduler = new Scheduler();

		{
			EnvMethod envMethod = clazz.findMethod("div", "(II)I");
			ExecFrame execFrame = envMethod.prepare(new TerminationHandler() {
				@Override
				public void onTermination(ExecFrame callsite) {
					System.out.println("div:" + callsite.popInt());
				}
			});
			execFrame.setInt(0, 1337);
			execFrame.setInt(1, 14);
			scheduler.start(execFrame);
		}

		{
			EnvMethod envMethod = clazz.findMethod("div2", "(II)I");
			ExecFrame execFrame = envMethod.prepare(new TerminationHandler() {
				@Override
				public void onTermination(ExecFrame callsite) {
					System.out.println("div2:" + callsite.popInt());
				}
			});
			execFrame.setInt(0, 1337);
			execFrame.setInt(1, 14);
			scheduler.start(execFrame);
		}

		{
			EnvMethod envMethod = clazz.findMethod("div3", "(II)I");
			ExecFrame execFrame = envMethod.prepare(new TerminationHandler() {
				@Override
				public void onTermination(ExecFrame callsite) {
					System.out.println("div3:" + callsite.popInt());
				}
			});
			execFrame.setInt(0, 1337);
			execFrame.setInt(1, 14);
			scheduler.start(execFrame);
		}

		if(Math.random() < 10000) {
			while (true) {
				scheduler.tick();

				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
