package nl.cwi.pevalg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;


public class PEval implements InvocationHandler {
	int counter = 0;
	private Map<String, MethodDeclaration> evalMethods = new HashMap<>();;
	private List<MethodDeclaration> prog = new ArrayList<>();
	private List<Type<?>> paramTypes;
	
	public PEval(Class<?> evAlg, Class<?> carrier) throws IOException {
		InputStream inEv = new ByteArrayInputStream(decompile(evAlg).getBytes());
		InputStream inCar = new ByteArrayInputStream(decompile(carrier).getBytes());
		
		CompilationUnit cuEv;
		CompilationUnit cuCar;
		
	    try {
	        cuEv = JavaParser.parse(inEv);
	        cuEv.accept(new MethodVisitor(), evalMethods);
	        cuCar = JavaParser.parse(inCar);
	        ClassOrInterfaceDeclaration carDec = 
	        		cuCar.getInterfaceByName(carrier.getSimpleName());
	        paramTypes = carDec.getMethods().get(0).getParameters().stream().map(p -> p.getType()).collect(Collectors.toList());
	        
	        
	    } finally {
	    	if (inEv != null) {
	    		inEv.close();
	    	}
	    	if (inCar != null){
	    		inCar.close();
	    	}
	    }
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return dispatchOn(method.getName(), args);
	}

	protected Object dispatchOn(String operation, Object[] args) {
		MethodDeclaration m = evalMethods.get(operation);
		//((ReturnStmt) m.getBody().getStmts().get(0)).getExpr()
		MethodDeclaration newM = peval(operation, m, args);
		prog.add(newM);
		return new MethodDeclarationBasedAST(newM, prog);
	}
	
	MethodDeclaration peval(String type, MethodDeclaration m, Object...args) {
		MethodDeclaration newM = new MethodDeclaration();
		String methodName = type + "_" + counter++;
		newM.setName(methodName);
		newM.addModifier(Modifier.STATIC, Modifier.PRIVATE);
		m.accept(new ClosureVisitor(type, m.getParameters(), paramTypes, args), newM);
		return newM;
	}
	
	String decompile(Class<?> clazz) {
		final DecompilerSettings settings = DecompilerSettings.javaDefaults();

		try (StringWriter writer = new StringWriter()) {

		    Decompiler.decompile(
		        clazz.getName(),
		        new PlainTextOutput(writer),
		        settings
		    );
		    String src = writer.toString();
		    System.out.println(src);
		    return src;
		}
		catch (final IOException e) {
		    // handle error
			System.out.println("error " + e);
			return null;
		}
	}

}
