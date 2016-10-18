package nl.cwi.pevalg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;

public class ReplaceVarsVisitor extends ModifierVisitorAdapter<Object[]> {
	private List<Parameter> formals;
	private List<String> stack;
	private Map<String, List<String>> stacks;
	private Map<String, List<String>> dependencies;
	private String newName;
	private Map<String, Expression> localInitializations = new HashMap<>();

	public ReplaceVarsVisitor(String newName, List<Parameter> formals, List<String> stack, Map<String, List<String>> stacks, Map<String, List<String>> dependencies) {
		this.formals = formals;
		this.stack = stack;
		this.stacks = stacks;
		this.dependencies = dependencies;
		this.newName = newName;
		
	}

	Object formalToValue(String name, Object[] args) {
		for (int i = 0; i < formals.size(); i++) {
			if (formals.get(i).getName().equals(name)) {
				return args[i];
			}
		}
		throw new NoSuchElementException();
	}
	
	public Node visit(CastExpr cast, Object[] args) {
		if (cast.getType() instanceof ClassOrInterfaceType){
			// TODO ugly hard-coding hack to deal with bad type inference of decompiler
			// Link
			// - https://bitbucket.org/mstrobel/procyon/issues/270/casting-issue-decompiled-code-doesnt
			if (((ClassOrInterfaceType) cast.getType()).getName().equals("Env")){
				return cast.getExpr();
			}
			
		}
		return cast;
	}
	
	@Override
	public Node visit(ObjectCreationExpr call, Object[] args) {
		if (call.getType().getName().equals("Env")) {
			List<Expression> newArgs = call.getArgs().stream().map(a -> (Expression) a.accept(this, args)).collect(Collectors.toList());
			call.setArgs(newArgs);
			localInitializations.put(((StringLiteralExpr) call.getArgs().get(0)).getValue(), call.getArgs().get(1));
		}
		return call;
	}

	@Override
	public Node visit(MethodCallExpr call, Object[] args) {
		System.out.println(call);
		if (call.getScope() == null) {
			// recursions are always called on arguments to factory methods.
			return call;
		}
		System.out.println(call.getScope().getClass());
		//System.out.println(((NameExpr)call.getScope()).getName());
		System.out.println(formals);
		
		List<Expression> newArgs = call.getArgs().stream().map(a -> (Expression) a.accept(this, args)).collect(Collectors.toList());
		call.setArgs(newArgs);
		if (call.getScope() instanceof NameExpr) {
			
			// TODO: naive assumption: every method named lookup is an environment lookup)
			if (call.getName().equals("lookup") && call.getArgs().size() == 1){
				if (call.getArgs().get(0) instanceof StringLiteralExpr){
					String varName = "__" + ((NameExpr)call.getScope()).getName() + "__" + ((StringLiteralExpr) call.getArgs().get(0)).getValue();
					stack.add(varName);
					return new NameExpr(varName);
				}
			}
			
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
				
				return val2node(val, newArgs); 
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

	private Expression val2node(Object val, List<Expression>... newArgs) {
		if (val instanceof MethodDeclaration) {
			// construct a call
			MethodDeclaration m = (MethodDeclaration)val;
			MethodCallExpr call = new MethodCallExpr();
			//call.setArgs(args); // this must be env etc.
			//call.setArgs(closureFormals.stream().map(p -> new NameExpr(p.getName()))
			//	.collect(Collectors.toList()));
			
			dependencies.get(newName).add(m.getName());
			
			if (stacks.containsKey(m.getName())){
				call.setArgs(
						stacks.get(m.getName()).stream().map(str -> new NameExpr(str)).collect(Collectors.toList()));
			}
			else{
				call.setArgs(newArgs[0]);
			}
			call.setName(m.getName());
			return call;
		}
		else {
			return toLiteral(val);
		}
	}

	private Expression toLiteral(Object val) {
		if (val instanceof Integer) {
			return new IntegerLiteralExpr(val.toString());
		}
		if (val instanceof String) {
			return new StringLiteralExpr((String)val);
		}
		if (val instanceof List){
			List<Object> l = (List<Object>) val;
			return new MethodCallExpr(new ClassExpr(new ClassOrInterfaceType("Arrays")), "asList", l.stream().map(v -> toLiteral(v)).collect(Collectors.toList()));
		}
		throw new AssertionError("not implemented literal " + val);
	}

	public Map<String, Expression> getLocalInitializations() {
		return localInitializations;
	}
	
	
}