import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;


/*
 * TODO
 * - refactor into 1 visitor
 * - how to deal with many sorted?
 * - make the partial evaluator generic: use a proxy
 * 
 * Links
 * - https://bitbucket.org/mstrobel/procyon/wiki/Decompiler%20API
 * - https://github.com/javaparser/javaparser
 * 
 * This should be the interface I thinks
 * 
 * ExpAlg<AST> peval = PEvalFactory.make(EvalExp.class);
 * AST ast = someAST(peval); // AST opaque type to hide Javaparser impl details.
 * IEval eval = ast.peval(IEval.class);
 * [maybe separate peval from compile?]
 * 
 */

public class PEvalExp implements ExpAlg<MethodDeclaration>{
	
	private int counter = 0;
	private Map<String, MethodDeclaration> evalMethods = new HashMap<>();
	private List<MethodDeclaration> prog = new ArrayList<>();
	
	public static void main(String[] args) throws IOException {
		PEvalExp peval = new PEvalExp(EvalExp.class);
		MethodDeclaration top = someExp(peval);
		CompilationUnit cu = new CompilationUnit();
		cu.addImport(Map.class);
		
		ClassOrInterfaceDeclaration clz = cu.addClass("EvalExp$PE");
		clz.addModifier(Modifier.PUBLIC);
		clz.addImplements(IEval.class);
		for (MethodDeclaration m2: peval.prog) {
			clz.addMember(m2);
		}
		MethodDeclaration entry = new MethodDeclaration();
		entry.addModifier(Modifier.PUBLIC);
		entry.addAnnotation(Override.class);
		entry.setType(Integer.class);
		entry.setName("eval");
		entry.addParameter(Map.class, "env");
		entry.addParameter(Integer.class, "x");
		MethodCallExpr topCall = new MethodCallExpr();
		topCall.setName(top.getName());
		topCall.addArgument(new NameExpr("env"));
		topCall.addArgument(new NameExpr("x"));
		ReturnStmt ret = new ReturnStmt(topCall);
		entry.setBody(new BlockStmt(Collections.singletonList(ret)));
		clz.addMember(entry);
		cu.getImports().removeIf(id -> !id.getName().toString().contains("."));
		System.out.println(cu);
		
		
		// Compilation
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
	    StringWriter writer = new StringWriter();
	    PrintWriter out = new PrintWriter(writer);
	    out.write(cu.toString());
		JavaFileObject file = new JavaSourceFromString("EvalExp$PE", writer.toString());
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
	            Object o = Class.forName("EvalExp$PE", true, classLoader).newInstance();
	            Object r = Class.forName("EvalExp$PE", true, classLoader).getDeclaredMethod("eval", new Class[] { Map.class, Integer.class }).invoke(o, new Object[] { null, null });
	            System.out.println("The Result is: " + r);
	        } catch (ClassNotFoundException e) {
	          System.err.println("Class not found: " + e);
	        } catch (NoSuchMethodException e) {
	          System.err.println("No such method: " + e);
	        } catch (IllegalAccessException e) {
	          System.err.println("Illegal access: " + e);
	        } catch (InvocationTargetException e) {
	          System.err.println("Invocation target: " + e);
	        } catch (InstantiationException e) {
	          System.err.println("Instantiation: " + e);
			}
	      }
	    
	}
	
	static <E> E someExp(ExpAlg<E> alg) {
		return alg.Add(alg.Add(alg.Lit(1), alg.Lit(2)), alg.Lit(3));
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
	
	
	public PEvalExp(Class<?> clazz) throws IOException {
		InputStream in = new ByteArrayInputStream(decompile(clazz).getBytes());
		
    CompilationUnit cu;
    try {
        cu = JavaParser.parse(in);
        cu.accept(new MethodVisitor(), evalMethods);
        
    } finally {
    	if (in != null) {
        in.close();
    	}
    }
	}
	
	private static class MethodVisitor extends VoidVisitorAdapter<Map<String,MethodDeclaration>> {

		@Override
		public void visit(MethodDeclaration n,  Map<String,MethodDeclaration> arg) {
			if (Character.isUpperCase(n.getName().charAt(0))) {
				arg.put(n.getName(), n);
			}
			super.visit(n, arg);
		}
	}	
	

	@Override
	public MethodDeclaration Add(MethodDeclaration l, MethodDeclaration r) {
		MethodDeclaration m = evalMethods.get("Add");
		List<Type> formalTypes = getFormalTypes((ClassOrInterfaceType) m.getType());
	    MethodDeclaration newM = peval("Add", m, formalTypes, l, r);
		prog.add(newM);
		return newM;
	    
	}
	
	private List<Type> getFormalTypes(ClassOrInterfaceType iface){
		ClassOrInterfaceType functInterface = iface;
		Class<?> clazz;
		InputStream in = null;

		try {
			clazz = Class.forName(functInterface.getName());
		
			in = new ByteArrayInputStream(decompile(clazz).getBytes());
		
	    CompilationUnit cu = JavaParser.parse(in);
	        ClassOrInterfaceDeclaration dec = cu.getInterfaceByName(functInterface.getName());
	        List<Type> formalTypes = dec.getMethods().get(0).getParameters().stream().map(p -> p.getType()).collect(Collectors.toList());
	        
			return formalTypes;
	    } catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		  
	    } finally {
	    	if (in != null) {
	        try {
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
	    	}
	    }
	}

	@Override
	public MethodDeclaration Lit(int n) {
		MethodDeclaration m = evalMethods.get("Lit");
		//((ReturnStmt) m.getBody().getStmts().get(0)).getExpr()
		List<Type> formalTypes = getFormalTypes((ClassOrInterfaceType) m.getType());
	    MethodDeclaration newM = peval("Lit", m, formalTypes, n);
		prog.add(newM);
		return newM;
	}

	MethodDeclaration peval(String type, MethodDeclaration m, List<Type> formalTypes, Object...args) {
		MethodDeclaration newM = new MethodDeclaration();
		String methodName = type + "_" + counter++;
		newM.setName(methodName);
		newM.addModifier(Modifier.STATIC, Modifier.PRIVATE);
		m.accept(new ClosureVisitor(type, m.getParameters(), formalTypes, args), newM);
		return newM;
	}
	
	private static class ReplaceVarsVisitor extends ModifierVisitorAdapter<Object[]> {
		private List<Parameter> formals;
		private List<Parameter> closureFormals;

		public ReplaceVarsVisitor(List<Parameter> formals, List<Parameter> closureFormals) {
			this.formals = formals;
			this.closureFormals = closureFormals;
		}

		Object formalToValue(String name, Object[] args) {
			for (int i = 0; i < formals.size(); i++) {
				if (formals.get(i).getName().equals(name)) {
					return args[i];
				}
			}
			throw new NoSuchElementException();
		}
		
		@Override
		public Node visit(MethodCallExpr call, Object[] args) {
			System.out.println(call);
			if (call.getScope() == null) {
				// recursions are always called on arguments to factory methods.
				return call;
			}
			System.out.println(call.getScope().getClass());
			System.out.println(((NameExpr)call.getScope()).getName());
			System.out.println(formals);
			if (call.getScope() instanceof NameExpr) {
				// TODO: && call.getName().equals(eval);
				// and getScope() is one of the static paarms.
				// so we have l.eval(env),
				// must change to Lit_1(env);
				System.out.println("Yes");
				//List<Node> kids = call.getParentNode().getChildrenNodes();
				//int i = kids.indexOf(call);
				//System.out.println("Call index " + i);
				//kids.remove(i);
				try {
					Object val = formalToValue(((NameExpr)call.getScope()).getName(), args);
					System.out.println("VAL = " + val);
					return val2node(val); 
				}
				catch (NoSuchElementException e) {
					System.err.println("error " + e);
				}
			}
			return call;
		}
		
		
		@Override
		public Node visit(NameExpr n, Object[] args) {
			try {
				Object val = formalToValue(n.getName(), args);
				return val2node(val);
			}
			catch (NoSuchElementException e) {
				super.visit(n, args);	
			}
			return n;
		}

		private Expression val2node(Object val) {
			if (val instanceof MethodDeclaration) {
				// construct a call
				MethodDeclaration m = (MethodDeclaration)val;
				MethodCallExpr call = new MethodCallExpr();
				//call.setArgs(args); // this must be env etc.
				call.setArgs(closureFormals.stream().map(p -> new NameExpr(p.getName()))
					.collect(Collectors.toList()));
				call.setName(m.getName());
				return call;
			}
			else {
				if (val instanceof Integer) {
					return new IntegerLiteralExpr(val.toString());
				}
				if (val instanceof String) {
					return new StringLiteralExpr((String)val);
				}
				throw new AssertionError("not implemented literal " + val);
			}
		}
	}
	
	private static class ClosureVisitor extends VoidVisitorAdapter<MethodDeclaration> {
		
		private String type;
		private Object[] args;
		private List<Parameter> formals;
		private List<Type> formalTypes;

		public ClosureVisitor(String type, List<Parameter> formals, List<Type> formalTypes, Object... args) {
			this.type = type;
			this.formals = formals;
			this.args = args;
			this.formalTypes = formalTypes;
		}
		
		@Override
		public void visit(LambdaExpr lambda, MethodDeclaration newMethod) {
			List<Parameter> params = lambda.getParameters();
			for (int i = 0; i< params.size(); i++) {
				//params.get(i).setType(Map.class); // todo: needs to come from outside.
				params.get(i).setType(formalTypes.get(i));
			}
			newMethod.setParameters(params);
			List<Statement> lst = new ArrayList<>();
			if (lambda.getBody() instanceof BlockStmt) {
				for (Statement stm: ((BlockStmt)lambda.getBody()).getStmts()) {
					lst.add((Statement) stm.clone());
				}
			}
			else if (lambda.getBody() instanceof ExpressionStmt) {
				ExpressionStmt exp = (ExpressionStmt)lambda.getBody().clone();
				lst.add(new ReturnStmt(exp.getExpression()));
			}
			else {
				throw new RuntimeException("unhandled lambda body " + lambda.getBody());
			}
			BlockStmt body = new BlockStmt(lst);
			body.accept(new ReplaceVarsVisitor(formals, params), args);
			newMethod.setBody(body);
			newMethod.setElementType(Integer.class);
			
		}
	}
	
}

class JavaSourceFromString extends SimpleJavaFileObject {
	  final String code;

	  JavaSourceFromString(String name, String code) {
	    super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension),Kind.SOURCE);
	    this.code = code;
	  }

	  @Override
	  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
	    return code;
	  }
}
