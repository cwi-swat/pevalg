package nl.cwi.pevalg;

@FunctionalInterface
public interface IEvalEnv {
	Integer eval(Env<Integer> env);
}
