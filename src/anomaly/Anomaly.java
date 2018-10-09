package anomaly;

import com.microsoft.z3.Model;

public class Anomaly {
	private String name;
	private Model model;

	public Anomaly() {
		this.model = model;
	}

	public void announce() {
		System.out.println("I'm an anomaly!");
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
