package nl.cwi.pevalg;

public interface ArithBindAlg<E> {
	E Add(E l, E r);
	E Lit(int n);
	E Let(String x, E exp,  E body);
	E Var(String x);
}
