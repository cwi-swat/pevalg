import java.util.Map;

@FunctionalInterface
public interface IEval {
	int eval(Map<String, Integer> env);
}
