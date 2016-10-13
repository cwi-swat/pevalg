package nl.cwi.pevalg;

public class ElementNotFoundException extends Exception {

	public ElementNotFoundException(String x) {
		super("The element "+ x + " has not been found.");
	}

}
