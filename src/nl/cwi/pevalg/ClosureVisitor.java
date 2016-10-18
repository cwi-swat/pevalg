package nl.cwi.pevalg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ClosureVisitor extends VoidVisitorAdapter<MethodDeclaration> {
	
	private String type;
	private Object[] args;
	private List<Parameter> formals;
	private List<Type<?>> formalTypes;
	private List<String> vars = new ArrayList<>();
	private Map<String, List<String>> stacks;
	private String newName;
	private Map<String, List<String>> dependencies;

	public ClosureVisitor(String type, String newName, List<Parameter> formals, List<Type<?>> formalTypes, Map<String, List<String>> stacks, Map<String, List<String>> dependencies, Object... args) {
		this.type = type;
		this.formals = formals;
		
		// TODO ugly hack
		this.args = Arrays.stream(args).map(o -> (o instanceof MethodDeclarationBasedAST) ? ((MethodDeclarationBasedAST) o).getMethodDeclaration() : o).toArray();
		this.formalTypes = formalTypes;
		this.stacks = stacks;
		this.newName = newName;
		this.dependencies = dependencies;
		if (stacks.get(newName) == null)
			stacks.put(newName, new ArrayList<>());
		if (dependencies.get(newName) == null)
			dependencies.put(newName, new ArrayList<>());
	}
	
	@Override
	public void visit(LambdaExpr lambda, MethodDeclaration newMethod) {
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
		ReplaceVarsVisitor rvVisitor = new ReplaceVarsVisitor(newName, formals, vars, stacks, dependencies);
		body.accept(rvVisitor, args);
		
		// TODO now assumes we just have an environment
		//List<Parameter> params = lambda.getParameters();
		//for (int i = 0; i< params.size(); i++) {
		//	//params.get(i).setType(Map.class); // todo: needs to come from outside.
		//	params.get(i).setType(formalTypes.get(i));
		//}
		
		//stacks.put(newName, vars);
		//List<String> computedVars = Stream.concat(dependencies.get(newName).stream(), Arrays.asList(newName).stream()).flatMap(s -> stacks.get(s).stream()).distinct().sorted().collect(Collectors.toList());
		//stacks.get(newName).addAll(computedVars);
		stacks.put(newName, vars);
		
		List<String> computedVars = Stream.concat(dependencies.get(newName).stream(), Arrays.asList(newName).stream()).flatMap(s -> stacks.get(s).stream()).distinct().sorted().collect(Collectors.toList());
		
		// TODO Mutability here is important but it is ugly
		stacks.get(newName).addAll(computedVars.stream().filter(s -> !vars.contains(s)).collect(Collectors.toList()));
		stacks.get(newName).sort(null);
		
		if (!rvVisitor.getLocalInitializations().isEmpty()){
			ListIterator<Statement> iter = lst.listIterator(lst.size()-1);
			for (String key : rvVisitor.getLocalInitializations().keySet()){
				iter.add(new ExpressionStmt(new VariableDeclarationExpr(new ClassOrInterfaceType("Integer"), new VariableDeclarator("__env__" + key, rvVisitor.getLocalInitializations().get(key)))));
				computedVars.remove("__env__" + key);
				stacks.get(newName).remove("__env__" + key);
			}
		}
		
		// TODO: Hardcoded Integer parameters
		List<Parameter> params = computedVars.stream().map(s -> new Parameter(new ClassOrInterfaceType("Integer"), new VariableDeclaratorId(s))).collect(Collectors.toList());
		newMethod.setParameters(params);
		newMethod.setBody(body);
		// TODO: Hardcoded Integer return type
		newMethod.setElementType(Integer.class);
		
	}

	public List<String> getVars() {
		return vars;
	}
	
	
}
