import java.util.Map;

public class EvalExp implements ExpAlg<IEval> {

	/*
	 * restrictions for now
	 * - fully typed closures
	 * - closures have statements, not exps.
	 */
	
	@Override
	public IEval Add(IEval l, IEval r) {
		return (Map<String, Integer> env) -> { return l.eval(env) + r.eval(env); };
	}

	@Override
	public IEval Lit(int n) {
		return (Map<String, Integer> env) -> { return n; };
	}

//	@Override
//	public IEval Let(String x, IEval exp, IEval body) {
//		return null;
//	}
//
//	@Override
//	public IEval Var(String x) {
//		return null;
//	}

}
