package nl.cwi.pevalg;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.github.javaparser.ast.ArrayBracketPair;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EmptyMemberDeclaration;
import com.github.javaparser.ast.body.EmptyTypeDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralMinValueExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralMinValueExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.QualifiedNameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.TypeDeclarationStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;

public class ReplaceVarsVisitor extends ModifierVisitorAdapter<Object[]> {
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
	public Node visit(MethodCallExpr call, Object[] args) {
		System.out.println(call);
		if (call.getScope() == null) {
			// recursions are always called on arguments to factory methods.
			return call;
		}
		System.out.println(call.getScope().getClass());
		System.out.println(((NameExpr)call.getScope()).getName());
		System.out.println(formals);
		List<Expression> newArgs = call.getArgs().stream().map(a -> (Expression) a.accept(this, args)).collect(Collectors.toList());
		call.setArgs(newArgs);
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
			call.setArgs(newArgs[0]);
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