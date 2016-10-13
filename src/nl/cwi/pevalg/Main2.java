package nl.cwi.pevalg;

import java.io.IOException;

import com.github.javaparser.ast.CompilationUnit;

public class Main2 {
	static <E> E someExp(ArithBindAlg<E> alg) {
		return alg.Let("x", alg.Add(alg.Lit(2), alg.Lit(3)), 
				alg.Add(alg.Lit(4), alg.Var("x")));
	}
	
	static <E> E someExp2(ArithBindAlg<E> alg) {
		return alg.Let("y", alg.Lit(3), 
				alg.Var("y"));
		
	}
	
	public static void main(String[] args) throws IllegalArgumentException, IOException, CompilationException, ReflectiveOperationException {
		 ArithBindAlg<AST> peval = (ArithBindAlg<AST>) PEvalFactory.make(EvalArithBind.class, ArithBindAlg.class, IEvalEnv.class);
		 AST ast = someExp(peval); // AST opaque type to hide Javaparser impl details.
		 CompilationUnit code = ast.peval("SomeExp", IEvalEnv.class);
		 System.out.println("CODE is:");
		 System.out.println(code);
		
		 IEvalEnv pEval = CUnitCompiler.compile(IEvalEnv.class, code);
		 Integer i = pEval.eval(new Env<Integer>());
		 System.out.println("RESULT is: "+ i);
		 
	}

}
