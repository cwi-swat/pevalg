package nl.cwi.pevalg;

public class Env<X> {
	private String name;
	private X value;
	private Env<X> rest;
	private boolean isNil;
	
	public Env(String name, X value, Env<X> rest){
		this.name = name;
		this.value = value;
		this.rest = rest;
		this.isNil = false;
	}
	
	public Env(){
		this.isNil = true;
	}
	
	public X lookup(String x){
		if (!isNil){
			if (name.equals(x))
				return value;
			else
				return rest.lookup(x);
		}else{
			throw new RuntimeException(new ElementNotFoundException(x));
		}
	}

	public boolean isNil() {
		return isNil;
	}
	
}
