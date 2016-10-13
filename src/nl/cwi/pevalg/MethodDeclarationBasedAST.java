package nl.cwi.pevalg;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

public class MethodDeclarationBasedAST implements AST {

	private List<MethodDeclaration> prog;
	private MethodDeclaration methodDeclaration;

	public MethodDeclarationBasedAST(MethodDeclaration methodDeclaration, List<MethodDeclaration> prog) {
		super();
		this.prog = prog;
		this.methodDeclaration = methodDeclaration;
	}

	@Override
	public <T> CompilationUnit peval(String programName, Class<T> carrier) {
		MethodDeclaration top = prog.get(prog.size() - 1);
		CompilationUnit cu = new CompilationUnit();
		// TODO this should not be hard-coded
		cu.addImport(Map.class);
		cu.addImport(Env.class);

		ClassOrInterfaceDeclaration clz = cu.addClass(programName + "$PE");
		clz.addModifier(Modifier.PUBLIC);
		clz.addImplements(carrier);
		for (MethodDeclaration m : prog) {
			clz.addMember(m);
		}

		// Assumes one method in the carrier (being the carrier a functional
		// interface)
		Method method = Arrays.stream(carrier.getDeclaredMethods()).findFirst().get();

		MethodDeclaration entry = new MethodDeclaration();
		entry.addModifier(Modifier.PUBLIC);
		entry.addAnnotation(Override.class);
		entry.setType(method.getReturnType());
		entry.setName(method.getName());
		for (Parameter p : method.getParameters())
			entry.addParameter(p.getType(), p.getName());
		MethodCallExpr topCall = new MethodCallExpr();
		topCall.setName(top.getName());
		for (Parameter p : method.getParameters())
			topCall.addArgument(new NameExpr(p.getName()));
		ReturnStmt ret = new ReturnStmt(topCall);
		entry.setBody(new BlockStmt(Collections.singletonList(ret)));
		clz.addMember(entry);
		cu.getImports().removeIf(id -> !id.getName().toString().contains("."));
		System.out.println(cu);
		return cu;
	}

	public void setProg(List<MethodDeclaration> prog) {
		this.prog = prog;
	}

	public MethodDeclaration getMethodDeclaration() {
		return methodDeclaration;
	}

	public void setMethodDeclaration(MethodDeclaration methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
	}

}
