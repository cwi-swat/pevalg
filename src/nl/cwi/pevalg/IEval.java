package nl.cwi.pevalg;
import java.util.Map;

@FunctionalInterface
public interface IEval {
	Integer eval(Map<String, Integer> env, Integer y);
}
