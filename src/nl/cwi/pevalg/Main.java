package nl.cwi.pevalg;

import java.io.IOException;

import com.github.javaparser.ast.CompilationUnit;

public class Main {
	static <E> E someExp(ExpAlg<E> alg) {
		return alg.Add(alg.Add(alg.Lit(1), alg.Lit(2)), alg.Lit(3));
	}
	
	public static void main(String[] args) throws IllegalArgumentException, IOException, CompilationException, ReflectiveOperationException {
		 ExpAlg<AST> peval = (ExpAlg<AST>) PEvalFactory.make(EvalExp.class, ExpAlg.class, IEval.class);
		 AST ast = someExp(peval); // AST opaque type to hide Javaparser impl details.
		 CompilationUnit code = ast.peval("SomeExp", IEval.class);
		 System.out.println("CODE is:");
		 System.out.println(code);
		
		 IEval pEval = CUnitCompiler.compile(IEval.class, code);
		 Integer i = pEval.eval(null, null);
		 System.out.println("RESULT is: "+ i);
		 
	}

}
