package nl.cwi.pevalg;
import java.util.Map;

public class EvalArithBind implements ArithBindAlg<IEvalEnv> {

	/*
	 * restrictions for now
	 * - fully typed closures
	 * - closures have statements, not exps.
	 */
	
	@Override
	public IEvalEnv Add(IEvalEnv l, IEvalEnv r) {
		return (Env<Integer> env) -> { return l.eval(env) + r.eval(env); };
	}

	@Override
	public IEvalEnv Lit(int n) {
		return  (Env<Integer> env) -> { return n; };
	}

	@Override
	public IEvalEnv Let(String x, IEvalEnv exp, IEvalEnv body) {
		return  (Env<Integer> env) -> { 
			return body.eval(new Env<>(x, exp.eval(env), env)); 
		};
	
	}

	@Override
	public IEvalEnv Var(String x) {
		return  (Env<Integer> env) -> { return env.lookup(x); };
	}

}
