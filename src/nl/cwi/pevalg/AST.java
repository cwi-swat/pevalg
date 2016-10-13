package nl.cwi.pevalg;

import com.github.javaparser.ast.CompilationUnit;

public interface AST {
		<T> CompilationUnit peval(String programName, Class<T> carrier);

}
