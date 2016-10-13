package nl.cwi.pevalg;
import java.util.Map;

public class EvalExp implements ExpAlg<IEval> {

	/*
	 * restrictions for now
	 * - fully typed closures
	 * - closures have statements, not exps.
	 */
	
	@Override
	public IEval Add(IEval l, IEval r) {
		return (Map<String, Integer> env, Integer z) -> { return l.eval(env, z) + r.eval(env, z); };
	}

	@Override
	public IEval Lit(int n) {
		return (Map<String, Integer> env, Integer y) -> { return n; };
	}

//	@Override
//	public IEval Let(String x, IEval exp, IEval body) {
//		return (Map<String, Integer> env, Integer y) -> { 
//			body.eval(env , y)
//		}
//
//	@Override
//	public IEval Var(String x) {
//		return (Map<String, Integer> env, Integer y) -> { return env.get(x); };
//	}

}
