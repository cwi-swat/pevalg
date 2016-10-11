import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
		ClassOrInterfaceDeclaration clz = new ClassOrInterfaceDeclaration();
		clz.setName("EvalExp$PE");
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
		MethodCallExpr topCall = new MethodCallExpr();
		topCall.setName(top.getName());
		topCall.addArgument(new NameExpr("env"));
		ReturnStmt ret = new ReturnStmt(topCall);
		entry.setBody(new BlockStmt(Collections.singletonList(ret)));
		clz.addMember(entry);
		System.out.println(clz);
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
		MethodDeclaration newM = peval("Add", m, l, r);
		prog.add(newM);
		return newM;
	}

	@Override
	public MethodDeclaration Lit(int n) {
		MethodDeclaration m = evalMethods.get("Lit");
		MethodDeclaration newM = peval("Lit", m, n);
		prog.add(newM);
		return newM;
	}

	MethodDeclaration peval(String type, MethodDeclaration m, Object...args) {
		MethodDeclaration newM = new MethodDeclaration();
		String methodName = type + "_" + counter++;
		newM.setName(methodName);
		newM.addModifier(Modifier.STATIC, Modifier.PRIVATE);
		m.accept(new ClosureVisitor(type, m.getParameters(), args), newM);
		return newM;
	}
	
	private static class ReplaceVarsVisitor extends ModifierVisitorAdapter<Object[]> {
		private List<Parameter> formals;

		public ReplaceVarsVisitor(List<Parameter> formals) {
			this.formals = formals;
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

		public ClosureVisitor(String type, List<Parameter> formals, Object... args) {
			this.type = type;
			this.formals = formals;
			this.args = args;
		}
		
		@Override
		public void visit(LambdaExpr lambda, MethodDeclaration newMethod) {
			List<Parameter> params = lambda.getParameters();
			for (Parameter p: params) {
				p.setType(Map.class); // todo: needs to come from outside.
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
			body.accept(new ReplaceVarsVisitor(formals), args);
			newMethod.setBody(body);
			newMethod.setElementType(Integer.class);
			
		}
	}
	
}
