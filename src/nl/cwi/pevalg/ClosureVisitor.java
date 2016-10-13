package nl.cwi.pevalg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ClosureVisitor extends VoidVisitorAdapter<MethodDeclaration> {
	
	private String type;
	private Object[] args;
	private List<Parameter> formals;
	private List<Type<?>> formalTypes;

	public ClosureVisitor(String type, List<Parameter> formals, List<Type<?>> formalTypes, Object... args) {
		this.type = type;
		this.formals = formals;
		
		// TODO ugly hack
		this.args = Arrays.stream(args).map(o -> (o instanceof MethodDeclarationBasedAST) ? ((MethodDeclarationBasedAST) o).getMethodDeclaration() : o).toArray();
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
