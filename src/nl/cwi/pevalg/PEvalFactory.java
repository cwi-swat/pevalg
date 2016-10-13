package nl.cwi.pevalg;

import java.io.IOException;
import java.lang.reflect.Proxy;

public class PEvalFactory {

	public static Object make(Class<?> evAlg, Class<?> algIface, Class<?> carrier) throws IllegalArgumentException, IOException {
		return Proxy.newProxyInstance(
			PEvalFactory.class.getClassLoader(),
			new Class<?>[]{
				algIface
			},
			new PEval(evAlg, carrier));
	}



}
 