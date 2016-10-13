package nl.cwi.pevalg;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public class CUnitCompiler {

	private CompilationUnit cunit;

	public static <T> T compile(Class<T> iface, CompilationUnit code)
			throws ClassNotFoundException, CompilationException, IOException, ReflectiveOperationException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		out.write(code.toString());
		String className = code.getChildrenNodes().stream().filter(n -> n instanceof ClassOrInterfaceDeclaration)
			.map(n -> ((ClassOrInterfaceDeclaration) n).getName()).findFirst().get();
		JavaFileObject file = new JavaSourceFromString(
				className,
				writer.toString());
		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
		CompilationTask task = compiler.getTask(null, null, diagnostics, null, null, compilationUnits);

		boolean success = task.call();

		for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
			System.out.println(diagnostic.getCode());
			System.out.println(diagnostic.getKind());
			System.out.println(diagnostic.getPosition());
			System.out.println(diagnostic.getStartPosition());
			System.out.println(diagnostic.getEndPosition());
			System.out.println(diagnostic.getSource());
			System.out.println(diagnostic.getMessage(null));

		}
		System.out.println("Success: " + success);
		if (success) {
			try {

				URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { new File("").toURI().toURL() });
				Object o = Class.forName(className, true, classLoader).newInstance();
				return (T) Proxy.newProxyInstance(CUnitCompiler.class.getClassLoader(), new Class<?>[] { iface },
						new InvocationHandler() {

							@Override
							public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
								Method theMethod = Arrays
										.stream(Class.forName(className, true, classLoader).getDeclaredMethods())
										.filter(m -> m.getName().equals(method.getName())).findFirst().get();
								if (method == null)
									throw new NoSuchMethodException(method.getName());
								Object r = method.invoke(o, args);
								return r;
							}
						});

			} catch (ClassNotFoundException e) {
				System.err.println("Class not found: " + e);
				throw e;
			} catch (IllegalAccessException e) {
				System.err.println("Illegal access: " + e);
				throw e;
			} catch (InstantiationException e) {
				System.err.println("Instantiation: " + e);
				throw e;
			}
		} else
			throw new CompilationException();
	}

}

class JavaSourceFromString extends SimpleJavaFileObject {
	final String code;

	JavaSourceFromString(String name, String code) {
		super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
		this.code = code;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return code;
	}
}
